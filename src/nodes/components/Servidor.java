package nodes.components;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


/**
 * Servidor concurrente (multihilos) que instancia un manejador de clientes por cada conexión aceptada.
 * Dicho manejador será definido por la clase que lo instancie, lo que permite variarlo en función
 * de donde se desee implementar.
 * 
 * @author rodrigo
 *
 */
public class Servidor implements Runnable {
	 
	/* --------- */
	/* Atributos */
	/* --------- */
	private Class<?> claseConsultor;
	protected Consultor manejadorClientes;
	private Integer aux;
	private ServerSocket sockServidor;
	private String nombre;
	
	
	/* ------- */
	/* Métodos */
	/* ------- */
	public Servidor(Integer puerto, String nombre, Class<?> claseConsultor){
		try {
			InetAddress addr = InetAddress.getByName("127.0.0.2");

			this.claseConsultor = claseConsultor;
			this.sockServidor = new ServerSocket(puerto, 50, addr);
			this.nombre = nombre;
		} catch (IOException e) {e.printStackTrace();}
	}

	public Servidor(String ip, Integer puerto, String nombre, Class<?> claseConsultor){
		try {
			InetAddress addr = InetAddress.getByName(ip);

			this.claseConsultor = claseConsultor;
			this.sockServidor = new ServerSocket(puerto, 50, addr);
			this.nombre = nombre;
		} catch (IOException e) {e.printStackTrace();}
	}


	/**Método principal del servidor. Acepta conexiones de los distintos clientes, generando un hilo por c/u.*/
	protected void atender(){
		boolean run_flag = true;
		Socket sock;
		
		// TODO: guardar todos los sockests conectados al servidor.
		
		//System.out.println("Servidor concurrente " + this.nombre);
		//System.out.println("-----------------------------------");
		System.out.printf("[Server] [%s] corriendo en %s\n", this.nombre, Integer.toString(this.sockServidor.getLocalPort()));
		try {
			// Bucle principal donde recibe conexiones.
			while (run_flag) {
				sock = this.sockServidor.accept();
				
				System.out.println("-> Conexión con <" + sock.getInetAddress().getHostAddress() + ":" + sock.getPort() +"> aceptada.");
				
				try {
					Consultor consultor = (Consultor) claseConsultor.newInstance();
					consultor.setSock(sock);
					//Creación del hilo que atenderá al cliente.
					(new Thread(consultor)).start();					
				} catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				
			}
			System.out.println("\n-> Servidor apagado");
		} catch (IOException e) {e.printStackTrace();}
	}

	
	@Override
	public void run() {
		atender();
		
	}
	

} // Fin clase
