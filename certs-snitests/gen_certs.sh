#!/bin/sh -x
# Copyright (c) Secure Sky Technology Inc. All rights reserved.

./clean_certs.sh

# create test root ca
openssl genrsa 2048 > testca_rsa2048.pem
openssl req -config testca_conf_req.txt -new -x509 -sha256 -days 3650 -key testca_rsa2048.pem > testca.crt

echo -n "" > index.txt
echo -n "01" > serial.txt
CERTS_OUTDIR=`pwd`/certs
mkdir -p $CERTS_OUTDIR

openssl genrsa 2048 > rsa2048.pem
openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa2048.pem -out rsa2048_pkcs8.der -nocrypt

openssl req -new -sha256 -days 365 -key rsa2048.pem \
  -subj "/C=JP/ST=Tokyo/L=Chiyoda/O=SST/OU=test/CN=snitest1/emailAddress=snitest1.sha2.rsa2048@localhost.localdomain" \
  -out rsa2048-snitest1.csr
openssl ca -config testca_conf_ca.txt -in rsa2048-snitest1.csr -batch -outdir $CERTS_OUTDIR -out rsa2048-snitest1.cer

openssl req -new -sha256 -days 365 -key rsa2048.pem \
  -subj "/C=JP/ST=Tokyo/L=Chiyoda/O=SST/OU=test/CN=snitest2/emailAddress=snitest2.sha2.rsa2048@localhost.localdomain" \
  -out rsa2048-snitest2.csr
openssl ca -config testca_conf_ca.txt -in rsa2048-snitest2.csr -batch -outdir $CERTS_OUTDIR -out rsa2048-snitest2.cer

openssl req -new -sha256 -days 365 -key rsa2048.pem \
  -subj "/C=JP/ST=Tokyo/L=Chiyoda/O=SST/OU=test/CN=snitest3/emailAddress=snitest3.sha2.rsa2048@localhost.localdomain" \
  -out rsa2048-snitest3.csr
openssl ca -config testca_conf_ca.txt -in rsa2048-snitest3.csr -batch -outdir $CERTS_OUTDIR -out rsa2048-snitest3.cer

