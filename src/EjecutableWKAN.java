import nodes.NodoAccesoBienConocido;;import java.net.UnknownHostException;

public class EjecutableWKAN {

	public static void main(String[] args) throws UnknownHostException {
		String configFile;
		
		/* El único parámetro que recibe es el path (absoluto) del archivo de configuración */
		if(args.length == 0){
			System.out.println("Faltan parámetros");
			System.exit(1);
		}
		
		configFile = args[0];
		
		NodoAccesoBienConocido NA1 = new NodoAccesoBienConocido(configFile);
		NA1.ponerEnMarcha();
	}

}
