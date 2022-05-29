#/usr/bin/bash

java -jar deltaiot_simulator.jar 55125 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_55125.txt

java -jar deltaiot_simulator.jar 55126 -constantjam 30 120 -50 630 420   false false DeepAnT > ./../output/deltaiotv1/details/constantjam/details_jam_run55126_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_false_rssiconfirmation_false_mitigationMethod_DeepAnT.txt

java -jar deltaiot_simulator.jar 55129 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_55129.txt

java -jar deltaiot_simulator.jar 55130 -constantjam 30 120 -50 630 420   true true DeepAnT > ./../output/deltaiotv1/details/constantjam/details_jam_run55130_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_true_rssiconfirmation_true_mitigationMethod_DeepAnT.txt

