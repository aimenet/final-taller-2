package nodes.components.clientes;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import commons.DireccionNodo;
import commons.Codigos;
import commons.ConexionTcp;
import commons.mensajes.Mensaje;
import commons.Tarea;
import commons.Tupla2;
import commons.mensajes.wkan_nc.SolicitudNcsVecinos;
import commons.mensajes.wkan_wkan.RetransmisionAnuncioNc;
import commons.mensajes.wkan_wkan.RetransmisionSolicitudNcsVecinos;
import nodes.components.atributos.AtributosAcceso;

/**
 * Una de las instancias que compone la "faceta" Cliente de un Nodo de Acceso.
 * Es la encargada de conectarse a otro Nodo de Acceso Bien Conocido e
 * interactuar con él.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber
 * racing conditions, actuando más como "consumidores". Esto se debe
 * principalmente a que cada instancia no se comunica con un nodo determinado
 * sino que establece conexiones temporales de acuerdo a la tarea que le haya
 * tocado.
 * 
 * @author rodrigo
 * @since 2019-10-01
 */


public class ClienteNA_NA extends Cliente {
	// Atributos
	// =========
	private ConexionTcp conexionConNodoAcceso;
	private boolean conexionEstablecida, sesionIniciada;
	public String idAsignadoNA, tipoConsumidor;

	// Métodos
	// =======
	public ClienteNA_NA(int idConsumidor) {
		super(idConsumidor, "salida");
		this.atributos = new AtributosAcceso(); // <atributos> está declarado en Cliente
	}

	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// ---------------------------------------------------------------------------------------------------
	private HashMap<String, Object> anuncioFnc(DireccionNodo destino) throws UnknownHostException {
		/**
		 *
		 * Se "presenta" ante otro NABC (comunicando su dirección) a fin de ingresar a la red
		 *
		 */

		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("result", true);

		System.out.printf("Consumidor %s: ejecutando ANUNCIO\n", this.id);

		// TODO 2020-09-13: Checkeo si ya conozco al nodo y si está vivo, para no mandarle al pedo?
		// El anunciarse ante un WKAN ya anunciado lo que hace es congestionar la red, porque después, si se "activa"
		// un nodo que ya estaba en el listado, es como si se reinciara su keepalive nada más

		Integer contador = 0;
		Integer intentos = 3;
		Boolean success = false;

		while ((contador < intentos) && (!success)) {
			if (this.establecerConexion(destino.ip.getHostName(), destino.puerto_na)) {
				Mensaje respuesta = (Mensaje) this.conexionConNodo.enviarConRta(
						new Mensaje(this.atributos.getDireccion(), Codigos.NA_NA_POST_SALUDO, null)
				);

				if (respuesta.getCodigo() == Codigos.OK) {
					success = true;
					((AtributosAcceso) this.atributos).activarNodo(destino);

					System.out.printf("Consumidor %s: anunciado a WKAN %s\n", this.id, destino.ip.getHostName());
				} else {
					contador = intentos + 1; // fuerza a terminar pues se rechazó el anuncio
				}
			} else {
				contador += 1;
				continue;
			}
		}

		output.put("result", success);
		return output;
	}

	private HashMap<String, Object> informarConocidosFnc(HashMap<String, Object> params) {
		/*
		 * Se informa a un WKAN determinado, el listado de todos los WKANs (activos) conocidos hasta el momento
		 * está 100% com
		 *
		 * params: {
		 * 	"direccion": DireccionNodo, la IP del WKAN al que conectarse
		 *  "conocidos": LinkedList<DireccionNodo>, los WKANs conocidos hasta el momento
		 * }
		 *
		 * */

		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		// Envía a un WKAN el listado de los nodos que tiene certeza están estrictamente activos
		System.out.printf("Consumidor %s: ejecutando INFORMAR_CONOCIDOS a WKAN ", this.id);
		System.out.printf(" %s", ((DireccionNodo) params.get("direccion")).ip.getHostName());

		String ipDestino = ((DireccionNodo) params.get("direccion")).ip.getHostName();
		Integer puertoDestino = ((DireccionNodo) params.get("direccion")).puerto_na;

		if (this.establecerConexion(ipDestino, puertoDestino)) {
			Mensaje listado = new Mensaje(
					this.atributos.getDireccion(),
					Codigos.NA_NA_POST_ANUNCIO_ACTIVOS,
					(LinkedList<DireccionNodo>) params.get("conocidos")
			);

			this.conexionConNodo.enviarSinRta(listado);
			System.out.printf(" [COMPLETADO]\n");
		} else {
			Integer status = ((AtributosAcceso) this.atributos).getStatusNodo((DireccionNodo) params.get("direccion"));
			status = status <= 0 ? 0 : status - 1;

			((AtributosAcceso) this.atributos).setKeepaliveNodo((DireccionNodo) params.get("direccion"), status);
			System.out.printf(" [FALLIDO]\n");
		}

		return output;
	}

	private HashMap<String, Object> informarWKANsFnc() throws InterruptedException {
		/*
		 * Dispara una serie de tareas (una por nodo activo) para informar el estado actual de la red (al menos la
		 * conocida por este nodo)
		 *
		 * params: {}
		 *
		 * */

		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		LinkedList<DireccionNodo> nodosActivos = new LinkedList<DireccionNodo>();
		Tupla2<DireccionNodo, LinkedList<DireccionNodo>> nodoAInformar;
		Integer contador = 0;

		System.out.printf("Consumidor %s: ejecutando INFORMAR_WKANS\n", this.id);

		for (Entry<DireccionNodo, Integer> me : ((AtributosAcceso) this.atributos).getNodos().entrySet()) {
			// Sólo informa a aquellos nodos que se sabe con certeza que están activos
			if (me.getValue() == ((AtributosAcceso) this.atributos).keepaliveNodoVecino)
				nodosActivos.add(me.getKey());
		}

		for (DireccionNodo nodo : nodosActivos) {
			nodoAInformar = new Tupla2<DireccionNodo, LinkedList<DireccionNodo>>(nodo, nodosActivos);
			atributos.encolar("salida", new Tarea(00, "INFORMAR_CONOCIDOS", nodoAInformar));
			contador += 1;
		}

		System.out.printf("Consumidor %s: disparadas %s tareas de INFORMAR_CONOCIDOS\n", this.id, contador);

		return output;
	}

	private HashMap<String, Object> retransmitirAnuncioNCFncAnterior(HashMap<String, Object> params) {
		// 2021-04-29: esta función está mal y puede que sea lo que me está mezclando todo. Al menos por lo que vi hasta
		// el momento (flujo "NC se anuncia ante WKAN") no tiene nada que ver así que dejo esta función comentada
		// (renombrada en realidad) y la paso en limpio

		/**
		 * Al no poder "hacerse cargo" de un NC retransmite la consulta a uno de los WKANs conocidos (elegido
		 * aleatoriamente) para que se haga cargo.
		 *
		 * La elección es aleatorioa para:
		 *     1) no controlar que 2 (o más) WKANs que reciben la retransmisión se hagan cargo del mismo nodo
		 * 	   2) no saturar la red con tantos mensajes
		 *
		 * Para evitar problemas se setea un TTL al mensaje a fin de que alcanzado el mismo se informe al NC que nadie
		 * puede hacerse cargo de él.
		 *
		 * params: {
		 *     "solicitud_original": RetransmisionSolicitudNcsVecinos
		 * }
		 *
		 */

		 HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		RetransmisionSolicitudNcsVecinos solicitud = (RetransmisionSolicitudNcsVecinos) params.get("solicitud_original");

		DireccionNodo wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();

		if (wkanDestino != null) {
			String ipDestino = wkanDestino.ip.getHostName();
			Integer puertoDestino = wkanDestino.puerto_na;

			System.out.printf("Consumidor %s: retransmitiendo anuncio NC a %s", this.id, wkanDestino.ip.getHostName());

			if (this.establecerConexion(ipDestino, puertoDestino)) {
				ArrayList<DireccionNodo> visitados = new ArrayList<DireccionNodo>();
				visitados.add(atributos.getDireccion());

				this.conexionConNodo.enviarSinRta(solicitud);

				System.out.printf(" [COMPLETADO]\n");
			} else {
				Integer status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
				status = status <= 0 ? 0 : status - 1;

				((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);
				System.out.printf(" [FALLIDO]\n");
			}
		} else {
			System.out.printf(
					"No se encontraron WKANs para retransmitir anuncio del NC %s\n",
					solicitud.getEmisor().ip.getHostName()
			);
		}

		return output;
	}

	private HashMap<String, Object> retransmitirAnuncioNCFnc(HashMap<String, Object> params) {
		/**
		 * Al no poder "hacerse cargo" de un NC retransmite la consulta a uno de los WKANs conocidos (elegido
		 * aleatoriamente) para que se haga cargo.
		 *
		 * La elección es aleatorioa para:
		 *     1) no controlar que 2 (o más) WKANs que reciben la retransmisión se hagan cargo del mismo nodo
		 * 	   2) no saturar la red con tantos mensajes
		 *
		 * Para evitar problemas se setea un TTL al mensaje a fin de que alcanzado el mismo se informe al NC que nadie
		 * puede hacerse cargo de él.
		 *
		 * params: {
		 *     "mensaje_retransmision", RetransmisionAnuncioNc
		 * }
		 *
		 */

		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		RetransmisionAnuncioNc mensaje = (RetransmisionAnuncioNc) params.get("mensaje_retransmision");

		DireccionNodo wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();

		if (wkanDestino != null) {
			String ipDestino = wkanDestino.ip.getHostName();
			Integer puertoDestino = wkanDestino.puerto_na;

			System.out.printf("Consumidor %s: retransmitiendo anuncio NC a %s", this.id, wkanDestino.ip.getHostName());

			if (this.establecerConexion(ipDestino, puertoDestino)) {
				this.conexionConNodo.enviarSinRta(mensaje);

				System.out.printf(" [COMPLETADO]\n");
			} else {
				Integer status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
				status = status <= 0 ? 0 : status - 1;

				((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);
				System.out.printf(" [FALLIDO]\n");
			}
		} else {
			System.out.printf(
					"No se encontraron WKANs para retransmitir anuncio del NC %s\n",
					mensaje.getNodoCentral().ip.getHostName()
			);
		}

		return output;
	}

	private HashMap<String, Object> retransmitirSolicitudNcsNh(HashMap<String, Object> params) {
		/**
		 * Método en el que se retransmite a un WKAN random un mensaje emitido por un NH solicitando NCs a los
		 * que conectarse.
		 * Se elige aleatoriamente como medida (muy simple) de distribución equitativa de mensajes en la red.
		 *
		 * params: {
		 *     "direccionNH_NA"
		 * 	   "direccionNH_NC"
		 * 	   "pendientes"
		 * 	   "consultados"
		 * 	   "saltos"
		 *
		 * }
		 *
		 */
		DireccionNodo wkanDestino = null;
		HashMap<String, Object> output = new HashMap<String, Object>();
		HashMap<String, Object> payload = new HashMap<String, Object>();
		Integer contador, saltos;
		LinkedList<String> wkanConsultados;


		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		// Si bien params y payload compartirán gran parte de su contenido, prefiero utilizar una variable nueva
		// con lo estrictamente necesario
		payload.put("direccionNH_NA", params.get("direccionNH_NA"));
		payload.put("direccionNH_NC", params.get("direccionNH_NC"));
		payload.put("pendientes", params.get("pendientes"));

		// Este WKAN puede ser el primero en retransmitir o estar "retransmitiendo una retransmisión"
		if (payload.containsKey("consultados")) {
			wkanConsultados = (LinkedList<String>) payload.get("consultados");
			saltos = (Integer) payload.get("saltos");
		} else {
			wkanConsultados = new LinkedList<String>();
			saltos = 9 + 1; // TODO: hardcodeo -> en realidad es 9 pero como es el primero y le resto uno después, se lo agrego acá (un asco)
		}
		wkanConsultados.add(atributos.getDireccion().ip.getHostName());
		payload.put("consultados", wkanConsultados);
		payload.put("saltos", saltos - 1);

		// Valida que el WKAN elegido no haya sido consultado previamente
		contador = 5;
		while (contador > 0) {
			wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();
			if ( !((LinkedList<String>) payload.get("consultados")).contains(wkanDestino) ) {
				contador = 0;
			} else {
				wkanDestino = null;
			}
		}

		System.out.printf("[Cli %s]\t", this.id);
		// Si no conoce ningún WKAN (o fallaron todos los intentos de elegir uno no visitado) no hace nada.
		if (wkanDestino != null) {
			System.out.printf("enviando solicitud de NCs para NH a %s", wkanDestino);

			if (this.establecerConexion(wkanDestino.ip.getHostName(), wkanDestino.puerto_na)) {
				this.conexionConNodo.enviarSinRta(
						new Mensaje(
								this.atributos.getDireccion(),
								Codigos.NA_NA_POST_RETRANSMISION_NH_SOLICITUD_NC,
								payload
						)
				);

				System.out.printf(" [COMPLETADO]\n");
			} else {
				Integer status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
				status = status <= 0 ? 0 : status - 1;

				((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);

				System.out.printf(" [FALLIDO]\n");
			}
		} else {
			System.out.printf("no se encontró WKAN al que enviar solicitud de NCs para NH\n");
		}

		return output;
	}

	private HashMap<String, Object> solicitarNCsVecinos(HashMap<String, Object> params) {
		/**
		 * Se aceptó un NC y ahora debe comenzar la transmisión del mensaje entre WKANs para solicitar NCs a los que
		 * enlazar al recién integrado.
		 *
		 * El mensaje se envía de a un WKAN a la vez, elegido aleatoriamente, para no inundar la red.
		 * Se definen una cantidad de saltos máxima (3 veces la cantidad de NCs a los que se conecta un NC) y se
		 * registran los NABC por los que pasa el mensaje a fin de no duplicar la rta. Si el WKAN posee un NC que pueda
		 * ser vecino del solicitante se lo informa directamente.
		 * La rta del NC indicará si aún no completó la cantidad requerida de vecinos para que el WKAN retransmita el
		 * mensaje o lo descarte, según corresponda.
		 * Aquél WKAN que reciba el mensaje de forma repetida simplemente lo retransmitirá.
		 *
		 * params: {
		 *     "ncDestino": DireccionNodo, la IP del NC al que conectarse
		 * }
		 *
		 */

		// TODO 2021-04-25: esta función creo que no se usa más

		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);


		DireccionNodo ncDestino = (DireccionNodo) params.get("ncDestino");
		DireccionNodo wkanDestino = null;

		// Armado del payload de la consulta (donde está toda la información de "control")
		// El payload puede tener distintos formatos, dependiendo de quién haya generado la tarea
		if (!params.containsKey("preparado")) {
			LinkedList<DireccionNodo> consultados = new LinkedList<DireccionNodo>();
			consultados.add(atributos.getDireccion());

			params = new HashMap<String, Object>();
			params.put("consultados", consultados);
			params.put("saltos", 9);
			params.put("ncDestino", ncDestino);
			params.put("ncsRestantes", 3); // TODO: esto no tiene que estar hardcodeado
		}

		// Valida que el WKAN elegido no haya sido consultado previamente
		Integer contador = 5;
		while (contador > 0) {
			wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();
			if ( !((LinkedList<DireccionNodo>) params.get("consultados")).contains(wkanDestino) ) {
				contador = 0;
			} else {
				wkanDestino = null;
			}
		}

		System.out.printf("[Cli %s]\t", this.id);
		// Si no conoce ningún WKAN (o fallaron todos los intentos de elegir uno no visitado) no hace nada.
		if (wkanDestino != null) {
			System.out.printf("enviando solicitud de vecinos para nuevo NC a %s", wkanDestino.ip.getHostName());

			if (this.establecerConexion(wkanDestino.ip.getHostName(), wkanDestino.puerto_na)) {
				this.conexionConNodo.enviarSinRta(
						new Mensaje(
								this.atributos.getDireccion(),
								Codigos.NA_NA_POST_SOLICITUD_VECINOS_NC,
								params
						)
				);

				System.out.printf(" [COMPLETADO]\n");
			} else {
				Integer status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
				status = status <= 0 ? 0 : status - 1;

				((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);
				System.out.printf(" [FALLIDO]\n");
			}
		} else {
			System.out.printf("no se encontró WKAN al que enviar solicitud de vecinos\n");
		}

		return output;
	}

	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		boolean success;
		HashMap<String, Comparable> salida;
		HashMap<String, Object> hashmapPayload;
		Integer contador = 0;
		Integer puertoDestino;
		Integer status = 0;
		LinkedList<String> strList;
		String ipDestino;
		String ncDestino;
		String wkanDestino;

		salida = null;

		// 2020-07-25 todo este switch debería ser como el de ClienteNA_NC.java que es mucho más claro
		switch (tarea.getName()) {
			case "ANUNCIO":
				try {
					this.anuncioFnc((DireccionNodo) tarea.getPayload());
				} catch (UnknownHostException e) {
					// No hago nada, si llegara a ser el único WKAN al que estoy conectado, la verificación que se hace
					// en el loop ppal de NodoAccesoBienConocido se encargaría de disparar nuevamente la tarea
				}

				break;
			case "INFORMAR_WKANS":
				this.informarWKANsFnc();
				break;
			case "INFORMAR_CONOCIDOS":
				hashmapPayload = new HashMap<String, Object>();
				hashmapPayload.put("direccion", ((Tupla2<DireccionNodo, LinkedList<DireccionNodo>>) tarea.getPayload()).getPrimero());
				hashmapPayload.put("conocidos", ((Tupla2<DireccionNodo, LinkedList<DireccionNodo>>) tarea.getPayload()).getSegundo());

				this.informarConocidosFnc(hashmapPayload);

				break;
			case "RETRANSMITIR_ANUNCIO_NC":
				hashmapPayload = new HashMap<String, Object>();
				hashmapPayload.put("mensaje_retransmision", (RetransmisionAnuncioNc) tarea.getPayload());

				this.retransmitirAnuncioNCFnc(hashmapPayload);

				break;
			case "SOLICITAR_NCS_VECINOS":
				this.solicitarNCsVecinos((HashMap<String, Object>) tarea.getPayload());

				break;
			case "RETRANSMITIR_SOLICITUD_NCS_NH":
				this.retransmitirSolicitudNcsNh((HashMap<String, Object>) tarea.getPayload());

				break;
			}
		return salida;
	}


}

