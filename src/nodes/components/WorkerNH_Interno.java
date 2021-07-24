package nodes.components;

import commons.Constantes;
import commons.DireccionNodo;
import commons.Tarea;
import nodes.components.atributos.AtributosHoja;
import nodes.components.clientes.Cliente;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Clase que se encarga de las tareas internas de un Nodo Hoja.
 * 
 * No es un Cliente propiamente dicho pues no establece comunicación con ningún servidor de la red. Sin embargo
 * por practicidad hago que herede de esa clase pues ya implementa los métodos para consumir tareas de 
 * una cola determinada.
 *	-> Señal de que hay que refactorizar y hacer una clase solo con la parte de colas de tareas
 * 
 * @author rodrigo
 * @since 2021-07-24
 */

public class WorkerNH_Interno extends Cliente {
	public WorkerNH_Interno(int idConsumidor) {
		super(idConsumidor, Constantes.COLA_INTERNA);
		this.atributos = new AtributosHoja();
	}

	private void beacon() {
		System.out.println("Beacon signal");
	}

	private void checkConexionNCs() throws InterruptedException {
		Integer difference = ((AtributosHoja) atributos).getCantCentrales();
		difference -= ((AtributosHoja) atributos).getCentrales().keySet().size();

		System.out.printf("[Wrk %s] NCs connection check: ", this.id);

		if (difference > 0) {
			long timeElapsed = Duration.between(
					((AtributosHoja) atributos).solicitudNCs.getLastRequest(),
					Instant.now()
			).toSeconds();

			if (timeElapsed > ((AtributosHoja) atributos).solicitudNCs.getLastDelay()) {
				HashMap<String, Object> payload = new HashMap<String, Object>();
				payload.put("direccionWKAN", ((AtributosHoja) atributos).getWkanInicial());
				payload.put("primeraVez", false);
				atributos.encolar("salida", new Tarea("SOLICITUD_NCS", payload));

				((AtributosHoja) atributos).solicitudNCs.setNextDelay();

				System.out.printf(" asked %d nodes\t[OK]\n", difference);
			}
		}
	}

	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		switch (tarea.getName()) {
			case Constantes.TSK_NH_NC_CONNECTED_CHECK:
				// Controla que el nodo esté conectado a la cantidad requerida de NCs, disparando tarea de solicitud al
				// WKAN de ser necesario
				this.checkConexionNCs();
				break;
			case "BEACON":
				this.beacon();
				break;
			case "STOP":
				// Provisorio (a fines académicos). Lanzo una excepción para capturarla y forzar la detención del thread
				throw new InterruptedException("Forzada detención del thread");
		}

		return null;
	}
}

