package commons.structs.nh;

import java.sql.Timestamp;
import java.time.Instant;

/** 
 * Usada para registrar todos los parámetros necesarios cuando se realiza una solicitud de NCs a un WKAN
 * 
 * */

public class SolicitudNCs {	
	private Instant lastRequest;
	private Double firstDelay;
	private Double lastDelay;
	private Double maxDelay;


	public SolicitudNCs() {
		this.lastRequest = null;
		this.firstDelay = 2.0;
		this.lastDelay = 2.0;
		this.maxDelay = 256.0;

		// Para no olvidarme:
		// long timeElapsed = Duration.between(start, finish).toMillis();
		// Instant.now();
	}


	// Getters
	public Double getLastDelay() {return  lastDelay;}
	public Instant getLastRequest() {return lastRequest;}


	// Setters
	/**
	 * Aumenta el delay para obtener así un tiempo de espera similar ciertos protocolos de red
	 * a fin de no saturar al receptor (WKAN en este caso).
	 *
	 * La espera en este caso es lineal, esperando el doble del retardo anterior (y = 2x) hasta alcanzar el
	 * máximo, momento en el cual se vuelve al retardo inicial.
	 *
	 * 2 -> 4 -> 8 -> 16 -> 32 -> 64 -> 128 -> 256 -> 2 -> 4 -> ...
	 * 2 + 4 + 8 + 16 + 32 + 64 + 128 + 256 = 510 segundos = 8.5 minutos
	 *
	 * @since 2020-04-19
	 */
	public double setNextDelay() {
		if (lastDelay >= maxDelay)
			lastDelay = firstDelay;
		else
			lastDelay = lastDelay * 2.0;

		return lastDelay;
	}

	public Instant setLastRequestNow() {
		this.lastRequest = Instant.now();

		return lastRequest;
	}
}
