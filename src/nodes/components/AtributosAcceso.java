package nodes.components;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import commons.Tarea;


/**
 * Clase que engloba todos los atributos de un Nodo de Acceso (Bien Conocido) e implementa los métodos 
 * necesarios para manipularlos.
 * 
 * Como puede notarse, los atributos están sicnronizados a nivel de clase de manera que todas las instancias
 * puedan compartirlos. La idea es poder accederlos tanto desde el servidor avocado a la atención de Hojas
 * como desde el encargado de atender otros Nodos Centrales (y futuros componentes como un servidor de 
 * monitoreo por ejemplo)
 * 
 * Nótese que lo anterior hace que sea thread-safe, así que quienes consuman de las colas sicnronizadas no deberán preocuparse por locks y demás
 * métodos de acceso, simplemente encolar y desencolar.
 * 
 * @author rodrigo
 */
public class AtributosAcceso extends Atributos {
	// Atributos
	// =========
	// Los defino acá porque todas las instancias van a usar el atributo de la clase 
	private static final Object lockNodos = new Object();
	private static volatile HashMap<String, Integer> nodos = new HashMap<String, Integer>();
	
	private static volatile HashMap<String,HashMap<String, Comparable>> centrales = new HashMap<String,HashMap<String, Comparable>>();
	
	// Parámetros "operativos"
	public static int keepaliveNC = 30;  // segundos -> TODO: debería venir de config
	public static int keepaliveNodoVecino = 3; // valor default, los nodos pueden sobreescribirlo si corresponde
	public static int maxNCCapacity = 10;
	
	
	// Constantes -> esta debería pisar a la de la clase padre no?
	private final int MAX_QUEUE_SIZE = 10;
	
	
	// Métodos
	// =======
	// Listado de WKAN
	public void addNodos(ArrayList<String> nuevosNodos) {
		synchronized (lockNodos) {
			// Evito duplicados sobreescribiendo a los existentes, si corresponde
			for (String nodo : nuevosNodos)
				nodos.put(nodo, 0);
		}
	}

	public void activarNodo(String nodo) {
		synchronized (lockNodos) {
			nodos.put(nodo, keepaliveNodoVecino);
		}
	}
	
	public void setKeepaliveNodo(String nodo, Integer valor) {
		synchronized (lockNodos) {
			nodos.put(nodo, valor);
		}
	}

	public String getRandomNABC() {
		Integer indice;
		Random generador = new Random();
		String direccion = null;
		
		// No sicnronizo acceso porque sólo leo y es un hash. Me "quedo" con estas keys, si en el transcurso 
		// aparecen nuevas no serán consideradas
		List<String> claves = new ArrayList<String>(nodos.keySet());
		boolean buscar = true;
		
		while (buscar && claves.size() > 0) {
			indice = generador.nextInt(claves.size());
			String key = (String) claves.get(indice);
			
			if (nodos.get(key) == keepaliveNodoVecino) {
				buscar = false;
				direccion = key;
			} else {
				claves.remove(indice);
			}
		}
		
		return direccion;
	}
	
	
	// Nodos Centrales
	public HashMap nuevoCentral(String direccion_NA, String direccion_NC, String direccion_NH) {
		HashMap<String, Comparable> nuevo =  new HashMap<String, Comparable>();
		
		// TODO: breve descripción de c/u
		nuevo.put("direccion_NA", direccion_NA);
		nuevo.put("direccion_NC", direccion_NC);
		nuevo.put("direccion_NH", direccion_NH);
		nuevo.put("hojas_max", 10);
		nuevo.put("hojas_activas", 0);
		nuevo.put("centrales_max", 6);
		nuevo.put("centrales_activos", 0);
		nuevo.put("alive", true);
		nuevo.put("timestamp", new Timestamp(System.currentTimeMillis()));
		
		return nuevo;
	}
	
	public void encolarCentral(HashMap nodo) {
		centrales.put((String)nodo.get("direccion_NA"), nodo);
	}
	
	public void desencolarCentral(String direccion) {
		centrales.remove(direccion);
	}
	
	public HashMap<String, HashMap<String, Comparable>> getCentrales() {
		return centrales;
	}
	
	
	// Getters
	public HashMap<String, Integer> getNodos() {
		synchronized(lockNodos) {
			return nodos;
		}
	}
	
	public Integer getStatusNodo(String nodo) {
		synchronized(lockNodos) {
			return nodos.get(nodo);
		}
	}

	
	// Setters
	public void setKeepaliveNodoVecino(int intentos) {keepaliveNodoVecino = intentos;}
	public void setMaxNCCapacity(int capacity) {maxNCCapacity = capacity;}
	
} //Fin clase


/**
 * Notas
 * -----
 * [2019-10-20] Tener más de una cola -(interna, salida) por ejemplo- implica un método que desencole de cualquiera
 *              según una prioridad específica. Lo difícil es que el método de desencolado no puede esperar notifys}
 *              de muchos locks, sólo de uno. Entonces, ¿cómo saber cuando una cualquiera de las colas tiene tareas?.
 *              Esto siempre acotado a varias colas para un mismo tipo de consumidor, no confundir con tener una cola
 *              para tareas relacionadas a WKANs y otra para NCs. 
 *
 *  */
