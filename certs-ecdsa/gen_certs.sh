#!/bin/sh -x
# Copyright (c) Secure Sky Technology Inc. All rights reserved.

./clean_certs.sh

# show supported ec curves
openssl ecparam -list_curves

# create test root ca (prime256v1)
openssl ecparam -genkey -name prime256v1 -out testca_ecp256v1.pem
openssl req -config testca_conf_req.txt -new -x509 -sha256 -days 3650 -key testca_ecp256v1.pem > testca.crt

echo -n "" > index.txt
echo -n "01" > serial.txt
CERTS_OUTDIR=`pwd`/certs
mkdir -p $CERTS_OUTDIR

for ecparam in secp384r1 secp521r1 prime256v1; do
  FILE_KEY=${ecparam}.pem
  FILE_KEY_DER=${ecparam}_pkcs8.der
  FILE_CSR=${ecparam}.csr
  FILE_CER=${ecparam}.cer
  openssl ecparam -genkey -name ${ecparam} -out ${FILE_KEY}
  openssl pkcs8 -topk8 -inform PEM -outform DER -in ${FILE_KEY} -out ${FILE_KEY_DER} -nocrypt
  openssl req -new -sha256 -days 365 -key ${FILE_KEY} \
    -subj "/C=JP/ST=Tokyo/L=Chiyoda/O=SST/OU=test/CN=sha2.${ecparam}.ecc.localdomain/emailAddress=sha2.${ecparam}.ecc@localhost.localdomain" \
    -out ${FILE_CSR}
  openssl ca -config testca_conf_ca.txt -in ${FILE_CSR} -batch -outdir $CERTS_OUTDIR -out ${FILE_CER}
  openssl x509 -in ${FILE_CER} -noout -text
done
