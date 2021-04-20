package nodes.components.servidores;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import commons.*;
import commons.mensajes.Mensaje;
import commons.structs.nc.NHIndexada;
import nodes.components.atributos.AtributosCentral;

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
	// Atributos
	// -----------------------------------------------------------------------------------------------------------------
	private AtributosCentral atributos = new AtributosCentral();
	private ObjectInputStream buffEntrada;
	private ObjectOutputStream buffSalida;
	private Socket sock;

	// Métodos Propios
	// -----------------------------------------------------------------------------------------------------------------

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
	private boolean recibirConsulta(ObjectInputStream entrada, ObjectOutputStream salida, Mensaje msj){
		CredImagen modelo;
		Integer codigoConsultaEncolada;
		
		
		// Se evalúa si la consulta no fue recibida previamente (en una determinada ventana de tiempo). En caso
		// contrario se procesa la (retransmisión de) consulta
		codigoConsultaEncolada = atributos.consultaDuplicada(msj); // TODO: revisar
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
				// Acá nunca debería llegar
				System.out.println("<ConsultorNC_NC> No tengo una consulta igual (no la encolé)");
				break;
		}
		
		// Imagen modelo (query)
		modelo = (CredImagen) msj.getCarga();

		// Pueden usarse varios criterios para paralelizar: un hilo por núcleo del CPU, uno por hoja conectada
		//(el elegido), un número empírico fijo, etc
		int cantHilos = atributos.getTamanioIndiceImagenes();;
		CountDownLatch latch = new CountDownLatch(cantHilos);
		HashConcurrente similares = new HashConcurrente();

		UUID[] hojasConImagenes =  (UUID[]) atributos.getClavesIndiceImagenes();

		for(int i=0; i<cantHilos; i++){
			DireccionNodo destino = ((NHIndexada) atributos.getHoja(hojasConImagenes[i])).getDireccion();
			
			ConsultorNC_Worker trabajador = new ConsultorNC_Worker(
					hojasConImagenes[i],
					similares,
					modelo,
					atributos.getImagenes(hojasConImagenes[i]),
					latch,
					destino,
					1,
					"Hoja"
			);

			new Thread( trabajador ).start();
			
			System.out.println("<ConcultorNC_NC.java> Worker consultando a Hoja " + destino);
		}
		
		// Si el TTL lo permite retransmite el mensaje a los NC a los que está conectado
		if(msj.getTTL() > 0){
			cantHilos =  atributos.getCentrales().size();
			ArrayList<DireccionNodo> centrales = atributos.getNcs();
			DireccionNodo direccionNodoActual = atributos.getDireccion();
			CountDownLatch latchCentrales = new CountDownLatch(cantHilos);

			for(int i=0; i<cantHilos; i++){
				// TODO 2020-11-06: debería ser random el NC elegido, para balancear carga
				DireccionNodo destino = centrales.get(i);
				
				System.out.println(
						String.format(
								"%s vs null vs %s vs %s",
								destino,msj.getNCEmisor().ip.getHostName(),
								msj.getOrigen().ip.getHostName()
						)
				);
				
				if ((destino != null) && (!destino.equals(msj.getNCEmisor())) && (!destino.equals(msj.getOrigen()))) {
					ConsultorNC_Worker trabajador = new ConsultorNC_Worker(
							UUID.randomUUID(),
							similares,
							modelo,
							new ArrayList<CredImagen>(),
							latchCentrales,
							destino,
							3,
							"Central",
							msj.getOrigen(),
							direccionNodoActual,
							msj.recepcionRta());
				
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
			latch.await();
		} catch (InterruptedException e1) {
			error = true;
		}

		HashMap<DireccionNodo, CredImagen[]> resultado = new HashMap<DireccionNodo, CredImagen[]>();
		if(error){
			System.out.println("<ConsultorNC_NC.java> OJO!! hubo un error");
			return false;
		} else {
			resultado = similares.getHash();
		}

		// Envío de respuesta a la Hoja solicitante
		try {
			// Conexión tmp con Hoja solicitante
			ConexionTcp conexionTmp = new ConexionTcp(
					msj.recepcionRta().ip.getHostAddress(),
					msj.recepcionRta().puerto_nh
			);

			// Podría agregar la dirección del NC para que la H sepa de donde vino la rta.
			Tupla2<CredImagen, HashMap<DireccionNodo, CredImagen[]>> respuesta;
			respuesta = new Tupla2<CredImagen, HashMap<DireccionNodo, CredImagen[]>>(modelo,resultado);

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

	/**
	 *
	 * @param nodo: DireccionNodo
	 */
	private void recibirSaludo(DireccionNodo nodo) {
		// TODO: [2019-11-30] por ahora acepto todos (yo soy su vecino, no ellos el mío) pero evaluar una
		// lógica para controlarlo

		// TODO 2020-10-20: antes le pasaba la direccion del NC que mandó el saludo, como emisor del mensaje. Ahora le
		// pasé al constructor la dirección de este nodo. Revisarlo por las dudas
		try {
			this.buffSalida.writeObject(new Mensaje(atributos.getDireccion(), Codigos.OK, null));
		} catch (IOException e) {
			e.printStackTrace();
			// Nótese que básicamente si falla no hago nada. Será responsabilidad del NC que se comunicó el reenvío
			// del saludo
		}
		System.out.printf("[Con NC] aceptado saludo de nuevo NC (%s) en la red [OK]\n", nodo.ip.getHostName());
	}


	// Métodos de Interfaces
	// -----------------------------------------------------------------------------------------------------------------
	@Override
	public void atender() {
		Integer emisor;
		Mensaje mensaje;
		boolean terminar = false;
		String auxStr;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
				case Codigos.NC_NC_POST_SALUDO:
					// Un NC solicita que éste sea su vecino
					this.recibirSaludo(mensaje.getEmisor());
					break;
				case Codigos.NC_NC_POST_CONSULTA:
					if(!recibirConsulta(buffEntrada,buffSalida,mensaje))
						terminar=true;

					break;
				//case TERMINAR:
				case Codigos.CONNECTION_END:
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
