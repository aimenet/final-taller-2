package nodes;

import commons.Constantes;
import commons.DireccionNodo;
import commons.Tarea;
import nodes.components.atributos.AtributosAcceso;
import nodes.components.clientes.ClienteNA_NA;
import nodes.components.clientes.ClienteNA_NC;
import nodes.components.corethreads.ClientCoreThread;
import nodes.components.corethreads.ServerCoreThread;
import nodes.components.corethreads.WorkerNAInternoCoreThread;
import nodes.components.servidores.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Properties;


public class NodoAccesoBienConocido extends Nodo {
	private ArrayList<DireccionNodo> wkanIniciales;
	private Integer maxClientes;
	private Properties config;


	private int[] getClientsAmount() {
		/* Devuelve la cantidad de Clientes que deben instanciarse para conectarse a los distintos tipos de Nodos.
		*
		*  La política definida es de 2 clientes de WKAN por cada cliente de NC
		*
		*  Rta: int[0] -> cantidad de clientes para atender WKAN
		*  Rta: int[1] -> cantidad de clientes para atender NC
		* */
		double ncFactor = 0.33;
		double wkanFactor = 0.66;
		int[] response = new int[2];

		response[0] = (int) Math.round(this.maxClientes * wkanFactor);
		response[1] = (int) Math.round(this.maxClientes * ncFactor);

		return response;
	}

	private void setServers() {
		this.addServerCoreThread(
			new ServerCoreThread(
				"NABC",
				config.getProperty("ip"),
				Constantes.PUERTO_NA,
				config.getProperty("nombre")+": Bien Conocidos",
				ConsultorNA_NA.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"CENTRALES",
				config.getProperty("ip"),
				Constantes.PUERTO_NC,
				config.getProperty("nombre")+": Centrales",
				ConsultorNA_NC.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"HOJAS",
				config.getProperty("ip"),
				Constantes.PUERTO_NH,
				config.getProperty("nombre")+": Hojas",
				ConsultorNA_NH.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"MANTENIMIENTO",
				config.getProperty("ip"),
				Constantes.PUERTO_MANTENIMIENTO,
				config.getProperty("nombre")+": Mantenimiento",
				ConsultorMantenimientoWKAN.class
			)
		);
	}

	private void setClients() {
		int[] clientsAmounts = this.getClientsAmount();
		int ncClients = clientsAmounts[1];
		int wkanClients = clientsAmounts[0];
		int clientSharedId = 0;

		for (int i=0; i < wkanClients; i++) {
			String name = "WKAN-" + clientSharedId;

			this.addClientCoreThread(
				new ClientCoreThread(name, clientSharedId, ClienteNA_NA.class)
			);

			clientSharedId += 1;
		}

		for (int i=0; i < ncClients; i++) {
			String name = "NC-" + clientSharedId;

			this.addClientCoreThread(
				new ClientCoreThread(name, clientSharedId, ClienteNA_NC.class)
			);

			clientSharedId += 1;
		}

		this.addClientCoreThread(
			new WorkerNAInternoCoreThread("Interno-1", clientSharedId)
		);
	}

	private void setWkanIniciales() throws UnknownHostException {
		for (Object key : config.keySet()) {
			String clave = (String) key;
			if ((clave).startsWith("wkan_")) {
				InetAddress dir = InetAddress.getByName(config.getProperty(clave));
				this.wkanIniciales.add(new DireccionNodo(dir));
			}
		}
		if (this.wkanIniciales.size() > 0)
			((AtributosAcceso) atributos).addNodos(wkanIniciales);
	}

	private void setInitialTasks() {
		for (DireccionNodo ip : ((AtributosAcceso) atributos).getNodos().keySet()) {
			this.addInitialTask("salida", new Tarea(00, "ANUNCIO", ip));
		}
	}

	private void setPeriodicTasks() {
		this.addPeriodicTask(
			Constantes.COLA_INTERNA, new Tarea(00, "CHECK_KEEPALIVE_NCS", null), 30
		);

		this.addPeriodicTask(
			Constantes.COLA_SALIDA, new Tarea(00, "INFORMAR_WKANS", null), 30
		);
	}


	public NodoAccesoBienConocido(String archivoConfiguracion) throws UnknownHostException {
		super(new AtributosAcceso());

		try {
			config = new Properties();
			config.load( new FileInputStream(archivoConfiguracion) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}

		maxClientes = Integer.parseInt(this.config.getProperty("clientes"));
		wkanIniciales = new ArrayList<DireccionNodo>();

		((AtributosAcceso) atributos).setKeepaliveNodoVecino(
				Integer.parseInt(config.getProperty("keepalive_nodo_vecino"))
		);
		atributos.setDireccion(InetAddress.getByName(config.getProperty("ip")));
		atributos.setNombreColas(new String[] {"salida","centrales","interna"});
		atributos.setColas();

		if (config.containsKey("max_nc"))
			((AtributosAcceso) atributos).setMaxNCCapacity(Integer.parseInt(this.config.getProperty("max_nc")));

		this.setServers();
		this.setClients();
		this.setWkanIniciales();
		this.setInitialTasks();

		// TODO: el "mandar" es para debuggeo, para limitar el envío de mensajes y que no sea un lío de paquetes
		if (config.getProperty("mandar").equals("si"))
			this.setPeriodicTasks();
	}
}
