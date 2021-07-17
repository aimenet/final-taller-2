package commons.structs.tarea;

import commons.Tarea;

public class OrdenEncoladoPeriodico extends OrdenEncolado {
    public Integer segundosDelay;


    public OrdenEncoladoPeriodico(String cola, Tarea tarea, Integer segundosDelay){
        super(cola, tarea);
        this.segundosDelay = segundosDelay;
    }
}
