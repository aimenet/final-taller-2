package nodes.components;
import java.net.Socket;

/**
 * Interface que define los métodos que implementarán los hilos generados por los Servidores para atender a 
 * los Clientes.
 * 
 * @author rodrigo
 * @since 2019-10-06
 */

// [2019-10-30] Podría hacer algo similar a lo que hice con Cliente para que esto sea más útil

public interface Consultor extends Runnable {
	
	// Método principal de todo hilo "consultor". Recibe y procesa consultas del cliente conectado.
	void atender();
	
	// Setea el socket del cliente conectado, al cual deberá atender.
	void setSock(Socket sock);
	
	// Devuelve la dirección del socket conectado (el otro extremo), en formato "amigable".
	String sockToString();
}

/** 
 * Más que Interface debería ser una clase de la que hereden las demás (teniendo sí una interfaz que defina
 * métodos requeridos) ya que los métodos setSock y sockToString -por ejemplo- son iguales para todos los consultores
 * 
 * */ 