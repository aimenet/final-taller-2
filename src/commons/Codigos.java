package commons;

public class Codigos {
	// Códigos específicos -> formarto: origen_destino_tipo(http style)_motivo

	// Usados entre NCs
	public static final int NC_NC_POST_CONSULTA = 30;
	public static final int NC_NC_POST_SALUDO = 31;
	
	// Usados entre WKANs
	public static final int NA_NA_POST_SALUDO = 40;
	public static final int NA_NA_POST_ANUNCIO_ACTIVOS = 41;
	public static final int NA_NA_POST_RETRANSMISION_ANUNCIO_NC = 42;
	public static final int NA_NA_POST_SOLICITUD_VECINOS_NC = 43;
	
	// Usados entre NCs y WKANs
	public static final int NC_NA_POST_ANUNCIO = 60;
	public static final int NA_NC_POST_ANUNCIO_ACEPTADO = 61;
	public static final int NA_NC_POST_NC_VECINO = 62;
	public static final int NC_NA_POST_KEEPALIVE = 63;
	
	// Usados entre Hs y WKANs
	public static final int NH_NA_POST_SOLICITUD_NCS = 70;
	
	
	// Generales
	public static final int OK = 200;
	public static final int ACCEPTED = 202;
	public static final int DENIED = 403;
	public static final int CONNECTION_END = 600;
	// 418 es "I'm a teapot" en http
}
