package nodes.components.corethreads;

import nodes.components.servidores.Consultor;
import nodes.components.servidores.ConsultorNA_NA;
import nodes.components.servidores.Servidor;


public class ServerCoreThread extends CoreThread {
    private String ip;
    private int port;
    private String serverName;
    private Class<? extends Consultor> serverClass;


    public ServerCoreThread (String workerName, String ip, int port, String serverName, Class<? extends Consultor> serverClass) {
        super(workerName);
        this.ip = ip;
        this.port = port;
        this.serverName = serverName;
        this.serverClass = serverClass;
    }

    @Override
    protected void createNewWorker() {
        this.worker = new Servidor(ip, port, serverName, serverClass);
    }

}
