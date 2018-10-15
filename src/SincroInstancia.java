import java.util.ArrayList;

/**
 * Ejemplo de sincronización de atributos a nivel de instancia. 
 * @author rodrigo
 *
 */
public class SincroInstancia {
	
	
	public static void main(String[] args) {
		Acumulador compartido = new Acumulador();
		Clase objeto1 = new Clase(1,compartido);
		Clase objeto2 = new Clase(2,compartido);
		
		Thread hilo1 = new Thread( objeto1 );
		Thread hilo2 = new Thread( objeto2 );
		
		
		hilo1.start();
		hilo2.start();
		
	}

}

class Clase implements Runnable {
	public Acumulador instancia;
	public Integer id;
	
	public Clase(Integer id, Acumulador instancia){
		this.id = id;
		this.instancia = instancia;
	}
	
	public void metodo() {
		Integer resultado = instancia.incrementar();
		System.out.println(id + " : " + resultado);
	}

	@Override
	public void run() {
		metodo();
		metodo();
		metodo();
	}
}

class Acumulador {
	public Integer suma;
	private final Object lock;
	
	public Acumulador(){
		suma = 0;
		lock = new Object();
	}
	
	public Integer incrementar(){
		synchronized (lock) {
			suma++;
			return suma;
		}
	}
	
}