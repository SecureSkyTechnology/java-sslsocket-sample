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
package sst.sslsocket.sample.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sst.sslsocket.sample.CliSSLParameters;
import sst.sslsocket.sample.TinyUtils;

/**
 * Echo クライアントアプリケーションのサンプルコード
 * 
 * @author Secure Sky Technology Inc.
 */
public class EchoSSLClient {

    final CliSSLParameters clisp;
    final boolean dontVerifyServerCert;
    final String serverCertPemFile;

    public EchoSSLClient(final CliSSLParameters clisp) {
        this.clisp = clisp;
        this.dontVerifyServerCert = false;
        this.serverCertPemFile = "";
    }

    public EchoSSLClient(final CliSSLParameters clisp, final boolean dontVerifyServerCert) {
        this.clisp = clisp;
        this.dontVerifyServerCert = dontVerifyServerCert;
        this.serverCertPemFile = "";
    }

    public EchoSSLClient(final CliSSLParameters clisp, final String serverCertPemFile) {
        this.clisp = clisp;
        this.dontVerifyServerCert = false;
        this.serverCertPemFile = serverCertPemFile;
    }

    /**
     * @param connectToHost 接続先ホスト名/IPアドレス
     * @param connectToPort 接続先ポート番号
     * @throws Exception
     */
    public void run(final String connectToHost, int connectToPort) throws Exception {
        final Logger logger = LoggerFactory.getLogger(this.getClass());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        if (this.dontVerifyServerCert) {
            // SSLContextを、証明書の検証を全てpassさせるテスト用のTrustManagerで初期化。
            sslContext.init(null, TinyUtils.createAllAllowTrustManagers(), null);
        } else if (!"".equals(this.serverCertPemFile)) {
            // SSLContextを、指定された証明書か検証するTrustManagerで初期化。
            sslContext.init(null, TinyUtils.createDebugPurposeTrustManagers(this.serverCertPemFile), null);
        } else {
            // 証明書検証をデフォルト処理に任せる方式で初期化。
            sslContext.init(null, null, null);
        }
        SSLSocket socket = (SSLSocket) sslContext.getSocketFactory().createSocket();
        SSLParameters copiedSSLParameters = socket.getSSLParameters();
        // SSLSocketのSSLParametersを、コマンドラインオプションに従い更新する。
        clisp.updateSSLParameters(copiedSSLParameters, true);
        TinyUtils.dumpSSLParameters(copiedSSLParameters, "SSLSocket updated");
        socket.setSSLParameters(copiedSSLParameters);

        try {
            socket.connect(new InetSocketAddress(connectToHost, connectToPort));
            logger.info("Echo SSL Client connect to {}, port={}", connectToHost, connectToPort);

            // 典型的な Echo クライアント処理
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader keyIn = new BufferedReader(new InputStreamReader(System.in));
            String input;
            while ((input = keyIn.readLine()).length() > 0) {
                // 標準入力から1行読み込んだら、接続先にそのまま送信する。
                out.println(input);
                // 接続先からの受信データを、標準出力にそのまま出力する。
                String line = in.readLine();
                if (line != null) {
                    System.out.println(line);
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Echo SSL Client I/O error", e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.error("Socket close error", e);
            }
            logger.info("Echo SSL Client disconnected");
        }
    }
}
