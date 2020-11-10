package commons.structs.nh;

import commons.DireccionNodo;

import java.time.Instant;
import java.util.UUID;

/**
 * Usada para registrar todos los parámetros asociados a la conexión con un NC
 *
 * @author rorro
 * @since 2020-04-11
 *
 * */


public class ParametrosConexionNC {
    public UUID idAsignado;  // el ID que el NC le asigna a la Hoja
    public DireccionNodo direccion;

    /**
     * @param direccion
     * @param idAsignado
     *
     * */
    public ParametrosConexionNC(DireccionNodo direccion, UUID idAsignado) {
        this.direccion = direccion;
        this.idAsignado = idAsignado;
    }
}
