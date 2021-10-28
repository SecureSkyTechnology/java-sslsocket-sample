FROM httpd:2.4.51-bullseye
COPY httpd.conf-sslenabled /usr/local/apache2/conf/httpd.conf
COPY httpd-ssl.conf-tlsv1_3-only /usr/local/apache2/conf/extra/httpd-ssl.conf
COPY server-key-rsa2048.pem /usr/local/apache2/conf/server.key
COPY server-crt-rsa2048-sha2.pem /usr/local/apache2/conf/server.crt
