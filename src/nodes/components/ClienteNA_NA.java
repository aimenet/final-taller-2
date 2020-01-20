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


/* [2020-01-16] Esta clase usá de ejemplo cuando quieras hacer un Cliente, HDP!!!! */


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

	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		boolean success;
		HashMap<String, Comparable> salida;
		HashMap<String, Object> hashmapPayload;
		Integer contador = 0;
		Integer intentos = 3;
		Integer puertoDestino;
		Integer status = 0;
		LinkedList<String> strList;
		Object lock;
		String ipDestino;
		String ncDestino;
		String wkanDestino;

		salida = null;
		
		switch (tarea.getName()) {
			case "ANUNCIO":
				System.out.printf("Consumidor %s: ejecutando ANUNCIO\n", this.id);
				// Se "presenta" ante otro NABC (comunicando su IP) a fin de ingresar a la red
				contador = 0;
				success = false;
				ipDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
	
				while ((contador < intentos) && (!success)) {
					if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
						// Si bien todos los WKAN deberían escuchar en el mismo puerto, para poder
						// correr más de uno
						// en local tengo que usar sí o sí distintos puertos
						Mensaje saludo = new Mensaje(this.atributos.getDireccion("acceso"), Codigos.NA_NA_POST_SALUDO,
								null);
						Mensaje respuesta = (Mensaje) this.conexionConNodoAcceso.enviarConRta(saludo);
	
						if (respuesta.getCodigo() == 200) {
							success = true;
							((AtributosAcceso) this.atributos).activarNodo((String) tarea.getPayload());
							// System.out.print("\nConsumidor " + this.idConsumidor + ": ");
							// System.out.println("aununciado a WKAN " + (String) tarea.getPayload());
							System.out.printf("Consumidor %s: anunciado a WKAN %s\n", this.id, (String) tarea.getPayload());
						} else {
							contador = intentos + 1; // fuerza a terminar pues se rechazó el anuncio
						}
					} else {
						contador += 1;
						continue;
					}
				}
	
				if (!success) {
					// No pudo establecerse conexión. Se encola una tarea de reintento
					atributos.encolar("salida", new Tarea(00, "REANUNCIO", (String) tarea.getPayload()));
					System.out.print("\nConsumidor " + this.id + ": ");
					System.out.println("falló 1er aununcio a WKAN " + (String) tarea.getPayload());
				}
				break;
			case "REANUNCIO":
				System.out.printf("Consumidor %s: ejecutando REANUNCIO\n", this.id);
				// Hace lo mismo que en "ANUNCIO" sólo que da por inalcanzable al NODO si no
				// logra conectarse.
				// TODO: mover código a un métod, hoy está duplicado con el de "ANUNCIO"
				contador = 0;
				success = false;
				ipDestino = ((String) tarea.getPayload()).split(":")[0];
				puertoDestino = Integer.parseInt(((String) tarea.getPayload()).split(":")[1]);
	
				while ((contador < intentos) && (!success)) {
					if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
						// No hace falta especificar nombre ni carga del mensaje, simplemente el destino
						// sabrá
						// que hay un WKAN que se quiere conectar y sacará la IP del socket. Si quisiera
						// informar
						// un nombre podría usar un GUID
						Mensaje saludo = new Mensaje(null, Codigos.NA_NA_POST_SALUDO, null);
						Mensaje respuesta = (Mensaje) this.conexionConNodoAcceso.enviarConRta(saludo);
	
						if (respuesta.getCodigo() == Codigos.OK) {
							success = true;
							((AtributosAcceso) this.atributos).activarNodo((String) tarea.getPayload());
						} else {
							contador = intentos + 1; // fuerza a terminar pues se rechazó el anuncio
						}
					} else {
						contador += 1;
						continue;
					}
				}
	
				if (!success) {
					((AtributosAcceso) this.atributos).setKeepaliveNodo((String) tarea.getPayload(), 0);
					System.out.print("\nConsumidor " + this.id + ": ");
					System.out.println("falló reintento de aununcio a WKAN " + (String) tarea.getPayload());
				}
				break;
			case "INFORMAR_WKANS":
				// Dispara una serie de tareas (una por nodo activo) para informar el estado
				// actual de la red (conocida por él)
				LinkedList<String> nodosActivos = new LinkedList<String>();
				Tupla2<String, LinkedList<String>> nodoAInformar;
				contador = 0;
	
				System.out.printf("Consumidor %s: ejecutando INFORMAR_WKANS\n", this.id);
	
				for (Entry<String, Integer> me : ((AtributosAcceso) this.atributos).getNodos().entrySet()) {
					// No considero a los nodos que alguna vez no hayan dado indicios de estar
					// activos
					if (me.getValue() == ((AtributosAcceso) this.atributos).keepaliveNodoVecino)
						nodosActivos.add(me.getKey());
				}
	
				for (String nodo : nodosActivos) {
					nodoAInformar = new Tupla2<String, LinkedList<String>>(nodo, nodosActivos);
					atributos.encolar("salida", new Tarea(00, "INFORMAR_CONOCIDOS", nodoAInformar));
					contador += 1;
				}
	
				System.out.printf("Consumidor %s: disparadas %s tareas de INFORMAR_CONOCIDOS\n", this.id, contador);
				break;
			case "INFORMAR_CONOCIDOS":
				// <ip nodo destino, listado nodos activos>
				Tupla2<String, LinkedList<String>> payload = (Tupla2<String, LinkedList<String>>) tarea.getPayload();
	
				// Envía a un WKAN el listado de los nodos que tiene certeza están estrictamente
				// activos
				System.out.printf("Consumidor %s: ejecutando INFORMAR_CONOCIDOS a %s", this.id, payload.getPrimero());
	
				ipDestino = payload.getPrimero().split(":")[0];
				puertoDestino = Integer.parseInt(payload.getPrimero().split(":")[1]);
	
				if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
					// No hace falta especificar nombre, el destino lo sabe por el socket
					Mensaje listado = new Mensaje(this.atributos.getDireccion("acceso"), Codigos.NA_NA_POST_ANUNCIO_ACTIVOS,
							payload.getSegundo());
					this.conexionConNodoAcceso.enviarSinRta(listado);
					System.out.printf(" [COMPLETADO]\n");
				} else {
					status = ((AtributosAcceso) this.atributos).getStatusNodo(payload.getPrimero());
					status = status <= 0 ? 0 : status - 1;
					((AtributosAcceso) this.atributos).setKeepaliveNodo(payload.getPrimero(), status);
					System.out.printf(" [FALLIDO]\n");
				}
				break;
			case "RETRANSMITIR_ANUNCIO_NC":
				// Al no poder "hacerse cargo" de un NC retransmite la consulta a 1 de los WKANs
				// conocidos
				// (elegido aleatoriamente) para que se haga cargo.
				// Escojo uno de forma random para:
				// 1) no controlar que 2 (o más) WKANs que reciben la retransmisión se hagan
				// cargo del mismo nodo
				// 2) no saturar la red con tantos mensajes
				// Para evitar problemas se setea un TTL al mensaje a fin de que alcanzado el
				// mismo se informe
				// al NC que nadie puede hacerse cargo de él.
	
				String nc_pendiente = (String) tarea.getPayload();
				wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();
	
				if (wkanDestino != null) {
					ipDestino = wkanDestino.split(":")[0];
					puertoDestino = Integer.parseInt(wkanDestino.split(":")[1]);
		
					System.out.printf("Consumidor %s: retransmitiendo anuncio NC a %s", this.id, wkanDestino);
		
					if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
						this.conexionConNodoAcceso.enviarSinRta(new Mensaje(this.atributos.getDireccion("acceso"),
								Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC, nc_pendiente));
						System.out.printf(" [COMPLETADO]\n");
					} else {
						status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
						status = status <= 0 ? 0 : status - 1;
						((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);
						System.out.printf(" [FALLIDO]\n");
					}
				} else {
					System.out.printf("No se encontraron WKANs para retransmitir anuncio del NC %s\n", nc_pendiente);
				}
	
				break;
			case "SOLICITAR_NCS_VECINOS":
				// Se aceptó un NC y ahora debe comenzar la transmisión del mensaje entre WKANs
				// para solicitar
				// NCs a los que enlazar al recién integrado.
	
				// El mensaje se envía de a un WKAN a la vez, elegido aleatoriamente, para no
				// inundar la red.
				// Se definen una cantidad de saltos máxima (3 veces la cantidad de NCs a los
				// que se conecta un NC)
				// y se registran los NABC por los que pasa el mensaje a fin de no duplicar la
				// rta.
				// Si el WKAN posee un NC que pueda ser vecino del solicitante se lo informa
				// directamente.
				// La rta del NC indicará si aún no completó la cantidad requerida de vecinos
				// para que el WKAN
				// retransmita el mensaje o lo descarte, según corresponda.
				// Aquél WKAN que reciba el mensaje de forma repetida simplemente lo
				// retransmitirá.
				hashmapPayload = (HashMap<String, Object>) tarea.getPayload();
				ncDestino = (String) hashmapPayload.get("ncDestino");
				wkanDestino = null;
				
				// Armado del payload de la consulta (donde está toda la información de "control" para todo esto
				// El payload puede tener distintos formatos, dependiendo de quién haya generado la tarea
				if (!hashmapPayload.containsKey("preparado")) {
					strList = new LinkedList<String>();
					strList.add(atributos.getDireccion("acceso"));
	
					hashmapPayload = new HashMap<String, Object>();
					hashmapPayload.put("consultados", strList);
					hashmapPayload.put("saltos", 9);
					hashmapPayload.put("ncDestino", ncDestino);
					hashmapPayload.put("ncsRestantes", 3); // TODO: esto no tiene que estar hardcodeado
				}
				
				// Valida que el WKAN elegido no haya sido consultado previamente
				contador = 5;
				while (contador > 0) {
					wkanDestino = ((AtributosAcceso) atributos).getRandomNABC();
					if ( !((LinkedList<String>) hashmapPayload.get("consultados")).contains(wkanDestino) ) {
						contador = 0;
					} else {
						wkanDestino = null;
					}
				}
		
				System.out.printf("[Cli %s]\t", this.id);
				// Si no conoce ningún WKAN (o fallaron todos los intentos de elegir uno no visitado) no hace nada.
				if (wkanDestino != null) {
					ipDestino = wkanDestino.split(":")[0];
					puertoDestino = Integer.parseInt(wkanDestino.split(":")[1]);
		
					System.out.printf("enviando solicitud de vecinos para nuevo NC a %s", wkanDestino);
					if (this.establecerConexionConNodoAcceso(ipDestino, puertoDestino)) {
						this.conexionConNodoAcceso.enviarSinRta(new Mensaje(this.atributos.getDireccion("acceso"),
								Codigos.NA_NA_POST_SOLICITUD_VECINOS_NC, hashmapPayload));
						System.out.printf(" [COMPLETADO]\n");
					} else {
						status = ((AtributosAcceso) this.atributos).getStatusNodo(wkanDestino);
						status = status <= 0 ? 0 : status - 1;
						((AtributosAcceso) this.atributos).setKeepaliveNodo(wkanDestino, status);
						System.out.printf(" [FALLIDO]\n");
					}
				} else {
					System.out.printf("no se encontró WKAN al que enviar solicitud de vecinos\n");
				}
				break;
			}
		return salida;
	}

	private boolean establecerConexionConNodoAcceso(String ip, Integer puerto) {
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

	private boolean terminarConexionConNodoAcceso() {
		if (this.conexionEstablecida) {
			this.conexionConNodoAcceso.cerrar();
			this.conexionEstablecida = false;
		}

		return this.conexionEstablecida;
	}
}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de
 * los threads consumidor, no sé por qué el notifyall() de los métodos de
 * encolado/desencolado de tareas pareciera despertar siempre al mismo (por más
 * que use retardos aleatorios para evitarlo)
 */
