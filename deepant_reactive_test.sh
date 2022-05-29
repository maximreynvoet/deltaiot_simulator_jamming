#/usr/bin/bash


java -jar deltaiot_simulator.jar 0123 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_0123.txt
java -jar deltaiot_simulator.jar 6124 -reactivejam 30 120 -50 630 420   false false DeepAnT > ./../output/deltaiotv1/details/reactivejam/details_jam_run0624_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_false_rssiconfirmation_false_mitigationMethod_DeepAnT.txt


java -jar deltaiot_simulator.jar 7136 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_7136.txt
java -jar deltaiot_simulator.jar 7137 -reactivejam 30 120 -50 630 420   true true DeepAnT > ./../output/deltaiotv1/details/reactivejam/details_jam_run7137_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_mitigation_true_rssiconfirmation_true_mitigationMethod_DeepAnT.txt
