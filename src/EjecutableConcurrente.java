import java.io.IOException;

// TODO: modificarlo ahora que cambié la estructura de los nodos Hoja.
public class EjecutableConcurrente {
	
	public static void main(String[] args) {
		int clientes = 3;
		for(int i=0; i<clientes; i++){
			//Creación del hilo donde correrá el controlador automático de Hojas.
			(new Thread(new ControladorConcurrente(i))).start();
		}
	}

}

class ControladorConcurrente implements Runnable {
	private int id, hojas;
	private NodoHoja hoja;
	
	public ControladorConcurrente(int id){
		this.id = id;
		this.hojas = 15;
	}
	
	@Override
	public void run() {
		
		for(int i=0; i<this.hojas; i++){
			hoja = new NodoHoja();
			/*try {
				//	Thread.sleep(10 + id*10 + i*10);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}*/
			hoja.establecerConexionNodoCentral("127.0.0.1", 5555);
			String texto = "<Controlador " + this.id + ">  Hoja " + i + ": ID: " + hoja.getConexionConNodoCentral().getId();
			System.out.println(texto);
			/*try {
				// Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			hoja.terminarConexionNodoCentral();		
		}
	}
}