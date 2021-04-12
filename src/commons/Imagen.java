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
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Imagen implements Serializable {
	/*-----------*/
	/* Atributos */
	/*-----------*/
	private byte[] imagen;
	private byte[] vistaPrevia;
	private int[] vectorCaracteristico;
	private int[] vecCarComprimido;
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
		
		//Carga de la imagen y generación de su vista previa.
		// -> Debería arrojar excepción en vez de terminar.
		//imagen = null;
		try {
			tmpImagen = ImageIO.read(archivo);
			ancho = tmpImagen.getWidth();
			alto = tmpImagen.getHeight();
			
			//
			int newW = 128;
			int newH = 128;
			Image tmp = tmpImagen.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		    //BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
		    //Graphics2D g2d = dimg.createGraphics();
			tmpVistaPrevia = new BufferedImage(newW, newH, BufferedImage.TYPE_3BYTE_BGR);
		    Graphics2D g2d = tmpVistaPrevia.createGraphics();
		    g2d.drawImage(tmp, 0, 0, null);
		    g2d.dispose();
		    
		    // Pasaje a byte arrray
		    //imagen = ((DataBufferByte) tmpImagen.getData().getDataBuffer()).getData();
		    //vistaPrevia = ((DataBufferByte) tmpVistaPrevia.getData().getDataBuffer()).getData();
		    
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
		
		//Cálculo del vector característico de la imagen.
		this.generarVectorCar();
		this.generarVectorCarComp();
		
		//Nombre de la imagen
		nombre = archivo.getName();
	}

	// Deprecated??
	/*public Imagen(String path){
		vectorCaracteristico = new int[768];
		vecCarComprimido = new int[12];
		
		//Carga de la imagen y generación de su vista previa.

		// -> Debería arrojar excepción en vez de terminar.
		imagen = null;
		try {
			imagen = ImageIO.read(new File(path));
			//Vista previa
			int newW = 128;
			int newH = 128;
			Image tmp = imagen.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		    BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
		    Graphics2D g2d = dimg.createGraphics();
		    vistaPrevia = dimg;
		    g2d.drawImage(tmp, 0, 0, null);
		    g2d.dispose();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		//Cálculo del vector característico de la imagen.
		this.generarVectorCar();
		this.generarVectorCarComp();
		
		//Falta cargar el nombre de la imagen.
	}*/

		
	//Generación del vector característico grande (histograma)
	private void generarVectorCar(){
		BufferedImage tmpImage = null;
		int alpha, azul, pixelRGB, rojo, verde;
		int[] histo = new int[768];
		
		tmpImage = this.getBufferedImage();
		
		//Recorre la imagen, pixel a pixel.
	    for(int x=0; x<this.ancho; x++){
	        for(int y=0; y<this.alto; y++){
	        	//Obtiene el valor RGB del pixel.
	        	pixelRGB = tmpImage.getRGB(x, y);
	        	//Calcula el valor de cada canal en base al obtenido previamente.
	        	alpha = (pixelRGB>>24) & 0xff;
	        	rojo = (pixelRGB>>16) & 0xff;
	        	verde = (pixelRGB>>8 ) & 0xff;
	        	azul = (pixelRGB) & 0xff;
	        	//
	        	histo[rojo] += 1;
	        	histo[256+verde] += 1;
	        	histo[512+azul] += 1;
	        }
	    }
	    this.vectorCaracteristico = histo;
	}

	//Generación del vector característico comprimido (histograma)
	private void generarVectorCarComp(){
		BufferedImage tmpImage;
		int alpha, azul, pixelRGB, rojo, verde;
		int[] histo = new int[12];
		
		tmpImage = this.getBufferedImage();
		
		//Recorre la imagen, pixel a pixel.
	    for(int x=0; x<this.ancho; x++){
	        for(int y=0; y<this.alto; y++){
	        	//Obtiene el valor RGB del pixel.
	        	pixelRGB = tmpImage.getRGB(x, y);
	        	//Calcula el valor de cada canal en base al obtenido previamente.
	        	alpha = (pixelRGB>>24) & 0xff;
	        	rojo = (pixelRGB>>16) & 0xff;
	        	verde = (pixelRGB>>8 ) & 0xff;
	        	azul = (pixelRGB) & 0xff;
	        	//
	        	if(rojo<=63){histo[0] += 1;}
	        	else if(rojo<=127){histo[1] += 1;}
	        	else if(rojo<=191){histo[2] += 1;}
	        	else if(rojo<=255){histo[3] += 1;}
	        	if(verde<=63){histo[4] += 1;}
	        	else if(verde<=127){histo[5] += 1;}
	        	else if(verde<=191){histo[6] += 1;}
	        	else if(verde<=255){histo[7] += 1;}
	        	if(azul<=63){histo[8] += 1;}
	        	else if(azul<=127){histo[9] += 1;}
	        	else if(azul<=191){histo[10] += 1;}
	        	else if(azul<=255){histo[11] += 1;}
	        }
	    }
	    this.vecCarComprimido = histo;
	}

	//Compara el vector característico con otro recibido como parámetro.
	public float compararVectorCar(int[] otroVector){
		float aux, distancia;
		
		distancia = 0;
		for(int i=0; i<this.vectorCaracteristico.length; i++){
			aux = Math.abs(this.vectorCaracteristico[i] - otroVector[i]);
			distancia += Math.pow(aux,2);
		}
		
		return distancia;
	}
	
	//Compara el vector característico comprimido con otro recibido como parámetro.
	public float compararVectorCarComp(int[] otroVector){
		float aux, distancia;
			
		distancia = 0;
		for(int i=0; i<this.vecCarComprimido.length; i++){
			aux = Math.abs(this.vecCarComprimido[i] - otroVector[i]);
			distancia += Math.pow(aux,2);
		}
		
		return distancia;
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
	
	public int[] getVectorCaracteristico() {return this.vectorCaracteristico;}
	public int[] getVecCarComprimido() {return this.vecCarComprimido;}
	public String getNombre() {return this.nombre;}
	public String getUbicacion() {return this.ubicacion;}

	//Setters
	//public void setImagen(BufferedImage imagen) {this.imagen = imagen;}
	//public void setVistaPrevia(BufferedImage vistaPrevia) {this.vistaPrevia = vistaPrevia;}
	public void setVectorCaracteristico(int[] vectorCaracteristico){
		this.vectorCaracteristico = vectorCaracteristico;
	}
	public void setVecCarComprimido(int[] vecCarComprimido) {this.vecCarComprimido = vecCarComprimido;}

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
