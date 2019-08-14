/**
 * Clase que uso para hacer pruebas rápidas
 * 
 * @author rodrigo
 *
 */

import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import commons.Imagen;


public class standalone {

	public static void splitTest() {
		System.out.println("Hasta acá vamos bien");
		
		String nombre = "palabra-1.palabra-2";
		String[] palabras = nombre.split("\\.");
		System.out.println(nombre);
		System.out.println(palabras.length);
		
		for(int i=0; i<palabras.length; i++) {
			System.out.println(palabras[i]);
		}
		
		String ip = "127.0.0.1:8080";
		String[] partes = ip.split(":");
		System.out.println(ip);
		System.out.println(partes.length);
		
		for(int i=0; i<partes.length; i++) {
			System.out.println(partes[i]);
		}
		
	}
	
	public static void byteArray2Bufferedimage() throws IOException {
		File archivo = null;
		

		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("bmp","jpg","jpeg","png");
	    fileChooser.setFileFilter(filter);
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.setMultiSelectionEnabled(false);
	    
	    int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    archivo = fileChooser.getSelectedFile();
		} else {
			System.out.println("Oops, algo salió mal");
		}
		
		/* Método 1 para obtener BufferedImage a partir de un byte array */
		/* ------------------------------------------------------------- */
		// Esto no anda, creo que porque no se obtiene así el bytearray de una buffered image
		/*BufferedImage bimg = ImageIO.read(archivo);
		byte[] barray = ((DataBufferByte) bimg.getData().getDataBuffer()).getData();
		ByteArrayInputStream bis = new ByteArrayInputStream(barray);
		BufferedImage rimg = ImageIO.read(bis);
		
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(bimg)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);*/

		/* Método 2 */
		/* -------- */
		// Esto sí funciona
		BufferedImage bimg = ImageIO.read(archivo);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ImageIO.write(bimg, "jpg", bos );
		byte[] barray = bos.toByteArray();
		ByteArrayInputStream bis = new ByteArrayInputStream(barray);
		BufferedImage rimg = ImageIO.read(bis);
		
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(rimg)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void pruebaConClaseImagen() throws IOException {
		File archivo = null;
		Imagen img = null;
		

		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("bmp","jpg","jpeg","png");
	    fileChooser.setFileFilter(filter);
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.setMultiSelectionEnabled(false);
	    
	    int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    archivo = fileChooser.getSelectedFile();
		} else {
			System.out.println("Oops, algo salió mal");
		}
		
		img = new Imagen(archivo);
		
		ByteArrayInputStream bis = new ByteArrayInputStream(img.getVistaPrevia());
		BufferedImage rimg = ImageIO.read(bis);
		
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(rimg)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public static void arrayObjetos() throws IOException {
		Integer elObjeto = null;
		Object[] elArrayDeObjetos = new Object[3];
		
		for (int i=0; i<3; i++) {
			elObjeto = i;
			elArrayDeObjetos[i] = elObjeto;
		}
		
		for (int j=0; j<elArrayDeObjetos.length; j++) {
			System.out.println(elArrayDeObjetos[j].toString());
		}
		
		elArrayDeObjetos = new Object[3];
		elArrayDeObjetos[0] = 1;
		elArrayDeObjetos[1] = "Hola";
		elArrayDeObjetos[2] = 35;
		
	}
	
	public static void lecturaEscrituraJson() throws IOException, ParseException {
		Path path = Paths.get(System.getProperty("user.dir"),"config", "new");
		System.out.println(path.toString());
		
		/* Lectura */
		JSONParser parser = new JSONParser();
		Reader reader = new FileReader("/home/rdg/eclipse-workspace/final-taller-2/prueba.json");

		Object jsonObj = parser.parse(reader);

		JSONObject jsonObject = (JSONObject) jsonObj;

		System.out.println(jsonObject.keySet());
		
		String cadena = (String) jsonObject.get("cadena");
		long entero = (long) jsonObject.get("entero");
		Double decimal = (Double) jsonObject.get("decimal");
		JSONArray lista = (JSONArray) jsonObject.get("lista");
		
		System.out.println("Cadena = " + cadena);
		System.out.println("Entero = " + (entero + 43));
		System.out.println("Decimal = " + (decimal + 0.43));
		System.out.println("Lista = " + lista);
		System.out.println();

		
		@SuppressWarnings("unchecked")
		Iterator<String> it = lista.iterator();
		while (it.hasNext()) {
			System.out.println("Palabra = " + it.next());
		}
		reader.close();
		
		/* Escritura (en el archivo existente) */
		jsonObject.put("nueva-key", "nuevo-value");
		
		lista.add("AA");
		lista.add("BB");
		lista.add("CC");

		jsonObject.put("lista", lista);
		
		try {
			// Crea el/los directorios necesarios en caso de no existir
			File file = new File("/home/rdg/eclipse-workspace/final-taller-2/new/new2/prueba2.json");
			file.getParentFile().mkdirs();
			
			FileWriter writer = new FileWriter(file);
			writer.write(jsonObject.toJSONString());
			writer.flush();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.print(jsonObject.toJSONString());
	}
	
	public static void pruebaIoJson(String[] args) throws IOException, ParseException {
		File file = new File("/home/rdg/eclipse-workspace/final-taller-2/new/new2/blablabla2.json");
		if (!file.exists()) {
			System.out.println("Creando");
			file.getParentFile().mkdirs();
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			writer.write("{}");
			writer.flush();
			writer.close();
		}
		
		JSONParser parser = new JSONParser();
		Reader reader = new FileReader(file);
		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		reader.close();

		jsonObject.put("3", "B");
		
		FileWriter writer = new FileWriter(file);
		writer.append(jsonObject.toJSONString());
		writer.write(jsonObject.toJSONString());
		writer.flush();
		writer.close();
		
		System.out.println(jsonObject);
	}

	public static void booleans() throws IOException, ParseException {
		String[] cadenas = {"true", "True", "false", "pepe", null};
		
		for (String cadena : cadenas) {
			System.out.println("->\t" + cadena);
			if (Boolean.valueOf(cadena))
				System.out.println("Sape");
			else
				System.out.println(Boolean.getBoolean(cadena));
		}
	}
	
	
	public static void main(String[] args) throws IOException, ParseException {
		
	}
}
