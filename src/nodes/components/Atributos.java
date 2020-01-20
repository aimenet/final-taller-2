package nodes.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import commons.Tarea;


/**
 * Clase de "atributos". De esta heredarán las clases de atributos particulares de cada uno de los nodos.
 * 
 * Básicamente implementa todo el esquema de colas sincronizadas para productores y consumidores.
 * 
 * Como puede notarse, los atributos están sicnronizados a nivel de clase de manera que todas las instancias
 * puedan compartirlos. La idea es poder accederlos tanto desde el servidor abocado a la atención de Hojas
 * como desde el encargado de atender otros Nodos Centrales (y futuros componentes como un servidor de 
 * monitoreo por ejemplo)
 * 
 * Nótese que lo anterior hace que sea thread-safe, así que quienes consuman de las colas sicnronizadas no deberán preocuparse por locks y demás
 * métodos de acceso, simplemente encolar y desencolar.
 * 
 * @author rodrigo
 */
public class Atributos {
	// Atributos
	// -----------------------------------------------------------------------------------------------
	// Colas de tareas, para interacción con otros NABC
	public static String[] colasDisponibles = {"entrada", "salida", "interna"};
	private static HashMap<String,Object> locksColas;
	private static HashMap<String,ArrayList<Tarea>> colas;
	
	// Direcciones (cada nodo podrá correr varios servidores para los distintos tipos de nodos, en distintos puertos)
	private static volatile HashMap<String, Comparable> network =  new HashMap<String, Comparable>();
	
	// Constantes
	private final int MAX_QUEUE_SIZE = 100;
	
	
	// Métodos
	// -----------------------------------------------------------------------------------------------
	// Colas de transmisión
	public void setNombreColas(String[] nombres) {
		// Este método modifica los atributos de la clase (static) por eso hay que tener mucho cuidado de donde
		// se lo llama. Debería usarse sólo en la clase general de cada nodo, antes de que los demás componentes (del nodo)
		// instancien los atributos.
		colasDisponibles = nombres;
	}
	
	public void setColas() {
		// Este método modifica los atributos de la clase (static) por eso hay que tener mucho cuidado de donde
		// se lo llama. Debería usarse sólo en la clase general del nodo, antes de instanciar los demás
		// componentes que usan las colas de Tx
		colas = new HashMap<String,ArrayList<Tarea>>();
		locksColas = new HashMap<String,Object>();
		
		for (String key : colasDisponibles) {
			colas.put(key, new ArrayList<Tarea>());
			locksColas.put(key, new Object());
		}
	}

	public boolean colaVacia(String cola) {
		synchronized (locksColas.get(cola)) {
			return colas.get(cola).isEmpty();
		}
	}
	
	public Tarea desencolar(String cola) throws InterruptedException{
		Tarea task = null;
		
		synchronized (locksColas.get(cola)) {
			if (colas.get(cola).isEmpty())
				locksColas.get(cola).wait();
			
			task = colas.get(cola).remove(0);
			locksColas.get(cola).notifyAll();
			
			return task;
		}
	}
	
	public void encolar(String cola, Tarea carga) throws InterruptedException{
		synchronized (locksColas.get(cola)) {
			if (colas.get(cola).size() == MAX_QUEUE_SIZE)
				locksColas.get(cola).wait();
				
			colas.get(cola).add(carga);
			locksColas.get(cola).notifyAll();
		}
	}

	
	// Direcciones de red
	public void setDirecciones(String ip, Integer acceso, Integer centrales, Integer hojas) {
		// Método que modifica los atributos de la clase (static). Tener cuidado cuando y dónde se lo llama
		network.put("ip", ip);
		network.put("acceso", acceso);
		network.put("centrales", centrales);
		network.put("hojas", hojas);
	}
	
	public String getDireccion(String puerto) {
		if (network.containsKey("ip") && network.containsKey(puerto))
			return network.get("ip") + ":" + Integer.toString((int) network.get(puerto));
		else
			return "--.--.--.--:--";
	}

	
	// Getters grales.
	public Object getLockCola(String cola) {
		synchronized(locksColas) {
			return locksColas.get(cola);
		}
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
