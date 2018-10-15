/**
 * Script 4 de 5 en los que voy a probar como funcionan los parámetros compartidos y sincronizados a nivel de
 * de clase.
 * 
 * La idea es tener una clase con atibutos STATIC y ejecutarla en dos archivos distintos: según entiendo, deberían
 * compartir todo pues se ejecutan en la misma JVM.
 * 
 * Este archivo particularmente sería el encargado de generar el par de hilos productor/consumidor encargado
 * de manipular sólo números pares.
 *
 *Si defino tipo = standalone, tengo que ejecutar este arhivo sólo, si pongo cualquier otra cosa se usa
 *junto con SincroClase5de5.
 *
 * @author rodrigo
 *
 */
public class SincroClase4de5 {

	public static void main(String[] args) {
		String tipo = "";
		
		if( tipo.equals("standalone") ) {
			// Va a ser usado solo
			SincroClase2de5 consumidorP = new SincroClase2de5("<Pares>");
			SincroClase2de5 consumidorI = new SincroClase2de5("<Impares>");
			SincroClase3de5 productorP = new SincroClase3de5("<Pares>","P");
			SincroClase3de5 productorI = new SincroClase3de5("<Impares>","I");
			
			Thread hiloCP = new Thread( consumidorP );
			Thread hiloCI = new Thread( consumidorI );
			Thread hiloPP = new Thread( productorP );
			Thread hiloPI = new Thread( productorI );
			
			hiloPP.start();
			hiloPI.start();
			hiloCP.start();
			hiloCI.start();
		} else {
			// Va a ser usado junto con SincroClase5de5
			SincroClase2de5 consumidor = new SincroClase2de5("<Pares>");
			SincroClase3de5 productor = new SincroClase3de5("<Pares>","P");
			
			Thread hiloC = new Thread( consumidor );
			Thread hiloP = new Thread( productor );
			
			hiloP.start();
			hiloC.start();
		}

	}

} // Fin clase
