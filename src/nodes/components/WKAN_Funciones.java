package nodes.components;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;

import commons.*;
import commons.mensajes.wkan_wkan.RetransmisionAnuncioNc;
import commons.structs.wkan.NCIndexado;
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

	// Respuestas predefinidas de métodos
	public enum atenderSolicutdVecinosNCOutput {
		OK_CON_VECINO,
		OK_SIN_VECINO,
		KO_NODO_DESCONOCIDO,
		KO_ERROR,
	}

	
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

	public DireccionNodo getWKANParaRetransmisiónSolicitudVecinosNCOutput(ArrayList<DireccionNodo> excluidos) {
		/**
		 * Método utilizado para elegir un WKAN al que enviar una retransmisión de Solicitud de Vecinos para un NC.
		 *
		 * Pasos:
		 * 	1) Obtiene los WKAN conocidos hasta el momento
		 * 	2) Descarta los WKANs ya consultados
		 * 	3) Escoge aleatoriamente uno
		 *
		 * La elección es aleatorioa para:
		 * 	a) No saturar la red con tantos mensajes
		 * 	b) Ampliar la cobertura de la red
		 *
		 * */
		DireccionNodo elegido = null;

		// Obtención de WKANs conocidos y descarte de aquellos ya consultados
		ArrayList<DireccionNodo> noConsultados = ((AtributosAcceso) atributos).getWkansActivos();
		noConsultados.removeAll(excluidos);

		if (noConsultados.size() > 0) {
			// Elección aleatoria del WKAN al que retransmitir
			Random generador = new Random();
			Integer indice = generador.nextInt(noConsultados.size());
			elegido = noConsultados.get(indice);
		}

		return elegido;
	}

	public atenderSolicutdVecinosNCOutput atenderSolicutdVecinosNC(DireccionNodo nodo) {
		/*
		* Se recibió un pedido de NCs vecinos.
		* Se busca entre todos los NCs administrados por el nodo si alguno puede ser informado, en cuyo caso se encola
		* la tarea de conectar ambos nodos.
		*
		* Algunas consideraciones:
		*     1) Solo se escogerá 1 NC para comunicar, independientemente de la cantidad solicitada. Esto puede ser una
		*        limitante en redes grandes.
		*     2) En la solicitud recibida se podría especificar los NCs conocidos hasta el momento, de manera de no
		*        sugerir uno al cual el NC solicitante ya se encuentra conectado
		*
		* */
		Boolean errorFlag = false;
		Boolean informar = false;
		DireccionNodo vecino = null;

		vecino = atributos.getRandomNCDistinto(nodo);

		if (vecino != null) {
			Tupla2<DireccionNodo, DireccionNodo> par = new Tupla2<DireccionNodo, DireccionNodo>(nodo, vecino);

			Tarea tarea = new Tarea(00, Constantes.TSK_NA_CONECTAR_NCS, par);

			try {
				atributos.encolar("centrales", tarea);
			} catch (InterruptedException e) {
				// TODO: qué hago?
				errorFlag = true;
				e.printStackTrace();
			}
		}

		if (vecino != null && !errorFlag) {
			return atenderSolicutdVecinosNCOutput.OK_CON_VECINO;
		} else if (vecino == null && !errorFlag) {
			return atenderSolicutdVecinosNCOutput.OK_SIN_VECINO;
		} else {
			return atenderSolicutdVecinosNCOutput.KO_ERROR;
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
