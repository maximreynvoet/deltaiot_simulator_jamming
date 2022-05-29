#/usr/bin/bash


java -jar deltaiot_simulator.jar 11123 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_11123.txt
java -jar deltaiot_simulator.jar 11124 -randomjam 30 120 -50 630 420 70 20 false false DeepAnT > ./../output/deltaiotv1/details/randomjam/details_jam_run11124_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_percentageActive_70_percentageBlock_20_mitigation_false_rssiconfirmation_false_mitigationMethod_DeepAnT.txt

java -jar deltaiot_simulator.jar 11136 standard DeepAnT > ./../output/deltaiotv1/details/standard/standard_11136.txt
java -jar deltaiot_simulator.jar 11137 -randomjam 30 120 -50 630 420 70 20 true true DeepAnT > ./../output/deltaiotv1/details/randomjam/details_jam_run11137_jamX630_jamY420_jamStart30_jamStop120_jamPower-50.0_percentageActive_70_percentageBlock_20_mitigation_true_rssiconfirmation_true_mitigationMethod_DeepAnT.txt


