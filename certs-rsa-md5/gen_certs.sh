#!/bin/sh -x
# Copyright (c) Secure Sky Technology Inc. All rights reserved.

export OPENSSL_ENABLE_MD5_VERIFY=1

./clean_certs.sh

# create test root ca
openssl genrsa 2048 > testca_rsa2048.pem
openssl req -config testca_conf_req.txt -new -x509 -md5 -days 3650 -key testca_rsa2048.pem > testca.crt

echo -n "" > index.txt
echo -n "01" > serial.txt
CERTS_OUTDIR=`pwd`/certs
mkdir -p $CERTS_OUTDIR

for bitlen in 512 768 1024 2048; do
  openssl genrsa ${bitlen} > rsa${bitlen}.pem
  openssl pkcs8 -topk8 -inform PEM -outform DER -in rsa${bitlen}.pem -out rsa${bitlen}_pkcs8.der -nocrypt
  openssl req -new -md5 -days 365 -key rsa${bitlen}.pem -subj "/C=JP/ST=Tokyo/L=Chiyoda/O=SST/OU=test/CN=md5.rsa${bitlen}.localdomain/emailAddress=md5.rsa${bitlen}@localhost.localdomain" -out rsa${bitlen}.csr
  openssl ca -config testca_conf_ca.txt -in rsa${bitlen}.csr -batch -outdir $CERTS_OUTDIR -out rsa${bitlen}.cer
  openssl x509 -in rsa${bitlen}.cer -noout -text
done
