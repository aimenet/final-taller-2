package commons.mensajes;

import commons.DireccionNodo;

import java.io.Serializable;
import java.util.UUID;

/**Unidad de datos intercambiada entre Cliente (Nodo Hoja) y Servidor (Nodo Central).
 * Posee un código entero que indica la acción a realizar y una carga que por ahora es un String.
 * 
 * Los códigos de acción están determiandos por la interfaz "Codigos.java".
 *
 */
public class Mensaje implements Serializable {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private Integer codigo, ttl;
	private Object carga;
	private DireccionNodo emisor;
	
	// Usados por mensajes que se envían entre NCs
	private DireccionNodo origen, ncEmisor, recepcionRta;
	
	// Usados por mensajes que se envían entre NH y NC
	private UUID idEmisor;


	// Métodos
	// -----------------------------------------------------------------------------------------------------------------
	/** Constructor básico (general) */
	public Mensaje(DireccionNodo emisor, Integer codigo, Object carga) {
		this.emisor = emisor;
		this.codigo = codigo;
		this.carga = carga;
		
		this.ttl = 1;
		this.origen = null;
		this.ncEmisor = null;

		this.idEmisor = null;
	}

	/** Constructor usado por NC para retransmitir consulta a otro NC */
	public Mensaje(DireccionNodo emisor, DireccionNodo origen, DireccionNodo ncEmisor, Integer codigo, Integer ttl,
				   Object carga, DireccionNodo direccionRta){
		this.emisor = emisor;
		this.origen = origen;
		this.codigo = codigo;
		this.ttl = ttl;
		this.carga = carga;
		this.ncEmisor = ncEmisor;
		this.recepcionRta = direccionRta;

		this.idEmisor = null;
	}

	/** Constructor usado por NH para comunicación con NC */
	public Mensaje(DireccionNodo emisor, UUID idEmisor, DireccionNodo direccionRta, Integer codigo, Object carga){
		this.emisor = emisor;
		this.recepcionRta = direccionRta;
		this.codigo = codigo;
		this.carga = carga;

		this.ttl = 1;
		this.origen = null;
		this.ncEmisor = null;

		this.idEmisor = idEmisor;
	}

	// Getters.
	// -------------------------------------------
	public Object getCarga() {return carga;}

	public Integer getCodigo() {return codigo;}

	public DireccionNodo getEmisor() {return emisor;}

	public UUID getIdEmisor() {return this.idEmisor;}

	public DireccionNodo getNCEmisor() {return ncEmisor;} // TODO: evaluar si se usa, si no, borrarla

	public DireccionNodo getOrigen() {return origen;}

	public Integer getTTL() {return ttl;}

	public DireccionNodo recepcionRta(){
		return recepcionRta;
	}

    // 2020-09-11: lo mato hasta que llegue su turno
	/*

	// Constructor usado por NC
	public Mensaje(String emisor, Integer codigo, Integer ttl, Object carga){
		this.emisor = emisor;
		this.codigo = codigo;
		this.ttl = ttl;
		this.carga = carga;
		
		this.origen=null;
		this.ncEmisor = null;
	}
	
	// Otro constructor usado por NC
	public Mensaje(String emisor, String origen, String ncEmisor, Integer codigo, Integer ttl, Object carga){
		this.emisor = emisor;
		this.origen = origen;
		this.codigo = codigo;
		this.ttl = ttl;
		this.carga = carga;
		this.ncEmisor = ncEmisor;
	}

	public void decrementarTTL(){
		ttl -= 1;
	}
	
	public Object getCarga(){
		return carga;
	}
	
	//Setters.
	private void setEmisor(String emisor){
		this.emisor = emisor;
	}
	
	private void setCodigo(Integer codigo){
		this.codigo = codigo;
	}
	
	private void setTTL(Integer ttl){
		this.ttl = ttl;
	}
	
	private void setCarga(Object carga){
		this.carga = carga;
	}
	
	@Override
	public String toString(){
		if (getCarga() == null )
			return getCodigo().toString();
		else
			return (getCodigo().toString() + " - <" + getCarga().toString() + ">");
	}


	 */
}
