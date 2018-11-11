
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
		
		System.out.println("Si me ves, preparate a ver que hacer con el hilo consumidor");
		System.out.println("DBG");
		
		/*while(true) {
			//Pause for 30 seconds
            try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("\n\t-> Estado del hilo: " + hiloCliente.getState());
		}*/
		
		
		try {
			hiloProductor.join();
			hiloServidor.interrupt();
			for(int i=0; i<hilosConsumidores.length; i++){ hilosConsumidores[i].interrupt(); }
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.exit(0);
	}

}// Fin clase
