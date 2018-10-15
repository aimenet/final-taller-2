/**
 * Script 3 de 5 en los que voy a probar como funcionan los parámetros compartidos y sincronizados a nivel de
 * de clase.
 * 
 * La idea es tener una clase con atibutos STATIC y ejecutarla en dos archivos distintos: según entiendo, deberían
 * compartir todo pues se ejecutan en la misma JVM.
 * 
 * Este archivo particularmente sería el hilo productor encargado de generar números pares o impares
 * (dependiendo del tipo definido) en la variable compartida.
 *
 * @author rodrigo
 *
 */
public class SincroClase3de5 implements Runnable {
	private String nombre;
	private String tipo;
	
	public SincroClase3de5(String nombre, String tipo) {
		this.nombre = nombre;
		this.tipo = tipo;
	}
	
	@Override
	public void run() {
		Integer devuelto = null;
		SincroClase1de5 sincro = new SincroClase1de5();
		
		for(int i=0; i<10; i++){
			try { Thread.sleep(1000);}
			catch (InterruptedException e1) {e1.printStackTrace();}
			
			devuelto = sincro.producir(tipo);
			System.out.println(nombre + " Productor: " + devuelto);
		}
		
	}

} // Fin clase
