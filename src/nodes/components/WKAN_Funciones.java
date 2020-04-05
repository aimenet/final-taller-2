package nodes.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import commons.Codigos;
import commons.Mensaje;
import commons.Tarea;

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
	// -------------------------------------------------------------------------------------------------------------
	private boolean checkAcceptationCapacity() {
		// Acá la lógica puede ser tan compleja como se quiera
		return atributos.getCentrales().size() < atributos.maxNCCapacity;
	}
	
	
	// Métodos externos
	// -------------------------------------------------------------------------------------------------------------
	public int atenderAnuncioNC(String direccionNC_NA, String direccionNC_NC, String direccionNC_NH, boolean retransmitir) throws InterruptedException {
		/* Función que determina si el nodo puede aceptar y administrar al NC que se está anunciando 
		 * 
		 * Devuelve el código que le será enviado como respuesta al NC (y del que puede inferirse el estado final
		 * de la evaluación 
		 * 
		 * */
		
		// Valida que no lo tenga ya entre los NC registrados
		HashMap<String, HashMap<String, Comparable>> actuales = this.atributos.getCentrales();
		Boolean nuevo = true;
		
		for (String key : actuales.keySet()) {
			HashMap<String, Comparable> nodo = actuales.get(key);
			
			if (nodo.get("direccion_NA").equals(direccionNC_NA)) {
				nuevo = false;
				break;
			}
		}
		
		if (nuevo) {
			// Se encola la tarea que indicará a alguno de los WKAN que el nuevo NC necesita enlazarse con
			// otros NCs
			// Le paso la dirección dónde el NC atiende a WKAN ya que será uno de estos últimos el que
			// le informará de su vecino
			HashMap<String, Object> payload = new HashMap<String, Object>();
			payload.put("ncDestino", direccionNC_NA);
			atributos.encolar("salida", new Tarea(00, "SOLICITAR_NCS_VECINOS", payload));
			//return Codigos.OK;
		}
		
		// Lógica de aceptación de NC
		if (this.checkAcceptationCapacity()) {
			atributos.encolarCentral(this.atributos.nuevoCentral(direccionNC_NA, direccionNC_NC, direccionNC_NH));
			return Codigos.OK;
		} else {
			if (retransmitir) {
				HashMap<String, Object> payload = new HashMap<String, Object>();
				payload.put("ncDestino_NA", direccionNC_NA);
				atributos.encolar("salida", new Tarea(00, "RETRANSMITIR_ANUNCIO_NC", direccionNC_NA));
			}	
			return Codigos.ACCEPTED;
		}
	}

	public LinkedList<HashMap<String, Comparable>> getNCsConCapacidadNH(Integer cantidad) {
		/* Método que devuelve un listado (de la cantidad requerida) de NCs que son capaces de recibir a un NH */
		
		LinkedList<HashMap<String, Comparable>> lista = new LinkedList<HashMap<String, Comparable>>();
		HashMap<String, HashMap<String, Comparable>> centrales;
		HashMap<String, Comparable> nodo;
		
		// Acá se puede implementar una política de balanceo, por ahora voy "llenando" los NCs de Hojas de manera ordenada
		centrales = ((AtributosAcceso) atributos).getCentrales();
		for (Entry<String, HashMap<String, Comparable>> me : ((AtributosAcceso) this.atributos).getCentrales().entrySet()) {
			if ((Integer) me.getValue().get("hojas_activas") < (Integer) me.getValue().get("hojas_max")) {
				lista.add(me.getValue());
				
				if (lista.size() >= cantidad)
					break;
			}
		}
		
		return lista;
	}
}
