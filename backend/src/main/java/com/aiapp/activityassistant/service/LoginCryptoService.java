package com.aiapp.activityassistant.service;

import cn.hutool.core.util.StrUtil;
import com.aiapp.activityassistant.common.exception.BusinessException;
import com.aiapp.activityassistant.common.result.ResultCode;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * 登录密码传输加密：RSA 2048（公钥加密、私钥解密）。
 *
 * <p>前端在发送登录请求前用公钥加密密码，后端用私钥解密后再做 BCrypt 校验，
 * 避免密码明文出现在网络请求中。密钥对在应用启动时生成、只存在于服务端内存，
 * 前端每次登录前通过 {@code GET /auth/public-key} 重新获取，不使用过期公钥。</p>
 */
@Service
public class LoginCryptoService {

    /** 与 JSEncrypt 默认填充方式一致（PKCS#1 v1.5） */
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final KeyPair keyPair;

    public LoginCryptoService() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
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
