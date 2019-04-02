

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
		
		NodoCentral NC1 = new NodoCentral(configFile);
		NC1.ponerEnMarcha();
	}

}
