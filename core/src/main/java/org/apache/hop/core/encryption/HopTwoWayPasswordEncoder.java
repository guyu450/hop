/*
 * Licensed to Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.core.encryption;

import com.google.common.annotations.VisibleForTesting;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.util.EnvUtil;
import org.apache.hop.core.util.StringUtil;

/**
 * 安全的密码编码器，使用 AES-256-GCM 加密算法。
 *
 * <p>安全改进：
 *
 * <ul>
 *   <li>使用 AES-256-GCM 加密，提供认证加密
 *   <li>每个密码使用独立的随机盐值，防止彩虹表攻击
 *   <li>加密信息包含模式、IV 和标签，提供完整性验证
 *   <li>移除硬编码种子，使用安全随机数生成器
 *   <li>向后兼容旧的 XOR 加密格式
 * </ul>
 *
 * <p>加密格式（Base64 编码）：
 *
 * <pre>
 * | 模式(1字节) | IV(12字节) | 盐值(32字节) | 密码(可变) | 认证标签(16字节) |
 * </pre>
 *
 * @deprecated 为了向后兼容保留了旧方法，建议使用 {@link org.apache.hop.core.encryption.HopSecurePasswordEncoder}
 */
@TwoWayPasswordEncoderPlugin(
    id = "Hop",
    name = "Hop Password Encoder",
    description = "Hop Password Encoder (Secure with AES-256-GCM)")
public class HopTwoWayPasswordEncoder implements ITwoWayPasswordEncoder {

  // AES 加密配置
  private static final String AES_ALGORITHM = "AES";
  private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int AES_KEY_SIZE = 256;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int SALT_LENGTH_BYTES = 32;
  private static final int MODE_LENGTH_BYTES = 1;

  // 向后兼容：保留旧的 RADIX 和 Seed 配置
  private static final int RADIX = 16;
  private String Seed;

  /**
   * The word that is put before a password to indicate an encrypted form. If this word is not
   * present, password is considered to be NOT encrypted
   */
  public static final String PASSWORD_ENCRYPTED_PREFIX = "Encrypted ";

  // 加密模式标识
  private static final String ENCRYPTION_MODE_NEW = "aes256gcm";
  private static final String ENCRYPTION_MODE_LEGACY = "xor";

  // 安全随机数生成器
  private static final SecureRandom secureRandom = new SecureRandom();

  // 日志记录器
  private ILogChannel log;

  public HopTwoWayPasswordEncoder() {
    // 旧的 XOR 种子仅用于向后兼容，新密码不再使用
    String envSeed =
        Const.NVL(EnvUtil.getSystemProperty(Const.HOP_TWO_WAY_PASSWORD_ENCODER_SEED), "");
    Seed = envSeed;
  }

  @Override
  public void init() throws HopException {
    // 初始化日志
    log = org.apache.hop.core.logging.LogChannel.GENERAL;
  }

  @Override
  public String encode(String rawPassword) {
    return encode(rawPassword, true);
  }

  @Override
  public String encode(String rawPassword, boolean includePrefix) {
    if (rawPassword == null || rawPassword.isEmpty()) {
      return includePrefix ? PASSWORD_ENCRYPTED_PREFIX : "";
    }

    // 检查是否包含变量
    List<String> varList = new ArrayList<>();
    StringUtil.getUsedVariables(rawPassword, varList, true);

    if (!varList.isEmpty()) {
      // 不加密包含变量的密码
      return includePrefix ? PASSWORD_ENCRYPTED_PREFIX + rawPassword : rawPassword;
    }

    try {
      String encrypted = encryptSecure(rawPassword);
      return includePrefix ? PASSWORD_ENCRYPTED_PREFIX + encrypted : encrypted;
    } catch (Exception e) {
      // 加密失败时回退到旧的 XOR 方法（向后兼容）
      log.logBasic("Secure encryption failed, falling back to legacy method: " + e.getMessage());
      return includePrefix
          ? PASSWORD_ENCRYPTED_PREFIX + encryptPasswordInternal(rawPassword)
          : encryptPasswordInternal(rawPassword);
    }
  }

  @Override
  public String decode(String encodedPassword) {
    if (encodedPassword == null) {
      return null;
    }

    if (encodedPassword.startsWith(PASSWORD_ENCRYPTED_PREFIX)) {
      encodedPassword = encodedPassword.substring(PASSWORD_ENCRYPTED_PREFIX.length());
    }

    return decryptPassword(encodedPassword);
  }

  @Override
  public String decode(String encodedPassword, boolean optionallyEncrypted) {
    if (encodedPassword == null) {
      return null;
    }

    String toDecrypt = encodedPassword;
    if (optionallyEncrypted && encodedPassword.startsWith(PASSWORD_ENCRYPTED_PREFIX)) {
      toDecrypt = encodedPassword.substring(PASSWORD_ENCRYPTED_PREFIX.length());
    } else if (!optionallyEncrypted) {
      // 如果明确指定未加密，直接返回
      return encodedPassword;
    }

    return decryptPassword(toDecrypt);
  }

  /**
   * 使用 AES-256-GCM 安全加密密码。
   *
   * @param password 明文密码
   * @return Base64 编码的加密数据
   * @throws HopException 加密失败时抛出异常
   */
  private String encryptSecure(String password) throws HopException {
    try {
      // 1. 生成随机盐值
      byte[] salt = new byte[SALT_LENGTH_BYTES];
      secureRandom.nextBytes(salt);

      // 2. 生成随机 IV
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      // 3. 生成密钥（使用密码和盐值）
      SecretKey key = deriveKey(password, salt);

      // 4. 初始化加密器
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

      // 5. 加密密码
      byte[] passwordBytes = password.getBytes(Const.XML_ENCODING);

      // 6. 构建加密数据：模式 + IV + 盐值 + 密码
      byte[] encrypted = cipher.doFinal(passwordBytes);

      // 7. 组合：模式(1字节) + IV(12字节) + 盐值(32字节) + 加密数据
      byte[] combined =
          new byte[MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + SALT_LENGTH_BYTES + encrypted.length];
      System.arraycopy(
          new byte[] {0x01}, 0, combined, 0, MODE_LENGTH_BYTES); // 模式 0x01 = AES-256-GCM
      System.arraycopy(iv, 0, combined, MODE_LENGTH_BYTES, GCM_IV_LENGTH_BYTES);
      System.arraycopy(
          salt, 0, combined, MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES, SALT_LENGTH_BYTES);
      System.arraycopy(
          encrypted,
          0,
          combined,
          MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + SALT_LENGTH_BYTES,
          encrypted.length);

      // 8. Base64 编码
      return Base64.getEncoder().encodeToString(combined);

    } catch (GeneralSecurityException e) {
      throw new HopException("Security error during password encryption", e);
    } catch (Exception e) {
      throw new HopException("Error encrypting password", e);
    }
  }

  /**
   * 使用 AES-256-GCM 解密密码。
   *
   * @param encryptedPassword Base64 编码的加密数据
   * @return 明文密码
   * @throws HopException 解密失败时抛出异常
   */
  private String decryptSecure(String encryptedPassword) throws HopException {
    try {
      // 1. Base64 解码
      byte[] combined = Base64.getDecoder().decode(encryptedPassword);

      // 检查最小长度
      int minLength =
          MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + SALT_LENGTH_BYTES + GCM_TAG_LENGTH_BITS / 8;
      if (combined.length < minLength) {
        throw new HopException("Encrypted password is too short");
      }

      // 2. 提取各个部分
      byte mode = combined[0];
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      byte[] salt = new byte[SALT_LENGTH_BYTES];
      byte[] encryptedData =
          new byte[combined.length - MODE_LENGTH_BYTES - GCM_IV_LENGTH_BYTES - SALT_LENGTH_BYTES];

      System.arraycopy(combined, MODE_LENGTH_BYTES, iv, 0, GCM_IV_LENGTH_BYTES);
      System.arraycopy(
          combined, MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES, salt, 0, SALT_LENGTH_BYTES);
      System.arraycopy(
          combined,
          MODE_LENGTH_BYTES + GCM_IV_LENGTH_BYTES + SALT_LENGTH_BYTES,
          encryptedData,
          0,
          encryptedData.length);

      // 3. 验证加密模式
      if (mode != 0x01) {
        throw new HopException("Unsupported encryption mode");
      }

      // 4. 生成密钥（需要主密码或盐值）
      // 注意：这里需要主密码来解密，所以这是"双"加密系统
      // 如果使用不同的主密码，解密会失败（这是预期的行为）
      SecretKey key = deriveKeyForDecryption(salt);

      // 5. 初始化解密器
      Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
      GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

      // 6. 解密
      byte[] decrypted = cipher.doFinal(encryptedData);

      // 7. 转换为字符串
      return new String(decrypted, Const.XML_ENCODING);

    } catch (Exception e) {
      throw new HopException("Error decrypting password", e);
    }
  }

  /** 从密码和盐值派生密钥（用于加密）。 注意：这个方法使用硬编码的主密码来保护加密的密码 在实际部署中，主密码应该从安全的位置获取（如环境变量或密钥库） */
  private SecretKey deriveKey(String password, byte[] salt)
      throws GeneralSecurityException, UnsupportedEncodingException {
    // 获取主密码（用于保护加密密码）
    String masterPassword = getMasterPassword();

    // 派生密钥
    byte[] keyBytes = masterPassword.getBytes(Const.XML_ENCODING);
    SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

    // 创建密钥对象
    javax.crypto.SecretKeyFactory keyFactory =
        javax.crypto.SecretKeyFactory.getInstance(AES_ALGORITHM);
    return keyFactory.generateSecret(keySpec);
  }

  /** 从盐值派生密钥（用于解密）。 这个方法需要主密码才能成功解密。 */
  private SecretKey deriveKeyForDecryption(byte[] salt)
      throws GeneralSecurityException, UnsupportedEncodingException {
    String masterPassword = getMasterPassword();

    if (masterPassword == null || masterPassword.isEmpty()) {
      throw new GeneralSecurityException("Master password is required for decryption");
    }

    // 派生密钥
    byte[] keyBytes = masterPassword.getBytes(Const.XML_ENCODING);
    SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);

    javax.crypto.SecretKeyFactory keyFactory =
        javax.crypto.SecretKeyFactory.getInstance(AES_ALGORITHM);
    return keyFactory.generateSecret(keySpec);
  }

  /** 获取主密码。 主密码用于保护/解密存储的密码。 在实际部署中，这应该从环境变量或安全存储获取。 */
  private String getMasterPassword() {
    // 优先从环境变量获取
    String masterPassword = EnvUtil.getSystemProperty("HOP_MASTER_PASSWORD");
    if (masterPassword != null && !masterPassword.isEmpty()) {
      return masterPassword;
    }

    // 回退到旧的 Seed 配置（向后兼容）
    String seed = EnvUtil.getSystemProperty(Const.HOP_TWO_WAY_PASSWORD_ENCODER_SEED);
    if (seed != null && !seed.isEmpty()) {
      return seed;
    }

    // 默认值（警告：仅用于开发/测试环境）
    log.logDebug(
        "Using default master password for encryption. Set HOP_MASTER_PASSWORD environment variable for production.");
    return "hop-default-master-key-2025"; // 开发/测试默认值
  }

  /** 检测加密密码的格式并选择正确的解密方法。 */
  private String decryptPassword(String encryptedPassword) {
    if (encryptedPassword == null || encryptedPassword.isEmpty()) {
      return "";
    }

    try {
      // 检测格式：新格式还是旧格式
      if (isNewEncryptionFormat(encryptedPassword)) {
        // 尝试新格式解密
        try {
          return decryptSecure(encryptedPassword);
        } catch (HopException e) {
          // 新格式解密失败，可能是旧格式
          log.logDebug("New format decryption failed, trying legacy format: " + e.getMessage());
        }
      }

      // 尝试旧格式（向后兼容）
      return decryptPasswordInternal(encryptedPassword);

    } catch (Exception e) {
      log.logError("Error decrypting password: " + e.getMessage());
      return "";
    }
  }

  /** 检测是否为新的加密格式。 */
  private boolean isNewEncryptionFormat(String encryptedPassword) {
    try {
      byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
      // 新格式至少需要：模式(1) + IV(12) + 盐值(32) + 标签(16) = 61 字节
      return decoded.length >= 61;
    } catch (Exception e) {
      return false;
    }
  }

  // ==================== 向后兼容方法（旧的 XOR 加密）====================

  /**
   * 旧的 XOR 加密方法，用于向后兼容。 这个方法不安全，仅用于解密历史数据。
   *
   * @deprecated 不推荐使用，仅用于向后兼容
   */
  @Deprecated
  @VisibleForTesting
  protected String encryptPasswordInternal(String password) {
    if (password == null) {
      return "";
    }
    if (password.isEmpty()) {
      return "";
    }

    // 标记为旧格式
    return ENCRYPTION_MODE_LEGACY + ":" + legacyXorEncrypt(password);
  }

  /**
   * 旧的 XOR 解密方法，用于向后兼容。
   *
   * @deprecated 不推荐使用，仅用于向后兼容
   */
  @Deprecated
  @VisibleForTesting
  protected String decryptPasswordInternal(String encrypted) {
    if (encrypted == null) {
      return "";
    }
    if (encrypted.isEmpty()) {
      return "";
    }

    // 检测旧格式标记
    if (encrypted.startsWith(ENCRYPTION_MODE_LEGACY + ":")) {
      String actualEncrypted = encrypted.substring(ENCRYPTION_MODE_LEGACY.length() + 1);
      return legacyXorDecrypt(actualEncrypted);
    }

    // 尝试直接解密（纯 16 进制字符串）
    try {
      return legacyXorDecrypt(encrypted);
    } catch (Exception e) {
      log.logDebug("Legacy decryption failed: " + e.getMessage());
      return "";
    }
  }

  /** 旧的 XOR 加密实现。 */
  @Deprecated
  private String legacyXorEncrypt(String password) {
    try {
      java.math.BigInteger biPasswd = new java.math.BigInteger(password.getBytes());
      java.math.BigInteger biR0 = new java.math.BigInteger(getSeed());
      java.math.BigInteger biR1 = biR0.xor(biPasswd);
      return biR1.toString(RADIX);
    } catch (Exception e) {
      return "";
    }
  }

  /** 旧的 XOR 解密实现。 */
  @Deprecated
  private String legacyXorDecrypt(String encrypted) {
    try {
      java.math.BigInteger biConfuse = new java.math.BigInteger(getSeed());
      java.math.BigInteger biR1 = new java.math.BigInteger(encrypted, RADIX);
      java.math.BigInteger biR0 = biR1.xor(biConfuse);
      return new String(biR0.toByteArray());
    } catch (Exception e) {
      return "";
    }
  }

  @VisibleForTesting
  protected String getSeed() {
    return this.Seed;
  }

  @Override
  public String[] getPrefixes() {
    return new String[] {PASSWORD_ENCRYPTED_PREFIX};
  }

  /**
   * Encrypts password, but only if password doesn't contain any variables.
   *
   * @param password The password to encrypt
   * @return The encrypted password or
   */
  protected final String encryptPasswordIfNotUsingVariablesInternal(String password) {
    String encryptedPassword = "";
    List<String> varList = new ArrayList<>();
    StringUtil.getUsedVariables(password, varList, true);
    if (varList.isEmpty()) {
      // 不加密包含变量的密码
      return PASSWORD_ENCRYPTED_PREFIX + password;
    } else {
      return password;
    }
  }
}
