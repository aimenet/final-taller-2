package commons.mensajes.wkan_wkan;

import commons.DireccionNodo;
import commons.mensajes.Mensaje;

import java.util.ArrayList;


/**
 * Mensaje mediante el que un WKAN retransmitirá a otro la solicitud de (NCs) vecinos efectuada por un NC
 *
 * */
public class RetransmisionSolicitudNcsVecinos extends Mensaje {
    private ArrayList<DireccionNodo> wkansVisitados;
    private DireccionNodo nodoCentral;
    private Integer faltantes;
    private Integer saltos;


    public RetransmisionSolicitudNcsVecinos(
            DireccionNodo emisor,
            Integer codigo,
            DireccionNodo nodoCentral,
            Integer saltos,
            Integer faltantes,
            ArrayList<DireccionNodo> wkansVisitados
    ) {
        super(emisor, codigo, null);

        this.nodoCentral = nodoCentral;
        this.saltos = saltos;
        this.faltantes = faltantes;
        this.wkansVisitados = wkansVisitados;
    }

    public Integer getSaltos() { return this.saltos; }
    public Integer getFaltantes() { return this.faltantes; }
    public DireccionNodo getNodoCentral() { return this.nodoCentral; }
    public ArrayList<DireccionNodo> getWkansVisitados() { return this.wkansVisitados; }
}
