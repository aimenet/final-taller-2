package nodes.components.clientes;

import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;

import commons.*;
import commons.mensajes.Mensaje;
import nodes.components.atributos.AtributosCentral;


/**
 * Clase Cliente de un Nodo Central, encargada de la comunicación con otro NC.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más 
 * como "consumidores". Esto se debe principalmente a que cada instancia no se comunica con un nodo determinado
 * sino que establece conexiones temporales de acuerdo a la tarea que le haya tocado.
 * 
 * @author rorro
 * @since 2019-11-28
 */

public class ClienteNC_NC extends Cliente {
	// Atributos
	// -----------------------------------------------------------------------------------------------------------------


	// Métodos
	// -----------------------------------------------------------------------------------------------------------------
	public ClienteNC_NC(int idConsumidor, String cola) {
		super(idConsumidor, cola);
		this.atributos = new AtributosCentral();
	}

	private HashMap<String, Object> anuncioVecinoFnc(HashMap<String, Object> params){
		/**
		 * params = {
		 *     "direccionNC": DireccionNodo
		 * }
		 * */
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje enviado, recibido;

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);

		// Asumo que va a salir todo bien
		output.put("result", true);

		enviado = new Mensaje(this.atributos.getDireccion(), Codigos.NC_NC_POST_SALUDO, null);
		recibido = (Mensaje) this.conexionConNodo.enviarConRta(enviado);

		DireccionNodo vecino = (DireccionNodo) params.get("direccionNC");

				// Si el código no es OK (200), independientemente de por qué no se ha aceptado la "vinculación" no hago nada.
		System.out.printf("[Cli %s]\t", this.id);
		System.out.printf("registrado nuevo NC vecino: %s ", vecino.ip.getHostAddress());

		if (recibido.getCodigo() == Codigos.OK) {
			if (((AtributosCentral) atributos).getCentrales().size() < ((AtributosCentral) atributos).getMaxCentralesVecinos()) {
				((AtributosCentral) atributos).indexarCentral(vecino);
				System.out.printf("[OK]\n");
			} else {
				System.out.printf("[OK pero sin capacidad]\n");
			}
		} else {
			System.out.println("[ERROR] (No aceptado)\n");
		}

		System.out.printf(
				"\tEstado actual: %d de %d NCs\n",
				atributos.getNcs().size(),
				((AtributosCentral) atributos).getMaxCentralesVecinos()
		);

		return output;
	}

	private HashMap<String, Object> conectarNcsFnc(HashMap<String, Object> params){
		/**
		 * params = {
		 *     "direccionNcVecino": DireccionNodo
		 * }
		 * */
		HashMap<String, Object> output = new HashMap<String, Object>();

		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		output.put("result", true);

		Mensaje msj = new Mensaje(
				this.atributos.getDireccion(),
				Codigos.NA_NC_POST_NC_VECINO,
				(String) params.get("direccionNcVecino")
		);
		msj = (Mensaje) this.conexionConNodo.enviarConRta(msj);

		System.out.println("revisá params a ver cual es la dir que tenés que registrar");

		System.out.printf("Consumidor %s: ", this.id);
		System.out.printf("registrado NC (%s) como vecino ", (String) params.get("direccionNcVecino"));

		if (msj.getCodigo() == Codigos.OK) {
			System.out.println("[COMPLETADO]");
			((AtributosCentral) atributos).indexarCentral((DireccionNodo) params.get("direccionNcVecino"));
		} else {
			System.out.println("[ERROR]");
		}

		System.out.println("revisá los NCs que tiene este registrado a ver qué tul");
		return output;
	}

	@Override
	protected HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		Integer contador=0; 
		Integer intentos=3; 
		Integer puertoNcDestino;
		Object generico;
		String auxStr;
		String ipNcDestino;
		
		// Inicializaciones de cortesía
		method = null;
		diccionario = null;
		
		// TODO: recordá que existe esto!
		// Si hago esto, el uso del CPU sube levemente pero me permite que trabajen todos los threads consumidores,
		// sino, pasa lo que detallé al final del archivo -[2019-10-19]-
		while(this.atributos.colaVacia("centrales")) {
			Random rand = new Random();
			Thread.sleep(rand.nextInt(3000));
    	}
		
		System.out.printf("Consumidor %s: esperando\n", this.id);
		tarea = atributos.desencolar("centrales"); // thread-safe
		
		ipNcDestino = null;
		puertoNcDestino = null;
		
		switch(tarea.getName()){
			case Constantes.TSK_NC_ANUNCIO_VECINO:
				// Le indica a un NC que puede usar a éste NC como uno de sus vecinos
				DireccionNodo destino = (DireccionNodo) tarea.getPayload();
				ipNcDestino = destino.ip.getHostAddress();
				puertoNcDestino = destino.puerto_nc;
				method = this::anuncioVecinoFnc;
				diccionario = new HashMap<String, Object>();
				diccionario.put("direccionNC", tarea.getPayload());
				break;
			default:
				System.out.println("Entré al default por: " + tarea.getName());
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
		System.out.println("\nConsumidor " + this.id + " arrancando de nuevo inmediatamente");

		return null;  // 2020-11-06: o esto está mal o el método debería retornar void
	}


}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de los threads consumidor, no sé por
 *              qué el notifyall() de los métodos de encolado/desencolado de tareas pareciera despertar siempre al
 *              mismo (por más que use retardos aleatorios para evitarlo)
 */
