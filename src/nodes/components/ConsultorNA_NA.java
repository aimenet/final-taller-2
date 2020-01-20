package nodes.components;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;

/**
 * Consultor que corre en hilos generado por el Servidor de Nodos de Acceso. Es el
 * encargado de la atención propiamente dicha de otros Nodos de Acceso.
 * 
 * @author rodrigo
 * @since 2019-10-06
 */

public class ConsultorNA_NA implements Consultor {
	/* --------- */
	/* Atributos */
	/* --------- */
	// De cada instancia
	private AtributosAcceso atributos = new AtributosAcceso();
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();
	
	// De la clase
	
	
	/* --------------- */
	/* Métodos Propios */
	/* --------------- */
	//public ConsultorNC_NC(){}


	/* --------------------- */
	/* Métodos de Interfaces */
	/* --------------------- */
	@Override
	public void atender() {
		HashMap<String, HashMap<String,Comparable>> centralesRegistrados;
		HashMap<String, Object> diccionario;
		Integer codigo;
		Integer contador;
		Integer emisor;
		Integer intentos;
		Integer puertoDestino;
		LinkedList<String> confirmados;
		Mensaje mensaje;
		boolean terminar = false;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		String ipDestino;
		String strAux;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
					case Codigos.NA_NA_POST_SALUDO:
						System.out.printf("Recibido anuncio de WKAN en %s ", mensaje.getEmisor());
						buffSalida.writeObject(new Mensaje(null, Codigos.OK, null));
						System.out.printf("[CONFIRMADO]\n");
						this.atributos.activarNodo(mensaje.getEmisor());
						break;
					case Codigos.NA_NA_POST_ANUNCIO_ACTIVOS:
						System.out.printf("Recibido listado de WKAN en %s", mensaje.getEmisor());
						confirmados = (LinkedList<String>) mensaje.getCarga();
						confirmados.add(mensaje.getEmisor());
						
						// TODO: oportunidad de mejora
						confirmados.remove(this.atributos.getDireccion("acceso"));
						for (String nodo : confirmados)
							this.atributos.activarNodo(nodo);
						
						System.out.printf(" [ACTUALIZADO]\n");
						break;
					case Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC:
						// Anuncio (retransmitido por un WKAN sin capacidad) de un NC
						
						/* carga = {"ncDestino_NA": ...,
									"ncDestino_NC": ...} */

						diccionario = (HashMap<String, Object>) mensaje.getCarga();
						codigo = funciones.atenderAnuncioNC((String) diccionario.get("ncDestino_NA"), 
															(String) diccionario.get("ncDestino_NC"),
															false);
						
						if (codigo == Codigos.OK) {
							// Acá tengo que conectarme con el servidor de WKAN del NC que se anunció e informarle
							// que yo lo acepto.
							System.out.printf("Aceptado NC %s. Comunicando\n", (String) diccionario.get("ncDestino_NA"));	
							atributos.encolar("centrales", new Tarea(00, "ANUNCIO-ACEPTADO", (String) diccionario.get("ncDestino_NA")));
						} else if (codigo == Codigos.ACCEPTED) {
							System.out.printf("Capacidad max NC alcanzada, rechazado NC %s\n", (String) diccionario.get("ncDestino_NA"));
						}
						break;
				case Codigos.NA_NA_POST_SOLICITUD_VECINOS_NC:
						/* Evaluar si algún NC posee capacidad de enlazarse 
						 * a otro NC para comunicarlo con el recientemente incorporado a la red*/
						diccionario = (HashMap<String, Object>) mensaje.getCarga();
						centralesRegistrados = atributos.getCentrales();
						
						/* Acordate que en diccionarios tengo: 
						 * {...
						 * 	"ncDestino": la dirección del NC que pidió vecinos, 
						 *               donde atiende WKAN y en la que le voy a decir cuál NC va a ser su vecino
						 * ...} 
						 */
						
						// Registra en el diccionario enviado durante la solicitud que éste nodo ya ha sido consultado
						diccionario.put("saltos", (Integer) diccionario.get("saltos")-1);
						confirmados = (LinkedList<String>) diccionario.get("consultados");
						confirmados.add(atributos.getDireccion("acceso"));
						diccionario.put("consultados", confirmados);
						
						for (String key : centralesRegistrados.keySet()) {
							Integer activos = (int)centralesRegistrados.get(key).get("centrales_activos");
							Integer maximos = (int) centralesRegistrados.get(key).get("centrales_max");
							strAux = (String) centralesRegistrados.get(key).get("direccion_NC");
							// Esta dirección es la del NC que será vecino del solicitante
							
							if ((boolean) centralesRegistrados.get(key).get("alive"))
								if (activos < maximos) {
									atributos.encolar("centrales", 
										new Tarea(
											00, 
											"CONECTAR-NCS", 
											new Tupla2<String, String>(strAux, (String) diccionario.get("ncDestino"))
									    )
									);

									// Se marca el anuncio del NC vecino.
									diccionario.put("ncsRestantes", (Integer) diccionario.get("ncsRestantes")-1);
									// Marca especial en el diccionario para conocer que se originó acá
									diccionario.put("preparado", true);
									break;
								}
						}
						
						// Si restan NCs por conectar se retransmite nuevamente el mensaje
						if ((Integer) diccionario.get("ncsRestantes") > 0 && (Integer) diccionario.get("saltos") > 0) {
							// marco para que se reutilice este diccionario en lugar de crear uno nuevo
							diccionario.put("preparado", true);
							atributos.encolar("salida", new Tarea(00, "SOLICITAR_NCS_VECINOS", diccionario));
						}
						
						System.out.println("Procesada solicitud de vecinos para NC [OK]");
						break;
					default:
						System.out.printf("\tAnuncio de nodo %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
		}
	}
	

	@Override
	public void run() {
		this.atender();
		
	}


	@Override
	public void setSock(Socket sock) {
		this.sock = sock;
		
		// ¿Será este el problema?
		
		String origen = sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort();
		String destino = sock.getInetAddress().getHostName() + ":" + sock.getPort();
		//System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
	}


} // Fin clase

