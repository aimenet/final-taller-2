package nodes.components;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import commons.Tarea;
import commons.DireccionNodo;
import nodes.components.atributos.AtributosAcceso;
import nodes.components.clientes.Cliente;

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
	// -----------------------------------------------------------------------------------------------------------------
	public String idAsignadoNA, tipoConsumidor;

	// Métodos
	// -----------------------------------------------------------------------------------------------------------------
	public WorkerNA_Interno(int idConsumidor) {
		super(idConsumidor, "interna");
		this.atributos = new AtributosAcceso(); // <atributos> está declarado en Cliente
	}

	private void beacon() {
		System.out.println("Beacon signal");
	}

	private HashMap<String, Object> checkKeepalivesFnc() {
		/**
		 * Evalúa el estado de los NCs de acuerdo a la última conexión establecida con cada uno.
		 *
		 */
		HashMap<String, Object> output = new HashMap<String, Object>();
		output.put("result", true);

		System.out.printf("[Wrk %s] Check keepalive NCs ", this.id);
		Integer contador = 0;
		Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

		HashMap<DireccionNodo, HashMap<String, Comparable>> centrales = ((AtributosAcceso) this.atributos).getCentrales();

		for (Entry<DireccionNodo, HashMap<String, Comparable>> me : centrales.entrySet()) {
			// Computa el tiempo transcurrido (en milisegundos) desde la última comunicación con el NC
			Long delta = currentTimestamp.getTime() - ((Timestamp) me.getValue().get("timestamp")).getTime();

			// Si el delta supera un determinado tiempo (en segundos) se marca como inactivo el NC
			if (TimeUnit.SECONDS.convert(delta, TimeUnit.MILLISECONDS) > ((AtributosAcceso) this.atributos).keepaliveNC) {
				me.getValue().put("alive", false);
			} else {
				me.getValue().put("alive", true);
				contador += 1;
			}
		}

		System.out.println("[OK]");
		System.out.printf("[Wrk %s] %s de ", this.id, contador);
		System.out.printf("%s NCs activos\n", ((AtributosAcceso) this.atributos).getCentrales().size());

		return output;
	}

	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		switch (tarea.getName()) {
			case "CHECK_KEEPALIVE_NCS":
				// Evalúa el estado de los NCs de acuerdo a la última conexión establecida con cada uno
				this.checkKeepalivesFnc();
				break;
			case "BEACON":
				this.beacon();
				break;
		}

		return null;
	}
}

