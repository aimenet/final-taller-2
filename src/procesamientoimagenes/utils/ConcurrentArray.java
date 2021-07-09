package procesamientoimagenes.utils;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Clase que permite utilizar un arreglo (de tipo definido en tiempo de ejecución) entre hilos sin problemas de
 * concurrencia.
 *
 * La sincronización se garantiza a nivel de instancia.
 *
 */
class ConcurrentArray<T> {
    protected T[] array;
    protected final Object[] lock;
    protected final Integer size;

    public ConcurrentArray(Class<T> clazz, Integer size, T initialValue) {
        this.size = size;

        this.lock = new Object[size];

        // Use Array native method to create array of a type only known at run time
        @SuppressWarnings("unchecked")
        final T[] a = (T[]) Array.newInstance(clazz, size);
        this.array = a;

        // Arrays initialization
        for (int i=0; i < size; i++)
            this.lock[i] = new Object();
        Arrays.fill(this.array, initialValue);
    }

    public Integer getSize() {
        return this.size;
    }
}