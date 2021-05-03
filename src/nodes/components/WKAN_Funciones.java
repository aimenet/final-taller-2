package nodes.components;

import java.util.*;

import commons.Codigos;
import commons.Tarea;
import commons.DireccionNodo;
import commons.mensajes.wkan_wkan.RetransmisionAnuncioNc;
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

	private int aceptarORetransmitirNC(
			DireccionNodo direccionNC,
			Boolean retransmitir,
			Integer saltos_retransmision,
			ArrayList<DireccionNodo> wkansVisitados
	) {
		/* Determina si el nodo puede aceptar y administrar al NC que se está anunciando, o si en su defecto debe
		 * retransmitir el anuncio a otro WKAN.
		 *
		 * Devuelve el código que le será enviado como respuesta al NC (y del que puede inferirse el estado final
		 * de la evaluación
		 *
		 * */
		// Valida que no lo tenga ya entre los NC registrados
		HashMap<DireccionNodo, HashMap<String, Comparable>> actuales = this.atributos.getCentrales();

		// Lógica de aceptación de NC
		if (this.checkAcceptationCapacity()) {
			atributos.encolarCentral(this.atributos.nuevoCentral(direccionNC));
			return Codigos.OK;
		}

		if (retransmitir && saltos_retransmision > 0) {
			RetransmisionAnuncioNc retransmision = new RetransmisionAnuncioNc(
					atributos.getDireccion(),
					Codigos.NA_NA_POST_RETRANSMISION_ANUNCIO_NC,
					direccionNC,
					saltos_retransmision,
					wkansVisitados
			);

			try {
				atributos.encolar("salida", new Tarea(00, "RETRANSMITIR_ANUNCIO_NC", retransmision));
			} catch (InterruptedException e) {
				e.printStackTrace();
				return Codigos.DENIED;
			}

			return Codigos.ACCEPTED;
		}

		return Codigos.DENIED;
	}
	
	// Métodos externos
	// -----------------------------------------------------------------------------------------------------------------
	public int atenderAnuncioNC(DireccionNodo direccionNC, boolean retransmitir) {
		Integer saltos_retransmision = 3; // TODO: esto no debe estar hardcodeado ni ser un número puesto a dedo
		ArrayList<DireccionNodo> wkansVisitados = new ArrayList<DireccionNodo>();

		wkansVisitados.add(atributos.getDireccion());

		Integer resultado = this.aceptarORetransmitirNC(
				direccionNC, retransmitir, saltos_retransmision, wkansVisitados
		);

		return resultado;
	}

	public int atenderAnuncioNC(RetransmisionAnuncioNc retransmision) {
		ArrayList<DireccionNodo> wkansVisitados = retransmision.getWkansVisitados();
		wkansVisitados.add(atributos.getDireccion());

		Integer resultado = this.aceptarORetransmitirNC(
				retransmision.getNodoCentral(),
				true,
				retransmision.getSaltos() - 1,
				wkansVisitados
		);

		return resultado;
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
