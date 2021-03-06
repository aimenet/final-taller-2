import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Clase que engloba todos los atributos de un Nodo Hoja e implementa los métodos necesarios para
 * manipular los mismos.
 * 
 * Como puede notarse, los atributos están sicnronizados a nivel de clase de manera que todas las instancias
 * puedan compartirlos. La idea es poder accederlos tanto desde el cliente de la Hoja, los hilos del
 * servidor y eventuales componentes que surjan en el futuro (¿un controlador de los datos, para un módulo
 * gui, backup, etc?)
 * 
 * @author rodrigo
 *
 */
public class AtributosHoja {
	// Atributos
	// =========
	// Constantes
	private static final Integer MAX_COLA_TX = 100;
	private static final Object lockDescargas = new Object();
	private static final Object lockIndice = new Object();
	private static final Object lockRtas = new Object();
	private static final String[] extensionesValidas = new String[]{"bmp","jpg","jpeg","png"};
	
	// que deben definirse antes de generar los hilos de la Hoja
	private static ArrayList<Object>[] colasTx;
	private static Integer puertoServidor;
	private static String ipServidor;
	private static String[] direccionesNCs;
	
	// que no deben inicializarse
	private static volatile HashMap<String,Imagen> indice = new HashMap<String,Imagen>();
	private static volatile ArrayList<Imagen> colaDescargas = new ArrayList<Imagen>();
	private static volatile ArrayList< HashMap<String,CredImagen[]> > colaRespuestas = new ArrayList<HashMap<String,CredImagen[]>>(); // Deprecated
	private static volatile HashMap<String,HashMap<String,CredImagen[]>> nuevaColaRespuestas = new HashMap<String,HashMap<String,CredImagen[]>>();
	private static volatile ArrayList<Object> colaTxHistorica = new ArrayList<Object>();
	
	
	
	// Métodos
	// =======
	public boolean almacenarImagen(Imagen img){
		//Al utilizar HashMap para el índice, si se carga una imagen con nombre ya existente, sobreescribirá
		//a la anterior.
		synchronized (lockIndice) {
			indice.put(img.getNombre(), img);
			return true;
		}
	}
	
	public Set<String> clavesIndice(){
		synchronized (lockIndice) {
			return indice.keySet();
		}
	}
	
	public String[] getExtensionesValidas(){
		return extensionesValidas;
	}
	
	public boolean hayImagenes(){
		synchronized (lockIndice) {
			return !indice.isEmpty();
		}
	}

	/*Por ahora devuelve el nombre de las imágenes descargadas hasta el momento*/
	public String[] listadoDescargas(){
		String[] nombres = new String[colaDescargas.size()];
		
		// No sincronizo porque es sólo lectura
		for(int i=0; i<colaDescargas.size(); i++){
			nombres[i] = colaDescargas.get(i).getNombre();
		}

		return nombres;
	}
	
	/** Método que devuelve un arreglo con los nombres de las imágenes actualmente indexadas.*/
	public String[] listadoImagenes(){
		synchronized (lockIndice) {
			String[] imagenes = indice.keySet().toArray(new String[0]);
			return imagenes;
		}
	}
	
	public boolean hayRespuestas(){
		synchronized (lockRtas) {
			return !colaRespuestas.isEmpty();
		}
	}
	
	public boolean almacenarDescarga(Imagen una){
		synchronized (lockDescargas) {
			colaDescargas.add(una);
			return true;
		}
	}
	
	// Deprecated
	/*public boolean almacenarRta(HashMap<String, CredImagen[]>  respuesta){
		synchronized (lockRtas) {
			colaRespuestas.add(respuesta);
			return true;
		}
	}*/
	public boolean almacenarRta(CredImagen query, HashMap<String,CredImagen[]>  respuesta){
		
		synchronized (lockRtas) {
			if(!nuevaColaRespuestas.containsKey(query.getNombre())){
				nuevaColaRespuestas.put(query.getNombre(), new HashMap<String,CredImagen[]>());
			}
			
			// Agrego listado de H que no tenía en el diccionario y sobreescribo las que sí tenía
			for(String clave : respuesta.keySet()){
				nuevaColaRespuestas.get(query.getNombre()).put(clave, respuesta.get(clave));
			}  
			return true;
		}
	}
	
	public boolean encolarTx(Object carga){
		/*Carga puede ser: CredImagen            -> consulta al/los NC
		                   ArrayList<CredImagen> -> anuncio de las imágenes a compartir*/
		// Cola histórica, para referencia. No hay problemas de concurrencia pues (por ahora) nadie consume de ella
		this.colaTxHistorica.add(carga);
		
		/* Encolado en colas "de trabajo" (una por consumidor): para realizar cada encolado se bloque la cola
		 * a fin de evitar problemas de concurrencia. Una vez completado se notifica al Consumidor para que se
		 * ponga en funcionamiento, conusma de la cola y realize la consulta.
		 */
		for(int i=0; i<colasTx.length; i++){
			synchronized (colasTx[i]) {
				//TODO: esto hoy es anecdótico porque no puedo bloquear el hilo porque se llenó una cola. Ver qué hacer (hoy descarto la consulta)
				//TODO: ver "Ejemplo productor - consumidor 3" (no modo manual) para saber como era originalmente
				if (colasTx[i].size() == MAX_COLA_TX) {
					System.out.println("Cola " + i + " llena: " + Thread.currentThread().getName() + " Consulta descartada. Tamaño: " + colasTx[i].size());
					colasTx[i].notifyAll(); // Se notifica al consumidor de la cola para que cominece a liberar espacio.
				} else {
					colasTx[i].add(carga);
					colasTx[i].notifyAll(); //Sólo hay un consumidor así que con notify() alcanza
				}
			}
		}
		
		return true;
	} 
	
	
	// Getters
	// -------
	public Imagen getDescarga(Integer indice){
		synchronized (lockDescargas) {
			return colaDescargas.get(indice);
		}
	}
	
	public Imagen getImagen(String nombre){
		synchronized (lockIndice) {
			return indice.get(nombre);
		}
	}
		
	public HashMap<String,Imagen> getImagenes(){
		synchronized (lockIndice) {
			return indice;
		}
	}

	public Integer getPuertoServidor(){ return puertoServidor; }

	public String getIpServidor(){ return ipServidor; }
	
	// Deprecated
	/*public ArrayList<HashMap<String,CredImagen[]>> getColaRtas(){
		return this.colaRespuestas;
	}*/
	
	public HashMap<String, HashMap<String, CredImagen[]>> getColaRtas(){
		return this.nuevaColaRespuestas;
	}
	
	public HashMap<String, CredImagen[]> getUnaRta(CredImagen query){
		return nuevaColaRespuestas.get(query.getNombre());
	}
	
	public ArrayList<Object>[] getColasTx(){
		return this.colasTx;
	}
	
	
	// Setters
	// -------
	public void setPuertoServidor(Integer puerto){ this.puertoServidor = puerto; }
	
	public void setIpServidor(String ip){ this.ipServidor = ip; }
	
	public void setDireccionesNCs(String[] direcciones){ 
		this.direccionesNCs = direcciones;
		this.setColasTx();
	}
	
	private void setColasTx(){
		colasTx = (ArrayList<Object>[]) new ArrayList[direccionesNCs.length];
		
		for(int i=0; i<direccionesNCs.length; i++)
			colasTx[i] = new ArrayList<Object>();	
	}
	

	
	
} //Fin clase
