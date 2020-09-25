package commons;
import commons.structs.DireccionNodo;

import java.io.Serializable;
import java.net.InetAddress;

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
	private String origen, ncEmisor, recepcionRta;
	
	
	/*---------*/
	/* Métodos */
	/*---------*/
//  Constructor básico: enviado desde Hoja a NC con consulta ¿y desde H a H para descarga?


	public Mensaje(DireccionNodo emisor, Integer codigo, Object carga) {
		this.emisor = emisor;
		this.codigo = codigo;
		this.carga = carga;
		
		this.ttl = 1;
		this.origen = null;
		this.ncEmisor = null;
	}

	// Getters.
	// -------------------------------------------
	public Integer getCodigo() {return codigo;}

	public DireccionNodo getEmisor() {return emisor;}

	public Object getCarga() {return carga;}

	// 2020-09-11: lo mato hasta que llegue su turno
	/*
	public Mensaje(String idEmisor, String direccionRta, Integer codigo, Object carga){
		this.emisor = idEmisor;
		this.recepcionRta = direccionRta;
		this.codigo = codigo;
		this.carga = carga;

		this.ttl = 1;
		this.origen = null;
		this.ncEmisor = null;
	}


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
	
	// Constructor usado por NC para retransmitir consulta a otro NC
	public Mensaje(String emisor, String origen, String ncEmisor, Integer codigo, Integer ttl, Object carga, String direccionRta){
		this.emisor = emisor;
		this.origen = origen;
		this.codigo = codigo;
		this.ttl = ttl;
		this.carga = carga;
		this.ncEmisor = ncEmisor;
		this.recepcionRta = direccionRta;
	}

	public void decrementarTTL(){
		ttl -= 1;
	}


	
	public Integer getTTL(){
		return ttl;
	}
	
	public Object getCarga(){
		return carga;
	}
	
	public Object getOrigen(){
		return origen;
	}
	
	public Object getNCEmisor(){
		return ncEmisor;
	}
	
	public String recepcionRta(){
		return recepcionRta;
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
