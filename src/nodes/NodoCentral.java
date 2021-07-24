package nodes;
/**
 * Nodo Central del sistema de recuperación de imágenes. Se enlaza con Hojas y otros Nodos Centrales. 
 * Posee un índice con todas las imágenes compartidas por las Hojas conectadas a él, donde registra
 * un vector característico comprimido por cada una. 
 * Recibe consultas, las transmite a aquellas Hojas con imágenes candidatas, a los Nodos Centrales vecinos
 * y envia las respuestas al solicitante.
 * 
 * Implementa la interfaz Runnable a fin de generar un hilo por cada Cliente que se conecta.
 * 
 * La cola de mensajes (ArrayList) es un atributo de la clase (static) a fin de ser compartido por todos los threads.
 * 
 * @author rodrigo
 *
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import commons.Constantes;
import commons.DireccionNodo;
import commons.Tarea;
import nodes.components.atributos.AtributosAcceso;
import nodes.components.atributos.AtributosCentral;
import nodes.components.clientes.ClienteNA_NA;
import nodes.components.clientes.ClienteNC_NA;
import nodes.components.clientes.ClienteNC_NC;
import nodes.components.corethreads.ClientCoreThread;
import nodes.components.corethreads.NCClientCoreThread;
import nodes.components.corethreads.ServerCoreThread;
import nodes.components.servidores.*;


public class NodoCentral extends Nodo {
	private Properties config;


	private void setClients() {
		Integer clientes_nc_nc = Integer.parseInt(this.config.getProperty("centrales"));
		clientes_nc_nc = clientes_nc_nc > 1 ? clientes_nc_nc : 2;

		int i = 0;
		while (i < clientes_nc_nc){
			String name = "NC-" + i;
			this.addClientCoreThread(
					new NCClientCoreThread(name, i, Constantes.COLA_NC, ClienteNC_NC.class)
			);

			i += 1;
		}

		this.addClientCoreThread(
				new NCClientCoreThread("NA-" + i, i, Constantes.COLA_NA, ClienteNC_NA.class)
		);
	}

	private void setServers() {
		this.addServerCoreThread(
			new ServerCoreThread(
				"hojas",
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nh,
				config.getProperty("nombre")+": Hojas",
				ConsultorNC_H.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"centrales",
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_nc,
				config.getProperty("nombre")+": Centrales",
				ConsultorNC_NC.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"acceso",
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_na,
				config.getProperty("nombre")+": Acceso",
				ConsultorNC_NA.class
			)
		);

		this.addServerCoreThread(
			new ServerCoreThread(
				"mantenimiento",
				atributos.getDireccion().ip.getHostAddress(),
				atributos.getDireccion().puerto_m,
				config.getProperty("nombre")+": Mantenimiento",
				ConsultorMantenimientoNC.class
			)
		);

	}

	private void setInitialTasks() {
		Tarea task = new Tarea(
				00,
				Constantes.TSK_NC_ANUNCIO_WKAN,
				((AtributosCentral) atributos).getWKANAsignado()
		);

		this.addInitialTask(Constantes.COLA_NA, task);
	}

	private void setPeriodicTasks() {
		this.addPeriodicTask(
			Constantes.COLA_NA, new Tarea(00, Constantes.TSK_NC_CHECK_ANUNCIO, null), 10
		);

		this.addPeriodicTask(
			Constantes.COLA_NA, new Tarea(00, Constantes.TSK_NC_CHECK_VECINOS, null), 10
		);

		this.addPeriodicTask(
			Constantes.COLA_NA,
			new Tarea(00, Constantes.TSK_NC_SEND_KEEPALIVE_WKAN, null),
				(int) TimeUnit.MILLISECONDS.convert(
					((AtributosCentral) atributos).keepaliveWKAN, TimeUnit.SECONDS
				)
		);
	}

	public NodoCentral(String archivoConfiguracion) throws UnknownHostException {
		super(new AtributosCentral());

		try {
			config = new Properties();
			config.load( new FileInputStream(archivoConfiguracion) );
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("No existe el archivo de configuración");
			System.exit(1);
		}
		
		atributos.setDireccion(InetAddress.getByName(config.getProperty("ip")));

		InetAddress nodoAcceso = InetAddress.getByName(config.getProperty("wkan"));
		((AtributosCentral) atributos).setWKANAsignado(new DireccionNodo(nodoAcceso));

		((AtributosCentral) atributos).setMaxCentralesVecinos(Integer.parseInt(this.config.getProperty("centrales")));
		
		atributos.setNombreColas(new String[]{Constantes.COLA_NA, Constantes.COLA_NC, Constantes.COLA_NH});
		atributos.setColas();

		this.setServers();
		this.setClients();
		this.setInitialTasks();
		this.setPeriodicTasks();
	}
}