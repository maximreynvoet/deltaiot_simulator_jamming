package domain;

import simulator.FileHandler;

import java.util.List;

public class FileProfile implements Profile<Double> {

    private List<Double> values;
    private Double defaultValue;

    public Double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public FileProfile(String relPath, Double defaultValue) {
        this.values = FileHandler.parseNumberList(relPath);
        this.setDefaultValue(defaultValue);
    }

    @Override
    public Double get(int runNumber) {
        // if (runNumber >= 0 && runNumber < values.size()) {
        double val = values.get(runNumber % values.size());
        return val;
        // }
        // else {
        // System.out.println("Unknown value data for run " + runNumber + " returning
        // default (" + defaultValue + ").");
        // return defaultValue;
        // }
    }
}
