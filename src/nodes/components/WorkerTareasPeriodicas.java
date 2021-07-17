package nodes.components;

import commons.structs.tarea.OrdenEncoladoPeriodico;
import nodes.components.atributos.Atributos;

import java.util.ArrayList;


public class WorkerTareasPeriodicas implements Runnable {
    private ArrayList<OrdenEncoladoPeriodico> tareasPeriodicas;
    private Atributos atributos;

    public WorkerTareasPeriodicas(ArrayList<OrdenEncoladoPeriodico> tareasPeriodicas, Atributos atributos) {
        this.tareasPeriodicas = tareasPeriodicas;
        this.atributos = atributos;
    }

    private void ejecutarTareasPeriódicas() throws InterruptedException {
        for (OrdenEncoladoPeriodico orden : tareasPeriodicas) {
            Integer delayMilisegundos = orden.segundosDelay != null ? orden.segundosDelay * 1000 : 0;

            Thread.sleep(delayMilisegundos);
            atributos.encolar(orden.cola, orden.tarea);

            System.out.println("[Worker Periódico] Disparada tarea periódica: " + orden.tarea.getName());
        }
    }

    private void controlFlujoTareasPeriodicas() throws InterruptedException {
        boolean terminar = false;

        while(!terminar) ejecutarTareasPeriódicas();
    }

    public void run() {
        try {
            controlFlujoTareasPeriodicas();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
