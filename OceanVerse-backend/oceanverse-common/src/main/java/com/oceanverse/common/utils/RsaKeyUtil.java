package com.oceanverse.common.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 密钥工具
 * <p>
 * 密钥加载优先级：
 * 1. 环境变量指定的外部文件路径（推荐，避免密钥打包在 JAR 中）
 *    - OCEANVERSE_RSA_PRIVATE_KEY_PATH
 *    - OCEANVERSE_RSA_PUBLIC_KEY_PATH
 * 2. classpath 资源（仅开发回退）
 */
public class RsaKeyUtil {

    private static final String PRIVATE_KEY_PATH = "rsa/private.pem";
    private static final String PUBLIC_KEY_PATH = "rsa/public.pem";

    private static final String ENV_PRIVATE_KEY_PATH = "OCEANVERSE_RSA_PRIVATE_KEY_PATH";
    private static final String ENV_PUBLIC_KEY_PATH = "OCEANVERSE_RSA_PUBLIC_KEY_PATH";

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
            String pem = readKeyContent(ENV_PRIVATE_KEY_PATH, PRIVATE_KEY_PATH);
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
            String pem = readKeyContent(ENV_PUBLIC_KEY_PATH, PUBLIC_KEY_PATH);
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

    /**
     * 优先从环境变量指定的外部文件加载，回退到 classpath 资源。
     */
    private static String readKeyContent(String envVar, String classpathPath) {
        String externalPath = System.getenv(envVar);
        if (externalPath != null && !externalPath.isBlank()) {
            try {
                return Files.readString(Path.of(externalPath), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("读取外部密钥文件失败: " + externalPath, e);
            }
        }
        // 回退到 classpath（仅开发环境使用）
        return readResource(classpathPath);
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
