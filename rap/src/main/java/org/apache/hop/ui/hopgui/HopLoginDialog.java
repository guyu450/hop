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

package org.apache.hop.ui.hopgui;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.apache.hop.core.Const;
import org.apache.hop.core.encryption.Encr;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.util.Utils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/** Dialog for user login in RAP Web UI */
public class HopLoginDialog extends Composite {

  private static final long serialVersionUID = 1L;
  private static final String SESSION_AUTHENTICATED = "authenticated";
  private static final String SESSION_USERNAME = "username";
  private static final String SESSION_REMEMBER_ME = "rememberMe";
  private static final String COOKIE_REMEMBER_ME = "hop_remember_me";
  private static final int REMEMBER_ME_DAYS = 30; // Remember for 30 days
  private static final LogChannel log = new LogChannel("HopLoginDialog");

  private Text usernameText;
  private Text passwordText;
  private Button rememberMeCheckbox;
  private Label messageLabel;
  private Runnable loginCallback;

  public HopLoginDialog(Composite parent, Runnable loginCallback) {
    super(parent, SWT.NONE);
    this.loginCallback = loginCallback;
    createDialog();
    checkRememberMeCookie();
  }

  private void createDialog() {
    // Main container with background color
    GridLayout mainLayout = new GridLayout(1, false);
    mainLayout.marginWidth = 0;
    mainLayout.marginHeight = 0;
    setLayout(mainLayout);
    setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

    // Create login panel with border and background
    Composite loginPanel = new Composite(this, SWT.BORDER);
    GridLayout panelLayout = new GridLayout(2, false);
    panelLayout.marginWidth = 40;
    panelLayout.marginHeight = 40;
    panelLayout.horizontalSpacing = 15;
    panelLayout.verticalSpacing = 20;
    loginPanel.setLayout(panelLayout);
    GridData panelData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    panelData.widthHint = 450;
    loginPanel.setLayoutData(panelData);

    // Set panel background color
    Color panelBg = new Color(getDisplay(), new RGB(248, 250, 252));
    loginPanel.setBackground(panelBg);

    // Title with larger font and color
    Label titleLabel = new Label(loginPanel, SWT.NONE);
    titleLabel.setText("Htfx - 登录");
    titleLabel.setBackground(panelBg);
    GridData titleData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    titleData.verticalIndent = 10;
    titleLabel.setLayoutData(titleData);

    // Decorative line
    Label lineLabel = new Label(loginPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
    lineLabel.setBackground(panelBg);
    GridData lineData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    lineData.heightHint = 1;
    lineLabel.setLayoutData(lineData);

    // Spacing after line
    Label spacer1 = new Label(loginPanel, SWT.NONE);
    spacer1.setBackground(panelBg);
    GridData spacerData1 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    spacerData1.heightHint = 10;
    spacer1.setLayoutData(spacerData1);

    // Message label for errors
    messageLabel = new Label(loginPanel, SWT.WRAP);
    messageLabel.setText("");
    messageLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
    messageLabel.setBackground(panelBg);
    GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    messageData.heightHint = 25;
    messageLabel.setLayoutData(messageData);

    // Username label
    Label usernameLabel = new Label(loginPanel, SWT.NONE);
    usernameLabel.setText("用户名:");
    usernameLabel.setBackground(panelBg);
    GridData usernameLabelData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    usernameLabelData.widthHint = 80;
    usernameLabel.setLayoutData(usernameLabelData);

    // Username text field
    usernameText = new Text(loginPanel, SWT.BORDER);
    GridData usernameData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    usernameData.heightHint = 32;
    usernameText.setLayoutData(usernameData);

    // Password label
    Label passwordLabel = new Label(loginPanel, SWT.NONE);
    passwordLabel.setText("密码:");
    passwordLabel.setBackground(panelBg);
    GridData passwordLabelData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    passwordLabelData.widthHint = 80;
    passwordLabel.setLayoutData(passwordLabelData);

    // Password text field
    passwordText = new Text(loginPanel, SWT.BORDER | SWT.PASSWORD);
    GridData passwordData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    passwordData.heightHint = 32;
    passwordText.setLayoutData(passwordData);

    // Remember me checkbox
    rememberMeCheckbox = new Button(loginPanel, SWT.CHECK);
    rememberMeCheckbox.setText("记住我（30天）");
    rememberMeCheckbox.setBackground(panelBg);
    GridData rememberData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    rememberData.verticalIndent = 5;
    rememberMeCheckbox.setLayoutData(rememberData);

    // Spacing before button
    Label spacer2 = new Label(loginPanel, SWT.NONE);
    spacer2.setBackground(panelBg);
    GridData spacerData2 = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    spacerData2.heightHint = 10;
    spacer2.setLayoutData(spacerData2);

    // Login button
    Button loginButton = new Button(loginPanel, SWT.PUSH);
    loginButton.setText("登录");
    GridData buttonData = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
    buttonData.widthHint = 100;
    buttonData.heightHint = 38;
    loginButton.setLayoutData(buttonData);
    loginButton.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            handleLogin();
          }
        });

    // Set default focus on username
    usernameText.setFocus();
  }

  /** Check for remember me cookie and auto-fill username */
  private void checkRememberMeCookie() {
    try {
      HttpServletRequest request = RWT.getRequest();
      Cookie[] cookies = request.getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (COOKIE_REMEMBER_ME.equals(cookie.getName())) {
            String username = cookie.getValue();
            if (!Utils.isEmpty(username)) {
              usernameText.setText(username);
              passwordText.setFocus(); // Focus on password field
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      // Ignore cookie errors
    }
  }

  private void handleLogin() {
    String username = usernameText.getText().trim();
    String password = passwordText.getText();
    boolean rememberMe = rememberMeCheckbox.getSelection();

    if (Utils.isEmpty(username)) {
      showError("请输入用户名");
      return;
    }

    if (Utils.isEmpty(password)) {
      showError("请输入密码");
      return;
    }

    // Validate credentials
    if (validateCredentials(username, password)) {
      // Set HttpSession as authenticated
      try {
        HttpServletRequest request = RWT.getRequest();
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_AUTHENTICATED, true);
        session.setAttribute(SESSION_USERNAME, username);
        session.setAttribute(SESSION_REMEMBER_ME, rememberMe);

        // Set remember me cookie if requested
        if (rememberMe) {
          Cookie rememberCookie = new Cookie(COOKIE_REMEMBER_ME, username);
          rememberCookie.setMaxAge(REMEMBER_ME_DAYS * 24 * 60 * 60); // 30 days in seconds
          rememberCookie.setPath("/");
          rememberCookie.setHttpOnly(true);
          // Add cookie to response (need to use RWT mechanism)
          try {
            RWT.getResponse().addCookie(rememberCookie);
          } catch (Exception e) {
            // Ignore cookie setting errors
          }
        }

        // Clear the dialog
        dispose();

        // Trigger callback
        if (loginCallback != null) {
          loginCallback.run();
        }
      } catch (Exception e) {
        showError("登录失败: " + e.getMessage());
      }
    } else {
      showError("用户名或密码错误");
      passwordText.setText("");
      passwordText.setFocus();
    }
  }

  private boolean validateCredentials(String username, String password) {
    String passwordFile = Const.getHopLocalServerPasswordFile();
    java.io.File pwdFile = new java.io.File(passwordFile);

    log.logBasic("=== 密码验证开始 ===");
    log.logBasic("用户名: " + username);
    log.logBasic("密码文件路径: " + passwordFile);
    log.logBasic("密码文件存在: " + pwdFile.exists());

    if (!pwdFile.exists()) {
      log.logBasic("密码文件不存在，使用默认 cluster/cluster");
      // If no password file, check against default cluster/cluster
      return "cluster".equals(username) && "cluster".equals(password);
    }

    try (java.io.BufferedReader reader =
        new java.io.BufferedReader(new java.io.FileReader(pwdFile))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Skip comments and empty lines
        if (line.trim().isEmpty() || line.trim().startsWith("#")) {
          continue;
        }

        // Parse: username: password,roles
        // Use limit=2 to split only on first ':' to preserve "SHA256:" prefix
        String[] parts = line.split(":", 2);
        if (parts.length >= 2) {
          String fileUsername = parts[0].trim();
          log.logBasic("检查用户: " + fileUsername + " (匹配: " + fileUsername.equals(username) + ")");

          if (fileUsername.equals(username)) {
            String credentials = parts[1].trim();
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
              log.logBasic("计算的密码哈希 (十六进制): SHA-256:" + inputHash);
              log.logBasic("哈希匹配: " + hexHash.equalsIgnoreCase(inputHash));
              boolean result = hexHash.equalsIgnoreCase(inputHash);
              log.logBasic("验证结果: " + (result ? "成功" : "失败"));
              return result;
            } else if (storedPassword.startsWith("SHA256:")) {
              // Hop SHA-256 format: SHA256:base64
              String inputHash = encryptPasswordSha256Base64(password);
              log.logBasic("存储的密码哈希 (Hop格式): " + storedPassword);
              log.logBasic("计算的密码哈希 (Base64): " + inputHash);
              log.logBasic("哈希匹配: " + storedPassword.equals(inputHash));
              boolean result = storedPassword.equals(inputHash);
              log.logBasic("验证结果: " + (result ? "成功" : "失败"));
              return result;
            } else if (storedPassword.startsWith("Encrypted ")) {
              // Hop legacy encryption
              String decryptedPassword = Encr.decryptPasswordOptionallyEncrypted(storedPassword);
              log.logBasic("解密后密码: " + decryptedPassword);
              log.logBasic("密码匹配: " + decryptedPassword.equals(password));
              return decryptedPassword.equals(password);
            } else {
              // Plain text comparison (backward compatibility)
              log.logBasic("明文密码匹配: " + storedPassword.equals(password));
              return storedPassword.equals(password);
            }
          }
        }
      }
      log.logBasic("未找到匹配的用户: " + username);
    } catch (Exception e) {
      log.logError("读取密码文件时出错", e);
    }
    log.logBasic("=== 密码验证结束 (失败) ===");
    return false;
  }

  private String deobfuscate(String obfuscated) {
    // Simple OBF deobfuscation (Jetty compatible)
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < obfuscated.length(); i++) {
      char c = obfuscated.charAt(i);
      if (c == '1') {
        i++;
        if (i < obfuscated.length()) {
          sb.append((char) (obfuscated.charAt(i) - 1));
        }
      } else if (c == '2') {
        i++;
        if (i < obfuscated.length()) {
          sb.append((char) (obfuscated.charAt(i) - 2));
        }
      } else if (c == '3') {
        i++;
        if (i < obfuscated.length()) {
          sb.append((char) (obfuscated.charAt(i) - 3));
        }
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private void showError(String message) {
    messageLabel.setText(message);
    messageLabel.requestLayout();
  }

  /** Check if current session is authenticated using HttpSession */
  public static boolean isAuthenticated() {
    try {
      HttpServletRequest request = RWT.getRequest();
      HttpSession session = request.getSession(false);
      if (session == null) {
        return false;
      }
      Boolean authenticated = (Boolean) session.getAttribute(SESSION_AUTHENTICATED);
      return Boolean.TRUE.equals(authenticated);
    } catch (Exception e) {
      return false;
    }
  }

  /** Get current authenticated username from HttpSession */
  public static String getUsername() {
    try {
      HttpServletRequest request = RWT.getRequest();
      HttpSession session = request.getSession(false);
      if (session == null) {
        return null;
      }
      return (String) session.getAttribute(SESSION_USERNAME);
    } catch (Exception e) {
      return null;
    }
  }

  /** Encrypt password using SHA-256 + Hex Format: SHA-256:<hex_hash> (Jetty compatible) */
  private static String encryptPasswordSha256Hex(String password) {
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
  private static String encryptPasswordSha256Base64(String password) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return "SHA256:" + Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      return password; // Fallback to plain text
    }
  }

  /** Logout current session - invalidate HttpSession and clear remember me cookie */
  public static void logout() {
    try {
      HttpServletRequest request = RWT.getRequest();
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }

      // Clear remember me cookie
      Cookie rememberCookie = new Cookie(COOKIE_REMEMBER_ME, "");
      rememberCookie.setMaxAge(0); // Immediately expire
      rememberCookie.setPath("/");
      RWT.getResponse().addCookie(rememberCookie);
    } catch (Exception e) {
      // Ignore logout errors
    }
  }
}
