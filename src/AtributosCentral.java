import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	
	private static final Object lockArchivoHojas = new Object();
	private static final String pathArchivoHojas = Paths.get(System.getProperty("user.dir"),"config", "hojas_conectadas.json").toString();
	
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
	
	/** Verifica si alguna vez la H consultada estuvo conectada al NC, con el id indicado 
	 * @throws ParseException 
	 * @throws IOException */
	public boolean verificarConexionHistoricaHoja(Integer idHoja, String token, String direccionesHoja) throws IOException, ParseException {
		boolean existio = false;
		
		synchronized (lockArchivoHojas) {
			JSONParser parser = new JSONParser();
			Reader reader = new FileReader(this.pathArchivoHojas);

			JSONObject jsonObject = (JSONObject) parser.parse(reader);

			if ( jsonObject.keySet().contains(idHoja.toString()) ) {
				JSONObject hoja = (JSONObject) jsonObject.get(idHoja.toString());
				existio = ((String) hoja.get("direcciones")).equals(direccionesHoja) && ((String) hoja.get("token")).equals(token);
			}
			
			reader.close();
		}
		
		return existio;
	}
	
	/** Registra una HOJA en el archivo correspondiente (para llevar un registro "histórico" en caso de reconexión 
	 * @throws ParseException 
	 * @throws IOException */
	private boolean guardarConexionHistoricaHoja(Integer idHoja, String token, String direccionesHoja) {
		boolean guardado = false;
		JSONObject hoja = new JSONObject();
		
		hoja.put("token", token);
		hoja.put("direcciones", direccionesHoja);
		
		synchronized (lockArchivoHojas) {
			try {
				// Si no existe el archivo donde se registran las H lo abre sino lo crea
				File file = new File(this.pathArchivoHojas);
				if (!file.exists()) {
					file.getParentFile().mkdirs();
					file.createNewFile();
					FileWriter writer = new FileWriter(file);
					writer.write("{}");
					writer.flush();
					writer.close();
				}
				
				// Lee el contenido del archivo -> no es lo más eficiente en cuanto a uso de memoria pero es
				// el problema de usar JSON
				JSONParser parser = new JSONParser();
				Reader reader = new FileReader(file);
				JSONObject hojas;
				hojas = (JSONObject) parser.parse(reader);
				reader.close();
				
				hojas.put(idHoja.toString(), hoja);
				
				// Sobreescribe el archivo con el actualizado -> de nuevo, no es lo más eficiente ni seguro
				FileWriter writer = new FileWriter(file);
				writer.write(hojas.toJSONString());
				writer.flush();
				writer.close();
				
				guardado = true;
			} catch (IOException e) {
				e.printStackTrace();
				guardado = false;
			} catch (ParseException e) {
				e.printStackTrace();
				guardado = false;
			}
		}
		
		return guardado;
	}
	
	
	
} //Fin clase


/**
 * Notas
 * -----
 * 	"archivoHojas" es el archivo donde registro las H que estuvieron conectadas al nodo
 * para recuperar una conexión en caso de caída
 *  
 *  */
