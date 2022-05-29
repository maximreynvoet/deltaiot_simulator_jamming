package util;

import mapek.Goal;
import smc.runmodes.SMCConnector;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import domain.SNREquation;

/**
 * Class Used to load the properties listed in the SMCConfig.properties file.
 */
public class ConfigLoader {

    public static final String configFileLocation = Paths.get(
            System.getProperty("user.dir"),
            "SMCConfig.properties"
    ).toString();

    private static ConfigLoader instance = null;
    private Properties properties;

    private ConfigLoader() {
        // Only load the properties file once (singleton pattern)
        properties = new Properties();
        try {
            InputStream inputStream = new FileInputStream(configFileLocation);
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Could not load the properties file correctly at location %f.", configFileLocation.toString())
            );
        }
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public String getProperty(String key) {
        String property = properties.getProperty(key);
        if (property != null) {
            return property.trim();
        } else {
            throw new RuntimeException(
                    String.format("Property '%f' not found in the properties file. Make sure this property is provided.", key)
            );
        }
    }

    public void setProperty(String key, String value) {
        
       
            this.properties.setProperty(key, value);
       
    }

    public HashMap<String,SNREquation> getSnrEquations() {
        HashMap<String, SNREquation> hsh = new HashMap<String, SNREquation> ();
        hsh.put("2_4" , new SNREquation(0.0169, 7.4076));
        hsh.put("3_1" , new SNREquation(0.4982, 1.2468));
        hsh.put("4_1" , new SNREquation(0.8282, -8.1246));
        hsh.put("5_9" , new SNREquation(0.4932, -2.4898)); // -4.4898
        hsh.put("6_4" , new SNREquation(0.6199, -4.8051)); // -9.8051
        hsh.put("7_3" , new SNREquation(0.5855, -2.644)); // -6.644
        hsh.put("7_2" , new SNREquation(0.5398, -2.0549));
        hsh.put("8_1" , new SNREquation(0.5298, -0.1031));
        hsh.put("9_1" , new SNREquation(0.8284, -7.2893));
        hsh.put("10_6" , new SNREquation(0.8219, -7.3331));
        hsh.put("10_5" , new SNREquation(0.6463, -3.0037));
        hsh.put("11_7" , new SNREquation(0.714, -3.1985));
        hsh.put("12_7" , new SNREquation(0.7254, -8.21)); // -16.12
        hsh.put("12_3" , new SNREquation(0.1, 6));
        hsh.put("13_11" , new SNREquation(0.6078, -3.6005));
        hsh.put("14_12" , new SNREquation(0.4886, -4.7704));
        hsh.put("15_12" , new SNREquation(0.5899, -5.1896)); // -7.1896
        return hsh;
        


        
    }
    public boolean getRssiConfirmation() {
        return Boolean.parseBoolean(this.getProperty("rssiConfirmation"));
    }
    public String getInitialKeys() {
        return (this.getProperty("initialKeys"));
    }

    public int getAmountOfLearningCycles() {
        return Integer.parseInt(this.getProperty("amountOfLearningCycles"));
    }
    
    public int getCurrentCycle() {
        return Integer.parseInt(this.getProperty("currentCycle"));
    }
    public int getAmountOfCycles() {
        return Integer.parseInt(this.getProperty("amountOfCycles"));
    }
    
    public int getStartJamCycle() {
        return Integer.parseInt(this.getProperty("startJamCycle"));
    }

    public int getStopJamCycle() {
        return Integer.parseInt(this.getProperty("stopJamCycle"));
    } 

    public Double getPowerJam() {
        return Double.parseDouble(this.getProperty("powerJam"));
    } 
    public Double getPosXJammer() {
        return Double.parseDouble(this.getProperty("pos_X_jammer"));
    } 
    public Double getPosYJammer() {
        return Double.parseDouble(this.getProperty("pos_Y_jammer"));
    } 
    public Double getRandActivePercentage() {
        return Double.parseDouble(this.getProperty("randActivePercentage"));
    } 
    public Double getRandSpreadPercentage() {
        return Double.parseDouble(this.getProperty("randSpreadPercentage"));
    } 
    public int getStartDriftCycle() {
        return Integer.parseInt(this.getProperty("startDriftCycle"));
    }

    public int getStopDriftCycle() {
        return Integer.parseInt(this.getProperty("stopDriftCycle"));
    } 

    public Double getMaxDriftRatio() {
        return Double.parseDouble(this.getProperty("maxDriftRatio"));
    } 

    public String getRunId() {
        return (this.getProperty("runId"));
    } 
    public String getMitigationChannelConfigMode() {
        return this.getProperty("mitigationChannelConfigMode").toUpperCase();
    }

    public String getMode() {
        return (this.getProperty("mode"));
    } 
    public boolean getMitigation() {
        return Boolean.parseBoolean(this.getProperty("mitigation"));
    }

    public int getDistributionGap() {
        return Integer.parseInt(this.getProperty("distributionGap"));
    }

    public int getTimeCap() {
        return Integer.parseInt(this.getProperty("cappedVerificationTime"));
    }

    public SMCConnector.Mode getRunMode() {
        return SMCConnector.Mode.getMode(this.getProperty("runMode").toLowerCase());
    }

    public String getSimulationNetwork() {
        return this.getProperty("simulationNetwork");
    }

    public boolean getVerificationTimeConstraint() {
        return Boolean.valueOf(this.getProperty("verificationTimeConstraint"));
    }
    //detectionMethod : deepAnt or ARIMA
    public String getDetectionMethod() {
        return (this.getProperty("detectionMethod"));
    } 
    

    public int getLearnerPort() {
        return Integer.valueOf(this.getProperty("learnerPort"));
    }
    // adding channelconfigMode
    public String getChannelConfigMode() {
        return this.getProperty("channelConfigMode").toUpperCase();
    } 

    public boolean getSingleChannelMode() {
        return Boolean.parseBoolean(this.getProperty("singleChannelMode"));
    }
    
    public List<Goal> getGoals() {
        List<Goal> goals = new ArrayList<>();

        String targets[] = this.getProperty("targets").split(",");
        String thressholds[] = this.getProperty("thressholds").split(",");
        String operators[] = this.getProperty("operators").split(",");

        for (int i = 0; i < targets.length; i++) {
            goals.add(new Goal(
                    targets[i].trim(), operators[i].trim(), Double.parseDouble(thressholds[i].trim())));
        }
        return goals;
    }

    /*public SMCConnector.TaskType getTaskType() {
        return SMCConnector.TaskType.getTaskType(this.getProperty("taskType").toLowerCase());
    }

    public double getExplorationPercentage() {
        return Double.parseDouble(this.getProperty("explorationPercentage"));
    }


    public boolean timeInReadableFormat() {
        return this.getProperty("readableTimeFormat").toLowerCase().equals("true");
    }

    public boolean shouldDeletePreviousModels() {
        return this.getProperty("deletePreviousModels").toLowerCase().equals("true");
    }
    */
}


