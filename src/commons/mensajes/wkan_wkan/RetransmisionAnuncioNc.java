package commons.mensajes.wkan_wkan;

import commons.DireccionNodo;
import commons.mensajes.Mensaje;

import java.util.ArrayList;


/**
 * Mensaje mediante el que un WKAN retransmitirá a otro el anuncio de un NC recién ingresado a la red
 *
 * */
public class RetransmisionAnuncioNc extends Mensaje {
    private ArrayList<DireccionNodo> wkansVisitados;
    private DireccionNodo nodoCentral;
    private Integer saltos;


    public RetransmisionAnuncioNc(
            DireccionNodo emisor,
            Integer codigo,
            DireccionNodo nodoCentral,
            Integer saltos,
            ArrayList<DireccionNodo> wkansVisitados
    ) {
        super(emisor, codigo, null);

        this.nodoCentral = nodoCentral;
        this.saltos = saltos;
        this.wkansVisitados = wkansVisitados;
    }

    public Integer getSaltos() { return this.saltos; }
    public DireccionNodo getNodoCentral() { return this.nodoCentral; }
    public ArrayList<DireccionNodo> getWkansVisitados() { return this.wkansVisitados; }
}
