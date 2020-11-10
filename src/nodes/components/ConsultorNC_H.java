package nodes.components;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import org.apache.commons.validator.routines.InetAddressValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import commons.*;

/**
 * Consultor que corre en cada uno de los hilos generado por el Servidor (dedicado a atender HOJAS) 
 * de los Nodos Centrales.
 * 
 * El <índice de Hojas> guarda por cada ID un string del tipo:
 * "ip_cliente:puerto_cliente;ip_servidor:puerto_servidor" siendo la primer parte (separada por ";"
 * la correspondiente al socket conectado actualmente al ConsultorNC. La segunda corresponde al server socket
 * donde la Hoja atiende consultas.
 * 
 * @author rodrigo
 *
 */
public class ConsultorNC_H implements Consultor {

	// Atributos
	// -----------------------------------------------------------------------------------------------------------------
	// De cada instancia
	private Socket sock;
	
	// Compartidos
	AtributosCentral atributos = new AtributosCentral();
	

	// Métodos Propios
	// -----------------------------------------------------------------------------------------------------------------
	//public ConsultorNC_H(){}
	
	
	private boolean recibirConsulta(ObjectInputStream entrada, ObjectOutputStream salida,
			Mensaje msj){
		CredImagen modelo;
		HashMap<String, CredImagen[]> resultado;
		String direccionNodoActual = null, direccionRta=null;
		Tupla2<CredImagen, HashMap<String, CredImagen[]>> respuesta = null;

		/*System.out.println("¿Qué pasha clarin, estás nerviosho?");
		System.out.println("NC 1: " + atributos.getCentral(0));
		System.out.println("Dir cen: " +  atributos.getDireccion());*/
		
		// Imagen modelo (query)
		modelo = (CredImagen) msj.getCarga();

		System.out.println("\tRecibida consulta de: " + sock.getInetAddress().toString() + 
				":" + sock.getPort());
		System.out.println("\t-> " + modelo.getNombre());

		// Pueden usarse varios criterios para paralelizar: un hilo por núcleo del CPU, uno por hoja conectada
		//(el elegido), un número empírico fijo, etc
		int cantHilos =  atributos.getTamanioIndiceImagenes();
		CountDownLatch latchHojas = new CountDownLatch(cantHilos);
		HashConcurrente similares = new HashConcurrente();

		/*Primera versión
		Integer[] hojasConImagenes =  atributos.getClavesIndiceImagenes();
		System.out.println("Checkpoint 0");
		for(int i=0; i<cantHilos; i++){
			String destino = atributos.getHoja(hojasConImagenes[i]).split(";")[1];
			Worker trabajador = new Worker(i, similares, modelo, atributos.getImagenes(hojasConImagenes[i]),
					latchHojas, destino.split(":")[0], destino.split(":")[1], 1, "Hoja");
			
			new Thread( trabajador ).start();
			
			System.out.println("<ConcultorNC_H.java> Worker consultando a Hoja " + destino);
		}*/
		
		// Segunda versión (se puede hacer más eficiente o al menos más prolijo buscando las hojas finales
		// para no estar decrementando el latch y demás)
		String[] hojasConImagenes =  atributos.getClavesIndiceImagenes();
		for(String clave : hojasConImagenes) {
			if(clave != msj.getEmisor()) {
				String destino = atributos.getHoja(clave);
				Worker trabajador = new Worker(clave, similares, modelo, atributos.getImagenes(clave),
						latchHojas, destino.split(":")[0], destino.split(":")[1], 1, "Hoja");
				new Thread( trabajador ).start();
				System.out.println("<ConcultorNC_H.java> Worker consultando a Hoja " + destino);
			} else {
				System.out.println("A la Hoja " + clave + " no le envío");
				latchHojas.countDown();
			}
		}
		
		
		System.out.println("Listas las Hojas, vamos por los Centrales");
		
		// Acá tengo que hacer la consulta a los NC -> sólo si el TTL del mensaje lo permite
		// La respuesta del servior debe ser algo así: HashMap<String, String[]>
		cantHilos =  atributos.getCentrales().size();
		direccionNodoActual = atributos.getDireccion("centrales");
		direccionRta = msj.recepcionRta();
		CountDownLatch latchCentrales = new CountDownLatch(cantHilos);
		
		for(int i=0; i<cantHilos; i++){
			String destino = atributos.getCentral(i);
			
			if (destino != null){
				Worker trabajador = new Worker(Integer.toString(i), similares, modelo, new ArrayList<CredImagen>(),
						latchCentrales, destino.split(":")[0], destino.split(":")[1], 3, "Central",
						direccionNodoActual, direccionNodoActual, direccionRta);
			
				new Thread( trabajador ).start();
				
				System.out.println("<ConcultorNC_H.java> Worker consultando a Nodo Central " + destino);
			} else {
				// TODO: esto es provisorio, tengo que hacer bien la cuenta de nodos centrales conectados (ignorando los null, cosa
				//       que ahora no hago)
				latchCentrales.countDown();
			}
		}
		
		// Barrera donde se espera por la respuesta de todas las hojas
		boolean error = false;
		try {
			latchHojas.await();
			//latchCentrales.await();
		} catch (InterruptedException e1) {
			error = true;
		}
		
		if(error){
			// resultado = new HashMap<String, CredImagen[]>(); // Devuelve diccionario vacío si hubo algún error
			System.out.println("<ConsultorNC_H.java> OJO!! hubo un error");
			return false;
		} else {
			resultado = similares.getHash();
		}

		// Envío de respuesta a la Hoja solicitante (sólo si hay imágenes similares)
		try {
			//salida.writeObject(new Mensaje(null,4,resultado));
			// Conexión tmp con Hoja solicitante
			ConexionTcp conexionTmp = new ConexionTcp(msj.recepcionRta().split(":")[0],
													  Integer.parseInt(msj.recepcionRta().split(":")[1]));
			
			/*La respuesta es una tupla donde el primer elemento es la imagen que la Hoja pasó como modelo
			 * y el segundo un diccionario. Las claves son las direcciones de las H que poseen imágenes
			 * similares y el value son las imágenes similares*/
			respuesta = new Tupla2<CredImagen, HashMap<String, CredImagen[]>>(modelo,resultado);
			conexionTmp.enviarSinRta((new Mensaje(null,11,respuesta)));
			conexionTmp.cerrar();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	/**
	 * Método por el cual el Consultor recibe el listado de imágenes que una Hoja desea compartir
	 * @param entrada
	 * @param salida
	 * @param msj
	 * @return
	 */
	private boolean recibirListado(ObjectInputStream entrada, ObjectOutputStream salida,
			Mensaje msj){		
		Integer cantidad, emisor, resultado;

		resultado = 0;

		// Obtiene de la solicitud la cantidad de imágenes a recibir
		cantidad = (Integer) msj.getCarga();
		
		//Acá debería mínimamente procesar las imágenes en caso de que haya alguna cuota por hoja
		//u otro factor que pudiera hacer que el servidor no acepte todas las imágenes compartidas.
		//Por ahora, asumo que se pueden indexar todas las imágenes.
		
		try {
			msj = (Mensaje) entrada.readObject();
		} catch (ClassNotFoundException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Carga las credenciales de las imágenes en el índice.
		atributos.indexarImagenes(msj.getEmisor(), (ArrayList<CredImagen>) msj.getCarga());
		System.out.println("\tIndexadas imágenes de: " + sockToString());

		try {
			salida.writeObject(new Mensaje(null, Codigos.OK,resultado));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String clave:atributos.getClavesIndiceImagenes() ){
			System.out.println(clave + ": " + atributos.getImagenes(clave));
		}
		return true;
	}
	
	
	/**
	 * Método por el cual el Consultor (servidor)  atiende por primera vez a un Cliente (Nodo Hoja).
	 * Recibe un saludo y le responde con otro, más un mensaje con el ID asignado
	 * @param entrada
	 * @param salida
	 * @param direccionServer
	 * @return
	 */
	private boolean saludo(ObjectInputStream entrada, ObjectOutputStream salida, String direccionServer){
		Mensaje mensaje;
		String idAsignado = null;
		UUID token = null;
		
		try {
			// direccionServer puede ser una dirección IP (del servidor de la H) o el ID (UUID propiamente dicho)
			// que identifica a la HOJA pues ya estuvo conectada al nodo y se envía a modo de validación
			// TODO: marcar H como activa nuevamente

			if (InetAddressValidator.getInstance().isValid(direccionServer.split(":")[0])) {
				// Controla (defensivamente) la existencia del NH entre los conocidos previamente
				idAsignado = atributos.getIDHoja(direccionServer);
				if (idAsignado == null)
					idAsignado = atributos.generarToken();
			} else {
				// se recibió un token -> saludo de reconexión
				token = UUID.fromString(direccionServer);

				if (this.atributos.getHoja(token.toString()) == null || this.atributos.getHoja(token.toString()).length() == 0 ) {
					// No es un ID válido por lo que se genera uno nuevo
					idAsignado = atributos.generarToken();
				} else {
					idAsignado = token.toString();

					// Obtengo la dirección del servidor de la H pues al ser reconexión ya se conoce
					direccionServer = this.atributos.getHoja(token.toString()).split(";")[1];
				}
			}

			//¿El servidor debería tener un ID, un Mensaje propio sin campo emisor o pongo null como ahora?
			salida.writeObject(new Mensaje(null,1, idAsignado.toString()));
			System.out.println("-> Asignado ID " +  idAsignado + " a cliente " + sockToString());
			
			//Guardado del par ID<->Nodo Hoja (atributo de clase, debe sincronizarse)
			atributos.indexarHoja(idAsignado, direccionServer);
						
			return true;
		} catch (IOException e){
			//IOException -> por escritura en buffer salida.
		    //ClassNotFoundException -> por lectura en buffer de entrada.
			System.out.println(e.toString());
			return false;
		}
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
				case 1:
					if (!saludo(buffEntrada, buffSalida, (String) mensaje.getCarga())) {terminar=true;}
					break;
				// case 2 podría ser saludo para reconexion
				case 3:
					if(!recibirListado(buffEntrada,buffSalida,mensaje)) {
						terminar=true;
						//Borrar
						/*ArrayList<CredImagen> listado = this.indiceImagenes.get(mensaje.getEmisor());
						for(int i=0;i<listado.size();i++) {
							System.out.println(mensaje.getEmisor() + ": " + listado.get(i));
						}*/
					}
					break;
				case 4:
					if(!recibirConsulta(buffEntrada,buffSalida,mensaje)) {
						terminar=true;
					}
					break;
				//case TERMINAR:
				case 0:
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
		return String.format("<%s:%d>", sock.getInetAddress().getHostAddress(), sock.getPort());
	}


} // Fin clase
