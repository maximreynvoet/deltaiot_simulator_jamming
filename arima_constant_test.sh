#/usr/bin/bash

java -jar deltaiot_simulator.jar 44125 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_44125.txt

java -jar deltaiot_simulator.jar 44126 -constantjam 30 120 -50 630 420   false false ARIMA > ./../output/deltaiotv1/details/constantjam/details_jam_run44126_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_false_rssiconfirmation_false_mitigationMethod_ARIMA.txt

java -jar deltaiot_simulator.jar 44129 standard ARIMA > ./../output/deltaiotv1/details/standard/standard_44129.txt

java -jar deltaiot_simulator.jar 44130 -constantjam 30 120 -50 630 420   true true ARIMA > ./../output/deltaiotv1/details/constantjam/details_jam_run44130_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_true_rssiconfirmation_true_mitigationMethod_ARIMA.txt
