package nodes;


// * Tengo que traer y unificar lo que hacen los "ponerEnMarcha" de cada uno de los Nodos
// * Tengo que crear un método que sea "setearTareasIniciales" que serían todas esas tareas que los Nodos tienen que
//   hacer sí o sí cuando arrancar (anunciarse a un WKAN por ejemplo)
// * Tengo que pensar si los wkanIniciales deben estar en esta clase o no. En ppio diría que no, que eso es algo
//   particular de una topología pero no de un Nodo en particular


import commons.Constantes;
import commons.Tarea;
import commons.structs.tarea.OrdenEncolado;
import commons.structs.tarea.OrdenEncoladoPeriodico;
import nodes.components.WorkerTareasPeriodicas;
import nodes.components.atributos.Atributos;
import nodes.components.atributos.AtributosAcceso;
import nodes.components.corethreads.CoreThread;
import nodes.components.corethreads.PeriodicTasksCoreThread;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;


public class Nodo {
    private ArrayList<CoreThread> clients;
    private ArrayList<CoreThread> periodics;
    private ArrayList<CoreThread> servers;
    private ArrayList<CoreThread> specials;
    private ArrayList<OrdenEncolado> tareasIniciales;
    private ArrayList<OrdenEncoladoPeriodico> tareasPeriodicas;
    protected Atributos atributos;


    public Nodo(Atributos atributos) {
        tareasIniciales = new ArrayList<>();
        tareasPeriodicas = new ArrayList<>();
        this.atributos = atributos;
        clients = new ArrayList<>();
        periodics = new ArrayList<>();
        servers = new ArrayList<>();
        specials = new ArrayList<>();
    }

    protected void addClientCoreThread(CoreThread coreThread) {
        this.clients.add(coreThread);
    }

    protected void addInitialTask(String cola, Tarea tarea) {
        this.tareasIniciales.add(
                new OrdenEncolado(cola, tarea)
        );
    }

    protected void addPeriodicTask(String cola, Tarea tarea, Integer segundosDelay) {
        this.tareasPeriodicas.add(
                new OrdenEncoladoPeriodico(cola, tarea, segundosDelay)
        );
    }

    protected void addServerCoreThread(CoreThread coreThread) {
        this.servers.add(coreThread);
    }

    private void ejecutarTareasIniciales() throws InterruptedException {
        for (OrdenEncolado encolar : tareasIniciales) {
            atributos.encolar(encolar.cola, encolar.tarea);
            System.out.println("[Core] Disparada tarea inicial: " + encolar.tarea.getName());
        }
    }

    private void setWorkerTareasPeriodicas() {
        if (tareasPeriodicas.size() > 0) {
            periodics.add(
                new PeriodicTasksCoreThread(
                        "Worker Tareas Periódicas",
                        this.tareasPeriodicas,
                        this.atributos
                )
            );
        }
    }

    private void checkThreadsStatus(ArrayList<CoreThread> coreThreads)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for (CoreThread coreThread : coreThreads) {
            if (!coreThread.thread.isAlive()) {
                coreThread.startThread();
            }
        }
    }

    private void checkAllThreadsStatus()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        checkThreadsStatus(servers);
        checkThreadsStatus(clients);
        checkThreadsStatus(specials);
        checkThreadsStatus(periodics);
    }

    private void startAllCoreThreads()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        startCoreThreads(servers);
        startCoreThreads(clients);
        startCoreThreads(specials);
        startCoreThreads(periodics);
    }

    private void startCoreThreads (ArrayList<CoreThread> coreThreads)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for (CoreThread coreThread : coreThreads) {
            coreThread.startThread();
            System.out.printf("Core thread %s has been started", coreThread.workerName);
        }
    }

    public void iniciar()
            throws InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException,
            IllegalAccessException {
        setWorkerTareasPeriodicas();

        startAllCoreThreads();

        ejecutarTareasIniciales();

        System.out.println("Completada puesta en marcha del Nodo, listo para operar");

        while (true) {
            checkAllThreadsStatus();
            Thread.sleep(Constantes.DELAY_MILISEGUNDOS_CONTROL_THREADS);
        }
    }
}
