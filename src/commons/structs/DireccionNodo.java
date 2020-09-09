package commons.structs;

import commons.Constantes;

public class DireccionNodo implements Comparable<DireccionNodo>{
    public String ip;
    public Integer puerto_na;
    public Integer puerto_nc;
    public Integer puerto_nh;


    public DireccionNodo() {
        this.puerto_na = Constantes.PUERTO_NA;
        this.puerto_nc = Constantes.PUERTO_NC;
        this.puerto_nh = Constantes.PUERTO_NH;
    }

    public DireccionNodo(String ip) {
        this.ip = ip;
        this.puerto_na = Constantes.PUERTO_NA;
        this.puerto_nc = Constantes.PUERTO_NC;
        this.puerto_nh = Constantes.PUERTO_NH;
    }


    public String get(String puerto) {
        Integer port;

        switch(puerto) {
            case "acceso":
                port = puerto_na;
                break;
            case "centrales":
                port = puerto_nc;
                break;
            case "hojas":
                port = puerto_nh;
                break;
            default:
                return "--.--.--.--:--";
        }

        return String.format("%s:%d", ip, port);
    }


    /**
     * Se evalúan las direcciones IP de cada nodo, pero no los puertos definidos por cada uno
     *
     * @param otroNodo
     * @return
     */
    @Override
    public int compareTo(DireccionNodo otroNodo) {
        return otroNodo.ip.compareTo(this.ip);
    }
}
