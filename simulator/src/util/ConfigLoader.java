package util;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
// import java.util.ArrayList;
// import java.util.List;
import java.util.Properties;

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
    
    public boolean getRssiConfirmation() {
        return Boolean.parseBoolean(this.getProperty("rssiConfirmation"));
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
    public int getStartDriftCycle() {
        return Integer.parseInt(this.getProperty("startDriftCycle"));
    }

    public int getStopDriftCycle() {
        return Integer.parseInt(this.getProperty("stopDriftCycle"));
    } 

    public Double getMaxDriftRatio() {
        return Double.parseDouble(this.getProperty("maxDriftRatio"));
    } 
    public Double getPosX() {
        return Double.parseDouble(this.getProperty("Jammer_X_Pos"));
    } 
    public Double getPosY() {
        return Double.parseDouble(this.getProperty("Jammer_Y_Pos"));
    } 

    public String getRunId() {
        return (this.getProperty("runId"));
    } 

    public String getMode() {
        return (this.getProperty("mode"));
    } 
    //detectionMethod : deepAnt or ARIMA
    public String getDetectionMethod() {
        return (this.getProperty("detectionMethod"));
    } 
    
    // adding channelconfigMode
    public String getChannelConfigMode() {
        return this.getProperty("channelConfigMode").toUpperCase();
    } 

    public boolean getSingleChannelMode() {
        return Boolean.parseBoolean(this.getProperty("singleChannelMode"));
    }
    public boolean getMitigation() {
        return Boolean.parseBoolean(this.getProperty("mitigation"));
    }
    public String getMitigationChannelConfigMode() {
        return this.getProperty("mitigationChannelConfigMode").toUpperCase();
    } 
    
    public int getDistributionGap() {
        return Integer.parseInt(this.getProperty("distributionGap"));
    }

    public int getTimeCap() {
        return Integer.parseInt(this.getProperty("cappedVerificationTime"));
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


