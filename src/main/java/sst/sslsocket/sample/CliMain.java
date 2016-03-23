/*
 * Copyright (c) 2016, Secure Sky Technology Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package sst.sslsocket.sample;

import java.net.URL;

import javax.net.ssl.KeyManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sst.sslsocket.sample.app.EchoSSLClient;
import sst.sslsocket.sample.app.EchoSSLServer;
import sst.sslsocket.sample.app.HttpsGetClient;

/**
 * サンプルアプリケーションのブートクラス
 * 
 * @author Secure Sky Technology Inc.
 */
public class CliMain {

    public static void main(String[] args) throws Exception {
        new CliMain().run(args);
    }

    final Options options = new Options();

    /**
     * コンストラクタの処理でオプション設定を初期化する。
     */
    public CliMain() {
        options.addOption(Option
            .builder("protocols")
            .hasArg()
            .argName("SSLv3,TLSv1,TLSv1.1,TLSv1.2")
            .desc("passed to SSLParameters.setProtocols()")
            .build());
        options.addOption(Option
            .builder("ciphers")
            .hasArg()
            .argName("file")
            .desc("passed to SSLParameters.setCipherSuites()")
            .build());
        options.addOption(Option
            .builder("cipherorder")
            .type(Boolean.class)
            .hasArg()
            .argName("true/false")
            .desc("set SSLParameters.setUseCipherSuitesOrder() (for echo_server)")
            .build());
        options.addOption(Option
            .builder("servername")
            .hasArg()
            .argName("hostname(not ip address)")
            .desc("specify SNI server name (for echo_client and https_get)")
            .build());
        options.addOption(Option
            .builder("servercert")
            .hasArg()
            .argName("file")
            .desc("server certificate der file (for echo_client and https_get)")
            .build());
        options.addOption(Option
            .builder("trustall")
            .desc("TEST PURPOSE ONLY. (for echo_client and https_get) (override servercert option)")
            .build());
        options.addOption(Option
            .builder("keyalgo")
            .hasArg()
            .argName("RSA/DSA/EC")
            .desc("KeyFactory Algorithm, see JSSE provider document. default:RSA (for echo_server)")
            .build());
        options.addOption(Option
            .builder("hostheader")
            .hasArg()
            .argName("hostname")
            .desc("Host request header value(optional, url is used by default. only for https_get)")
            .build());
        options.addOption(Option.builder("h").longOpt("help").desc("show this message").build());
    }

    /**
     * コマンドライン引数を解析してオプション指定を読み取り、サンプルアプリケーションを実行する。
     * 
     * @param args
     * @throws Exception
     */
    public void run(String[] args) throws Exception {
        final Logger logger = LoggerFactory.getLogger(this.getClass());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        CliSSLParameters clisp = new CliSSLParameters();
        String cliKeyAlgo = "RSA";
        String cliHostHeader = "";
        String clientVerifyServerCertPemFile = "";
        boolean trustAllServerCert = false;
        if (cmd.hasOption("protocols")) {
            String protocols = cmd.getOptionValue("protocols");
            clisp.protocols = protocols.split(",");
        }
        if (cmd.hasOption("ciphers")) {
            clisp.loadCiphersFromFile(cmd.getOptionValue("ciphers"));
        }
        if (cmd.hasOption("cipherorder")) {
            clisp.cipherOrder = Boolean.parseBoolean(cmd.getOptionValue("cipherorder"));
        }
        if (cmd.hasOption("servername")) {
            clisp.serverName = cmd.getOptionValue("servername");
        }
        if (cmd.hasOption("keyalgo")) {
            cliKeyAlgo = cmd.getOptionValue("keyalgo", "RSA");
        }
        if (cmd.hasOption("hostheader")) {
            cliHostHeader = cmd.getOptionValue("hostheader");
        }
        if (cmd.hasOption("servercert")) {
            clientVerifyServerCertPemFile = cmd.getOptionValue("servercert");
        }
        if (cmd.hasOption("trustall")) {
            trustAllServerCert = true;
        }
        logger.info("SSLParameters from command line options: {}", clisp.toString());
        if (cmd.hasOption("h")) {
            usage();
        }

        String[] remainArgs = cmd.getArgs();
        if (remainArgs.length < 1) {
            usage();
        }
        String app = remainArgs[0];
        switch (remainArgs[0]) {
        case "echo_server":
            if (remainArgs.length < 4) {
                usage();
            }
            int listenPort = Integer.parseInt(remainArgs[1]);
            KeyManager[] keyManagers = TinyUtils.loadAndCreateKeyManagers(remainArgs[2], remainArgs[3], cliKeyAlgo);
            new EchoSSLServer(clisp).run(listenPort, keyManagers);
            break;
        case "echo_client":
            if (remainArgs.length < 2) {
                usage();
            }
            final String[] host_port = remainArgs[1].split(":");
            if (host_port.length < 2) {
                throw new IllegalArgumentException("illegal host:port format [" + remainArgs[1] + "]");
            }
            EchoSSLClient echoClient;
            if (trustAllServerCert) {
                echoClient = new EchoSSLClient(clisp, true);
            } else if (!"".equals(clientVerifyServerCertPemFile)) {
                echoClient = new EchoSSLClient(clisp, clientVerifyServerCertPemFile);
            } else {
                echoClient = new EchoSSLClient(clisp);
            }
            echoClient.run(host_port[0], Integer.parseInt(host_port[1]));
            break;
        case "https_get":
            if (remainArgs.length < 2) {
                usage();
            }
            URL url = new URL(remainArgs[1]);
            String connectToHost = url.getHost();
            int connectToPort = url.getPort();
            if (-1 == connectToPort) {
                connectToPort = 443;
            }
            String hostHeader = cliHostHeader.isEmpty() ? connectToHost : cliHostHeader;
            HttpsGetClient httpsGetClient;
            if (trustAllServerCert) {
                httpsGetClient = new HttpsGetClient(clisp, true);
            } else if (!"".equals(clientVerifyServerCertPemFile)) {
                httpsGetClient = new HttpsGetClient(clisp, clientVerifyServerCertPemFile);
            } else {
                httpsGetClient = new HttpsGetClient(clisp);
            }
            httpsGetClient.run(remainArgs[1], connectToHost, connectToPort, url.getPath(), hostHeader);
            break;
        default:
            logger.error("Unknown app:[{}]", app);
            usage();
        }
    }

    /**
     * 使い方を標準出力に表示して、アプリケーションを終了する。
     */
    void usage() {
        StringBuilder usageHeader = new StringBuilder();
        usageHeader.append("\n");
        usageHeader.append("Java SSLSocket Sample Application\n");
        usageHeader.append("\n");
        usageHeader.append("app:\n");
        usageHeader.append("echo_server <listening_port> <server_certificate:der> <server_key_file:pkcs8>\n");
        usageHeader.append("    -> run echo SSL server, listen 0.0.0.0:<listening_port>\n");
        usageHeader.append("echo_client <host>:<port>\n");
        usageHeader.append("    -> run echo SSL client, connect to <host>:<port>\n");
        usageHeader.append("https_get <url>\n");
        usageHeader.append("    -> send HTTPS GET request to <url>, receive response, then print response.\n");
        usageHeader.append("\n");
        usageHeader.append("SSL options (see javax.net.ssl.SSLParameters javadoc:\n");
        String usageFooter = "\nCopyright (c) Secure Sky Technology Inc. All rights reserved.\n\n";
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(1024);
        formatter.printHelp(
            "java -jar java-sslsocket-sample-1.x.x.jar <app:echo_server|echo_client|https_get> <app args>",
            usageHeader.toString(),
            options,
            usageFooter,
            true);
        System.exit(1);
    }
}
