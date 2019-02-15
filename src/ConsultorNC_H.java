import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Consultor que corre en cada uno de los hilos generado por el Servidor de los Nodos Centrales. Es el
 * encargado de la atención propiamente dicha de los clientes (ya sean H o NC).
 * Puede considerarse como el verdadero Nodo Central ya que tiene toda su funcionalidad. 
 * 
 * El <índice de Hojas> guarda por cada ID un string del tipo:
 * "ip_cliente:puerto_cliente;ip_servidor:puerto_servidor" siendo la primer parte (separada por ";"
 * la correspondiente al socket conectado actualmente al ConsultorNC. La segunda corresponde al server socket
 * donde la Hoja atiende consultas.
 * 
 * @author rodrigo
 *
 */
public class ConsultorNC_H implements Consultor {
	/* --------- */
	/* Atributos */
	/* --------- */
	// De cada instancia
	private Socket sock;
	
	// Compartidos
	AtributosCentral atributos = new AtributosCentral();
	
	
	/* --------------- */
	/* Métodos Propios */
	/* --------------- */
	//public ConsultorNC_H(){}
	
	
	private boolean recibirConsulta(ObjectInputStream entrada, ObjectOutputStream salida,
			Mensaje msj){
		CredImagen modelo;
		HashMap<String, CredImagen[]> resultado;
		String direccionNodoActual = null, direccionRta=null;
		Tupla2<CredImagen, HashMap<String, CredImagen[]>> respuesta = null;

		/*System.out.println("¿Qué pasha clarin, estás nerviosho?");
		System.out.println("NC 1: " + atributos.getCentral(0));
		System.out.println("Dir cen: " +  atributos.getDireccion());*/
		
		// Imagen modelo (query)
		modelo = (CredImagen) msj.getCarga();

		System.out.println("\tRecibida consulta de: " + sock.getInetAddress().toString() + 
				":" + sock.getPort());
		System.out.println("\t-> " + modelo.getNombre());

		// Pueden usarse varios criterios para paralelizar: un hilo por núcleo del CPU, uno por hoja conectada
		//(el elegido), un número empírico fijo, etc
		int cantHilos =  atributos.getTamanioIndiceImagenes();
		CountDownLatch latchHojas = new CountDownLatch(cantHilos);
		HashConcurrente similares = new HashConcurrente();

		/*Primera versión
		Integer[] hojasConImagenes =  atributos.getClavesIndiceImagenes();
		System.out.println("Checkpoint 0");
		for(int i=0; i<cantHilos; i++){
			String destino = atributos.getHoja(hojasConImagenes[i]).split(";")[1];
			Worker trabajador = new Worker(i, similares, modelo, atributos.getImagenes(hojasConImagenes[i]),
					latchHojas, destino.split(":")[0], destino.split(":")[1], 1, "Hoja");
			
			new Thread( trabajador ).start();
			
			System.out.println("<ConcultorNC_H.java> Worker consultando a Hoja " + destino);
		}*/
		
		// Segunda versión (se puede hacer más eficiente o al menos más prolijo buscando las hojas finales
		// para no estar decrementando el latch y demás)
		String[] hojasConImagenes =  atributos.getClavesIndiceImagenes();
		for(String clave : hojasConImagenes) {
			if(clave != msj.getEmisor()) {
				String destino = atributos.getHoja(clave).split(";")[1];
				Worker trabajador = new Worker(clave, similares, modelo, atributos.getImagenes(clave),
						latchHojas, destino.split(":")[0], destino.split(":")[1], 1, "Hoja");
				new Thread( trabajador ).start();
				System.out.println("<ConcultorNC_H.java> Worker consultando a Hoja " + destino);
			} else {
				System.out.println("A la Hoja " + clave + " no le envío");
				latchHojas.countDown();
			}
		}
		
		
		System.out.println("Listas las Hojas, vamos por los Centrales");
		
		// Acá tengo que hacer la consulta a los NC -> sólo si el TTL del mensaje lo permite
		// La respuesta del servior debe ser algo así: HashMap<String, String[]>
		cantHilos =  atributos.getCentrales().size();
		direccionNodoActual = atributos.getDireccion();
		direccionRta = msj.recepcionRta();
		CountDownLatch latchCentrales = new CountDownLatch(cantHilos);
		
		for(int i=0; i<cantHilos; i++){
			String destino = atributos.getCentral(i);
			
			if (destino != null){
				Worker trabajador = new Worker(Integer.toString(i), similares, modelo, new ArrayList<CredImagen>(),
						latchCentrales, destino.split(":")[0], destino.split(":")[1], 3, "Central",
						direccionNodoActual, direccionNodoActual, direccionRta);
			
				new Thread( trabajador ).start();
				
				System.out.println("<ConcultorNC_H.java> Worker consultando a Nodo Central " + destino);
			} else {
				// TODO: esto es provisorio, tengo que hacer bien la cuenta de nodos centrales conectados (ignorando los null, cosa
				//       que ahora no hago)
				latchCentrales.countDown();
			}
		}
		
		// Barrera donde se espera por la respuesta de todas las hojas
		boolean error = false;
		try {
			latchHojas.await();
			//latchCentrales.await();
		} catch (InterruptedException e1) {
			error = true;
		}
		
		if(error){
			// resultado = new HashMap<String, CredImagen[]>(); // Devuelve diccionario vacío si hubo algún error
			System.out.println("<ConsultorNC_H.java> OJO!! hubo un error");
			return false;
		} else {
			resultado = similares.getHash();
		}

		// Envío de respuesta a la Hoja solicitante (sólo si hay imágenes similares)
		try {
			//salida.writeObject(new Mensaje(null,4,resultado));
			// Conexión tmp con Hoja solicitante
			ConexionTcp conexionTmp = new ConexionTcp(msj.recepcionRta().split(":")[0],
													  Integer.parseInt(msj.recepcionRta().split(":")[1]));
			
			/*La respuesta es una tupla donde el primer elemento es la imagen que la Hoja pasó como modelo
			 * y el segundo un diccionario. Las claves son las direcciones de las H que poseen imágenes
			 * similares y el value son las imágenes similares*/
			respuesta = new Tupla2<CredImagen, HashMap<String, CredImagen[]>>(modelo,resultado);
			conexionTmp.enviarSinRta((new Mensaje(null,11,respuesta)));
			conexionTmp.cerrar();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	
	/**
	 * Método por el cual el Consultor recibe el listado de imágenes que una Hoja desea compartir
	 * @param entrada
	 * @param salida
	 * @param msj
	 * @return
	 */
	private boolean recibirListado(ObjectInputStream entrada, ObjectOutputStream salida,
			Mensaje msj){		
		Integer cantidad, emisor, resultado;

		resultado = 0;

		// Obtiene de la solicitud la cantidad de imágenes a recibir
		cantidad = (Integer) msj.getCarga();
		
		//Acá debería mínimamente procesar las imágenes en caso de que haya alguna cuota por hoja
		//u otro factor que pudiera hacer que el servidor no acepte todas las imágenes compartidas.
		//Por ahora, asumo que se pueden indexar todas las imágenes.
		
		try {
			msj = (Mensaje) entrada.readObject();
		} catch (ClassNotFoundException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Carga las credenciales de las imágenes en el índice.
		atributos.indexarImagenes(msj.getEmisor(), (ArrayList<CredImagen>) msj.getCarga());
		System.out.println("\tIndexadas imágenes de: " + sockToString());

		//Envía el resultado al cliente (por ahora no contemplo que esto pueda fallar así que siempre
		//va a ser código 0 (sin problemas)
		try {
			salida.writeObject(new Mensaje(null,3,resultado));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (String clave:atributos.getClavesIndiceImagenes() ){
			System.out.println(clave + ": " + atributos.getImagenes(clave));
		}
		return true;
	}
	
	
	/**
	 * Método por el cual el Consultor (servidor)  atiende por primera vez a un Cliente (Nodo Hoja).
	 * Recibe un saludo y le responde con otro, más un mensaje con el ID asignado
	 * @param entrada
	 * @param salida
	 * @param direcciones 
	 * @return
	 */
	private boolean saludo(ObjectInputStream entrada, ObjectOutputStream salida, String direccionesServer){
		String idAsignado;
		Mensaje mensaje;
		String direccionesHoja;
		
		try {
			// direccionesServer puede ser en realidad un TOKEN que identifica a la HOJA pues ya estuvo conectada al nodo
			// En ese caso se recuperará e informará el ID que le fue asignado anteriormente
			// El token es un string alfanumérico de 16 dígitos, que en este contexto se identifica de la siguiente manera
			// ##DDDDDDDDDDDDDDDD##
			if (direccionesServer.length()==20 && direccionesServer.startsWith("##") && direccionesServer.endsWith("##")) {
				// se recibió un token
				
				// IGNORO ESTO POR AHORA
				
			}
			
			// El ID de Hoja es compartido por todas las instancias por lo que debe sincronizarse su acceso.
			idAsignado = atributos.generarToken();
			
			//¿El servidor debería tener un ID, un Mensaje propio sin campo emisor o pongo null como ahora?
			salida.writeObject(new Mensaje(null,1, idAsignado.toString()));
			System.out.println("-> Asignado ID " +  idAsignado + " a cliente " + sockToString());
			
			// La Hoja envía en el saludo la dirección y puerto de su servidor de consultas. Junto a los de
			// su faceta cliente se confecciona el arreglo de direcciones que será indexado
			direccionesHoja = sock.getInetAddress().getHostAddress() + ":" + sock.getPort();
			direccionesHoja += ";" + direccionesServer;
			
			//Guardado del par ID<->Nodo Hoja (atributo de clase, debe sincronizarse)
			atributos.indexarHoja(idAsignado, direccionesHoja);
						
			return true;
		} catch (IOException e){
			//IOException -> por escritura en buffer salida.
		    //ClassNotFoundException -> por lectura en buffer de entrada.
			System.out.println(e.toString());
			return false;
		}
	}



	/* --------------------- */
	/* Métodos de Interfaces */
	/* --------------------- */
	@Override
	public void atender() {
		Integer emisor;
		Mensaje mensaje;
		boolean terminar = false;
		ObjectInputStream buffEntrada;
		ObjectOutputStream buffSalida;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
				case 1:
					if (!saludo(buffEntrada, buffSalida, (String) mensaje.getCarga())) {terminar=true;}
					break;
				// case 2 podría ser saludo para reconexion
				case 3:
					if(!recibirListado(buffEntrada,buffSalida,mensaje)) {
						terminar=true;
						//Borrar
						/*ArrayList<CredImagen> listado = this.indiceImagenes.get(mensaje.getEmisor());
						for(int i=0;i<listado.size();i++) {
							System.out.println(mensaje.getEmisor() + ": " + listado.get(i));
						}*/
					}
					break;
				case 4:
					if(!recibirConsulta(buffEntrada,buffSalida,mensaje)) {
						terminar=true;
					}
					break;
				//case TERMINAR:
				case 0:
					//El paquete indicaba el cierre de la conexión.
					terminar = true;
					break;
				default:
					System.out.println("\tRecibí: " + mensaje.getCarga());
					break;
				}
			}
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
		}
		
	}
	

	@Override
	public void run() {
		this.atender();
		
	}


	@Override
	public void setSock(Socket sock) {
		this.sock = sock;
		
		// ¿Será este el problema?
		
		String origen = sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort();
		String destino = sock.getInetAddress().getHostName() + ":" + sock.getPort();
		System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("<%s:%d>", sock.getInetAddress().getHostAddress(), sock.getPort());
	}


} // Fin clase



/* --------------------------------------------------------- */
/* Clases auxiliares (debería moverlas a archivos separados) */
/* --------------------------------------------------------- */

/**
 * Clase que permite utilizar un HashMap<String,CredImagen[]> entre hilos sin problemas de concurrencia.
 * Se usa para almacenar las respuestas de aquellas Hojas que posean imágenes similares a la query.
 * La sincronización se garantiza a nivel de instancia.
 * @author rodrigo
 *
 */
class HashConcurrente{
	private HashMap<String, CredImagen[]> hash;
	private final Object lock;
	
	public HashConcurrente() {
		hash = new HashMap<String, CredImagen[]>();
		lock = new Object();
	}
	
	//public void agregar(String clave, String[] valor){
	public void agregar(String clave, CredImagen[] valor){
		synchronized (lock) {
			hash.put(clave, valor);
		}
	}

	public HashMap<String, CredImagen[]> getHash(){
		// No hace falta que esté sincronizado en realidad
		synchronized (lock) {
			return hash;
		}
	}
	
	public void mergear(HashMap<String, CredImagen[]> hashNuevo){
		synchronized (lock) {
			for(String clave : hashNuevo.keySet()){
				hash.put(clave, hashNuevo.get(clave));
			}
		}
	}
}


/**
 * Worker del Nodo Central. Se encarga de retransmitir una consulta recibida, ya sea a un Nodo Hoja
 * o un Nodo Central.
 * 
 * Tal vez un mejor nombre sería "Retransmisor" o algo así.
 * 
 * @author rodrigo
 *
 */
class Worker implements Runnable {
	private ArrayList<CredImagen> indexadas;
	private HashConcurrente respuesta;
	private int puerto, ttl;
	private String emisor, id, ip, modo, origen, direccionRta;
	private String[] candidatas;
	private final CredImagen referencia;
	private final CountDownLatch doneSignal;
	
	// ip y puerto corresponden al destino. origen y emisor corresponden al NC que emitió la consulta
	// originalmente y al que la retransmite respectivamente
	
	
	Worker(String id, HashConcurrente respuestas, CredImagen referencia, ArrayList<CredImagen> indexadas,
			CountDownLatch doneSignal, String ip, String puerto, int ttl, String modo) {
		this.id = id;
		this.respuesta = respuestas;
		this.referencia = referencia;
		this.indexadas = indexadas;
		this.doneSignal = doneSignal;
		
		// TTL de los mensajes utilizados en las consultas (1 para los enviados a Hojas)
		this.ttl = ttl;
		
		// "Socekt" servidor de la Hoja/Central a consultar
		this.ip = ip;
		this.puerto = Integer.parseInt(puerto);
		
		// Destino de la consulta (Hoja/Central)
		this.modo = (modo == "H" || modo == "Hoja") ? "HOJA" : "CENTRAL";
		
		// NC que origina la consulta
		this.origen = null;
	}

	// Constructor usado para las instancias que interacturán con NC (se agrega la dirección del NC
	// que origina la consulta y la dirección a donde debe enviarse la rta)
	Worker(String i, HashConcurrente respuestas, CredImagen referencia, ArrayList<CredImagen> indexadas,
			CountDownLatch doneSignal, String ip, String puerto, int ttl, String modo,
			String direccionOrigen, String direccionEmisor, String direccionRta) {
		this.id = i;
		this.respuesta = respuestas;
		this.referencia = referencia;
		this.indexadas = indexadas;
		this.doneSignal = doneSignal;
		
		// TTL de los mensajes utilizados en las consultas (1 para los enviados a Hojas)
		this.ttl = ttl;
		
		// "Socekt" servidor de la Hoja/Central a consultar
		this.ip = ip;
		this.puerto = Integer.parseInt(puerto);
		
		// Destino de la consulta (Hoja/Central)
		this.modo = (modo == "H" || modo == "Hoja") ? "HOJA" : "CENTRAL";
		
		// NC que origina la consulta
		this.origen = direccionOrigen;
		
		// NC que retransmite la consulta (quien instancia al Worker)
		this.emisor = direccionEmisor;
		
		// Hoja que espera la respuesta
		this.direccionRta = direccionRta;
	}

	
	public void run() {
		if (modo == "HOJA") {
			consultarHoja();
		} else {
			consultarCentral();
		}
		
		doneSignal.countDown();
	}

	
	/**
	 * Método en el que se establece conexión con un Nodo Hoja (su faceta Servidor) y se consulta por
	 * imágenes similares a la dada como referencia.
	 */
	boolean consultarHoja() {
		//ArrayList<String> candidatas;
		ArrayList<CredImagen> candidatas;
		ConexionTcp conexion;
		Mensaje rta, rta2;
		
		// Funcionamiento básico: Evalúa las imágenes indexadas correspondientes al Nodo Hoja
		// indicado (por los parámetros IP y puerto) identificando aquellas que potencialmente son similares
		// a la recibida como referencia. Cuando la rta es recibida la almacena en el arreglo <respuesta>,
		// en la posición determinada por su ID de worker (de manera tal que no necesite sincronización
		// pues sólo accede a su posición pre establecida) 
		
		// Paso 1: conexión con Nodo Hoja
		try {
			// TODO: cambiar el puerto (bien conocido en el sistema) cuando no esté más hardcodeado
			conexion = new ConexionTcp(ip, puerto);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		// Paso 2: búsqueda de imágenes candidatas
		//candidatas = new ArrayList<String>();
		candidatas = new ArrayList<CredImagen>();
		for(int i=0; i<indexadas.size(); i++){
			int[] otroVector = (indexadas.get(i)).getVecCarComprimido();
			float distancia = referencia.comparacionRapida(otroVector);
			if(distancia < 10000){
				candidatas.add((indexadas.get(i)));
			}
		}
		
		if(candidatas.isEmpty())
			return false;
		
		// TODO: acá forzé el "00" porque da igual pero debería usar el ID
		// Paso 3: envío de consulta, imagen de referencia y recepción de respuestas
		conexion.enviarSinRta(new Mensaje("00",10,referencia));
		rta = (Mensaje) conexion.enviarConRta(new Mensaje("00",10,candidatas)); //rta: imgs similares
		rta2 = (Mensaje) conexion.recibir(); //rta2: ip+puerto del nodo que las posee.
		
		if(rta == null){
			System.out.println("<ConsultorNC.java> Entró en fix provisorio");
			//rta = new Mensaje(00,10,new ArrayList<String>());
			rta = new Mensaje("00",10,new ArrayList<CredImagen>());
		}
		
		// Paso 4: almacenamiento del listado de imágenes de la Hoja similares a la de referencia
		ArrayList<CredImagen> tmp = (ArrayList<CredImagen>) rta.getCarga();
		CredImagen[] arreglo = (CredImagen[]) ( (ArrayList<CredImagen>) rta.getCarga() ).toArray(new CredImagen[0]);
		if(arreglo.length != 0)
			respuesta.agregar((String) rta2.getCarga(), arreglo);
		
		conexion.cerrar();
		
		return true;
	}
	
	
	/**
	 * Método en el que se establece conexión con un Nodo Central y se retransmite una consulta
	 */
	boolean consultarCentral() {
		ArrayList<CredImagen> candidatas;
		ConexionTcp conexion;
		Mensaje rta;
		String destinoRta;
		
		// Funcionamiento básico: retransmite la consulta (la imagen recibida como referencia, concretamente)
		// a los Nodos Centrales indexados. 
		// Se espera la respuesta de todos antes de devolver el resultado. 
		
		// Paso 1: conexión con Nodo Central
		try {
			// TODO: cambiar el puerto (bien conocido en el sistema) cuando no esté más hardcodeado
			conexion = new ConexionTcp(ip, puerto);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		// Paso 2: envío de consulta, imagen de referencia y recepción de respuestas
		destinoRta = direccionRta;
		
		rta = (Mensaje) conexion.enviarConRta(new Mensaje("00",origen,emisor,30,ttl,referencia, destinoRta)); //rta: imgs similares
		
		if(rta == null){
			System.out.println("<ConsultorNC_H-Worker.java> Entró en fix provisorio");
			rta = new Mensaje("00",30,new ArrayList<CredImagen>());
		}
		
		// Paso 3: almacenamiento del listado recibido
		System.out.println("Fijate rta que tiene y por qué no castea!");
		HashMap<String,CredImagen[]> similares = ( HashMap<String,CredImagen[]> ) rta.getCarga();
		if(similares.keySet().size() != 0)
			respuesta.mergear(similares);
		
		conexion.cerrar();
		
		return true;
	}
 } // Fin clase



