# java-sslsocket-sample

Java8のSSLSocket/SSLServerSocketの使い方を紹介するサンプルJavaアプリケーションです。SSL/TLS通信を使ったEchoクライアントとEchoサーバ、およびHTTPSでGETリクエストを送るだけのシンプルなHTTPSクライアントが含まれています。

[株式会社セキュアスカイ・テクノロジーのSSTtechblog](https://www.securesky-tech.com/column/techlog/index.html) の連載記事で詳しい使い方やソースコードの説明をしています。

 * SSTtechlog 05 JavaでSSL/TLS接続アプリケーションを作ってみよう(1)：SSL版Echoサーバ/クライアントの作成と脆弱性テスト(testssl.sh,Nmap)
   * http://www.securesky-tech.com/column/techlog/05.html

 * SSTtechblog 連載記事一覧 : https://www.securesky-tech.com/column/techlog/index.html

## requirements

- jdk >= 11

## reference

- Java 11 Security Developer's Guide
  - https://docs.oracle.com/en/java/javase/11/security/index.html
  - [8 Java Secure Socket Extension (JSSE) Reference Guide](https://docs.oracle.com/en/java/javase/11/security/java-secure-socket-extension-jsse-reference-guide.html)
- Java 11 セキュリティ開発者ガイド
  - https://docs.oracle.com/javase/jp/11/security/index.html
  - [8 Java Secure Socket Extension (JSSE)リファレンス・ガイド](https://docs.oracle.com/javase/jp/11/security/java-secure-socket-extension-jsse-reference-guide.html)

### debug options

- `-Djava.security.properties=<URL or file>`
  - Javaのセキュリティ・プロパティをカスタマイズ
  - see: `<java.home>/conf/security/java.security` or `<java.home>/jre/lib/security/java.security`
  - RHEL8/CentOS(-Stream-)8 系の場合は `-Djava.security.disableSystemPropertiesFile=true` を追加すること。 (see also : `update-crypto-policies(8)` )
- `java.security.debug` システムプロパティ
  - see: [java11 セキュリティ開発者ガイド, "1 一般的なセキュリティ" -> セキュリティのトラブルシューティング](https://docs.oracle.com/javase/jp/11/security/troubleshooting-security.html)
  - ↑には載っていないが、セキュリティプロパティ設定ファイルの読み込み状況をデバッグ出力する `properties` という値があり、動作した。
    - https://stackoverflow.com/questions/65327349/java-security-properties-changes-not-applied
  - 使用例 : `java -Djava.security.debug=properties ...`
- `javax.net.debug` システムプロパティ
  - see: [java11 セキュリティ開発者ガイド, "8 Java Secure Socket Extension (JSSE)リファレンス・ガイド" -> JSSEのトラブルシューティング -> デバッグ・ユーティリティ](https://docs.oracle.com/javase/jp/11/security/java-secure-socket-extension-jsse-reference-guide.html#GUID-31B7E142-B874-46E9-8DD0-4E18EC0EB2CF)
  - 使用例 : `java -Djavax.net.debug=ssl:handshake ...`

## changelog

- 2021-10
  - `mvn -N io.takari:maven:0.7.7:wrapper` 適用
  - java 11 以上に対応
  - TLSv1.3 対応
  - `dump_props`, `dump_ciphers` コマンド追加
