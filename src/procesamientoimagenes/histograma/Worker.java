package procesamientoimagenes.histograma;

import java.util.HashMap;

class Worker implements Runnable {
    private Task task;
    private Histograma histogram;

    public Worker(Task task, Histograma histogram) {
        this.task = task;
        this.histogram = histogram;
    }

    private int processGrayscalePixel(int pixel){
        return pixel & 0xFF;
    }

    private int[] processRGBPixel(int pixel){
        final Integer redZeroIndex = 0;
        final Integer greenZeroIndex = 256;
        final Integer blueZeroIndex = 512;

        int alpha = (pixel>>24) & 0xff;
        Integer red = (pixel>>16) & 0xff;
        Integer green = (pixel>>8 ) & 0xff;
        Integer blue = (pixel) & 0xff;

        int redIndex = redZeroIndex + red;
        int greenIndex = greenZeroIndex + green;
        int blueIndex = blueZeroIndex + blue;

        int[] pixels = {redIndex, greenIndex, blueIndex};
        return pixels;
    }

    private int[] processPixelForHistogram(int x, int y) {
        // Método que "sabe" como procesar un px de una imagen de cara a registrarlo en el histograma
        int[] output;

        // Me devuelve un entero con el valor del pixel o de los pixeles, dependiedo la cantidad de
        // canales de la imagen. Debe ser convertido a un entero del ramgo 0-255 (8 bits)
        int encodedPixel = task.image.getRGB(x, y);

        if (task.image.channels == 1)
                output = new int[]{processGrayscalePixel(encodedPixel)};
        else if (task.image.channels == 3)
            output = processRGBPixel(encodedPixel);
        else
            output = new int[0];

        return output;

    }

    private void taskProcessing() {
        HashMap<Integer, Integer> partialHistogram = new HashMap<>();

        // Local calculation of partial histogram (here there is no racing condition when storing pixels count)
        for (int index=task.beginningIndex; index <= task.endingIndex; index++) {
            int y = index / task.image.columns;  // ROW
            int x = index % task.image.columns;  // COLUMN

            // Me devuelve un entero (en 8 bits) con el valor del pixel o de los pixeles, dependiedo la cantidad de
            // canales de la imagen. El valor está expresado entre 0-65535 y debe ser llevado a 0-255
            int[] pixels = processPixelForHistogram(x, y);

            for (int pixelValue : pixels) {
                if (!partialHistogram.containsKey(pixelValue)){ partialHistogram.put(pixelValue, 0); }
                partialHistogram.put(pixelValue, partialHistogram.get(pixelValue) + 1);
            }
        }

        // Pixel value storing in threads shared variable. Doing this here there is only one access per pixel,
        // reducing the number of serialized access to shared histogram
        for (Integer pixel : partialHistogram.keySet()){
            this.histogram.incrementar(
                    pixel,
                    (double) partialHistogram.get(pixel)
            );
        }

    }

    @Override
    public void run() {
        this.taskProcessing();
    }
}
