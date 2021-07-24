package nodes.components.corethreads;

import nodes.components.WorkerNH_Interno;


public class WorkerNHInternoCoreThread extends CoreThread {
    protected int id;

    public WorkerNHInternoCoreThread(String workerName, int id) {
        super(workerName);
        this.id = id;
    }

    @Override
    protected void createNewWorker() {
        this.worker = new WorkerNH_Interno(this.id);
    }

}
