package nodes.components;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;

public class ConsultorNA_NC implements Consultor {
	// De cada instancia
	private AtributosAcceso atributos = new AtributosAcceso();
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();
	
	
	@Override
	public void run() {
		this.atender();	
	}
	

	@Override
	public void atender() {
		Mensaje mensaje;
		boolean terminar = false;
		HashMap<String, Comparable> diccionario;
		Integer codigo;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		Timestamp auxTimestamp;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				// Si no se trata de un NC nuevo, registra que está activo
				if (((AtributosAcceso) atributos).getCentrales().containsKey(mensaje.getEmisor())) {
					auxTimestamp = new Timestamp(System.currentTimeMillis());
					((AtributosAcceso) atributos).getCentrales().get(mensaje.getEmisor()).put("timestamp", auxTimestamp);
					((AtributosAcceso) atributos).getCentrales().get(mensaje.getEmisor()).put("alive", true);
				}
				
				switch(mensaje.getCodigo()){
					case Codigos.NC_NA_POST_ANUNCIO:
						/* Mensaje = (dirección del NC para atender WKAN,
					  	              código de tarea,
					  	              diccionario con direcciones servidor del NC)*/
						
						System.out.printf("Recibido anuncio de NC en %s\n", mensaje.getEmisor());
						
						diccionario = (HashMap<String, Comparable>) mensaje.getCarga();
						
						codigo = funciones.atenderAnuncioNC(mensaje.getEmisor(), 
															(String) diccionario.get("direccionNC_NC"),
															(String) diccionario.get("direccionNC_NH"),
															true);
						buffSalida.writeObject(new Mensaje(null, codigo, null));
						
						if (codigo == Codigos.OK) {
							System.out.printf("Aceptado NC %s\n", (String) mensaje.getEmisor());	
						} else if (codigo == Codigos.ACCEPTED) {
							System.out.printf("Capacidad max NC alcanzada, "); 
							System.out.printf("retransmitiendo anuncio de NC %s\n", mensaje.getEmisor());
						}
						
						terminar = true;
						break;
					case Codigos.NC_NA_POST_KEEPALIVE:
						// El NC informa que está activo. No debería llegar un mensaje de un NC no registrado ya que esto debería ser posterior
						// al anuncio
						System.out.printf("[Con NC] Recibido keepalive de NC %s", mensaje.getEmisor());
						
						diccionario = ((AtributosAcceso) this.atributos).getCentrales().get(mensaje.getEmisor());
						
						if (diccionario == null) {
							System.out.println(" [ERROR] (NC desconocido)");
						} else {
							diccionario.put("alive", true);
							diccionario.put("timestamp", new Timestamp(System.currentTimeMillis()));
							System.out.println(" [OK]");
						}

						terminar = true;
						break;
					default:
						System.out.printf("\tRecibido mensaje en %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
			e.printStackTrace();
		}
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

}
