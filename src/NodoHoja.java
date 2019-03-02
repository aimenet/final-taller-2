
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Nodo Hoja del sistema distribuido. Estos poseen las imágenes a compartir, calculan sus vectores
 * característicos, emiten consultas, respuestas y calculan similitud entre imágenes.
 * Están conectadas a dos Nodos Centrales.
 * 
 * La faceta cliente responde a un esquema Productor/Consumidor: un thread (gui) se encarga de producir las consultas
 * que los hilos consumidores (valga la redundancia) enviarán a los NCs.
 *
 * @author rodrigo
 *
 */
public class NodoHoja {
	// Atributos
	//===========
	private AtributosHoja atributos;
	private Properties config;
	private Servidor servidor;
	private Thread hiloProductor;
	private Thread hiloServidor;
	private Thread[] hilosConsumidores;
	

	// Métodos
	//=========
	public NodoHoja(String configFile){
		Integer cantCentrales;
		String[] centrales;
		
		try {
			config = new Properties();
			config.load( new FileInputStream(configFile) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		//NCs a los que se conectará la H
		cantCentrales = Integer.parseInt(config.getProperty("max_nc"));
		centrales = new String[ cantCentrales ];
		hilosConsumidores = new Thread[ cantCentrales ];
		for(int i=1; i<=cantCentrales; i++)
			centrales[i-1] = config.getProperty("nc_"+i);
		
		// Seteo de los atributos de la H.
		atributos = new AtributosHoja(); // Acá debería ir la dirección del/los nodos centrales
		atributos.setIpServidor( config.getProperty("ip") );
		atributos.setPuertoServidor( Integer.parseInt(config.getProperty("puerto_server")) );
		atributos.setDireccionesNCs(centrales);
		
		// Hilo Servidor de la H, donde recibe consultas y respuestas
		servidor = new Servidor(Integer.parseInt(config.getProperty("puerto_server")),
								"Bla bla bla",
								ConsultorH.class);
		
		// Hilos Clientes (esquema Productor/Consumidor)
		// Primero se instancia al hilo Controlador, el productor de consultas. 
		hiloProductor = new Thread( new ControladorHoja() );
		// Luego se instancian los consumidores, quienes enviarán dichas consultas a los NCs.
		for(int i=0; i<cantCentrales; i++)
			hilosConsumidores[i] = new Thread( new HojaConsumidor(i,
					centrales[i].split(":")[0],
					Integer.parseInt(centrales[i].split(":")[1])) );
		
		// Hilo servidor
		hiloServidor = new Thread( this.servidor );
	}
	
	
	/**
	 * En el método ppal. de la Hoja debo poner a correr los hilos y definir una barrera: cuando el hilo Cliente
	 * termina, termina la ejecución del Nodo. Esto es importante así puedo "acceder" al hilo desde afuera
	 * para comunicarlo con la interfaz gráfica (espero).
	 */
	
	public void ponerEnMarcha(){
		hiloProductor.start();
		hiloServidor.start();
		
		for(int i=0; i<hilosConsumidores.length; i++){
			hilosConsumidores[i].start();
		}
		
		// Acá estaría muy bueno hacer un bucle que corra mientras los hilos estén activos para monitorearlos
		//System.out.println("El código sigue después de poner al hilo en marcha");
		//System.out.println("Estado del hilo: " + hiloCliente.getState());
		
		System.out.println("Si me ves, preparate a ver qué hacer con el hilo consumidor");
		System.out.println("DBG");
		
		// Hago esto por hora nada más
		/*int counter = 0;
		while(true) {
			//Pause for 10 seconds
            try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            // Básico: muestra el estado de cada thread
            /*for(int i=0; i<hilosConsumidores.length; i++){
            	System.out.print(hilosConsumidores[i].getName() + " || ");
    			System.out.print("Consumer thread #" + Integer.toString(i) + " state: ");
    			System.out.println(hilosConsumidores[i].getState().toString());
    		}*/
            
            // Simulo caída y generación de un thread
            /*counter += 1;
            if(counter == 3) {
	        	// Interrumpo el thread
            	System.out.println("\nThread state before interrupt: " + hilosConsumidores[0].getState().toString());
	        	hilosConsumidores[0].interrupt();
	        	// Agrego un delay para que efectivamente se interrumpa el thread
	        	try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
	        	System.out.println("Thread state after interrupt:  " + hilosConsumidores[0].getState().toString());
	        	
	        	// Muestro el estado del hilo que acabo de interrumpir
	        	System.out.println("\nThread alive:       " + hilosConsumidores[0].isAlive());	     
	        	System.out.println("Thread interrupted: " + hilosConsumidores[0].isInterrupted());
	        	
	        	// Instancio el nuevo hilo. Es importante usar el ID del que va a reemplazar para que utilice
	        	// las colas de Tx/Rx del anterior
	        	System.out.print("\nSetting up thread: ");
	        	hilosConsumidores[0] = new Thread( new HojaConsumidor(0,
	        			atributos.getDireccionesNCs()[0].split(":")[0],
						Integer.parseInt(atributos.getDireccionesNCs()[0].split(":")[1])) );
	        	System.out.println("DONE");
	        	
	        	// Inicio el nuevo hilo
	        	System.out.println("\nThread state before start: " + hilosConsumidores[0].getState().toString());
	        	hilosConsumidores[0].start();
	        	System.out.println("Thread state after start:  " + hilosConsumidores[0].getState().toString());
            } else {
            	for(int i=0; i<hilosConsumidores.length; i++){
            		System.out.print(hilosConsumidores[i].getName() + " || ");
            		System.out.print("Consumer thread #" + Integer.toString(i) + " state: ");
	    			System.out.println(hilosConsumidores[i].getState().toString());
	    		}
            }*/
               
		/*}*/
		
		
		while(this.hiloProductor.getState() != Thread.State.TERMINATED) {
			//Pause for 10 seconds
            try {Thread.sleep(10000);}
            catch (InterruptedException e) {e.printStackTrace();}
            
            System.out.println("[HOJA] Waiting until Producer stop...");
		}
		
		// TODO: ¿debería controlar que la salida del while anterior sea porque efectivamente se salió desde
		//       el menú?
		
		// Detención de todos los threads en ejecución
		for(int i=0; i<hilosConsumidores.length; i++){ hilosConsumidores[i].interrupt(); }
		hiloProductor.interrupt();
		hiloServidor.interrupt(); // Por alguna razón esto no para al thread servidor
		
		// Esto no sirver, el hilo servidor queda corriendo indefinidamente
		/*while(hiloServidor.getState() != Thread.State.TERMINATED) {
			hiloServidor.interrupt();
			System.out.println("\t\tX");
		}*/

		try {Thread.sleep(1000);}
        catch (InterruptedException e) {e.printStackTrace();}
		
		System.out.println("[HOJA] Finalizando ejecución...");
		for(int i=0; i<hilosConsumidores.length; i++){
			System.out.print("\tconsumer thread #"+ Integer.toString(i) +" state: ");
			System.out.println(hilosConsumidores[i].getState().toString());
		}
		System.out.println("\tproducer thread state: " + hiloProductor.getState().toString());
		System.out.println("\tserver thread state: " + hiloServidor.getState().toString());
		System.out.println("\n[HOJA] Terminada");
		System.exit(0);
	}

}// Fin clase

/* 
 * Los threads corren una instancia de una clase. Desde acá, o sea usando el hilosConsumidores[i]
 * accedo al thread pero no tengo manera de llegar a a instancia que ejecuta ese hilo a menos que guarde dicha
 * instancia en un arreglo. Por ahora para lo único que se me ocurre que podría llegar a necesitarla
 * es para ver el ID de consumidor del que se trata, pero nada más.
 * 
 * En su lugar lo hago fácil y hago que coincida el índice de hilosConsumidores con el ID de consumidor y listo
 * 
 * Lo que digo es que lo primero no se puede hacer y lo segundo sí:
 * 
 * System.out.print("\nSetting up thread: ");
 * hilosConsumidores[0] = new Thread( new HojaConsumidor(0,
 *	        			atributos.getDireccionesNCs()[0].split(":")[0],
 *						Integer.parseInt(atributos.getDireccionesNCs()[0].split(":")[1])) );
 * System.out.println("DONE");
 * System.out.println("Instancia que corre el hilo: " + Integer.toString(hilosConsumidores[i].idConsumidor));
 *
 *
 * HojaConsumidor consumidor = new HojaConsumidor(0,atributos.getDireccionesNCs()[0].split(":")[0],Integer.parseInt(atributos.getDireccionesNCs()[0].split(":")[1]));
 * hilosConsumidores[0] = new Thread( consumidor );
 * System.out.println("DONE");
 * System.out.println("Instancia que corre el hilo: " + Integer.toString(consumidor.idConsumidor));
 * 
 *  
 * */
