package domain;

public class LostPacketLink {
    public Node getFrom() {
        return from;
    }
    public void setFrom(Node from) {
        this.from = from;
    }
    public Node getTo() {
        return to;
    }
    public void setTo(Node to) {
        this.to = to;
    }
    public double getSNR() {
        return SNR;
    }
    public void setSNR(double sNR) {
        SNR = sNR;
    }
    public int getRunid() {
        return runid;
    }
    public void setRunid(int runid) {
        this.runid = runid;
    }
    private Node from;
    private Node to;
    private double SNR;
    private int runid;
    public LostPacketLink(Node from, Node to, double sNR, int runid) {
        this.from = from;
        this.to = to;
        SNR = sNR;
        this.runid = runid;
    }
}
