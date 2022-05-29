package domain;

// import java.util.ArrayList;
import java.util.HashMap;
// import java.util.List;


public class ChannelConfiguration {
    private String ChannelConfigMode;
    
    private HashMap<String, Double> ChannelConfig;

    public String getChannelConfigMode() {
        return ChannelConfigMode;
    }

    
    public void setChannelConfigMode(String channelConfigMode) {
        ChannelConfigMode = channelConfigMode;
        // need to set corresponding ChannelConfig
        //first clear current
        ChannelConfig.clear();
        switch (ChannelConfigMode) {
            case "SINGLE":
                ChannelConfig.put("CH_13_868", 866.10);
                break;
            case "BASIC":
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                break;
            case "INTERMEDIATE" :
                ChannelConfig.put("CH_11_868", 865.50);
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                ChannelConfig.put("CH_15_868", 866.70);
                break;
            case "FULL" :
                ChannelConfig.put("CH_10_868", 865.20);
                ChannelConfig.put("CH_11_868", 865.50);
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                ChannelConfig.put("CH_15_868", 866.70);
                ChannelConfig.put("CH_16_868", 867.00);
                ChannelConfig.put("CH_17_868", 868.00);
                break;


        }

    }

    public HashMap<String, Double> getChannelConfig() {
        return ChannelConfig;
    }

    public void setChannelConfig(HashMap<String, Double> channelConfig) {
        ChannelConfig = channelConfig;
    }

    public ChannelConfiguration(String channelConfigMode) {
        ChannelConfigMode = channelConfigMode;
        ChannelConfig = new HashMap<String, Double>();
        switch (ChannelConfigMode) {
            case "SINGLE":
                ChannelConfig.put("CH_13_868", 866.10);
                break;
            case "BASIC":
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                break;
            case "INTERMEDIATE" :
                ChannelConfig.put("CH_11_868", 865.50);
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                ChannelConfig.put("CH_15_868", 866.70);
                break;
            case "FULL" :
                ChannelConfig.put("CH_10_868", 865.20);
                ChannelConfig.put("CH_11_868", 865.50);
                ChannelConfig.put("CH_12_868", 865.80);
                ChannelConfig.put("CH_13_868", 866.10);
                ChannelConfig.put("CH_14_868", 866.40);
                ChannelConfig.put("CH_15_868", 866.70);
                ChannelConfig.put("CH_16_868", 867.00);
                ChannelConfig.put("CH_17_868", 868.00);
                break;


        }
    }

   
}
