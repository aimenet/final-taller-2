package commons.structs;

import commons.DireccionNodo;
import java.util.ArrayList;


/**
 * Agrupa todos los datos que detallan el conocimiento que un determinado Nodo posee hasta el momento sobre la red. En
 * otras palabras, lista los demás Nodos descubietos/conocidos/conectados a un determinado Nodo.
 *
 * @since 2021-04-10
 */
public class ReporteNodosConocidos {
    // Estos van a evolucionar a clases de manera que pueda guardar información propia de cada Nodo (ver nuevoCentral()
    // en AtribustosAcceso para tener una idea de qué hablo)
    private ArrayList<DireccionNodo> wkans;
    private ArrayList<DireccionNodo> ncs;
    private ArrayList<DireccionNodo> nhs;


    // Constructores
    // -----------------------------------------------------------------------------------------------------------------
    public ReporteNodosConocidos(
            ArrayList<DireccionNodo> wkans, ArrayList<DireccionNodo>ncs, ArrayList<DireccionNodo> nhs
    ) {
        this.wkans = wkans;
        this.ncs = ncs;
        this.nhs = nhs;
    }


    // Getters
    // -----------------------------------------------------------------------------------------------------------------
    private String getStringListaNodos(ArrayList<DireccionNodo> nodos, String titulo) {
        String output;

        output = this.getHeader(titulo);

        if (nodos.size() > 0) {
            for (DireccionNodo nodo : nodos) {
                output += String.format(
                        "\n\t* %s (%d,%d,%d)",
                        nodo.ip.getHostAddress(),
                        nodo.puerto_na,
                        nodo.puerto_nc,
                        nodo.puerto_nh
                );
            }
        } else {
            output += "\n\t - Vacío";
        }

        return output;
    }

    public String getReporte(DireccionNodo nodoSolicitante) {
        String reporte;
        String header;

        if (nodoSolicitante != null) {
            header = getHeader("Datos del Nodo");
            header += String.format("\n\t* %s", nodoSolicitante.ip.getHostAddress());
            header += String.format(" (WKANs: %d", nodoSolicitante.puerto_na);
            header += String.format("\tNCs: %d", nodoSolicitante.puerto_nc);
            header += String.format("\tNHs: %d)", nodoSolicitante.puerto_nh);
        } else {
            header = "";
        }

        return String.format(
                "%s\n%s\n%s\n%s\n\n",
                header,
                getStringListaNodos(this.wkans, "WKANs"),
                getStringListaNodos(this.ncs, "NCs"),
                getStringListaNodos(this.nhs, "NHs")
        );
    }


    // Otros
    // -----------------------------------------------------------------------------------------------------------------
    private String getHeader(String titulo) {
        Integer largoLinea = 30;
        String divisor = "-";
        String header;


        String linea = divisor.repeat(largoLinea);
        Integer blancos = largoLinea - 4 - titulo.length(); // 4 = 1 "-" + " " antes del título + " " después del título + 1 "-" al final
        String tituloFormateado = String.format("- %s %s-", titulo, divisor.repeat(blancos));

        header = String.format("%s%s%s", linea, tituloFormateado, linea);

        return header;
    }
}

