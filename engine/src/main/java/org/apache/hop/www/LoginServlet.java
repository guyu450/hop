/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.www;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.hop.core.Const;
import org.apache.hop.core.encryption.Encr;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.util.Utils;

/**
 * Servlet to handle user login via form POST request. Validates credentials against HopServerMeta
 * or pwd/hop.pwd file.
 */
public class LoginServlet extends HttpServlet {

  private static final Class<?> PKG = LoginServlet.class;
  private static final long serialVersionUID = 1L;
  public static final String CONTEXT_PATH = "/login";

  private static final ILogChannel log = new LogChannel("LoginServlet");

  private PipelineMap pipelineMap;
  private WorkflowMap workflowMap;
  private HopServerConfig serverConfig;

  @Override
  public void init() throws ServletException {
    // Get the configuration from HopServerSingleton
    if (HopServerSingleton.getInstance() != null) {
      this.pipelineMap = HopServerSingleton.getInstance().getPipelineMap();
      this.workflowMap = HopServerSingleton.getInstance().getWorkflowMap();
      if (pipelineMap != null) {
        this.serverConfig = pipelineMap.getHopServerConfig();
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // Serve the login page
    request.getRequestDispatcher("/static/login.html").forward(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    String username = request.getParameter("username");
    String password = request.getParameter("password");
    String rememberMe = request.getParameter("rememberMe");

    if (log.isDebug()) {
      log.logDebug("Login attempt for user: " + username);
    }

    boolean authenticated = false;

    // Validate credentials
    if (serverConfig != null && serverConfig.getHopServer() != null) {
      authenticated = validateCredentials(username, password);
    }

    if (authenticated) {
      // Create session and mark as authenticated
      HttpSession session = request.getSession(true);
      session.setAttribute("authenticated", true);
      session.setAttribute("username", username);
      session.setAttribute("loginTime", System.currentTimeMillis());
      session.setAttribute("rememberMe", "true".equals(rememberMe));

      if (log.isBasic()) {
        log.logBasic("User '" + username + "' logged in successfully");
      }

      // Handle "Remember Me" cookie
      if ("true".equals(rememberMe)) {
        Cookie rememberCookie = new Cookie("hop_remember_me", username);
        rememberCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days in seconds
        rememberCookie.setPath("/");
        rememberCookie.setHttpOnly(true);
        rememberCookie.setSecure(request.isSecure()); // Use secure cookie if using HTTPS
        response.addCookie(rememberCookie);
      } else {
        // Clear existing remember me cookie if not checking remember me
        Cookie rememberCookie = new Cookie("hop_remember_me", "");
        rememberCookie.setMaxAge(0);
        rememberCookie.setPath("/");
        response.addCookie(rememberCookie);
      }

      // Redirect to originally requested URL or home
      String originalUrl =
          session.getAttribute("originalUrl") != null
              ? (String) session.getAttribute("originalUrl")
              : "/";
      session.removeAttribute("originalUrl");

      response.sendRedirect(originalUrl);
    } else {
      if (log.isBasic()) {
        log.logBasic("Failed login attempt for user: " + username);
      }
      // Redirect back to login page with error
      response.sendRedirect(request.getContextPath() + "/login/login.html?error=invalid");
    }
  }

  /**
   * Validate user credentials against HopServerMeta configuration or pwd/hop.pwd file.
   *
   * @param username The username to validate
   * @param password The password to validate
   * @return true if credentials are valid, false otherwise
   */
  private boolean validateCredentials(String username, String password) {
    if (Utils.isEmpty(username) || Utils.isEmpty(password)) {
      log.logBasic("用户名或密码为空");
      return false;
    }

    log.logBasic("=== 密码验证开始 ===");
    log.logBasic("用户名: " + username);

    // First check HopServerMeta configuration
    if (serverConfig != null && serverConfig.getHopServer() != null) {
      String configUsername = serverConfig.getHopServer().getUsername();
      String configPassword = serverConfig.getHopServer().getPassword();

      log.logBasic("HopServerMeta 配置用户名: " + configUsername);

      if (!Utils.isEmpty(configPassword)) {
        // Validate against HopServerMeta
        String decryptedPassword = Encr.decryptPasswordOptionallyEncrypted(configPassword);
        log.logBasic("HopServerMeta 密码" + password + " ; 解密密码: " + decryptedPassword);
        if (username.equals(configUsername) && password.equals(decryptedPassword)) {
          return true;
        }
        // If HopServerMeta has password configured, don't check pwd file
        log.logBasic("HopServerMeta 密码不匹配，不检查 pwd 文件");
        return false;
      }
    }

    // Check pwd/hop.pwd file
    log.logBasic("检查 pwd/hop.pwd 文件");
    return validateFromPwdFile(username, password);
  }

  /**
   * Validate credentials from pwd/hop.pwd file.
   *
   * @param username The username to validate
   * @param password The password to validate
   * @return true if credentials are valid, false otherwise
   */
  private boolean validateFromPwdFile(String username, String password) {
    String passwordFile = Const.getHopLocalServerPasswordFile();
    Path pwdPath = Paths.get(passwordFile);

    log.logBasic("密码文件路径: " + passwordFile);
    log.logBasic("密码文件存在: " + Files.exists(pwdPath));

    if (!Files.exists(pwdPath)) {
      if (log.isDetailed()) {
        log.logDetailed("Password file not found: " + passwordFile);
      }
      return false;
    }

    try (BufferedReader reader = Files.newBufferedReader(pwdPath)) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Skip comments and empty lines
        if (line.trim().isEmpty() || line.trim().startsWith("#")) {
          continue;
        }

        // Parse line: username: password,roles
        // Use limit=2 to split only on first ':' to preserve "SHA256:" prefix
        String[] parts = line.split(":", 2);
        if (parts.length >= 2) {
          String fileUsername = parts[0].trim();
          log.logBasic("检查用户: " + fileUsername + " (匹配: " + fileUsername.equals(username) + ")");

          if (fileUsername.equals(username)) {
            String credentials = parts[1].trim();
            // Split password and roles
            String[] credParts = credentials.split(",");
            String storedPassword = credParts[0].trim();

            log.logBasic("存储的密码哈希: " + storedPassword);

            // Support multiple password formats:
            // 1. SHA-256:<hex> - Jetty SHA-256 hash (hex format, compatible with Jetty Basic Auth)
            // 2. SHA256:<base64> - SHA-256 hash (base64 format, Hop custom format)
            // 3. Encrypted <hex> - Hop legacy encryption
            // 4. Plain text - for backward compatibility

            if (storedPassword.startsWith("SHA-256:")) {
              // Jetty SHA-256 format: SHA-256:hex (64 hex characters)
              String hexHash = storedPassword.substring(8); // Remove "SHA-256:" prefix
              String inputHash = encryptPasswordSha256Hex(password);
              log.logBasic("存储的密码哈希 (Jetty格式): " + storedPassword);
              log.logBasic("计算的密码哈希 (十六进制): " + inputHash);
              log.logBasic("哈希匹配: " + hexHash.equalsIgnoreCase(inputHash));
              boolean result = hexHash.equalsIgnoreCase(inputHash);
              log.logBasic("=== 密码验证结果: " + (result ? "成功" : "失败") + " ===");
              return result;
            } else if (storedPassword.startsWith("SHA256:")) {
              // Hop SHA-256 format: SHA256:base64
              String inputHash = encryptPasswordSha256Base64(password);
              log.logBasic("存储的密码哈希 (Hop格式): " + storedPassword);
              log.logBasic("计算的密码哈希 (Base64): " + inputHash);
              log.logBasic("哈希匹配: " + storedPassword.equals(inputHash));
              boolean result = storedPassword.equals(inputHash);
              log.logBasic("=== 密码验证结果: " + (result ? "成功" : "失败") + " ===");
              return result;
            } else if (storedPassword.startsWith("Encrypted ")) {
              // Hop legacy encryption
              String decryptedPassword = Encr.decryptPasswordOptionallyEncrypted(storedPassword);
              log.logBasic("解密后密码: " + decryptedPassword);
              log.logBasic("密码匹配: " + decryptedPassword.equals(password));
              boolean result = decryptedPassword.equals(password);
              log.logBasic("=== 密码验证结果: " + (result ? "成功" : "失败") + " ===");
              return result;
            } else {
              // Plain text comparison (backward compatibility)
              log.logBasic("明文密码匹配: " + storedPassword.equals(password));
              boolean result = storedPassword.equals(password);
              log.logBasic("=== 密码验证结果: " + (result ? "成功" : "失败") + " ===");
              return result;
            }
          }
        }
      }
      log.logBasic("未找到匹配的用户: " + username);
    } catch (IOException e) {
      log.logError("Error reading password file: " + passwordFile, e);
    }

    log.logBasic("=== 密码验证结束 (失败) ===");
    return false;
  }

  /** Encrypt password using SHA-256 + Hex Format: SHA-256:<hex_hash> (Jetty compatible) */
  private String encryptPasswordSha256Hex(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      // Convert to hexadecimal string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      return password; // Fallback to plain text
    }
  }

  /** Encrypt password using SHA-256 + Base64 Format: SHA256:<base64_hash> (Hop custom format) */
  private String encryptPasswordSha256Base64(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return "SHA256:" + Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      return password; // Fallback to plain text
    }
  }

  @Override
  public String toString() {
    return "Login Servlet";
  }
}
