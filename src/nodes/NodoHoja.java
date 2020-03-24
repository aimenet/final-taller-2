package nodes;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import nodes.components.AtributosAcceso;
import nodes.components.AtributosHoja;
import nodes.components.ClienteNA_NA;
import nodes.components.ClienteNA_NC;
import nodes.components.ConsultorH;
import nodes.components.ConsultorNA_NA;
import nodes.components.ConsultorNA_NC;
import nodes.components.ControladorHoja;
import nodes.components.HojaConsumidor;
import nodes.components.Servidor;
import nodes.components.WorkerNA_Interno;

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
	public NodoHoja(String configFile){
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
		atributos.setIpServidor(config.getProperty("ip"));
		atributos.setPuertoServidor(Integer.parseInt(config.getProperty("puerto_nh")));
		// Recordar que los NH tiene un único puerto servidor porque es viejo y todavía no había definido el esquema actual.
		// Uso el puerto_nh pero no es exclusivo para Hojas
		
		// NABC que oficia de access point a la red
		atributos.setWkanInicial(config.getProperty("wkan"));
		
		// NCs a los que se conectará la H -> el wkan le indicará cuáles son
		atributos.setCantCentrales(Integer.parseInt(config.getProperty("max_nc")));
		
		// Definición de servidores
		// La H por ser el Nodo más viejo que hice no tiene separados en distintos archivos los Consultores de c/u de los Nodos
		servers.put("GENERAL", new Servidor(Integer.parseInt(config.getProperty("puerto_nh")), 
				                            config.getProperty("nombre")+": General", 
				                            ConsultorH.class));
		
		for (Servidor server : servers.values())
			serverThreads.add(new Thread(server));
		
		// Productores
		producers.put("GUI", new ControladorHoja());
		
		for (Runnable producer : producers.values())
			producerThreads.add(new Thread(producer));
		
		// Consumidores: se define uno solo, cuando se conozcan los NCs se ampliará la cantidad según corresponda
		clients.put("PPAL - NC0", new HojaConsumidor(0));
		
		for (Runnable client : clients.values())
			clientThreads.add(new Thread(client));
	}
	
	public void ponerEnMarcha(){
		for (Thread thread : producerThreads)
			thread.start();
		
		for (Thread thread : clientThreads)
			thread.start();
		
		for (Thread thread : serverThreads)
			thread.start();
				
		// atributos.getWkanInicial().split(":")[0],
		// Integer.parseInt(atributos.getWkanInicial().split(":")[1])));
		
		// Bucle "ppal" de la HOJA: revisión y recuperación de hilos mientras hilo PRODUCTOR esté vivo
		while(producerThreads.get(0).getState() != Thread.State.TERMINATED) {
			// Check consumer threads status
            System.out.println("\n[HOJA] check consumer threads status...");
            for(int i=0; i < clientThreads.size(); i++){
            	System.out.print("\t" + clientThreads.get(i).getName() + " || ");
    			System.out.print("Consumer thread #" + Integer.toString(i) + " state: ");
    			System.out.println(clientThreads.get(i).getState().toString());
    			
    			// revivir los consumidores caídos
    			if(clientThreads.get(i).getState() == Thread.State.TERMINATED) {
    				// SIEMPRE en la posición 0 va a estar el cliente que se conecta al WKAN. En las demás estan los que se conectan a NCs
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
    				
    				// Esta parte está horrible, porque no sé qué keys del hashmap de clientes se correponde con cada posición
    				// del arreglo de client threads. Pero bueno, queda así por ahora
    				clients.put(name, new HojaConsumidor(i, node.split(":")[0], Integer.parseInt(node.split(":")[1])));
    				clientThreads.set(i, new Thread(clients.get(name)));
    				clientThreads.get(i).start();
    				
    				System.out.print("\t\tConsumer thread #" + Integer.toString(i) + " revived. ");
    				System.out.println("Actual state: " + clientThreads.get(i).getState().toString());
    			}
    		}
			
			// Check server thread status
			// Por ahora hay un único server -> generalizarlo a muchos
            System.out.println("\n[HOJA] check server thread status...");
			System.out.print("\t" + serverThreads.get(0).getName() + " || ");
			System.out.print("Server thread state: " + serverThreads.get(0).getState().toString());
			
			if(serverThreads.get(0).getState() == Thread.State.TERMINATED) {
				servers.put("GENERAL", new Servidor(Integer.parseInt(config.getProperty("puerto_server")), 
                        config.getProperty("nombre")+": General", 
                        ConsultorH.class));
				
				serverThreads.set(0, new Thread(servers.get("GENERAL")));
				serverThreads.get(0).start();
				
				System.out.print("\t\tServer thread revived. ");
				System.out.println("Actual state: " + serverThreads.get(0).getState().toString());
			}
			
			// Pause for 10 seconds
            try {Thread.sleep(10000);}
            catch (InterruptedException e) {e.printStackTrace();}
            
            System.out.println("\n[HOJA] Waiting until Producer stop...");
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
