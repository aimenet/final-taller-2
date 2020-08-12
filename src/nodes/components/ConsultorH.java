package nodes.components;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import commons.*;

/**
 * Clase que se instancia en cada uno de los hilos generados por el Servidor de un nodo hoja para atender
 * consultas de conexiones entrantes (H solicitando descarga, NC consultando,
 * NC enviando rta a consulta realizada)
 * @author rodrigo
 *
 */
public class ConsultorH implements Consultor{
	/* --------- */
	/* Atributos */
	/* --------- */
	private AtributosHoja variables;
	ObjectOutputStream buffSalida;
	ObjectInputStream buffEntrada;
	private Socket sock;
	
	private static final Integer INTENTOS_TX_IMG = 3;
	
	

	/* --------------- */
	/* Métodos propios */
	/* --------------- */
	public ConsultorH(){
		//Instanciación de los atributos del Nodo Hoja
		variables = new AtributosHoja();
	}


	public boolean consultaNodoCentral(Mensaje msj){
		ArrayList<CredImagen> similares;
		CredImagen referencia;
		ArrayList<CredImagen> candidatas;
		
		// Imagen recibida como referencia
		referencia = (CredImagen) msj.getCarga();
		
		// Recepción de posibles imágenes similares a la dada como referencia
		try {msj = (Mensaje) buffEntrada.readObject();}
		catch (ClassNotFoundException | IOException e) {return false;}
		candidatas = (ArrayList<CredImagen>) msj.getCarga();
		
		System.out.println("<ConsultorH.java> Reicbí consulta del NC");
		
		// Evaluación de imágenes similares
		similares = new ArrayList<CredImagen>();
		for(CredImagen cred : candidatas){
			int[] imagen = variables.getImagen(cred.getNombre()).getVectorCaracteristico();
			float distancia = referencia.comparacion(imagen);
			if(distancia < 10000){
				similares.add(cred);
			}
			System.out.println(cred.getNombre() + " vs " + referencia.getNombre() + ": " + distancia);
		}
		
		try {
			buffSalida.writeObject(new Mensaje(null,10,similares));
			buffSalida.writeObject(new Mensaje(null,10,variables.getDireccion("hojas")));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/** Método donde se reciba la respuesta (de un NC) a una consulta realizada. Se encola la misma
	 * para su futuro procesamiento. 
	 * La carga del mensaje es una tupla de dos elementos: 1) la imagen dada como referencia por la H solicitante
	 * 2) un diccionario. key->dirección de la H con imágenes similares, value->las imágene similares 
	 * 
	 * El almacenamiento de las respuestas lo hago en un diccionario de diccionarios:
	 * 		La "clave del 1er diccionario" es el ID de imágen acá en la H (su nombre). El value es naturalmente otro diccionario
	 * 		La "clave del 2do diccionario" es la dirección de otro Nodo H. El value es el listado de imágenes
	 * 		similares a la "clave de 1er nivel"
	 * 
	 *  	Voy a hacer (al menos por ahora) que en esta estructura se guarden las imágenes similares que posee una
	 *		Hoja según lo indicado en la última rta. De esta manera si cuando llega la rta ya tenía un listado de esa
	 *		misma H, lo sobreescribo con la información nueva porque es la más actual.
	 * 
	 * */
	public boolean respuestaNodoCentral(Mensaje msj){
		HashMap<String, CredImagen[]> similares;
		CredImagen referencia;
		Tupla2<CredImagen,HashMap<String, CredImagen[]>> rta;
		
		// Obtención de imágenes similares recibidas como respuesta
		rta = (Tupla2<CredImagen,HashMap<String, CredImagen[]>>) msj.getCarga();
		referencia = rta.getPrimero();
		similares = rta.getSegundo();
		
		// TODO: tengo que probar que las rtas se almacenen bien (27/02/2018)
		
		if(similares.isEmpty()){
			System.out.println("<ConsultorH.java> Reicbí respuesta de NC pero estaba vacía");
		} else {
			System.out.println("<ConsultorH.java> Reicbí respuesta de NC. Voy a encolar");
			variables.almacenarRta(referencia,similares);
			System.out.println("<ConsultorH.java> Listo el encolado");
		}
		
		return true;
	}

	// TODO: La solicitud de descarga debería hacerse mediante una CredImagen y no por el nombre de la img a
	// descargar porque nada garantiza que este haya cambiado o que se le haya asignado a otra imagen
	// totalmente distinta
	public boolean enviarImagen(Mensaje msj){
		boolean exito = false;
		ConexionTcp conexionTmp;
		CredImagen solicitada;
		Imagen img;
		Integer descargada, intentos;
		Mensaje respuesta;
		String direccionRta, enviada;


		// 
		solicitada = (CredImagen) msj.getCarga();
		direccionRta = msj.recepcionRta();
		
		System.out.println("\n\tRecibida consulta de: " + sock.getInetAddress().toString() + 
				":" + sock.getPort());
		System.out.println("\t-> " + solicitada.getNombre());
		
		img = variables.getImagen(solicitada.getNombre());
		
		try {
			conexionTmp = new ConexionTcp(direccionRta.split(":")[0], Integer.parseInt(direccionRta.split(":")[1]));
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el Nodo Hoja para envío de imagen solicitada");
			return false;
		}
		
		// TODO: Imagen tiene que ser serializable sino estoy al horno (01/03/2018)
		
		// Envío de imagen solicitada. Intentará un número fijo de veces, en caso de que ocurra un problema en la H
		// destino que le impidiera encolar la descarga
		intentos = 0;
		while(intentos < INTENTOS_TX_IMG){
			//respuesta = (Mensaje) conexionTmp.enviarConRta(new Mensaje(0,22,img.getNombre()));
			respuesta = (Mensaje) conexionTmp.enviarConRta(new Mensaje("0",22,img));
			descargada = (Integer) respuesta.getCarga();
			
			if(descargada==0){
				System.out.println("Enviada imagen " +img.getNombre()+" a "+direccionRta+" con éxito");
				exito = true;
				break;
			} else {
				intentos++;
				System.out.println("Intentos de envío de " +img.getNombre()+ " a " +direccionRta+ ": " +intentos);
			}
		}
		
		if(!exito){
			System.out.println("Abortado envío de " +img.getNombre()+ " a " +direccionRta+ ": agotado timeout");
		}
		
		return true;
	}

	public boolean recibirImagen(Mensaje msj){
		boolean exito = false;
		Imagen descargada = null;
		Integer intentos;
		String direccionRta, enviada, solicitada;


		// TODO: por ahora es una mentira esto de los intentos de descarga
		intentos = 0;
		while(intentos < INTENTOS_TX_IMG){
			descargada = (Imagen) msj.getCarga();
			variables.almacenarDescarga(descargada);
			System.out.println("Descargando " +descargada.getNombre()+ ": " +intentos+ " intentos");
			intentos++;
			
			try { buffSalida.writeObject( new Mensaje(null,22,0) ); }// <0> indica que la descarga fue exitosa  
			catch (IOException e) { e.printStackTrace(); } 
			
			exito = true;
			break;
		}
		
		if(!exito){
			System.out.println("Abortada descarga : agotado timeout");
		} else {
			System.out.println("Descarga de " +descargada.getNombre()+ " completada");
		}
		
		return true;
	}
	
	private void recibirDirNC(Mensaje msj) {
		HashMap<String, Object> payload;
		Integer amount;

		// Si no necesita NC igual podría guardarlo como un "suplente"
		amount = ((AtributosHoja) this.variables).getCantCentrales() ;
		amount -= ((AtributosHoja) this.variables).getCentrales().size();

		if (amount > 0) {
			((AtributosHoja) variables).encolarCentral((String) msj.getCarga(), null);

			payload = new HashMap<String, Object>();
			payload.put("direccionNC", (String) msj.getCarga());

			try {
				variables.encolar("salida", new Tarea("ESTABLECER_CONEXION_NC", payload));
			} catch (InterruptedException e) {
				// No hago nada, hay una tarea periódica que solicia NCs si resta conectarse a alguno
			}
		}
	}

	
	/* ------------------------- */
	/* Implementaciones interfaz */
	/* ------------------------- */
	@Override
	public void atender() {
		Mensaje mensaje;
		boolean terminar = false;

		// TODO: pensar si una Hoja no debería atender una única consulta y cerrar la conexión con el Cliente.
		// TODO: en cada CASE debería ver bien como manejar los errores para que no haya loops infinitos
		try {
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
				// Consultas desde un Nodo Central
				case 10:
					consultaNodoCentral(mensaje);
					terminar = true;
					break;
				
				// Rta (de NC) a consulta realizada
				case 11:
					System.out.println("<ConsultorH> Rta (de NC) a consulta realizada");
					respuestaNodoCentral(mensaje);
					terminar = true;
					break;

				// TODO: ver si mantengo el cierre de la conexión después de cada intercambio o lo dejo a criterio del usuario.
				// Consultas desde un Nodo Hoja.
				case 21:
					// Solicitud de descarga
					enviarImagen(mensaje);
					terminar=true;
					break;
				case 22:
					// Recepción de imagen descargada
					recibirImagen(mensaje);
					terminar=true;
					break;
				case Codigos.NC_NH_POST_ANUNCIO:
					// Un NC anuncia su dirección pues tiene capacidad de recibir NHs

					recibirDirNC(mensaje);

					break;
				case 20:
					// Cierre de conexión
					terminar = true;
					break;
				default:
					System.out.println("\tRecibí: " + mensaje.getCarga());
					break;
				}
			}
			
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
			sock.close();
		} catch (IOException | ClassNotFoundException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
		}
		
	}
	

	@Override
	public void run() {
		atender();	
	}
	
	
	@Override
	public void setSock(Socket sock) {
		this.sock = sock;
		
		// Instanciación de los manejadores del buffer.
		// TODO: capturar este error
		try {
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();	
		}
	}

	
	@Override
	public String sockToString() {
		return String.format("<%s:%d>", sock.getInetAddress(), sock.getPort());
	}
}
