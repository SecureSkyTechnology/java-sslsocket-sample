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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

/**
 * SSL/TLSアプリで便利なユーティリティクラス
 * 
 * @author Secure Sky Technology Inc.
 */
public class TinyUtils {

    /**
     * 注意：テスト/サンプルアプリ専用, クライアントSSLSocketで証明書の検証で全て検証OKとするためのTrustManagerを返す。
     * 
     * 実際のアプリケーションでは、カスタマイズせずにJavaのデフォルトの証明書検証に任せるか、独自の検証ロジックを実装のこと。
     * 
     * @return {@link javax.net.ssl.SSLContext#init(KeyManager[], TrustManager[], java.security.SecureRandom)} で使うTrustManagerインスタンスの配列
     */
    public static TrustManager[] createAllAllowTrustManagers() {
        return new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
            }
        } };
    }

    /**
     * 引数で指定されたサーバ証明書のみ検証をpassするTrustManagerを返す。
     * 
     * @param pemCertFile PEM形式のサーバ証明書ファイル
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static TrustManager[] createDebugPurposeTrustManagers(String pemCertFile) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        // 空のJKSキーストアを初期化。
        KeyStore ks1 = KeyStore.getInstance("JKS");
        ks1.load(null, null);

        try (FileInputStream fisPemCertFile = new FileInputStream(pemCertFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> c = cf.generateCertificates(fisPemCertFile);
            // JKSキーストアに証明書を格納
            ks1.setCertificateEntry("dummy-alias", c.toArray(new Certificate[0])[0]);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        // TrustManagerFactoryを指定された証明書のみで検証するよう初期化
        tmf.init(ks1);

        return tmf.getTrustManagers();
    }

    /**
     * {@link javax.net.ssl.SSLParameters} の主要パラメータを標準出力にダンプする。
     * 
     * @param sslp
     * @param msgPrefix 分かりやすいよう、プレフィックス文字列を指定可能。
     */
    public static void dumpSSLParameters(SSLParameters sslp, String msgPrefix) {
        System.out.println("SSLParameters (" + msgPrefix + "):");
        String[] protocols = sslp.getProtocols();
        if (null != protocols) {
            for (int i = 0; i < protocols.length; i++) {
                System.out.println("  Protocols[" + i + "]: " + protocols[i]);
            }
        }
        System.out.println("  useCipherSuitesOrder: [" + sslp.getUseCipherSuitesOrder() + "]");
        String[] cipherSuites = sslp.getCipherSuites();
        if (null != cipherSuites) {
            for (int i = 0; i < cipherSuites.length; i++) {
                System.out.println("  CipherSuites[" + i + "]: " + cipherSuites[i]);
            }
        }
        List<SNIServerName> serverNames = sslp.getServerNames();
        if (null != serverNames) {
            for (int i = 0; i < serverNames.size(); i++) {
                System.out.println("  SNIServerName[" + i + "]: " + serverNames.get(i).toString() + "]");
            }
        }
        System.out.println("  endpointIdentificationAlgorithm: [" + sslp.getEndpointIdentificationAlgorithm() + "]");
        System.out.println("  needClientAuth: [" + sslp.getNeedClientAuth() + "]");
        System.out.println("  wantClientAuth: [" + sslp.getWantClientAuth() + "]");
    }

    /**
     * サーバ証明書と鍵ファイルをロードし、KeyManagerインスタンスの配列を生成する。 
     *
     * @param pemCertFile PEM形式のサーバ証明書ファイル
     * @param pemKeyFile PKCS #8 のDER形式の鍵ファイル(パスワード/パスフレーズなどで暗号化されていない状態)
     * @param algo {@link java.security.KeyFactory#getInstance(String)} で指定する鍵のアルゴリズム。Java8なら RSA/DSA/EC のいずれか。
     * @return {@link javax.net.ssl.SSLContext#init(KeyManager[], TrustManager[], java.security.SecureRandom)} で使うKeyManagerインスタンスの配列
     * @throws Exception
     */
    public static KeyManager[] loadAndCreateKeyManagers(final String pemCertFile, final String pemKeyFile,
            final String algo) throws Exception {

        Certificate[] certs = new Certificate[0];
        try (FileInputStream fisPemCertFile = new FileInputStream(pemCertFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> c = cf.generateCertificates(fisPemCertFile);
            certs = c.toArray(new Certificate[0]);
        }

        // 空のJKSキーストアを初期化。
        KeyStore ks1 = KeyStore.getInstance("JKS");
        ks1.load(null, null);

        // PKCS #8 のDER形式の鍵ファイルを読み込む。
        try (FileInputStream fisPemKeyFile = new FileInputStream(pemKeyFile)) {
            byte[] pemKeyAsByte = IOUtils.toByteArray(fisPemKeyFile);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pemKeyAsByte);
            KeyFactory keyFactory = KeyFactory.getInstance(algo);
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            // パスワードは空で、鍵と、対応する証明書をキーストアに読み込む。
            ks1.setKeyEntry("dummyKeyAlias", privateKey, "".toCharArray(), certs);
        }

        // 空のパスワードでキーマネージャファクトリを初期化し、キーストアを元にキーマネージャを生成する。
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks1, "".toCharArray());

        return kmf.getKeyManagers();
    }
}
