package nodes.components.servidores;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

import commons.*;
import commons.mensajes.Mensaje;
import commons.mensajes.wkan_nc.SolicitudNcsVecinos;
import commons.mensajes.wkan_wkan.RetransmisionAnuncioNc;
import commons.mensajes.wkan_wkan.RetransmisionSolicitudNcsVecinos;
import nodes.components.clientes.ClienteNA_NC;
import nodes.components.WKAN_Funciones;
import nodes.components.atributos.AtributosAcceso;

/**
 * Consultor que corre en hilos generado por el Servidor de Nodos de Acceso. Es el
 * encargado de la atención propiamente dicha de otros Nodos de Acceso.
 * 
 * @author rodrigo
 * @since 2019-10-06
 */

public class ConsultorNA_NA implements Consultor {
	/* --------- */
	/* Atributos */
	/* --------- */
	private AtributosAcceso atributos = new AtributosAcceso();
	private ObjectInputStream buffEntrada;
	private ObjectOutputStream buffSalida;
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();


	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// ---------------------------------------------------------------------------------------------------
	private HashMap<String, Object> anuncioFnc(Mensaje mensaje) throws IOException {
		/*
		* Un WKAN se anuncia a fin de ingresar a la red.
		*
		* mensaje: no tiene payload, lo importante es el emisor
		* */

		DireccionNodo anunciante = mensaje.getEmisor();

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		System.out.printf("Recibido anuncio de WKAN en %s ", anunciante.ip.getHostName());
		this.buffSalida.writeObject(new Mensaje(this.atributos.getDireccion(), Codigos.OK, null));
		System.out.printf("[CONFIRMADO]\n");
		this.atributos.activarNodo(anunciante);

		return output;
	}

	private HashMap<String, Object> anuncioActivosFnc(Mensaje mensaje) {
		/*
		 * Un WKAN envía listado de Nodos (wkan) conocidos
		 *
		 * Payload del mensaje: LinkedList<DireccionNodo>
		 * */

		DireccionNodo anunciante = mensaje.getEmisor();

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		System.out.printf("Recibido listado de WKAN en %s", mensaje.getEmisor().ip.getHostName());

		LinkedList<DireccionNodo> confirmados = (LinkedList<DireccionNodo>) mensaje.getCarga();

		confirmados.add(mensaje.getEmisor());
		confirmados.remove(this.atributos.getDireccion());

		// No sé si lo correcto es marcarlos como activos confiando en lo que diga otro nodo, pero en este momento
		// a fines prácticos da igual
		for (DireccionNodo nodo : confirmados)
			this.atributos.activarNodo(nodo);

		System.out.printf(" [ACTUALIZADO]\n");

		return output;
	}

	private HashMap<String, Object> retransmisionAnuncioNCFnc(RetransmisionAnuncioNc retransmision) {
		/*
		* Anuncio (retransmitido por un WKAN sin capacidad de atenderlo) de un NC
		*
		* */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		DireccionNodo nodo = retransmision.getNodoCentral();
		Integer codigo = funciones.atenderAnuncioNC(retransmision);

		if (codigo == Codigos.OK) {
			// Tarea donde se le informará al NC que este Nodo lo aceptará
			System.out.printf("Aceptado NC %s. Comunicando\n", nodo.getUnaDireccion("acceso"));

			try {
				atributos.encolar(
						"centrales", new Tarea(00, "ANUNCIO-ACEPTADO", nodo)
				);
			} catch (InterruptedException e) {
				// TODO: ver cómo lo soluciono
				e.printStackTrace();
			}
		} else if (codigo == Codigos.ACCEPTED) {
			System.out.printf("Capacidad max NC alcanzada, rechazado NC %s\n", nodo.getUnaDireccion("acceso"));
		}

		return output;
	}

	private HashMap<String, Object> retransmisionSolicitudNCsNHFnc(Mensaje mensaje) throws InterruptedException {
		/**
		 * Método en el que se retransmite a un WKAN random un mensaje emitido por un NH solicitando NCs a los
		 * que conectarse.
		 *
		 * Se elige aleatoriamente como medida (muy simple) de distribución equitativa de mensajes en la red.
		 *
		 * Payload del mensaje: {
		 *     "direccionNH": DireccionNodo,
		 *     "pendientes": Integer,
		 *     "consultados": LinkedList<DireccionNodo>,
		 *     "saltos": Integer
		 * }
		 *
		 */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		HashMap<String, Object> params = (HashMap<String, Object>) mensaje.getCarga();

		// -------------------------------------------------------------------------------------------------------------
		// Esto que sigue es un copy paste de lo que hace ConsultorNA_NH.java -> unificarlo

		// Obtrención de NCs que pueden recibir a la H: si no existen más WKANs en la red entonces buscará entre sus NCs
		// la cantidad solicitada, sino escogerá sólo 1 (y retransmitirá la consulta)
		ArrayList<DireccionNodo> disponibles = new ArrayList<DireccionNodo>(
				((AtributosAcceso) atributos).getNodos().keySet()
		);

		ArrayList<DireccionNodo> consultados = new ArrayList<DireccionNodo>(
				(LinkedList<DireccionNodo>) params.get("consultados")
		);

		disponibles.removeAll(consultados);

		Integer requeridos = disponibles.size() > 0 ? 1 : (Integer) params.get("pendientes");
		Boolean forward = disponibles.size() > 0;

		// "Trae" el doble de lo requerido para aumentar las probabilidades de encontar un NC que no tenga ya al NH
		List<DireccionNodo> candidatos = funciones.getNCsConCapacidadNH(requeridos * 2, new HashSet<DireccionNodo>());

		// Consulta a los NC si cuentan con el NH entre sus filas, quedándose con aquellos que no
		ClienteNA_NC consultor = new ClienteNA_NC(99);
		LinkedList<DireccionNodo> elegidos = new LinkedList<DireccionNodo>();

		for (DireccionNodo central : candidatos) {
			HashMap<String, Comparable> payload = new HashMap<String, Comparable>();
			payload.put("direccionNC", central);
			payload.put("direccionNH", (DireccionNodo) params.get("direccionNH"));

			Tarea tarea = new Tarea("CAPACIDAD-ATENCION-NH", payload);

			if ((Boolean) consultor.procesarTarea(tarea).get("status")) {
				elegidos.add(central);

				if (elegidos.size() >= requeridos)
					break;
			}
		}

		// Hasta acá lo que es igual
		// -------------------------------------------------------------------------------------------------------------

		// retransmite la consulta a otro WKAN si corresponde
		if (forward && (Integer) params.get("pendientes") > elegidos.size()) {
			LinkedList<DireccionNodo> aux = (LinkedList<DireccionNodo>) params.get("consultados");
			aux.add(atributos.getDireccion());

			params.put("consultados", aux);
			params.put("pendientes", (Integer) params.get("pendientes") > elegidos.size());

			atributos.encolar("salida", new Tarea("RETRANSMITIR_SOLICITUD_NCS_NH", params));
		}

		// A cada NC elegido se le enviará la orden de establecer contacto con el NH
		// -> es una ineficiencia no hacerlo al momento en que se le consulta la capacidad pero en esta primer versión
		//    lo hago así deliveradamente para "modularizar" lo más posible (a costa de eficiencia)
		for(DireccionNodo central : elegidos) {
			HashMap<String, Object> payload = new HashMap<String, Object>();

			payload.put("direccionNC", central);
			payload.put("direccionNH", (DireccionNodo) params.get("direccionNH"));

			atributos.encolar("centrales", new Tarea(00, "ACEPTAR-NH", payload));
		}

		return output;
	}

	private HashMap<String, Object> retransmisionSolicitudVecinosNCFnc(RetransmisionSolicitudNcsVecinos solicitud) {
		/**
		 * Un WKAN ha retransmitido una solicitud de NCs vecinos, emitida por un NC.
		 *
		 * Evalúa la disponibilidad de NCs para conectarlos y retransmite nuevamente si corresponde.
		 *
		 */

		// 2021-05-11
		// Es prácticamente igua a lo que hace  ConsultorNA_NC.solicitudVecinos, la unica diferencia es que este recibe
		// una solicitud de retransmision de otro WKAN mientras que aquel no (es quien genera la primera).
		// La logica esta contenida en WKAN_Funciones y en el ClienteNA_NA por lo que aca me puedo dar el lujo de hacer
		// un copy-paste de ConsultorNA_NC.solicitudVecinos

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		// 1ra parte: checkeo de NC disponible para conectar
		System.out.printf(
				"[Con NA] Recibido pedido de %d NCs vecinos para %s",
				solicitud.getFaltantes(),
				solicitud.getNodoCentral().ip.getHostName()
		);

		WKAN_Funciones.atenderSolicutdVecinosNCOutput status;
		status = funciones.atenderSolicutdVecinosNC(solicitud.getNodoCentral());

		if (status.equals(WKAN_Funciones.atenderSolicutdVecinosNCOutput.KO_NODO_DESCONOCIDO)) {
			// Si el NC es desconocido debe estar siendo administrado por otro WKAN
			System.out.printf(" [ERROR] (NC desconocido)");
			output.put("result", false);
			return output;
		} else if (status.equals(WKAN_Funciones.atenderSolicutdVecinosNCOutput.KO_ERROR)) {
			System.out.printf(" [ERROR] (tarea de conexión con vecino no encolada)");
		} else if (status.equals(WKAN_Funciones.atenderSolicutdVecinosNCOutput.OK_SIN_VECINO)) {
			System.out.printf(" (No hay NCs para sugerir)");
		}

		// 2da parte: retransmisión
		ArrayList<DireccionNodo> visitados = solicitud.getWkansVisitados();
		visitados.add(this.atributos.getDireccion());

		Integer saltos = solicitud.getSaltos() - 1;
		Integer faltantes = solicitud.getFaltantes();;
		if (status.equals(WKAN_Funciones.atenderSolicutdVecinosNCOutput.OK_CON_VECINO))
			faltantes -= 1;

		if (faltantes <= 0 || saltos <= 0)
			return output;

		RetransmisionSolicitudNcsVecinos solicitud_retransmitir = new RetransmisionSolicitudNcsVecinos(
				this.atributos.getDireccion(),
				Codigos.NA_NA_POST_RETRANSMISION_SOLICITUD_VECINOS_NC,
				solicitud.getNodoCentral(),
				saltos,
				faltantes,
				visitados
		);

		Tarea tarea = new Tarea(00, Constantes.TSK_NA_RETRANSMITIR_SOLICITUD_VECINOS_NC, solicitud_retransmitir);

		try {
			atributos.encolar("salida", tarea);
			// System.out.println(" [OK]");
			System.out.println("[Con NA] Encolada retransmisión pedido NCs vecinos");
		} catch (InterruptedException e) {
			// No hago nada, el NC volverá a solicitar vecinos de ser necesario. Sí debería controlar por qué no se
			// puede enconlar
			System.out.printf(" [ERROR] (imposible retransmitir a WKANs)");
			e.printStackTrace();
			output.put("result", false);
		}

		return output;
	}

	private HashMap<String, Object> solicitudVecinosNCFnc(RetransmisionSolicitudNcsVecinos solicitud) throws InterruptedException {
		/**
		 * Evalua si algún NC posee capacidad de enlazarse a otro, a fin de comunicarlo al NC recientemente incorporado
		 * a la red
		 *
		 * Payload del mensaje: RetransmisionSolicitudNcsVecinos
		 *
		 */

		// Estos son comunes a todas las funciones
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		// RetransmisionSolicitudNcsVecinos solicitud = (RetransmisionSolicitudNcsVecinos) mensaje.getCarga();

		HashMap<DireccionNodo, HashMap<String, Comparable>> centralesRegistrados = atributos.getCentrales();

		Integer faltantes = solicitud.getFaltantes();
		Integer saltos = solicitud.getSaltos() - 1;
		ArrayList<DireccionNodo> confirmados = solicitud.getWkansVisitados();
		confirmados.add(atributos.getDireccion());

		for (DireccionNodo key : centralesRegistrados. keySet()) {
			Integer activos = (int) centralesRegistrados.get(key).get("centrales_activos");
			Integer maximos = (int) centralesRegistrados.get(key).get("centrales_max");

			if ((boolean) centralesRegistrados.get(key).get("alive"))
				if (activos < maximos) {
					atributos.encolar(
							"centrales",
							new Tarea(
									00,
									"CONECTAR-NCS",
									new Tupla2<DireccionNodo, DireccionNodo> (key, (DireccionNodo) solicitud.getNodoCentral())
							)
					);

					// Se marca el anuncio del NC vecino.
					faltantes -= 1;

					break;
				}
		}

		// Si restan NCs por conectar se retransmite nuevamente el mensaje
		if (faltantes > 0 && saltos > 0) {
			RetransmisionSolicitudNcsVecinos solicitud_retransmitir = new RetransmisionSolicitudNcsVecinos(
					this.atributos.getDireccion(),
					Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC,
					solicitud.getNodoCentral(),
					saltos,
					faltantes,
					confirmados
			);

			atributos.encolar("salida", new Tarea(00, "SOLICITAR_NCS_VECINOS", solicitud_retransmitir));
		}

		System.out.println("Procesada solicitud de vecinos para NC [OK]");

		return output;
	}


	@Override
	public void atender() {
		HashMap<String, HashMap<String,Comparable>> centralesRegistrados;
		HashMap<String, Object> diccionario;
		Integer codigo;
		Integer contador;
		Integer emisor;
		Integer intentos;
		Integer puertoDestino;
		LinkedList<String> confirmados;
		Mensaje mensaje;
		boolean terminar = false;
		ObjectInputStream buffEntrada;
		//ObjectOutputStream buffSalida;
		String ipDestino;
		String strAux;
		
		try {
			// Instanciación de los manejadores del buffer.
			this.buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión

			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();

				// 2020-07-25 todo este switch debería ser como el de ClienteNA_NC.java que es mucho más claro (o el de algún Consultor que haya hecho mś prolijo)
				switch(mensaje.getCodigo()){
					case Codigos.NA_NA_POST_SALUDO:
						this.anuncioFnc(mensaje);
						break;
					case Codigos.NA_NA_POST_ANUNCIO_ACTIVOS:
						this.anuncioActivosFnc(mensaje);
						break;
					case Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC:
						this.retransmisionAnuncioNCFnc((RetransmisionAnuncioNc) mensaje);
						break;
					case Codigos.NA_NA_POST_SOLICITUD_VECINOS_NC:
						this.solicitudVecinosNCFnc((RetransmisionSolicitudNcsVecinos) mensaje.getCarga());
						break;
					case Codigos.NA_NA_POST_RETRANSMISION_NH_SOLICITUD_NC:
						this.retransmisionSolicitudNCsNHFnc(mensaje);
						break;
					case Codigos.NA_NA_POST_RETRANSMISION_SOLICITUD_VECINOS_NC:
						this.retransmisionSolicitudVecinosNCFnc((RetransmisionSolicitudNcsVecinos) mensaje);
						break;
					default:
						System.out.printf("\tAnuncio de nodo %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
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
		//System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
	}


} // Fin clase

