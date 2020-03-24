package nodes.components;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import my_exceptions.ManualInterruptException;
import commons.Codigos;
import commons.ConexionTcp;
import commons.CredImagen;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;

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

public class HojaConsumidor extends Cliente {
	// Atributos
	// =========
	private AtributosHoja atributos;
	private ConexionTcp conexionConNodoCentral;
	private boolean conexionEstablecida, sesionIniciada;
	public Integer idConsumidor, puertoNC;
	public String idAsignadoNC, ipNC;
	

	// Métodos
	// =======
	public HojaConsumidor(int idConsumidor) {
		super(idConsumidor, "centrales");
		this.atributos = new AtributosHoja();
	}
	
	
	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// ---------------------------------------------------------------------------------------------------
	private HashMap<String, Object> anunciarAnteNCFnc(HashMap<String, Object> params){
		ArrayList<CredImagen> credencialesImgs;
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje mensaje;
		Mensaje respuesta;
		String ipServerPropia;
		String token;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		ipServerPropia = this.atributos.getIpServidor() + ":" + this.atributos.getPuertoServidor().toString();
		
		// Si existe un ID de Hoja definido en los atributos, se envía un mensaje de reconexión.
		// En caso contrario se envía un saludo
		token = this.atributos.getId(this.idConsumidor);
		if (token == null || token.length() == 0) {
			// Saludo, 1ra conexión
			mensaje = new Mensaje(null,1, ipServerPropia);
		} else {
			// Saludo de reconexión
			mensaje = new Mensaje(null,1, "##"+token+"##");
		}
		
		respuesta = (Mensaje) conexionConNodo.enviarConRta(mensaje);
		
		if (respuesta.getCodigo().equals(1) && respuesta.getCarga() != null){
			//La respuesta contiene el ID con el que se identificará al Cliente.
			idAsignadoNC = respuesta.getCarga().toString();
			sesionIniciada = true;
			
			// Seteo del ID que recibió la H en los atributos comartidos para ser conocido por todos los
			// "componentes" del nodo Hoja. Si se trataba de una reconexión el ID será igual así que en realidad es
			// redundante ese paso
			atributos.setId(this.idConsumidor, idAsignadoNC);
		} else {
			output.put("result", false);
		}
		
		return output;
	}

	private HashMap<String, Object> anunciarImgsFnc(HashMap<String, Object> params){
		ArrayList<CredImagen> credencialesImgs;
		HashMap<String, Object> output = new HashMap<String, Object>();
		
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		credencialesImgs = (ArrayList<CredImagen>) params.get("imagenes");
		
		conexionConNodo.enviarSinRta(new Mensaje(this.idAsignadoNC, 3, credencialesImgs.size()));
		
		Mensaje respuesta = (Mensaje) conexionConNodo.enviarConRta(new Mensaje(this.idAsignadoNC, 3, credencialesImgs));
		
		// Si carga del mensaje = 0 -> recibió todo OK, si = 1 -> algo salió mal.
		if ((Integer) respuesta.getCarga() != 0){
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
		String direccionRecepcionRta;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		direccionRecepcionRta = this.atributos.getIpServidor() + ":" + this.atributos.getPuertoServidor().toString();
		conexionConNodo.enviarSinRta(new Mensaje(null, direccionRecepcionRta, 21, params.get("credImg")));
		
		System.out.printf("[Cli %s]", this.idConsumidor);
		System.out.printf(" solicitada imagen <%s> para descarga\n", ((CredImagen) params.get("credImg")).getNombre());
		return output;
	}

	private HashMap<String, Object> queryNCFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje solicitud;
		String direccionRecepcionRta;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);
		
		direccionRecepcionRta = this.atributos.getIpServidor() + ":" + this.atributos.getPuertoServidor().toString();
		conexionConNodo.enviarSinRta(new Mensaje(idAsignadoNC, direccionRecepcionRta, 4, (CredImagen) params.get("credImg")));
		
		System.out.printf("[Cli %s]", this.idConsumidor);
		System.out.printf(" enviada consulta por <%s> a NC\n", ((CredImagen) params.get("credImg")).getNombre());
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
				ipDestino = ((String) diccionario.get("direccionNC")).split(":")[0];
				puertoDestino = Integer.parseInt(((String) diccionario.get("direccionNC")).split(":")[1]);
				method = this::anunciarImgsFnc;	
				break;
			case "ESTABLECER_CONEXION_NC":
				/* Se conecta por primera vez a un NC, registrándose y recibiendo el ID de hoja que lo identificará */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((String) diccionario.get("direccionNC")).split(":")[0];
				puertoDestino = Integer.parseInt(((String) diccionario.get("direccionNC")).split(":")[1]);
				method = this::anunciarAnteNCFnc;
				break;
			case "DESCARGA":
				/* Descarga (request en realidad) de una imagen particular del Nodo Hoja que la posee */
				
				/* diccionario = {
				 * 		"direccionNH": ip de la H que tiene la imagen que se va a solicitar descargar
				 * 		"credImg": la imagen que se va a solicitar descargar
				 * } */
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((String) ((HashMap<String,Object>) tarea.getPayload()).get("direccionNH")).split(":")[0];
				puertoDestino = Integer.parseInt(((String) diccionario.get("direccionNH")).split(":")[1]);
				method = this::descargarImgFnc;
				break;
			case "QUERY":
				/* Consulta a NC por imágenes similares a la dada cómo referencia */
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipDestino = ((String) diccionario.get("direccionNC")).split(":")[0];
				puertoDestino = Integer.parseInt(((String) diccionario.get("direccionNC")).split(":")[1]);
				method = this::queryNCFnc;
				break;
			case "STOP":
				// Provisorio -> naturalmente a fines académicos
				// Lanzo una excepción para capturarla y detener el thread
				// throw new ManualInterruptException("Forzada detención del thread", 1);
				throw new InterruptedException("Forzada detención del thread");
				// break;
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

		this.conexionConNodo.enviarSinRta(
				new Mensaje(String.format("%s:%s", this.atributos.getIpServidor(), this.atributos.getPuertoServidor().toString()), 
						    Codigos.CONNECTION_END, 
						    null));
		this.terminarConexion();
		System.out.println("[Cli  " + this.idConsumidor + "] arrancando de nuevo inmediatamente");
		
		// TODO: hacelo bien
		return salida;
	}

}
	
