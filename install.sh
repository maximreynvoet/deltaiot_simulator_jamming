#/usr/bin/bash

rm -rf ./../output
mkdir ./../output
mkdir ./../output/deltaiotv1
mkdir ./../output/deltaiotv1/details
mkdir ./../output/deltaiotv1/details/standard
mkdir ./../output/deltaiotv1/details/reactivejam
mkdir ./../output/deltaiotv1/details/constantjam
mkdir ./../output/deltaiotv1/details/randomjam
mkdir ./../output/deltaiotv1/standard
mkdir ./../output/deltaiotv1/reactivejam
mkdir ./../output/deltaiotv1/constantjam
mkdir ./../output/deltaiotv1/randomjam


YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}deltaiot_simulator successfully installed${NC}"

echo -e "Make sure ${YELLOW} jamming_mitigation_with_SAS-main ${NC} is installed and running at default port : http://127.0.0.1:8000"

echo -e "Try running an example script , e.g. by typing ./arima_random_test.sh"
