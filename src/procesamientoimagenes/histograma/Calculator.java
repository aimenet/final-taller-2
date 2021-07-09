package procesamientoimagenes.histograma;

import procesamientoimagenes.imagenes.Image;

public class Calculator {
    private Integer cores;

    public Calculator() {
        cores = Runtime.getRuntime().availableProcessors();

        if (cores == null || cores <= 0)
            cores = 1;
    }

    private Task[] defineTasks(Integer cores, Image image) {
        int pixelsPerCore = (int) Math.ceil(image.pixels * 1.0 / cores);
        int minIndex = 0;
        int maxIndex = image.pixels - 1;

        // En un entorno productivo acá se necesitaría una lógica para determinar a partir de qué tamaño de imagen
        // es conveniente paralelizar el procesamiento.

        Task[] tasks = new Task[cores];

        for (int core=0; core < cores; core++) {
            int floor = core * pixelsPerCore;
            int ceil = floor + pixelsPerCore - 1;

            floor = Math.max(floor, minIndex); // It have no sense
            ceil = Math.min(ceil, maxIndex);

            tasks[core] = new Task(
                    floor,
                    ceil,
                    image
            );
        }

        return tasks;
    }

    public Histograma parallelCalculation(Image image) {
        Integer channels = image.colorDepth * image.channels;
        Task[] tasks = defineTasks(cores, image);

        Histograma histogram = new Histograma(channels);
        Thread[] threads = new Thread[this.cores];

        for (int i=0; i < tasks.length; i++) {
            Task task = tasks[i];

            Thread thread = new Thread(
                    new Worker(task, histogram)
            );
            thread.setName("Worker-" + i);
            thread.start();
            threads[i] = thread;
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return histogram;
    }
}
