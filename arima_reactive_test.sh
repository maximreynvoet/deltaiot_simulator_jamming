#/usr/bin/bash


java -jar deltaiot_simulator.jar 0123 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_0123.txt
java -jar deltaiot_simulator.jar 8124 -reactivejam 30 120 -50 630 420   false false ARIMA > ./../output/deltaiotv1/details/reactivejam/details_jam_run8124_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_false_rssiconfirmation_false_mitigationMethod_ARIMA.txt

#

java -jar deltaiot_simulator.jar 9136 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_9136.txt
java -jar deltaiot_simulator.jar 9137 -reactivejam 30 120 -50 630 420   true true ARIMA > ./../output/deltaiotv1/details/reactivejam/details_jam_run9137_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_true_rssiconfirmation_true_mitigationMethod_ARIMA.txt
