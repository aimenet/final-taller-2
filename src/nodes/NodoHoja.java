package nodes;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.HashMap;
import java.util.Properties;

import commons.Constantes;
import commons.Tarea;
import nodes.components.atributos.AtributosCentral;
import nodes.components.atributos.AtributosHoja;
import nodes.components.clientes.ClienteNH_Gral;
import nodes.components.corethreads.*;
import nodes.components.servidores.*;

/**
 * Nodo Hoja del sistema distribuido. Estos poseen las imágenes a compartir, calculan sus vectores
 * característicos, emiten consultas, respuestas y calculan similitud entre imágenes.
 * Están conectadas a dos Nodos Centrales.
 * 
 * La faceta cliente responde a un esquema Productor/Consumidor: un thread (gui) se encarga de producir las consultas
 * que los hilos consumidores (valga la redundancia) enviarán a los NCs.
 *
 * @author rodrigo
 *
 */
public class NodoHoja extends Nodo {
	private Properties config;


	private void setClients() {
		int max_clients = Integer.parseInt(config.getProperty("max_nc"));

		this.addClientCoreThread(
			new ClientCoreThread("PPAL + NC-0", 0, ClienteNH_Gral.class)
		);

//		for (int i=1; i < max_clients; i++) {
//			this.addClientCoreThread(
//				new ClientCoreThread("NC-" + i, i, ClienteNH_Gral.class)
//			);
//		}
	}

	private void setControllers() {
		this.addClientCoreThread(
			new ControladorNHCoreThread("Controller-0")
		);
	}

	private void setInitialTasks() {
		HashMap<String, Object> taskPayload = new HashMap<String, Object>();
		taskPayload.put("direccionWKAN", ((AtributosHoja) atributos).getWkanInicial());
		taskPayload.put("primeraVez", true);
		this.addInitialTask(
			Constantes.COLA_SALIDA,
			new Tarea(Constantes.TSK_NH_SOLICITUD_NCS, taskPayload)
		);
	}

	private void setServers() {
		this.addServerCoreThread(
				new ServerCoreThread(
						"acceso",
						atributos.getDireccion().ip.getHostAddress(),
						atributos.getDireccion().puerto_na,
						config.getProperty("nombre")+": Acceso",
						ConsultorH.class
				)
		);

		this.addServerCoreThread(
				new ServerCoreThread(
						"centrales",
						atributos.getDireccion().ip.getHostAddress(),
						atributos.getDireccion().puerto_nc,
						config.getProperty("nombre")+": Centrales",
						ConsultorH.class
				)
		);

		this.addServerCoreThread(
				new ServerCoreThread(
						"hojas",
						atributos.getDireccion().ip.getHostAddress(),
						atributos.getDireccion().puerto_nh,
						config.getProperty("nombre")+": Hojas",
						ConsultorH.class
				)
		);

		this.addServerCoreThread(
				new ServerCoreThread(
						"mantenimiento",
						atributos.getDireccion().ip.getHostAddress(),
						atributos.getDireccion().puerto_m,
						config.getProperty("nombre")+": Mantenimiento",
						ConsultorMantenimientoNH.class
				)
		);
	}

	private void setWorkers() {
//		int id = 0;
//
//		// Pese a ser "worker" se carga en la cola de clientes
//		this.addClientCoreThread(
//			new WorkerNHInternoCoreThread("Interno-" + id, id)
//		);
	}

	public NodoHoja(String configFile) throws UnknownHostException {
		super(new AtributosHoja());

		try {
			config = new Properties();
			config.load( new FileInputStream(configFile) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}

		atributos.setDireccion(InetAddress.getByName(config.getProperty("ip")));
		atributos.setNombreColas(new String[] {Constantes.COLA_SALIDA, Constantes.COLA_INTERNA});
		atributos.setColas();
		((AtributosHoja) atributos).setWkanInicial(config.getProperty("wkan"));
		((AtributosHoja) atributos).setCantCentrales(Integer.parseInt(config.getProperty("max_nc")));

		this.setClients();
		this.setServers();
		this.setControllers();
		this.setInitialTasks();
		this.setWorkers();
	}
}
