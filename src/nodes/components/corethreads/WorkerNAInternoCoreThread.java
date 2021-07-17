package nodes.components.corethreads;

import nodes.components.WorkerNA_Interno;
import nodes.components.clientes.Cliente;

import java.lang.reflect.InvocationTargetException;


public class WorkerNAInternoCoreThread extends CoreThread {
    protected int id;

    public WorkerNAInternoCoreThread(String workerName, int id) {
        super(workerName);
        this.id = id;
    }

    @Override
    protected void createNewWorker() {
        this.worker = new WorkerNA_Interno(this.id);
    }

}
