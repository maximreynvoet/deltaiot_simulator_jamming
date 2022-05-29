package domain;

import java.util.*;

// import javax.naming.LinkLoopException;

public class Gateway extends Node {

    private List<Mote> view;
    private HashMap<Integer, List<Packet>> packetStore = new HashMap<Integer, List<Packet>>();
    private HashMap<Integer, List<Packet>> expectedPacketStore = new HashMap<Integer, List<Packet>>(); // list expected packages
    private HashMap<Integer, List<Packet>> lostPacketStore = new HashMap<Integer, List<Packet>>(); // list expected packages
    private HashMap<Integer, List<LostPacketLink>> lostPacketStoreLink = new HashMap<Integer, List<LostPacketLink>>(); // list lost packages
    private HashMap<Integer, List<Packet>> queueLostPacketsStore = new HashMap<Integer, List<Packet>>();
    private HashMap<Integer, List<String>> lostPacketLinkStore = new HashMap<Integer,List<String>>();
    private HashMap<Integer, List<String>> lostQueueLinkStore = new HashMap<Integer,List<String>>();
    private HashMap<Integer, HashMap<String, Double>> rssiStore = new HashMap<Integer, HashMap<String, Double >>();
  

    public HashMap<Integer, HashMap<String, Double>> getRssiStore() {
        return rssiStore;
    }

    public void setRssiStore(HashMap<Integer, HashMap<String, Double>> rssiStore) {
        this.rssiStore = rssiStore;
    }

    public HashMap<Integer, List<String>> getLostPacketLinkStore() {
        return lostPacketLinkStore;
    }

    public void setLostPacketLinkStore(HashMap<Integer, List<String>> lostPacketLinkStore) {
        this.lostPacketLinkStore = lostPacketLinkStore;
    }


    private HashMap<Integer, List<String>> generatedPacketLinkStore = new HashMap<Integer,List<String>>();
    public HashMap<Integer, List<String>> getGeneratedPacketLinkStore() {
        return generatedPacketLinkStore;
    }

    public void setGeneratedPacketLinkStore(HashMap<Integer, List<String>> generatedPacketLinkStore) {
        this.generatedPacketLinkStore = generatedPacketLinkStore;
    }

    
    private long prevFramePackets;
    // private List<Packet> lostPackets = new ArrayList<>();
    // private List<Packet> queueLoss = new ArrayList<>();
    // private List<Packet> queuedPackets = new ArrayList<>();
    private HashMap<Integer, Integer> expectedPacketCount = new HashMap<>();
    private double powerConsumed = 0;
    private double packetLoss;

    public Gateway(int id) {
        this(id, null);
    }

    public Gateway(int id, Position position) {
        super(id, position);
    }

    public List<Mote> getView() {
        return Collections.unmodifiableList(view);
    }

    public void setView(Mote... motes) {
        this.view = Arrays.asList(motes);
    }

    @Override
    void receivePacket(Packet packet, RunInfo runInfo) {
        List<Packet> list;
        if (!packetStore.containsKey(packet.getStartingRun())) {
            list = new LinkedList<Packet>();
            packetStore.put(packet.getStartingRun(), list);
        } else {
            list = packetStore.get(packet.getStartingRun());
        }

        if (!list.contains(packet)) {
            list.add(packet);
            if (packet.getStartingRun() == runInfo.getRunNumber() - 1)
                prevFramePackets++;
        }
        packetStore.put(packet.getStartingRun(),list);
    }

    // store the expected packages, allowing to see where they are actually blocked (by a jammer)
    void storeExpectedPacket(Packet packet, RunInfo runInfo) {
        List<Packet> list;
        if (!expectedPacketStore.containsKey(packet.getStartingRun())) {
            list = new LinkedList<Packet>();
            expectedPacketStore.put(packet.getStartingRun(), list);
        } else {
            list = expectedPacketStore.get(packet.getStartingRun());
        }

        if (!list.contains(packet)) {
            list.add(packet);
            
        }
        expectedPacketStore.put(packet.getStartingRun(),list);
    }
    // store the rssi's
    
   
    void storeRssiLink(String key, Double value, int cycle) {
       
        if (!rssiStore.containsKey(cycle)) {
            HashMap<String, Double> rssilink= new HashMap<>();
            rssilink.put(key, value);
            rssiStore.put(cycle, rssilink);
        } else{
            HashMap <Integer, HashMap<String,Double>> rssiInfo  = getRssiStore();
            HashMap<String, Double> hashMap = rssiInfo.get(cycle);
            hashMap.put(key, value);
            rssiInfo.put(cycle, hashMap);
            setRssiStore(rssiInfo);

        }

         
    }

     // store the generated packages, allowing to see where they are actually blocked (by a jammer)
     void storeGeneratedPacketLinkStore(PacketLinkCombination packetLink, RunInfo runInfo) {
        List<String> list;
        if (!generatedPacketLinkStore.containsKey(runInfo.getRunNumber())) {
            list = new LinkedList<String>();
            generatedPacketLinkStore.put(runInfo.getRunNumber(), list);
        } else {
            list = generatedPacketLinkStore.get(runInfo.getRunNumber());
        }
        // packetlink combination for cycle always new, so add to list
        // if (!list.contains(packet)) {
        //     list.add(packet);
            
        // }
        list.add(packetLink.getKey() + "_" + packetLink.getPacket().getStartingRun());
        generatedPacketLinkStore.put(runInfo.getRunNumber(),list);
    }



    void reportQueueLoss(Packet packet) {
        List<Packet> list;
        if (!queueLostPacketsStore.containsKey(packet.getStartingRun())) {
            list = new LinkedList<Packet>();
            queueLostPacketsStore.put(packet.getStartingRun(), list);
        } else {
            list = queueLostPacketsStore.get(packet.getStartingRun());
        }

        if (!list.contains(packet)) {
            list.add(packet);
        }
        queueLostPacketsStore.put(packet.getStartingRun(),list);
    }

    void addPacketToExpect(RunInfo runInfo) {
        int packets = 1;
        if (expectedPacketCount.containsKey(runInfo.getRunNumber())) {
            packets = expectedPacketCount.get(runInfo.getRunNumber());
            packets++;
        }
        expectedPacketCount.put(runInfo.getRunNumber(), packets);
    }

    public void resetPacketStore() {

        // lostPackets.clear();
    }

    public void setExpectedPackets(RunInfo runInfo, int packets) {
        for (int i = 0; i < packets; i++)
            // expectedPacketCount = packets;//countQueuedPackets();
            addPacketToExpect(runInfo);
    }

    public int getExpectedPackets(RunInfo runInfo) {
        return expectedPacketCount.get(runInfo.getRunNumber());
    }

    public List<LostPacketLink> getlostPacketStoreLink(int i) {
        return lostPacketStoreLink.get(i);  // this is linkinfo of lost packets for runnumber "i" 
    }


    public void reportPacketLost(Packet packet) {
        // if (!lostPackets.contains(packet))
        // lostPackets.add(packet);
    }
    public List<Packet> getLostPackedStore(int i) {
        return lostPacketStore.get(i);  // this is linkinfo of lost packets for runnumber "i" 
    }


    // store the expected packages, allowing to see where they are actually blocked (by a jammer)
    void storeLostPacket(Packet packet, RunInfo runInfo) {
        List<Packet> list;
        if (!lostPacketStore.containsKey(packet.getStartingRun())) {
            list = new LinkedList<Packet>();
            lostPacketStore.put(packet.getStartingRun(), list);
        } else {
            list = lostPacketStore.get(packet.getStartingRun());
        }

        if (!list.contains(packet)) {
            list.add(packet);
            
        }
        lostPacketStore.put(runInfo.getRunNumber(),list);
    }

     // store the generated packages, allowing to see where they are actually blocked (by a jammer)
     void storeLostPacketLinkStore(PacketLinkCombination packetLink, RunInfo runInfo) {
        List<String> list;
        if (!lostPacketLinkStore.containsKey(runInfo.getRunNumber())) {
            list = new LinkedList<String>();
            lostPacketLinkStore.put(runInfo.getRunNumber(), list);
        } else {
            list = lostPacketLinkStore.get(runInfo.getRunNumber());
        }
        // packetlink combination for cycle always new, so add to list of lost packages if called to store
        try {
            if (null != packetLink.getKey()) {
                // if (0 != packetLink.getPacket().getStartingRun()) {
            list.add(packetLink.getKey()+ "_" + packetLink.getPacket().getStartingRun());
            lostPacketLinkStore.put(runInfo.getRunNumber(),list);
               // }
           // } else {
           //     System.out.println("not expected");
          }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
        }

    }

       // store the generated packages, allowing to see where they are actually blocked (by a jammer)
       void storeLostQueueLinkStore(PacketLinkCombination packetLink, RunInfo runInfo) {
        List<String> list;
        if (!lostQueueLinkStore.containsKey(runInfo.getRunNumber())) {
            list = new LinkedList<String>();
            lostQueueLinkStore.put(runInfo.getRunNumber(), list);
        } else {
            list = lostQueueLinkStore.get(runInfo.getRunNumber());
        }
        // packetlink combination for cycle always new, so add to list of lost packages if called to store
        try {
            if (null != packetLink.getKey()) {
                if (0 != packetLink.getPacket().getStartingRun()) {
            list.add(packetLink.getKey()+ "_" + packetLink.getPacket().getStartingRun());
            lostQueueLinkStore.put(runInfo.getRunNumber(),list);
                }
            } else {
                System.out.println("not expected");
            }
        } catch (Exception e) {
            System.out.println("Exception " + e.getMessage());
        }

    }

    //    // store the expected packages, allowing to see where they are actually blocked (by a jammer)
    //    void storeLostPacketLink(Link packetLink, RunInfo runInfo) {
    //     LostPacketLink lpl = new LostPacketLink(packetLink.getFrom(), packetLink.getTo(), packetLink.getSNR(), runInfo.getRunNumber());
    //     List<LostPacketLink> list;
    //     if (!lostPacketStoreLink.containsKey(runInfo.getRunNumber())) {
    //         list = new LinkedList<LostPacketLink>();
    //         lostPacketStoreLink.put(runInfo.getRunNumber(), list);
    //     } else {
    //         list = lostPacketStoreLink.get(runInfo.getRunNumber());
    //     }

    //     if (!list.contains(lpl)) {
    //         list.add(lpl);
            
    //     }
    //     lostPacketStoreLink.put(runInfo.getRunNumber(),list);
    // }


    // public void resetQueueLoss(){
    // queueLoss.clear();
    // }
    //
    // public void resetQueuePackets(){
    // queuedPackets.clear();
    // }
    //
    // public double calculateQueueLoss(){
    // int queuedLoss = (int) queueLoss.stream().distinct().count();
    // return (double)queuedLoss / (double)expectedPacketCount;
    // }

    // public double calculateQueuedPackets(){
    // int queuedPackets = countQueuedPackets();
    // return (double)queuedPackets / (double)expectedPacketCount;
    // }

    public double calculateLatency(RunInfo runInfo) {
        // int totalTime = 0;
        // List<Packet> distinctPackets =
        // currentRunPackets.stream().distinct().collect(Collectors.toList());
        // for(Packet packet:distinctPackets){
        // totalTime += runInfo.getRunNumber() - packet.getStartingRun();
        // }
        //
        // return totalTime/(double)distinctPackets.size();
        double latency = 0;
        if (packetStore.containsKey(runInfo.getRunNumber() - 1)) {
            latency = ((double) prevFramePackets) / packetStore.get(runInfo.getRunNumber() - 1).size();
        }
        prevFramePackets = 0;
        return latency;
    }

    void reportPowerConsumed(double amount) {
        powerConsumed += amount;
    }

    // void reportQueueLoss(Packet packet){
    // queueLoss.add(packet);
    // }

    public void resetPowerConsumed() {
        powerConsumed = 0;
    }

    // public int countQueuedPackets(){
    // int queuedPacketsCount = (int) queuedPackets.stream().distinct().count();
    // return queuedPacketsCount;
    // }
    //
    // public void reportQueuedPacket(Packet packet){
    // queuedPackets.add(packet);
    // }

    public double calculatePacketLoss(RunInfo runInfo, boolean packetDuplication) {
        if (packetDuplication) {
            int period = runInfo.getRunNumber() - 1;
            if (packetStore.containsKey(period)) {
                if (queueLostPacketsStore.containsKey(period)) {
                    int queueLost = (int) queueLostPacketsStore.get(period).stream().distinct().count();
                    List<Packet> packets = new LinkedList<>();
                    packets.addAll(queueLostPacketsStore.get(period));
                    packets.addAll(packetStore.get(period));
                    packetLoss = 1 - (((double) packets.stream().distinct().count()))
                            / (expectedPacketCount.get(period) - queueLost);
                } else {
                    packetLoss = 1 - (((double) packetStore.get(period).stream().distinct().count()))
                            / expectedPacketCount.get(period);
                }
            }
        } else {
            packetLoss = (double) Link.lostPackets / Link.sentPackets;
            Link.lostPackets = 0;
            Link.sentPackets = 0;
        }

        return packetLoss * 100;

    }

    public double getPowerConsumed() {
        return powerConsumed;
    }

    // Debugging helpers
    public void printInfoPacketLoss(int queuedPackets) {
        // long packetStoreSizeWithoutDuplicates =
        // currentRunPackets.stream().distinct().count();

        // System.out.println("GW" + getId() + " packetloss: 1 - "
        // + packetStoreSizeWithoutDuplicates + "/" + expectedPacketCount + " =
        // " + packetLoss);
    }

    public void printInfoPacketStore() {
        // System.out.println("GW" + getId() + " PacketStore: ");
        // for (Packet packet: packetStore) {
        // System.out.println("\tnumber " + packet.getStartingRun() + " from " +
        // packet.getSource().getId() + " to " +
        // packet.getDestination().getId());
        // }
    }

    @Override
    public String toString() {
        // double packetloss = calculatePacketLoss();
        return "Gateway " + String.format("%2d", getId()) + " [storedPackets=" + packetStore.size()
                + ", expectedPackets=" + expectedPacketCount + ", packetloss="
                + String.format("%2d", Math.round(packetLoss * 100)) + ", powerConsumed="
                + String.format("%.2f", powerConsumed) + "]";
    }

    @Override
    void calculatePacketReceiveBatteryConsumption(int timeSlots) {
        // We are not calculating packet receiving battery consumption for
        // gateways
    }
}
