/**
 * Script 2 de 5 en los que voy a probar como funcionan los parámetros compartidos y sincronizados a nivel de
 * de clase.
 * 
 * La idea es tener una clase con atibutos STATIC y ejecutarla en dos archivos distintos: según entiendo, deberían
 * compartir todo pues se ejecutan en la misma JVM.
 * 
 * Este archivo particularmente sería el hilo consumidor encargado de leer la variable compartida.
 *
 * @author rodrigo
 *
 */
public class SincroClase2de5 implements Runnable {
	private String nombre = "";
	
	public SincroClase2de5(String nombre) {
		this.nombre = nombre;
	}

	@Override
	public void run() {
		Integer devuelto = null;
		SincroClase1de5 sincro = new SincroClase1de5();
		
		for(int i=0; i<10; i++){
			try { Thread.sleep(1000);}
			catch (InterruptedException e1) {e1.printStackTrace();}
			
			devuelto = sincro.consumir();
			System.out.println(nombre + " Consumidor: " + devuelto);
		}
		
	}

} // Fin clase
