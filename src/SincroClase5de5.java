/**
 * Script 5 de 5 en los que voy a probar como funcionan los parámetros compartidos y sincronizados a nivel de
 * de clase.
 * 
 * La idea es tener una clase con atibutos STATIC y ejecutarla en dos archivos distintos: según entiendo, deberían
 * compartir todo pues se ejecutan en la misma JVM.
 * 
 * Este archivo particularmente sería el encargado de generar el par de hilos productor/consumidor encargado
 * de manipular sólo números impares.
 *
 * @author rodrigo
 *
 */
public class SincroClase5de5 {

	public static void main(String[] args) {
		SincroClase2de5 consumidor = new SincroClase2de5("<Impares>");
		SincroClase3de5 productor = new SincroClase3de5("<Impares>","I");
		
		Thread hiloC = new Thread( consumidor );
		Thread hiloP = new Thread( productor );
		
		hiloP.start();
		hiloC.start();

	}

} // Fin clase
