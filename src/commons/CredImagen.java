package commons;
import java.io.Serializable;

/**
 * "Credencial" que representa una imagen. Es el conjunto formado por el par de vectores característicos de
 * una imagen y su nombre.
 * 
 * Existen 2 tipos de credenciales:
 *     a) todos los datos, se utilia para solicitudes de Hoja a Nodo Central
 *     b) vec.car comprimido + nombre, se utiliza cuando la Hoja le informa las imágenes a compartir al N. Cental.
 */

public class CredImagen implements Serializable {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private String nombre;
	private Double[] vectorCaracteristico;
	private Double[] vecCarComprimido;

	/*---------*/
	/* Métodos */
	/*---------*/

	/**Constructor 1
	 * @param nombre
	 * @param vectorCaracteristico
	 * @param vecCarComprimido
	 */
	public CredImagen(String nombre, Double[] vectorCaracteristico, Double[] vecCarComprimido){
		this.nombre = nombre;
		this.vectorCaracteristico = vectorCaracteristico;
		this.vecCarComprimido = vecCarComprimido;
	}

	
	/**Constructor 2
	 * @param nombre
	 * @param vecCarComprimido
	 */
	public CredImagen(String nombre, Double[] vecCarComprimido){
		this.nombre = nombre;
		this.vecCarComprimido = vecCarComprimido;
	}
	
	
	/**
	 * Compara el vector característico comprimido con otro recibido como parámetro.
	 * @param otroVector
	 * @return distancia: double
	 */
	public double comparacionRapida(Double[] otroVector){
		return Calculos.euclideanDistance(this.vecCarComprimido, otroVector);
	}

	/**
	 * Compara el vector característico con otro recibido como parámetro.
	 * @param otroVector
	 * @return distancia: float
	 */
	public double comparacion(Double[] otroVector){
		return Calculos.euclideanDistance(this.vectorCaracteristico, otroVector);
	}


	//Getters
	public String getNombre() {return this.nombre;}
	public Double[] getVectorCaracteristico() {return this.vectorCaracteristico;}
	public Double[] getVecCarComprimido() {return this.vecCarComprimido;}

	//Setters

	@Override
	public String toString(){
		return (this.nombre + " - VC: [...] - VCC: [...]");
	}
}


// TODO: lo fejo para no olvidarme: lo que está pasando es que los histogramas (vectores característicos) guardan el
//  valor absoluto de una imagen (la cantidad de pixeles) pero cuando se comparan imágenes el tamaño no debería importar
//  sino que debería primar la "composición" de la misma. Para ello voy a tener que usar vectores característicos que
//  guarden el valor relativo de pixeles en cada bin del mismo (quizás dividiendo cada bin por el total de píxeles en la
//  imagen?)