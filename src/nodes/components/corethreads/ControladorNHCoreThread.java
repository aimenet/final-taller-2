package nodes.components.corethreads;

import nodes.components.ControladorHoja;
import nodes.components.clientes.Cliente;

import java.lang.reflect.InvocationTargetException;


public class ControladorNHCoreThread extends CoreThread {

    public ControladorNHCoreThread(String workerName) {
        super(workerName);
    }

    @Override
    protected void createNewWorker() {
        this.worker = new ControladorHoja();
    }

}
