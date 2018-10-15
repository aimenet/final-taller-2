import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;

public class Borrar1 {
	private static ArrayList<Integer> lista;
	
	
	public static void main(String[] args) {
		
		// Actualizando datos de un diccionario 
		// -----------------------------------------------------------------------------------------------
		HashMap<String, Integer> map1 = new HashMap<String, Integer>();
		
		// El "viejo"
		map1.put("Uno", 10);
		map1.put("Dos", 20);
		map1.put("Tres", 30);
		map1.put("Cuatro", 40);
		map1.put("Cinco", 50);
		
		// La actualización
		map1.put("Tres", 300);
		map1.put("Cuatro", 400);
		
		for(String clave : map1.keySet()){
			System.out.println(clave + ": " + map1.get(clave));
		}
		
		System.exit(0);
				
				
		
		// Identificando la clase de un objeto 
		// -----------------------------------------------------------------------------------------------
		ArrayList<Object[]> objeto1 = new ArrayList<Object[]>();
		
		if(objeto1.getClass() == ArrayList.class){
			System.out.println("Es un ArrayList");
		} else {
			System.out.println("No tengo idea qué es");
		}
		
		System.exit(0);
		
		
		// Algunas pruebas con threads 
		// -----------------------------------------------------------------------------------------------
		//Ejemplo productor - consumidor 3: como ejemplo 2 pero produciendo en varias colas (y con modo manual)
		List<Integer> taskQueue1 = new ArrayList<Integer>();
		List<Integer> taskQueue2 = new ArrayList<Integer>();
		int MAX_CAPACITY = 5;
		Thread tProducer = new Thread(new Producer2(taskQueue1, taskQueue2, MAX_CAPACITY), "Producer");
		Thread tConsumer1 = new Thread(new Consumer2(1,taskQueue1), "Consumer 1");
		Thread tConsumer2 = new Thread(new Consumer2(2,taskQueue2), "Consumer 2");
		tProducer.start();
		tConsumer1.start();
		tConsumer2.start();
		
		try {Thread.sleep(60000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println("\n\nChau");
		System.exit(0);
		//-----------------------------------------------------------------------------------------
		//Ejemplo productor - consumidor 2: uso un poco mejor de wait/notify
		//Lo comento para repetir nombres en el bloque de arriba
		/*List<Integer> taskQueue = new ArrayList<Integer>();
		int MAX_CAPACITY = 5;
		Thread tProducer = new Thread(new Producer(taskQueue, MAX_CAPACITY), "Producer");
		Thread tConsumer = new Thread(new Consumer(taskQueue), "Consumer");
		tProducer.start();
		tConsumer.start();
		
		try {Thread.sleep(10000);}
		catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println("\n\nChau");
		System.exit(0);*/
		//-----------------------------------------------------------------------------------------
		//Ejemplo productor - consumidor 1: no es muy "fino" el uso de wait/notify pero funca  
		/*Integer contador_prod_1 = 0;
		Integer contador_prod_2 = 0;
		Integer contador_con_1 = 0;
		Integer contador_con_2 = 0;
		Productor productor = new Productor();
		Consumidor consumidor1 = new Consumidor(productor, 1);
		Consumidor consumidor2 = new Consumidor(productor, 2);
		
		productor.start();
		consumidor1.start();
		consumidor2.start();
		
		System.out.println("\n\tHilo P: " + productor.getState());
		System.out.println("\n\tHilo C1: " + consumidor1.getState());
		System.out.println("\n\tHilo C2: " + consumidor2.getState());
		
		try { Thread.sleep(10000); }
		catch (InterruptedException e) { e.printStackTrace(); }
		
		productor = null;
		consumidor1 = null;
		consumidor2 = null;
		
		System.exit(0);*/
		

		// Ejemplo de división con % 
		// -----------------------------------------------------------------------------------------------

		// Con %4 puedo controlar que el resultado de la división siempre C [0,3]
		Integer numero = 5;
		System.out.println(numero+" % 4 debería ser 1 -> " + (numero%4) );
		System.exit(0);
		
		
		// Ejemplo de arreglo para almacenar Objects (y así guardar instancias de distintas clases) 
		// -----------------------------------------------------------------------------------------------
		Object[][] matriz = new Object[2][5];
		
		matriz[0][0] = "Fila 0 Columna 0";
		matriz[1][0] = 0;
		
		matriz[0][1] = 567;
		matriz[1][1] = "567";
		
		System.out.println(matriz.length);
		System.out.println(matriz[0].length);
		
		for(int i=0; i<matriz[0].length; i++){
			System.out.println("matriz[0]["+i+"] = " + matriz[0][i]);
			System.out.println("matriz[1]["+i+"] = " + matriz[1][i]);
		}
		
		// -----------------------------------------------------------------------------------------------	
		
		String cadena = "192.168.0.22:9898;192.168.0.22:9899";
		
		if(Consts.VERDADERO){
			System.out.println("Cadena antes: " + Consts.cadena);
			Consts.setCadena("Chau");
			System.out.println("Cadena después: " + Consts.cadena);
		}
		
		// -----------------------------------------------------------------------------------------------		
		
		ArrayList<Integer> numeros = new ArrayList<Integer>();
		numeros.add(5);numeros.add(2);numeros.add(3);
		numeros.toArray();
		System.out.println(numeros);
		
		
		
		String[][] nombres = new String[3][];
		String[] aux;
		
		aux = new String[2];
		aux[0] = "Ana";
		aux[1] = "Alberto";
		nombres[0] = aux;
		
		aux = new String[1];
		aux[0] = "Beto";
		nombres[1] = aux;
		
		aux = new String[3];
		aux[0] = "Carlos";
		aux[1] = "Claudia";
		aux[2] = "Cirilo";
		nombres[2] = aux;
		
		System.out.println(nombres.length);
		System.out.println(nombres[0].length);
		System.out.println(nombres[1].length);
		System.out.println(nombres[2].length);
		
		
		
		
		
		BorrarInterface atributoVariable1;
		BorrarInterface atributoVariable2;
		
		atributoVariable1 = new clase1();
		atributoVariable2 = new clase2();
		
		atributoVariable1.metodo();
		atributoVariable2.metodo();
		
		Integer[] origen = {1,2,3,4,5,6};
		Integer[] destino = new Integer[4];
		
		System.arraycopy(origen, 0, destino, 0, 4);
		
		for(int i=0; i<destino.length; i++){
			System.out.println(destino[i]);
		}
		
		if (lista != null) {
			System.out.println("Está definido");
		} else {
			System.out.println("No está definido");
		}
		
	}

}


class clase1 implements BorrarInterface {
	@Override
	public void metodo() {
		System.out.println("Clase1.metodo()");
	}
}
// -------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
class clase2 implements BorrarInterface {
	@Override
	public void metodo() {
		System.out.println("Clase2.metodo()");
	}
}
//-------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
final class Consts {
	  public static final boolean VERDADERO = true;
	  public static String cadena = "hola!";
	  
	  public static void setCadena(String foo){
		  cadena = foo;
	  }
	  
	  private Consts(){
		  //this prevents even the native class from 
		  //calling this ctor as well :
		  throw new AssertionError();
	  }
}
//-------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
class Hilo implements Runnable {
	public int id;
	public int contador = 0;
	public static Object compartido = new Object();
	
	public Hilo(int id){
		this.id = id;
}

	@Override
	public void run() {
		while(true){
			synchronized (compartido) {
				contador += id;
			}
		}
	}
}
//-------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
class Productor extends Thread {
	 
    static final int MAXQUEUE = 5;
    private Vector messages1 = new Vector();
    private Vector messages2 = new Vector();
    Integer contador1, contador2;
 
    public Productor(){
    	this.contador1 = 0;
    	this.contador2 = 0;
    }
    
    @Override
    public void run() {
        try {
            //while (true) {
        	for(int i=0; i<10; i++){
                putMessage(i);
                sleep(500);
            }
        	System.out.println("\nMsjs 1 producidos: " + contador1);
        	System.out.println("Msjs 2 producidos: " + contador2);
        } catch (InterruptedException e) {
        }
    }
 
    private synchronized void putMessage(Integer i) throws InterruptedException {
        /*while (messages.size() == MAXQUEUE) {
            wait();
        }*/
        messages1.addElement("Msj 1: " + i);
        contador1 += 1;
        messages2.addElement("Msj 2 (1): " + i);
        contador2 += 1;
        messages2.addElement("Msj 2 (2): " + i);
        contador2 += 1;
        System.out.println("put messages");
        notify();
        //Later, when the necessary event happens, the thread that is running it calls notify() from a block synchronized on the same object.
    }
 
    // Called by Consumer 1
    public synchronized String getMessage1() throws InterruptedException {
        //notify();
        while (messages1.size() == 0) {
            wait();//By executing wait() from a synchronized block, a thread gives up its hold on the lock and goes to sleep.
        }
        String message = (String) messages1.firstElement();
        messages1.removeElement(message);
        return message;
    }
    
    // Called by Consumer 2
    public synchronized String getMessage2() throws InterruptedException {
        //notify();
        while (messages2.size() == 0) {
            wait();//By executing wait() from a synchronized block, a thread gives up its hold on the lock and goes to sleep.
        }
        String message = (String) messages2.firstElement();
        messages2.removeElement(message);
        return message;
    }
}

class Consumidor extends Thread { 
    int id;
	Productor productor;
	Integer contador1, contador2;
    
 
    Consumidor(Productor p, int id) {
        productor = p;
        this.id = id;
        if(id == 1){
        	contador1 = 0;
        } else {
        	contador2 = 0;
        }
    }
 
    @Override
    public void run() {
        try {
            while (true) {
            	if(this.id == 1){
            		String message = this.productor.getMessage1();
            		System.out.println("Got message1: " + message);
            		contador1 += 1;
            		//sleep(200);
            	} else {
            		String message = productor.getMessage2();
            		System.out.println("Got message2: " + message);
            		contador2 += 1;
            		//sleep(200);
            	}
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
//-------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
class Consumer implements Runnable {
	private final List<Integer> taskQueue;
 
	public Consumer(List<Integer> sharedQueue) {
		this.taskQueue = sharedQueue;
	}
 
	@Override
	public void run() {
		while (true) {
			try{consume();}
			catch (InterruptedException ex){ex.printStackTrace();}
		}
	}
 
	private void consume() throws InterruptedException{
		synchronized (taskQueue){
			while (taskQueue.isEmpty()){
				System.out.println("Queue is empty " + Thread.currentThread().getName() + " is waiting , size: " + taskQueue.size());
				taskQueue.wait();
			}

			int i = (Integer) taskQueue.remove(0);
			System.out.println("Consumed: " + i);
			taskQueue.notifyAll();
		}
		Thread.sleep(new Random().nextInt(800-400) + 400);
	}
}

class Producer implements Runnable{
	private final List<Integer> taskQueue;
	private final int           MAX_CAPACITY;
 
	public Producer(List<Integer> sharedQueue, int size) {
		this.taskQueue = sharedQueue;
		this.MAX_CAPACITY = size;
	}
 
	@Override
	public void run() {
		int counter = 0;
		//while (true) {
		while (counter < 10) {
			try { produce(counter++); }
			catch (InterruptedException ex) { ex.printStackTrace(); }
		}
	}
 
	private void produce(int i) throws InterruptedException {
		synchronized (taskQueue) {
			while (taskQueue.size() == MAX_CAPACITY) {
				System.out.println("Queue is full " + Thread.currentThread().getName() + " is waiting , size: " + taskQueue.size());
				taskQueue.wait();
			}
           
			taskQueue.add(i);
			System.out.println("Produced: " + i);
			taskQueue.notifyAll();
		}
		Thread.sleep(new Random().nextInt(500-100) + 100);
	}
}
//-------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------
class Consumer2 implements Runnable {
	//Nada más le agrego un ID para mostrar en las salidas por pantalla
	private final List<Integer> taskQueue;
	public Integer id;

	public Consumer2(Integer id, List<Integer> sharedQueue) {
		this.id = id;
		this.taskQueue = sharedQueue;
	}

	@Override
	public void run() {
		while (true) {
			try{consume();}
			catch (InterruptedException ex){ex.printStackTrace();}
		}
	}

	private void consume() throws InterruptedException{
		synchronized (taskQueue){
			while (taskQueue.isEmpty()){
				System.out.println(id + ") Queue is empty " + Thread.currentThread().getName() + " is waiting , size: " + taskQueue.size());
				taskQueue.wait();
			}

			int i = (Integer) taskQueue.remove(0);
			System.out.println(id + ") Consumed: " + i);
			taskQueue.notifyAll();
		}
		Thread.sleep(new Random().nextInt(800-400) + 400);
	}
}

class Producer2 implements Runnable{
	//Ahora produce en dos colas
	private final List<Integer> taskQueue1;
	private final List<Integer> taskQueue2;
	private final int           MAX_CAPACITY;

	public Producer2(List<Integer> sharedQueue1, List<Integer> sharedQueue2, int size) {
		this.taskQueue1 = sharedQueue1;
		this.taskQueue2 = sharedQueue2;
		this.MAX_CAPACITY = size;
	}

	@Override
	public void run() {
		// Modo 1
		/*int counter = 0;
		//while (true) {
		while (counter < 10) {
			try { produce(counter++); }
			catch (InterruptedException ex) { ex.printStackTrace(); }
		}*/
		
		// Modo 2
		/*try {
			produce(counter++);
			produce(counter++);
			produce(counter++);
			
			produce2(counter++);
			produce2(counter++);
			
			produce(counter++);
		}
		catch (InterruptedException ex) { ex.printStackTrace(); }*/
		
		// Modo 3
		/*try {
			for(int i=0; i<20; i++){
				if(new Random().nextInt(10) <= 4){
					produce(i);
				} else {
					produce2(i);
				}
			}
		} catch (InterruptedException ex) { ex.printStackTrace(); }*/
		
		// Modo "manual" -> implica descomentar el primer notifyAll() y comentar el segundo, en los métodos productores
		boolean terminar = false;
		Scanner teclado = new Scanner(System.in);
		String opcion;
		Integer contador = 0;
		while(!terminar){
			System.out.println("\n\n----------");
			System.out.println("[G_1] [G_2] [N_1] [N_2]");
			opcion = teclado.nextLine();
			switch(opcion){
				case "1":
					try {produce(contador);contador++;}
					catch (InterruptedException e) {e.printStackTrace();}
					break;
				case "2":
					try {produce2(contador);contador++;}
					catch (InterruptedException e) {e.printStackTrace();}
					break;
				case "3":
					synchronized (taskQueue1) {taskQueue1.notifyAll();}
					break;
				case "4":
					synchronized (taskQueue2) {taskQueue2.notifyAll();}
					break;
			}
		}
		

	}

	private void produce(int i) throws InterruptedException {
		synchronized (taskQueue1) {
			while (taskQueue1.size() == MAX_CAPACITY) {
				System.out.println("Queue 1 is full " + Thread.currentThread().getName() + " is waiting , size: " + taskQueue1.size());
				taskQueue1.notifyAll(); //(modo manual) Si no se notifica, se bloquea el hilo porque la cola está llena y nadie la vacía
				taskQueue1.wait();
			}
         
			taskQueue1.add(i);
			System.out.println("Produced for 1: " + i);
			//taskQueue1.notifyAll(); //comentar en modo manual
		}
		Thread.sleep(new Random().nextInt(500-100) + 100);
	}
	
	private void produce2(int i) throws InterruptedException {
		synchronized (taskQueue2) {
			while (taskQueue2.size() == MAX_CAPACITY) {
				System.out.println("Queue 2 is full " + Thread.currentThread().getName() + " is waiting , size: " + taskQueue2.size());
				taskQueue2.notifyAll(); //(modo manual) Si no se notifica, se bloquea el hilo porque la cola está llena y nadie la vacía
				taskQueue2.wait();
			}
         
			taskQueue2.add(i);
			System.out.println("Produced for 2: " + i);
			//taskQueue2.notifyAll(); //Comentar en modo manual
		}
		Thread.sleep(new Random().nextInt(500-100) + 100);
	}
}