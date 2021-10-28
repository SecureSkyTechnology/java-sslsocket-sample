# TLSv1.3 検証用の Apache HTTPD docker イメージ

Java から TLSv1.3 での接続を検証するため、TLSv1.3 およびそれ用の ciphersuite 「のみ」サポートする Apache HTTPD の docker イメージ。

## Apache HTTPD オフィシャルイメージからのカスタマイズ

ベースイメージには [Apache HTTPD オフィシャルイメージ](https://hub.docker.com/_/httpd)の debian 11 (bullseye) ベースを使用。
(alpineも提供されているが、muslとglibcの違いでトラブルが発生したときの対処が大変そうなので、素直に debian 11 (bullseye) ベースを採用)

1. `$ docker pull httpd:2.4.51-bullseye`
2. まずオフィシャルイメージをそのまま起動し、内部を調査する。

```
$ docker run --rm -dit --name h01 -p 8081:80 httpd:2.4.51-bullseye

$ curl http://localhost:8081/
<html><body><h1>It works!</h1></body></html>

$ docker exec -it h01 /bin/sh -c "dpkg -l | grep ssl"
ii  libssl1.1:amd64           1.1.1k-1+deb11u1               amd64        Secure Sockets Layer toolkit - shared libraries
ii  libzstd1:amd64            1.4.8+dfsg-2.1                 amd64        fast lossless compression algorithm
```

→ `libssl1.1` の詳細バージョンから OpenSSL 1.1.1k を使っているものと推測され、このバージョンであればTLSv1.3に対応していると思われる。

3. 元の設定ファイルをエキスポートする。

```
$ docker exec -it h01 cat /usr/local/apache2/conf/httpd.conf > httpd.conf-base

$ docker exec -it h01 cat /usr/local/apache2/conf/extra/httpd-ssl.conf > httpd-ssl.conf-base
```

4. `httpd.conf` のカスタマイズ

```
$ cp httpd.conf-base httpd.conf-sslenabled

$ vim httpd.conf-sslenabled
(httpdオフィシャルイメージの説明にあるとおりSSLを有効化)
```

5. `httpd-ssl.conf` のカスタマイズ
   1. `SSLProtocol TLSv1.3` に変更
   2. `SSLCipherSuite AES128-GCM-SHA256:AES256-GCM-SHA384` に変更し、TLSv1.3で導入されたcipherだけに可能な限り絞る(後述)

```
$ cp httpd-ssl.conf-base httpd-ssl.conf-tlsv1_3-only

$ vim httpd-ssl.conf-tlsv1_3-only
```

6. サーバ用秘密鍵と自己署名証明書を作成

```
$ cd ../certs-rsa-sha2/
$ ./gen_certs.sh
$ cp rsa2048.pem ../docker-httpd-tlsv1_3-only/server-key-rsa2048.pem
$ cp rsa2048.cer  ../docker-httpd-tlsv1_3-only/server-crt-rsa2048-sha2.pem
```

7. ビルドと動作確認 → curl で TLSv1.3 で接続成功

```
$ docker build -t httpd-tlsv1_3 .

$ docker run --rm -dit --name demo2 -p 8082:80 -p 8083:443 httpd-tlsv1_3


$ curl -k -v https://localhost:8083/
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8083 (#0)
* ALPN, offering h2
* ALPN, offering http/1.1
* successfully set certificate verify locations:
*   CAfile: /etc/pki/tls/certs/ca-bundle.crt
  CApath: none
* TLSv1.3 (OUT), TLS handshake, Client hello (1):
* TLSv1.3 (IN), TLS handshake, Server hello (2):
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, Encrypted Extensions (8):
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, Certificate (11):
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, CERT verify (15):
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, Finished (20):
* TLSv1.3 (OUT), TLS change cipher, Change cipher spec (1):
* TLSv1.3 (OUT), TLS handshake, [no content] (0):
* TLSv1.3 (OUT), TLS handshake, Finished (20):
* SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384
* ALPN, server accepted to use http/1.1
* Server certificate:
*  subject: C=JP; ST=Tokyo; O=SST; OU=test; CN=sha2.rsa2048.localdomain; emailAddress=sha2.rsa2048@localhost.localdomain
*  start date: Oct 28 02:19:55 2021 GMT
*  expire date: Oct 26 02:19:55 2031 GMT
*  issuer: C=JP; ST=Tokyo; O=Test Root CA; CN=Test CA; emailAddress=testca@localhost.localdomain
*  SSL certificate verify result: unable to get local issuer certificate (20), continuing anyway.
* TLSv1.3 (OUT), TLS app data, [no content] (0):
> GET / HTTP/1.1
> Host: localhost:8083
> User-Agent: curl/7.61.1
> Accept: */*
>
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* TLSv1.3 (IN), TLS handshake, [no content] (0):
* TLSv1.3 (IN), TLS handshake, Newsession Ticket (4):
* TLSv1.3 (IN), TLS app data, [no content] (0):
< HTTP/1.1 200 OK
< Date: Thu, 28 Oct 2021 03:06:36 GMT
< Server: Apache/2.4.51 (Unix) OpenSSL/1.1.1k
< Last-Modified: Mon, 11 Jun 2007 18:53:14 GMT
< ETag: "2d-432a5e4a73a80"
< Accept-Ranges: bytes
< Content-Length: 45
< Content-Type: text/html
<
<html><body><h1>It works!</h1></body></html>
* Connection #0 to host localhost left intact
```

8. [testssl.sh](https://testssl.sh/) からのプロトコルバージョンとcipher一覧のチェック

```
$ docker pull drwetter/testssl.sh:3.0

$ docker run --rm -it drwetter/testssl.sh:3.0 -p -E (dockerホスト自体のIPアドレス):8083

###########################################################
    testssl.sh       3.0.6 from https://testssl.sh/

      This program is free software. Distribution and
             modification under GPLv2 permitted.
      USAGE w/o ANY WARRANTY. USE IT AT YOUR OWN RISK!

       Please file bugs @ https://testssl.sh/bugs/

###########################################################

 Using "OpenSSL 1.0.2-chacha (1.0.2k-dev)" [~183 ciphers]
 on e58700258ebd:/home/testssl/bin/openssl.Linux.x86_64
 (built: "Jan 18 17:12:17 2019", platform: "linux-x86_64")


 Start 2021-10-28 03:21:12        -->> 10.0.2.15:8083 (10.0.2.15) <<--

 rDNS (10.0.2.15):       ip-10-0-2-15.ap-northeast-1.compute.internal.
 10.0.2.15:8083 appears to support TLS 1.3 ONLY. You better use --openssl=<path_to_openssl_supporting_TLS_1.3>
 Type "yes" to proceed and accept all scan problems --> yes
 Service detected:       Couldn't determine what's running on port 8083, assuming no HTTP service => skipping all HTTP checks


 Testing protocols via sockets except NPN+ALPN

 SSLv2      not offered (OK)
 SSLv3      not offered (OK)
 TLS 1      not offered
 TLS 1.1    not offered
 TLS 1.2    not offered
 TLS 1.3    offered (OK): final
 NPN/SPDY   not offered
 ALPN/HTTP2 not offered

 Testing ciphers per protocol via OpenSSL plus sockets against the server, ordered by encryption strength

Hexcode  Cipher Suite Name (OpenSSL)       KeyExch.   Encryption  Bits     Cipher Suite Name (IANA/RFC)
-----------------------------------------------------------------------------------------------------------------------------
SSLv2
SSLv3
TLS 1
TLS 1.1
TLS 1.2
TLS 1.3
 x1302   TLS_AES_256_GCM_SHA384            ECDH 253   AESGCM      256      TLS_AES_256_GCM_SHA384
 x1303   TLS_CHACHA20_POLY1305_SHA256      ECDH 253   ChaCha20    256      TLS_CHACHA20_POLY1305_SHA256
 x1301   TLS_AES_128_GCM_SHA256            ECDH 253   AESGCM      128      TLS_AES_128_GCM_SHA256

 Done 2021-10-28 03:21:43 [  34s] -->> 10.0.2.15:8083 (10.0.2.15) <<--
```

→ TLSv1.3 および想定したciphersuiteのみ検出された。

### SSLCipherSuite について

`SSLCipherSuite` は Apache HTTPD の公式マニュアルの解説もあるが、OpenSSL側の `openssl ciphers` コマンドの解説の方がより詳しい。

- [Apache HTTPD の公式マニュアル](https://httpd.apache.org/docs/2.4/mod/mod_ssl.html#sslciphersuite)
- [OpenSSL の ciphers コマンド](https://www.openssl.org/docs/man1.0.2/man1/ciphers.html)

TLSv1.3 で整理・導入された cipher suite については以下の解説記事が参考になる。

- `SSL/TLSとは何なんだ？ 今こそ知ってもらいたいSSL/TLSのお話 〜 2回目 〜 TLS1.3 HTTP/2 のお話 | さくらのナレッジ`
  - https://knowledge.sakura.ad.jp/21470/

実際に手元の CentOS Stream 8 環境で `openssl ciphers` コマンドを実行した結果:

```
$ openssl version
OpenSSL 1.1.1k  FIPS 25 Mar 2021

$ openssl ciphers -V -tls1_3 | grep TLSv1.3
          0x13,0x02 - TLS_AES_256_GCM_SHA384  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(256) Mac=AEAD
          0x13,0x03 - TLS_CHACHA20_POLY1305_SHA256 TLSv1.3 Kx=any      Au=any  Enc=CHACHA20/POLY1305(256) Mac=AEAD
          0x13,0x01 - TLS_AES_128_GCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(128) Mac=AEAD
          0x13,0x04 - TLS_AES_128_CCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESCCM(128) Mac=AEAD
```

このうち、[Java11の標準アルゴリズム名に記載されているもの](https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#jsse-cipher-suite-names)は以下の2つ。

```
0x13,0x01 - TLS_AES_128_GCM_SHA256
0x13,0x02 - TLS_AES_256_GCM_SHA384
```

なるべくこれだけになるよう試行錯誤して絞り込んだ結果が、以下となる。

```
$ openssl ciphers -V -v "AES128-GCM-SHA256:AES256-GCM-SHA384"
          0x13,0x02 - TLS_AES_256_GCM_SHA384  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(256) Mac=AEAD
          0x13,0x03 - TLS_CHACHA20_POLY1305_SHA256 TLSv1.3 Kx=any      Au=any  Enc=CHACHA20/POLY1305(256) Mac=AEAD
          0x13,0x01 - TLS_AES_128_GCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESGCM(128) Mac=AEAD
          0x13,0x04 - TLS_AES_128_CCM_SHA256  TLSv1.3 Kx=any      Au=any  Enc=AESCCM(128) Mac=AEAD
          0x00,0x9C - AES128-GCM-SHA256       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(128) Mac=AEAD
          0x00,0x9D - AES256-GCM-SHA384       TLSv1.2 Kx=RSA      Au=RSA  Enc=AESGCM(256) Mac=AEAD
```

TLSv1.2 用のが混ざるが、 `SSLProtocol` の方でTLSv1.3だけに絞っているので問題ないと思われる。

## Java11 から TLSv1.3 接続を確認

1. `-p 8083:443` を付けて本コンテナを起動

```
$ pushd docker-httpd-tlsv1_3-only/
$ docker build -t httpd-tlsv1_3 .
$ docker run --rm -dit --name demo2 -p 8082:80 -p 8083:443 httpd-tlsv1_3
$ popd
```

2. openjdk 11 でビルド

```
$ java -version
openjdk version "11.0.12" 2021-07-20 LTS
OpenJDK Runtime Environment 18.9 (build 11.0.12+7-LTS)
OpenJDK 64-Bit Server VM 18.9 (build 11.0.12+7-LTS, mixed mode, sharing)

$ ./mvnw package
```

3. `-protocols TLSv1.3` で TLSv1.3 に限定してGETアクセス

```
$ java -jar target/java-sslsocket-sample-1.0.jar -trustall -protocols TLSv1.3 https_get https://localhost:8083/
2021-10-28 14:07:14,713 [main] INFO sst.sslsocket.sample.CliMain ## - SSLParameters from command line options: CliSSLParameters [protocols=[TLSv1.3], ciphers=[], cipherorder=false, servername=]
SSLParameters (SSLSocket updated):
  Protocols[0]: TLSv1.3
  useCipherSuitesOrder: [false]
  CipherSuites[0]: TLS_AES_128_GCM_SHA256
  CipherSuites[1]: TLS_AES_256_GCM_SHA384
(...)
  CipherSuites[26]: TLS_EMPTY_RENEGOTIATION_INFO_SCSV
  endpointIdentificationAlgorithm: [null]
  needClientAuth: [false]
  wantClientAuth: [false]
2021-10-28 14:07:15,198 [main] INFO sst.sslsocket.sample.app.HttpsGetClient ## - HTTPS GET Client connect to https://localhost:8083/
-----------send>>
GET / HTTP/1.1
Host: localhost
Connection: close


-----------send<<
-----------recv>>
HTTP/1.1 200 OK
Date: Thu, 28 Oct 2021 05:07:15 GMT
Server: Apache/2.4.51 (Unix) OpenSSL/1.1.1k
Last-Modified: Mon, 11 Jun 2007 18:53:14 GMT
ETag: "2d-432a5e4a73a80"
Accept-Ranges: bytes
Content-Length: 45
Connection: close
Content-Type: text/html

<html><body><h1>It works!</h1></body></html>
-----------recv<<
2021-10-28 14:07:15,572 [main] INFO sst.sslsocket.sample.app.HttpsGetClient ## - HTTPS GET Client disconnected
```

→ TLSv1.3 で接続できた。

JavaのSSL/TLS実装の内部処理については、 `javax.net.debug` システムプロパティを指定するとデバッグprintが有効になる。

- [JavaSE 11 -> セキュリティ開発者ガイド -> 8. Java Secure Socket Extension (JSSE)リファレンス・ガイド -> JSSEのトラブルシューティング -> デバッグ・ユーティリティ](https://docs.oracle.com/javase/jp/11/security/java-secure-socket-extension-jsse-reference-guide.html#GUID-31B7E142-B874-46E9-8DD0-4E18EC0EB2CF)

```
$ java -Djavax.net.debug=ssl:handshake -jar target/java-sslsocket-sample-1.0.jar -trustall -protocols TLSv1.3 https_get https://localhost:8083/
```

→ 実際の出力は省略するが、想定通り TLSv1.3 でハンドシェイクが進む様子を確認できた。

