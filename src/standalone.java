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

	public void splitTest() {
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
	
	public static void main(String[] args) {
		BufferedImage imagen = null;
		BufferedImage reconstruida = null;
		File archivo;
		

		JFileChooser fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("bmp","jpg","jpeg","png");
	    fileChooser.setFileFilter(filter);
	    fileChooser.setAcceptAllFileFilterUsed(false);
	    fileChooser.setMultiSelectionEnabled(false);
	    
	    int result = fileChooser.showOpenDialog(null);
		if (result == JFileChooser.APPROVE_OPTION) {
		    archivo = fileChooser.getSelectedFile();
	    	try { imagen = ImageIO.read(archivo); }
	    	catch (IOException e) { e.printStackTrace(); }
		} else {
			System.out.println("Oops, algo salió mal");
		}
		
		System.out.println("Si llegué acá es porque cargué la imagen");
		
		byte[] imageBytes = ((DataBufferByte) imagen.getData().getDataBuffer()).getData();
		
		System.out.println("Si llegué acá es porque obtuve el byte array");
		
	    JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(imagen)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		BufferedImage bImage;
	    ByteArrayOutputStream bos = new ByteArrayOutputStream();
	    try { ImageIO.write(imagen, "jpg", bos ); } 
	    catch (IOException e1) { e1.printStackTrace(); }
	    byte [] data = bos.toByteArray();
	    ByteArrayInputStream bis = new ByteArrayInputStream(data);
		try { reconstruida = ImageIO.read(bis); } 
		catch (IOException e) { e.printStackTrace(); }
		
		frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		frame.getContentPane().add(new JLabel(new ImageIcon(reconstruida)));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.println("Si llegué acá es porque reconstruí la imagen desde el byte array");
	}
}
