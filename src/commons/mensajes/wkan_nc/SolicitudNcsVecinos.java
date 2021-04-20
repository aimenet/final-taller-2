package commons.mensajes.wkan_nc;

import commons.DireccionNodo;
import commons.mensajes.Mensaje;

/**
 * Mensaje mediante el que un NC solicitará vecinos a un WKAN
 *
 * */
public class SolicitudNcsVecinos extends Mensaje {

    public SolicitudNcsVecinos(DireccionNodo emisor, Integer codigo, Integer faltantes) {
        super(emisor, codigo, faltantes);
    }

    public Integer faltantes() {
        return (Integer) this.getCarga();
    }
}
