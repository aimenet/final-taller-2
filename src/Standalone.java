import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import commons.Tarea;
import nodes.components.AtributosAcceso;

public class Standalone {

	private static Integer method1(String s) {
		System.out.println("Method 1");
		return 1;
	}
	
	private static Integer method2(String s) {
		System.out.println("Method 2");
		return 0;
	}
	
	public static void OtraClase() {
		Otra instancia = new Otra();
        
        if (instancia.funcMap.get("First").apply("al pedo"))
        	System.out.println("first dio TRUE");
        
        if (instancia.funcMap.get("Second").apply("al pedo"))
        	System.out.println("second dio TRUE");
        else
        	System.out.println("second dio FALSE");
	}
	
	public static void Timestamps() {
		long diffInMS;
		long diffInS;
		Timestamp marca1;
		Timestamp marca2;
		TimeUnit timeUnit = null;
        
		marca1 = new Timestamp(System.currentTimeMillis());
		
		try {Thread.sleep(5000);} catch (InterruptedException e) {e.printStackTrace();}
		
		marca2 = new Timestamp(System.currentTimeMillis());
		
		System.out.printf("%s - %s: ", marca2.toString(), marca1.toString());
		
		diffInMS = marca2.getTime() - marca1.getTime();
		diffInS = TimeUnit.SECONDS.convert(diffInMS, TimeUnit.MILLISECONDS);
		
		System.out.printf("%s segundos\n", diffInS);
	}
	
	
	public static void mutableDict(String[] args) {
		/* Voy a probar si los diccionarios acá son mutables como en python */
		HashMap<String, HashMap<String, Comparable>> diccionarioAnidado = new HashMap<String, HashMap<String, Comparable>>(); 
		HashMap<String, Comparable> nodo;
		
		nodo = new HashMap<String, Comparable>();
		nodo.put("nombre", "john");
		nodo.put("apellido", "doe");
		diccionarioAnidado.put("uno", nodo);

		nodo = new HashMap<String, Comparable>();
		nodo.put("nombre", "juan");
		nodo.put("apellido", "perez");
		diccionarioAnidado.put("dos", nodo);
		
		for (Entry<String, HashMap<String, Comparable>> me : diccionarioAnidado.entrySet())
			((HashMap<String, Comparable>) me.getValue()).put("apellido", "modificado");
	
		for (Entry<String, HashMap<String, Comparable>> me : diccionarioAnidado.entrySet()) {
			System.out.printf("%s: ", me.getKey());
			System.out.printf("%s ", ((HashMap<String, Comparable>) me.getValue()).get("nombre"));
			System.out.printf("%s\n", ((HashMap<String, Comparable>) me.getValue()).get("apellido"));
		}
	}


	public static void main(String[] args) {
		System.out.println(Math.pow(3,2));
	}
}

class Otra {
	public HashMap<String, Function<String, Boolean>> funcMap = new HashMap<>();
	
	public Boolean method1(String s) {
		System.out.println("Method 1");
		return true;
	}
	
	public Boolean method2(String s) {
		System.out.println("Method 2");
		return false;
	}
	
	public Otra() {
		funcMap.put("First", this::method1);
	    funcMap.put("Second", this::method2);
	}
}
