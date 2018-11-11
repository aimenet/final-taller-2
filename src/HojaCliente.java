import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * "Parte" Cliente de un Nodo Hoja. Es la encargada de conectarse al Nodo Central e interactuar con él.
 * @author rodrigo
 *
 */
public class HojaCliente {
	// Atributos
	// =========
	private AtributosHoja atributos;
	private ConexionTcp conexionConNodoCentral;
	private boolean conexionEstablecida, sesionIniciada;
	private int id;
	
	
	
	// Métodos
	// =======
	public HojaCliente(String ipNodoCentral, Integer puertoNodoCentral){
		atributos = new AtributosHoja();
		conexionEstablecida = false;
		sesionIniciada = false;
		try {
			this.conexionConNodoCentral = new ConexionTcp(ipNodoCentral, puertoNodoCentral);
			conexionEstablecida = true;
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el servidor");
		}
		// TODO: sacar dirección del servidor hardcodeada.
	}
	
	
	//Agrega una imagen al indice.
	public String agregarImagen(File archivoFile){
		//this.indice.put(archivoFile.getName(), new Imagen(archivoFile.getAbsolutePath()));
		boolean almacenada = atributos.almacenarImagen(new Imagen(archivoFile));
		if (almacenada) {return archivoFile.getName();}
		else {return null;}
	}

		
	//Permite la carga manual de imágenes en el Nodo.
	public String cargaManual(){
		String[] extensionesValidas = atributos.getExtensionesValidas();
			
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF Images", extensionesValidas);
		fileChooser.setFileFilter(filter);
		fileChooser.setAcceptAllFileFilterUsed(false);
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			System.out.println("Selected file: " + selectedFile.getAbsolutePath());
			System.out.println("Name: " + selectedFile.getName());
			atributos.almacenarImagen(new Imagen(selectedFile));
			return selectedFile.getName();
		} else {
			return null;
		}
	}
		
	
	// TODO: des-hardcodear el puerto y sacarlo de donde corresponda (config)
	public String descargarImagen(String ip, Integer puerto, String nombre){
		ConexionTcp conexionTmp;
		Mensaje respuesta;
		String descargada = null;
		
		try {
			conexionTmp = new ConexionTcp(ip, puerto);
		} catch (IOException e) {
			System.out.println("No se pudo establecer conexión con el Nodo Hoja");
			return descargada;
		}
		
		respuesta = (Mensaje) conexionTmp.enviarConRta(new Mensaje(0,21,nombre));
		descargada = (String) respuesta.getCarga();
		
		return descargada;
		
	}
	
	
	public HashMap<String,String[]> enviarConsulta(File imagenObjetivo) {
		CredImagen referencia;
		HashMap<String,String[]> similares;
		Imagen imagen;
		Mensaje respuesta;
		String direccionServidor;

		imagen = new Imagen(imagenObjetivo);
		referencia = new CredImagen(imagen.getNombre(), imagen.getVecCarComprimido());
		direccionServidor = String.format("%s:%d", atributos.getIpServidor(), atributos.getPuertoServidor());
		
		//respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(id,direccionServidor,4,referencia));
		conexionConNodoCentral.enviarSinRta(new Mensaje(id,direccionServidor,4,referencia));
		
		/*if(respuesta != null){
			similares = (HashMap<String,String[]>) respuesta.getCarga();
		} else{
			similares = new HashMap<String,String[]>();
		}
		return similares;*/
		System.out.println("<HojaCliente.java> Ya terminé. Devuelvo vacío temporalmente, fijate el server");
		return new HashMap<String,String[]>();
	}


	// TODO: probar
	/**
	 * Informa al Nodo Central las imágenes que dispone para compartir.
	 * Se utiliza para tal fin la "Credencial XXX" que contiene el Vector Característico Comprimido
	 * y el nombre de la imagen (en el Nodo Hoja). 
	 * @return boolean estado de la transacción
	 */
	public boolean enviarImagenes() {
		ArrayList<CredImagen> pendientes;
		boolean enviado;
		Mensaje respuesta;
			
		if(!atributos.hayImagenes()){
			return false;
		}
			
		pendientes = new ArrayList<CredImagen>();
			
		//Selección de las imágenes a enviar. Se carga la "credencial" de cada una en un arreglo.
		for(String clave : atributos.clavesIndice()) {
			pendientes.add(new CredImagen(atributos.getImagen(clave).getNombre(),
										  atributos.getImagen(clave).getVecCarComprimido()));
		}
			
		// Mensaje indicando la cantidad de imágenes a enviar
		enviado = conexionConNodoCentral.enviarSinRta(new Mensaje(this.id,3,pendientes.size()));
		
		if(!enviado){
			return false;
		}
			
		// Envío de imágenes
		respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(this.id,3,pendientes));
			
		// Si carga del mensaje = 0 -> recibió todo OK, si = 1 -> algo salió mal.
		if ((Integer) respuesta.getCarga() == 0){ return true;}
		else {return false;}
	}


	// TODO: probar
	public int enviarMensajePrueba(){
		boolean resultado;
		int numero;
		Mensaje respuesta;
		String texto;
		Random aleatorio;

		aleatorio = new Random();
		numero = aleatorio.nextInt(100);
		texto = "Texto de prueba + " + numero;
		
		resultado = conexionConNodoCentral.enviarSinRta(new Mensaje(this.id,2,texto));
		if(!resultado){
			numero = 666;
		}
		
		return numero;
	}


	public boolean envioPermitido(){
		return atributos.hayImagenes();
	}
	

	// TODO: probar
	//Establece conexión con el Nodo Central, instanciando a ConexionTcp
	public boolean establecerConexionNodoCentral(String ipServidor, Integer puertoServidor){
		boolean resultado;
		Mensaje respuesta;
		String hojaServer;
		
		hojaServer = atributos.getIpServidor() + ":" + atributos.getPuertoServidor().toString();

		respuesta = (Mensaje) conexionConNodoCentral.enviarConRta(new Mensaje(null,1, hojaServer));
		if (respuesta.getCodigo().equals(1) && respuesta.getCarga() != null){
			//La respuesta contiene el ID con el que se identificará al Cliente.
			id = Integer.valueOf(respuesta.getCarga().toString());
			System.out.println("ID asignado: " + id);
			sesionIniciada = true;
			return true;
		} else {
			return false;
		}
	}

	
	public String[] listadoImagenes(){
		return atributos.listadoImagenes();
	} 
	

	public HashMap<String, HashMap<String, CredImagen[]>> getColaRespuestas(){
		return atributos.getColaRtas();
	}
	
	
	// TODO: probar
	public boolean terminarConexionNodoCentral(){
		boolean r1, r2;
		
		r1 = true;
		r2 = true;
		
		// TODO: ¿realmente debo capturar los errores?
		if(sesionIniciada){
			r1 = conexionConNodoCentral.enviarSinRta(new Mensaje(this.id,0, null));
			sesionIniciada = false;
			System.out.println("Cerrada sesión");
		}
		if(conexionEstablecida){
			r2 = conexionConNodoCentral.cerrar();
			conexionEstablecida = false;
			System.out.println("Cerrada conexión");
		}
		
		return r1 & r2;
	}

}// Fin clase
