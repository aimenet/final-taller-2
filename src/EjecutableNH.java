import nodes.NodoHoja;

import java.net.UnknownHostException;

public class EjecutableNH {

	public static void main(String[] args) throws InterruptedException {
		String configFile;
		
		/* El único parámetro que recibe es el path (absoluto) del archivo de configuración */
		if(args.length == 0){
			System.out.println("Faltan parámetros");
			System.exit(1);
		}
		
		configFile = args[0];
		
		//NodoHoja hoja = new NodoHoja(ipServidor,puertoServidor,ipNodoCentral,puertoNodoCentral);
		NodoHoja hoja = null;
		try {
			hoja = new NodoHoja(configFile);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		hoja.ponerEnMarcha();
		
		System.out.println("\nFin script.");
	}

}
