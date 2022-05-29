#/usr/bin/bash


java -jar deltaiot_simulator.jar 5523 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_5523.txt
java -jar deltaiot_simulator.jar 5524 -randomjam 30 120 -50 630 420 70 20 false false ARIMA > ./../output/deltaiotv1/details/randomjam/details_jam_run5524_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_percentageActive_70_percentageBlock_20_mitigation_false_rssiconfirmation_false_mitigationMethod_ARIMA.txt
#
#mitigation

java -jar deltaiot_simulator.jar 5536 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_5536.txt
java -jar deltaiot_simulator.jar 5537 -randomjam 30 120 -50 630 420 70 20 true true ARIMA > ./../output/deltaiotv1/details/randomjam/details_jam_run5537_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_percentageActive_70_percentageBlock_20_mitigation_true_rssiconfirmation_true_mitigationMethod_ARIMA.txt


