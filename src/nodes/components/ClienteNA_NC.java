package nodes.components;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

import commons.DireccionNodo;
import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;

/**
 * Una de las instancias que compone la "faceta" Cliente de un Nodo de Acceso. Es la encargada de 
 * conectarse a otro Nodo de Acceso Bien Conocido e interactuar con él.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más como
 * "consumidores". Esto se debe principalmente a que cada instancia no se comunica con un nodo determinado
 * sino que establece conexiones temporales de acuerdo a la tarea que le haya tocado.
 * 
 * @author rodrigo
 * @since 2019-10-01
 */

/* [2020-03-01] -> esta clase es la que tengo que tomar como ejemplo cada vez que quiera hacer un Cliente */ 

public class ClienteNA_NC extends Cliente {
	// Atributos
	// =========
	private Callable<HashMap<String, Object>> funcion;
	//private ConexionTcp conexionConNodoCentral;
	//private boolean conexionEstablecida, sesionIniciada;
	public Integer idConsumidor;
	public String idAsignadoNA, tipoConsumidor;


	// Métodos
	// =======
	public ClienteNA_NC(int idConsumidor) {
		super(idConsumidor, "centrales");
		this.atributos = new AtributosAcceso();
	}
	
	
	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// ---------------------------------------------------------------------------------------------------
	// TODO: hacer una clase, interfaz o algo así
	private HashMap<String, Object> anuncioAceptadoFnc(HashMap<String, Object> params){
		/**
		 * Le indica a un NC que fue aceptado en la red, siendo éste el WKAN que lo administrará.
		 *
		 * params: {
		 *     "direccionNC": DireccionNodo,
		 * }
		 *
		 */
		HashMap<String, Object> output = new HashMap<String, Object>();
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", true);
		output.put("result", true);

		DireccionNodo nodo = (DireccionNodo) params.get("direccionNC");
		
		if (params.containsKey("callbackOnFailure") && (Boolean) params.get("callbackOnFailure")) {
			// No se pudo informar al NC. Se lo descarta
			((AtributosAcceso) atributos).desencolarCentral(nodo);
			
			System.out.print("\nConsumidor " + this.idConsumidor + ": ");
			System.out.printf("falló aununcio aceptación a NC %s. Descartado\n", nodo.ip.getHostName());
		} else {
			// Código "normal" que se ejecuta cuando sí se conecta al NC
			Mensaje saludo = new Mensaje(
					this.atributos.getDireccion(),
					Codigos.NA_NC_POST_ANUNCIO_ACEPTADO,
					null
			);

			this.conexionConNodo.enviarSinRta(saludo);

			System.out.printf("Consumidor %s: ", this.idConsumidor);
			System.out.printf("anunciada aceptación a NC %s\n", nodo.ip.getHostName());
		}
		
		return output;
	}
	
	private HashMap<String, Object> conectarNcsFnc(HashMap<String, Object> params) {
		/**
		 * Se informará la dirección de un NC (manejado por este nodo) al NC recientemente incorporado a la red.
		 *
		 * params: {
		 * 	"direccionNcNuevo": DireccionNodo. El NC recientemente ingresado a la red, quién solicita NCs vecinos
		 * 	"direccionNcVecino": DireccionNodo. El NC que será vecino del nuevo
		 * }
		 *
		 */
		HashMap<String, Object> output = new HashMap<String, Object>();

		DireccionNodo nodoNuevo = (DireccionNodo) params.get("direccionNcNuevo");
		DireccionNodo nodoVecino = (DireccionNodo) params.get("direccionNcVecino");

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		this.conexionConNodo.enviarSinRta(
				new Mensaje(
						this.atributos.getDireccion(),
						Codigos.NA_NC_POST_NC_VECINO,
						nodoVecino
				)
		);
		
		System.out.printf("[Cli %s]\t", this.idConsumidor);
		System.out.printf("anunciado NC vecino (%s) a ", nodoVecino.ip.getHostName());
		System.out.printf("%s\n", nodoNuevo.ip.getHostName());
		
		return output;
	}

	private HashMap<String, Object> consultaCapacidadNHFnc(HashMap<String, Object> params) {
		/**
		 * Consulta a un NC si tiene capacidad para recibir a un NH.
		 * Además se verifica que dicho NH no esté registrado en el NC.
		 *
		 * params = {
		 *     "direccionNH": DireccionNodo, el NH ingresante a la red
		 *     "direccionNC": DireccionNodo, el NC que atenderá al NH
		 * }
		 */
		HashMap<String, Object> output = new HashMap<String, Object>();

		DireccionNodo nodo = (DireccionNodo) params.get("direccionNH");

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		Mensaje msj = (Mensaje) this.conexionConNodo.enviarConRta(
				new Mensaje(
						this.atributos.getDireccion(),
						Codigos.NA_NC_POST_CAPACIDAD_NH,
						nodo
				)
		);

		output.put("status", (boolean) msj.getCarga());

		if (msj.getCodigo() != Codigos.OK)
			output.put("result", false);

		// Podría hacer un print para verificar

		return output;
	}

	private HashMap<String, Object> aceptarNHFnc(HashMap<String, Object> params) {
		/**
		 * La finalidad de esta tarea es simplemente ordearle al NC que se anuncie ante el NH que solicitó NCs
		 * por ende basta con informar su dirección, más nada.
		 *
		 * params = {
		 *     "direccionNH": DireccionNodo
		 *     "direccionNC": DireccionNodo
		 * }
		 */
		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		DireccionNodo nodoCentral = (DireccionNodo) params.get("direccionNC");
		DireccionNodo nodoHoja = (DireccionNodo) params.get("direccionNH");

		this.conexionConNodo.enviarSinRta(
				new Mensaje(
						this.atributos.getDireccion(),
						Codigos.NA_NC_POST_ACEPTAR_NH,
						nodoHoja
				)
		);

		System.out.print("[Cli  " + this.idConsumidor + "] Informado a NC " + nodoCentral.ip.getHostName());
		System.out.println(" que debe aceptar al NH " + nodoHoja.ip.getHostName());

		return output;
	}


	@Override
	protected HashMap<String, Comparable> procesarTarea(Tarea tarea) {
		DireccionNodo direccion;
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		HashMap<String, Comparable> diccionario2;
		Integer contador=0;
		Integer intentos=3; 
		Integer puertoNcDestino;
		String ipNcDestino;
		
		// Inicializaciones de cortesía
		method = null;
		diccionario = null;
		
		ipNcDestino = null;
		puertoNcDestino = null;
		
		switch(tarea.getName()){
			case "ANUNCIO-ACEPTADO":
				// Le indica a un NC que fue aceptado en la red, siendo éste el WKAN que lo administrará
				ipNcDestino = ((DireccionNodo) tarea.getPayload()).ip.getHostName();
				puertoNcDestino = ((DireccionNodo) tarea.getPayload()).puerto_nc;
				method = this::anuncioAceptadoFnc;
				diccionario = new HashMap<String, Object>();
				diccionario.put("direccionNC", tarea.getPayload());
				break;
			case "CONECTAR-NCS":
				// Se informará la dirección de un NC (manejado por este nodo) al NC recientemente incorporado a la red
				
				// payload = Tupla de strings. El primero es la dirección del NC que será vecino del solicitante.
				//           El segundo es la dirección del NC que solicita los vecinos

				diccionario = new HashMap<String, Object>();

				// NC nuevo en la red (quien solicita vecinos)
				direccion = ((Tupla2<DireccionNodo, DireccionNodo>) tarea.getPayload()).getSegundo();
				ipNcDestino = direccion.ip.getHostName();
				puertoNcDestino = direccion.puerto_na;
				diccionario.put("direccionNcNuevo", direccion);

				// NC existente que será vecino del nuevo
				direccion = ((Tupla2<DireccionNodo, DireccionNodo>) tarea.getPayload()).getPrimero();
				diccionario.put("direccionNcVecino", direccion);

				method = this::conectarNcsFnc;

				break;
			case "CAPACIDAD-ATENCION-NH":
				// Consulta a un NC si tiene capacidad para recibir a un NH. Además se verifica que dicho NH
				// no esté registrado en el NC

				// TODO 2020-09-27: revisar que la key del diccionario del payload, referida a la hoja, sea direccionNH

				diccionario = (HashMap<String, Object>) tarea.getPayload();
				direccion = (DireccionNodo) diccionario.get("direccionNC");
				ipNcDestino = direccion.ip.getHostName();
				puertoNcDestino = direccion.puerto_na;
				method = this::consultaCapacidadNHFnc;
				break;
			case "ACEPTAR-NH":
				// El WKAN le indica a un NC bajo su supervisión que debe anunciarse ante un NH a fin de establecer
				// conexión

				// TODO 2020-09-27: revisar que las key del diccionario del payload sean direccionNC y direccionNH

				diccionario = (HashMap<String, Object>) tarea.getPayload();
				direccion = (DireccionNodo) diccionario.get("direccionNC");
				ipNcDestino = direccion.ip.getHostName();
				puertoNcDestino = direccion.puerto_na;
				method = this::aceptarNHFnc;
				break;
		}
		
		contador = 0;
		while (contador < intentos) {
			if (this.establecerConexion(ipNcDestino, puertoNcDestino)) {
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
				new Mensaje(this.atributos.getDireccion(), Codigos.CONNECTION_END, null));
		this.terminarConexion();
		System.out.println("[Cli  " + this.idConsumidor + "] arrancando de nuevo inmediatamente");
		
		// TODO: hacelo bien
		diccionario2 = new HashMap<String, Comparable>();
		diccionario2.put("status", (Comparable) diccionario.get("result"));
		return diccionario2;
	}

}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de los threads consumidor, no sé por
 *              qué el notifyall() de los métodos de encolado/desencolado de tareas pareciera despertar siempre al
 *              mismo (por más que use retardos aleatorios para evitarlo)
 */
