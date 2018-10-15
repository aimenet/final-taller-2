import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Clase que agrupa un conjunto de métodos miscelaneos que resultan de utilidad para otras clases
 * pero no pertenecen a estas de forma directa.
 * @author rodrigo
 *
 */

public class Utilidades {
	// Atributos
	//===========
	private final String[] IMAGE_EXTENSIONS = new String[]{"bmp","jpg","jpeg","png"};
	
	// Métodos
	//=========
	/*Método que dado un directorio devuelve un arreglo con todas las imágenes (BufferedImage)
	 * en él presentes.*/
	public String[] imagenesDelDirectorio(String path){
		String[] salida = null;
		
		File directorio = new File(path);

	    FilenameFilter IMAGE_FILTER = new FilenameFilter() {
	        @Override
	        public boolean accept(final File directorio, final String name) {
	            for (String ext : IMAGE_EXTENSIONS) {
	                if (name.endsWith("." + ext)) {
	                    return (true);
	                }
	            }
	            return (false);
	        }
	    };
	    if (directorio.isDirectory()) {
	    	System.out.println(directorio.listFiles(IMAGE_FILTER).length);
	    	salida = new String[directorio.listFiles(IMAGE_FILTER).length];
            int indice = 0;
	    	for (final File f : directorio.listFiles(IMAGE_FILTER)) {
                BufferedImage img = null;
                //System.out.println(f.getAbsolutePath());
                try {
                    img = ImageIO.read(f);
                    img = null;
                    //System.out.println("image: " + f.getName());
                    salida[indice] = f.getName();
                } catch (final IOException e) {
                    // handle errors here
                	salida[indice] = null;
                }
                indice++;
            }
        }
	    return salida;
	}

}
