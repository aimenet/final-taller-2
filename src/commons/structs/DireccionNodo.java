package commons.structs;

import commons.Constantes;

import java.net.InetAddress;

public class DireccionNodo implements Comparable<DireccionNodo>{
    public InetAddress ip;
    public Integer puerto_na;
    public Integer puerto_nc;
    public Integer puerto_nh;


    public DireccionNodo() {
        this.puerto_na = Constantes.PUERTO_NA;
        this.puerto_nc = Constantes.PUERTO_NC;
        this.puerto_nh = Constantes.PUERTO_NH;
    }

    public DireccionNodo(InetAddress ip) {
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
                return ip.getHostName();
        }

        return String.format("%s:%d", ip.getHostName(), port);
    }



    @Override
    public int compareTo(DireccionNodo otroNodo) {
        return otroNodo.ip.getHostName().compareTo(this.ip.getHostName());
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DireccionNodo otroNodo = (DireccionNodo) obj;

        return this.compareTo(otroNodo) == 0;
    }


    @Override
    public int hashCode() {
        String ipSinPuntos = this.ip.getHostName().replace(".", "");

        return Integer.parseInt(ipSinPuntos);
    }

}
