package util;

import domain.*;

public class Distance {
    private double distance; //distance in meters
    
    public Distance(Position A , Position B) {
        this.distance = extracted(A, B);

    }
    public double getDistance() {
        return distance;
    }
    public static Double calcDistance(Position A , Position B) {
        return extracted(A, B);

    }
    private static double extracted(Position A, Position B) {
        return Math.sqrt ((Math.pow( (A.getX() - B.getX()),2)) + ((Math.pow((A.getY() - B.getY()),2))));
    }

}
