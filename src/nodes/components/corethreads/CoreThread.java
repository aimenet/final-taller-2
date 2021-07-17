package nodes.components.corethreads;

import java.lang.reflect.InvocationTargetException;

public abstract class CoreThread {
    public Runnable worker;  // TODO: creo que este puede ser private
    public String workerName;
    public Thread thread;

    public CoreThread(String workerName) {
        this.workerName = workerName;
    }

    protected void createNewThread() throws NullPointerException {
        if (this.worker == null)
            throw new NullPointerException();

        this.thread = new Thread(this.worker);
    }

    abstract void createNewWorker()
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;

    public void startThread()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        createNewWorker();
        createNewThread();
        thread.start();
    }

}
