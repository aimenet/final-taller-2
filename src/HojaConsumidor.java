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
	private Integer idAsignadoNC, idConsumidor, puertoNC;
	private String ipNC;



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
		
		if(establecerConexionNodoCentral()){
			System.out.println("Consumidor " + idConsumidor +": iniciada sesión en NC");
			sesionIniciada = true;
		} else {
			System.out.println("Consumidor " + idConsumidor +": imposible iniciar sesión en NC");
			// TODO: ver como capturar el error y parar el consumidor sin detener el Nodo (this.wait() no sirve)
		}
		
		while (true) {
			//try{consumir();}
			try{consumir2();}
			catch (InterruptedException ex){ex.printStackTrace();}
		}
	}

	// Deprecated en cuanto termine consultar2()
	private void consumir() throws InterruptedException{
		ArrayList<CredImagen> anuncio = null;
		ArrayList<Object> colaPropia = atributos.getColasTx()[idConsumidor];
		CredImagen credencial = null;
		Tupla2<CredImagen,String> tarea = null;
		
		synchronized (colaPropia){
			while ( colaPropia.isEmpty() ){
				System.out.println("Consumidor " + idConsumidor + " esperando. Tamaño cola: " + colaPropia.size());
				colaPropia.wait();
			}

			Object carga = colaPropia.remove(0);
			if(carga.getClass() == CredImagen.class){
				credencial = (CredImagen) carga;
				System.out.println("Consumidor " + idConsumidor + " desencoló: " + credencial.getNombre());
			} else if(carga.getClass() == ArrayList.class) {
				anuncio = (ArrayList<CredImagen>) carga;
				System.out.println("Consumidor " + idConsumidor + " desencoló anuncio de "+anuncio.size()+" imágenes");
			} else if(carga.getClass() == Tupla2.class){
				tarea = (Tupla2<CredImagen,String>) carga;
				System.out.println("Consumidor " + idConsumidor + " desencoló solicitud de descarga de " + tarea.getPrimero().getNombre());
			}
			colaPropia.notifyAll();
		}
		
		enviarConsulta(credencial, anuncio);
		
		System.out.println("Consumidor " + this.idConsumidor + " arrancando de nuevo inmediatamente");
	}
	
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
				Mensaje respuesta = (Mensaje) conexionTmp.enviarConRta(solicitud);
				Imagen descargada = (Imagen) respuesta.getCarga();
				
				atributos.almacenarDescarga(descargada);
				System.out.println("Consumidor "+idConsumidor+" : descargada " + descargada.getNombre());
				break;
				
			case "ANUNCIO": //Anuncio a NC de imágenes compartidas
				muchas = (ArrayList<CredImagen>)tarea.getPrimero();
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
				break;
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
		Mensaje respuesta;
		String hojaServer;
		
		hojaServer = atributos.getIpServidor() + ":" + atributos.getPuertoServidor().toString();

		respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(null,1, hojaServer));
		if (respuesta.getCodigo().equals(1) && respuesta.getCarga() != null){
			//La respuesta contiene el ID con el que se identificará al Cliente.
			idAsignadoNC = Integer.valueOf(respuesta.getCarga().toString());
			sesionIniciada = true;
			return true;
		} else {
			return false;
		}
	}
}
