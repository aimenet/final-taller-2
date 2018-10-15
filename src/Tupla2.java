import java.io.Serializable;

/**Clase que representa una tupla de dos elementos (un par).*/
public class Tupla2<P,S> implements Serializable {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private final P primero;
	private final S segundo;
	
	
	/*---------*/
	/* Métodos */
	/*---------*/
	public Tupla2(P primero, S segundo) {
	  this.primero = primero;
	  this.segundo = segundo;
	}

	@Override
	public boolean equals(Object objeto) {
	  if (!(objeto instanceof Tupla2)) return false;
	  Tupla2 objetoPar = (Tupla2) objeto;
	  return getPrimero().equals(objetoPar.getPrimero()) && getSegundo().equals(objetoPar.getSegundo());
	  }
	
	/*Getters y Setters*/
	public P getPrimero() { return primero; }
	public S getSegundo() { return segundo; }
}