package simulator;

import deltaiot.services.QoS;
import domain.*;
import util.Distance;

import java.util.*;

public class Simulator {

    private List<Mote> motes = new ArrayList<>();
    private List<Gateway> gateways = new ArrayList<>();
    private List<Integer> turnOrder = new ArrayList<>();
    private int MaxTimeSlots;
    private RunInfo runInfo = new RunInfo();
    private List<QoS> qosValues = new ArrayList<>();
    private boolean packetDuplication;
    private ChannelConfiguration channelconfig;
    private Double activeChannelFrequency;
    private Jammer jammer;
    private ArrayList<String> lstUsedActiveChannels;

    

    // Constructor
    public Simulator() {
    }

  

    public Double getActiveChannelFrequency() {
        return activeChannelFrequency;
    }

    public void setActiveChannelFrequency(Double activeChannelFrequency) {
        this.activeChannelFrequency = activeChannelFrequency;
    }

    public ChannelConfiguration getChannelconfig() {
        return channelconfig;
    }

    public void setChannelconfig(ChannelConfiguration channelconfig) {
        this.channelconfig = channelconfig;
    }

    // we switch the config (from single to basic, or from basic to intermediate,..)
    public boolean performChannelHopping(String strConfig) {
        this.channelconfig.setChannelConfigMode(strConfig);
        
        // first get current active channel, then hop to new channel
        Double dCurrent;
        Mote mToGetChannel = getMoteWithId(2);
        if (null != mToGetChannel.getActiveChannelFrequency()) {
            dCurrent = mToGetChannel.getActiveChannelFrequency();
        } else {
            ChannelConfiguration ch = new ChannelConfiguration("SINGLE");
            // ChannelConfig.put("CH_13_868", 866.10);
            dCurrent = ch.getChannelConfig().get("CH_13_868");
        }
        // select random new channel within config and use it to switch active
        // frequencyChannel
        if (null == lstUsedActiveChannels) {
            lstUsedActiveChannels = new ArrayList<>();
            lstUsedActiveChannels.add("CH_13_868");
        }
        
        

        while (true) {
            Double dNew = 0.0;
            // first determine exclusive range , excluding lower range channels (to be
            // efficient and not to loose cycles)
            if (this.getChannelconfig().getChannelConfigMode().equals("BASIC")) {
                if (!lstUsedActiveChannels.contains("CH_12_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_12_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_12_868");
                    }
                    return false;
                }
                if (!lstUsedActiveChannels.contains("CH_14_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_14_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_14_868");
                    }
                    return true;
                } else {
                    return true;
                }

             
            }
            if (this.getChannelconfig().getChannelConfigMode().equals("INTERMEDIATE")) {
                if (!lstUsedActiveChannels.contains("CH_11_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_11_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_11_868");
                    }
                    return false;
                }
                if (!lstUsedActiveChannels.contains("CH_15_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_15_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_15_868");
                    }
                    return true;
                } else {
                    return true;
                }

             
            }
            if (this.getChannelconfig().getChannelConfigMode().equals("FULL")) {
                if (!lstUsedActiveChannels.contains("CH_10_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_10_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_10_868");
                    }
                    return false;
                }
                if (!lstUsedActiveChannels.contains("CH_16_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_16_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_16_868");
                    }
                    return false;
                }
                if (!lstUsedActiveChannels.contains("CH_17_868")) {
                    dNew = this.getChannelconfig().getChannelConfig().get("CH_17_868");
                    if (Double.compare(dNew, dCurrent) != 0) {
                        activeChannelFrequency = dNew; // simulator holds the new active channel, will be used to change channel
                        lstUsedActiveChannels.add("CH_17_868");
                    }
                    return true;
                } else {
                    return true;
                }

             
            }
          
         }

    }

    public static Simulator createBaseCase() {
        Simulator simul = new Simulator();

        // Motes
        double battery = 11880;
        int load = 10;
        Mote mote1 = new Mote(1, battery, load);
        Mote mote12 = new Mote(12, battery, load);
        Mote mote2 = new Mote(2, battery, load);
        simul.addMotes(mote1, mote12, mote2);

        // Gateways
        // I use the convention to give gateways negative ids
        // Nothing enforces this, but all ids have to be unique between all nodes (=
        // motes & gateways)
        Gateway gateway1 = new Gateway(-1);
        gateway1.setView(mote1, mote12);
        Gateway gateway2 = new Gateway(-2);
        gateway2.setView(mote2, mote12);
        simul.addGateways(gateway1, gateway2);

        // Links
        int power = 15;
        int distribution = 100;
        mote1.addLinkTo(gateway1, gateway1, power, distribution);
        mote2.addLinkTo(gateway2, gateway2, power, distribution);
        mote12.addLinkTo(mote1, gateway1, power, distribution);
        mote12.addLinkTo(mote2, gateway2, power, distribution);

        simul.setTurnOrder(mote12, mote1, mote2);

        return simul;
    }

    public static Simulator createBaseCase2() {
        Simulator simul = new Simulator();

        // Motes
        double battery = 11880;
        int load = 10;
        Mote mote0 = new Mote(0, battery, load);
        Mote mote11 = new Mote(11, battery, load);
        Mote mote12 = new Mote(12, battery, load);
        Mote mote21 = new Mote(21, battery, load);
        Mote mote22 = new Mote(22, battery, load);
        simul.addMotes(mote0, mote11, mote12, mote21, mote22);

        // Gateways
        // I use the convention to give gateways negative ids
        // Nothing enforces this, but all ids have to be unique between all nodes (=
        // motes & gateways)
        Gateway gateway1 = new Gateway(-1);
        gateway1.setView(mote11, mote12, mote0);
        Gateway gateway2 = new Gateway(-2);
        gateway2.setView(mote21, mote22, mote0);
        simul.addGateways(gateway1, gateway2);

        // Links
        int power = 15;
        int distribution = 100;
        mote0.addLinkTo(mote11, gateway1, power, distribution);
        mote0.addLinkTo(mote12, gateway1, power, distribution);
        mote0.addLinkTo(mote21, gateway2, power, distribution);
        mote0.addLinkTo(mote22, gateway2, power, distribution);

        mote11.addLinkTo(gateway1, gateway1, power, distribution);
        mote12.addLinkTo(gateway1, gateway1, power, distribution);

        mote21.addLinkTo(gateway2, gateway2, power, distribution);
        mote22.addLinkTo(gateway2, gateway2, power, distribution);

        simul.setTurnOrder(mote0, mote11, mote12, mote21, mote22);

        return simul;
    }

    // Pre-build simulators

    public boolean isPacketDuplication() {
        return packetDuplication;
    }

    public void setPacketDuplication(boolean packetDuplication) {
        this.packetDuplication = packetDuplication;
    }

    // Creation API

    public void addMotes(Mote... motes) {
        Collections.addAll(this.motes, motes);
    }

    public void addGateways(Gateway... gateways) {
        Collections.addAll(this.gateways, gateways);
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    public void addJammer(Jammer jammer) {
        this.jammer = jammer;
    }

    public Jammer getJammer() {
        return this.jammer;
    }

    /**
     * Do a single simulation run. This will simulate the sending of packets through
     * the network to the gateways. Each gateway will aggregate information about
     * packet-loss and power-consumption. To get this information, use
     * gateway.calculatePacketLoss and gateway.getPowerConsumed respectively.
     */
    public void doSingleRun() {
        // Reset the gateways aggregated values, so we can start a new window to see
        // packet loss and power consumption
        resetGatewaysAggregatedValues();

        /**
         * before giving each mode a turn, a downlink communication of the gateway
         * is foreseen as part of MAPE-K solution. If jamming is detected :
         * - channel list is extend (from SINGLE to BASIC (3 channels) or from BASIC to
         * INTERMEDIATE (5 channels))
         * - or from INTERMEDIATE to FULL (8 channels) in case jammer has adapted
         * - the channel is chosen pseudo random (// mitigating single channel jamming
         * attack), start is set excluding previous range
         */

        // select pseudo random the channelconfiguration that will be used by all Motes
        // for this turn
        // current channel not allowed
        // initialise in first run
        // next code obsolete : simul holds active channel, and in case of hopping
        // the hop has already set the new active channel frequency
        // Double dCurrent ;
        // Mote mToGetChannel = getMoteWithId(2);
        // if (null != mToGetChannel.getActiveChannelFrequency()) {
        // dCurrent = mToGetChannel.getActiveChannelFrequency();
        // } else {
        // ChannelConfiguration ch = new ChannelConfiguration("SINGLE");
        // // ChannelConfig.put("CH_13_868", 866.10);
        // dCurrent = ch.getChannelConfig().get("CH_13_868");
        // }
        // //
        Double dCurrent = getActiveChannelFrequency();
        // Do the actual run, this will give all motes a turn
        // Give each mote a turn, in the given order
        for (Integer id : turnOrder) {
            Mote mote = getMoteWithId(id);
            mote.setActiveChannelFrequency(dCurrent);
            // Let mote handle its turn
            mote.handleTurn(runInfo, MaxTimeSlots); // return value doesn't include packets send for other motes, only
            // its own packets
        }

        // if i = 1 : first run, no jamming yet => set the rssi's as would be measured
        // by emperical testing
        if (runInfo.getRunNumber() == 1) {
            HashMap<Integer, HashMap<String, Double>> rssiStore;
            if (null == this.getGateways().get(0).getRssiStore()) {
                rssiStore = new HashMap<>();
            } else {

                rssiStore = this.getGateways().get(0).getRssiStore();
            }
            for (Integer id : turnOrder) {
                Mote mote = getMoteWithId(id);
                for (domain.Link l : mote.getLinks()) {

                    Double rssilink = 35.0 - (32.0
                            * Math.log(Distance.calcDistance(l.getFrom().getPosition(), l.getTo().getPosition())));
                    String key = l.getFrom().getId() + "_" + l.getTo().getId();
                    HashMap<String, Double> hskKeyValue;
                    if (null == this.getGateways().get(0).getRssiStore().get(runInfo.getRunNumber())) {
                        hskKeyValue = new HashMap<>();
                    } else {
                        hskKeyValue = this.getGateways().get(0).getRssiStore().get(runInfo.getRunNumber());
                    }
                    hskKeyValue.put(key, rssilink);
                    rssiStore.put(runInfo.getRunNumber(), hskKeyValue);
                    this.getGateways().get(0).setRssiStore(rssiStore);

                }
            }
        
        }

        // QoS
        QoS qos = new QoS();
        qos.setEnergyConsumption(gateways.get(0).getPowerConsumed());

        List<Packet> queuePackets = new LinkedList<Packet>();
        for (Mote mote : gateways.get(0).getView()) {
            queuePackets.addAll(mote.getPacketQueue());
        }

        qos.setPacketLoss(gateways.get(0).calculatePacketLoss(runInfo, packetDuplication));
        qos.setLatency(gateways.get(0).calculateLatency(runInfo));
        int queueLoss = 0;
        for (Mote mote : gateways.get(0).getView()) {
            queueLoss += mote.getQueueLoss();
            mote.resetQueueLoss();
        }

        qos.setQueueLoss(queueLoss / (double) gateways.get(0).getExpectedPackets(runInfo));
        qos.setSent((double) gateways.get(0).getExpectedPackets(runInfo));
        qos.setPeriod("" + runInfo.getRunNumber());
        qosValues.add(qos);

        // Increase run number
        runInfo.incrementRunNumber();
    }

    private void resetGatewaysAggregatedValues() {
        // Reset gateways' packetstore and expected packet count, so the packetloss for
        // this run can be calculated easily
        // Also reset the consumed power, so this is correctly aggregated for this run
        for (Gateway gateway : gateways) {
            gateway.resetPacketStore();
            int queuedPackets = 0;
            for (Mote mote : gateways.get(0).getView()) {
                queuedPackets += mote.getPacketQueue().size();
            }
            // gateway.setQueuedPacketsToExpectedPackets();
            // gateway.setExpectedPackets(queuedPackets);
            // gateway.resetPacketStoreAndExpectedPacketCount();
            // gateway.resetQueueLoss();
            // gateway.resetQueuePackets();
            gateway.resetPowerConsumed();
        }
    }

    // Simulation API

    public Mote getMoteWithId(int id) {
        for (Mote mote : motes) {
            if (mote.getId() == id)
                return mote;
        }
        return null;
    }

    public Gateway getGatewayWithId(int id) {
        for (Gateway gw : gateways) {
            if (gw.getId() == id)
                return gw;
        }
        return null;
    }

    // Alteration and inspection API

    public List<Integer> getTurnOrder() {
        return Collections.unmodifiableList(turnOrder);
    }

    public void setTurnOrder(Mote... motes) {
        Integer[] ids = new Integer[motes.length];
        for (int i = 0; i < motes.length; ++i) {
            ids[i] = motes[i].getId();
        }
        setTurnOrder(ids);
    }

    public void setTurnOrder(Integer... ids) {
        this.turnOrder = Arrays.asList(ids);
    }

    

    /**
     * Gets the Node with a specified id if one exists This can be both a Mote or a
     * Gateway
     *
     * @param id The id
     * @return The node with the given id (either a mote or gateway) if one exists
     *         (null otherwise)
     */
    public Node getNodeWithId(int id) {
        Mote mote = getMoteWithId(id);
        if (mote == null) {
            Gateway gw = getGatewayWithId(id);
            return gw;
        } else
            return mote;
    }

    public List<Gateway> getGateways() {
        return Collections.unmodifiableList(gateways);
    }

    public List<Mote> getMotes() {
        return Collections.unmodifiableList(motes);
    }

    public RunInfo getRunInfo() {
        return runInfo;
    }

    public List<QoS> getQosValues() {
        return qosValues;
    }

    public int getMaxTimeSlots() {
        return MaxTimeSlots;
    }

    public void setMaxTimeSlots(int maxTimeSlots) {
        MaxTimeSlots = maxTimeSlots;
    }

    
}
