package nodes.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import my_exceptions.ManualInterruptException;
import commons.Codigos;
import commons.ConexionTcp;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;

/**
 * Una de las instancias que compone la "faceta" Cliente de un Nodo Central. Es la encargada de 
 * conectarse a un Nodo de Acceso Bien Conocido e interactuar con él.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más como
 * "consumidores".
 * 
 * @author rodrigo
 * @since 2019-11-02
 */

public class ClienteNC_NA extends Cliente {
	// Atributos
	// =========
	private ConexionTcp conexionConNodoAcceso;
	private boolean conexionEstablecida;
	public String idAsignadoNA, tipoConsumidor;


	// Métodos
	// =======
	public ClienteNC_NA(int idConsumidor) {
		super(idConsumidor, "acceso");  // la cola de la que consume debería recibirla como parámetro?
		this.atributos = new AtributosCentral();  // <atributos> está declarado en Cliente
	}

	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		boolean success;
		HashMap<String, Comparable> diccionario;
		HashMap<String, Comparable> output;
		Integer contador = 0; 
		Integer intentos = 3;
		Integer auxInt, puertoDestino;
		Integer status = 0;
		Object lock;
		String auxStr, ipDestino;
		
		output = null;
		
		switch(tarea.getName()){
			case "ANUNCIO-WKAN":
				System.out.printf("Consumidor %s: ejecutando ANUNCIO-WKAN\n", this.id);
				// Se "presenta" ante un NABC (comunicando su IP) a fin de ingresar a la red
				contador = 0;
				success = false;
				ipDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
				
				while ((contador < intentos) && (!success)) {
					if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
						System.out.printf("Anunciando a WKAN %s", (String) tarea.getPayload());
						
						// Al WKAN le informará las direcciones de su servidor de: WKAN, NC y NH
						diccionario = new HashMap<String, Comparable>();
						diccionario.put("direccionNC_NA", this.atributos.getDireccion("acceso"));
						diccionario.put("direccionNC_NC", this.atributos.getDireccion("centrales"));
						diccionario.put("direccionNC_NH", this.atributos.getDireccion("hojas"));
						
						// Si bien todos los WKAN deberían escuchar en el mismo puerto, para poder correr más de uno
						// en local tengo que usar sí o sí distintos puertos
						Mensaje saludo = new Mensaje(this.atributos.getDireccion("acceso"), 
								                     Codigos.NC_NA_POST_ANUNCIO, 
								                     diccionario);
						Mensaje respuesta = (Mensaje) this.conexionConNodoAcceso.enviarConRta(saludo);
						
						if (respuesta.getCodigo() == Codigos.OK) {
							System.out.printf(" [OK]\n");
						} else if (respuesta.getCodigo() == Codigos.ACCEPTED) {
							// el wkan no tiene capacidad para aceptarme pero retransmitió la consulta.
							// Tengo que esperar que algún WKAN se comunique para decir que me aceptó
							System.out.printf(" [ERROR]\n");
							System.out.println("Iniciando espera de aceptación");
						}
						
						// Cualquiera haya sido la respuesta, termina el bucle
						success = true;
						
					} else {
						contador += 1;
						continue;
					}
				}
				
				if (!success) {
					// No pudo establecerse conexión. Se encola una tarea de reintento
					//atributos.encolar("salida", new Tarea(00, "REANUNCIO", (String) tarea.getPayload()));
					System.out.print("\nConsumidor " + this.id + ": ");
					System.out.println("falló 1er aununcio a WKAN " + (String) tarea.getPayload());
					
					// Marca el timestamp de último intento de acceso para reintentar si expira sin haber
					// podido ingresar a la red
					((AtributosCentral) atributos).marcarIntentoConexionWKAN();
					
					// TODO: necesito una cola temporal donde haya tareas con delay. El hilo que la controle debe
					// estar revisando constantemente cuando expire el delay de alguna tarea para encolarla en la cola
					// definitiva
					
					// Esto queda truncado por ahora, el NC va a esperar que un WKAN se comunique para indicar que lo
					// acepte, pero si eso no pasa no intentará más entrar a la red
				}
				
				// TODO: terminar la conexión con WKAN
				break;
			case "SEND_KEEPALIVE_WKAN":
				// Tarea mediante la que se le informa al WKAN que este nodo está "vivo"
				
				ipDestino = ((AtributosCentral) atributos).getWKANAsignado().split(":")[0];
				puertoDestino = Integer.parseInt(((AtributosCentral) atributos).getWKANAsignado().split(":")[1]);
				
				System.out.printf("[Cli WKAN %s] ", this.id);
				System.out.printf("Envío keepalive WKAN %s ", ((AtributosCentral) atributos).getWKANAsignado());
				
				if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
					Mensaje saludo = new Mensaje(this.atributos.getDireccion("acceso"), 
							                     Codigos.NC_NA_POST_KEEPALIVE, 
							                     this.atributos.getDireccion("centrales")); // innecesaria
					this.conexionConNodoAcceso.enviarSinRta(saludo);
					
					System.out.println("[OK]");
				} else {
					// Acá no le doy mucha vuelta porque esta es una tarea periódica
					System.out.println("[ERROR]");
				}
				break;
		}
		
		this.terminarConexionConNodoAcceso();
		
		return output;
	}
	
	
	private boolean establecerConexionConNodoAcceso(String ip, Integer puerto){
		if (this.conexionEstablecida)
			this.terminarConexionConNodoAcceso();
		
		try {
			this.conexionConNodoAcceso = new ConexionTcp(ip, puerto);
			this.conexionEstablecida = true;
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el servidor");
			conexionEstablecida = false;
		}
		
		return this.conexionEstablecida;
	}
	
	private boolean terminarConexionConNodoAcceso(){
		if (this.conexionEstablecida) {
			this.conexionConNodoAcceso.cerrar();
			this.conexionEstablecida = false;
		}
		
		return this.conexionEstablecida;
	}
}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de los threads consumidor, no sé por
 *              qué el notifyall() de los métodos de encolado/desencolado de tareas pareciera despertar siempre al
 *              mismo (por más que use retardos aleatorios para evitarlo)
 *              
 * [2019-11-09] Necesito un nuevo tipo de cola: una donde las tareas tengan un delay o un timestamp en que deben
 *              ser encoladas en la cola definitiva para su procesamiento             
 */
