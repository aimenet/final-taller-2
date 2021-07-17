package nodes.components.corethreads;

import nodes.components.clientes.Cliente;
import nodes.components.servidores.Servidor;

import java.lang.reflect.InvocationTargetException;


public class ClientCoreThread extends CoreThread {
    protected int id;
    protected Class<? extends Cliente> clientClass;


    public ClientCoreThread(String workerName, int id, Class<? extends Cliente> clientClass) {
        super(workerName);
        this.id = id;
        this.clientClass = clientClass;
    }

    @Override
    protected void createNewWorker()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.worker = clientClass.getDeclaredConstructor(int.class).newInstance(this.id);
    }

}
