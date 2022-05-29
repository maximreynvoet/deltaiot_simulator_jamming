package domain;
// import domain.Packet;
// import domain.Link;

public class PacketLinkCombination {

    private Packet packet ;
    private Link link;
    private int creationCycle; //cycle the packet was created and the destination link was determined
    private String key;

    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public int getCreationCycle() {
        return creationCycle;
    }
    public void setCreationCycle(int creationCycle) {
        this.creationCycle = creationCycle;
    }
    public Packet getPacket() {
        return packet;
    }
    public void setPacket(Packet packet) {
        this.packet = packet;
    }
    public Link getLink() {
        return link;
    }
    public void setLink(Link link) {
        this.link = link;
    }
    
    public PacketLinkCombination(Packet packet, Link link, int pCreationCycle) {
        this.packet = packet;
        this.link = link;
        this.creationCycle = pCreationCycle;
        this.key = creationCycle + "_" + packet.getId() + "_" + link.getFrom().getId() + "_" + link.getTo().getId();
    }
   
    
}
