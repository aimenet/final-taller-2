package commons;

public class Calculos {
    public static double euclideanDistance(Double[] arrayA, Double[] arrayB){
        double aux, distancia;

        distancia = 0;
        for(int i=0; i<arrayA.length; i++){
            aux = Math.abs(arrayA[i] - arrayB[i]);
            distancia += Math.pow(aux,2);
        }

        return Math.sqrt(distancia);
    }
}
