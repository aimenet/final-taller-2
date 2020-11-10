package nodes.components;

import commons.*;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Clase Cliente de un Nodo Central, encargada de la comunicación con Nodos Hoja
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más 
 * como "consumidores". Esto se debe principalmente a que cada instancia no se comunica con un nodo determinado
 * sino que establece conexiones temporales de acuerdo a la tarea que le haya tocado.
 * 
 * @author rorro
 * @since 2020-08-01
 */

public class ClienteNC_NH extends Cliente {
	// Atributos
	// -----------------------------------------------------------------------------------------------------------------
	public Integer idConsumidor;


	// Métodos
	// -----------------------------------------------------------------------------------------------------------------
	public ClienteNC_NH(int idConsumidor) {
		super(idConsumidor, "centrales");
		this.atributos = new AtributosAcceso();
	}


	// Métodos que se usan para atender los distintos tipos de órdenes recibidas en una Tarea
	// --------------------------------------------------------------------------------------
	// TODO: hacer una clase, interfaz o algo así
	private HashMap<String, Object> anuncioDireccionANHFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		Mensaje mensaje = new Mensaje(
				this.atributos.getDireccion(),
				Codigos.NC_NH_POST_ANUNCIO,
				this.atributos.getDireccion()
		);

		this.conexionConNodo.enviarSinRta(mensaje);

		return output;
	}


	@Override
	protected HashMap<String, Comparable> procesarTarea(Tarea tarea) {
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		HashMap<String, Comparable> diccionario2;
		Integer contador;
		Integer intentos=3;
		Integer puertoNcDestino;
		String ipNcDestino;

		// Inicializaciones de cortesía
		method = null;
		diccionario = null;

		ipNcDestino = null;
		puertoNcDestino = null;

		switch(tarea.getName()){
			case "INFORMAR-DIRECCION-A-NH":
				// Comunica dirección y disponibilidad de aceptación a un NH

				ipNcDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoNcDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
				method = this::anuncioDireccionANHFnc;
				diccionario = new HashMap<String, Object>();
				diccionario.put("direccionNH", tarea.getPayload());
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
				new Mensaje(
						this.atributos.getDireccion(),
						Codigos.CONNECTION_END,
						null
				)
		);

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
