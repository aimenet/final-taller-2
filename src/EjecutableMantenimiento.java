import commons.Codigos;
import commons.ConexionTcp;
import commons.DireccionNodo;
import commons.mensajes.Mensaje;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

// TODO: hacer un Cliente al uso para esto
public class EjecutableMantenimiento {
	/**Método que simula el borrado de la pantalla.*/

	private String consultarNodo(DireccionNodo nodo) throws IOException {
		String localIp = "127.0.0.10";  // TODO: des-hardcodear
		DireccionNodo estaDireccion = new DireccionNodo(InetAddress.getByName(localIp));

		ConexionTcp conexion = new ConexionTcp(
				nodo.ip.getHostAddress(),
				nodo.puerto_m,
				localIp,
				0
		);

		Mensaje pedido = new Mensaje(estaDireccion, Codigos.MANTENIMIENTO_GET_REPORTE_NODOS, null);

		Mensaje respuesta = (Mensaje) conexion.enviarConRta(pedido);

		return (String) respuesta.getCarga();
	}


	private void cls(){
		for(int i=0; i<50; i++){
			System.out.println("");
		}
	}

	public static void main(String[] args) {
		EjecutableMantenimiento consultor;
		String reporte;

		List<String> direcciones = Arrays.asList(
				"127.0.0.1",
				"127.0.0.2",
				"127.0.0.3",
				"127.0.0.4",
				"127.0.0.5",
				"127.0.0.6",
				"127.0.0.7",
				"127.0.0.8",
				"127.0.0.9"
		);

		for (String ip : direcciones) {
			consultor = new EjecutableMantenimiento();

			try {
				reporte = consultor.consultarNodo(
						new DireccionNodo(
								InetAddress.getByName(ip)
						)
				);
			} catch (UnknownHostException e) {
				//e.printStackTrace();
				reporte = "";
			} catch (IOException e) {
				//e.printStackTrace();
				reporte = "";
			}

			System.out.println(reporte);
		}
	}

}
