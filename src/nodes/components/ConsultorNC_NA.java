package nodes.components;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;

public class ConsultorNC_NA implements Consultor {
	// De cada instancia
	private AtributosCentral atributos = new AtributosCentral();
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();
	
	
	@Override
	public void run() {
		try {
			this.atender();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}
	

	@Override
	public void atender() throws InterruptedException {
		Mensaje mensaje;
		boolean auxBol;
		boolean terminar = false;
		Integer codigo;
		Object auxObj;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		String auxStr;
		Tarea tarea;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
					case Codigos.PING:
						// PING - PONG para probar la conexión entre los nodos
						System.out.printf("Recibido PING de WKAN %s ", mensaje.getEmisor());
						buffSalida.writeObject(
								new Mensaje(atributos.getDireccion("acceso"), Codigos.PONG, null));
						break;
					case Codigos.NA_NC_POST_ANUNCIO_ACEPTADO:
						// Otro WKAN (no el asignado inicialmente) será el "administrador". Actualiza los datos
						System.out.printf("Recibido aceptación de WKAN en %s ", mensaje.getEmisor());
						atributos.setWKANAsignado(mensaje.getEmisor());
						break;
					case Codigos.NA_NC_POST_NC_VECINO:
						// Recibe la dirección de un NC al que conectarse
						//conectar con NC vecino usando (String) mensaje.getCarga();
						System.out.printf("[Con WKAN] ");
						System.out.printf("WKAN %s informó dirección de ", mensaje.getEmisor());
						System.out.printf("NC vecino %s\n", (String) mensaje.getCarga());
						tarea = new Tarea(00, "ANUNCIO-VECINO", (String) mensaje.getCarga());
						atributos.encolar("centrales", tarea);
					case Codigos.NA_NC_POST_CAPACIDAD_NH:
						// Evalúa la capacidad de aceptar un nuevo NH, siempre que no se encuentre ya registrado
						auxStr = (String) mensaje.getCarga();
						
						auxObj = atributos.getClavesIndiceHojas();
						auxBol = !((Set<String>) auxObj).contains(auxStr);
						auxBol = auxBol && ((Set<String>) auxObj).size() < this.atributos.getNHCapacity();
						auxObj = auxBol ? Codigos.OK : Codigos.ACCEPTED;  // TODO: es ACCEPTED la respuesta negativa correcta??
						
						buffSalida.writeObject(new Mensaje(atributos.getDireccion("acceso"), 
								                           (Integer) auxObj, auxBol));
						//terminar = true;
						break;
					case Codigos.CONNECTION_END:
						terminar = true;
						break;
					default:
						System.out.printf("\tRecibido mensaje en %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
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
