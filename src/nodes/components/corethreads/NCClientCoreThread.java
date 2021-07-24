package nodes.components.corethreads;

import nodes.components.clientes.Cliente;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class NCClientCoreThread extends ClientCoreThread {
    private String queueName;


    public NCClientCoreThread(String workerName, int id, String queueName, Class<? extends Cliente> clientClass) {
        super(workerName, id, clientClass);
        this.queueName = queueName;
    }

    @Override
    protected void createNewWorker()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<? extends Cliente> constructor = clientClass.getDeclaredConstructor(int.class, String.class);
        this.worker = constructor.newInstance(this.id, this.queueName);
    }

}
