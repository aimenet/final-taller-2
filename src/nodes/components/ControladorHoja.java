package nodes.components;
import java.io.File;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileView;

import commons.CredImagen;
import commons.Imagen;
import commons.Tarea;
import commons.Tupla2;

/**
 * Clase que permite el control de un Nodo Hoja (su faceta Cliente) mediante una interfaz gráfica muy simple
 * por consola.
 * 
 * Corresponde al hilo productor de consultas.
 * 
 * @author rodrigo
 *
 */
public class ControladorHoja implements Runnable {
	// Atributos
	//==========
	private AtributosHoja variables;
	private String[] formatosAceptados;
	
	
	// Métodos
	//========
	
	//Constructor.
	public ControladorHoja(){
		variables = new AtributosHoja();
		formatosAceptados = (new AtributosHoja()).getExtensionesValidas(); 
	}

	
	/**Método que simula el borrado de la pantalla.*/
	private void cls(){
		for(int i=0; i<50; i++){
			System.out.println("");
		}
	}

	
	//Menú que permite al usuario cargar una imagen al nodo.
	private void menuCargar(Scanner teclado) {
		boolean terminar = false;
		
		cls();
		System.out.println("Carga de Imágenes");
		System.out.println("-----------------\n");
		
		// Elección de la imagen
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes", formatosAceptados);
	    fileChooser.setFileFilter(filter);
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.setMultiSelectionEnabled(true);
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    File[] selectedFiles = fileChooser.getSelectedFiles();
		    for(File archivo : selectedFiles){
		    	boolean almacenada = variables.almacenarImagen(new Imagen(archivo));
				if (almacenada) {System.out.println("Imagen cargada: " + archivo.getName());}
				else {System.out.println("Un error impidió cargar la imagen");}
		    }
		} else {
			System.out.println("Ha ocurrido un error que impidió cargar la imagen");
		}
		
		
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}//Fin menuCargar()

	
	//Menú que permite generar una consulta para el/los NCs y encolarla para su envío
	private void menuConsultar(Scanner teclado){
		boolean terminar=false, resultado;	
		File elegida = null;
		Imagen imagen;
		
			
		cls();
		System.out.println("Consulta a Nodo Centrales de imágenes similares");
		System.out.println("-----------------------------------------------\n");
		
		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Imágenes", formatosAceptados);
	    fileChooser.setFileFilter(filter);
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.setMultiSelectionEnabled(false);
		int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    elegida = fileChooser.getSelectedFile();
		} else {
			System.out.println("Ha ocurrido un error que impidió cargar la imagen");
			return;
		}
		
		System.out.println("\nElegida: " + elegida.getName() + "\n");
		System.out.println("\nPresione una tecla para encolar: _");
		teclado.nextLine();
		
		if( elegida != null ){
			imagen = new Imagen(elegida);
			resultado =  this.variables.encolarTx(
							new Tupla2<CredImagen,String>(
									new CredImagen(imagen.getNombre(), imagen.getVecCarComprimido()),
									"QUERY"
							)
						);
			if(resultado){
				System.out.println("Consulta encolada satisfactoriamente");
			} else {
				System.out.println("No pudo encolarse la consulta");
			}
			
			//Moverlo a menú independiente
			/*if( resultado.isEmpty() ){
				 System.out.println("No hay imágenes similares");
			} else {
				
				System.out.println("\nImágenes similares encontradas:");
				for( String clave : resultado.keySet() ){
					String[] nombres = resultado.get(clave);
					System.out.println(clave.toString() + ": " + Arrays.toString(nombres));
				}
				
				System.out.println("\n¿Desea descargar alguna imagen? S/N: _");
				String opcion = teclado.nextLine();
					
				switch (opcion.toUpperCase()) {
					case "S":
						menuDescargarDeHoja(resultado, teclado);
						break;
				}
			}*/
		}
			
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	} // Fin consulta

		
	//TODO: repensar
	//Por ahora no se usa, tengo que ver si la reemplazo o confío en que los consumidores se conectan o si puedo
	//desde ac{a controlar al menos que estén conectados (¿variable compartida?)
	//Menú que permite al usuario establecer conexión con un Nodo Central (por ahora de forma manual).
	/*private void menuConectarse(Scanner teclado) {
		boolean resultado;
		
		// TODO: capturar error para no terminar controlador abruptamente
		
		String ipServidor = "127.0.0.1";
		Integer puertoServidor = 5555;
		resultado = this.nodoHoja.establecerConexionNodoCentral(ipServidor, puertoServidor);
		
		cls();
		System.out.println("Conexión con Nodo Central");
		System.out.println("-------------------------\n");
		System.out.println(resultado ? "Conexión: establecida" : "Conexión: imposible de establecer");
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}*/

	
	// TODO: adaptar a cola de rtas
	/*private void menuDescargarDeHoja(HashMap<String,String[]> disponibles, Scanner teclado){
		boolean resultado, terminar = false;
		String descargada;
		
		while(!terminar){
			cls();
			System.out.println("Descarga de imagen");
			System.out.println("------------------\n");
			int i = 0;
			for( String clave : disponibles.keySet() ){
				System.out.println(i+") "+clave+" "+ Arrays.toString(disponibles.get(clave)));
				i++;
			}
			System.out.println("\nEscoja un nodo de descarga: _");
			Integer opcion = new Integer(teclado.nextLine());
			String clave = (String) disponibles.keySet().toArray()[opcion];
			
			System.out.println("\n");
			for(i=0; i<disponibles.get(clave).length; i++ ){
				System.out.println(i+") " + disponibles.get(clave)[i]);
			}
			System.out.println("\nEscoja una imagen: _");
			opcion = new Integer(teclado.nextLine());
			
			String objetivo = disponibles.get(clave)[opcion];
			String direccion = clave.split(":")[0];
			Integer puerto = Integer.parseInt(clave.split(":")[1]);
			
			// Bloqueante: no se puede "descargar en segundo plano"
			descargada = nodoHoja.descargarImagen(direccion, puerto, objetivo);
			
			if(descargada == null){
				System.out.println("\nNo pudo descargarse la imagen");
			} else {
				System.out.println(String.format("Descargada '%s' de <%s:%d>", descargada, direccion, 9898));
			}
			
			terminar = true;
		}
	}*/

		
	// TODO: ver que hago ahora que hay consumidores
	/*private void menuDesconectarse(Scanner teclado){
		boolean resultado;
		
		resultado = this.nodoHoja.terminarConexionNodoCentral();
		cls();
		System.out.println("Finalización de conexión con Nodo Central");
		System.out.println("-----------------------------------------\n");
		System.out.println(resultado ? "Conexión: finalizada" : "Conexión: imposible de finalizar");
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}*/

	
	// TODO: adaptarlo a <HojaConsumidor> consumir2() 
	//Menú que permite enviar al Nodo Central el listado de imágenes a compartir.
	private void menuAnunciarImagenes(Scanner teclado){
		ArrayList<CredImagen> pendientes;
		String resultado;
		Tupla2<ArrayList<CredImagen>,String> anuncio;
		
		cls();
		System.out.println("Envío de imágenes a compartir");
		System.out.println("-----------------------------\n");
		
		pendientes = new ArrayList<CredImagen>();
		
		//Selección de las imágenes a enviar. Se carga la "credencial" de cada una en un arreglo.
		for(String clave : variables.clavesIndice()) {
			pendientes.add(new CredImagen(variables.getImagen(clave).getNombre(),
										  variables.getImagen(clave).getVecCarComprimido()));
		}
		
		if(variables.hayImagenes()){
			anuncio = new Tupla2<ArrayList<CredImagen>,String>(pendientes,"ANUNCIO");
			resultado =  variables.encolarTx(anuncio) ? "Envío realizado con éxito" : "No pudo realiarse el envío"; 
			System.out.println(resultado);
			System.out.println("\nPresione una tecla para continuar: _");
		} else {
			System.out.println("Debe cargar imágenes antes de su envío");
		}
		
		teclado.nextLine();
	}

	
	// TODO: rehacer tomando como referencia menuRespuestas
	/*private void menuDescargar(Scanner teclado){
		boolean resultado, terminar = false;
		ArrayList<HashMap<String,CredImagen[]>> disponibles;
		CredImagen elegida;
		Integer opcion;
		String descargada;
		
		disponibles = variables.getColaRtas();
		
		while(!terminar){
			cls();
			System.out.println("Descarga de imagen");
			System.out.println("------------------\n");
			
			// TODO acá me quedé
			
			// Acá debería iterar por cada una de las respuestas que hay en la cola pero como no registro a
			// qué consulta corresponde cada una no tiene sentido. Hago de cuenta que hay una sola por ahora
			// (muestro siempre la última)
			if(disponibles.size() > 0){
				HashMap<String,CredImagen[]> una = disponibles.get( disponibles.size() - 1 );
				Integer indice = 0;
				for(String clave : una.keySet()){
					CredImagen[] carga = una.get(clave);
					System.out.print(indice + ") " + clave);
					for(int j=0; j<carga.length; j++){
						System.out.print(carga[j].getNombre() + ",");
					}
					System.out.println("\n");
					indice += 1;
				}
			}
			
			System.out.println("\nEscoja un nodo de descarga: _");
			opcion = new Integer(teclado.nextLine());
			
			HashMap<String,CredImagen[]> una = disponibles.get(opcion);
			String nodo = una.keySet().toArray(new String[0])[opcion];
			CredImagen[] similares = una.get(nodo);
			System.out.print("\n\nImágenes similares en " + nodo);
			
			for(int i=0; i<similares.length; i++){
				System.out.println("\t" + i + ") " + similares[i]);
			}
			System.out.println("\nEscoja una imagen a descargar: _");
			opcion = new Integer(teclado.nextLine());
			
			elegida = similares[opcion];
			
			System.out.println("Encolando solicitud de descarga de " + elegida.getNombre());
			
			Tupla2<String,CredImagen> solicitudDescarga = new Tupla2<String,CredImagen>(nodo,elegida); 
			resultado =  this.variables.encolarTx(new Tupla2<Tupla2<String,CredImagen>,String>(solicitudDescarga,"DESCARGAR"));
			if(resultado){
				System.out.println("Solicitud de descarga encolada satisfactoriamente");
			} else {
				System.out.println("No pudo encolarse la solicitud de descarga");
			}
			
			terminar = true;
		}
	}*/
	
	
	//Lista en pantalla todas las imágenes descargadas en el nodo. 
	private void menuListarDescargas(Scanner teclado){
		String[] imagenesDescargadas;
		
		imagenesDescargadas = variables.listadoDescargas(); 
		cls();
		System.out.println("Imágenes Descargadas");
		System.out.println("--------------------\n");
		for(int i=0; i<imagenesDescargadas.length; i++){
			System.out.println(i + ") " + imagenesDescargadas[i]);
		}
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}
	

	//Lista en pantalla todas las respuestas recibidas hasta el momento 
	private void menuDescargar(Scanner teclado){
		Boolean encolado = false;
		CredImagen laImg;
		CredImagen[] candidatas;
		HashMap<String, HashMap<String,CredImagen[]>> querysRtas;
		HashMap<String,CredImagen[]> unaRta;
		Integer indice=0, opcion, contador=0;
		String laQuery, laHoja;
		Tupla2<String,CredImagen> solicitud;
		
		querysRtas = variables.getColaRtas();
		cls();
		System.out.println("Respuestas Encoladas");
		System.out.println("--------------------\n");
		
		if(querysRtas.size() == 0){
			System.out.println("\nNo hay respuestas. Presione una tecla para continuar_");
			teclado.nextLine();
			return;
		}
		
		// La primer elección corresponde a la imagen (para la que existen rtas) 
		indice = 0;
		for(String clave : querysRtas.keySet()){
			System.out.print(indice + ") " + clave + ": ");
			contador = 0;
			for(String clave2 : querysRtas.get(clave).keySet()){
				CredImagen[] credenciales = querysRtas.get(clave).get(clave2);
				contador += querysRtas.get(clave).get(clave2).length;
			}
			System.out.println(contador + " imágenes similares");
			indice += 1;
		}
		System.out.println("\nEscoja una imagen: _");
		opcion = Integer.parseInt(teclado.nextLine());
		
		// La segunda elección consiste en el Nodo Hoja del que se desea descargar
		laQuery = (String) querysRtas.keySet().toArray()[opcion];
		unaRta = querysRtas.get(laQuery);
		cls();
		System.out.println("Hojas con imágenes similares a " + laQuery);
		System.out.println("------------------------------------------\n");
		indice = 0;
		for(String hoja : unaRta.keySet()){
			System.out.print(indice + ") " + hoja + ": ");
			System.out.println( unaRta.get(hoja).length + " imágenes" );
			indice += 1;
		}
		System.out.println("\nEscoja un nodo de descarga: _");
		opcion = Integer.parseInt(teclado.nextLine());
		
		// La tercer elección es la de la imagen que se quiere descargar
		laHoja = (String) unaRta.keySet().toArray()[opcion];
		candidatas = unaRta.get(laHoja);
		cls();
		System.out.println("Imágenes similares en Hoja " + laHoja);
		System.out.println("-------------------------------------\n");
		for(int i=0; i<candidatas.length; i++){
			System.out.println("\t" + i + ") " + candidatas[i].getNombre());
		}
		System.out.println("\nEscoja la imagen a descargar: _");
		opcion = Integer.parseInt(teclado.nextLine());
		
		laImg = candidatas[opcion];
		System.out.println("\n\n Bien pibe de los astilleros, vas a descargar " + laImg.getNombre() + " de " + laHoja);
		
		solicitud = new Tupla2<String,CredImagen>(laHoja,laImg);
		encolado =  this.variables.encolarTx( new Tupla2<Tupla2<String,CredImagen>,String>(solicitud, "DESCARGA") );
		
		if(encolado){
			System.out.println("Solicitud de descarga encolada con éxito");
		} else {
			System.out.println("No se pudo encolar la solicitud de descarga");
		}
	}
	
	
	//Lista en pantalla todas las imágenes cargadas en el nodo. 
	private void menuListarImagenes(Scanner teclado){
		String[] imagenesIndexadas;
		
		imagenesIndexadas = variables.listadoImagenes(); 
		cls();
		System.out.println("Imágenes Indexadas");
		System.out.println("------------------\n");
		for(int i=0; i<imagenesIndexadas.length; i++){
			System.out.println(i + ") " + imagenesIndexadas[i]);
		}
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}


	//Lista en pantalla las respuestas recibias hasta el momento 
	private void menuListarRespuestas(Scanner teclado){
		CredImagen laImg;
		CredImagen[] candidatas;
		HashMap<String, HashMap<String,CredImagen[]>> querysRtas;
		HashMap<String,CredImagen[]> unaRta;
		Integer indice=0, opcion, contador=0;
		String laQuery, laHoja;
		
		querysRtas = variables.getColaRtas();
		cls();
		System.out.println("Respuestas Encoladas");
		System.out.println("--------------------\n");
		
		if(querysRtas.size() == 0){
			System.out.println("\nNo hay respuestas. Presione una tecla para continuar_");
			teclado.nextLine();
			return;
		}
		
		// La primer elección corresponde a la imagen (para la que existen rtas) 
		indice = 0;
		for(String clave : querysRtas.keySet()){
			System.out.print(indice + ") " + clave + ": ");
			contador = 0;
			for(String clave2 : querysRtas.get(clave).keySet()){
				CredImagen[] credenciales = querysRtas.get(clave).get(clave2);
				contador += querysRtas.get(clave).get(clave2).length;
			}
			System.out.println(contador + " imágenes similares");
			indice += 1;
		}
		System.out.println("\nEscoja una imagen: _");
		opcion = Integer.parseInt(teclado.nextLine());
		
		// La segunda elección consiste en el Nodo Hoja del que se desea descargar
		laQuery = (String) querysRtas.keySet().toArray()[opcion];
		unaRta = querysRtas.get(laQuery);
		cls();
		System.out.println("Hojas con imágenes similares a " + laQuery);
		System.out.println("------------------------------------------\n");
		indice = 0;
		for(String hoja : unaRta.keySet()){
			System.out.print(indice + ") " + hoja + ": ");
			System.out.println( unaRta.get(hoja).length + " imágenes" );
			indice += 1;
		}
		System.out.println("\nEscoja un nodo de descarga: _");
		opcion = Integer.parseInt(teclado.nextLine());
		
		// La tercer elección es la de la imagen que se quiere descargar
		laHoja = (String) unaRta.keySet().toArray()[opcion];
		candidatas = unaRta.get(laHoja);
		cls();
		System.out.println("Imágenes similares en Hoja " + laHoja);
		System.out.println("-------------------------------------\n");
		for(int i=0; i<candidatas.length; i++){
			System.out.println("\t" + i + ") " + candidatas[i].getNombre());
		}
		System.out.println("\nPresione una tecla para continuar: _");
		teclado.nextLine();
	}


	public void menuPrincipal(Scanner teclado){
		boolean terminar = false;
		
		while(!terminar){
			cls();
			System.out.println("Nodo Hoja");
			System.out.println("---------\n");
			
			System.out.println("1) Menú de carga de imágenes");
			System.out.println("2) Listar imágenes existentes");
			System.out.println("3) X Conectarse por primera vez con Nodo Central");
			System.out.println("4) X Desconectarse de Nodo Central");
			System.out.println("5) X Enviar mensaje de prueba a Nodo Central");
			System.out.println("6) Enviar imágenes compartidas a Nodo Central");
			System.out.println("7) Enviar consulta a Nodo Central");
			System.out.println("8) Ver cola de respuestas");
			System.out.println("9) Ver cola de descargas");
			System.out.println("10) Descargar imagen");
			System.out.println("11) Simular caída HILO CONSUMIDOR (aleatorio)");
			System.out.println("0) Salir");
			System.out.println("\nEscoja una opción: _");
			String opcion = teclado.nextLine();
			
			switch (opcion) {
			case "1":
				this.menuCargar(teclado);
				break;
			case "2":
				this.menuListarImagenes(teclado);
				break;
			case "3":
				//this.menuConectarse(teclado);
				break;
			case "4":
				//this.menuDesconectarse(teclado);
				break;
			case "5":
				//this.menuMensajePrueba(teclado);
				break;
			case "6":
				this.menuAnunciarImagenes(teclado);
				break;
			case "7":
				this.menuConsultar(teclado);
				break;
			case "8":
				this.menuListarRespuestas(teclado);
				break;
			case "9":
				this.menuListarDescargas(teclado);
				break;
			case "10":
				this.menuDescargar(teclado);
				break;
			case "11":
				// TODO: <2019-03-02> Provisorio, si queda emprolijarlo
				Random rand = new Random(); 
				int consumerToInterrupt = rand.nextInt( this.variables.getColasTx().length ); 
				
				System.out.println("Interrumpiendo consumidor #" + consumerToInterrupt);
				//this.variables.encolarTx(new Tupla2<Object, String>(null, "STOP"));
				this.variables.encolarTxEspecifica(new Tarea("STOP"), consumerToInterrupt);
				break;
			case "0":
				terminar = true;
				//nodoHoja.terminarConexionNodoCentral();
				break;
			default:
				break;
			} //Fin switch-case
		}
		
	}
	
		
	@Override
	public void run() {
		Scanner teclado;
		teclado = new Scanner(System.in);
		
		menuPrincipal(teclado);
	}
}
