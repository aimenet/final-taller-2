package commons.mensajes.wkan_nc;

import commons.DireccionNodo;
import commons.mensajes.Mensaje;

import java.util.ArrayList;

/**
 * Mensaje mediante el cual un WKAN informa a un NC una serie de vecinos (otros NCs) a los que puede conectarse
 *
 * */
public class InformeNcsVecinos extends Mensaje {

    public InformeNcsVecinos(DireccionNodo emisor, Integer codigo, ArrayList<DireccionNodo> vecinos) {
        super(emisor, codigo, vecinos);
    }

    public ArrayList<DireccionNodo> getVecinos() {
        return (ArrayList<DireccionNodo>) getCarga();
    }
}
