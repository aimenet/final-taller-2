package nodes.components;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import commons.ConexionTcp;
import commons.CredImagen;
import commons.Mensaje;
import commons.Tupla2;

/**
 * Consultor que corre en cada uno de los hilos generado por el Servidor de los Nodos Centrales. Es el
 * encargado de la atención propiamente dicha de Nodos Centrales.
 * 
 *  Se decidió implementar como un servidor independiente al de Hojas pues de esta manera podrá monitorearse
 *  mejor.
 * 
 * El <índice de Nodos Centrales> registra para cada Nodo Central conocido el par "ip:puerto".
 * 
 * @author rodrigo
 *
 */
public class ConsultorNC_NC implements Consultor {
	/* --------- */
	/* Atributos */
	/* --------- */
	// De cada instancia
	private Socket sock;
	private AtributosCentral atributos = new AtributosCentral();
	
	// De la clase
	
	
	/* --------------- */
	/* Métodos Propios */
	/* --------------- */
	//public ConsultorNC_NC(){}


	/**
	 * Método en el que se recibe de otro Nodo Central una retransmisión de consulta, se solicitan imágenes
	 * similares a las Hojas conectadas y se envía la respuesta al Nodo consultor.
	 * 
	 * Nótese que la búsqueda en las Hojas indexadas es idéntica a la que hace el ConsultorNC_H, por lo que
	 * tengo código duplicado. Debería ver la forma de unificar ambos métodos o bien acá instanciar un
	 * ConsultorNC_H 
	 * 
	 * @param entrada
	 * @param salida
	 * @param msj
	 * @return
	 */
	private boolean recibirConsulta(ObjectInputStream entrada, ObjectOutputStream salida,
			Mensaje msj){
		CredImagen modelo;
		CountDownLatch latch=null, latchCentrales=null;
		HashMap<String, CredImagen[]> resultado;
		Integer codigoConsultaEncolada;
		String direccionNodoActual;
		Tupla2<CredImagen, HashMap<String, CredImagen[]>> respuesta;
		
		
		// Se evalúa si la consulta no fue recibida previamente (en una determinada ventana de tiempo). En caso
		// contrario se procesa la (retransmisión de) consulta
		codigoConsultaEncolada = atributos.consultaDuplicada(msj);
		switch(codigoConsultaEncolada) {
			case 0:
				System.out.println("<ConsultorNC_NC> Ya tengo una consulta igual encolada");
				return true;
			case 1:
				System.out.println("<ConsultorNC_NC> Ya tengo una consulta igual encolada pero no está vigente");
				break;
			case 2:
				System.out.println("<ConsultorNC_NC> No tengo una consulta igual (la encolé)");
				break;
			default:
				System.out.println("<ConsultorNC_NC> No tengo una consulta igual (no la encolé)"); // Acá nunca debería llegar
				break;
		}
		
		// Imagen modelo (query)
		modelo = (CredImagen) msj.getCarga();
		
		/*System.out.println("\tRecibida consulta de: " + sock.getInetAddress().toString() + 
				":" + sock.getPort());
		System.out.println("\t-> " + modelo.getNombre());
		
		System.out.println("Enviar rta a Hoja " + msj.recepcionRta());*/
		
		// Pueden usarse varios criterios para paralelizar: un hilo por núcleo del CPU, uno por hoja conectada
		//(el elegido), un número empírico fijo, etc
		int cantHilos = atributos.getTamanioIndiceImagenes();;
		latch = new CountDownLatch(cantHilos);
		HashConcurrente similares = new HashConcurrente();
		
		// TODO: antes de buscar tengo que verificar que no me haya llegado la misma consulta desde otro lado
		
		String[] hojasConImagenes =  (String[]) atributos.getClavesIndiceImagenes();
		for(int i=0; i<cantHilos; i++){
			String destino = atributos.getHoja(hojasConImagenes[i]).split(";")[1];
			
			Worker trabajador = new Worker(hojasConImagenes[i], similares, modelo,
					atributos.getImagenes(hojasConImagenes[i]), latch,
					destino.split(":")[0], destino.split(":")[1],1,"Hoja");
			new Thread( trabajador ).start();
			
			System.out.println("<ConcultorNC_NC.java> Worker consultando a Hoja " + destino);
		}
		
		// Si el TTL lo permite retransmite el mensaje a los NC a los que está conectado
		if(msj.getTTL() > 0){
			cantHilos =  atributos.getCentrales().size();
			direccionNodoActual = atributos.getDireccion();
			latchCentrales = new CountDownLatch(cantHilos);
			for(int i=0; i<cantHilos; i++){
				String destino = atributos.getCentral(i);
				
				System.out.println(String.format("%s vs null vs %s vs %s", destino,msj.getNCEmisor(),msj.getOrigen()));
				
				if ( (destino != null) && (!destino.equals(msj.getNCEmisor())) && (!destino.equals(msj.getOrigen())) ) {
					Worker trabajador = new Worker(Integer.toString(i), similares, modelo,
							new ArrayList<CredImagen>(), latchCentrales,
							destino.split(":")[0], destino.split(":")[1], 3, "Central",
							(String)msj.getOrigen(), direccionNodoActual, msj.recepcionRta());
				
					new Thread( trabajador ).start();
					
					System.out.println("<ConcultorNC_NC.java> Reenviada consulta a Nodo Central " + destino);
				} else {
					// TODO: esto es provisorio, tengo que hacer bien la cuenta de nodos centrales conectados (ignorando los null, cosa
					//       que ahora no hago)
					latchCentrales.countDown();
					System.out.println("No mando a NC");
				}
			}

		}
		
		
		// Barrera donde se espera por la respuesta de todas las hojas
		boolean error = false;
		try {
			//if(latchCentrales != null)
			//	latchCentrales.await();
			latch.await();
		} catch (InterruptedException e1) {
			error = true;
		}
		
		if(error){
			System.out.println("<ConsultorNC_NC.java> OJO!! hubo un error");
			return false;
		} else {
			resultado = similares.getHash();
		}

		// Envío de respuesta a la Hoja solicitante
		/*try {
			salida.writeObject(new Mensaje(null,31,resultado));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		// Envío de respuesta a la Hoja solicitante
		try {
			//salida.writeObject(new Mensaje(null,4,resultado));
			// Conexión tmp con Hoja solicitante
			ConexionTcp conexionTmp = new ConexionTcp(msj.recepcionRta().split(":")[0],
													  Integer.parseInt(msj.recepcionRta().split(":")[1]));
			// Podría agregar la dirección del NC para que la H sepa de donde vino la rta.
			respuesta = new Tupla2<CredImagen, HashMap<String, CredImagen[]>>(modelo,resultado);
			conexionTmp.enviarSinRta((new Mensaje(null,11,respuesta)));
			conexionTmp.cerrar();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Se registra la consulta recibida en caso de que no hubiera una igual encolada
		if ( codigoConsultaEncolada==3 ) {
			System.out.println("\tEncolando...");
			atributos.encolarConsulta(msj);
		}
		
		System.out.println("<ConsultorNC_NC> Así quedó la cola de últimas consultas");
		for(int i=0; i<atributos.getUltimasConsultas()[0].length; i++){
			if( atributos.getUltimasConsultas()[0][i] != null) {
				Timestamp timestamp = (Timestamp) atributos.getUltimasConsultas()[1][i];
				System.out.println( i+": " + atributos.getUltimasConsultas()[0][i] + " ("+timestamp.toString()+")");
			}
		}
		
		return true;
	}
	
	
	/* --------------------- */
	/* Métodos de Interfaces */
	/* --------------------- */
	@Override
	public void atender() {
		Integer emisor;
		Mensaje mensaje;
		boolean terminar = false;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
				case 30:
					if(!recibirConsulta(buffEntrada,buffSalida,mensaje)) {
						terminar=true;
					}
					break;
				//case TERMINAR:
				case 333:
					//El paquete indicaba el cierre de la conexión.
					terminar = true;
					break;
				default:
					System.out.println("\tRecibí: " + mensaje.getCarga());
					break;
				}
			}
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
		}
		
	}
	

	@Override
	public void run() {
		this.atender();
		
	}


	@Override
	public void setSock(Socket sock) {
		this.sock = sock;
		
		// ¿Será este el problema?
		
		String origen = sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort();
		String destino = sock.getInetAddress().getHostName() + ":" + sock.getPort();
		System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
	}


} // Fin clase



/* --------------------------------------------------------- */
/* Clases auxiliares (debería moverlas a archivos separados) */
/* --------------------------------------------------------- */

