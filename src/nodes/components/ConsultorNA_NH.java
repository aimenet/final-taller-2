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

public class ConsultorNA_NH implements Consultor {
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
		Integer auxInt;
		Integer codigo;
		LinkedList<String> auxLstStr;
		LinkedList<String> auxLstStr2;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		String auxStr;
		Timestamp auxTimestamp;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
					case Codigos.NH_NA_POST_SOLICITUD_NCS:
						// Un NH solicita NCs a los que conectarse
						
						/* Mensaje = (dirección del NH para atender WKAN, 
						 *            código de tarea,
						 *            {'direccionNH_NC: xxx,
						 *            'direccionNH_NA': xxx,
						 *            'pendientes': xxx}
						 *            )
						 */
						
						System.out.printf("[Con NH] Solicitud de %s NCs por parte de NH en %s ", mensaje.getCarga(), mensaje.getEmisor());
						
						diccionario = (HashMap<String, Comparable>) mensaje.getCarga();
						
						// Obtrención de NCs que pueden recibir a la H: si no existen más WKANs en la red entonces buscará entre sus NCs la
						// cantidad solicitada, sino escogerá sólo 1 (y retransmitirá la consulta)
						auxInt = ((AtributosAcceso) atributos).getNodos().size() > 0 ? 1 : (Integer) diccionario.get("pendientes");
						// "Trae" el doble de lo requerido para aumentar las probabilidades de encontar un NC que no tenga ya al NH
						auxLstStr = funciones.getNCsConCapacidadNH(auxInt * 2);
						
						// Consulta al NC si cuenta con el NH entre sus filas, quedándose con aquellos que no lo posean
						auxLstStr2 = new LinkedList<String>();
						for (String dirNC : auxLstStr) {
							// Creo que acá tengo que instanciar un Cliente para que se conecte al NC y le consulte. No voy a poder
							// usar el método "atender" sino que voy a tener que hacer otro teniendo cuidado de que devuela OK o lo que 
							// sea que haga falta
						}
						
						
						buffSalida.writeObject(new Mensaje(atributos.getDireccion("hojas"), Codigos.OK, auxLstStr));
						
						System.out.println("[OK]");
						
						// Si no se cubrió la cantidad requerida de NCs encola la tarea para retransmitir la solicitud a otro WKAN
						if (Integer.parseInt((String) mensaje.getCarga()) - auxInt > 0) {
							if (((AtributosAcceso) atributos).getNodos().size() > 0) {
								diccionario.put("pendientes", (Integer) diccionario.get("pendientes") - auxInt);								
								atributos.encolar("salida", new Tarea("RETRANSMITIR_SOLICITUD_NCS_NH", diccionario));
								
								System.out.printf("[Con NH] Encolada retransmisión de solicitud de NH %s\n", mensaje.getEmisor());
							}
						}
						
						terminar = true;
						break;
					default:
						System.out.printf("[Con NH] Recibido mensaje de %s: %s\n",  mensaje.getEmisor(), mensaje.getCarga());
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
