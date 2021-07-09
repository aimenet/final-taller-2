package procesamientoimagenes.utils;

public class ConcurrentDoubleArray extends ConcurrentArray{
    public ConcurrentDoubleArray(Integer size, Object initialValue) {
        super(Double.class, size, initialValue);
    }

    public double get(Integer index) {
        synchronized (this.lock[index]) {
            return (double) this.array[index];
        }
    }

    public void increment(Integer index, Double amount) {
        synchronized (this.lock[index]) {
            this.array[index] = (double) this.array[index] + amount;
        }
    }

    public void set(Integer index, double value){
        synchronized (this.lock[index]) {
            this.array[index] = value;
        }
    }

    public Double[] toDoubleArray() {
        return (Double[]) array;
    }
}
