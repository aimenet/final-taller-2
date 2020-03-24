package nodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import commons.Tarea;
import nodes.components.AtributosAcceso;
import nodes.components.ClienteNA_NA;
import nodes.components.ClienteNA_NC;
import nodes.components.ConsultorNA_NA;
import nodes.components.ConsultorNA_NC;
import nodes.components.ConsultorNA_NH;
import nodes.components.Servidor;
import nodes.components.WorkerNA_Interno;


/**
 * Nodo de Acceso Bien Conocido (wkan por sus siglas en inglés o nabc en español). 
 * 
 * Como su nombre lo indica es el punto de acceso de los demás nodos (independientemente de su tipo) a la red.
 * En conjunto forman una topología (lógica) de malla (mesh) completamente conectada, intercambiando mensajes 
 * en broadcast. Esto ofrece ventajas (como la NO necesidad de retransmitir mensajes a vecinos o cosas así)
 * pero también desventajas como la saturación de la red con mensajes
 * 
 * La función de los WKAN es "administrar" una serie de NC (determinada por la capacidad de su hardware), de los
 * cuales lleva registro de su carga de trabajo (NH a los que sirve cada uno). De esta forma ante la llegada de un
 * Nodo Central nuevo a la red podrá realizarse balanceo de carga entre los integrantes de la red a fin de
 * distribuirlo de la manera más conveniente posible
 * 
 * @author rodrigo
 */
public class NodoAccesoBienConocido {
	private AtributosAcceso atributos;
	private Integer id, maxClientes;
	private Properties config;
	private Servidor servidorHojas, servidorCentrales, servidorAcceso;
	private HashMap<String,Servidor> servers;
	private ArrayList<Thread> serverThreads;
	private HashMap<String, Runnable> clients;
	private ArrayList<Thread> clientThreads;
    private ArrayList<String> wkanIniciales;
    
	//TODO: éste creo que va a ser el único Nodo que al final recibirá archivo de configuración
	public NodoAccesoBienConocido(String archivoConfiguracion){
		Integer aux_puerto;
		String aux_ip;
		
		try {
			config = new Properties();
			config.load( new FileInputStream(archivoConfiguracion) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		
		// Inicializaciones generales
		atributos = new AtributosAcceso();
		clients = new HashMap<String, Runnable>();
		clientThreads = new ArrayList<Thread>();
		servers = new HashMap<String, Servidor>();
		serverThreads = new ArrayList<Thread>();
		wkanIniciales = new ArrayList<String>();
		
		// Definición de servidores -> moverlo a método si crece mucho
		// ------------------------
		servers.put("NABC", new Servidor(Integer.parseInt(config.getProperty("puerto_na")),
	 			config.getProperty("nombre")+": Bien Conocidos", ConsultorNA_NA.class));
		servers.put("CENTRALES", new Servidor(Integer.parseInt(config.getProperty("puerto_nc")),
				config.getProperty("nombre")+": Centrales", ConsultorNA_NC.class));
		servers.put("HOJAS", new Servidor(Integer.parseInt(config.getProperty("puerto_nh")),
				config.getProperty("nombre")+": Hojas", ConsultorNA_NH.class));
		
		for (Servidor server : servers.values()) {
			serverThreads.add(new Thread(server));
		}
		
		// Carga de atributos del NABC
		// ---------------------------
		atributos.setKeepaliveNodoVecino(Integer.parseInt(config.getProperty("keepalive_nodo_vecino")));
		atributos.setDirecciones(config.getProperty("ip"),
				                 Integer.parseInt(config.getProperty("puerto_na")), 
				                 Integer.parseInt(config.getProperty("puerto_nc")), 
				                 Integer.parseInt(config.getProperty("puerto_nh")));
		
		// NABC conocidos inicialmente
		for (Object key : config.keySet()) {
			String clave = (String) key;
			if ((clave).startsWith("wkan_"))
				this.wkanIniciales.add(config.getProperty(clave));
		}
		if (this.wkanIniciales.size() > 0)
			atributos.addNodos(wkanIniciales);
		
		// Inicialización de las colas donde se cargarán las "tareas" 
		atributos.setNombreColas(new String[] {"salida","centrales","interna"});
		atributos.setColas();
		
		// Definición de los "Clientes" -> que consumirán de las colas para enviar mensajes
		// ----------------------------
		// TODO: acá debería definir una política de clientes: voy a necesitar muchos más para comunicar
		//       con WKAN que con Nodos Centrales (y tal vez ni precise para hojas)
		//       Por ahora (2019-09-17) hago 2 para WKAN por 1 para Nodos Centrales
		this.maxClientes = Integer.parseInt(this.config.getProperty("clientes"));
		
		while (this.clients.size() < this.maxClientes) {
			this.clients.put("WKAN-"+Integer.toString(this.clients.size()), new ClienteNA_NA(this.clients.size()));
		
			if (this.clients.size() <= (int) Math.floor(this.maxClientes / 2))
				this.clients.put("NC-"+Integer.toString(this.clients.size()), new ClienteNA_NC(this.clients.size()));
		}
		
		// Voy a tener un único thread dedicado a las tareas internas
		this.clients.put("Interno-1", new WorkerNA_Interno(1));
		
		// Hacer otro bucle para esto no creo que sea lo mejor
		for (String cliente : this.clients.keySet())
			clientThreads.add(new Thread(this.clients.get(cliente)));
		
		// Parámetros "operativos" del nodo
		if (config.containsKey("max_nc"))
			atributos.setMaxNCCapacity(Integer.parseInt(this.config.getProperty("max_nc")));
		
		
		/*Recordatorio
		 * 
		 * Si en el archivo de configuración pongo < nc_conectado_3= >, la propiedad leída
		 * con < config.getProperty("nc_conectado_3") > da "" de resultado.
		 * Si por el contrario no pongo nada la propiedad leída es null
		 * 
		 * Tengo que optar por una porque de lo contrario debería corroborar que la propiedad no sea
		 * ni null ni string: si es un string no pasa nada porque se puede hacer "string vs null" pero
		 * no puedo preguntar si un null es igual a tal string porque sería algo del tipo "null.equals('bla bla')"
		 * lo cual tira null pointer exception.
		 * 
		 * Yo opto por no poner nada en el archivo de configuración pero tengo que comentarlo en el mismo
		 * porque el código Java depende de eso. 
		 * 
		 * */
	}
	
	public void ponerEnMarcha() {
		boolean terminar = false;
		Tarea tarea;
		
		// Inicio de los hilos clientes y servidores
		for (Thread hilo : serverThreads)
			hilo.start();
		
		for (Thread hilo : clientThreads)
			hilo.start();
		
		// Anuncio en la red para darse a conocer
		for (String ip : atributos.getNodos().keySet()) {
			try {
				atributos.encolar("salida", new Tarea(00, "ANUNCIO", ip));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("Terminé de poner en marcha el nodo de acceso");
		
		// Loop donde se administran ciertas tareas que ejecuta periódicamente el nodo
		// Claramente esto es quick and dirty y funca porque hay 2 tarea periódicas, sino la lógica
		// de "Thread.sleep" es inviable
		if (config.getProperty("mandar").equals("si"))
			// TODO: el "mandar" es para debuggeo, para limitar el envío de mensajes y que no sea un lío de paquetes
			// que vienen y van
			while(!terminar) {
				// Dispara tarea que actualiza el estado de los NCs administrados. Es una tarea interna del Nodo
				try {
					Thread.sleep(30000);
					atributos.encolar("interna", new Tarea(00, "CHECK_KEEPALIVE_NCS", null));
					System.out.println("[Core] Disparada tarea periódica: keep alive NCs");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				// Naturalmente podría verificar que haya WKAN para no disparar tareas innecesarias
				try {
					Thread.sleep(30000);
					atributos.encolar("salida", new Tarea(00, "INFORMAR_WKANS", null));
					System.out.println("[Core] Disparada tarea periódica: anuncio WKANs");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
		
		// Si quiero una única instancia
		//servidorHojas.atender();
	}
} // Fin clase 


/** 
 * [2019-10-28]
 * Debería hacer una clase "Cliente" de la que hereden todos los clientes?
 * 
 * 
 * */