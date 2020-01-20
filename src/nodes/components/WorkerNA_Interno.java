package nodes.components;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.Random;
import my_exceptions.ManualInterruptException;
import commons.Codigos;
import commons.ConexionTcp;
import commons.Mensaje;
import commons.Tarea;
import commons.Tupla2;

/**
 * Clase que se encarga de las tareas internas de un WKAN.
 * 
 * No es un Cliente propiamente dicho pues no establece comunicación con ningún servidor de la red. Sin embargo
 * por practicidad hago que herede de esa clase pues ya implementa los métodos para consumir tareas de 
 * una cola determinada.
 * 
 * @author rodrigo
 * @since 2020-01-09
 */

public class WorkerNA_Interno extends Cliente {
	// Atributos
	// =========
	public String idAsignadoNA, tipoConsumidor;

	// Métodos
	// =======
	public WorkerNA_Interno(int idConsumidor) {
		super(idConsumidor, "interna");
		this.atributos = new AtributosAcceso(); // <atributos> está declarado en Cliente
	}

	@Override
	public void procesarTarea(Tarea tarea) throws InterruptedException {
		boolean success;
		HashMap<String, Object> diccionario;
		HashMap<String, Object> hashmapPayload;
		Integer contador = 0;
		Integer intentos = 3;
		Integer puertoDestino;
		Integer status = 0;
		LinkedList<String> strList;
		Long auxLong;
		Object lock;
		String ipDestino;
		String ncDestino;
		String wkanDestino;
		Timestamp currentTimestamp;

		switch (tarea.getName()) {
			case "CHECK_KEEPALIVE_NCS":
				// Evalúa el estado de los NCs de acuerdo a la última conexión establecida con cada uno
				
				System.out.printf("[Wrk %s] Check keepalive NCs ", this.id);
				contador = 0;
				currentTimestamp = new Timestamp(System.currentTimeMillis());
				
				for (Entry<String, HashMap<String, Comparable>> me : ((AtributosAcceso) this.atributos).getCentrales().entrySet()) {
					// Computa el tiempo transcurrido (en milisegundos) desde la última comunicación
					// con el NC
					auxLong = currentTimestamp.getTime() - ((Timestamp) me.getValue().get("timestamp")).getTime();
					
					// Si el delta supera un determinado tiempo (en segundos) se marca como inactivo el NC
					if (TimeUnit.SECONDS.convert(auxLong, TimeUnit.MILLISECONDS) > ((AtributosAcceso) this.atributos).keepaliveNC) {
						me.getValue().put("alive", false);
					} else {
						me.getValue().put("alive", true);
						contador += 1;
					}
				}
				
				System.out.println("[OK]");
				System.out.printf("[Wrk %s] %s de %s NCs activos\n", this.id, contador, ((AtributosAcceso) this.atributos).getCentrales().size());
				break;
			}
	}
}

