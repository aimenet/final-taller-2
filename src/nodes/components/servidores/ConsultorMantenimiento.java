package nodes.components.servidores;

import commons.Codigos;
import commons.DireccionNodo;
import commons.mensajes.Mensaje;
import commons.structs.ReporteNodosConocidos;
import nodes.components.atributos.Atributos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;


/**
 * Consultor general que se implemente en todos los Nodos que necesiten recibir consultas relacionadas a debuggeo.
 *
 * Es independiente del tipo de Nodo
 */
public class ConsultorMantenimiento implements Consultor {
    // Nótese que uso la interfaz como clase, porque los métodos ahí declarados son los únicos que me interesan
    private final Atributos atributos;
    private ObjectInputStream buffEntrada;
    private ObjectOutputStream buffSalida;
    private Socket sock;

    // Métodos del Consultor_Mantenimiento
    // -----------------------------------------------------------------------------------------------------------------
    public ConsultorMantenimiento(Atributos atributos) {
        this.atributos = atributos;
    }


    // Métodos para atención de consultas
    // -----------------------------------------------------------------------------------------------------------------
    private void pedidoReporteNodos() {
        /* Solicitud de todos los Nodos de la red, conocidos por este Nodo */
        Mensaje respuesta;
        ReporteNodosConocidos reporte;

        // Datos para el reporte
        ArrayList<DireccionNodo> wkans = this.atributos.getWkans();
        ArrayList<DireccionNodo> ncs = this.atributos.getNcs();
        ArrayList<DireccionNodo> nhs = this.atributos.getNhs();

        // Armado del reporte
        reporte = new ReporteNodosConocidos(wkans, ncs, nhs);

        respuesta = new Mensaje(
                this.atributos.getDireccion(),
                Codigos.MANTENIMIENTO_GET_REPORTE_NODOS,
                reporte.getReporte(this.atributos.getDireccion())
        );

        try {
            buffSalida.writeObject(respuesta);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Métodos de la interfaz Consultor
    // -----------------------------------------------------------------------------------------------------------------
    @Override
    public void atender() {
        Mensaje mensaje;
        boolean terminar = false;

        try {
            // Instanciación de los manejadores del buffer.
            buffSalida = new ObjectOutputStream(sock.getOutputStream());
            buffEntrada = new ObjectInputStream(sock.getInputStream());

            // Bucle principal de atención al cliente (no espera cierre de conexión del cliente)
            while(!terminar){
                mensaje = (Mensaje) buffEntrada.readObject();

                switch(mensaje.getCodigo()){
                    case Codigos.MANTENIMIENTO_GET_REPORTE_NODOS:
						this.pedidoReporteNodos();
                        terminar = true;
                        break;
                    default:
                        System.out.printf("\tRecibido mensaje desconocido en %s: %s\n", sockToString(), mensaje.getCarga());
                        break;
                }
            }

            sock.close();
        } catch (IOException | ClassNotFoundException e) {
            //IOException -> buffer de salida (se cae el Cliente y el Servidor espera la recepción de un mensaje).
            //ClassNotFoundException -> buffer de entrada.
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        this.atender();
    }

    @Override
    public void setSock(Socket sock) {
        this.sock = sock;
    }

    @Override
    public String sockToString() {
        return String.format("%s:%d", sock.getInetAddress().getHostAddress(), sock.getPort());
    }
}
