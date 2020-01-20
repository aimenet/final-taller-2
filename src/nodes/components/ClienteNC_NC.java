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
 * Clase Cliente de un Nodo Central, encargada de la comunicación con otro NC.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más 
 * como "consumidores". Esto se debe principalmente a que cada instancia no se comunica con un nodo determinado
 * sino que establece conexiones temporales de acuerdo a la tarea que le haya tocado.
 * 
 * @author rorro
 * @since 2019-11-28
 */

public class ClienteNC_NC implements Runnable {
	// Atributos
	// =========
	private AtributosCentral atributos;
	private Callable<HashMap<String, Object>> funcion;
	private ConexionTcp conexionConNodoCentral;
	private boolean conexionEstablecida, sesionIniciada;
	public Integer idConsumidor;
	public String tipoConsumidor;


	// Métodos
	// =======
	public ClienteNC_NC(int idConsumidor) {
		this.atributos = new AtributosCentral();
		this.idConsumidor = idConsumidor;
	}

	@Override
	public void run() {
		boolean runFlag = true;
		
		while (runFlag) {
			try{
				consumir();
			} catch (ManualInterruptException ex){
				// Excepción para detener el hilo
				ex.printStackTrace();
				runFlag = false;
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	
	private void consumir() throws InterruptedException, ManualInterruptException {
		Function<HashMap<String, Object>, HashMap<String, Object>> method;
		HashMap<String,Object> diccionario;
		Integer contador=0; 
		Integer intentos=3; 
		Integer puertoNcDestino;
		Object generico;
		String auxStr;
		String ipNcDestino;
		Tarea tarea = null;
		
		// Inicializaciones de cortesía
		method = null;
		diccionario = null;
		
		// Si hago esto, el uso del CPU sube levemente pero me permite que trabajen todos los threads consumidores,
		// sino, pasa lo que detallé al final del archivo -[2019-10-19]-
		while(this.atributos.colaVacia("centrales")) {
			Random rand = new Random();
			Thread.sleep(rand.nextInt(3000));
    	}
		
		System.out.printf("Consumidor %s: esperando\n", this.idConsumidor);
		tarea = atributos.desencolar("centrales"); // thread-safe
		
		ipNcDestino = null;
		puertoNcDestino = null;
		
		switch(tarea.getName()){
			case "ANUNCIO-VECINO":
				// Le indica a un NC que puede usar a éste NC como uno de sus vecinos
				ipNcDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoNcDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
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
			if (this.establecerConexionConNodoCentral(ipNcDestino, puertoNcDestino)) {
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

		this.conexionConNodoCentral.enviarSinRta(
				new Mensaje(this.atributos.getDireccion("centrales"), Codigos.CONNECTION_END, null));
		this.terminarConexionConNodoCentral();
		System.out.println("\nConsumidor " + this.idConsumidor + " arrancando de nuevo inmediatamente");
	}
	
	
	// TODO: hacer una clase, interfaz o algo así
	private HashMap<String, Object> anuncioVecinoFnc(HashMap<String, Object> params){
		HashMap<String, Object> output = new HashMap<String, Object>();
		Mensaje enviado, recibido;
		
		// Estos son comunes a todas las funciones
		output.put("callBackOnSuccess", false);
		output.put("callBackOnFailure", false);
		
		// Asumo que va a salir todo bien
		output.put("result", true);
		
		enviado = new Mensaje(this.atributos.getDireccion("centrales"), Codigos.NC_NC_POST_SALUDO, null);
		recibido = (Mensaje) this.conexionConNodoCentral.enviarConRta(enviado);
		
		// Si el código no es OK (200), independientemente de por qué no se ha aceptado la "vinculación"
		// no hago nada.
		// TODO: hacer tarea periódica que controle si falta enlazarse a NCs y dispare tarea de pedidos de vecinos
		// (hacer que esa tarea pida de a 1, así es más fácil)
		System.out.printf("[Cli %s]\t", this.idConsumidor);
		System.out.printf("registrado nuevo NC vecino: %s ", (String) params.get("direccionNC"));
		if (recibido.getCodigo() == Codigos.OK) {
			if (atributos.getCentrales().size() < atributos.getMaxCentralesVecinos()) {
				atributos.indexarCentral((String) params.get("direccionNC"));
				System.out.printf("[OK]\n");
			} else {
				System.out.printf("[OK pero sin capacidad]\n");
			}
		} else {
			System.out.printf("[ERROR]\n");
			System.out.println("No me aceptaron, acordate de hacer tarea periódica que pida un vecino nuevo");
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
							      (String) params.get("direccionNcVecino"));
		msj = (Mensaje) this.conexionConNodoCentral.enviarConRta(msj);
		
		System.out.println("revisá params a ver cual es la dir que tenés que registrar");
		
		System.out.printf("Consumidor %s: ", this.idConsumidor);
		System.out.printf("registrado NC (%s) como vecino ", (String) params.get("direccionNcVecino"));
		
		if (msj.getCodigo() == Codigos.OK) {
			System.out.println("[COMPLETADO]");
			atributos.indexarCentral((String) params.get("direccionNcVecino"));
		} else {
			System.out.println("[ERROR]");
		}
		
		System.out.println("revisá los NCs que tiene este registrado a ver qué tul");
		return output;
	}
	
	
	// TODO: generalizar, porque lo usan todos los Clientes (y sería bueno tenerlo en los Consultores para conexiones
	//       rápidas)
	// Ojo, es una encapsulación de una encapsulación (ConexionTcp) así que podría prescindir de esto
	// porque en realidad nunca uso las "cosas extras" que hace.
	private boolean establecerConexionConNodoCentral(String ip, Integer puerto){
		if (this.conexionEstablecida)
			this.terminarConexionConNodoCentral();
		
		try {
			this.conexionConNodoCentral = new ConexionTcp(ip, puerto);
			this.conexionEstablecida = true;
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el servidor del NC");
			conexionEstablecida = false;
		}
		
		return this.conexionEstablecida;
	}
	
	
	private boolean terminarConexionConNodoCentral(){
		if (this.conexionEstablecida) {
			this.conexionConNodoCentral.cerrar();
			this.conexionEstablecida = false;
		}
		
		return this.conexionEstablecida;
	}
}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de los threads consumidor, no sé por
 *              qué el notifyall() de los métodos de encolado/desencolado de tareas pareciera despertar siempre al
 *              mismo (por más que use retardos aleatorios para evitarlo)
 */
