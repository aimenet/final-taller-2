package commons.structs.wkan;

import commons.DireccionNodo;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Parámetros ue registra un WKAN sobre un NC por él administrado
 *
 * @since 2021-04-24
 *
 * */


public class NCIndexado {
    public DireccionNodo direccion;
	public Integer hojasMax;
    public Integer hojasActivas;
    public Integer centralesMax;
    public Integer centralesActivos;
    public Boolean alive;
	public Timestamp timestamp;
	public Timestamp ultimoNncInformado;

    /**
     *
     * @param direccion
     * @param hojasMax
     * @param hojasActivas
     * @param centralesMax
     * @param centralesActivos
     * @param alive
     * @param timestamp
     * @param ultimoNncInformado
     *
     * */
    public NCIndexado(
            DireccionNodo direccion,
            Integer hojasMax,
            Integer hojasActivas,
            Integer centralesMax,
            Integer centralesActivos,
            Boolean alive,
            Timestamp timestamp,
            Timestamp ultimoNncInformado
    ) {
        this.direccion = direccion;
        this.hojasMax = hojasMax;
        this.hojasActivas = hojasActivas;
        this.centralesMax = centralesMax;
        this.centralesActivos = centralesActivos;
        this.alive = alive;
        this.timestamp = timestamp;
        this.ultimoNncInformado = ultimoNncInformado;
    }
}
