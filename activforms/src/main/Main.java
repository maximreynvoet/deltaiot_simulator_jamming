package main;

import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.client.SimulationClient;
import deltaiot.services.QoS;
import domain.Link;
import domain.Mote;
import mapek.FeedbackLoop;
import mapek.SNREquation;
import simulator.Simulator;
import util.ConfigLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    Probe probe;
    Effector effector;
    Simulator simulator;

    public static void main(String[] args) {
        String strMode;
        String strStartJamCycle;
        String strStopJamCycle ;
        String strPowerJam;
        String strJammerX_Pos;
        String strJammerY_Pos;
        String strRandActivePercentage;
        String strRandSpreadPercentage;
        String strMitigation;
        String strRssiConfirmation;
        String strDetectionMethod;
        if (args.length == 3) {
            strMode = "standard";
            ConfigLoader.getInstance().setProperty("mode", strMode);
            ConfigLoader.getInstance().setProperty("runId", args[0]);
            ConfigLoader.getInstance().setProperty("detectionMethod", args[2]);
        }
        else {
            ConfigLoader.getInstance().setProperty("runId", args[0]);
            strMode = args[1].substring(1); // options  are -constantjam, -drift, -jamAndDrift , -reactivejam , -randomjam
            ConfigLoader.getInstance().setProperty("mode", strMode);
        }
        switch(strMode) {
            case "standard" :
               
                ConfigLoader.getInstance().setProperty("startJamCycle", "0");
                ConfigLoader.getInstance().setProperty("stopJamCycle", "0");
                ConfigLoader.getInstance().setProperty("powerJam", "0");
                ConfigLoader.getInstance().setProperty("startDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("stopDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("maxDriftRatio", "0");
                ConfigLoader.getInstance().setProperty("mitigation", "false");
                ConfigLoader.getInstance().setProperty("rssiConfirmation", "false");
                break;

            case "constantjam" :
                // System.out.println("starting attack mode: next params should be start and stop cycle and power of jam");
                strStartJamCycle = args[2];
                strStopJamCycle = args[3];
                strPowerJam = args[4];
                strJammerX_Pos = args[5];
                strJammerY_Pos = args[6];
                strMitigation = args[7];
                strRssiConfirmation = args[8];
                strDetectionMethod = args[9];
                System.out.println ("start jam cycle:" + strStartJamCycle + "; stop jam cycle:" + strStopJamCycle + "; power jam: " + strPowerJam);
                ConfigLoader.getInstance().setProperty("startJamCycle", strStartJamCycle);
                ConfigLoader.getInstance().setProperty("stopJamCycle", strStopJamCycle);
                ConfigLoader.getInstance().setProperty("powerJam", strPowerJam);
                ConfigLoader.getInstance().setProperty("startDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("stopDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("maxDriftRatio", "0");
                ConfigLoader.getInstance().setProperty("pos_X_jammer", strJammerX_Pos);
                ConfigLoader.getInstance().setProperty("pos_Y_jammer", strJammerY_Pos);
                ConfigLoader.getInstance().setProperty("mitigation", strMitigation);
                ConfigLoader.getInstance().setProperty("rssiConfirmation", strRssiConfirmation);
                ConfigLoader.getInstance().setProperty("detectionMethod", strDetectionMethod);
                break;
            case "reactivejam" :
                // System.out.println("starting attack mode: next params should be start and stop cycle and power of jam");
                strStartJamCycle = args[2];
                strStopJamCycle = args[3];
                strPowerJam = args[4];
                strJammerX_Pos = args[5];
                strJammerY_Pos = args[6];
                strMitigation = args[7];
                strRssiConfirmation = args[8];
                strDetectionMethod = args[9];
                System.out.println ("start jam cycle:" + strStartJamCycle + "; stop jam cycle:" + strStopJamCycle + "; power jam: " + strPowerJam);
                ConfigLoader.getInstance().setProperty("startJamCycle", strStartJamCycle);
                ConfigLoader.getInstance().setProperty("stopJamCycle", strStopJamCycle);
                ConfigLoader.getInstance().setProperty("powerJam", strPowerJam);
                ConfigLoader.getInstance().setProperty("startDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("stopDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("maxDriftRatio", "0");
                ConfigLoader.getInstance().setProperty("pos_X_jammer", strJammerX_Pos);
                ConfigLoader.getInstance().setProperty("pos_Y_jammer", strJammerY_Pos);
                ConfigLoader.getInstance().setProperty("mitigation", strMitigation);
                ConfigLoader.getInstance().setProperty("rssiConfirmation", strRssiConfirmation);
                ConfigLoader.getInstance().setProperty("detectionMethod", strDetectionMethod);
            
                break;
            case "randomjam" :
                // System.out.println("starting attack mode: next params should be start and stop cycle and power of jam");
                strStartJamCycle = args[2];
                strStopJamCycle = args[3];
                strPowerJam = args[4];
                strJammerX_Pos = args[5];
                strJammerY_Pos = args[6];
                strRandActivePercentage = args[7]; // the % of the time the random jammer is active
                strRandSpreadPercentage = args[8]; // the % indicating spreading % of jammer active; e.g. 5 means 20 blocks (each blocks has jam & sleep period within)
                strMitigation = args[9];
                strRssiConfirmation = args[10];
                strDetectionMethod = args[11];
                System.out.println ("start jam cycle:" + strStartJamCycle + "; stop jam cycle:" + strStopJamCycle + "; power jam: " + strPowerJam);
                ConfigLoader.getInstance().setProperty("startJamCycle", strStartJamCycle);
                ConfigLoader.getInstance().setProperty("stopJamCycle", strStopJamCycle);
                ConfigLoader.getInstance().setProperty("powerJam", strPowerJam);
                ConfigLoader.getInstance().setProperty("startDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("stopDriftCycle", "0");
                ConfigLoader.getInstance().setProperty("maxDriftRatio", "0");
                ConfigLoader.getInstance().setProperty("pos_X_jammer", strJammerX_Pos);
                ConfigLoader.getInstance().setProperty("pos_Y_jammer", strJammerY_Pos);
                ConfigLoader.getInstance().setProperty("randActivePercentage", strRandActivePercentage);
                ConfigLoader.getInstance().setProperty("randSpreadPercentage", strRandSpreadPercentage);
                ConfigLoader.getInstance().setProperty("mitigation", strMitigation);
                ConfigLoader.getInstance().setProperty("rssiConfirmation", strRssiConfirmation);
                ConfigLoader.getInstance().setProperty("detectionMethod", strDetectionMethod);
                break;

            case "drift":
                String strDriftStartCycle = args[2];
                String strDriftStopCycle = args[3];
                String strMaxDriftRatio = args[4];
                System.out.println ("start drift cycle:" + strDriftStartCycle + "; stop jam cycle:" + strDriftStopCycle + "; power jam: " + strMaxDriftRatio);
                ConfigLoader.getInstance().setProperty("startJamCycle", "0");
                ConfigLoader.getInstance().setProperty("stopJamCycle", "0");
                ConfigLoader.getInstance().setProperty("powerJam", "0");
                ConfigLoader.getInstance().setProperty("startDriftCycle", strDriftStartCycle);
                System.out.println("startDriftCycle:" +  strDriftStartCycle);
                ConfigLoader.getInstance().setProperty("stopDriftCycle", strDriftStopCycle);
                System.out.println("strDriftStopCycle:" +  strDriftStopCycle);
                ConfigLoader.getInstance().setProperty("maxDriftRatio", strMaxDriftRatio);
                System.out.println("strMaxDriftRatio:" +  strMaxDriftRatio);
                break;

            case "jamAndDrift" :
                System.out.println("starting attack and drift mode: next params should be start and stop cycle and power of jam, followed by param drift");
                strStartJamCycle = args[2];
                strStopJamCycle = args[3];
                strPowerJam = args[4];
                strDriftStartCycle = args[5];
                strDriftStopCycle = args[6];
                strMaxDriftRatio = args[7];
                System.out.println ("start jam cycle:" + strStartJamCycle + "; stop jam cycle:" + strStopJamCycle + "; power jam: " + strPowerJam);
                System.out.println ("and drift cycle:" + strDriftStartCycle + "; stop jam cycle:" + strDriftStopCycle + "; power jam: " + strMaxDriftRatio);
                ConfigLoader.getInstance().setProperty("startJamCycle", strStartJamCycle);
                ConfigLoader.getInstance().setProperty("stopJamCycle", strStopJamCycle);
                ConfigLoader.getInstance().setProperty("powerJam", strPowerJam);
                ConfigLoader.getInstance().setProperty("startDriftCycle", strDriftStartCycle);
                ConfigLoader.getInstance().setProperty("stopDriftCycle", strDriftStopCycle);
                ConfigLoader.getInstance().setProperty("maxDriftRatio", strMaxDriftRatio);
                break;

        }
       

        
        Main ddaptation = new Main();
        ddaptation.initializeSimulator();
        ddaptation.start();
    }

    public void start() {

        new Thread(() -> {
            // Compile the list of SNREquations for all the links in the simulator
            List<SNREquation> equations = new ArrayList<>();

            // Firstly, assemble all the links in the simulator
            List<Link> links = simulator.getMotes().stream()
                    .map(Mote::getLinks)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            // Secondly, loop over all the links, and add their SNREquations to the overall list
            for (Link link : links) {
                equations.add(new SNREquation(link.getFrom().getId(),
                        link.getTo().getId(),
                        link.getSnrEquation().multiplier,
                        link.getSnrEquation().constant));
            }

            // Start a new feedback loop
            FeedbackLoop feedbackLoop = new FeedbackLoop();
            feedbackLoop.setProbe(probe);
            feedbackLoop.setEffector(effector);
            feedbackLoop.setEquations(equations);

            // StartFeedback loop (this runs for the amount of cycles specified in the configuration)e
            feedbackLoop.start();

            // printResults();

        }).start();
    }

    void printResults() {
        // Get QoS data of previous runs
        // probe.getNetworkQoS() should not have less number than the number of times
        // feedback loop will run, e.g, feedback loop runs 5 times, this should have >=5
        List<QoS> qosList = probe.getNetworkQoS(ConfigLoader.getInstance().getAmountOfCycles());
        System.out.println("\nPacketLoss;Latency;EnergyConsumption");
        for (QoS qos : qosList) {
            System.out.println(String.format("%f;%f;%f", qos.getPacketLoss(), qos.getLatency(), qos.getEnergyConsumption()));
        }
    }

    // Initialises a new simulator and probe
    public void initializeSimulator() {
        String simulationNetwork = ConfigLoader.getInstance().getSimulationNetwork();

        // Start a completely new sim
        SimulationClient client = new SimulationClient(simulationNetwork);

        // Assign a new probe, effector and simulator to the main object.
        probe = client.getProbe();
        effector = client.getEffector();
        simulator = client.getSimulator();
    }

    public Simulator getSimulator() {
        return simulator;
    }
}
