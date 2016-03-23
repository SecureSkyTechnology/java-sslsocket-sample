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
import java.net.Socket;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sst.sslsocket.sample.CliSSLParameters;
import sst.sslsocket.sample.TinyUtils;

/**
 * Echo サーバアプリケーションのサンプルコード
 * 
 * @author Secure Sky Technology Inc.
 */
public class EchoSSLServer {
    final CliSSLParameters clisp;

    public EchoSSLServer(final CliSSLParameters clisp) {
        this.clisp = clisp;
    }

    /**
     * @param port 待ち受け(listen)用ポート番号
     * @param keyManagers {@link javax.net.ssl.SSLContext#init(KeyManager[], javax.net.ssl.TrustManager[], java.security.SecureRandom)} に渡すKeyManagerインスタンスの配列
     * @throws Exception
     */
    public void run(int port, KeyManager[] keyManagers) throws Exception {
        final Logger logger = LoggerFactory.getLogger(this.getClass());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        // SSLContextを指定されたKeyManagerインスタンスの配列で初期化
        sslContext.init(keyManagers, null, null);
        SSLServerSocket serverSocket = (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket();
        SSLParameters copiedSSLParameters = serverSocket.getSSLParameters();
        // SSLServerSocketのSSLParametersを、コマンドラインオプションに従い更新する。
        clisp.updateSSLParameters(copiedSSLParameters, false);
        TinyUtils.dumpSSLParameters(copiedSSLParameters, "SSLServerSocket updated");
        serverSocket.setSSLParameters(copiedSSLParameters);
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));

        while (true) {
            try {
                Socket socket = serverSocket.accept();
                logger.info("Echo SSL Server connection accepted from {}", socket.getRemoteSocketAddress());
                // 接続を受け付けたら、別スレッドでEchoサーバ処理を開始
                new EchoServerThreadWorker(socket).start();
            } catch (Exception e) {
                logger.error("Echo SSL Server exception", e);
            }
        }
    }
}

/**
 * Echo サーバ処理用のスレッドクラス
 * 
 * @author Secure Sky Technology Inc.
 */
class EchoServerThreadWorker extends Thread {
    private Socket socket = null;

    public EchoServerThreadWorker(Socket socket) {
        this.socket = socket;
    }

    /**
     * 典型的なEchoサーバ処理
     */
    public void run() {
        final Logger logger = LoggerFactory.getLogger(this.getClass());
        try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                logger.info("RECV >> [{}] >> ECHO SEND", inputLine);
                // 接続元から受信してきた1行メッセージを、そのまま接続元に送り返す(echo)。
                out.println(inputLine);
            }
        } catch (Exception e) {
            logger.error("echo thread exception", e);
        } finally {
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.error("socket close exception", e);
            }
            logger.info("socket closed for {}", socket.getRemoteSocketAddress());
        }
    }
}
