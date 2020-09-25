package nodes.components;

import commons.structs.DireccionNodo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


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
	private static volatile HashMap<DireccionNodo, Integer> nodosAcceso = new HashMap<DireccionNodo, Integer>();
	
	private static volatile HashMap<DireccionNodo,HashMap<String, Comparable>> centrales = new HashMap<DireccionNodo,HashMap<String, Comparable>>();
	
	// Parámetros "operativos"
	public static int keepaliveNC = 30;  // segundos -> TODO: debería venir de config
	public static int keepaliveNodoVecino = 3; // valor default, los nodos pueden sobreescribirlo si corresponde
	public static int maxNCCapacity = 10;
	
	
	// Constantes -> esta debería pisar a la de la clase padre no?
	private final int MAX_QUEUE_SIZE = 10;
	
	
	// Métodos
	// =======
	// Listado de WKAN
	public void addNodos(ArrayList<DireccionNodo> nuevosNodos) {
		synchronized (lockNodos) {
			// Evito duplicados sobreescribiendo a los existentes, si corresponde
			for (DireccionNodo nodo : nuevosNodos)
				nodosAcceso.put(nodo, 0);
		}
	}

	public void activarNodo(DireccionNodo nodo) {
		// Nótese que si se "activa" un nodo que ya existía en el listado, a fin de cuentas lo que se hace es reiniciar
		// el keepalive
		synchronized (lockNodos) {
			nodosAcceso.put(nodo, keepaliveNodoVecino);
		}
	}
	
	public void setKeepaliveNodo(DireccionNodo nodo, Integer valor) {
		synchronized (lockNodos) {
			nodosAcceso.put(nodo, valor);
		}
	}

	public DireccionNodo getRandomNABC() {
		Integer indice;
		Random generador = new Random();
		DireccionNodo direccion = null;
		
		// No sicnronizo acceso porque sólo leo y es un hash. Me "quedo" con estas keys, si en el transcurso 
		// aparecen nuevas no serán consideradas
		List<DireccionNodo> claves = new ArrayList<DireccionNodo>(nodosAcceso.keySet());
		boolean buscar = true;
		
		while (buscar && claves.size() > 0) {
			indice = generador.nextInt(claves.size());
			DireccionNodo nodo = claves.get(indice);
			
			if (nodosAcceso.get(nodo) == keepaliveNodoVecino) {
				buscar = false;
				direccion = nodo;
			} else {
				claves.remove(indice);
			}
		}
		
		return direccion;
	}
	
	
	// Nodos Centrales
	// -----------------------------------------------------------------------------------------------------------------
	public HashMap nuevoCentral(DireccionNodo central) {
		HashMap<String, Comparable> nuevo =  new HashMap<String, Comparable>();
		
		// TODO: breve descripción de c/u
		nuevo.put("direccion", central);
		nuevo.put("hojas_max", 10);
		nuevo.put("hojas_activas", 0);
		nuevo.put("centrales_max", 6);
		nuevo.put("centrales_activos", 0);
		nuevo.put("alive", true);
		nuevo.put("timestamp", new Timestamp(System.currentTimeMillis()));
		
		return nuevo;
	}
	
	public void encolarCentral(HashMap<String, Comparable> nodo) {
		centrales.put((DireccionNodo) nodo.get("direccion"), nodo);
	}
	
	public void desencolarCentral(DireccionNodo direccion) {
		centrales.remove(direccion);
	}
	
	public HashMap<DireccionNodo, HashMap<String, Comparable>> getCentrales() {
		return centrales;
	}
	
	
	// Getters
	public HashMap<DireccionNodo, Integer> getNodos() {
		synchronized(lockNodos) {
			return nodosAcceso;
		}
	}
	
	public Integer getStatusNodo(DireccionNodo nodo) {
		synchronized(lockNodos) {
			return nodosAcceso.get(nodo);
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
