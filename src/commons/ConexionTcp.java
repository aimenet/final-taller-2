package commons;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.InputMismatchException;
import java.util.Random;

/** Clase que le permite a un Nodo Hoja (Cliente) conectarse a un Nodo Central (Servidor).
 * 
 * El método principal es comunicarse() mediante el cual se invocan los restantes.
 * 
 * La conexión (TCP) con el Servidor se lleva a cabo en el constructor.
 * */

public class ConexionTcp /*implements  InterfazConexion*/ {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private Socket sock;
	private ObjectOutputStream buffSalida;
	private ObjectInputStream buffEntrada;


	/*---------*/
	/* Métodos */
	/*---------*/
	//Constructor. Creo que debería arrojar una escepción si no puede establecer conexión.
	public ConexionTcp(String ip, Integer puerto) throws UnknownHostException, IOException{
		sock = new Socket(ip, puerto);
		buffSalida = new ObjectOutputStream(sock.getOutputStream());
		buffEntrada = new ObjectInputStream(sock.getInputStream());

//		String origen = sock.getLocalAddress().getHostAddress() + ":" + sock.getLocalPort();
//		String destino = sock.getInetAddress().getHostAddress() + ":" + sock.getPort();
//		String aux = "<" + sock.getInetAddress().getHostAddress() +  
//				":" + sock.getLocalPort() +"> Conexión iniciada";
//		System.out.println(String.format("*** Socket conectando <%s> con <%s>", origen, destino));
	}


	public Object enviarConRta(Serializable carga){
		Object respuesta;
		
		respuesta = null;
		try {
//			String origen = sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort();
//			String destino = sock.getInetAddress().getHostName() + ":" + sock.getPort();
//			System.out.println(String.format("Voy a conectar <%s> con <%s>", origen,destino));

			buffSalida.writeObject(carga);
			respuesta = (Object) buffEntrada.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		return respuesta;
		
	}


	public boolean enviarSinRta(Serializable carga){
		boolean enviado;
		
		enviado = true;
		try {
			buffSalida.writeObject(carga);
		} catch (IOException e) {
			e.printStackTrace();
			enviado = false;
		}
		
		return enviado;
	}


	public Object recibir(){
		Object respuesta;
		
		respuesta = null;
		try {
			respuesta = (Object) buffEntrada.readObject();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		
		return respuesta;
		
	}
	
	
	//@Override
	public boolean cerrar(){
		try {
			sock.close();
			buffSalida.close();
			buffEntrada.close();
			return true;
		} catch (IOException e) {return false;}
	}
	
}// Fin clase

