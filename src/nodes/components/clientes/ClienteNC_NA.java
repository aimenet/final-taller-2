package nodes.components.clientes;

import java.sql.Timestamp;
import java.util.HashMap;

import commons.*;
import commons.mensajes.Mensaje;
import commons.mensajes.wkan_nc.SolicitudNcsVecinos;
import nodes.components.atributos.AtributosCentral;

/**
 * Una de las instancias que compone la "faceta" Cliente de un Nodo Central. Es la encargada de 
 * conectarse a un Nodo de Acceso Bien Conocido e interactuar con él.
 * 
 * Las instancias consumen de una cola de tareas, sincronizada pues puede haber racing conditions, actuando más como
 * "consumidores".
 * 
 * @author rodrigo
 * @since 2019-11-02
 */

public class ClienteNC_NA extends Cliente {
	// Atributos
	// -----------------------------------------------------------------------------------------------------------------


	// Métodos auxiliares
	// -----------------------------------------------------------------------------------------------------------------
	private void logUnaLinea(String mensaje) {
		System.out.printf("[Cli WKAN %s] ", this.id);
		System.out.printf("%s\n", mensaje);
	}

	// Métodos de procesamiento de tareas
	// -----------------------------------------------------------------------------------------------------------------
	public ClienteNC_NA(int idConsumidor, String cola) {
		super(idConsumidor, cola);
		this.atributos = new AtributosCentral();  // <atributos> está declarado en Cliente
	}


	private Boolean anuncioFnc(DireccionNodo wkan) {
		/**
		 * Se "presenta" ante un NABC (comunicando su IP) a fin de ingresar a la red.
		 *
		 */
		Boolean output = false;

		System.out.printf("[Con %s] Ejecutando ANUNCIO-WKAN\n", this.id);

		Boolean success = false;
		Integer contador = 0;
		Integer intentos = 3;

		while ((contador < intentos) && (!success)) {
			if (this.establecerConexion(wkan.ip.getHostAddress(), wkan.puerto_nc)) {
				System.out.printf("Anunciando a WKAN %s", wkan.ip.getHostName());

				Mensaje saludo = new Mensaje(
						this.atributos.getDireccion(),
						Codigos.NC_NA_POST_ANUNCIO,
						null
				);
				Mensaje respuesta = (Mensaje) this.conexionConNodo.enviarConRta(saludo);

				if (respuesta.getCodigo() == Codigos.OK) {
					System.out.printf(" [OK]\n");
					((AtributosCentral) atributos).marcarIntentoConexionWKAN(true);
				} else if (respuesta.getCodigo() == Codigos.ACCEPTED) {
					// el wkan no tiene capacidad para aceptarme pero retransmitió la consulta.
					System.out.printf(" [ERROR]\n");
					System.out.println("Iniciando espera de aceptación");
				}

				// Cualquiera haya sido la respuesta, termina el bucle
				success = true;

			} else {
				contador += 1;
				continue;
			}
		}

		if (!success) {
			System.out.print("\n[Con " + this.id + "]: ");
			System.out.println("falló aununcio a WKAN " + wkan.ip.getHostName());

			// Marca el timestamp de último intento de acceso para reintentar si expira sin haber podido anunciarse
			((AtributosCentral) atributos).marcarIntentoConexionWKAN(false);

			// TODO: necesito una cola temporal donde haya tareas con delay. El hilo que la controle debe
			// estar revisando constantemente cuando expire el delay de alguna tarea para encolarla en la cola
			// definitiva
		}

		return output;
	}


	private Boolean checkAnuncioFnc() {
		/**
		 * Evalúa si el nodo ingresó a la red, para reenviar el anuncio al WKAN en caso contrario.
		 *
		 */
		Boolean output = false;
		DireccionNodo wkan = ((AtributosCentral) atributos).getWKANAsignado();

		System.out.printf("[Cli WKAN %s] ", this.id);
		System.out.printf("Checkeando anuncio ante WKAN: ");

		if (!((AtributosCentral) atributos).getAceptadoPorWKAN()){
			Integer espera = ((AtributosCentral) atributos).getTimeoutEsperaAnuncioWKAN();  // segundos
			Timestamp ultimoIntento = ((AtributosCentral) atributos).getUltimoIntentoConexionWKAN();

			if(ultimoIntento.compareTo(new Timestamp(System.currentTimeMillis() - espera * 1000)) <= 0) {
				try {
					atributos.encolar("acceso", new Tarea(00,"ANUNCIO_WKAN", wkan));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		output = true;
		return output;
	}


	private Boolean checkVecinosFaltantes() {
		Integer faltantes = ((AtributosCentral) this.atributos).getNcsVecinosFaltantes();
		DireccionNodo wkanAsignado = ((AtributosCentral) this.atributos).getWKANAsignado();

		if (faltantes > 0) {
			SolicitudNcsVecinos solicitud = new SolicitudNcsVecinos(
					atributos.getDireccion(),
					Codigos.NC_NA_GET_SOLICITUD_VECINOS,
					faltantes
			);

			if (this.establecerConexion(wkanAsignado.ip.getHostAddress(), wkanAsignado.puerto_nc)) {
				this.conexionConNodo.enviarSinRta(solicitud);
			} else {
				logUnaLinea("Solicitud de NCs vecinos a WKAN: imposible establecer conexión");
				return false;
			}

			logUnaLinea(String.format("Solicitados %s NCs vecinos a WKAN", faltantes));
		}

		return true;
	}


	private Boolean keepAliveFnc() {
		/**
		 * Se informa al WKAN que este nodo está "vivo"
		 *
		 */
		Boolean output = false;
		DireccionNodo wkan = ((AtributosCentral) atributos).getWKANAsignado();

		System.out.printf("[Cli WKAN %s] ", this.id);
		System.out.printf("Envío keepalive WKAN %s ", wkan.ip.getHostName());

		if (this.establecerConexion(wkan.ip.getHostAddress(), wkan.puerto_nc)) {
			Mensaje saludo = new Mensaje(this.atributos.getDireccion(),
					Codigos.NC_NA_POST_KEEPALIVE,
					null
			);

			this.conexionConNodo.enviarSinRta(saludo);

			output = true;

			System.out.println("[OK]");
		} else {
			// Acá no le doy mucha vuelta porque esta es una tarea periódica
			System.out.println("[ERROR]");
		}

		return output;
	}


	@Override
	public HashMap<String, Comparable> procesarTarea(Tarea tarea) throws InterruptedException {
		HashMap<String, Comparable> output;

		output = null;
		
		switch(tarea.getName()){
			case "ANUNCIO_WKAN":
				// Se "presenta" ante un NABC (comunicando su IP) a fin de ingresar a la red
				this.anuncioFnc((DireccionNodo) tarea.getPayload());
				
				// TODO: terminar la conexión con WKAN
				break;
			case "CHECK_ANUNCIO":
				// Verifica si es necesario reenviar el anuncio al WKAN pues aún no se ingresó a la red
				this.checkAnuncioFnc();

				break;
			case "SEND_KEEPALIVE_WKAN":
				// Tarea mediante la que se le informa al WKAN que este nodo está "vivo"
				
				this.keepAliveFnc();

				break;
			case Constantes.TSK_NC_CHECK_VECINOS:
				// Controla la cantidad de NCs vecinos conocidos hasta el momento y en caso de ser necesario solicita
				// más al WKAN
				this.checkVecinosFaltantes();
				break;
		}
		
		this.terminarConexion();
		
		return output;
	}
}

/**
 * [2019-10-19] En las pruebas que hice hasta ahora sólo está consumiendo uno de los threads consumidor, no sé por
 *              qué el notifyall() de los métodos de encolado/desencolado de tareas pareciera despertar siempre al
 *              mismo (por más que use retardos aleatorios para evitarlo)
 *              
 * [2019-11-09] Necesito un nuevo tipo de cola: una donde las tareas tengan un delay o un timestamp en que deben
 *              ser encoladas en la cola definitiva para su procesamiento             
 */
