package procesamientoimagenes.histograma;

import procesamientoimagenes.utils.ConcurrentDoubleArray;

public class Histograma {
    private ConcurrentDoubleArray histogram;

    public Histograma(Integer size) {
        this.histogram = new ConcurrentDoubleArray(size, (double) 0.0);
    }

    public void incrementar(Integer index, Double amount) {
        this.histogram.increment(index, amount);
    }

    public void normalizar(Float valor) {
        for(int i=0; i<this.histogram.getSize(); i++){
            this.histogram.set(i, this.histogram.get(i) / valor);
        }
    }

    public void normalizar(Integer valor) {
        for(int i=0; i<this.histogram.getSize(); i++){
            this.histogram.set(i, this.histogram.get(i) / valor);
        }
    }

    public Double[] toDoubleArray() {
        return  histogram.toDoubleArray();
    }
}
