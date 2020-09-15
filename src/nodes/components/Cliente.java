package nodes.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import commons.Codigos;
import commons.ConexionTcp;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;
import my_exceptions.ManualInterruptException;

/** Clase que implementa los métodos básicos del Cliente de un Nodo, el cual actúa como "consumidor" y permite la
 * comunicación con otros nodos (independientemente de su tipo).
 * 
 * 
 * @author rorro
 * @since 2019-10-30
 */

public abstract class Cliente implements Runnable {
	protected Atributos atributos;
	protected boolean conexionEstablecida, sesionIniciada;
	protected ConexionTcp conexionConNodo;
	protected Integer id;
	protected String cola;
	
	// Constructores
	// -------------
	protected Cliente(int id, String cola) {
		// [2019-10-30] en esta primera versión sólo consumira de una cola en particular
		this.cola = cola;
		this.id = id;
	}
	
	
	
	// Métodos relacionados a las tares que ejecuta un Cliente
	// -------------------------------------------------------
	@Override
	public void run() {
		boolean runFlag = true;
		
		while (runFlag) {
			try{
				consumir();
			} catch (ManualInterruptException ex){
				// Excepción para detener el hilo
				ex.printStackTrace();
				runFlag = false;
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	protected void consumir() throws InterruptedException, ManualInterruptException {
		Tarea tarea = null;
		
		// Si hago esto, el uso del CPU sube levemente pero me permite que trabajen todos los threads consumidores,
		// sino, pasa lo que detallé al final del archivo -[2019-10-19]-
		while(this.atributos.colaVacia(this.cola)) {
			Random rand = new Random();
			Thread.sleep(rand.nextInt(3000));
    	}
		
		System.out.printf("Cliente %s: esperando\n", this.id);
		tarea = atributos.desencolar(this.cola);  // thread-safe
		
		// Éste método lo van a sobreescribir cada una de las clases que implementen Cliente, para que procesen
		// las tareas particulares según la necesidad
		procesarTarea(tarea);
		
		// TODO: terminar conexión actual en curso
		System.out.println("\nCliente " + this.id + " arrancando de nuevo inmediatamente");
	}
	
	// Lo declaro como abstract para forzar a las clases que hereden de esta a que lo sobreescriban
	protected abstract HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException;

	
	// Métodos relacionados a la conexión que establece con el Nodo destino
	// --------------------------------------------------------------------
	// Métodos de conexiones
	protected boolean establecerConexion(String ip, Integer puerto) {
		if (this.conexionEstablecida)
			this.terminarConexion();
		// TODO 2020-09-10: esto es para debuggear en la misma máquina, no tengo que especificar ip+port locales si esto
		//                  corriera en prod
		try {
			//this.conexionConNodo = new ConexionTcp(ip, puerto); // prod
			this.conexionConNodo = new ConexionTcp(ip, puerto, atributos.getDireccion(null), 0); // debug
			this.conexionEstablecida = true;
		} catch (IOException e) {
			System.out.printf("[Cli %s] No se pudo establecer conexión con el servidor", this.id);
			conexionEstablecida = false;
		}

		return this.conexionEstablecida;
	}

	
	protected boolean terminarConexion() {
		if (this.conexionEstablecida) {
			this.conexionConNodo.cerrar();
			this.conexionEstablecida = false;
		}

		return this.conexionEstablecida;
	}



}
