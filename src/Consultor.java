import java.net.Socket;

/**
 * Interface que define los métodos que implementarán los hilos generados por los Servidores para atender a 
 * los Clientes.
 * @author rodrigo
 *
 */
public interface Consultor extends Runnable {
	
	// Método principal de todo hilo "consultor". Recibe y procesa consultas del cliente conectado.
	void atender();
	
	// Setea el socket del cliente conectado, al cual deberá atender.
	void setSock(Socket sock);
	
	// Devuelve la dirección del socket conectado (el otro extremo), en formato "amigable".
	String sockToString();
}
