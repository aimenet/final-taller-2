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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Scanner;



public class NodoCentral /*implements Runnable*/ {
	private Integer id;
	private Properties config;
	private Servidor servidorHojas, servidorCentrales;
	private Thread hiloServidorHojas, hiloServidorCentrales;
	private String[] centralesVecinos;
	
	public NodoCentral(String archivoConfiguracion){
		try {
			config = new Properties();
			config.load( new FileInputStream(archivoConfiguracion) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		
		servidorHojas = new Servidor(Integer.parseInt(config.getProperty("puerto_h")),
				                     config.getProperty("nombre")+": Hojas",
				                     ConsultorNC_H.class);
		servidorCentrales = new Servidor(Integer.parseInt(config.getProperty("puerto_nc")),
										 config.getProperty("nombre")+": Centrales",
										 ConsultorNC_NC.class);
		
		// Carga de atributos del NC
		AtributosCentral atributos = new AtributosCentral();
		
		atributos.setIp( config.getProperty("ip") );
		atributos.setPuertoServidorHojas( Integer.parseInt(config.getProperty("puerto_h")) );
		atributos.setPuertoServidorCentrales( Integer.parseInt(config.getProperty("puerto_nc")) );
		
		// Por ahora está hardcodeado a 3, hacerlo más general
		atributos.indexarCentral( config.getProperty("nc_conectado_1") );
		atributos.indexarCentral( config.getProperty("nc_conectado_2") );
		atributos.indexarCentral( config.getProperty("nc_conectado_3") );
		
		atributos = null;
		
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
		// Si deseo hacer que el servidor corra en un hilo, pudiendo tener de esta manera muchos en paralelo.		
		hiloServidorHojas = new Thread( this.servidorHojas );
		hiloServidorCentrales = new Thread( this.servidorCentrales );
		
		hiloServidorHojas.start();
		hiloServidorCentrales.start();
		
		// Loop donde puedo establecer una "terminal" para verificar el estado del servidor
		/*while(true){
			Scanner teclado = new Scanner(System.in);
			System.out.print("_");
			String eleccion = teclado.nextLine();
			System.out.println(eleccion);
		}*/
		
		// Si quiero una única instancia
		//servidorHojas.atender();
	}
	
	
	/**
	 *	05/02/2018
	 *		Como no existe una conexión permanente entre NC sino que las levanto sólo cuando hace falta, puedo hacer acá (no sé bien
	 *		en que parte corresponde, hay que verlo) un bucle donde cada X tiempo se intente establecer conexión con el NC y enviar un
	 *		mensaje keep alive. Si la conexión se cayó lo que puedo hacer es reemplazar la dirección de ese NC por otro (así el NC actual
	 *		mantiene la misma cantidad de conexiones con otros nodos) o bien eliminarla de los parámetros accesibles, volver a probar después
	 *		de un tiempo y si se arregló, cargalo otra vez como un nodo accesible en los parámetros.
	 *
	 *		La "terminal" creo que sería una buena implementación y rápida 
	 */
	
	
} // Fin clase 
