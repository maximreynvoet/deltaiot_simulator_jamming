# Jamming simulation in the DeltaIoT exemplar 

This version of DeltaIoT simulation software extends [previous versions](https://people.cs.kuleuven.be/~danny.weyns/software/DeltaIoT/).
The goal is to enable modeling 3 types of network jammers (constant, reactive and random) and to allow to position them within the network simulation.
Detection of jamming attacks and mitigation is developed using a MAPE-K architecture.

Detection was developed by Omid Gheibi and calls to FastAPI-based server allow the detection based on ARIMA and DeepAnT algorithms.

## Setup (Linux)

1. Install a java jdk, version 11 or above. We have developed and tested the software using "openjdk 11.0.15".

2. Unzip the software in a folder where you have write access to.

3. Install the detection software provided by Omid Gheibi that is included in the 'jamming_mitigation_with_SAS-main.zip'. 

4. Open a terminal, go to unzipped folder and type `./install.sh`.

5. Folder structure with topfolder "output" are created at same level as "deltaiot_simulator" folder.

6. Next, start the detection software (`uvicorn main:app --reload`). Make sure the service is listening on the default port http://127.0.0.1:8000.

7. Run one of the example scripts in the "deltaiot_simulator" folder (e.g. execute `./arima_random_test.sh`)

You can verify the output in the corresponding folders within outputfolder.

To change the configuration, the parameters are available in the SMCConfig.properties file.

## Testing
The most important parameters to change during testing:

### The channel config mode
(BASIC = 3 channels, INTERMEDIATE = 5 channels, FULL = 8 channels), will extend channel usage in case of jamming attack.

- channelConfigMode=SINGLE
- singleChannelMode=true
- mitigationChannelConfigMode=BASIC

To create your own scripts, make sure to have a run with the standard option in order to initialize ARIMA or DeepAnT :
e.g. 

`java -jar deltaiot_simulator.jar 58125 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_58125.txt`

The commandline parameters to provide per type (standard, reactive, random, constant) can easily be found in the code.
We provide some information below.


### Standard

3 parameters to be provided:

- param 1: runid; a runid, e.g. 001000, to identify the run
- param 2: standard; the value "standard" (without quotes) to be provided as as second parameter
- param 3: detectionmethod; value "ARIMA" or "DeepAnT" to be provided. Attention: case sensitive!

In our scripts, we redirect standard output to a details folder to have the output available for analysis

so example standard call: 

`java -jar deltaiot_simulator.jar 58125 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_58125.txt`


### Reactive

example reactive call:

`java -jar deltaiot_simulator.jar 9133 -reactivejam 30 120 -50 300 265   true true ARIMA > ./../output/deltaiotv1/details/reactivejam/details_jam_run9133_jamX300_jamY265_jamStart30_jamStop120_jamPower-50.0_mitigation_true_rssiconfirmation_true_mitigationMethod_ARIMA.txt`

- param 1: runid; a runid, e.g. 9133, to identify the run
- param 2: reactivejam; the value "-reactivejam" (without quotes) to be provided as as second parameter
- param 3: strStartJamCycle; e.g 30, identifies cycle the jamming starts
- param 4: strStopJamCycle; e.g. 120, identifies cycle the jamming stops
- param 5: strPowerJam; e.g. -50, negative value that impacts the SNR equation, represents the jam power (and would be correlated with transmission power jammer)
- param 6: strJammerX_Pos; e.g. 300; the X-coordinate within our deltaIot network (see map or code for location motes; direction is east to west
- param 7: strJammerY_Pos; e.g. 265; the Y-coordinate within our deltaIot network (see map or code for location motes; direction on map is north to south
- param 8: strMitigation; e.g. true; option whether or not to mitigate the attack using MAPE-K architecture. 
- param 9: strRssiConfirmation; e.g. true; option whether RSSI-confirmation is active
- param 10: strDetectionMethod; e.g ARIMA; option to choose detection algo, thus ARIMA or DeepAnT

### Constant

example constant call:

`java -jar deltaiot_simulator.jar 55126 -constantjam 30 120 -50 630 420   false false DeepAnT > ./../output/deltaiotv1/details/constantjam/details_jam_run55126_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_false_rssiconfirmation_false_mitigationMethod_DeepAnT.txt`

- param 1: runid; a runid, e.g. 55126, to identify the run
- param 2: -constantjam; the value "-constantjam" (without quotes) to be provided as as second parameter
- param 3: strStartJamCycle; e.g 30, identifies cycle the jamming starts
- param 4: strStopJamCycle; e.g. 120, identifies cycle the jamming stops
- param 5: strPowerJam; e.g. -50, negative value that impacts the SNR equation, represents the jam power (and would be correlated with transmission power jammer)
- param 6: strJammerX_Pos; e.g. 630; the X-coordinate within our deltaIot network (see map or code for location motes; direction is east to west
- param 7: strJammerY_Pos; e.g. 420; the Y-coordinate within our deltaIot network (see map or code for location motes; direction on map is north to south
- param 8: strMitigation; e.g. false; option whether or not to mitigate the attack using MAPE-K architecture. 
- param 9: strRssiConfirmation; e.g. false; option whether RSSI-confirmation is active
- param 10: strDetectionMethod; e.g ARIMA; option to choose detection algo, thus ARIMA or DeepAnT

### Random

example random call:

`java -jar deltaiot_simulator.jar 11124 -randomjam 30 120 -50 630 420 70 20 false false DeepAnT > ./../output/deltaiotv1/details/randomjam/details_jam_run11124_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_percentageActive_70_percentageBlock_20_mitigation_false_rssiconfirmation_false_mitigationMethod_DeepAnT.txt`

- param 1: runid; a runid, e.g. 11124, to identify the run
- param 2: -randomjam; the value "-randomjam" (without quotes) to be provided as as second parameter
- param 3: strStartJamCycle; e.g 30, identifies cycle the jamming starts
- param 4: strStopJamCycle; e.g. 120, identifies cycle the jamming stops
- param 5: strPowerJam; e.g. -50, negative value that impacts the SNR equation, represents the jam power (and would be correlated with transmission power jammer)
- param 6: strJammerX_Pos; e.g. 630; the X-coordinate within our deltaIot network (see map or code for location motes; direction is east to west
- param 7: strJammerY_Pos; e.g. 420; the Y-coordinate within our deltaIot network (see map or code for location motes; direction on map is north to south
- param 8: strRandActivePercentage; e;g. 70; the % of the time the random jammer is active
- param 9: strRandSpreadPercentage; e.g. 20; the % indicating spreading % of jammer active; e.g. 5 means 20 blocks (each blocks has jam & sleep period within)
- param 10: strMitigation; e.g. false; option whether or not to mitigate the attack using MAPE-K architecture. 
- param 11: strRssiConfirmation; e.g. false; option whether RSSI-confirmation is active
- param 12: strDetectionMethod; e.g ARIMA; option to choose detection algo, thus ARIMA or DeepAnT

## Scripts for the experiments in the paper

The scripts used in the paper are provided within the deltaiot_simulator folder.
Before executing them, make sure the "jamming_mitigation_with_SAS" service is running (http://127.0.0.1:8000).

1. Unzip the 'experiment_scripts.zip' archive.

2. Then open folder deltaiot_simulator in a terminal and launch the experiment scripts in the unzipped archive. E.g. :  `./ARIMA_constant_v1_0.sh`

3. The output is written in the outputfolder as described above.


