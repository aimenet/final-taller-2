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
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.filechooser.FileNameExtensionFilter;


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
	
	public static void main(String[] args) throws IOException {
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
	
}
