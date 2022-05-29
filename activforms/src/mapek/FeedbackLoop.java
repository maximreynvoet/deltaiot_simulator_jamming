package mapek;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.time.Duration;
import java.time.Instant;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import deltaiot.DeltaIoTSimulator;
import deltaiot.client.Effector;
import deltaiot.client.Probe;
import deltaiot.services.LinkSettings;
import deltaiot.services.QoS;
//import smc.runmodes.SMCConnector.TaskType
import domain.DoubleRange;
import domain.Jammer;
import domain.Position;
import simulator.Simulator;
import smc.runmodes.SMCConnector;
import smc.runmodes.SMCConnector.Mode;
import util.ConfigLoader;
import util.Distance;

public class FeedbackLoop {

    // Thresholds for when you want to adapt/change the network
    static final int SNR_BELOW_THRESHOLD = 0;
    static final int SNR_UPPER_THRESHOLD = 5;
    static final int ENERGY_CONSUMPTION_THRESHOLD = 5;
    static final int PACKET_LOSS_THRESHOLD = 5;
    static final int MOTES_TRAFFIC_THRESHOLD = 10;
    static final double THRESHOLD_RSSI = 0.01; // needs tuning, using SF8, according to E. Aras (see thesis 02/2021
                                              // "Security and Reliability for Emerging IoT Newtworks") delta should be
                                              // 15 dB

    // Knowledge
    private final int DISTRIBUTION_GAP = ConfigLoader.getInstance().getDistributionGap();
    // private final boolean timeInReadableFormat =
    // ConfigLoader.getInstance().timeInReadableFormat();
    // The probe and effector of the network being worked on.
    Probe probe;
    Effector effector;
    Configuration currentConfiguration;
    Configuration previousConfiguration;
    List<domain.Mote> beforeJamMotes = new LinkedList<domain.Mote>();
    int cycle;
    HashMap<Integer, List<HashMap<String, Boolean>>> mapJamDetected = new HashMap<Integer, List<HashMap<String, Boolean>>>();

    // The steps that are filled in by the planner to adjust to the newly chosen
    // best configuration
    List<PlanningStep> steps = new LinkedList<>();
    // The equations for interference on the links
    List<SNREquation> snrEquations = new LinkedList<>();
    // The current adaptation options are the options specific to the current cycle
    List<AdaptationOption> currentAdaptationOptions = new LinkedList<>();
    List<AdaptationOption> verifiedOptions = new LinkedList<>();
    SMCConnector smcConnector;
    Goals goals = Goals.getInstance();

    public FeedbackLoop() {
        Mode runmode = ConfigLoader.getInstance().getRunMode();
        smcConnector = runmode.getConnector();
    }

    public void setProbe(Probe probe) {
        this.probe = probe;
    }

    public void setEffector(Effector effector) {
        this.effector = effector;
    }

    public void setEquations(List<SNREquation> equations) {
        snrEquations = equations;
    }

    private void setCycle(int cycle) {
        this.cycle = cycle;
    }

    private int getCycle() {
        return this.cycle;
    }

    public void start() {

        // LocalDateTime now;
        // double step = 1.5;
        // double max_snr_val = 10.0;
        // double min_snr_val = 2.5;
        // int max_snr_cycle = 450;
        // Double snr_middle_window = 2.5;

        Random rand = new Random(150);

        // determine if we run with jamming, if so create jammer
        // starting the jamming attack
        Jammer jammer = null; // will be created

        // Run the mape-k loop and simulator for the specified amount of cycles
        // check the mode & create jammer if mode contains "jam"
        if (ConfigLoader.getInstance().getMode().contains("jam")) {
            // currently starting with jammer that only support jamming on BASIC channels
            // param on commandline to support multichannel (default is single channel)
            Double jammer_X_Pos = ConfigLoader.getInstance().getPosXJammer();
            Double jammer_Y_Pos = ConfigLoader.getInstance().getPosYJammer();
            jammer = new Jammer(16, 15, 10, new Position(jammer_X_Pos, jammer_Y_Pos),
                    ConfigLoader.getInstance().getMode(), ConfigLoader.getInstance().getChannelConfigMode(),
                    ConfigLoader.getInstance().getSingleChannelMode());

            for (int i = 0; i < ConfigLoader.getInstance().getAmountOfCycles(); i++) {
                // System.out.println(i + ";");
                setCycle(i);
                ConfigLoader.getInstance().setProperty("currentCycle", String.valueOf(i)); // put current cycle in
                                                                                           // singleton
                                                                                           // config

                Double epsilon1 = 0.0;
                Double epsilon2 = 0.0;
                // changed first 60 (i <= 60) to 80 on 07/05/2022
                if (i <= 80) {
                    Double epsilon = 4.0 / 800.0;
                    epsilon1 = 2.0 + (i - 1) * epsilon;
                    epsilon2 = 2.0 + i * epsilon;
                } else if (i > 80 && i <= 140) {
                    Double epsilon = 3.5 / 60.0;
                    epsilon2 = 6.0 - (i - 80 - 1) * epsilon;
                    epsilon1 = 6.0 - (i - 80) * epsilon;
                } else if (i > 140 && i <= 210) {
                    Double epsilon = 2.0 / 70.0;
                    epsilon1 = 2.5 + (i - 140 - 1) * epsilon;
                    epsilon2 = 2.5 + (i - 140) * epsilon;
                } else if (i > 210 && i <= 290) {
                    Double epsilon = 1.5 / 80.0;
                    epsilon2 = 4.5 - (i - 210 - 1) * epsilon;
                    epsilon1 = 4.5 - (i - 210) * epsilon;
                } else if (i > 290 && i <= 400) {
                    Double epsilon = 3.5 / 110.0;
                    epsilon1 = 3.0 + (i - 290 - 1) * epsilon;
                    epsilon2 = 3.0 + (i - 290) * epsilon;
                } else if (i > 400) {
                    Double epsilon = 3.0 / 100.0;
                    epsilon2 = 6.5 - (i - 400 - 1) * epsilon;
                    epsilon1 = 6.5 - (i - 400) * epsilon;
                }
                if (rand.nextInt(20) > 10) {
                    epsilon1 = 1.0;
                    epsilon2 = 4.0;
                }

                Map<Integer, Double> memory = ((DoubleRange) deltaiot.DeltaIoTSimulator.simul.getRunInfo()
                        .getGlobalInterference()).memory;
                deltaiot.DeltaIoTSimulator.simul.getRunInfo()
                        .setGlobalInterference(new DoubleRange(epsilon1, epsilon2));
                ((DoubleRange) deltaiot.DeltaIoTSimulator.simul.getRunInfo().getGlobalInterference()).memory = memory;

                // Start the monitor part of the mapek loop

                if ((i >= ConfigLoader.getInstance().getStartJamCycle())
                        && (i < ConfigLoader.getInstance().getStopJamCycle())) {

                    jam(deltaiot.DeltaIoTSimulator.simul, jammer, ConfigLoader.getInstance().getPowerJam(), i);

                }

                if (i == ConfigLoader.getInstance().getStopJamCycle()) {

                    unjam(deltaiot.DeltaIoTSimulator.simul, i);

                }

                if ((i >= ConfigLoader.getInstance().getStartDriftCycle())
                        && (i <= ConfigLoader.getInstance().getStopDriftCycle())) {

                    Double calcRatio = (double) i + 1.0 - ConfigLoader.getInstance().getStartDriftCycle();
                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = calcRatio / (ConfigLoader.getInstance().getStopDriftCycle()
                            - ConfigLoader.getInstance().getStartDriftCycle());

                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = calcRatio * (1.0 - ConfigLoader.getInstance().getMaxDriftRatio());
                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = 1.0 - calcRatio;
                    System.out.println("calcRatio : " + calcRatio);

                    deltaiot.DeltaIoTSimulator.drift(calcRatio); // drifting gradually

                }

                if (i == ConfigLoader.getInstance().getStopDriftCycle()) {
                    deltaiot.DeltaIoTSimulator.resetDrift();
                }

                // Start the monitor part of the mapek loop
                monitor();
                analysisjam(ConfigLoader.getInstance().getCurrentCycle());
            }
        } else {
            // no jamming mode

            for (int i = 0; i < ConfigLoader.getInstance().getAmountOfCycles(); i++) {
                // System.out.println(i + ";");
                setCycle(i);
                ConfigLoader.getInstance().setProperty("currentCycle", String.valueOf(i)); // put current cycle in
                                                                                           // singleton
                                                                                           // config

                Double epsilon1 = 0.0;
                Double epsilon2 = 0.0;
                if (i <= 60) {
                    Double epsilon = 4.0 / 800.0;
                    epsilon1 = 2.0 + (i - 1) * epsilon;
                    epsilon2 = 2.0 + i * epsilon;
                } else if (i > 80 && i <= 140) {
                    Double epsilon = 3.5 / 60.0;
                    epsilon2 = 6.0 - (i - 80 - 1) * epsilon;
                    epsilon1 = 6.0 - (i - 80) * epsilon;
                } else if (i > 140 && i <= 210) {
                    Double epsilon = 2.0 / 70.0;
                    epsilon1 = 2.5 + (i - 140 - 1) * epsilon;
                    epsilon2 = 2.5 + (i - 140) * epsilon;
                } else if (i > 210 && i <= 290) {
                    Double epsilon = 1.5 / 80.0;
                    epsilon2 = 4.5 - (i - 210 - 1) * epsilon;
                    epsilon1 = 4.5 - (i - 210) * epsilon;
                } else if (i > 290 && i <= 400) {
                    Double epsilon = 3.5 / 110.0;
                    epsilon1 = 3.0 + (i - 290 - 1) * epsilon;
                    epsilon2 = 3.0 + (i - 290) * epsilon;
                } else if (i > 400) {
                    Double epsilon = 3.0 / 100.0;
                    epsilon2 = 6.5 - (i - 400 - 1) * epsilon;
                    epsilon1 = 6.5 - (i - 400) * epsilon;
                }
                if (rand.nextInt(20) > 10) {
                    epsilon1 = 1.0;
                    epsilon2 = 4.0;
                }

                Map<Integer, Double> memory = ((DoubleRange) deltaiot.DeltaIoTSimulator.simul.getRunInfo()
                        .getGlobalInterference()).memory;
                deltaiot.DeltaIoTSimulator.simul.getRunInfo()
                        .setGlobalInterference(new DoubleRange(epsilon1, epsilon2));
                ((DoubleRange) deltaiot.DeltaIoTSimulator.simul.getRunInfo().getGlobalInterference()).memory = memory;

                if ((i >= ConfigLoader.getInstance().getStartDriftCycle())
                        && (i <= ConfigLoader.getInstance().getStopDriftCycle())) {

                    Double calcRatio = (double) i + 1.0 - ConfigLoader.getInstance().getStartDriftCycle();
                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = calcRatio / (ConfigLoader.getInstance().getStopDriftCycle()
                            - ConfigLoader.getInstance().getStartDriftCycle());

                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = calcRatio * (1.0 - ConfigLoader.getInstance().getMaxDriftRatio());
                    System.out.println("calcRatio : " + calcRatio);
                    calcRatio = 1.0 - calcRatio;
                    System.out.println("calcRatio : " + calcRatio);

                    deltaiot.DeltaIoTSimulator.drift(calcRatio); // drifting gradually

                }

                if (i == ConfigLoader.getInstance().getStopDriftCycle()) {
                    deltaiot.DeltaIoTSimulator.resetDrift();
                }

                monitor();

            }
        }
        // as no jamming, we have standard set , so use it to train detection algorithm
        // (after writing to output files)
        if (!ConfigLoader.getInstance().getMode().contains("jam")) {
            HashMap<String, ArrayList<Double>> packetlossratios = new HashMap<String, ArrayList<Double>>();
            printGeneratedAndLost(deltaiot.DeltaIoTSimulator.simul, packetlossratios);
            train(packetlossratios); // call the analyze function

        } else {
            // add the end, we write the lossratios for analysis
            HashMap<String, ArrayList<Double>> packetlossratios = new HashMap<String, ArrayList<Double>>();
            printGeneratedAndLost(deltaiot.DeltaIoTSimulator.simul, packetlossratios);
        }
        // finished, so retrieving link info
        // printLinkInfo(deltaiot.DeltaIoTSimulator.simul);
        // create a hashmap which will contain the keys (eg 12_7) identifying the links
        // second param is the ArrayList<Float> which will contain the packetloss over
        // the link per cycle
        // first x (e.g. 1000) will be stored as jsonarray and send to fastapi in the
        // analysis fase

    }

    // for the moment, using all cycles to train the algo
    private void train(HashMap<String, ArrayList<Double>> packetlossratios) {
        JsonObject jsonObj = new JsonObject();
        for (Entry<String, ArrayList<Double>> entry : packetlossratios.entrySet()) {

            JsonArray jsonArray1 = new Gson().toJsonTree(entry.getValue()).getAsJsonArray();
            jsonObj.add(entry.getKey(), jsonArray1);
            // this is the json object that would be used to call the fast api (per link)

        }
        // try by reading correct inputfile as send via python

        String jsonInput = jsonObj.toString().replaceAll(" ", "");

        try {
            // we will time the call to this method to compare ARIMA with DeepAnT
            Instant instantstart = Instant.now();

            String detectionMethod = ConfigLoader.getInstance().getDetectionMethod();
            URL url = new URL("http://127.0.0.1:8000/learning/" + detectionMethod + "/initiate");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Instant instantstop = Instant.now();
                Duration duration = Duration.between(instantstart, instantstop);
                System.out.println(detectionMethod + ":initiate:" + duration.getSeconds() + "," + duration.getNano());
                // System.out.println(response.toString());
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // within the run, we will already analyze the cycle and retrieve indication of
    // jam by ARIMA/DeepAnt analysis of packetlossratios
    // we confirm by using rssi values, which may have been altered due to jammer =>
    // the confirmation we use to avoid false positives
    private void analysisjam(int j) {

        HashMap<String, Double> packetlossratioOfCurrentCycle = new HashMap<String, Double>();
        String strKeys = ConfigLoader.getInstance().getInitialKeys();
        ArrayList<String> keys = new ArrayList<>(Arrays.asList(strKeys.split(";")));
        // initialize keys from config
        int i = ConfigLoader.getInstance().getCurrentCycle();
        boolean packetlost = true;
        HashMap<String, Integer> sumGeneratedPackages = new HashMap<>();
        HashMap<String, Integer> sumLostPackages = new HashMap<>();
        // initialize with keys and values 0.0
        for (String key : keys) {
            sumGeneratedPackages.put(key, 0);
            sumLostPackages.put(key, 0);
        }
        if (null != deltaiot.DeltaIoTSimulator.simul.getGateways().get(0).getGeneratedPacketLinkStore().get(i)) {

            List<String> listgenerated = deltaiot.DeltaIoTSimulator.simul.getGateways().get(0)
                    .getGeneratedPacketLinkStore().get(i);
            listgenerated.forEach((temp) -> {
                int packetcount = 0;
                String[] line = temp.split("_");
                String key = line[2] + "_" + line[3]; // eg 15_12 , representing link
                if (sumGeneratedPackages.containsKey(key)) {
                    packetcount = sumGeneratedPackages.get(key);
                    packetcount++;
                } else {
                    packetcount++;
                }
                // if (null == packetlossratios.get(key)) {
                // packetlossratios.put(key, new ArrayList<Double>());
                // }

                sumGeneratedPackages.put(key, packetcount);
                System.out.println(packetcount + ":for key " + key);

                System.out.println(temp);
            });

        } else {
            System.out.println("run " + i + ": no packets generated");
        }
      
    
        if (null != deltaiot.DeltaIoTSimulator.simul.getGateways().get(0).getLostPacketLinkStore().get(i)) {

            List<String> listlost = deltaiot.DeltaIoTSimulator.simul.getGateways().get(0).getLostPacketLinkStore()
                    .get(i);
            listlost.forEach((temp) -> {
                int lostpacketcount = 0;
                String[] line = temp.split("_");
                String key = line[2] + "_" + line[3]; // eg 15_12 , representing link
                if (sumLostPackages.containsKey(key)) {
                    lostpacketcount = sumLostPackages.get(key);
                    lostpacketcount++;
                } else {
                    lostpacketcount++;
                }

                sumLostPackages.put(key, lostpacketcount);
                System.out.println(lostpacketcount + ": lost for key " + key);
                System.out.println(temp);
            });

        } else {
            packetlost = false; // no lost packets for this cycle
        }
        for (Map.Entry<String, Integer> entry : sumGeneratedPackages.entrySet()) {
            System.out.println("Key = " + entry.getKey() +
                    ", Value = " + entry.getValue());
            if (!packetlost) {
                // all keys have a 0% packetloss ratio
                if (null != packetlossratioOfCurrentCycle.get(entry.getKey())) {
                    packetlossratioOfCurrentCycle.put(entry.getKey(), 0.0);
                } else {

                    packetlossratioOfCurrentCycle.put(entry.getKey(), 0.0);
                }

            } else {
                // get the lost packages
                int lostpackages = 0;
                if (null != sumLostPackages.get(entry.getKey())) {
                    lostpackages = sumLostPackages.get(entry.getKey());
                }
                // calculate ratio
                double packetlossratio = 0.0;
                if (entry.getValue() != 0) {
                    packetlossratio = (100.0 * (double) lostpackages) / (double) entry.getValue();
                }

                System.out.println(cycle + ":" + entry.getKey() + ":packages sent : " + entry.getValue()
                        + "; package lost : " + lostpackages + ";plr :" + packetlossratio);

                if (null != packetlossratioOfCurrentCycle.get(entry.getKey())) {
                    packetlossratioOfCurrentCycle.put(entry.getKey(), packetlossratio);
                } else {

                    packetlossratioOfCurrentCycle.put(entry.getKey(), packetlossratio);
                }
            }
        }

        ArrayList<Integer> arrResult = analyze(packetlossratioOfCurrentCycle); // call the analyze function
        // now store result in hashmap for printing at end of run
        int k = 0;
        for (String keyDetected : packetlossratioOfCurrentCycle.keySet()) {
            if (1 == arrResult.get(k)) {
                List<HashMap<String, Boolean>> list;
                if (!mapJamDetected.containsKey(j)) {
                    list = new LinkedList<HashMap<String, Boolean>>();
                    mapJamDetected.put(j, list);
                } else {
                    list = mapJamDetected.get(j);
                }
                HashMap<String, Boolean> hsh = new HashMap<>();
                hsh.put(keyDetected, true);
                list.add(hsh);
                mapJamDetected.put(j, list);
            } else {
                List<HashMap<String, Boolean>> list;
                if (!mapJamDetected.containsKey(j)) {
                    list = new LinkedList<HashMap<String, Boolean>>();
                    mapJamDetected.put(j, list);
                } else {
                    list = mapJamDetected.get(j);
                }
                HashMap<String, Boolean> hsh = new HashMap<>();
                hsh.put(keyDetected, false);
                list.add(hsh);
                mapJamDetected.put(j, list);
            }

            k++;
        }

        if (arrResult.contains(1)) {
            System.out.println(" at least one mote was jammed (packetloss & rssi confirmation).");
            if (ConfigLoader.getInstance().getMitigation()) {
                // mitigation is active
                // first we try channel hopping (most effective)
                boolean success = plan(MitigationMethod.CHANNELHOPPING);
                if (!success) {
                    System.out.println(
                            "channelhopping was already exhausted, try change distribution upstream, send info on jammed links and key info");
                    // next line : hack as long as we have no real classification logic : should be
                    // treshold on jam power as constant jammer yields high rssi

                    success = plan(MitigationMethod.CHANGE_DISTRIBUTION, arrResult, packetlossratioOfCurrentCycle);

                }
            }

        }

    }

    private boolean plan(MitigationMethod changeDistributionAndRaiseSf, ArrayList<Integer> arrResult,
            HashMap<String, Double> packetlossratioOfCurrentCycle) {
        System.out.println("starting changing distribution factor for jammed link sending motes");
        int i = 0;
        ArrayList<String> lstJammedLinks = new ArrayList<>();
        for (String keys : packetlossratioOfCurrentCycle.keySet()) {
            if (1 == arrResult.get(i)) {
                lstJammedLinks.add(keys);
            }
            i++;
        }
        // for each jammed link, we move up to source and check
        // alternative route & if source is target for other links
        for (String key : lstJammedLinks) {
            String[] link = key.split("_");
            int idSource = Integer.parseInt(link[0]);
            int idDest = Integer.parseInt(link[1]);
            // raise SF ?
            boolean blnAlternativeLink = false;
            for (domain.Mote m : DeltaIoTSimulator.simul.getMotes()) {
                for (domain.Link l : m.getLinks()) {
                    if (l.getFrom().getId() == idSource) {
                        // a link from same source, so possible path
                        if (l.getTo().getId() != idDest) {
                            // found alternative path
                            // first check if this link is jammed also
                            String strLink = l.getFrom().getId() + "_" + l.getTo().getId();
                            if (!lstJammedLinks.isEmpty()) {
                                if (lstJammedLinks.contains(strLink)) {
                                    // this link is also jammed => no action needed
                                    System.out.println("alternative link " + strLink + " also jammed");
                                } else {
                                    // alternative found => change distribution
                                    l.setDistribution(100);
                                    System.out.println("changed distribution key to 100 for " + strLink
                                            + " as altenative to jammed " + key);
                                    // need to set distribution of original jammed link to 0
                                    blnAlternativeLink = true;
                                }
                            }
                        }
                    }
                }
            }
            if (blnAlternativeLink) {
                // lookup origal link and set distri to 0
                for (domain.Mote m : DeltaIoTSimulator.simul.getMotes()) {
                    for (domain.Link l : m.getLinks()) {
                        if ((l.getFrom().getId() == idSource) && (l.getTo().getId() == idDest)) {
                            l.setDistribution(0);
                            System.out.println("setting distribution to 0 for jammed link " + l.toString());
                        }
                    }
                }
            }

        }
        return true;

    }

    // we plan the channelhopping
    private boolean plan(MitigationMethod method) {
        switch (method) {
            case CHANNELHOPPING: {
                String configMode = ConfigLoader.getInstance().getMitigationChannelConfigMode();
                if (!configMode.equals("EXHAUSTED")) {
                    // we can raise number of channels
                    System.out.println("trying channelhopping with mode " + configMode);
                    boolean exhausted = deltaiot.DeltaIoTSimulator.simul.performChannelHopping(configMode);
                    if (exhausted) {
                        ConfigLoader.getInstance().setProperty("mitigationChannelConfigMode",
                                determineNextChannelConfigMode(configMode));
                    }
                    return true;
                } else {
                    return false;
                }

            }

            default:
                throw new AssertionError();
        }

    }

    // we support 3 configmodes for mitigation (BASIC, INTERMEDIATE and FULL)
    // method cycles to next
    private String determineNextChannelConfigMode(String currentMode) {
        switch (currentMode) {
            case "BASIC": {
                return "INTERMEDIATE";

            }
            case "INTERMEDIATE": {
                return "FULL";
            }
            case "FULL": {
                return "EXHAUSTED";
            }
            default:
                throw new AssertionError();

        }

    }
    // to analyze , the data is converted to json arrays and sent to
    // fastapi-frontend of CNN DeepAnt(or ARIMA) implementation

    private ArrayList<Integer> analyze(HashMap<String, Double> packetlossratioOfCurrentCycle) {
        JsonObject jsonObj = new JsonObject();
        ArrayList<String> arrKeys = new ArrayList<String>();

        for (Entry<String, Double> entry : packetlossratioOfCurrentCycle.entrySet()) {

            Number n = (Number) entry.getValue();
            JsonPrimitive jp = new JsonPrimitive(n);
            jsonObj.add(entry.getKey(), jp);
            arrKeys.add(entry.getKey());
            // this is the json object that would be used to call the fast api (per link)

        }
        // try by reading correct inputfile as send via python

        String jsonInput = jsonObj.toString().replaceAll(" ", "");

        try {
            Instant instantStart = Instant.now();
            String detectionMethod = ConfigLoader.getInstance().getDetectionMethod();
            URL url = new URL("http://127.0.0.1:8000/learning/" + detectionMethod + "/detect");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.flush();
            }
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                Instant instantStop = Instant.now();
                Duration duration = Duration.between(instantStart, instantStop);
                System.out.println(detectionMethod + ":detect:" + duration.getSeconds() + "," + duration.getNano());
                String responseReceived = response.toString();
                JSONObject obj = new JSONObject(responseReceived);
                // Retrieve number array from JSON object.
                JSONArray arrayJS = obj.optJSONArray("anomaly_state");
                int[] flagResults = JSonArray2IntArray(arrayJS);
                ArrayList<Integer> flagResultsConfirmed = new ArrayList<Integer>();

                for (int j = 0; j < flagResults.length; j++) {
                    if (flagResults[j] == 1) {
                        if (!ConfigLoader.getInstance().getRssiConfirmation()) {
                            flagResultsConfirmed.add(1); // without rssi confirmation, it is detection algo that
                                                         // determines jamming detection
                            continue;
                        }
                        // possible jam, need to check rssi of target mote/link and compare with initial
                        // value
                        String strKeyname = arrKeys.get(j);
                        for (domain.Mote m : deltaiot.DeltaIoTSimulator.simul.getMotes()) {
                            for (domain.Link l : m.getLinks()) {
                                int indexFrom = Integer.parseInt(strKeyname.split("_")[0]);
                                int indexTo = Integer.parseInt(strKeyname.split("_")[1]);
                                if ((l.getFrom().getId() == indexFrom) && (l.getTo().getId() == indexTo)) {
                                    Double standardRssilink = 35.0 - (32.0 * Math.log(
                                            Distance.calcDistance(l.getFrom().getPosition(), l.getTo().getPosition())));
                                    Double currentRssiLink = 0.0;
                                    HashMap<String, Double> hskKeyValue;
                                    if (null == deltaiot.DeltaIoTSimulator.simul.getGateways().get(0).getRssiStore()
                                            .get(Integer.parseInt(
                                                    ConfigLoader.getInstance().getProperty("currentCycle")))) {
                                        hskKeyValue = new HashMap<>();
                                    } else {
                                        hskKeyValue = deltaiot.DeltaIoTSimulator.simul.getGateways().get(0)
                                                .getRssiStore().get(Integer.parseInt(
                                                        ConfigLoader.getInstance().getProperty("currentCycle")));
                                    }
                                    if (null == hskKeyValue.get(strKeyname)) {
                                        flagResultsConfirmed.add(0);
                                    } else {
                                        currentRssiLink = hskKeyValue.get(strKeyname);
                                        if (((currentRssiLink - standardRssilink) >= THRESHOLD_RSSI)) {
                                            flagResultsConfirmed.add(1); //

                                        } else {
                                            flagResultsConfirmed.add(0);
                                        }
                                    }

                                }
                            }
                        }
                    } else {
                        flagResultsConfirmed.add(0);
                    }

                }

                return flagResultsConfirmed;

            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
        return null;

    }
    // return false;

    public static int[] JSonArray2IntArray(JSONArray jsonArray) {
        int[] intArray = new int[jsonArray.length()];
        for (int i = 0; i < intArray.length; ++i) {
            intArray[i] = jsonArray.optInt(i);
        }
        return intArray;
    }

    private void jam(Simulator simul, Jammer jammer, Double powerJam, int cycle) {
        // int idMoteClosestToJammer = 0;
        // int idMoteSendingClosestToTarget = 0;
        // Double lowestDistance = Double.MAX_VALUE;
        // Double calcDistance = 0.0;
        // for (domain.Mote m : simul.getMotes()) {
        // calcDistance = Distance.calcDistance(m.getPosition(), jammer.getPosition());
        // if (calcDistance <= lowestDistance) {
        // lowestDistance = calcDistance;
        // idMoteClosestToJammer = m.getId();
        // }
        // }

        // make distinction in type of jam
        // first constantjammer
        // constant jammer => see Aras : Selective Jamming of LoRaWAN using Commodity Hardware
        // show that jammer with same power needs to be closer (for SF = 8, our initial SF) to receiver to be able to jam
        if (jammer.getMode().equals("constantjam")) {
            for (domain.Mote m : simul.getMotes()) {
                for (domain.Link l : m.getLinks()) {
                    Double linkDistance = Distance.calcDistance(l.getFrom().getPosition(), l.getTo().getPosition());
                    Double calcjammerToDistance = Distance.calcDistance(l.getTo().getPosition(), jammer.getPosition());
                    if (calcjammerToDistance < linkDistance) {
                        String strLinkid = l.getFrom().getId() + "_" + l.getTo().getId();
                        // we get from the gateway the original SNR equation for the link, stored during
                        // first cycle
                        domain.SNREquation eq = ConfigLoader.getInstance().getSnrEquations().get(strLinkid);
                        System.out.println(l.toString() + ": original eq " + eq.toString());
                        // change the snr using jam power
                        // we can vary via the commandline the success of the jam by modelling it's power. Could be by raising transmission power
                        // or placing constant jammer closer to receiver
                        eq.constant = eq.constant + powerJam; 
                        
                        System.out.println(l.toString() + ": new eq has constant " + eq.constant);

                        l.setSnrEquation(eq);
                        // now change rssi based on distance
                        // get hashmap with calc rssi's from first cycle, in real live, would be
                        // measured average rssi
                        HashMap<Integer, HashMap<String, Double>> rssiStore = simul.getGateways().get(0)
                                .getRssiStore();
                        // if it's the first jammed link, we get the rssi's from first run, otherwise
                        // retrieve the one stored from previous loop
                        HashMap<String, Double> hsh;
                        if (null != rssiStore.get(cycle)) {
                            hsh = rssiStore.get(cycle);
                        } else {
                            hsh = rssiStore.get(1);
                        }
                        String linkKey = l.getFrom().getId() + "_" + l.getTo().getId();
                        // now we replace standard rssi for this link with rssi perceived due to jammer
                        Double rssilinkjam = 35.0 - (32.0 * Math
                                .log(Distance.calcDistance(jammer.getPosition(), l.getTo().getPosition())));
                        System.out.println("storing rssi jammer :  " + rssilinkjam);    
                        hsh.put(linkKey, rssilinkjam);
                        rssiStore.put(cycle, hsh);
                        simul.getGateways().get(0).setRssiStore(rssiStore);

                    } else {
                        System.out.println("not changing. Distance to jammer " + calcjammerToDistance
                                + "; link distance" + linkDistance);
                        // get hashmap with calc rssi's from first cycle, in real live, would be

                        // now change rssi based on distance
                        // get hashmap with calc rssi's from first cycle, in real live, would be
                        // measured average rssi
                        HashMap<Integer, HashMap<String, Double>> rssiStore = simul.getGateways().get(0)
                                .getRssiStore();
                        // if it's the first jammed link, we get the rssi's from first run, otherwise
                        // retrieve the one stored from previous loop
                        HashMap<String, Double> hsh;
                        if (null != rssiStore.get(cycle)) {
                            hsh = rssiStore.get(cycle);
                        } else {
                            hsh = rssiStore.get(1);
                        }
                        rssiStore.put(cycle, hsh);
                        simul.getGateways().get(0).setRssiStore(rssiStore);

                    }
                }

            }

        }
        // // check RSSI jammed mote, is jammer is close enough, rssi will be raised
        // get hashmap with calc rssi's from first cycle, in real live, would be
        // measured average rssi

        // implementing random jammer
        if (jammer.getMode().equals("randomjam")) {
            // first check of jammed cycles are determined
            if (jammer.getJammedCyclesRandomJammer().isEmpty()) {
                setJammedCycles(jammer, simul);
            }
            // check if cycle in list, then jam, else unjam
            if (null != jammer.getJammedCyclesRandomJammer().get(cycle)) {

                if (false == jammer.getJammedCyclesRandomJammer().get(cycle)) {
                    // this cycle should not be jammed, so unjamming
                    unjam(deltaiot.DeltaIoTSimulator.simul, cycle);
                    return;
                }
                if (null == jammer.getActiveChannels()) {
                    System.out.println("jammer not initialized");
                    return;
                }

                if (!jammer.getActiveChannels()
                        .contains(deltaiot.DeltaIoTSimulator.simul.getActiveChannelFrequency())) {
                    // this cycle should not be jammed, so unjamming
                    unjam(deltaiot.DeltaIoTSimulator.simul, cycle);
                    return;
                } else {

                    for (domain.Mote m : simul.getMotes()) {
                        for (domain.Link l : m.getLinks()) {
                            Double linkDistance = Distance.calcDistance(l.getFrom().getPosition(),
                                    l.getTo().getPosition());

                            Double calcjammerToDistance = Distance.calcDistance(l.getTo().getPosition(),
                                    jammer.getPosition());

                            if (calcjammerToDistance < linkDistance) {
                                // jammer close enough

                                System.out.println("jam at frequence "
                                        + deltaiot.DeltaIoTSimulator.simul.getActiveChannelFrequency()
                                        + " in cycle " + ConfigLoader.getInstance().getCurrentCycle());
                                System.out.println("link info receiving , so changed" + l.toString());
                                // get original equation to apply jam
                                String strLinkid = l.getFrom().getId() + "_" + l.getTo().getId();
                                // we get from the gateway the original SNR equation for the link, stored during
                                // first cycle
                                domain.SNREquation eq = ConfigLoader.getInstance().getSnrEquations()
                                        .get(strLinkid);
                                System.out.println(l.toString() + ": original eq " + eq.toString());
                                // change the snr using jam power
                                eq.constant = eq.constant + powerJam;
                                System.out.println(l.toString() + ": new eq " + eq.toString());

                                l.setSnrEquation(eq);
                                // get hashmap with calc rssi's from first cycle, in real live, would be
                                // now change rssi based on distance
                                // get hashmap with calc rssi's from first cycle, in real live, would be
                                // measured average rssi
                                HashMap<Integer, HashMap<String, Double>> rssiStore = simul.getGateways().get(0)
                                        .getRssiStore();
                                // if it's the first jammed link, we get the rssi's from first run, otherwise
                                // retrieve the one stored from previous loop
                                HashMap<String, Double> hsh;
                                if (null != rssiStore.get(cycle)) {
                                    hsh = rssiStore.get(cycle);
                                } else {
                                    hsh = rssiStore.get(1);
                                }
                                String linkKey = l.getFrom().getId() + "_" + l.getTo().getId();
                                // now we replace standard rssi for this link with rssi perceived due to jammer
                                Double rssilinkjam = 35.0 - (32.0 * Math
                                        .log(Distance.calcDistance(jammer.getPosition(),
                                                l.getTo().getPosition())));

                                hsh.put(linkKey, rssilinkjam);
                                rssiStore.put(cycle, hsh);
                                simul.getGateways().get(0).setRssiStore(rssiStore);

                            }

                            else {

                                System.out.println("link to far, so not changed" + l.toString());
                            }
                        }
                    }

                }

            } else {
                System.out.println("not initialized");
                return;
            }
        }

        if (jammer.getMode().equals("reactivejam")) {
            // lowestDistance = Double.MAX_VALUE; // reinitialise
            // calcDistance = 0.0;
            // for (domain.Mote m : simul.getMotes()) {
            // calcDistance = Distance.calcDistance(m.getPosition(), jammer.getPosition());
            // if (calcDistance <= lowestDistance) {
            // lowestDistance = calcDistance;
            // idMoteClosestToJammer = m.getId();
            // }
            // }
            for (domain.Mote m : simul.getMotes()) {
                for (domain.Link l : m.getLinks()) {
                    Double linkDistance = Distance.calcDistance(l.getFrom().getPosition(), l.getTo().getPosition());
                    // Double calcjammerFromDistance =
                    // Distance.calcDistance(l.getFrom().getPosition(), jammer.getPosition());
                    Double calcjammerToDistance = Distance.calcDistance(l.getTo().getPosition(), jammer.getPosition());

                    if (calcjammerToDistance < linkDistance) {
                        // jammer close enough

                        if (null != jammer.getActiveChannels()) {
                            if (jammer.getActiveChannels()
                                    .contains(deltaiot.DeltaIoTSimulator.simul.getActiveChannelFrequency())) {

                                System.out.println("jam at frequence "
                                        + deltaiot.DeltaIoTSimulator.simul.getActiveChannelFrequency() + " in cycle "
                                        + ConfigLoader.getInstance().getCurrentCycle());
                                System.out.println("link info receiving , so changed" + l.toString());
                                // retrieve original link equation to apply jam

                                String strLinkid = l.getFrom().getId() + "_" + l.getTo().getId();
                                // we get from the gateway the original SNR equation for the link, stored during
                                // first cycle
                                domain.SNREquation eq = ConfigLoader.getInstance().getSnrEquations().get(strLinkid);
                                System.out.println(l.toString() + ": original eq constant " + eq.constant);
                                // change the snr using jam power
                                eq.constant = eq.constant + powerJam;
                                System.out.println(l.toString() + ": new eq constant" + eq.constant);
                                // now change rssi based on distance
                                // get hashmap with calc rssi's from first cycle, in real live, would be
                                // measured average rssi
                                HashMap<Integer, HashMap<String, Double>> rssiStore = simul.getGateways().get(0)
                                        .getRssiStore();
                                // if it's the first jammed link, we get the rssi's from first run, otherwise
                                // retrieve the one stored from previous loop
                                HashMap<String, Double> hsh;
                                if (null != rssiStore.get(cycle)) {
                                    hsh = rssiStore.get(cycle);
                                } else {
                                    hsh = rssiStore.get(1);
                                }
                                String linkKey = l.getFrom().getId() + "_" + l.getTo().getId();
                                // now we replace standard rssi for this link with rssi perceived due to jammer
                                Double rssilinkjam = 35.0 - (32.0 * Math
                                        .log(Distance.calcDistance(jammer.getPosition(), l.getTo().getPosition())));

                                hsh.put(linkKey, rssilinkjam);
                                rssiStore.put(cycle, hsh);
                                simul.getGateways().get(0).setRssiStore(rssiStore);

                                l.setSnrEquation(eq);

                                // simul.addJammer(jammer);

                            } else {
                                unjam(deltaiot.DeltaIoTSimulator.simul, cycle);
                            }
                        }
                    } else {
                        System.out.println("link to far, so not changed" + l.toString());
                    }

                }
            }

        }

    }

    /*
     * random jammer has % of cycles to be jammed
     * first implementation : fixed block of jam + sleep
     */
    private void setJammedCycles(Jammer jammer, Simulator simul) {
        long numberOfBlocks = Math.round((100) / (ConfigLoader.getInstance().getRandSpreadPercentage()));
        long blocklength = Math
                .round((ConfigLoader.getInstance().getStopJamCycle() - ConfigLoader.getInstance().getStartJamCycle())
                        / numberOfBlocks);
        long activeCycles = Math.round((blocklength * ConfigLoader.getInstance().getRandActivePercentage()) / 100.0);
        long sleepCycles = blocklength - activeCycles;
        // loop over cycles and determine the jammed ones
        Integer iActiveCylces = new BigDecimal(activeCycles).intValueExact();
        Integer isleepCylces = new BigDecimal(sleepCycles).intValueExact();
        int activecycle = 0;
        int sleepcycle = 0;
        for (int i = ConfigLoader.getInstance().getStartJamCycle(); i <= ConfigLoader.getInstance()
                .getStopJamCycle(); i++) {
            activecycle++;
            if (activecycle <= iActiveCylces) {
                jammer.getJammedCyclesRandomJammer().put(i, true);
                continue;
            }
            sleepcycle++;
            if (sleepcycle <= isleepCylces) {
                jammer.getJammedCyclesRandomJammer().put(i, false);
                continue;
            }
            if (sleepcycle > isleepCylces) {
                activecycle = 1;
                jammer.getJammedCyclesRandomJammer().put(i, true);
                sleepcycle = 0;
            }

        }

    }

    private void unjam(Simulator simul, int icycle) {

        /*
         * mote2.getLinkTo(mote4).setSnrEquation(new SNREquation(0.0169, 7.4076));
         * mote3.getLinkTo(gateway).setSnrEquation(new SNREquation(0.4982, 1.2468));
         * mote4.getLinkTo(gateway).setSnrEquation(new SNREquation(0.8282, -8.1246));
         * mote5.getLinkTo(mote9).setSnrEquation(new SNREquation(0.4932, -2.4898)); //
         * -4.4898
         * mote6.getLinkTo(mote4).setSnrEquation(new SNREquation(0.6199, -4.8051));
         * //-9.8051
         * mote7.getLinkTo(mote3).setSnrEquation(new SNREquation(0.5855, -2.644)); //
         * -6.644
         * mote7.getLinkTo(mote2).setSnrEquation(new SNREquation(0.5398, -2.0549));
         * mote8.getLinkTo(gateway).setSnrEquation(new SNREquation(0.5298, -0.1031));
         * mote9.getLinkTo(gateway).setSnrEquation(new SNREquation(0.8284, -7.2893));
         * mote10.getLinkTo(mote6).setSnrEquation(new SNREquation(0.8219, -7.3331));
         * mote10.getLinkTo(mote5).setSnrEquation(new SNREquation(0.6463, -3.0037));
         * mote11.getLinkTo(mote7).setSnrEquation(new SNREquation(0.714, -3.1985));
         * mote12.getLinkTo(mote7).setSnrEquation(new SNREquation(0.9254, -12.21)); //
         * -16.12
         * mote12.getLinkTo(mote3).setSnrEquation(new SNREquation(0.1, 6));
         * mote13.getLinkTo(mote11).setSnrEquation(new SNREquation(0.6078, -3.6005));
         * mote14.getLinkTo(mote12).setSnrEquation(new SNREquation(0.4886, -4.7704));
         * mote15.getLinkTo(mote12).setSnrEquation(new SNREquation(0.5899, -5.1896)); //
         * -7.1896
         */

        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(2).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(4))
                .setSnrEquation(new domain.SNREquation(0.0169, 7.4076));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(3)
                .getLinkTo(deltaiot.DeltaIoTSimulator.simul.getGatewayWithId(1))
                .setSnrEquation(new domain.SNREquation(0.4982, 1.2468));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(4)
                .getLinkTo(deltaiot.DeltaIoTSimulator.simul.getGatewayWithId(1))
                .setSnrEquation(new domain.SNREquation(0.8282, -8.1246));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(5).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(9))
                .setSnrEquation(new domain.SNREquation(0.4932, -2.4898));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(6).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(4))
                .setSnrEquation(new domain.SNREquation(0.6199, -4.8051));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(7).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(3))
                .setSnrEquation(new domain.SNREquation(0.5855, -2.644));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(7).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(2))
                .setSnrEquation(new domain.SNREquation(0.5398, -2.0549));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(8)
                .getLinkTo(deltaiot.DeltaIoTSimulator.simul.getGatewayWithId(1))
                .setSnrEquation(new domain.SNREquation(0.5298, -0.1031));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(9)
                .getLinkTo(deltaiot.DeltaIoTSimulator.simul.getGatewayWithId(1))
                .setSnrEquation(new domain.SNREquation(0.8284, -7.2893));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(10).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(6))
                .setSnrEquation(new domain.SNREquation(0.8219, -7.3331));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(10).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(5))
                .setSnrEquation(new domain.SNREquation(0.6463, -3.0037));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(11).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(7))
                .setSnrEquation(new domain.SNREquation(0.714, -3.1985));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(12).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(7))
                .setSnrEquation(new domain.SNREquation(0.9254, -12.21));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(12).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(3))
                .setSnrEquation(new domain.SNREquation(0.1, 6));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(13).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(11))
                .setSnrEquation(new domain.SNREquation(0.6078, -3.6005));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(14).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(12))
                .setSnrEquation(new domain.SNREquation(0.4886, -4.7704));
        deltaiot.DeltaIoTSimulator.simul.getMoteWithId(15).getLinkTo(deltaiot.DeltaIoTSimulator.simul.getMoteWithId(12))
                .setSnrEquation(new domain.SNREquation(0.5899, -5.1896));

        // get calc rssi's from first cycle
        HashMap<Integer, HashMap<String, Double>> rssiStore = simul.getGateways().get(0).getRssiStore();
        HashMap<String, Double> hashMap = rssiStore.get(1); // first cycle is not jammed, so contains all calculated
                                                            // rssi's without jammer
        rssiStore.put(cycle, hashMap);
        simul.getGateways().get(0).setRssiStore(rssiStore); // basically copied rssi's from first run

    }

    // private static void printLinkInfo(Simulator simul) {
    // for (int i = 1; i <= ConfigLoader.getInstance().getAmountOfCycles(); i++) {
    // if (null != simul.getGateways().get(0).getlostPacketStoreLink(i)) {
    // List<domain.LostPacketLink> listlost =
    // simul.getGateways().get(0).getlostPacketStoreLink(i);
    // System.out.println("run:" + i + "\n");
    // // now adding packets lost for this run
    // if (null != simul.getGateways().get(0).getLostPackedStore(i)) {
    // List<domain.Packet> listpacketslost =
    // simul.getGateways().get(0).getLostPackedStore(i);
    // HashMap<Integer, Integer> sumLostPackages = new HashMap<>();
    // listpacketslost.forEach((temp) -> {
    // int packetcount = 0;
    // int moteId = temp.getSource().getId();
    // if (sumLostPackages.containsKey(moteId)) {
    // packetcount = sumLostPackages.get(moteId);
    // packetcount++;
    // } else {
    // packetcount++;
    // }
    // sumLostPackages.put(moteId, packetcount);
    // System.out.println(packetcount + ":for mote " + moteId);

    // });

    // printPacketLossPerMote(sumLostPackages, i);
    // }
    // listlost.forEach((temp) -> {
    // System.out.println(temp.getFrom().getId() + ":" + temp.getTo().getId() + ":"
    // + temp.getSNR());

    // });

    // }
    // }
    // }

    private void printGeneratedAndLost(Simulator simul, HashMap<String, ArrayList<Double>> packetlossratios) {
        // initialize keys (= links)
        ArrayList<String> keys = new ArrayList<>();
        for (int i = 0; i < ConfigLoader.getInstance().getAmountOfCycles(); i++) {
            if (null != simul.getGateways().get(0).getGeneratedPacketLinkStore().get(i)) {
                List<String> listgen = simul.getGateways().get(0).getGeneratedPacketLinkStore().get(i);
                listgen.forEach((temp) -> {

                    String[] line = temp.split("_");
                    String key = line[2] + "_" + line[3]; // eg 15_12 , representing link
                    if (!keys.contains(key)) {
                        keys.add(key);
                    }

                });
            }
        }

        // keys of links determined

        for (int i = 0; i < ConfigLoader.getInstance().getAmountOfCycles(); i++) {
            boolean packetlost = true;
            HashMap<String, Integer> sumGeneratedPackages = new HashMap<>();
            HashMap<String, Integer> sumLostPackages = new HashMap<>();
            // initialize with keys and values 0.0
            for (String key : keys) {
                sumGeneratedPackages.put(key, 0);
                sumLostPackages.put(key, 0);
            }
            if (null != simul.getGateways().get(0).getGeneratedPacketLinkStore().get(i)) {

                List<String> listgenerated = simul.getGateways().get(0).getGeneratedPacketLinkStore().get(i);
                listgenerated.forEach((temp) -> {
                    int packetcount = 0;
                    String[] line = temp.split("_");
                    String key = line[2] + "_" + line[3]; // eg 15_12 , representing link
                    if (sumGeneratedPackages.containsKey(key)) {
                        packetcount = sumGeneratedPackages.get(key);
                        packetcount++;
                    } else {
                        packetcount++;
                    }
                    // if (null == packetlossratios.get(key)) {
                    // packetlossratios.put(key, new ArrayList<Double>());
                    // }

                    sumGeneratedPackages.put(key, packetcount);
                    System.out.println(packetcount + ":for key " + key);

                    System.out.println(temp);
                });

            } else {
                System.out.println("run " + i + ": no packets generated");
            }
            if (null != simul.getGateways().get(0).getLostPacketLinkStore().get(i)) {

                List<String> listlost = simul.getGateways().get(0).getLostPacketLinkStore().get(i);
                listlost.forEach((temp) -> {
                    int lostpacketcount = 0;
                    String[] line = temp.split("_");
                    String key = line[2] + "_" + line[3]; // eg 15_12 , representing link
                    if (sumLostPackages.containsKey(key)) {
                        lostpacketcount = sumLostPackages.get(key);
                        lostpacketcount++;
                    } else {
                        lostpacketcount++;
                    }

                    sumLostPackages.put(key, lostpacketcount);
                    System.out.println(lostpacketcount + ": lost for key " + key);
                    System.out.println(temp);
                });

            } else {
                packetlost = false; // no lost packets for this cycle
            }

            // now print packetloss ratios
            printPacketLossRatioPerLink(sumGeneratedPackages, sumLostPackages, packetlost, i, packetlossratios);
        }

    }

    private void printPacketLossRatioPerLink(HashMap<String, Integer> sumGeneratedPackages,
            HashMap<String, Integer> sumLostPackages, boolean packetlost, int cycle,
            HashMap<String, ArrayList<Double>> packetlossratios) {

        // a link used for sending encompasses possible lost package stores
        for (Map.Entry<String, Integer> entry : sumGeneratedPackages.entrySet()) {
            System.out.println("Key = " + entry.getKey() +
                    ", Value = " + entry.getValue());
            File dataset_file = null;
            switch (ConfigLoader.getInstance().getMode()) {
                case "standard":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/standard/",
                            "packetlossLink" + "_" + ConfigLoader.getInstance().getRunId() + "_" + entry.getKey()
                                    + ".csv")
                            .toString());
                    break;

                case "drift":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/drift/",
                            "packetlossLink" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                    + ConfigLoader.getInstance().getStartDriftCycle() + "_"
                                    + ConfigLoader.getInstance().getStopDriftCycle() + "_"
                                    + ConfigLoader.getInstance().getMaxDriftRatio() + "_" + entry.getKey() + ".csv")
                            .toString());
                    break;
                case "constantjam":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/constantjam/",
                            "packetlossLink" + "_run" + ConfigLoader.getInstance().getRunId() + "_constantjam_"
                                    + "_jamX"
                                    + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                    + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                    + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                    + ConfigLoader.getInstance().getStopJamCycle() + "_jamPower"
                                    + ConfigLoader.getInstance().getPowerJam() + "_linkSourceDest" + entry.getKey()
                                    + ".csv")
                            .toString());
                    break;
                case "reactivejam":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/reactivejam/",
                            "packetlossLink" + "_run" + ConfigLoader.getInstance().getRunId() + "_reactivejam_"
                                    + "_jamX"
                                    + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                    + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                    + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                    + ConfigLoader.getInstance().getStopJamCycle() + "_jamPower"
                                    + ConfigLoader.getInstance().getPowerJam() + "_linkSourceDest" + entry.getKey()
                                    + ".csv")
                            .toString());
                    break;
                case "randomjam":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/randomjam/",
                            "packetlossLink" + "_run" + ConfigLoader.getInstance().getRunId() + "_randomjam_" + "_jamX"
                                    + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                    + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                    + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                    + ConfigLoader.getInstance().getStopJamCycle() + "_randActivePercentage"
                                    + ConfigLoader.getInstance().getRandActivePercentage() + "_randSpreadPercentage"
                                    + ConfigLoader.getInstance().getRandSpreadPercentage() + "_jamPower"
                                    + ConfigLoader.getInstance().getPowerJam() + "_linkSourceDest" + entry.getKey()
                                    + ".csv")
                            .toString());
                    break;

            }

            try {

                FileWriter writer = new FileWriter(dataset_file, true);
                // DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd
                // HH:mm:ss.SSS");
                // LocalDateTime localDateTime = LocalDateTime.now();

                // String format = fmt.format(localDateTime);
                // write the PacketLossRatio, so retrieving lost packages sent over this link in
                // the same cycle, then calculate ratio
                if (!packetlost) {
                    // all keys have a 0% packetloss ratio
                    if (null != packetlossratios.get(entry.getKey())) {
                        packetlossratios.get(entry.getKey()).add((Double) 0.0);
                    } else {
                        ArrayList<Double> arrList = new ArrayList<Double>();
                        arrList.add((Double) 0.0);
                        packetlossratios.put(entry.getKey(), arrList);
                    }

                    writer.write("0;0\n");
                } else {
                    // get the lost packages
                    int lostpackages = 0;
                    if (null != sumLostPackages.get(entry.getKey())) {
                        lostpackages = sumLostPackages.get(entry.getKey());
                    }
                    // calculate ratio
                    double packetlossratio = 0.0;
                    if (entry.getValue() != 0) {
                        packetlossratio = (100.0 * (double) lostpackages) / (double) entry.getValue();
                    }

                    boolean flagDetected = false;
                    if (ConfigLoader.getInstance().getMode().contains("jam")) {
                        for (HashMap<String, Boolean> hsh : mapJamDetected.get(cycle)) {
                            if (hsh.keySet().contains(entry.getKey())) {
                                flagDetected = hsh.get(entry.getKey());
                                break;
                            }
                        }
                    }

                    if (flagDetected) {
                        System.out.println(cycle + ":" + entry.getKey() + ":packages sent : " + entry.getValue()
                                + "; package lost : " + lostpackages + ";plr :" + packetlossratio
                                + "; jam detected = true");
                        writer.write(packetlossratio + ";" + "50\n");
                    } else {
                        System.out.println(cycle + ":" + entry.getKey() + ":packages sent : " + entry.getValue()
                                + "; package lost : " + lostpackages + ";plr :" + packetlossratio
                                + "; jam detected = false");
                        writer.write(packetlossratio + ";" + "0\n");
                    }
                    if (null != packetlossratios.get(entry.getKey())) {
                        packetlossratios.get(entry.getKey()).add(packetlossratio);
                    } else {
                        ArrayList<Double> arrList = new ArrayList<Double>();
                        arrList.add(packetlossratio);
                        packetlossratios.put(entry.getKey(), arrList);
                    }
                }

                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format("Could not create the output file at %s", dataset_file.toPath().toString()));
            }
        }
    }

    void monitor() {
        // The method "probe.getAllMotes()" also makes sure the simulator is run for a
        // single cycle
        ArrayList<deltaiot.services.Mote> motes = probe.getAllMotes();

        // now the cycle has ended, we retrieve the packetloss,

        List<Mote> newMotes = new LinkedList<>();
        previousConfiguration = currentConfiguration;
        currentConfiguration = new Configuration();

        // Make a copy of the IoT network in its current state
        Mote newMote;
        Link newLink;

        // Iterate through all the motes of the simulator
        for (deltaiot.services.Mote mote : motes) {

            newMote = new Mote();
            newMote.moteId = mote.getMoteid();
            newMote.energyLevel = mote.getBattery();
            newMote.load = mote.getLoad();
            newMote.queueSize = mote.getCurrentQSize();

            // motesLoad holds a list of the probabilities that certain motes generate
            // packets (probability in range [0, 100])
            currentConfiguration.environment.motesLoad
                    .add(new TrafficProbability(mote.getMoteid(), mote.getDataProbability()));

            // Copy the links and their SNR values
            for (deltaiot.services.Link link : mote.getLinks()) {
                newLink = new Link();
                newLink.source = link.getSource();
                newLink.destination = link.getDest();
                newLink.distribution = link.getDistribution();
                newLink.power = link.getPower();
                newMote.links.add(newLink);
                currentConfiguration.environment.linksSNR.add(new SNR(link.getSource(), link.getDest(), link.getSNR()));

            }

            // add the mote to the configuration
            newMotes.add(newMote);
        }

        // This saves the architecture of the system to the new configuration by adding
        // the
        // new motes which contain all the necessary data
        currentConfiguration.system = new ManagedSystem(newMotes);

        // getNetworkQoS(n) returns a list of the QoS
        // values of the n previous cycles.
        // This returns the latest QoS and
        // returns the first (and only) element of the list.
        QoS qos = probe.getNetworkQoS(1).get(0);

        // Adds the QoS of the previous configuration to the current configuration
        currentConfiguration.qualities.packetLoss = qos.getPacketLoss();
        currentConfiguration.qualities.energyConsumption = qos.getEnergyConsumption();
        currentConfiguration.qualities.latency = qos.getLatency();
        printQoS(qos);
        // Call the next step off the mapek loop
        // analysis();
    }

    void analysis() {

        boolean adaptationRequired = analysisRequired();

        if (!adaptationRequired)
            return;

        AdaptationOption newPowerSettingsConfig = new AdaptationOption();
        newPowerSettingsConfig.system = currentConfiguration.system.getCopy();

        // Find the optimal power setting for this cycle
        analyzePowerSettings(newPowerSettingsConfig);
        // Make sure packets are not duplicated in case a mote has more than 1 parent
        // (not of interest at the moment)
        removePacketDuplication(newPowerSettingsConfig);
        // This adds the possible link distributions to the motes who have 2 outgoing
        // links
        // ~= (construction of the adaptation space)
        composeAdaptationOptions(newPowerSettingsConfig);

        // Pass the adaptionOptions and the environment (noise and load) to the
        // connector
        smcConnector.setAdaptationOptions(currentAdaptationOptions, currentConfiguration.environment);

        // let the model checker and/or machine learner start to predict which adaption
        // options
        // should be considered by the planner
        smcConnector.verify();

        // Only consider those options which have been formally verified by the model
        // checker
        verifiedOptions.clear();
        for (AdaptationOption option : currentAdaptationOptions) {
            if (option.isVerified) {
                verifiedOptions.add(option);
            }
        }

        // Continue to the planning step.
        planning();
    }

    /**
     * Sets the distributions for the links of motes with 2 parents to 0-100
     * respectively.
     *
     * @param newConfiguration the adaptation option which should be adjusted
     */
    void initializeMoteDistributions(AdaptationOption newConfiguration) {
        for (Mote mote : newConfiguration.system.motes.values()) {
            if (mote.getLinks().size() == 2) {
                mote.getLink(0).setDistribution(0);
                mote.getLink(1).setDistribution(100);
            }
        }
    }

    void composeAdaptationOptions(AdaptationOption newConfiguration) {
        // Clear the previous list of adaptation options
        currentAdaptationOptions.clear();
        List<Mote> moteOptions = new LinkedList<>();

        initializeMoteDistributions(newConfiguration);

        int initialValue = 0;
        for (Mote mote : newConfiguration.system.motes.values()) {
            // Search for the motes with 2 parents
            if (mote.getLinks().size() == 2) {
                mote = mote.getCopy();
                moteOptions.clear();

                // iterate over all the possible distribution options
                for (int i = initialValue; i <= Math.ceil(100 / (double) DISTRIBUTION_GAP); i++) {
                    int distributionValue = Math.min(i * DISTRIBUTION_GAP, 100);
                    mote.getLink(0).setDistribution(distributionValue);
                    mote.getLink(1).setDistribution(100 - distributionValue);
                    moteOptions.add(mote.getCopy());
                }
                initialValue = 1;

                // add the new option to the global (feedbackloop object) adaption options for
                // the mote
                saveAdaptationOptions(newConfiguration, moteOptions, mote.getMoteId());
            }

        }

        // Update the indices and verification status of the adaptation options
        for (int i = 0; i < currentAdaptationOptions.size(); i++) {
            currentAdaptationOptions.get(i).overallIndex = i;
            currentAdaptationOptions.get(i).isVerified = false;
        }
    }

    private void saveAdaptationOptions(AdaptationOption firstConfiguration, List<Mote> moteOptions, int moteId) {
        AdaptationOption newAdaptationOption;

        if (currentAdaptationOptions.isEmpty()) {
            // for the new options, add them to the global options
            for (int j = 0; j < moteOptions.size(); j++) {
                newAdaptationOption = firstConfiguration.getCopy();
                newAdaptationOption.system.motes.put(moteId, moteOptions.get(j));

                currentAdaptationOptions.add(newAdaptationOption);
            }

        } else {
            int size = currentAdaptationOptions.size();

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < moteOptions.size(); j++) {
                    newAdaptationOption = currentAdaptationOptions.get(i).getCopy();
                    newAdaptationOption.system.motes.put(moteId, moteOptions.get(j));
                    currentAdaptationOptions.add(newAdaptationOption);
                }
            }

        }
    }

    /**
     * Finds the optimal power settings over all the links (by minimizing packet
     * loss).
     *
     * @param newConfiguration the configuration which will hold the optimal power
     *                         settings.
     */
    private void analyzePowerSettings(AdaptationOption newConfiguration) {
        int powerSetting;
        double newSNR;

        // Iterate over the motes of the managed system (values returns a list or array
        // with the motes)
        for (Mote mote : newConfiguration.system.motes.values()) {
            // Iterate over all the outgoing links of the mote
            for (Link link : mote.getLinks()) {

                powerSetting = link.getPower();
                newSNR = currentConfiguration.environment.getSNR(link);

                // find interference
                double diffSNR = getSNR(link.getSource(), link.getDestination(), powerSetting) - newSNR;

                // Calculate the most optimal power setting (higher if packet loss, lower if
                // energy can be reserved)
                if (powerSetting < 15 && newSNR < 0 && newSNR != -50) {

                    while (powerSetting < 15 && newSNR < 0) {
                        newSNR = getSNR(link.getSource(), link.getDestination(), ++powerSetting) - diffSNR;
                    }

                } else if (newSNR > 0 && powerSetting > 0) {
                    do {
                        newSNR = getSNR(link.getSource(), link.getDestination(), powerSetting - 1) - diffSNR;

                        if (newSNR >= 0) {
                            powerSetting--;
                        }

                    } while (powerSetting > 0 && newSNR >= 0);
                }

                // Adjust the powersetting of the link if it is not yet the optimal one
                if (link.getPower() != powerSetting) {
                    link.setPower(powerSetting);
                    currentConfiguration.environment.setSNR(link,
                            getSNR(link.getSource(), link.getDestination(), powerSetting) - diffSNR);
                }
            }
        }
    }

    /**
     * If there are 2 outgoing links which are both set to 100,
     * packets will be duplicated and sent over both links.
     * This method sets the distribution of the first link to 0 in that case.
     *
     * @param newConfiguration the adaptation option which should be adjusted.
     */
    private void removePacketDuplication(AdaptationOption newConfiguration) {
        for (Mote mote : newConfiguration.system.motes.values()) {
            if (mote.getLinks().size() == 2) {
                if (mote.getLink(0).getDistribution() == 100 && mote.getLink(1).getDistribution() == 100) {
                    mote.getLink(0).setDistribution(0);
                    mote.getLink(1).setDistribution(100);
                }
            }
        }
    }

    double getSNR(int source, int destination, int newPowerSetting) {
        for (SNREquation equation : snrEquations) {
            if (equation.source == source && equation.destination == destination) {
                return equation.multiplier * newPowerSetting + equation.constant;
            }
        }
        throw new RuntimeException("Link not found:" + source + "-->" + destination);
    }

    boolean analysisRequired() {
        // for simulation we use adaptation after 4 periods
        // return i++%4 == 0;

        // if first time perform adaptation
        if (previousConfiguration == null)
            return true;

        Map<Integer, Mote> motes = currentConfiguration.system.motes;

        // Retrieve the amount of links present in the system (count links for each
        // mote)
        final int MAX_LINKS = (int) motes.values().stream().map(o -> o.links.size()).count();
        // Check LinksSNR
        for (int j = 0; j < MAX_LINKS; j++) {
            double linksSNR = currentConfiguration.environment.linksSNR.get(j).SNR;
            if (linksSNR < SNR_BELOW_THRESHOLD || linksSNR > SNR_UPPER_THRESHOLD) {
                return true;
            }
        }

        // Check MotesTraffic
        double diff;

        for (int i : motes.keySet()) {
            diff = currentConfiguration.environment.motesLoad.get(i).load
                    - previousConfiguration.environment.motesLoad.get(i).load;
            if (diff > Math.abs(diff)) {
                return true;
            }
        }

        // check qualities
        if ((currentConfiguration.qualities.packetLoss > previousConfiguration.qualities.packetLoss
                + PACKET_LOSS_THRESHOLD)
                || (currentConfiguration.qualities.energyConsumption > previousConfiguration.qualities.energyConsumption
                        + ENERGY_CONSUMPTION_THRESHOLD)) {
            return true;
        }

        // check if system settings are not what should be
        return !currentConfiguration.system.toString().equals(previousConfiguration.system.toString());

    }

    // The planning step of the mape loop
    // Selects "the best" addaption options of the predicted/ verified ones
    // and plans the option to be executed
    void planning() {

        AdaptationOption bestAdaptationOption = null;

        // TODO: What course of action if not all the goals are met?
        for (int i = 0; i < verifiedOptions.size(); i++) {

            AdaptationOption option = verifiedOptions.get(i);
            Goal pl = goals.getPacketLossGoal();

            /*
             * TaskType type = ConfigLoader.getInstance().getTaskType();
             * if (type.equals(TaskType.PLLAMULTICLASS) ||
             * type.equals(TaskType.PLLAMULTIREGR)) {
             * Goal la = goals.getLatencyGoal();
             * 
             * if (la.evaluate(option.verificationResults.latency)
             * && pl.evaluate(option.verificationResults.packetLoss)
             * && goals.optimizeGoalEnergyConsumption(bestAdaptationOption, option)) {
             * bestAdaptationOption = option;
             * }
             * 
             * } else {
             * if (pl.evaluate(option.verificationResults.packetLoss)
             * && goals.optimizeGoalEnergyConsumption(bestAdaptationOption, option)) {
             * bestAdaptationOption = option;
             * }
             * }
             */

            Goal la = goals.getLatencyGoal();

            if (la.evaluate(option.verificationResults.latency)
                    && pl.evaluate(option.verificationResults.packetLoss)
                    && goals.optimizeGoalEnergyConsumption(bestAdaptationOption, option)) {
                bestAdaptationOption = option;
            }
        }

        // Use the failsafe configuration if none of the options fullfill the goals
        if (bestAdaptationOption == null) {
            for (int i = 0; i < verifiedOptions.size(); i++) {
                if (goals.optimizeGoalEnergyConsumption(bestAdaptationOption, verifiedOptions.get(i))) {
                    bestAdaptationOption = verifiedOptions.get(i);
                }
            }
        }

        // printBestAdaptionResults(bestAdaptationOption);
        System.out.print(";" + bestAdaptationOption.verificationResults.packetLoss);
        System.out.print(";" + bestAdaptationOption.verificationResults.latency);
        System.out.print(";" + bestAdaptationOption.verificationResults.energyConsumption);

        storeBestFeatures(bestAdaptationOption, getCycle());

        // Go through all links and construct the steps that have to be made to change
        // to the best adaptation option
        Link newLink, oldLink;
        for (Mote mote : bestAdaptationOption.system.motes.values()) {
            for (int i = 0; i < mote.getLinks().size(); i++) {

                // predicted mote, which will be executed
                newLink = mote.getLinks().get(i);

                // get the current link configuration. which will become the old one
                oldLink = currentConfiguration.system.motes.get(mote.moteId).getLink(i);

                if (newLink.getPower() != oldLink.getPower()) {
                    // add a step/change to be executed later
                    steps.add(new PlanningStep(Step.CHANGE_POWER, newLink, newLink.getPower()));
                }

                if (newLink.getDistribution() != oldLink.getDistribution()) {
                    // add a step/change to be executed later
                    steps.add(new PlanningStep(Step.CHANGE_DIST, newLink, newLink.getDistribution()));
                }
            }
        }

        // if there are steps to be executed, trigger execute to do them
        if (steps.size() > 0) {
            execution();
        } else {
            System.out.println();

            /*
             * if (ConfigLoader.getInstance().timeInReadableFormat()) {
             * LocalDateTime now = LocalDateTime.now();
             * System.out.println("; " + String.format("%02d:%02d:%02d", now.getHour(),
             * now.getMinute(), now.getSecond()));
             * } else {
             * System.out.print(";" + System.currentTimeMillis());
             * }
             */
        }
    }

    private void storeBestFeatures(AdaptationOption bestOption, int cycle) {
        // save best results from config in seperate file
        // put outside of root folder of deltaiot, need extra output folder for batch
        // processing
        File dataset_file = null;
        switch (ConfigLoader.getInstance().getMode()) {
            case "standard":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/standard/",
                        "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_" + getCycle() + ".json")
                        .toString());
                break;

            case "drift":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/drift/",
                        "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                + ConfigLoader.getInstance().getStartDriftCycle() + "_"
                                + ConfigLoader.getInstance().getStopDriftCycle() + "_"
                                + ConfigLoader.getInstance().getMaxDriftRatio() + "_" + getCycle() + ".json")
                        .toString());
                break;
            case "jam":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/jam/",
                        "dataset_with_all_features" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                + ConfigLoader.getInstance().getStartJamCycle() + "_"
                                + ConfigLoader.getInstance().getStopJamCycle() + "_"
                                + ConfigLoader.getInstance().getPowerJam() + "_" + getCycle() + ".json")
                        .toString());
                break;

        }

        // File dataset_file = new
        // File(Paths.get("./output/","best_configs_v1","dataset_with_all_features" +
        // cycle + ".json").toString());
        if (dataset_file.exists()) {
            // At the first cycle, remove the file if it already exists

            dataset_file.delete();
        }

        try {
            dataset_file.createNewFile();
            JSONObject root = new JSONObject();
            root.put("packetloss", new JSONArray());
            root.put("latency", new JSONArray());
            root.put("energyconsumption", new JSONArray());
            FileWriter writer = new FileWriter(dataset_file, false);
            writer.write(root.toString(2));
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not create the output file at %s", dataset_file.toPath().toString()));
        }

        try {
            JSONTokener tokener = new JSONTokener(dataset_file.toURI().toURL().openStream());
            JSONObject root = new JSONObject(tokener);

            // Packet loss values
            // root.getJSONArray("target_classification_packetloss").put(
            // goals.getPacketLossGoal().evaluate(option.verificationResults.packetLoss) ? 1
            // : 0);
            root.getJSONArray("packetloss").put(bestOption.verificationResults.packetLoss);

            // Latency values
            // root.getJSONArray("target_classification_latency").put(
            // goals.getLatencyGoal().evaluate(option.verificationResults.latency) ? 1 : 0);
            root.getJSONArray("latency").put(bestOption.verificationResults.latency);

            // Energy consumption values
            root.getJSONArray("energyconsumption").put(bestOption.verificationResults.energyConsumption);

            FileWriter writer = new FileWriter(dataset_file);
            writer.write(root.toString());
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not write to the output file at %s", dataset_file.toPath().toString()));
        }
    }

    void printQoS(QoS varQoS) {
        // receiving the best adaption, we write it timestamped to a file with runid
        // info in filename

        // Store the features and the targets in their respective files
        // put outside of root folder of deltaiot, need extra output folder for batch
        // processing
        File dataset_file = null;
        switch (ConfigLoader.getInstance().getMode()) {
            case "standard":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/standard/",
                        "packetlossTotal" + "_run" + ConfigLoader.getInstance().getRunId() + ".txt").toString());
                break;

            case "drift":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/drift/",
                        "packetlossTotal" + "_" + ConfigLoader.getInstance().getRunId() + "_driftStart"
                                + ConfigLoader.getInstance().getStartDriftCycle() + "_driftStop"
                                + ConfigLoader.getInstance().getStopDriftCycle() + "_maxDriftRatio"
                                + ConfigLoader.getInstance().getMaxDriftRatio() + ".txt")
                        .toString());
                break;
            case "constantjam":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/constantjam/",
                        "packetlossTotal" + "_run" + ConfigLoader.getInstance().getRunId() + "_jamX"
                                + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                + ConfigLoader.getInstance().getStopJamCycle() + "_jamPower"
                                + ConfigLoader.getInstance().getPowerJam() + ".txt")
                        .toString());
                break;
            case "reactivejam":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/reactivejam/",
                        "packetlossTotal" + "_run" + ConfigLoader.getInstance().getRunId() + "_jamX"
                                + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                + ConfigLoader.getInstance().getStopJamCycle() + "_jamPower"
                                + ConfigLoader.getInstance().getPowerJam() + ".txt")
                        .toString());
                break;
            case "randomjam":
                dataset_file = new File(Paths.get(
                        "./../output/",
                        ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/randomjam/",
                        "packetlossTotal" + "_run" + ConfigLoader.getInstance().getRunId() + "_jamX"
                                + ConfigLoader.getInstance().getPosXJammer() + "_jamY"
                                + ConfigLoader.getInstance().getPosYJammer() + "_jamStart"
                                + ConfigLoader.getInstance().getStartJamCycle() + "_jamStop"
                                + ConfigLoader.getInstance().getStopJamCycle() + "_randActivePercentage"
                                + ConfigLoader.getInstance().getRandActivePercentage() + "_randSpreadPercentage"
                                + ConfigLoader.getInstance().getRandSpreadPercentage() + "_jamPower"
                                + ConfigLoader.getInstance().getPowerJam() + ".txt")
                        .toString());
                break;

        }

        try {

            FileWriter writer = new FileWriter(dataset_file, true);
            // DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd
            // HH:mm:ss.SSS");
            // LocalDateTime localDateTime = LocalDateTime.now();

            // String format = fmt.format(localDateTime);
            writer.write(varQoS.getPacketLoss() + "\n");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not create the output file at %s", dataset_file.toPath().toString()));
        }
    }

    static void printPacketLossPerMote(HashMap<Integer, Integer> sumLostPackages, int cycle) {
        // receiving the best adaption, we write it timestamped to a file with runid
        // info in filename

        // Store the features and the targets in their respective files
        // put outside of root folder of deltaiot, need extra output folder for batch
        // processing
        // loop over moteids and write info for cycle to corresponding file
        for (Map.Entry<Integer, Integer> entry : sumLostPackages.entrySet()) {
            System.out.println("Key = " + entry.getKey() +
                    ", Value = " + entry.getValue());
            File dataset_file = null;
            switch (ConfigLoader.getInstance().getMode()) {
                case "standard":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/standard/",
                            "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_" + entry.getKey() + ".csv")
                            .toString());
                    break;

                case "drift":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/drift/",
                            "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                    + ConfigLoader.getInstance().getStartDriftCycle() + "_"
                                    + ConfigLoader.getInstance().getStopDriftCycle() + "_"
                                    + ConfigLoader.getInstance().getMaxDriftRatio() + "_" + entry.getKey() + ".csv")
                            .toString());
                    break;
                case "constantjam":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/constantjam/",
                            "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                    + ConfigLoader.getInstance().getStartJamCycle() + "_"
                                    + ConfigLoader.getInstance().getStopJamCycle() + "_"
                                    + ConfigLoader.getInstance().getPowerJam() + "_" + entry.getKey() + ".csv")
                            .toString());
                    break;
                case "reactivejam":
                    dataset_file = new File(Paths.get(
                            "./../output/",
                            ConfigLoader.getInstance().getSimulationNetwork().toLowerCase(), "/reactivejam/",
                            "packetloss" + "_" + ConfigLoader.getInstance().getRunId() + "_"
                                    + ConfigLoader.getInstance().getStartJamCycle() + "_"
                                    + ConfigLoader.getInstance().getStopJamCycle() + "_"
                                    + ConfigLoader.getInstance().getPowerJam() + "_" + entry.getKey() + ".csv")
                            .toString());
                    break;

            }

            try {

                FileWriter writer = new FileWriter(dataset_file, true);
                // DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd
                // HH:mm:ss.SSS");
                // LocalDateTime localDateTime = LocalDateTime.now();

                // String format = fmt.format(localDateTime);
                writer.write(cycle + ";" + entry.getValue() + "\n");
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(
                        String.format("Could not create the output file at %s", dataset_file.toPath().toString()));
            }
        }
    }

    // Execute the steps which were composed by the planner if applicable
    void execution() {

        Set<Mote> motesEffected = new HashSet<>();

        // Execute the planning steps, and keep track of the motes that will need
        // changing
        for (PlanningStep step : steps) {
            Link link = step.link;
            Mote mote = currentConfiguration.system.motes.get(link.getSource());

            if (step.step == Step.CHANGE_POWER) {
                findLink(mote, link.getDestination()).setPower(step.value);
            } else if (step.step == Step.CHANGE_DIST) {
                findLink(mote, link.getDestination()).setDistribution(step.value);
            }
            motesEffected.add(mote);
        }

        List<LinkSettings> newSettings;

        for (Mote mote : motesEffected) {

            newSettings = new LinkedList<LinkSettings>();

            for (Link link : mote.getLinks()) {

                // add a new linksettings object containing the source mote id, the dest id, the
                // (new) power of the link,
                // the (new) distribution of the link and the link spreading as zero to the
                // newsetting list.
                newSettings.add(newLinkSettings(mote.getMoteId(), link.getDestination(), link.getPower(),
                        link.getDistribution(), 0));
            }

            // Here you push the changes for the mote to the actual network via the effector
            effector.setMoteSettings(mote.getMoteId(), newSettings);
        }

        steps.clear();

        // System.out.print(";" + System.currentTimeMillis());

        /*
         * if (!timeInReadableFormat) {
         * System.out.print(";" + System.currentTimeMillis());
         * } else {
         * LocalDateTime now = LocalDateTime.now();
         * System.out.print("; " + String.format("%02d:%02d:%02d",
         * now.getHour(), now.getMinute(), now.getSecond()));
         * }
         */
    }

    // Returns the link from mote to dest
    Link findLink(Mote mote, int dest) {
        for (Link link : mote.getLinks()) {
            if (link.getDestination() == dest)
                return link;
        }
        throw new RuntimeException(String.format("Link %d --> %d not found", mote.getMoteId(), dest));
    }

    // returns a link settings object with the given parameters as arguments.
    public LinkSettings newLinkSettings(int src, int dest, int power, int distribution, int sf) {
        LinkSettings settings = new LinkSettings();
        settings.setSrc(src);
        settings.setDest(dest);
        settings.setPowerSettings(power);
        settings.setDistributionFactor(distribution);
        settings.setSpreadingFactor(sf);
        return settings;
    }

    // dont know where this get used
    void printMote(Mote mote) {
        System.out.println(String.format("MoteId: %d, BatteryRemaining: %f, Links:%s", mote.getMoteId(),
                mote.getEnergyLevel(), getLinkString(mote.getLinks())));
    }

    // dont know where this gets used
    String getLinkString(List<Link> links) {
        StringBuilder strBuilder = new StringBuilder();
        for (Link link : links) {
            strBuilder.append(String.format("[Dest: %d, Power:%d, DistributionFactor:%d]", link.getDestination(),
                    link.getPower(), link.getDistribution()));
        }
        return strBuilder.toString();
    }
}
