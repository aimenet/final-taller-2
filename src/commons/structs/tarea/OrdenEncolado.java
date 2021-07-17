package commons.structs.tarea;

import commons.DireccionNodo;
import commons.Tarea;

import java.util.ArrayList;


/**
 * TODO: agregar descripción
 *
 * @since 2021-07-09
 */

public class OrdenEncolado {
    public String cola;
    public Tarea tarea;


    public OrdenEncolado(String cola, Tarea tarea){
        this.cola = cola;
        this.tarea = tarea;
    }
}

