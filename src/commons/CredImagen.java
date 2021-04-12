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
	private int[] vectorCaracteristico;
	private int[] vecCarComprimido;

	/*---------*/
	/* Métodos */
	/*---------*/

	/**Constructor 1
	 * @param nombre
	 * @param vectorCaracteristico
	 * @param vecCarComprimido
	 */
	public CredImagen(String nombre, int[] vectorCaracteristico, int[] vecCarComprimido){
		this.nombre = nombre;
		this.vectorCaracteristico = vectorCaracteristico;
		this.vecCarComprimido = vecCarComprimido;
	}

	
	/**Constructor 2
	 * @param nombre
	 * @param vecCarComprimido
	 */
	public CredImagen(String nombre, int[] vecCarComprimido){
		this.nombre = nombre;
		this.vecCarComprimido = vecCarComprimido;
	}
	
	
	/**
	 * Compara el vector característico comprimido con otro recibido como parámetro.
	 * @param otroVector
	 * @return distancia: float
	 */
	public float comparacionRapida(int[] otroVector){
		float aux, distancia;
		
		distancia = 1000;
		if( vecCarComprimido != null ){
			distancia = 0;
			for(int i=0; i<this.vecCarComprimido.length; i++){
				aux = Math.abs(this.vecCarComprimido[i] - otroVector[i]);
				distancia += Math.pow(aux,2);
			}
		}
			
		return distancia;
	}


	/**
	 * Compara el vector característico con otro recibido como parámetro.
	 * @param otroVector
	 * @return distancia: float
	 */
	public float comparacion(int[] otroVector){
		float aux, distancia; 
		
		distancia = 1000;
		if( vectorCaracteristico != null ){
			distancia = 0;
			for(int i=0; i<this.vectorCaracteristico.length; i++){
				aux = Math.abs(this.vectorCaracteristico[i] - otroVector[i]);
				distancia += Math.pow(aux,2);
			}
		}

		return distancia;
	}

	
	
	
	//Getters
	public String getNombre() {return this.nombre;}
	public int[] getVectorCaracteristico() {return this.vectorCaracteristico;}
	public int[] getVecCarComprimido() {return this.vecCarComprimido;}	

	//Setters

	@Override
	public String toString(){
		return (this.nombre + " - VC: [...] - VCC: [...]");
	}
}
