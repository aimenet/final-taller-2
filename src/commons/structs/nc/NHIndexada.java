package commons.structs.nc;

import commons.DireccionNodo;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa un NH indexado en un NC posterior al anuncio
 *
 * */

public class NHIndexada {
	private DireccionNodo direccion;
	private UUID uuid;


	public NHIndexada(DireccionNodo direccion) {
		this.direccion = direccion;
	}

	public NHIndexada(UUID id, DireccionNodo direccion) {
		this.direccion = direccion;
		this.uuid = id;
	}


	public DireccionNodo getDireccion() {return this.direccion;}
	public UUID getUuid() {return this.uuid;}

	public void setUuid(UUID uuid) {this.uuid = uuid;}
}
