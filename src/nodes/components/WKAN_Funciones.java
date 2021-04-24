package nodes.components;

import java.util.*;

import commons.Codigos;
import commons.Tarea;
import commons.DireccionNodo;
import nodes.components.atributos.AtributosAcceso;

/** 
 * Clase que engloba funciones, algoritmos y similares, propios de un WKAN y que son utilizado por más
 * de un Consultor y/o Cliente
 * 
 * @author rorro
 * @since 2019-11-09
 * */

public class WKAN_Funciones {
	// Atributos
	// -------------------------------------------------------------------------------------------------------------
	private AtributosAcceso atributos = new AtributosAcceso();
	
	
	// Métodos internos
	// -----------------------------------------------------------------------------------------------------------------
	private boolean checkAcceptationCapacity() {
		// Acá la lógica puede ser tan compleja como se quiera
		return atributos.getCentrales().size() < atributos.maxNCCapacity;
	}
	
	
	// Métodos externos
	// -----------------------------------------------------------------------------------------------------------------
	public int atenderAnuncioNC(DireccionNodo direccionNC, boolean retransmitir) throws InterruptedException {
		/* Función que determina si el nodo puede aceptar y administrar al NC que se está anunciando 
		 * 
		 * Devuelve el código que le será enviado como respuesta al NC (y del que puede inferirse el estado final
		 * de la evaluación 
		 * 
		 * */
		
		// Valida que no lo tenga ya entre los NC registrados
		HashMap<DireccionNodo, HashMap<String, Comparable>> actuales = this.atributos.getCentrales();
		Boolean nuevo = true;
		
		for (DireccionNodo key : actuales.keySet()) {
			if (key.equals(direccionNC)) {
				nuevo = false;
				break;
			}
		}
		
		if (nuevo) {
			// TODO 2021-04-20: revisar que esto se esté ejecutando
			// Se encola la tarea que indicará a alguno de los WKAN que el nuevo NC necesita enlazarse con otros NCs
			HashMap<String, Object> payload = new HashMap<String, Object>();
			payload.put("ncDestino", direccionNC);
			atributos.encolar("salida", new Tarea(00, "SOLICITAR_NCS_VECINOS", payload));

		}
		
		// Lógica de aceptación de NC
		if (this.checkAcceptationCapacity()) {
			atributos.encolarCentral(this.atributos.nuevoCentral(direccionNC));

			return Codigos.OK;
		} else {
			if (retransmitir)
				atributos.encolar("salida", new Tarea(00, "RETRANSMITIR_ANUNCIO_NC", direccionNC));

			return Codigos.ACCEPTED;
		}
	}

	public List<DireccionNodo> getNCsConCapacidadNH(Integer cantidad, Set<DireccionNodo> excepciones) {
		/**
		 * Método que devuelve un listado (de la cantidad requerida) de NCs que son capaces de recibir a un NH.
		 *
		 * Se excluyen los NCs indicados en el conjunto excepciones.
		 *
		 * Puede devolver lista vacía
		 *
		 */

		LinkedList<DireccionNodo> lista = new LinkedList<DireccionNodo>();
		List<DireccionNodo> shuffled;
		HashMap<String, Comparable> nodo;
		Set<DireccionNodo> actuales;

		actuales = new HashSet<DireccionNodo>(((AtributosAcceso) this.atributos).getCentrales().keySet());
		actuales.removeAll(excepciones);

		// Política de balanceo de carga random
		shuffled = new ArrayList<DireccionNodo>(actuales);
		Collections.shuffle(shuffled);

		for (DireccionNodo key : shuffled) {
			nodo = ((AtributosAcceso) this.atributos).getCentrales().get(key);

			if ((Integer) nodo.get("hojas_activas") < (Integer) nodo.get("hojas_max")) {
				lista.add(key);
				
				if (lista.size() >= cantidad)
					break;
			}
		}
		
		return lista;
	}
}
