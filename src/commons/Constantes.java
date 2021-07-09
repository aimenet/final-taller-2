package commons;

/** Constantes del sistema */
public class Constantes {
	public static final int PUERTO_NA = 10101;
	public static final int PUERTO_NC = 10102;
	public static final int PUERTO_NH = 10103;
	public static final int PUERTO_MANTENIMIENTO = 10104;

	public static final String COLA_NA = "acceso";
	public static final String COLA_NC = "centrales";
	public static final String COLA_NH = "hojas";
	public static final String COLA_SALIDA = "salida";
	public static final String COLA_INTERNA = "interna";

	// Nombres de tareas
	public static final String TSK_NC_CHECK_VECINOS = "TSK_NC_CHECK_VECINOS";
	public static final String TSK_NA_CONECTAR_NCS = "CONECTAR-NCS";
	public static final String TSK_NC_ANUNCIO_VECINO = "ANUNCIO-VECINO";
	public static final String TSK_NA_RETRANSMITIR_SOLICITUD_VECINOS_NC = "RETRANSMITIR_SOLICITUD_VECINOS_NC";

	// Propios de WKANs
	public static final Integer ESPERA_ENTRE_INFORME_DE_NCS_VECINOS = 60;  // segundos

	// Propios de comparación de imágenes
	public static final double DST_EUC_UMBRAL_COMPARACION = 0.3;
	public static final double DST_EUC_UMBRAL_COMPARACION_RAPIDA = 1.0;
}
