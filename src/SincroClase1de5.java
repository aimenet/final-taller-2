import java.util.Random;

/**
 * Script 1 de 5 en los que voy a probar como funcionan los parámetros compartidos y sincronizados a nivel de
 * de clase.
 * 
 * La idea es tener una clase con atibutos STATIC y ejecutarla en dos archivos distintos: según entiendo, deberían
 * compartir todo pues se ejecutan en la misma JVM.
 * -> No, entendí mal. Cada proceso tiene su propio espacio de memoria (en realidad funciona a nivel de
 *                     class loader, pero a fines prácticos es lo mismo).
 *                     Ver <https://stackoverflow.com/questions/10372232/java-static-variable-and-process>
 * 
 * Este archivo particularmente sería la clase que maneja los atributos compartidos y sincronizados a nivel
 * de clase.
 * @author rodrigo
 *
 */
public class SincroClase1de5 {
	// Atributos
	// =========
	private static Integer acumulador = 0;
	private static final Object lock = new Object();
	
	
	// Métodos
	// =======
	public Integer producir(String tipo){
		Integer[] pares = {2,4,6,8,10};
		Integer[] impares = {1,3,5,7,9};
		
		synchronized (lock) {
			if (tipo == "P"){
				int rnd = new Random().nextInt(pares.length);
			    acumulador = pares[rnd];
			    return acumulador;
			} else {
				int rnd = new Random().nextInt(impares.length);
			    acumulador = impares[rnd];
			    return acumulador;
			}
		}
	}
	
	public Integer consumir(){
		synchronized (lock) {
			return acumulador;
		}
	}

} // Fin clase
