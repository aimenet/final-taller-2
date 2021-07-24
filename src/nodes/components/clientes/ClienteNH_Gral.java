package nodes.components.clientes;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import commons.*;
import commons.mensajes.Mensaje;
import nodes.components.atributos.AtributosHoja;

/**
 * Uno de las instancias que compone la "faceta" Cliente de un Nodo Hoja. Es la encargada de 
 * conectarse a un Nodo Central e interactuar con él. Consume de la cola de transmisión que le fue asignada,
 * enviando tanto consultas como listado de imágenes a compartir
 * 
 * [2020-03-01] Ahora el consumidor debe conectarse tanto con NCs como con WKANs.
 *              Lo correcto es implementar el esquema nuevo, es decir, que esta clase extienda de Cliente
 * 
 * @author rodrigo
 */

public class ClienteNH_Gral extends Cliente {
	// Atributos
	// =========
	private ConexionTcp conexionConNodoCentral;
	private boolean conexionEstablecida, sesionIniciada;
	public Integer idConsumidor, puertoNC;
	

	// Métodos
	// =======
	public ClienteNH_Gral(int idConsumidor) {
		super(idConsumidor, "salida");  // TODO: la cola debería ser un parámetro y no algo hardcodeado 
		this.atributos = new AtributosHoja();
	}
	
	
	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// ---------------------------------------------------------------------------------------------------
	private HashMap<String, Object> anunciarAnteNCFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje mensaje;
		Mensaje respuesta;
		UUID token;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		// Si existe un ID de Hoja definido en los atributos, se envía un mensaje de reconexión.
		// En caso contrario se envía un saludo
		token = ((AtributosHoja) this.atributos).getId((DireccionNodo) params.get("direccionNC"));

		if (token == null) {
			// Saludo, 1ra conexión
			mensaje = new Mensaje(null,1, this.atributos.getDireccion());
		} else {
			// Saludo de reconexión
			//mensaje = new Mensaje(null,1, "##"+token+"##");
			mensaje = new Mensaje(null,1, token);
		}

		respuesta = (Mensaje) conexionConNodo.enviarConRta(mensaje);
		
		if (respuesta.getCodigo().equals(1) && respuesta.getCarga() != null){
			//La respuesta contiene el ID con el que se identificará al Cliente.
			token = (UUID) respuesta.getCarga();
			sesionIniciada = true;
			
			// Seteo del ID que recibió la H en los atributos comartidos para ser conocido por todos los
			// "componentes" del nodo Hoja. Si se trataba de una reconexión el ID será igual así que en realidad es
			// redundante ese paso
			((AtributosHoja) this.atributos).setId((DireccionNodo) params.get("direccionNC"), token);

			System.out.printf("\n[Cli %s] anunciado ante NC [OK]\n", this.idConsumidor);
		} else {
			output.put("result", false);
			System.out.printf("\n[Cli %s] anunciado ante NC [ERROR]\n", this.idConsumidor);
		}
		
		return output;
	}

	private HashMap<String, Object> anunciarImgsFnc(HashMap<String, Object> params){
		ArrayList<CredImagen> credencialesImgs;
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje mensaje;

		UUID idAsignado = ((AtributosHoja) this.atributos).getCentrales().get(
				((DireccionNodo) params.get("direccionNC"))
		).idAsignado;


		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		credencialesImgs = (ArrayList<CredImagen>) params.get("imagenes");

		// First, the NH informs the amount of image to send
		mensaje = new Mensaje(
			((AtributosHoja) this.atributos).getDireccion(),
			idAsignado,
			null,  // dirección a dónde enviar la rta
			3,  // TODO: este código no puede estar hardcodeado, tiene que salir de una constante
			credencialesImgs.size()
		);
		conexionConNodo.enviarSinRta(mensaje);

		// Then, it sends the image
		mensaje = new Mensaje(
				((AtributosHoja) this.atributos).getDireccion(),
				idAsignado,
				null,  // dirección a dónde enviar la rta
				3,  // TODO: este código no puede estar hardcodeado, tiene que salir de una constante
				credencialesImgs
		);
		Mensaje respuesta = (Mensaje) conexionConNodo.enviarConRta(mensaje);

		if ((Integer) respuesta.getCodigo() != Codigos.OK){
			System.out.printf("[Cli %s] falló anuncio de imágenes compartidas\n", this.idConsumidor);
			output.put("result", false);
		} else {
			System.out.printf("[Cli %s] compartidas %s imágenes\n", this.idConsumidor, credencialesImgs.size());
		}

		return output;
	}

	private HashMap<String, Object> descargarImgFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje solicitud;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		conexionConNodo.enviarSinRta(new Mensaje(
				this.atributos.getDireccion(),
				21,
				params.get("credImg")
		));
		
		System.out.printf("[Cli %s]", this.idConsumidor);
		System.out.printf(" solicitada imagen <%s> para descarga\n", ((CredImagen) params.get("credImg")).getNombre());
		return output;
	}

	private HashMap<String, Object> queryNCFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje solicitud;
		UUID idAsignado = ((AtributosHoja) this.atributos).getId((DireccionNodo) params.get("direccionNC"));
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		solicitud = new Mensaje(
				this.atributos.getDireccion(),
				idAsignado,
				this.atributos.getDireccion(),
				4,
				(CredImagen) params.get("credImg")
		)
		;
		conexionConNodo.enviarSinRta(solicitud);
		
		System.out.printf("[Cli %s]", this.idConsumidor);
		System.out.printf(" enviada consulta por <%s> a NC\n", ((CredImagen) params.get("credImg")).getNombre());
		return output;
	}

	private HashMap<String, Object> solicitarNCsFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		HashMap<String, Object> payload;
		Integer amount;
		LinkedList<String> centrales;
		Mensaje mensaje;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		// Determina la cantidad de NCs que debe solicitar
		amount = ((AtributosHoja) this.atributos).getCantCentrales() ;
		amount -= ((AtributosHoja) this.atributos).getCentrales().size();
		
		if (amount.equals(0))
			return output;

		// Lista de NCs actuales
		params.put("conocidos", new HashSet<DireccionNodo>(((AtributosHoja) this.atributos).getCentrales().keySet()));

		System.out.printf("[Cli %s]", this.idConsumidor);
		System.out.printf(" enviando solicitud por <%s> NCs a WKAN", amount);
		
		params.put("direccionNH", this.atributos.getDireccion());
		params.put("pendientes", amount);
		
		mensaje = (Mensaje) conexionConNodo.enviarConRta(new Mensaje(
				this.atributos.getDireccion(),
				Codigos.NH_NA_POST_SOLICITUD_NCS,
				params
		));
		
		// TODO: ver qué pasa acá con lo que encolo (idAsignado)
		if (mensaje.getCodigo().equals(Codigos.OK)) {
			for (DireccionNodo central : (ArrayList<DireccionNodo>) mensaje.getCarga()) {
				((AtributosHoja) atributos).encolarCentral(central, null);

				payload = new HashMap<String, Object>();
				payload.put("direccionNC", central);
				
				try {
					atributos.encolar("salida", new Tarea("ESTABLECER_CONEXION_NC", payload));
				} catch (InterruptedException e) {
					// No hago nada, hay una tarea periódica que solicia NCs si resta conectarse a alguno
				}
			}
		}
		
		// Registra el timestamp en que se esolicitudNCsfectuó la solicitud
		((AtributosHoja) this.atributos).solicitudNCs.setLastRequestNow();
		
		System.out.printf("\t[OK]\n");
		
		return output;
	}
		
	// Procesamiento de las tareas
	// ---------------------------------------------------------------------------------------------------
	@Override
	protected HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		// Variables "básicas" para procesar tareas
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		HashMap<String, Comparable> salida;
		Integer contador;
		Integer intentos;
		Integer puertoDestino;
		String ipDestino;
		
		// Variables propias de este Cliente
		
		// Inicializaciones de cortesía
		method = null;
		diccionario = null;
		salida = null;
		ipDestino = null;
		puertoDestino = null;
		
		// En casi todos los case hago lo mismo, lo único que varía siempre es la difinición del "method"
		switch(tarea.getName()){
			case "ANUNCIO":
				/* Informa a NC las imágenes compartidas */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((DireccionNodo) diccionario.get("direccionNC")).ip.getHostAddress();
				puertoDestino = ((DireccionNodo) diccionario.get("direccionNC")).puerto_nh;
				method = this::anunciarImgsFnc;	
				break;
			case "ESTABLECER_CONEXION_NC":
				/* Se conecta por primera vez a un NC, registrándose y recibiendo el ID de hoja que lo identificará */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((DireccionNodo) diccionario.get("direccionNC")).ip.getHostAddress();
				puertoDestino = ((DireccionNodo) diccionario.get("direccionNC")).puerto_nh;
				method = this::anunciarAnteNCFnc;
				break;
			case "SOLICITUD_NCS":
				/* Solicita al WKAN que oficia de pto de acceso a la red, una determinada cantidad de NCs a los que conectarse */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((DireccionNodo) diccionario.get("direccionWKAN")).ip.getHostAddress();
				puertoDestino = ((DireccionNodo) diccionario.get("direccionWKAN")).puerto_nh;
				method = this::solicitarNCsFnc;
				break;
			case "DESCARGA":
				/* Descarga (request en realidad) de una imagen particular del Nodo Hoja que la posee */
				
				/* diccionario = {
				 * 		"direccionNH": ip de la H que tiene la imagen que se va a solicitar descargar
				 * 		"credImg": la imagen que se va a solicitar descargar
				 * } */
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((DireccionNodo) diccionario.get("direccionNH")).ip.getHostAddress();
				puertoDestino = ((DireccionNodo) diccionario.get("direccionNH")).puerto_nh;
				method = this::descargarImgFnc;
				break;
			case "QUERY":
				/* Consulta a NC por imágenes similares a la dada cómo referencia */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((DireccionNodo) diccionario.get("direccionNC")).ip.getHostAddress();
				puertoDestino = ((DireccionNodo) diccionario.get("direccionNC")).puerto_nh;
				method = this::queryNCFnc;
				break;
		}
		
		contador = 0;
		intentos = 3;
		while (contador < intentos) {
			if (this.establecerConexion(ipDestino, puertoDestino)) {
				// Ahora llamo al método correspondiente para realizar la tarea (independientemente de cual sea)
				diccionario = method.apply(diccionario);
				contador = intentos + 1;
			} else {
				contador += 1;
				continue;
			}
		}

		if ((Boolean) diccionario.containsKey("callbackOnSuccess") 
			|| (Boolean) diccionario.containsKey("callbackOnFailure"))
			method.apply(diccionario);

		// TODO Ojo que estoy mandando una única dirección sin considerar qué tipo de Nodo es el destinatario 
		this.conexionConNodo.enviarSinRta(
				new Mensaje(
						this.atributos.getDireccion(),
						Codigos.CONNECTION_END,
						null
				)
		);

		this.terminarConexion();
		System.out.println("[Cli  " + this.idConsumidor + "] arrancando de nuevo inmediatamente");
		
		// TODO: hacelo bien
		return salida;
	}

}
	
