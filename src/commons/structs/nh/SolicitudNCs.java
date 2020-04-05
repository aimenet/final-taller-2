package commons.structs.nh;

import java.sql.Timestamp;
import java.time.Instant;

/** 
 * Usada para registrar todos los parámetros necesarios cuando se realiza una solicitud de NCs a un WKAN
 * 
 * */

public class SolicitudNCs {	
	public Instant lastRequest;
	public Double lastDelay;
	public Double maxDelay;
	
	/**
	 * @param lastDelay in seconds
	 * @param maxDelay in seconds
	 * 
	 * */
	public SolicitudNCs(Instant last, Double lastDelay, Double maxDelay) {
		this.lastRequest = last;
		this.lastDelay = lastDelay;
		this.maxDelay = maxDelay; 
		
		// long timeElapsed = Duration.between(start, finish).toMillis();
	}
}
