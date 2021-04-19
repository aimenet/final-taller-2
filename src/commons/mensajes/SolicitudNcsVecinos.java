package commons.mensajes;

import commons.DireccionNodo;

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
