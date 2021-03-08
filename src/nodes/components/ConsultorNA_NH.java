package nodes.components;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;
import commons.DireccionNodo;

public class ConsultorNA_NH implements Consultor {
	// De cada instancia
	private AtributosAcceso atributos = new AtributosAcceso();
	private ObjectInputStream buffEntrada;
	private ObjectOutputStream buffSalida;
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();


	// Atención de requests
	// -----------------------------------------------------------------------------------------------------------------
	private HashMap<String, Object> solicitudNCsFnc(
			DireccionNodo nodoHoja,
			Integer solicitados,
			HashSet<DireccionNodo> excepciones) {
		/**
		 * Un NH que acaba de ingresar a la red solicita NCs a los que conectarse.
		 *
		 * nodoHoja: el NH que solitita NCs a los que conectarse
		 * solicitados: la cantidad de NCs que necesita
		 * excepciones: conjunto de NCs que el NH ya conoce y se deben ignorar
		 *
		 */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		System.out.printf("[Con NH] Solicitud de %s NCs por parte de NH en %s ", solicitados, nodoHoja.ip.getHostName());

		// Obtrención de NCs que pueden recibir a la H: si no existen más WKANs en la red entonces buscará entre sus
		// NCs la cantidad solicitada, sino escogerá sólo 1 (y retransmitirá la consulta)
		Integer cantidad = ((AtributosAcceso) atributos).getNodos().size() > 0 ? 1 : solicitados;

		// "Trae" el doble de lo requerido para aumentar las probabilidades de encontar un NC que no tenga ya al NH
		List<DireccionNodo> candidatos = funciones.getNCsConCapacidadNH(
				solicitados * 2,
				excepciones);

		// Nótese que este nodo puede no manejar tantos NCs como los solicitados, pero si no existen otros WKANs en la
		// red, estos serán los únicos NCs existentes por lo que no podrá informar más que dicha cantidad (obviamente)
		List<DireccionNodo> elegidos = candidatos.subList(
				0,
				candidatos.size() >= cantidad ? cantidad : candidatos.size()
		);

		try {
			buffSalida.writeObject(new Mensaje(atributos.getDireccion(), Codigos.OK, elegidos));
		} catch (IOException e) {
			// No hago nada, el NH seguirá sin concer NCs y volverá a enviar la solicitud
			System.out.println("[ERROR en envío de mensaje a NH]");
			output.put("result", false);

			return output;
		}

		System.out.println("[OK]");
		System.out.printf("[Con NH] Informados %s NCs a Hoja %s\n", elegidos.size(), nodoHoja.ip.getHostName());

		// Si no se cubrió la cantidad requerida de NCs encola la tarea para retransmitir la solicitud a otro WKAN
		if (solicitados - cantidad > 0) {
			if (((AtributosAcceso) atributos).getNodos().size() > 0) {
				HashMap<String,Object> payload = new HashMap<String,Object>();
				payload.put("pendientes", elegidos.size() - solicitados);
				payload.put("direccionNH", nodoHoja);
				payload.put("excepciones", excepciones);

				try {
					atributos.encolar("salida", new Tarea("RETRANSMITIR_SOLICITUD_NCS_NH", payload));
					System.out.printf("[Con NH] Encolada retransmisión de solicitud de ");
					System.out.printf("NH %s\n", nodoHoja.ip.getHostName());
				} catch (InterruptedException e) {
					// No hago nada, el NH seguirá sin concer NCs y volverá a enviar la solicitud, pero al menos la rta
					// de este WKAN le llegó a la Hoja

					System.out.printf("[Con NH] Falló retransmisión de solicitud de ");
					System.out.printf("NH %s\n", nodoHoja.ip.getHostName());
				}
			}
		}

		return output;
	}


	@Override
	public void atender() {
		Mensaje mensaje;
		boolean terminar = false;
		HashMap<String, Comparable> diccionario;

		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
					case Codigos.NH_NA_POST_SOLICITUD_NCS:
						// Un NH solicita NCs a los que conectarse. 
						
						/* Mensaje = (dirección del NH para atender WKAN, 
						 *            código de tarea,
						 *            {'direccionNH: DireccionNodo,
						 *            'pendientes': Integer,
						 *            'conocidos': HashSet<DireccionNodo>
						 *            }
						 *            )
						 */
						diccionario = (HashMap<String, Comparable>) mensaje.getCarga();

						this.solicitudNCsFnc(
								mensaje.getEmisor(),
								(Integer) diccionario.get("pendientes"),
								(HashSet<DireccionNodo>) diccionario.get("conocidos")
						);
						
						//terminar = true;  // Y si mejor lo dejo en True para que la comunicación sea sólo de 1 mensaje? Tengo que cambiarlo desde el cliente también
						break;
					case Codigos.CONNECTION_END:
						terminar = true;
						break;
					default:
						System.out.printf("[Con NH] Recibido mensaje de %s: ",  mensaje.getEmisor().ip.getHostName());
						System.out.printf("%s\n",  mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
			e.printStackTrace();
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
		//System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
	}

}
