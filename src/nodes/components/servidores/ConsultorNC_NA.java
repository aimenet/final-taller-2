package nodes.components.servidores;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

import commons.*;
import commons.mensajes.Mensaje;
import commons.mensajes.wkan_nc.InformeNcsVecinos;
import commons.structs.nc.NHIndexada;
import nodes.components.clientes.ClienteNC_NH;
import nodes.components.WKAN_Funciones;
import nodes.components.atributos.AtributosCentral;

public class ConsultorNC_NA implements Consultor {
	// De cada instancia
	private AtributosCentral atributos = new AtributosCentral();
	private ObjectInputStream buffEntrada;
	private ObjectOutputStream buffSalida;
	private Socket sock;
	private WKAN_Funciones funciones = new WKAN_Funciones();
	
	
	@Override
	public void run() {
		this.atender();
	}
	

	private void pingPongFnc(DireccionNodo wkan) {
		/**
		 * PING - PONG para probar la conexión entre los nodos
		 *
 		 */

		System.out.printf("[Con WKAN] Recibido PING de WKAN %s\n", wkan.ip.getHostName());

		try {
			buffSalida.writeObject(new Mensaje(atributos.getDireccion(), Codigos.PONG, null));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void anuncioAceptadoFnc(DireccionNodo wkan) {
		/**
		 * Un WKAN (no el asignado inicialmente) confirma anuncio indicando que será el "administrador".
		 * Se actualizan los datos en consecuencia
		 *
		 */

		System.out.printf("[Con WKAN] Recibido aceptación de WKAN en %s\n", wkan.ip.getHostName());
		atributos.setWKANAsignado(wkan);
		atributos.setAceptadoPorWKAN(true);
	}

	private void anuncioNCFnc(DireccionNodo wkan, DireccionNodo nodoCentral) {
		/**
		 * Se recibió la dirección de un NC al que conectarse (un nuevo "vecino")
		 *
		 */

		System.out.printf("[Con WKAN] ");
		System.out.printf("WKAN %s informó dirección de ", wkan.ip.getHostName());
		System.out.printf("NC vecino %s\n", (String) nodoCentral.ip.getHostName());

		Tarea tarea = new Tarea(00, "ANUNCIO-VECINO", nodoCentral);
		try {
			atributos.encolar("centrales", tarea);
		} catch (InterruptedException e) {
			// TODO 2020-10-02: hacer algo
			e.printStackTrace();
		}
	}

	private void consultaCapacidadFnc(DireccionNodo wkan, DireccionNodo nodoHoja) throws IOException {
		/**
		 * Evalúa la capacidad de aceptar un nuevo NH, siempre que no se encuentre ya registrado.
		 *
		 */

		System.out.printf("[Con WKAN] ");
		System.out.printf("WKAN %s consulta capacidad aceptación ", wkan.ip.getHostName());
		System.out.printf("NH %s", (String) nodoHoja.ip.getHostName());

		Boolean aceptar = false;
		Integer codFinal = Codigos.ACCEPTED;

		if ((((HashMap<UUID, NHIndexada>) atributos.getHojas()).values().size() < this.atributos.getNHCapacity())) {
			if (!atributos.hojaIndexada(nodoHoja)) {
				aceptar = true;
				codFinal = Codigos.OK;
			}
		}

		buffSalida.writeObject(new Mensaje(atributos.getDireccion(), codFinal, aceptar));

		System.out.printf("\t[%s]", aceptar ? "ACEPTAR" : "RECHAZAR");
	}

	private void aceptarNHFnc(DireccionNodo wkan, DireccionNodo nodoHoja) {
		/* WKAN informa NH ante el que anununciarse */

		System.out.printf("[Con WKAN] ");
		System.out.printf("WKAN %s informó dirección de NH ", wkan.ip.getHostName());
		System.out.printf("%s ante el que anunciarse\n", nodoHoja.ip.getHostName());

		// 2020-08-01:
		// Este nodo informará su dirección a la Hoja y nada más. La H se anunciará tal como lo haría
		// si un WKAN hubiera sido quien le proporcionara la dirección del NC.
		// Esto si bien es ineficiente y genera más tráfico en la red, me gusta pues me permite
		// "encapsular" y atomizar los distintos intercambios que se dan en la red, facilitando la
		// comprensión a futuro y "segmentando" en caso de debuggeo

		// Como es de las pocas actividades en las que el NC se conectará a un NH, levanto "on demand"
		// el cliente para tal fin (en lugar de encolar y tener un cliente consultando en todo momento)
		Tarea tarea = new Tarea(00, "INFORMAR-DIRECCION-A-NH", nodoHoja);
		ClienteNC_NH anunciante = new ClienteNC_NH(99, Constantes.COLA_NH);

		HashMap<String, Comparable> resultado = null;
		resultado = anunciante.procesarTarea(tarea);

		System.out.printf("[Con WKAN] ");
		System.out.printf("Enviada dirección a NH %s\t", nodoHoja.ip.getHostName());

		if ((Boolean) resultado.get("status"))
			System.out.printf("[COMPLETADO]\n");
		else
			System.out.printf("[ERROR]\n");
	}

	private void recibirListadoVecinos(InformeNcsVecinos mensaje) {
		ArrayList<DireccionNodo> listado = mensaje.getVecinos();
		Integer cantidadIndexados = 0;

		System.out.printf("[Con WKAN] Recibido listado de %d NCs vecinos", listado.size());

		if (listado.size() > 0) {
			//ArrayList<DireccionNodo> indexados = ((AtributosCentral) atributos).indexarCentrales(listado);
			//cantidadIndexados = indexados.size();

			for (DireccionNodo indexado : listado) {
				Tarea tarea = new Tarea(00, Constantes.TSK_NC_ANUNCIO_VECINO, indexado);

				try {
					atributos.encolar("centrales", tarea);
					System.out.printf("[Con WKAN] Encolada tarea TSK_NC_ANUNCIO_VECINO\n");
				} catch (InterruptedException e) {
					// TODO 2021-04-25: hacer algo
					e.printStackTrace();
				}
			}
		}

		System.out.println(" [OK]");

		//System.out.printf("\tIndexados  %d NCs.", cantidadIndexados);

		//System.out.printf(
		//		"\tEstado actual: %d de %d NCs\n",
		//		atributos.getNcs().size(),
		//		atributos.getMaxCentralesVecinos()
		//);
	}


	@Override
	public void atender() {
		Mensaje mensaje;
		boolean terminar = false;
		
		try {
			// Instanciación de los manejadores del buffer.
			buffSalida = new ObjectOutputStream(sock.getOutputStream());
			buffEntrada = new ObjectInputStream(sock.getInputStream());
			
			// Bucle principal de atención al cliente. Finalizará cuando este indica que cerrará la conexión
			while(!terminar){
				mensaje = (Mensaje) buffEntrada.readObject();
				
				switch(mensaje.getCodigo()){
					case Codigos.PING:
						// PING - PONG para probar la conexión entre los nodos
						this.pingPongFnc(mensaje.getEmisor());
						break;
					case Codigos.NA_NC_POST_ANUNCIO_ACEPTADO:
						// Otro WKAN (no el asignado inicialmente) será el "administrador". Actualiza los datos
						this.anuncioAceptadoFnc(mensaje.getEmisor());
						break;
					case Codigos.NA_NC_POST_NC_VECINO:
						// Recibe la dirección de un NC al que conectarse
						this.anuncioNCFnc(mensaje.getEmisor(), (DireccionNodo) mensaje.getCarga());
						break;
					case Codigos.NA_NC_POST_CAPACIDAD_NH:
						// Evalúa la capacidad de aceptar un nuevo NH, siempre que no se encuentre ya registrado
						this.consultaCapacidadFnc(mensaje.getEmisor(), (DireccionNodo) mensaje.getCarga());
						//terminar = true;
						break;
					case Codigos.NA_NC_POST_ACEPTAR_NH:
						// WKAN informa NH ante el que anununciarse
						this.aceptarNHFnc(mensaje.getEmisor(), (DireccionNodo) mensaje.getCarga());
						break;
					case Codigos.NA_NC_POST_VECINOS:
						// WKAN informa posibles NCs vecinos
						this.recibirListadoVecinos((InformeNcsVecinos) mensaje);
						break;
					case Codigos.CONNECTION_END:
						terminar = true;
						break;
					default:
						System.out.printf("\tRecibido mensaje en %s: %s\n", sockToString(), mensaje.getCarga());
						break;
				}
			}
			
			sock.close();
			System.out.printf("-> Conexión con %s finalizada\n", sockToString());
		} catch (IOException | ClassNotFoundException e) {
			//IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
			//ClassNotFoundException -> buffer de entrada.
			// TODO: hacer algo en caso de error
			e.printStackTrace();
		}
	}


	@Override
	public void setSock(Socket sock) {
		this.sock = sock;
		
		// ¿Será este el problema?
		
		String origen = sock.getLocalAddress().getHostName() + ":" + sock.getLocalPort();
		String destino = sock.getInetAddress().getHostName() + ":" + sock.getPort();
		//System.out.println(String.format("<ConsultorNC.java> <%s> -> <%s>", origen,destino));
	}


	@Override
	public String sockToString() {
		return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
	}

}
