package com.aiapp.activityassistant.service;

import cn.hutool.core.util.StrUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * 登录密码传输加密：RSA 2048（公钥加密、私钥解密）。
 *
 * <p>前端在发送登录请求前用公钥加密密码，后端用私钥解密后再做 BCrypt 校验，
 * 避免密码明文出现在网络请求中。密钥对在应用启动时生成、只存在于服务端内存，
 * 前端每次登录前通过 {@code GET /auth/public-key} 重新获取，不使用过期公钥。</p>
 */
@Slf4j
@Service
public class LoginCryptoService {

    /** 与 JSEncrypt 默认填充方式一致（PKCS#1 v1.5） */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /** 集群部署时用于共享同一把密钥：私钥文件路径（PKCS#8 PEM 或纯 Base64） */
    private static final String KEY_FILE_ENV = "LOGIN_RSA_KEY_FILE";
    /** 也可直接把 Base64 私钥放进环境变量 */
    private static final String KEY_BASE64_ENV = "LOGIN_RSA_KEY";

    private final KeyPair keyPair;

    public LoginCryptoService() {
        this.keyPair = loadSharedKeyOrGenerate();
    }

    /**
     * 优先加载配置的共享密钥：多实例集群共用一个文件，避免「公钥来自 A、请求落到 B」导致解密失败。
     * 未配置时退回原有行为（本实例随机生成），单机部署不受影响。
     */
    private KeyPair loadSharedKeyOrGenerate() {
        String base64 = System.getenv(KEY_BASE64_ENV);
        String keyFile = System.getenv(KEY_FILE_ENV);
        if (StrUtil.isBlank(base64) && StrUtil.isNotBlank(keyFile) && Files.exists(Path.of(keyFile))) {
            try {
                base64 = Files.readString(Path.of(keyFile));
            } catch (Exception e) {
                log.warn("[登录加密] 读取共享密钥文件失败：{}", keyFile, e);
            }
        }
        if (StrUtil.isNotBlank(base64)) {
            try {
                String normalized = base64.replaceAll("-----[^-]+-----", "").replaceAll("\\s", "");
                byte[] der = Base64.getDecoder().decode(normalized);
                KeyFactory factory = KeyFactory.getInstance("RSA");
                PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(der));
                RSAPrivateCrtKey crt = (RSAPrivateCrtKey) privateKey;
                PublicKey publicKey = factory.generatePublic(
                        new RSAPublicKeySpec(crt.getModulus(), crt.getPublicExponent()));
                log.info("[登录加密] 已加载共享 RSA 密钥，多实例可共用（{}）", StrUtil.isNotBlank(keyFile) ? keyFile : "env");
                return new KeyPair(publicKey, privateKey);
            } catch (Exception e) {
                log.warn("[登录加密] 共享密钥无效，退回本实例随机生成", e);
            }
        }
        return generate();
    }

    private KeyPair generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            log.info("[登录加密] 本实例随机生成 RSA 密钥（未配置共享密钥，集群下登录会随机失败）");
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("初始化登录密码加密密钥失败", e);
        }
    }

    /**
     * 对外暴露的公钥（X.509 编码的 Base64，可直接交给 JSEncrypt.setPublicKey）
     */
    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    /**
     * 解密前端传来的密码密文，解密失败说明不是本实例公钥加密的（或客户端传了明文）
     */
    public String decrypt(String cipherText) {
        if (StrUtil.isBlank(cipherText)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] plain = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "密码解密失败，请刷新页面后重试");
        }
    }
}
