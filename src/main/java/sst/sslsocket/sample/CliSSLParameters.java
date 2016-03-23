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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;

import org.apache.commons.io.FileUtils;

/**
 * コマンドラインオプションで指定された、 {@link javax.net.ssl.SSLParameters} カスタマイズ用のパラメータ
 * 
 * @author Secure Sky Technology Inc.
 */
public class CliSSLParameters {

    /**
     * {@link javax.net.ssl.SSLParameters#setProtocols(String[])} に渡される。空配列なら設定しない。
     */
    public String[] protocols = new String[] {};

    /**
     * {@link javax.net.ssl.SSLParameters#setCipherSuites(String[])} に渡される。空配列なら設定しない。
     */
    public String[] ciphers = new String[] {};

    /**
     * {@link javax.net.ssl.SSLParameters#setUseCipherSuitesOrder(boolean)} に渡される。デフォルトはfalse.
     */
    public boolean cipherOrder = false;

    /**
     * {@link javax.net.ssl.SSLParameters#setServerNames(List)} に設定される。空文字列なら設定しない。
     */
    public String serverName = "";

    /**
     * CipherSuiteが一行毎にリストアップされたテキストファイルを読み込む。
     * 
     * @param filepath
     * @throws IOException
     */
    public void loadCiphersFromFile(String filepath) throws IOException {
        List<String> lines = FileUtils.readLines(new File(filepath));
        this.ciphers = lines.toArray(new String[] {});
    }

    /**
     * sslpで渡されてきた {@link javax.net.ssl.SSLParameters} を更新する。
     * 
     * @param sslp
     * @param isClientSocket trueならSNI用にserverNameを調整する。
     */
    public void updateSSLParameters(SSLParameters sslp, boolean isClientSocket) {
        if (protocols.length > 0) {
            sslp.setProtocols(protocols);
        }
        if (ciphers.length > 0) {
            sslp.setCipherSuites(ciphers);
        }
        sslp.setUseCipherSuitesOrder(cipherOrder);
        if (isClientSocket && !serverName.isEmpty()) {
            SNIHostName sniHostName = new SNIHostName(serverName);
            List<SNIServerName> serverNames = new ArrayList<>(1);
            serverNames.add(sniHostName);
            sslp.setServerNames(serverNames);
        }
    }

    @Override
    public String toString() {
        return "CliSSLParameters [protocols="
            + Arrays.toString(protocols)
            + ", ciphers="
            + Arrays.toString(ciphers)
            + ", cipherorder="
            + cipherOrder
            + ", servername="
            + serverName
            + "]";
    }
}
