package nodes.components.atributos;

import commons.Constantes;
import commons.DireccionNodo;
import commons.structs.wkan.NCIndexado;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;


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

	// De NCs
	private static volatile HashMap<DireccionNodo,HashMap<String, Comparable>> centrales = new HashMap<DireccionNodo,HashMap<String, Comparable>>();
	public static final Integer esperaEntreInformeDeVecinos = Constantes.ESPERA_ENTRE_INFORME_DE_NCS_VECINOS;// segundos

	// Parámetros "operativos"
	public static int keepaliveNC = 60;  // segundos -> TODO: debería venir de config
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

	public HashMap<DireccionNodo, Integer> getNodos() {
		synchronized(lockNodos) {
			return nodosAcceso;
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

	public void setKeepaliveNodo(DireccionNodo nodo, Integer valor) {
		synchronized (lockNodos) {
			nodosAcceso.put(nodo, valor);
		}
	}

	public ArrayList<DireccionNodo> getWkansActivos() {
		/*
		* Para cada WKAN conocido por este Nodo, se guarda un indicador para determinar si está activo o con qué
		* intervalo de confianza se puede creer que sigue activo antes de darlo por muerto.
		*
		* Al 2021-04-20 ese indicardor es un entero que va de 0 a 3, siendo 3 100% activo y 0 inactivo.
		* Hasta donde vi, no estoy decrementando dicho contador, sólo vuelvo a poner en 3 los WKANs que contestan el
		* keep alive así que medio anecdótico el asunto (pero lo voy a corregir).
		*
		* */
		Integer umbral_activo = 1;  // Valor mínimo para considerar a un WKAN activo

		List<DireccionNodo> activos = nodosAcceso.entrySet().stream().filter(
				e -> e.getValue() >= umbral_activo
		).map(Map.Entry::getKey).collect(Collectors.toList());

		return new ArrayList<DireccionNodo>(activos);
	}


	// Nodos Centrales
	// -----------------------------------------------------------------------------------------------------------------
	public HashMap nuevoCentral(DireccionNodo central) {
		HashMap<String, Comparable> nuevo =  new HashMap<String, Comparable>();
		
		// TODO: reemplazar por clase NCIdexado
		nuevo.put("direccion", central);
		nuevo.put("hojas_max", 10);
		nuevo.put("hojas_activas", 0);
		nuevo.put("centrales_max", 6);
		nuevo.put("centrales_activos", 0);
		nuevo.put("alive", true);
		nuevo.put("timestamp", new Timestamp(System.currentTimeMillis()));
		nuevo.put("ultimo_nc_informado", null);
		
		return nuevo;
	}
	
	public void encolarCentral(HashMap<String, Comparable> nodo) {
		centrales.put((DireccionNodo) nodo.get("direccion"), nodo);
	}
	
	public void desencolarCentral(DireccionNodo direccion) {
		centrales.remove(direccion);
	}

	public NCIndexado getCentral(DireccionNodo direccion) {
		NCIndexado output = null;

		// Recordar que por defecto devuelve null si la key no existe
		HashMap<String, Comparable> nodo = centrales.get(direccion);

		if (nodo != null)
			// TODO: fix temporal hasta que reemplace el Hashmap<String, Comparable> de centrales por NCIndexado
			output = new NCIndexado(
					(DireccionNodo) nodo.get("direccion"),
					(Integer) nodo.get("hojasMax"),
					(Integer) nodo.get("Integer hojasActivas"),
					(Integer) nodo.get("centralesMax"),
					(Integer) nodo.get("centralesActivos"),
					(Boolean) nodo.get("alive"),
					(Timestamp) nodo.get("timestamp"),
					(Timestamp) nodo.get("ultimoNncInformado")
			);

		return output;
	}

	public HashMap<DireccionNodo, HashMap<String, Comparable>> getCentrales() {
		return centrales;
	}

	public DireccionNodo getRandomNCDistinto(DireccionNodo distinto) {
		Integer indice;
		Random generador = new Random();
		DireccionNodo direccion = null;

		// No sicnronizo acceso porque sólo leo y es un hash. Me "quedo" con estas keys, si en el transcurso
		// aparecen nuevas no serán consideradas
		List<DireccionNodo> claves = new ArrayList<DireccionNodo>(centrales.keySet());

		if (distinto != null)
			claves.remove(distinto);

		indice = generador.nextInt(claves.size());
		direccion = claves.get(indice);

		return direccion;
	}


	// Getters
	public Integer getStatusNodo(DireccionNodo nodo) {
		synchronized(lockNodos) {
			return nodosAcceso.get(nodo);
		}
	}

	
	// Setters
	public void setKeepaliveNodoVecino(int intentos) {keepaliveNodoVecino = intentos;}
	public void setMaxNCCapacity(int capacity) {maxNCCapacity = capacity;}

	// Implementaciones de los abstractos de la clase padre
	@Override
	public ArrayList<DireccionNodo> getWkans() {
		return new ArrayList<DireccionNodo>(this.nodosAcceso.keySet());
	}

	@Override
	public ArrayList<DireccionNodo> getNcs() {
		return new ArrayList<DireccionNodo>(this.centrales.keySet());
	}

	@Override
	public ArrayList<DireccionNodo> getNhs() {
		return new ArrayList<DireccionNodo>();
	}
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
