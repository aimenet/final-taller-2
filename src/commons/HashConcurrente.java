package commons;

import java.util.HashMap;

// TODO 2020-11-03 -> el nombre de la clase es cualquier cosa, ponerle uno que represente que el hash usa dos tipos de
//  datos fijos (DireccionNodo y CredImagen) -> o hacer que sea de tipos variables (como hice con Tupla)

/**
 * Clase que permite utilizar un HashMap<DireccionNodo,CredImagen[]> entre hilos sin problemas de concurrencia.
 * Se usa para almacenar las respuestas de aquellas Hojas que posean imágenes similares a la query.
 * La sincronización se garantiza a nivel de instancia.
 *
 * @author rodrigo
 *
 */
public class HashConcurrente {
    private HashMap<DireccionNodo, CredImagen[]> hash;
    private final Object lock;

    public HashConcurrente() {
        hash = new HashMap<DireccionNodo, CredImagen[]>();
        lock = new Object();
    }

    public void agregar(DireccionNodo clave, CredImagen[] valor){
        synchronized (lock) {
            hash.put(clave, valor);
        }
    }

    public HashMap<DireccionNodo, CredImagen[]> getHash(){
        // No hace falta que esté sincronizado en realidad
        synchronized (lock) {
            return hash;
        }
    }

    public void mergear(HashMap<DireccionNodo, CredImagen[]> hashNuevo){
        synchronized (lock) {
            for(DireccionNodo clave : hashNuevo.keySet()){
                hash.put(clave, hashNuevo.get(clave));
            }
        }
    }
}