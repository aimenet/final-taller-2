package nodes.components;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;
import commons.structs.DireccionNodo;

public class ConsultorNA_NC implements Consultor {
	// De cada instancia
	private AtributosAcceso atributos = new AtributosAcceso();
	private ObjectInputStream buffEntrada;
	private ObjectOutputStream buffSalida;
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();


	// Procesamiento de peticiones
	// -----------------------------------------------------------------------------------------------------------------
	private HashMap<String, Object> anuncioFnc(DireccionNodo anunciante) {
		/**
		 * Anuncio de un NC que acaba de ingresar a la red
		 *
		 */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		System.out.printf("Recibido anuncio de NC en %s\n", anunciante.ip.getHostName());

		Integer codigo = null;
		try {
			codigo = funciones.atenderAnuncioNC(anunciante, true);
		} catch (InterruptedException e) {
			// A fines prácticos, está bien la retransmisión en tanto el código ACCEPTED no se use para otra cosa
			codigo = Codigos.ACCEPTED;
		}

		try {
			buffSalida.writeObject(new Mensaje(null, codigo, null));
		} catch (IOException e) {
			// TODO 2020-09-27: ¿hacer algo?
		}

		if (codigo == Codigos.OK) {
			System.out.printf("Aceptado NC %s\n", anunciante.ip.getHostName());
		} else if (codigo == Codigos.ACCEPTED) {
			System.out.printf("Capacidad max NC alcanzada, ");
			System.out.printf("retransmitiendo anuncio de NC %s\n", anunciante.ip.getHostName());
		}

		return output;
	}

	private HashMap<String, Object> keepAliveFnc(DireccionNodo anunciante) {
		/**
		 * El NC informa que está activo. No debería llegar un mensaje de un NC no registrado ya que esto debería ser
		 * posterior al anuncio.
		 *
		 */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		System.out.printf("[Con NC] Recibido keepalive de NC %s", anunciante.ip.getHostName());

		HashMap<String, Comparable> diccionario = ((AtributosAcceso) this.atributos).getCentrales().get(anunciante);

		if (diccionario == null) {
			System.out.println(" [ERROR] (NC desconocido)");
		} else {
			// TODO 2020-09-27: "diccionario" es una referencia?
			diccionario.put("alive", true);
			diccionario.put("timestamp", new Timestamp(System.currentTimeMillis()));
			System.out.println(" [OK]");
		}

		return output;
	}

	@Override
	public void atender() {
		Mensaje mensaje;
		boolean terminar = false;
		HashMap<String, Comparable> diccionario;
		Integer codigo;
		Timestamp auxTimestamp;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				// Si no se trata de un NC nuevo, registra que está activo
				if (((AtributosAcceso) atributos).getCentrales().containsKey(mensaje.getEmisor())) {
					auxTimestamp = new Timestamp(System.currentTimeMillis());
					((AtributosAcceso) atributos).getCentrales().get(mensaje.getEmisor()).put("timestamp", auxTimestamp);
					((AtributosAcceso) atributos).getCentrales().get(mensaje.getEmisor()).put("alive", true);
				}
				
				switch(mensaje.getCodigo()){
					case Codigos.NC_NA_POST_ANUNCIO:
						/* Mensaje = (dirección del NC para atender WKAN,
					  	              código de tarea,
					  	              diccionario con direcciones servidor del NC)*/
						
						this.anuncioFnc(mensaje.getEmisor());
						terminar = true;
						break;
					case Codigos.NC_NA_POST_KEEPALIVE:
						// El NC informa que está activo
						this.keepAliveFnc(mensaje.getEmisor());

						terminar = true;
						break;
					default:
						System.out.printf("\tRecibido mensaje en %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
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
