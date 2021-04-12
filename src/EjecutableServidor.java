import nodes.NodoCentral;

import java.net.UnknownHostException;

public class EjecutableServidor {

	public static void main(String[] args) {
		Integer puertoServidorHojas, puertoServidorCentrales;
		String configFile;
		
		/* El único parámetro que recibe es el path (absoluto) del archivo de configuración */
		if(args.length == 0){
			System.out.println("Faltan parámetros");
			System.exit(1);
		}
		
		configFile = args[0];

		NodoCentral NC1 = null;
		try {
			NC1 = new NodoCentral(configFile);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		NC1.ponerEnMarcha();
	}

}
