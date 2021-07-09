package commons;
/**
 * Clase que representa una imagen. Almacena (valga la redundancia) la imagen en tamaño original, 
 * su vista previa, el vector caraterístico que la representa y el vector característico comprimido.
 * 
 */

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import procesamientoimagenes.histograma.Calculator;
import procesamientoimagenes.histograma.Histograma;

public class Imagen implements Serializable {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private byte[] imagen;
	private byte[] vistaPrevia;
	private Double[] vectorCaracteristico;
	private Double[] vecCarComprimido;
	private Integer ancho;
	private Integer alto;
	private String nombre;
	private String ubicacion;

	/*---------*/
	/* Métodos */
	/*---------*/

	//Constructores.
	public Imagen(File archivo){
		BufferedImage tmpImagen;
		BufferedImage tmpVistaPrevia;
		
		try {
			tmpImagen = ImageIO.read(archivo);
			ancho = tmpImagen.getWidth();
			alto = tmpImagen.getHeight();
			
			//
			int newW = 128;
			int newH = 128;
			Image tmp = tmpImagen.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
			tmpVistaPrevia = new BufferedImage(newW, newH, BufferedImage.TYPE_3BYTE_BGR);
		    Graphics2D g2d = tmpVistaPrevia.createGraphics();
		    g2d.drawImage(tmp, 0, 0, null);
		    g2d.dispose();
		    
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ImageIO.write(tmpImagen, FilenameUtils.getExtension(archivo.getAbsolutePath()), bos);
			imagen = bos.toByteArray();
			bos = new ByteArrayOutputStream();
			ImageIO.write(tmpVistaPrevia, "jpg", bos);
			vistaPrevia = bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		this.generarVectorCar();
		this.generarVectorCarComp();

		nombre = archivo.getName();
	}

	private void generarVectorCar(){
		procesamientoimagenes.imagenes.Image tmpImage = new procesamientoimagenes.imagenes.Image(
				this.getBufferedImage()
		);
		Calculator calculator = new Calculator();
		Histograma histograma = calculator.parallelCalculation(tmpImage);
		histograma.normalizar(tmpImage.pixels);
		this.vectorCaracteristico = histograma.toDoubleArray();
	}

	// TODO: corregir: hardcodeado, mover lógica a otro lado, paralelizar
	private void generarVectorCarComp(){
		int bins = 12;
		Double[] histo = new Double[bins];
		int largoVectorCaracteristico = this.vectorCaracteristico.length;
		int valoresPorBin = largoVectorCaracteristico / bins;

		Arrays.fill(histo, (double) 0);

		for(int i=0; i<largoVectorCaracteristico; i++){
	        Double contados = this.vectorCaracteristico[i];
			int index = i / valoresPorBin;

			histo[index] += contados;
	    }

	    this.vecCarComprimido = histo;
	}

	public double compararVectorCar(Double[] otroVector){
		return Calculos.euclideanDistance(this.vectorCaracteristico, otroVector);
	}

	public double compararVectorCarComp(Double[] otroVector){
		return Calculos.euclideanDistance(this.vecCarComprimido, otroVector);
	}


	//Getters
	private BufferedImage getBufferedImage() {
		BufferedImage imagen = null;
		ByteArrayInputStream bis;
		
		bis = new ByteArrayInputStream(this.imagen);
		
	    try { imagen = ImageIO.read(bis); } 
		catch (IOException e) { e.printStackTrace(); }
	    
	    return imagen;
	}
	
	// TODO: definir si los necesito
	//public BufferedImage getImagen() { return this.getBufferedImage(true); }
	//public BufferedImage getVistaPrevia() { return this.getBufferedImage(false); }
	public byte[] getImagen() { return this.imagen; }
	public byte[] getVistaPrevia() { return this.vistaPrevia; }
	
	public Double[] getVectorCaracteristico() {return this.vectorCaracteristico;}
	public Double[] getVecCarComprimido() {return this.vecCarComprimido;}
	public String getNombre() {return this.nombre;}
	public String getUbicacion() {return this.ubicacion;}

	//Setters
	//public void setImagen(BufferedImage imagen) {this.imagen = imagen;}
	//public void setVistaPrevia(BufferedImage vistaPrevia) {this.vistaPrevia = vistaPrevia;}
	public void setVectorCaracteristico(Double[] vectorCaracteristico){
		this.vectorCaracteristico = vectorCaracteristico;
	}
	public void setVecCarComprimido(Double[] vecCarComprimido) {this.vecCarComprimido = vecCarComprimido;}

	// Misc (temporal, después borrarlo)	
	public void ver() {
		//Imagen objeto = new Imagen("/home/rodrigo/Escritorio/2CE.jpg");
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new FlowLayout());
		//frame.getContentPane().add(new JLabel(new ImageIcon(objeto.getImagen())));
		//frame.getContentPane().add(new JLabel(new ImageIcon(objeto.getVistaPrevia())));
		frame.getContentPane().add(new JLabel(new ImageIcon(this.getImagen())));
		frame.getContentPane().add(new JLabel(new ImageIcon(this.getVistaPrevia())));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.println("Script terminado");

	}

}
