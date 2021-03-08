package nodes;
/**
 * Nodo Central del sistema de recuperación de imágenes. Se enlaza con Hojas y otros Nodos Centrales. 
 * Posee un índice con todas las imágenes compartidas por las Hojas conectadas a él, donde registra
 * un vector característico comprimido por cada una. 
 * Recibe consultas, las transmite a aquellas Hojas con imágenes candidatas, a los Nodos Centrales vecinos
 * y envia las respuestas al solicitante.
 * 
 * Implementa la interfaz Runnable a fin de generar un hilo por cada Cliente que se conecta.
 * 
 * La cola de mensajes (ArrayList) es un atributo de la clase (static) a fin de ser compartido por todos los threads.
 * 
 * @author rodrigo
 *
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import commons.Constantes;
import commons.DireccionNodo;
import commons.Tarea;
import nodes.components.AtributosCentral;
import nodes.components.ClienteNC_NA;
import nodes.components.ClienteNC_NC;
import nodes.components.ConsultorNC_H;
import nodes.components.ConsultorNC_NA;
import nodes.components.ConsultorNC_NC;
import nodes.components.Servidor;


public class NodoCentral {
	private AtributosCentral atributos;
	private Properties config;
	
	private ArrayList<Thread> clientThreads;
	private ArrayList<Thread> serverThreads;
	private HashMap<String, Runnable> clients;
	private HashMap<String,Servidor> servers;
	
	
	public NodoCentral(String archivoConfiguracion) throws UnknownHostException {
		try {
			config = new Properties();
			config.load( new FileInputStream(archivoConfiguracion) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		
		// Inicializaciones
		this.clients = new HashMap<String, Runnable>();
		this.servers = new HashMap<String, Servidor>();
		this.clientThreads = new ArrayList<Thread>();
		this.serverThreads = new ArrayList<Thread>();
		
		// Carga de atributos del NC
		atributos = new AtributosCentral();

		// 2020-09-30 Esto puede tirar una excepción UnknownHostException. Por ahora dejo que explote, porque supongo
		// que en un entorno real no cargaría así la IP (ni siquiera sé si cargaría la IP)
		atributos.setDireccion(InetAddress.getByName(config.getProperty("ip")));

		// Punto de acceso a la red
		InetAddress nodoAcceso = InetAddress.getByName(config.getProperty("wkan"));
		atributos.setWKANAsignado(new DireccionNodo(nodoAcceso));
		
		// Cantidad de NC a los que debe conectarse
		atributos.setMaxCentralesVecinos(Integer.parseInt(this.config.getProperty("centrales")));
		
		// Inicialización de las colas donde se cargarán las "tareas" 
		// Podría setear sólo las colas que voy a usar pero dejo las default
		atributos.setNombreColas(new String[]{Constantes.COLA_NA, Constantes.COLA_NC, Constantes.COLA_NH});
		atributos.setColas();
		
		// Servidores
		this.servers.put("hojas", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nh,
				config.getProperty("nombre")+": Hojas",
				ConsultorNC_H.class
		));
		this.servers.put("centrales", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nc,
				config.getProperty("nombre")+": Centrales",
				ConsultorNC_NC.class
		));
		this.servers.put("acceso", new Servidor(
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_na,
				config.getProperty("nombre")+": Acceso",
		        ConsultorNC_NA.class
		));
		
		for (Servidor server : servers.values()) {
			serverThreads.add(new Thread(server));
		}
				
		// Clientes
		// 1 para conectarse al WKAN que sirve de pto de entrada a la red
		// 3 (def. por el archivo de configuración) para interactuar con los NCs a los que se conectará
		while (this.clients.size() < Integer.parseInt(this.config.getProperty("centrales")))
			this.clients.put(
					"NC-" + Integer.toString(this.clients.size()),
					new ClienteNC_NC(this.clients.size(), Constantes.COLA_NC)
			);

		this.clients.put(
				"NC-" + Integer.toString(this.clients.size()),
				new ClienteNC_NA(this.clients.size(), Constantes.COLA_NA)
		);
		
		// Hacer otro bucle para esto no creo que sea lo mejor
		for (String cliente : this.clients.keySet())
			this.clientThreads.add(new Thread(this.clients.get(cliente)));


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
		
		// Inicio de los hilos clientes y servidores
		for (Thread hilo : serverThreads)
			hilo.start();
		
		for (Thread hilo : clientThreads)
			hilo.start();
		
		// Ingreso a la red
		// Nótese que no está hardcodeada la IP del WKAN así que este nodo tranquilamente podría estar concetado
		// a más de un Nodo de Acceso
		try {
			atributos.encolar(Constantes.COLA_NA, new Tarea(00, "ANUNCIO_WKAN", atributos.getWKANAsignado()));
			atributos.encolar(Constantes.COLA_NC, new Tarea(00, "ANUNCIO_WKAN", atributos.getWKANAsignado()));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Terminé de poner en marcha el Nodo Central");
		
		// Loop donde se administran ciertas tareas que ejecuta periódicamente el nodo
		// Claramente esto es una porquería porque el tiempo de espera se va acumulando cuando debería funcionar como un
		// CRONJOB
		while(!terminar) {
			// Tarea que determina si es necesario enviar anuncio a WKAN en caso de que aún no se ingresó a la red
			try {
				Thread.sleep(10000);
				atributos.encolar(Constantes.COLA_NA, new Tarea(00, "CHECK_ANUNCIO", null));
				System.out.println("[Core] Disparada tarea periódica: CHECK ANUNCIO para determinar ingreso a la red");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Hasta acá esperé: 10 segundos ---------------------------------------------------------------------------

			// Dispara tarea de envío de keepalive a WKAN
			try {
				Thread.sleep(TimeUnit.MILLISECONDS.convert(atributos.keepaliveWKAN, TimeUnit.SECONDS));
				atributos.encolar(Constantes.COLA_NA, new Tarea(00, "SEND_KEEPALIVE_WKAN", null));
				System.out.println("[Core] Disparada tarea periódica: keepalive WKAN");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Hasta acá esperé: 30 segundos ---------------------------------------------------------------------------

			// TODO 2020-11-23: revisar y revivir threads caídos
		}
	}
	
	
	/**
	 *	05/02/2018
	 *		Como no existe una conexión permanente entre NC sino que las levanto sólo cuando hace falta, 
	 *		puedo hacer acá (no sé bien en qué parte corresponde, hay que verlo) un bucle donde cada X tiempo 
	 *		se intente establecer conexión con el NC y enviar un mensaje keep alive. Si la conexión se cayó lo 
	 *		que puedo hacer es reemplazar la dirección de ese NC por otro (así el NC actual mantiene la mism
	 *		cantidad de conexiones con otros nodos) o bien eliminarla de los parámetros accesibles, volver a probar
	 *		después de un tiempo y si se arregló, cargalo otra vez como un nodo accesible en los parámetros.
	 *
	 *		La "terminal" creo que sería una buena implementación y rápida 
	 */
	
	
} // Fin clase 


/** 
 * [2019-11-02]
 * Tendría que hacer una clase Nodo de la que hereden todos los nodos porque la estructura básica, independientemente
 * del tipo, es la misma: tiene atributos, servidores y clientes (y tengo clases super de casi todos esos componentes)
 * */

