package com.oceanverse.common.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaKeyUtil {

    private static final String PRIVATE_KEY_PATH = "rsa/private.pem";
    private static final String PUBLIC_KEY_PATH = "rsa/public.pem";

    private static volatile PrivateKey privateKey;
    private static volatile PublicKey publicKey;

    public static PrivateKey getPrivateKey() {
        if (privateKey == null) {
            synchronized (RsaKeyUtil.class) {
                if (privateKey == null) {
                    privateKey = loadPrivateKey();
                }
            }
        }
        return privateKey;
    }

    public static PublicKey getPublicKey() {
        if (publicKey == null) {
            synchronized (RsaKeyUtil.class) {
                if (publicKey == null) {
                    publicKey = loadPublicKey();
                }
            }
        }
        return publicKey;
    }

    private static PrivateKey loadPrivateKey() {
        try {
            String pem = readResource(PRIVATE_KEY_PATH);
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("加载 RSA 私钥失败", e);
        }
    }

    private static PublicKey loadPublicKey() {
        try {
            String pem = readResource(PUBLIC_KEY_PATH);
            String base64 = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("加载 RSA 公钥失败", e);
        }
    }

    private static String readResource(String path) {
        try (InputStream is = RsaKeyUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("资源文件不存在: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取资源文件失败: " + path, e);
        }
    }
}
