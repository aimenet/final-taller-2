package commons.structs.nh;

import java.time.Instant;

/**
 * Usada para registrar todos los parámetros asociados a la conexión con un NC
 *
 * @author rorro
 * @since 2020-04-11
 *
 * */


public class ParametrosConexionNC {
    public Integer idAsignado;  // el ID que el NC le asigna a la Hoja
    public String direccion;

    /**
     * @param direccion
     * @param idAsignado
     *
     * */
    public ParametrosConexionNC(String direccion, Integer idAsignado) {
        this.direccion = direccion;
        this.idAsignado = idAsignado;
    }
}
