package domain;

public class SNREquation {

    public double multiplier;
    public double constant;
    

    public SNREquation(double multiplier, double constant) {
        this.multiplier = multiplier;
        this.constant = constant;
      
        System.out.println("SNRequation multiplier :" + multiplier + "; constant :" + constant);
    }
   


    public double getSNR(int power) {
        return multiplier * power + constant;
    }
   
}
