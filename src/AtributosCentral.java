import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Clase que engloba todos los atributos de un Nodo Central e implementa los métodos necesarios para
 * manipular los mismos.
 * 
 * Como puede notarse, los atributos están sicnronizados a nivel de clase de manera que todas las instancias
 * puedan compartirlos. La idea es poder accederlos tanto desde el servidor avocado a la atención de Hojas
 * como desde el encargado de atender otros Nodos Centrales (y futuros componentes como un servidor de monitoreo
 * por ejemplo)
 * 
 * @author rodrigo
 *
 */
public class AtributosCentral {
	// Atributos
	// =========
	private static Integer puertoServidorCentrales;
	private static Integer puertoServidorHojas;
	private static String ip;

	private static Integer idHoja = 1;
	private static final Object lockIDHoja = new Object();
	private static final Object lockIndiceHojas = new Object();
	private static final Object lockIndiceImagenes = new Object();
	private static volatile HashMap<Integer,String> indiceHojas = new HashMap<Integer,String>();
	private static volatile HashMap<Integer,ArrayList<CredImagen>> indiceImagenes = new HashMap<Integer,ArrayList<CredImagen>>();

	private static final Object lockIndiceCentrales = new Object();
	private static volatile ArrayList<String> indiceCentrales = new ArrayList<String>();
	
	// Últimas consultas recibidas para evitar duplicar respuestas. En la primer fila de la matriz se guardan
	// las consultas (Mensaje) y en la segunda la hora en que fue recibida
	private static final Object lockUltimasConsultas = new Object();
	private static volatile Object[][] ultimasConsultas = new Object[2][10];
	private static volatile Integer indiceActualConsultas = 0;


	// Métodos
	// =======
	public String getHoja(Integer idHoja){
		return indiceHojas.get(idHoja);
	}
	
	public String getCentral(Integer index){
		return indiceCentrales.get(index);
	}
	
	public ArrayList<String> getCentrales(){
		return indiceCentrales;
	}
	
	public ArrayList<CredImagen> getImagenes(Integer idHoja){
		return indiceImagenes.get(idHoja);
	}
	
	public void indexarImagenes(Integer idHoja, ArrayList<CredImagen> imagenes){
		synchronized(lockIndiceImagenes){ indiceImagenes.put(idHoja, imagenes); }
	}
	
	public Integer[] getClavesIndiceImagenes(){
		return indiceImagenes.keySet().toArray(new Integer[0]);
	}
	
	public Integer getIncrementarIdHoja(){
		Integer asignado;
		
		synchronized(lockIDHoja){
			asignado = idHoja;
			idHoja = asignado + 1;
		}
		
		return asignado;
	}
	
	public Integer getTamanioIndiceImagenes(){
		return indiceImagenes.size();
	}
	
	public void indexarHoja(Integer idHoja, String direcciones){
		synchronized(lockIndiceHojas){ indiceHojas.put(idHoja, direcciones); }
	}
	
	public Set<Integer> getClavesIndiceHojas(){
		return indiceHojas.keySet();
	}
	
	public void indexarCentral(String direccion){
		indiceCentrales.add(direccion);
	}
	
	public String getDireccion(){
		return String.format("%s:%d", this.ip,this.puertoServidorCentrales);
	}
	
	public void setPuertoServidorCentrales(Integer puerto){
		this.puertoServidorCentrales = puerto; 
	}
	public void setPuertoServidorHojas(Integer puerto){
		this.puertoServidorHojas = puerto;
	}
	public void setIp(String ip){
		this.ip = ip;
	}
	
	/**Evalúa si una consulta ya fue recibida previamente. Si existe y expiró actualiza el horario, en caso contrario la encola.
	 * Código de salida: 0_ La consulta existe y está vigente
	 *                   1_ La consulta existe, no está vigente y fue actualizada
	 *                   2_ La consulta no existe y fue encolada
	 *                   3_ La consulta no existe pero no fue encolada
	 */
	public Integer consultaDuplicada(Mensaje msj){
		Boolean existe = false;
		Integer codigoSalida = 3;
		Integer tiempoVida = 120; // Expresado en segundos
		Timestamp tiempoUmbral;
		
		// TODO: tengo que guardar la consulta y el timestamp si no exste para evitar problemas en <ConsutorNC_NC>: qué pasa?
		// El NC recibe consulta, se fija si existe y si no es así "trabaja". Una vez que termina todo bien, encola la consulta.
		// Pero si en ese interín  otro servidor le mandó la mimsa consulta, la cola no va a tener la consulta y se procesará igual.
		// Debería bloquear de alguna manera esto, pero implicaría que exista algun estado para cada consulta encolada. Lo más fácil creo
		// que es encolar la consulta si no existe y que después el NC tenga opción de desencolarla si el procesamiento falló o lo que fuera.
		// De ahí a agregarle un status a cada una es un paso, pero vamos por parte
		
		
		synchronized (lockUltimasConsultas) {	
			for(int i=0; i<ultimasConsultas.length; i++){
				// Para facilitar la lectura del código lo hago así
				String hojaEmisora = msj.recepcionRta();
				CredImagen credencial = (CredImagen) msj.getCarga();
				if( ultimasConsultas[0][i] != null ) {
					String cmp1 = (String) ((Mensaje) ultimasConsultas[0][i]).recepcionRta();
					CredImagen cmp2 = (CredImagen) ((Mensaje) ultimasConsultas[0][i]).getCarga(); 
					Timestamp horaMsj = (Timestamp)ultimasConsultas[1][i];
					
					if( hojaEmisora.equals( cmp1 ) && credencial.getNombre().equals(cmp2.getNombre()) ){
						// Se evalúa la hora del mensaje registrado: el mismo tiene una vigencia de 2 minutos.
						// Los dos minutos se expresan en milisegundos (por eso el *1000) 
						tiempoUmbral = new Timestamp(System.currentTimeMillis() - tiempoVida * 1000);
						
						if( horaMsj.after(tiempoUmbral) ) {
							// La respuesta encolada sigue vigente pues pasaron menos de 2 minutos desde su recepción
							existe = true;
							codigoSalida = 0;
							
							System.out.println("El mensaje sigue vigente");
							
							break;
						} else {
							// La consulta previa expiró por lo que se actualiza el horario
							ultimasConsultas[1][i] = new Timestamp(System.currentTimeMillis());
							System.out.println("El mensaje no está vigente");
							codigoSalida = 1;
							break;
						}
					}
				}
			}
			
			// Si no existía una consulta igual, almacena la actual
			if(codigoSalida > 1) {
				ultimasConsultas[0][indiceActualConsultas] = msj;
				ultimasConsultas[1][indiceActualConsultas] = new Timestamp(System.currentTimeMillis());
				indiceActualConsultas += 1;
				// Controla que el índice no se vaya de rango
				indiceActualConsultas = indiceActualConsultas % ultimasConsultas[0].length;
				codigoSalida = 2;
			}
		}
		
		return codigoSalida;
	}
	
	/** Técnicamente no es una cola */
	public boolean encolarConsulta(Mensaje msj){
		
		synchronized (lockUltimasConsultas) {
			// Se guarda el mensaje y la hora en que fue encolado
			ultimasConsultas[0][indiceActualConsultas] = msj;
			ultimasConsultas[1][indiceActualConsultas] = new Timestamp(System.currentTimeMillis());
			
			indiceActualConsultas += 1;
			
			// Controla que el índice no se vaya de rango
			indiceActualConsultas = indiceActualConsultas % ultimasConsultas[0].length;
		}
		
		return true;
	}
	
	public Object[][] getUltimasConsultas() {
		return ultimasConsultas;
	}
	
} //Fin clase
