package procesamientoimagenes.imagenes;


import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;


public class Image {
    public BufferedImage bufferedImage;
    public Integer channels;
    public Integer colorDepth;
    public Integer columns;
    public Integer height;
    public Integer pixels;
    public Integer rows;
    public Integer width;


    public Image(BufferedImage bufferedImage) {
        this.bufferedImage = bufferedImage;
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        columns = width;
        rows = height;
        pixels = rows * columns;

        ColorModel colorModel = bufferedImage.getColorModel();
        ColorSpace colorSpace = colorModel.getColorSpace();

        channels = colorSpace.getNumComponents();

        // TODO: revisar porque esto es COMPLETAMENTE dependiente de "bufferedImage.getRGB(x, y)" (en línea 42)
        colorDepth = 256;
    }

    public Integer getRGB(int x, int y) {
        return bufferedImage.getRGB(x, y);
    }
}
