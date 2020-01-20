import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;

class SharedQueue {
	// Según entiendo para sincronizar a nivel de clase tengo que usar
	//     synchronized(SharedQueue.lock)
	// Para sincronizar a nivel de instancia tengo que hacerlo con
	//     synchronized(this.lock)
	
	private static ArrayList<String> queue = new ArrayList<String>();
	public static final Object lock = new Object();
	
	// No uso constructor porque defino los atributos "en la clase", sino cada instancia los pisaría
	// El los métodos creo que no tengo que usar "this.{queue|lock}" porque eso hace referencia a las variables de
	// cada instancia en particular, cuando lo que quiero es que todas las instancias usen los mismos atributos
	// así los sincronizo
	// Creo que no poner this equivale a hacer "SharedQueue.{queue|lock}"
	
	public static ArrayList<String> getQueue() {
		synchronized (lock) {
			return queue;
		}
	}

	public boolean isEmpty() {
		synchronized (lock) {
			return queue.isEmpty();
		}
	}
	
	public String pop(){
		synchronized (lock) {
			return queue.remove(0);
		}
	}
	
	public void push(String task){
		synchronized (lock) {
			queue.add(task);
			lock.notifyAll();
		}
	}
	
}


class MyProducer implements Runnable {
	SharedQueue syncQueue = new SharedQueue();
	
	public void run() {
		for (int i=0; i < 10; i++) {
			try {Thread.sleep((long)(Math.random() * 1000));} 
			catch (InterruptedException e) {e.printStackTrace();}
			
			SecureRandom random = new SecureRandom();
			byte bytes[] = new byte[4];
			random.nextBytes(bytes);
			Encoder encoder = Base64.getUrlEncoder().withoutPadding();
			String randomString = encoder.encodeToString(bytes);
		    System.out.println("\tProductor: generado " + randomString + "\n");
			
		    this.syncQueue.push(randomString);
		}
	}
}


class MyConsumer implements Runnable {
	private Integer id;
	SharedQueue syncQueue = new SharedQueue();
	
	public MyConsumer(Integer id_number) {
		this.id = id_number;
	}
	
	public void run() {
		String task = "SI ME VES ALGO SALIÓ MAL";
		
		while (true) {
			try {Thread.sleep((long)(Math.random() * 1000));} 
			catch (InterruptedException e) {e.printStackTrace();}
			
		    	while(this.syncQueue.isEmpty()) {
		    		//System.out.print("\tConsumidor " + this.id.toString() + ": ");
		    		//System.out.println("esperando a que haya tareas en cola");
		    	}
		    	
		    	try {
		    		task = this.syncQueue.pop();
		    	} catch (IndexOutOfBoundsException e) {}
			
			System.out.print("\tConsumidor " + this.id.toString() + ": ");
			System.out.println("consumido " + task);
			
			try {Thread.sleep((long)(Math.random() * 1000));} 
			catch (InterruptedException e) {e.printStackTrace();}
		}
	}
}



public class PruebaProdCons {	
	public static void main(String[] args) {
		Thread hiloProductor;
		Thread[] hilosConsumidores;
		
		hiloProductor = new Thread(new MyProducer());
		hilosConsumidores = new Thread[3];
		
		for (int i=0; i<hilosConsumidores.length; i++) {
			hilosConsumidores[i] = new Thread(new MyConsumer(i));
			hilosConsumidores[i].start();
			System.out.println("Consumidor " + Integer.toString(i) + ": iniciado");
		}
		System.out.println("Consumidores iniciados");
		
		hiloProductor.start();
		System.out.println("Productor iniciado");
		
		try {
			hiloProductor.join();
			for (int i=0; i<hilosConsumidores.length; i++) {
				hilosConsumidores[i].join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

