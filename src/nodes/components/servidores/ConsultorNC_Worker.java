package nodes.components.servidores;

import commons.ConexionTcp;
import commons.CredImagen;
import commons.DireccionNodo;
import commons.HashConcurrente;
import commons.mensajes.Mensaje;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


/**
 * Worker del Nodo Central. Se encarga de retransmitir una consulta recibida, ya sea a un Nodo Hoja
 * o un Nodo Central.
 *
 * Tal vez un mejor nombre sería "Retransmisor" o algo así.
 *
 * @author rodrigo
 *
 */
public class ConsultorNC_Worker implements Runnable {
    private ArrayList<CredImagen> indexadas;
    private DireccionNodo destino,  direccionRta, emisor, origen;
    private HashConcurrente respuesta;
    private int ttl;
    private String modo;
    private String[] candidatas;
    private UUID id;
    private final CredImagen referencia;
    private final CountDownLatch doneSignal;

    // ip y puerto corresponden al destino. origen y emisor corresponden al NC que emitió la consulta
    // originalmente y al que la retransmite respectivamente


    ConsultorNC_Worker(UUID id, HashConcurrente respuestas, CredImagen referencia, ArrayList<CredImagen> indexadas,
           CountDownLatch doneSignal, DireccionNodo destino, int ttl, String modo) {
        this.id = id;
        this.respuesta = respuestas;
        this.referencia = referencia;
        this.indexadas = indexadas;
        this.doneSignal = doneSignal;

        // TTL de los mensajes utilizados en las consultas (1 para los enviados a Hojas)
        this.ttl = ttl;

        // Dirección del "socekt" servidor de la Hoja/Central a consultar
        this.destino = destino;

        // Destino de la consulta (Hoja/Central)
        this.modo = (modo == "H" || modo == "Hoja") ? "HOJA" : "CENTRAL";

        // NC que origina la consulta
        this.origen = null;

        // NC que retransmite la consulta (quien instancia al Worker)
        this.emisor = emisor;

        // Hoja que espera la respuesta
        this.direccionRta = direccionRta;
    }

    // Constructor usado para las instancias que interacturán con NC (se agrega la dirección del NC
    // que origina la consulta y la dirección a donde debe enviarse la rta)
    ConsultorNC_Worker(UUID id, HashConcurrente respuestas, CredImagen referencia, ArrayList<CredImagen> indexadas,
           CountDownLatch doneSignal, DireccionNodo destino, int ttl, String modo,
           DireccionNodo origen, DireccionNodo emisor, DireccionNodo direccionRta) {
        this.id = id;
        this.respuesta = respuestas;
        this.referencia = referencia;
        this.indexadas = indexadas;
        this.doneSignal = doneSignal;

        // TTL de los mensajes utilizados en las consultas (1 para los enviados a Hojas)
        this.ttl = ttl;

        // Dirección del "socekt" servidor de la Hoja/Central a consultar
        this.destino = destino;

        // Destino de la consulta (Hoja/Central)
        this.modo = (modo == "H" || modo == "Hoja") ? "HOJA" : "CENTRAL";

        // NC que origina la consulta
        this.origen = origen;

        // NC que retransmite la consulta (quien instancia al Worker)
        this.emisor = emisor;

        // Hoja que espera la respuesta
        this.direccionRta = direccionRta;
    }


    public void run() {
        if (modo == "HOJA") {
            consultarHoja();
        } else {
            consultarCentral();
        }

        doneSignal.countDown();
    }

    // TODO 2020-11-03: cuando lo pruebe, revisar bien el tema de los "null" que uso en el constructor del mensaje, me da mala espina
    /**
     * Método en el que se establece conexión con un Nodo Hoja (su faceta Servidor) y se consulta por
     * imágenes similares a la dada como referencia.
     */
    boolean consultarHoja() {
        ArrayList<CredImagen> candidatas;
        ConexionTcp conexion;
        Mensaje rta;

        // Paso 1: conexión con Nodo Hoja
        try {
            conexion = new ConexionTcp(destino.ip.getHostAddress(), destino.puerto_nc);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Paso 2: búsqueda de imágenes candidatas
        candidatas = new ArrayList<CredImagen>();
        for(int i=0; i < indexadas.size(); i++){
            int[] otroVector = (indexadas.get(i)).getVecCarComprimido();
            float distancia = referencia.comparacionRapida(otroVector);
            if(distancia < 10000){
                candidatas.add((indexadas.get(i)));
            }
        }

        if(candidatas.isEmpty())
            return false;

        // TODO: acá forzé el "00" porque da igual pero debería usar el ID
        // Paso 3: envío de consulta, imagen de referencia y recepción de respuestas
        conexion.enviarSinRta(new Mensaje(this.emisor,10,referencia));
        rta = (Mensaje) conexion.enviarConRta(new Mensaje(this.emisor,10,candidatas)); //rta: imgs similares

        if(rta == null){
            System.out.println("<ConsultorNC_Worker.java> Entró en fix provisorio");
            rta = new Mensaje(emisor,10,new ArrayList<CredImagen>());
        }

        // Paso 4: almacenamiento del listado de imágenes de la Hoja similares a la de referencia
        ArrayList<CredImagen> tmp = (ArrayList<CredImagen>) rta.getCarga();
        CredImagen[] arreglo = (CredImagen[]) ( (ArrayList<CredImagen>) rta.getCarga() ).toArray(new CredImagen[0]);
        if(arreglo.length != 0)
            respuesta.agregar(rta.getEmisor(), arreglo);

        conexion.cerrar();

        return true;
    }


    // TODO 2020-11-03: cuando lo pruebe, revisar bien el tema de los "null" que uso en el constructor del mensaje, me da mala espina
    /** Método en el que se establece conexión con un Nodo Central y se retransmite una consulta */
    boolean consultarCentral() {
        ArrayList<CredImagen> candidatas;
        ConexionTcp conexion;
        Mensaje rta;
        String destinoRta;

        // Funcionamiento básico: retransmite la consulta (la imagen recibida como referencia, concretamente)
        // a los Nodos Centrales indexados.
        // Se espera la respuesta de todos antes de devolver el resultado.

        // Paso 1: conexión con Nodo Central
        try {
            conexion = new ConexionTcp(destino.ip.getHostAddress(), destino.puerto_nc);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Paso 2: envío de consulta, imagen de referencia y recepción de respuestas
        rta = (Mensaje) conexion.enviarConRta(new Mensaje(
               null,
                origen,
                emisor,
                30,
                ttl,
                referencia,
                direccionRta
        )); //rta: imgs similares

        if(rta == null){
            System.out.println("<ConsultorNC_H-Worker.java> Entró en fix provisorio");
            rta = new Mensaje(null,30,new ArrayList<CredImagen>());
        }

        // Paso 3: almacenamiento del listado recibido
        System.out.println("Fijate rta que tiene y por qué no castea!");
        HashMap<DireccionNodo,CredImagen[]> similares = (HashMap<DireccionNodo,CredImagen[]>) rta.getCarga();
        if(similares.keySet().size() != 0)
            respuesta.mergear(similares);

        conexion.cerrar();

        return true;
    }
} // Fin clase