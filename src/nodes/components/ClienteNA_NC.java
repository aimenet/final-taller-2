package nodes.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Function;

import my_exceptions.ManualInterruptException;
import commons.Codigos;
import commons.ConexionTcp;
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
		HashMap<String, Object> output = new HashMap<String, Object>();
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", true);
		
		// Asumo que va a salir todo bien
		output.put("result", true);
		
		if (params.containsKey("callbackOnFailure") && (Boolean) params.get("callbackOnFailure")) {
			// No se pudo informar al NC. Se lo descarta
			((AtributosAcceso) atributos).desencolarCentral((String) params.get("direccionNC"));
			
			System.out.print("\nConsumidor " + this.idConsumidor + ": ");
			System.out.printf("falló aununcio aceptación a NC %s. Descartado\n", (String) params.get("direccionNC"));
		} else {
			// Código "normal" que se ejecuta cuando sí se conecta al NC
			Mensaje saludo = new Mensaje(this.atributos.getDireccion("centrales"), 
					                     Codigos.NA_NC_POST_ANUNCIO_ACEPTADO, null);
			this.conexionConNodo.enviarSinRta(saludo);
			System.out.printf("Consumidor %s: anunciada aceptación a NC %s\n", this.idConsumidor, (String) params.get("direccionNC"));
		}
		
		return output;
	}
	
	private HashMap<String, Object> conectarNcsFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		
		output.put("result", true);
		Mensaje msj = new Mensaje(this.atributos.getDireccion("centrales"), 
							      Codigos.NA_NC_POST_NC_VECINO, 
							      (String) params.get("direccionNC"));
		this.conexionConNodo.enviarSinRta(msj);
		
		System.out.printf("[Cli %s]\t", this.idConsumidor);
		System.out.printf("anunciado NC vecino (%s) a ", (String) params.get("direccionNcVecino"));
		System.out.printf("%s\n", (String) params.get("direccionNcNuevo"));
		
		return output;
	}
	
	private HashMap<String, Object> consultaCapacidadNH(String direccionNH_NC) {
		HashMap<String, Object> output = new HashMap<String, Object>();
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		
		output.put("result", true);
		Mensaje msj = new Mensaje(this.atributos.getDireccion("centrales"), 
							      Codigos.NA_NC_POST_CAPACIDAD_NH, 
							      direccionNH_NC);
		msj = (Mensaje) this.conexionConNodo.enviarConRta(msj);
		output.put("status", (boolean) msj.getCarga());
		
		return output;
	}
	
	
	@Override
	protected HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		boolean flag;
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		HashMap<String, Comparable> diccionario2;
		HashMap<String, Comparable> salida;
		Integer contador=0; 
		Integer intentos=3; 
		Integer puertoNcDestino;
		Integer status=0;
		Object generico;
		Object lock;
		String auxStr;
		String ipNcDestino;
		
		// Inicializaciones de cortesía
		method = null;
		diccionario = null;
		salida = null;
		
		ipNcDestino = null;
		puertoNcDestino = null;
		
		switch(tarea.getName()){
			case "ANUNCIO-ACEPTADO":
				// Le indica a un NC que fue aceptado en la red, siendo éste el WKAN que lo administrará
				ipNcDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoNcDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
				method = this::anuncioAceptadoFnc;
				diccionario = new HashMap<String, Object>();
				diccionario.put("direccionNC", tarea.getPayload());
				break;
			case "CONECTAR-NCS":
				// Se informará la dirección de un NC (manejado por este nodo) al NC recientemente incorporado a la red
				
				// payload = Tupla de strings. El primero es la dirección servidor de NC del NC que será
				//           vecino del solicitante. El segundo es la dirección servidor de WKAN del NC
				//           que solicita los vecinos 
				
				generico = (Tupla2<String,String>) tarea.getPayload();
				auxStr = (String) ((Tupla2<String,String>) generico).getSegundo();
				ipNcDestino = auxStr.split(":")[0];
				puertoNcDestino = Integer.parseInt(auxStr.split(":")[1]);
				diccionario = new HashMap<String, Object>();
				diccionario.put("direccionNcNuevo", auxStr); 
				diccionario.put("direccionNcVecino", (String) ((Tupla2<String,String>) generico).getPrimero());
				method = this::conectarNcsFnc;
				break;
			case "CAPACIDAD-ATENCION-NH":
				// Consulta a un NC si tiene capacidad para recibir a un NH. Además se verifica que dicho NH
				// no esté registrado en el NC
				
				diccionario = (HashMap<String, Object>) tarea.getPayload();
				ipNcDestino = ((String) diccionario.get("direccionNC")).split(":")[0];
				puertoNcDestino = Integer.parseInt(((String) diccionario.get("direccionNC")).split(":")[1]);
				method = this::conectarNcsFnc;
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
				new Mensaje(this.atributos.getDireccion("centrales"), Codigos.CONNECTION_END, null));
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
