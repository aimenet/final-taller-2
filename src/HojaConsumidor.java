import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Uno de las instancias que compone la "faceta" Cliente de un Nodo Hoja. Es la encargada de 
 * conectarse a un Nodo Central e interactuar con él. Consume de la cola de transmisión que le fue asignada,
 * enviando tanto consultas como listado de imágenes a compartir
 * 
 * @author rodrigo
 *
 */

public class HojaConsumidor implements Runnable {
	// Atributos
	// =========
	private AtributosHoja atributos;
	private ConexionTcp conexionConNodoCentral;
	private boolean conexionEstablecida, sesionIniciada;
	public Integer idConsumidor, puertoNC;
	public String idAsignadoNC, ipNC;



	// Métodos
	// =======
	public HojaConsumidor(Integer idConsumidor, String ipNodoCentral, Integer puertoNodoCentral) {
		ipNC = ipNodoCentral;
		puertoNC = puertoNodoCentral;
		atributos = new AtributosHoja();
		conexionEstablecida = false;
		sesionIniciada = false;
		this.idConsumidor = idConsumidor;
		try {
			this.conexionConNodoCentral = new ConexionTcp(ipNodoCentral, puertoNodoCentral);
			conexionEstablecida = true;
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el servidor");
			// TODO: ver como capturar el error y parar el consumidor sin detener el Nodo (this.wait() no sirve)
		}
	}

	@Override
	public void run() {
		// TODO: ¿loop infinito? Pensarlo bien
		
		
		// TODO: <2019-02-16> Comento mientras pruebo como parar y restartear un thread
		if(establecerConexionNodoCentral()){
			System.out.println("Consumidor " + idConsumidor +": iniciada sesión en NC");
			sesionIniciada = true;
		} else {
			System.out.println("Consumidor " + idConsumidor +": imposible iniciar sesión en NC");
			// TODO: ver como capturar el error y parar el consumidor sin detener el Nodo (this.wait() no sirve)
			//       Creo que va por el lado de matar este hilo (dejar que muera, interrumpirlo, algo de eso)
			//		 y revivirlo en un bucle en NodoHoja.java
		}
		
		// <2019-03-01> Comento esto para implementar una interrupción del consumidor desde el menú ppal
		/*while (true) {
			try{consumir2();}
			catch (InterruptedException ex){ex.printStackTrace();}
			
		}*/
		boolean runFlag = true;
		while (runFlag) {
			try{
				consumir2();
			} catch (InterruptedException ex){
				// TODO: debería usar una excepción propia? Al menos para terminar manualmente el thread
				ex.printStackTrace();
				runFlag = false;
			}
		}
		
		//
		//try {Thread.sleep(60000);}
		//catch (InterruptedException e) {e.printStackTrace(); /*Acá debería estar terminado si no entiendo mal*/}
		
		
	}
	
	// TODO: Renombrar a "consumir"
	private void consumir2() throws InterruptedException{
		/**/
		ArrayList<CredImagen> muchas = null;
		CredImagen una = null;
		ArrayList<Object> colaPropia = atributos.getColasTx()[idConsumidor];
		Tupla2<Object,String> tarea;
		Tupla2<String,CredImagen> solicitudDescarga;
		String direccionServidor;
		
		synchronized (colaPropia){
			while ( colaPropia.isEmpty() ){
				System.out.println("Consumidor " + idConsumidor + " esperando. Tamaño cola: " + colaPropia.size());
				colaPropia.wait();
			}

			tarea = (Tupla2<Object,String>) colaPropia.remove(0);
			colaPropia.notifyAll();
		}
		
		direccionServidor = String.format("%s:%d", atributos.getIpServidor(), atributos.getPuertoServidor());
		
		switch(tarea.getSegundo()){
			case "QUERY": //Consulta a NC
				una = (CredImagen) tarea.getPrimero();
				conexionConNodoCentral.enviarSinRta(new Mensaje(idAsignadoNC,direccionServidor,4,una));
				System.out.println("Consumidor "+idConsumidor+" : enviada consulta por "+una.getNombre());
				break;
				
			case "DESCARGA": //Descarga de H
				String ipServerH = atributos.getIpServidor() + ":" + atributos.getPuertoServidor();
				solicitudDescarga = (Tupla2<String,CredImagen>) tarea.getPrimero();
				
				// El primer elemento es la dirección (y puerto) de la H de la que descarga
				// El segundo es la creddencial de la imagen que se desea descargar.
				
				String ipDestino = solicitudDescarga.getPrimero().split(":")[0];
				Integer portDestino = Integer.parseInt( solicitudDescarga.getPrimero().split(":")[1] );
				
				ConexionTcp conexionTmp = null;
				try { conexionTmp = new ConexionTcp(ipDestino, portDestino); }
				catch (IOException e) { System.out.println("No se pudo establecer conexión con el Nodo Hoja"); return;}
				
				//Mensaje respuesta = (Mensaje) conexionTmp.enviarConRta(new Mensaje(0,21,solicitudDescarga.getSegundo().getNombre()));
				//Imagen descargada = (Imagen) respuesta.getCarga();
				
				String direccionRecepcionRta = this.atributos.getIpServidor() + ":" + this.atributos.getPuertoServidor().toString();
				Mensaje solicitud = new Mensaje(null,direccionRecepcionRta,21,solicitudDescarga.getSegundo());
				/*Mensaje respuesta = (Mensaje) conexionTmp.enviarConRta(solicitud);
				Imagen descargada = (Imagen) respuesta.getCarga();
				
				atributos.almacenarDescarga(descargada);
				System.out.println("Consumidor "+idConsumidor+" : descargada " + descargada.getNombre());*/
				
				conexionTmp.enviarSinRta(solicitud);
				break;
				
			case "ANUNCIO": //Anuncio a NC de imágenes compartidas
				muchas = (ArrayList<CredImagen>)tarea.getPrimero();
				// Mensaje indicando la cantidad de imágenes a enviar
				conexionConNodoCentral.enviarSinRta(new Mensaje(this.idAsignadoNC,3,muchas.size()));
				// Envío de imágenes
				Mensaje respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(this.idAsignadoNC,3,muchas));
				// Si carga del mensaje = 0 -> recibió todo OK, si = 1 -> algo salió mal.
				if ((Integer) respuesta.getCarga() != 0){
					System.out.println("Consumidor " + idConsumidor + " falló anuncio de imágenes compartidas");
				} else {
					System.out.println("Consumidor "+idConsumidor+" : compartidas " +muchas.size()+ " imágenes");
				}
				break;
				
			case "STOP":
				// Provisorio.
				// Lanzo una excepción para capturarla en el método run()
				throw new InterruptedException("Forzada detención del thread");
		}
		
		System.out.println("\nConsumidor " + this.idConsumidor + " arrancando de nuevo inmediatamente");
	}
	
	
	public void enviarConsulta(CredImagen una, ArrayList<CredImagen> muchas) {
		String direccionServidor; // -> servidor de la H, donde espera la rta

		direccionServidor = String.format("%s:%d", atributos.getIpServidor(), atributos.getPuertoServidor());

		if( muchas == null ){
			conexionConNodoCentral.enviarSinRta(new Mensaje(idAsignadoNC,direccionServidor,4,una));
			System.out.println("Consumidor "+idConsumidor+" : enviada consulta por "+una.getNombre());
		} else {
			Mensaje respuesta;
				
			// Mensaje indicando la cantidad de imágenes a enviar
			conexionConNodoCentral.enviarSinRta(new Mensaje(this.idAsignadoNC,3,muchas.size()));
			
			// Envío de imágenes
			respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(this.idAsignadoNC,3,muchas));
				
			// Si carga del mensaje = 0 -> recibió todo OK, si = 1 -> algo salió mal.
			if ((Integer) respuesta.getCarga() != 0){
				System.out.println("Consumidor " + idConsumidor + " falló anuncio de imágenes compartidas");
			} else {
				System.out.println("Consumidor "+idConsumidor+" : compartidas " +muchas.size()+ " imágenes");
			}
		}
	}
	
	
	private boolean establecerConexionNodoCentral(){
		boolean resultado;
		Mensaje saludo, respuesta;
		String hojaServer, token;
		
		hojaServer = atributos.getIpServidor() + ":" + atributos.getPuertoServidor().toString();

		// Si existe un ID de Hoja definido en los atributos, se envía un mensaje de reconexión.
		// En caso contrario se envía un saludo
		token = this.atributos.getId(this.idConsumidor);
		if (token == null || token.length() == 0) {
			// Saludo, 1ra conexión
			saludo = new Mensaje(null,1, hojaServer);
		} else {
			// Saludo de reconexión
			saludo = new Mensaje(null,1, "##"+token+"##");
		}
		
		respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(saludo);
		if (respuesta.getCodigo().equals(1) && respuesta.getCarga() != null){
			//La respuesta contiene el ID con el que se identificará al Cliente.
			idAsignadoNC = respuesta.getCarga().toString();
			sesionIniciada = true;
			
			// Seteo del ID que recibió la H en los atributos comartidos para ser conocido por todos los
			// "componentes" del nodo Hoja. Si se trataba de una reconexión el ID será igual así que en realidad es
			// redundante ese paso
			atributos.setId(this.idConsumidor, idAsignadoNC);
			
			return true;
		} else {
			return false;
		}
	}
}
