package nodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import commons.Tarea;
import nodes.components.ControladorHoja;
import nodes.components.atributos.AtributosHoja;
import nodes.components.clientes.ClienteNH_Gral;
import nodes.components.servidores.ConsultorH;
import nodes.components.servidores.ConsultorMantenimientoNH;
import nodes.components.servidores.Servidor;

/**
 * Nodo Hoja del sistema distribuido. Estos poseen las imágenes a compartir, calculan sus vectores
 * característicos, emiten consultas, respuestas y calculan similitud entre imágenes.
 * Están conectadas a dos Nodos Centrales.
 * 
 * La faceta cliente responde a un esquema Productor/Consumidor: un thread (gui) se encarga de producir las consultas
 * que los hilos consumidores (valga la redundancia) enviarán a los NCs.
 *
 * @author rodrigo
 *
 */
public class NodoHoja {
	// Atributos
	//===========
	
	/* No te olvides que usas "private ArrayList<Thread> clientThreads" y "private HashMap<String, Runnable> clients" porque de esa
	 * manera tenes en una variable los hilos que están corriendo y en la otra las instancias que corren los hilos anteriores de manera de poder
	 * acceder a ellas si fuera necesario.
	 */
	
	private ArrayList<Thread> clientThreads;
	private ArrayList<Thread> producerThreads;
	private ArrayList<Thread> serverThreads;
	private AtributosHoja atributos;
	private HashMap<String, Runnable> clients;
	private HashMap<String, Runnable> producers;
	private HashMap<String, Servidor> servers;
	private Integer maxClientes;
	private Properties config;
	private Servidor servidorAcceso, servidorCentrales, servidorHojas;
	
	
	// Métodos
	//=========
	public NodoHoja(String configFile) throws UnknownHostException {
		Integer cantCentrales;
		String[] centrales;
		
		try {
			config = new Properties();
			config.load( new FileInputStream(configFile) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		
		// Inicializaciones generales
		atributos = new AtributosHoja();
		clients = new HashMap<String, Runnable>();
		producers = new HashMap<String, Runnable>();
		servers = new HashMap<String, Servidor>();
		clientThreads = new ArrayList<Thread>();
		producerThreads = new ArrayList<Thread>();
		serverThreads = new ArrayList<Thread>();
		
		// Seteo de los atributos de la H.
		atributos = new AtributosHoja();
		atributos.setDireccion(InetAddress.getByName(config.getProperty("ip")));
		
		// Inicialización de las colas donde se cargarán las "tareas" 
		atributos.setNombreColas(new String[] {"salida"});
		atributos.setColas();
		
		// NABC que oficia de access point a la red
		atributos.setWkanInicial(config.getProperty("wkan"));
		
		// NCs a los que se conectará la H -> el wkan le indicará cuáles son
		atributos.setCantCentrales(Integer.parseInt(config.getProperty("max_nc")));
		
		// Definición de servidores
		// La H por ser el Nodo más viejo que hice no tiene separados en distintos archivos los Consultores de c/u de los Nodos
		this.servers.put("acceso", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_na,
				config.getProperty("nombre")+": Acceso",
				ConsultorH.class
		));
		this.servers.put("centrales", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nc,
				config.getProperty("nombre")+": Centrales",
				ConsultorH.class
		));
		this.servers.put("hojas", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nh,
				config.getProperty("nombre")+": Hojas",
				ConsultorH.class
		));
		this.servers.put("mantenimiento", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_m,
				config.getProperty("nombre")+": Mantenimiento",
				ConsultorMantenimientoNH.class
		));

		for (Servidor server : servers.values())
			serverThreads.add(new Thread(server));
		
		// Productores
		producers.put("GUI", new ControladorHoja());
		
		for (Runnable producer : producers.values())
			producerThreads.add(new Thread(producer));
		
		// Consumidores
		for (int i=0; i < Integer.parseInt(config.getProperty("max_nc")); i++) {
			switch (i) {
				case 0:
					// Nada especial, sólo quiero un nombre distinto porque va a tratar con WKANs además de NCs
					clients.put("PPAL + NC-0", new ClienteNH_Gral(0));
					break;
				default:
					clients.put("NC-" + Integer.toString(i), new ClienteNH_Gral(i));
					break;
			}
		}
		
		for (Runnable client : clients.values())
			clientThreads.add(new Thread(client));

	}
	
	
	public void ponerEnMarcha() throws InterruptedException{
		HashMap<String, Object> payload = new HashMap<String, Object>();
		
		for (Thread thread : producerThreads)
			thread.start();
		
		for (Thread thread : clientThreads)
			thread.start();
		
		for (Thread thread : serverThreads)
			thread.start();
				
		// Conexión con WKAN para ingreso a la red: no hay saludo inicial, directamente solicita NCs
		payload.put("direccionWKAN", atributos.getWkanInicial());
		payload.put("primeraVez", true);
		atributos.encolar("salida", new Tarea("SOLICITUD_NCS", payload));
		
		// Bucle "ppal" de la HOJA: revisión y recuperación de hilos mientras hilo PRODUCTOR esté vivo
		while(producerThreads.get(0).getState() != Thread.State.TERMINATED) {
			// NC amount check
			NCPeriodicCheck();

			// TODO 2020-11-23: por ahora comento lo de abajo porque me molestan para debuggear el funcionamiento de la
			//  red

			// Clients status
			// ClientThreadsStatus();

			// Servers status
			// ServerThreadsStatus();
			
			// Pause for 10 seconds
            try {Thread.sleep(10000);}
            catch (InterruptedException e) {e.printStackTrace();}
            
            // System.out.println("\n[HOJA] Waiting until Producer stop...");
		}
		
		// TODO: ¿debería controlar que la salida del while anterior sea porque efectivamente se salió desde el menú?
		
		// Detención de todos los threads en ejecución
		for (Thread thread : clientThreads)
			thread.interrupt();
		
		for (Thread thread : producerThreads)
			thread.interrupt();
		
		for (Thread thread : serverThreads)
			thread.interrupt(); // Por alguna razón esto no para al thread servidor
		
		// Esto no sirver, el hilo servidor queda corriendo indefinidamente
		/*while(serverThreads.get(0).getState() != Thread.State.TERMINATED) {
			serverThreads.get(0).interrupt();
			System.out.println("\t\tX");
		}*/

		try {Thread.sleep(1000);}
        catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println("[HOJA] Finalizando ejecución...");
		for(int i=0; i < clientThreads.size(); i++){
			System.out.print("\tconsumer thread #"+ Integer.toString(i) +" state: ");
			System.out.println(clientThreads.get(i).getState().toString());
		}
		
		for(int i=0; i < producerThreads.size(); i++){
			System.out.print("\tproducer thread #"+ Integer.toString(i) +" state: ");
			System.out.println(producerThreads.get(i).getState().toString());
		}
		
		for(int i=0; i < serverThreads.size(); i++){
			System.out.print("\tserver thread #"+ Integer.toString(i) +" state: ");
			System.out.println(serverThreads.get(i).getState().toString());
		}
		
		System.out.println("\n[HOJA] Terminada");
		System.exit(0);
	}


	/**
	 * Controla periódicamente que el nodo esté conectado a la cantidad requerida de NCs.
	 * De no ser así dispara la tarea de solicitud al WKAN.
	 *
	 * Existe un retardo o tiempo de espera entre solicitud y solicitud a fin de no saturar al WKAN receptor
	 *
	 * @since 2020-04-19
	 * */
	private void NCPeriodicCheck() throws InterruptedException {
		HashMap<String, Object> payload = new HashMap<String, Object>();
		Integer difference;
		long timeElapsed;

		// Si aún no se hizo el pedido inicial, no se vuelve a encolar la tarea
		if (((AtributosHoja) atributos).solicitudNCs.getLastRequest() == null)
			return;

		difference = ((AtributosHoja) atributos).getCantCentrales();
		difference -= ((AtributosHoja) atributos).getCentrales().keySet().size();

		if (difference > 0) {
			timeElapsed = Duration.between(((AtributosHoja) atributos).solicitudNCs.getLastRequest(),
					Instant.now()).toSeconds();

			if (timeElapsed > ((AtributosHoja) atributos).solicitudNCs.getLastDelay()) {
				payload.put("direccionWKAN", atributos.getWkanInicial());
				payload.put("primeraVez", false);
				atributos.encolar("salida", new Tarea("SOLICITUD_NCS", payload));

				((AtributosHoja) atributos).solicitudNCs.setNextDelay();

				System.out.printf("NCs Periodic Check: asked %d nodes\t[OK]\n", difference);
			}
		}
	}


	// TODO 2020-11-23: la manera en que tengo un hash <servers> y otro de <serverthreads> (y los análogos para los
	//  clientes es una porquería porque no escala y es difícil de mantener.
	// Puede hacer una clase <RunningInstance> o algo así dónde tenga todos los parámetros necesarios para crear un
	// nuevo hilo caído y listo. Puedo tener un único arreglo de "hilos que deben correr siempre" y darles un único
	// tratamiento (es decir, checkeo el status del hilo (el hilo sería un atributo de la clase), si está caído lo
	// revivo porque tengo todos los parámetros en la misma clase RunningInstance y chau).
	// Puedo tener N arreglos/hash/loquesea donde estén esas RunningInstance, eso es cuestión de orden


	/** Controla el estado de los hilos Clientes y los revive en caso de ser necesario */
	private void ClientThreadsStatus() {
		for(int i=0; i < clientThreads.size(); i++){
			System.out.print("\t" + clientThreads.get(i).getName() + " || ");
			System.out.print("Consumer thread #" + Integer.toString(i) + " state: ");
			System.out.println(clientThreads.get(i).getState().toString());

			// revivir los consumidores caídos
			// TODO: hacerlo bien porque no es así
			// TODO 2020-11-23: creo que me refería a los threads caídos
			if(clientThreads.get(i).getState() == Thread.State.TERMINATED) {
				// SIEMPRE en la posición 0 va a estar el cliente que se conecta al WKAN.
				// En las demás estan los que se conectan a NCs
				String name = null;
				String node = null;
				switch (i) {
					case 0:
						name = "PPAL - NC0";
						node = this.config.getProperty("wkan");
						break;
					default:
						name = "NC" + i;
						node = this.config.getProperty("nc_" + i);
						break;
				}

				// Esta parte está horrible, porque no sé qué keys del hashmap de clientes se correponde con cada
				// posición del arreglo de client threads. Pero bueno, queda así por ahora
				clients.put(name, new ClienteNH_Gral(i));
				clientThreads.set(i, new Thread(clients.get(name)));
				clientThreads.get(i).start();

				System.out.print("\t\tConsumer thread #" + Integer.toString(i) + " revived. ");
				System.out.println("Actual state: " + clientThreads.get(i).getState().toString());
			}
		}
	}


	private void ServerThreadsStatus() {
		String name = null;

		for(int i=0; i < serverThreads.size(); i++) {
			System.out.print("\t" + serverThreads.get(i).getName() + " || ");
			System.out.print("Server thread state: " + serverThreads.get(i).getState().toString());

			if (serverThreads.get(i).getState() == Thread.State.TERMINATED) {
				switch (i) {
					case 0:
						name = "acceso";
						this.servers.put("acceso", new Servidor(
								atributos.getDireccion().ip.getHostAddress(),
								atributos.getDireccion().puerto_na,
								config.getProperty("nombre")+": Acceso",
								ConsultorH.class
						));
						break;
					case 1:
						name = "centrales";
						this.servers.put("centrales", new Servidor(
								atributos.getDireccion().ip.getHostAddress(),
								atributos.getDireccion().puerto_nc,
								config.getProperty("nombre")+": Centrales",
								ConsultorH.class
						));
						break;
					case 2:
						name = "hojas";
						this.servers.put("hojas", new Servidor(
								atributos.getDireccion().ip.getHostAddress(),
								atributos.getDireccion().puerto_nh,
								config.getProperty("nombre")+": Hojas",
								ConsultorH.class
						));
						break;
				}

				serverThreads.set(i, new Thread(servers.get(name)));
				serverThreads.get(i).start();

				System.out.print("\t\tServer thread revived. ");
				System.out.println("Actual state: " + serverThreads.get(0).getState().toString());
			}
		}
	}

}// Fin clase

/* 
 * Los threads corren una instancia de una clase. Desde acá, o sea usando el hilosConsumidores[i]
 * accedo al thread pero no tengo manera de llegar a a instancia que ejecuta ese hilo a menos que guarde dicha
 * instancia en un arreglo. Por ahora para lo único que se me ocurre que podría llegar a necesitarla
 * es para ver el ID de consumidor del que se trata, pero nada más.
 * 
 * En su lugar lo hago fácil y hago que coincida el índice de hilosConsumidores con el ID de consumidor y listo
 * 
 * Lo que digo es que lo primero no se puede hacer y lo segundo sí:
 * 
 * System.out.print("\nSetting up thread: ");
 * hilosConsumidores[0] = new Thread( new HojaConsumidor(0,
 *	        			atributos.getDireccionesNCs()[0].split(":")[0],
 *						Integer.parseInt(atributos.getDireccionesNCs()[0].split(":")[1])) );
 * System.out.println("DONE");
 * System.out.println("Instancia que corre el hilo: " + Integer.toString(hilosConsumidores[i].idConsumidor));
 *
 *
 * HojaConsumidor consumidor = new HojaConsumidor(0,atributos.getDireccionesNCs()[0].split(":")[0],Integer.parseInt(atributos.getDireccionesNCs()[0].split(":")[1]));
 * hilosConsumidores[0] = new Thread( consumidor );
 * System.out.println("DONE");
 * System.out.println("Instancia que corre el hilo: " + Integer.toString(consumidor.idConsumidor));
 * 
 *  
 * */
