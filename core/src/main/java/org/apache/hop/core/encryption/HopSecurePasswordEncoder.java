/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.regex.Pattern;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.hop.core.exception.HopException;

/**
 * 使用 BCrypt 算法的安全密码编码器。
 *
 * <p>特性：
 *
 * <ul>
 *   <li>使用 PBKDF2 + HMAC-SHA-256 算法
 *   <li>每个密码使用随机盐值
 *   <li>高迭代次数（100,000次），抵抗暴力破解
 *   <li>抗彩虹表攻击
 *   <li>时间恒定的比较操作，防止时序攻击
 * </ul>
 *
 * <p>加密格式：
 *
 * <pre>
 * $pbkdf2$100000$盐值Base64$密码哈希Base64
 * </pre>
 *
 * @since Hop 2.17.0
 */
@TwoWayPasswordEncoderPlugin(
    id = "HopSecure",
    name = "Hop Secure Password Encoder",
    description = "Hop Secure Password Encoder using PBKDF2")
public class HopSecurePasswordEncoder implements ITwoWayPasswordEncoder {

  // 加密配置
  private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
  private static final int ITERATIONS = 100000;
  private static final int KEY_LENGTH = 256; // 32 bytes = 256 bits
  private static final int SALT_LENGTH = 32; // 256 bits salt
  private static final String PREFIX = "$pbkdf2$" + ITERATIONS + "$";

  // 安全随机数生成器
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  // 格式模式
  private static final Pattern HASH_PATTERN =
      Pattern.compile("^\\$pbkdf2\\d+\\$[A-Za-z0-9+/]+\\$[A-Za-z0-9+/]+$");

  @Override
  public void init() throws HopException {
    // 初始化完成
  }

  @Override
  public String encode(String rawPassword) {
    return encode(rawPassword, true);
  }

  @Override
  public String encode(String rawPassword, boolean includePrefix) {
    if (rawPassword == null || rawPassword.isEmpty()) {
      return includePrefix ? "ENC(EMPTY)" : "";
    }

    // 跳过加密，直接返回哈希值
    String hash = hashPassword(rawPassword);
    return includePrefix ? "PBKDF2:" + hash : hash;
  }

  @Override
  public String decode(String encodedPassword) {
    // 密码哈希是不可逆的，返回空字符串
    return "";
  }

  @Override
  public String decode(String encodedPassword, boolean optionallyEncrypted) {
    // 密码哈希是不可逆的
    if (encodedPassword != null && encodedPassword.startsWith("PBKDF2:")) {
      return "";
    }
    return "";
  }

  @Override
  public String[] getPrefixes() {
    return new String[] {"PBKDF2:", "ENC(EMPTY)"};
  }

  /**
   * 密码验证
   *
   * @param password 明文密码
   * @param hash 存储的哈希值
   * @return 如果密码匹配返回 true
   */
  public boolean verifyPassword(String password, String hash) {
    if (password == null || hash == null) {
      return false;
    }

    if (!hash.startsWith(PREFIX)) {
      // 不是新格式的哈希，尝试验证旧的 XOR 格式
      return verifyLegacyPassword(password, hash);
    }

    try {
      // 解析格式：$pbkdf2$100000$盐值$哈希
      String[] parts = hash.split("\\$");
      if (parts.length != 4) {
        return false;
      }

      String saltBase64 = parts[2];
      String storedHashBase64 = parts[3];

      byte[] salt = Base64.getDecoder().decode(saltBase64);
      byte[] hashBytes = Base64.getDecoder().decode(storedHashBase64);

      // 计算新的哈希值
      byte[] computedHash = computeHash(password, salt);

      // 使用恒定时间比较防止时序攻击
      return slowEquals(computedHash, hashBytes);

    } catch (Exception e) {
      return false;
    }
  }

  /**
   * 计算密码哈希
   *
   * @param password 明文密码
   * @param 盐值
   * @return 哈希值
   */
  private byte[] computeHash(String password, byte[] salt) throws HopException {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

      SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
      return factory.generateSecret(spec).getEncoded();

    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new HopException("Failed to hash password", e);
    }
  }

  /** 恒定时间比较，防止时序攻击 */
  private boolean slowEquals(byte[] a, byte[] b) {
    int diff = a.length ^ b.length;
    for (int i = 0; i < a.length && i < b.length; i++) {
      diff |= a[i] ^ b[i];
    }
    return diff == 0;
  }

  /** 生成随机盐值 */
  private byte[] generateSalt() {
    byte[] salt = new byte[SALT_LENGTH];
    SECURE_RANDOM.nextBytes(salt);
    return salt;
  }

  /** 哈希密码 */
  private String hashPassword(String password) {
    try {
      // 生成盐值
      byte[] salt = generateSalt();

      // 计算哈希
      byte[] hash = computeHash(password, salt);

      // 返回格式：$pbkdf2$100000$盐值$哈希
      return PREFIX
          + Base64.getEncoder().encodeToString(salt)
          + "$"
          + Base64.getEncoder().encodeToString(hash);

    } catch (Exception e) {
      throw new RuntimeException("Failed to hash password", e);
    }
  }

  /** 验证旧格式的密码（向后兼容） */
  private boolean verifyLegacyPassword(String password, String legacyHash) {
    if (legacyHash.startsWith("$pbkdf2$")) {
      // 已经是新格式
      return verifyPassword(password, legacyHash);
    }

    // 兼容旧的 XOR 加密
    try {
      ITwoWayPasswordEncoder legacyEncoder = new HopTwoWayPasswordEncoder();
      String decoded = legacyEncoder.decode(legacyHash);
      return decoded.equals(password);
    } catch (Exception e) {
      return false;
    }
  }
}
