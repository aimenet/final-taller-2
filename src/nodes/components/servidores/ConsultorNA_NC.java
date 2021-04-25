package nodes.components.servidores;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

import commons.*;
import commons.mensajes.Mensaje;
import commons.mensajes.wkan_nc.InformeNcsVecinos;
import commons.mensajes.wkan_nc.SolicitudNcsVecinos;
import commons.mensajes.wkan_wkan.RetransmisionSolicitudNcsVecinos;
import commons.structs.wkan.NCIndexado;
import nodes.components.WKAN_Funciones;
import nodes.components.atributos.AtributosAcceso;

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

	private void solicitudVecinos(SolicitudNcsVecinos solicitud) {
		/* Un NC solicita otros NCs vecinos a los que enlazarse.
		*
		*  Acá debe existir una política ya que si este WKAN responde al menos 1 NC cada vez que responde, se corre el
		*  riesgo de que gran parte de los vecinos a los que termine enlazado el NC sean de este WKAN y por ende la red
		*  no estaría "distribuida" (o balanceada).
		*
		*  Por ahora la política definida es la siguiente: si puede, le indica un NC aleatorio administrado por él y
		*  registra el timestamp. No volverá a informarle nuevos NCs vecinos hasta que no haya pasado X tiempo desde el
		*  último.
		*
		*  Y siempre retransmitirá la consulta a otros WKANs a fin de distribuír las conexiones a lo largo de toda la
		*  red. Esto es exactamente lo mismo que se hace cuando un NC se anuncia por primera vez.
		*
		* */
		Boolean errorFlag = false;

		System.out.printf("[Con NC] Recibido pedido de %d ", solicitud.getFaltantes());
		System.out.printf(" NCs vecinos para %s", solicitud.getEmisor().ip.getHostName());

		// Busca un NC de los administrados para informarle
		// -> Acá debería encolar para que un cliente le mande al NC (que en este caso va a estar oficiando de server)
		// -> Lo hago acá mismo pero no va, hay que encolar la tarea CONECTAR_NCS
		NCIndexado nodo = atributos.getCentral(solicitud.getEmisor());

		if ((nodo == null) || (solicitud.getFaltantes() <= 0)) {
			// Si el NC es desconocido debe estar siendo administrado por otro WKAN
			System.out.printf(" [ERROR] (NC desconocido)");
			errorFlag = true;
			return;
		}

		Boolean informar = false;

		if (nodo.ultimoNncInformado == null) {
			informar = true;
		} else {
			Timestamp start = nodo.ultimoNncInformado;
			Timestamp end = new Timestamp(System.currentTimeMillis());

			long timeElapsed = Duration.between(start.toInstant(),end.toInstant()).toSeconds();

			if (timeElapsed >= atributos.esperaEntreInformeDeVecinos)
				informar = true;
		}

		if (informar) {
			DireccionNodo vecino = atributos.getRandomNCDistinto(solicitud.getEmisor());

			if (vecino != null) {
				Tupla2<DireccionNodo, DireccionNodo> par = new Tupla2<DireccionNodo, DireccionNodo>(
						solicitud.getEmisor(), vecino
				);

				Tarea tarea = new Tarea(00, Constantes.TSK_NA_CONECTAR_NCS, par);

				try {
					atributos.encolar("centrales", tarea);
				} catch (InterruptedException e) {
					// TODO: qué hago?
					System.out.printf(" [ERROR] (tarea no encolada)");
					errorFlag = true;
					e.printStackTrace();
				}
			} else {
				System.out.printf(" (No hay NCs para sugerir)");
			}

		}

		// Se retransmite a otros WKANs la solicitud a fin de distribuir por toda la red la carga de Nodos
		RetransmisionSolicitudNcsVecinos solicitud_retransmitir = new RetransmisionSolicitudNcsVecinos(
				this.atributos.getDireccion(),
				Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC,
				solicitud.getEmisor(),
				(solicitud.getFaltantes() * 2) - 1,
				solicitud.getFaltantes(),
				new ArrayList<DireccionNodo>()
		);

		try {
			atributos.encolar("salida", new Tarea(00, "RETRANSMITIR_ANUNCIO_NC", solicitud_retransmitir));
		} catch (InterruptedException e) {
			// No hago nada, el NC volverá a solicitar vecinos de ser necesario. Sí debería controlar por qué no se
			// puede enconlar
			System.out.printf(" [ERROR] (imposible retransmitir a WKANs)");
			errorFlag = true;
			e.printStackTrace();
		}

		if (!errorFlag)
			System.out.println(" [OK]");
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
			
			// Bucle principal de atención al cliente. Finalizará cuando este indique que cerrará la conexión
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
					  	              null)*/
						
						this.anuncioFnc(mensaje.getEmisor());
						terminar = true;
						break;
					case Codigos.NC_NA_POST_KEEPALIVE:
						// El NC informa que está activo
						this.keepAliveFnc(mensaje.getEmisor());

						terminar = true;
						break;
					case Codigos.NC_NA_GET_SOLICITUD_VECINOS:
						this.solicitudVecinos((SolicitudNcsVecinos) mensaje);
						terminar = true;
						break;
					default:
						System.out.printf("\tRecibido mensaje en %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (ClassNotFoundException | IOException e) {
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
