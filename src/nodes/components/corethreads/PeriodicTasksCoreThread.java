package nodes.components.corethreads;

import commons.structs.tarea.OrdenEncoladoPeriodico;
import nodes.components.WorkerTareasPeriodicas;
import nodes.components.atributos.Atributos;
import nodes.components.atributos.AtributosAcceso;
import nodes.components.clientes.Cliente;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;


public class PeriodicTasksCoreThread extends CoreThread {
    protected ArrayList<OrdenEncoladoPeriodico> periodicTasks;
    protected Atributos attributes;
    protected Class<Cliente> clientClass;


    public PeriodicTasksCoreThread(String workerName, ArrayList<OrdenEncoladoPeriodico> periodicTasks,
            Atributos attributes
    ) {
        super(workerName);
        this.periodicTasks = periodicTasks;
        this.attributes = attributes;
    }

    @Override
    protected void createNewWorker()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.worker = new WorkerTareasPeriodicas(periodicTasks, attributes);
    }

}
