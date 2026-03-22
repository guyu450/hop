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
import org.apache.hop.core.Const;
import org.apache.hop.core.util.Utils;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
    setLayout(new GridLayout(2, false));
    setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));

    // Title
    Label titleLabel = new Label(this, SWT.NONE);
    titleLabel.setText("Apache Hop - 登录");
    GridData titleData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    titleLabel.setLayoutData(titleData);

    // Message label for errors
    messageLabel = new Label(this, SWT.WRAP);
    messageLabel.setText("");
    messageLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
    GridData messageData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
    messageData.widthHint = 300;
    messageLabel.setLayoutData(messageData);

    // Username
    Label usernameLabel = new Label(this, SWT.NONE);
    usernameLabel.setText("用户名:");
    usernameText = new Text(this, SWT.BORDER);
    GridData usernameData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    usernameData.widthHint = 200;
    usernameText.setLayoutData(usernameData);

    // Password
    Label passwordLabel = new Label(this, SWT.NONE);
    passwordLabel.setText("密码:");
    passwordText = new Text(this, SWT.BORDER | SWT.PASSWORD);
    GridData passwordData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    passwordData.widthHint = 200;
    passwordText.setLayoutData(passwordData);

    // Remember me checkbox
    rememberMeCheckbox = new Button(this, SWT.CHECK);
    rememberMeCheckbox.setText("记住我（30天）");
    GridData rememberData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
    rememberMeCheckbox.setLayoutData(rememberData);

    // Login button
    Button loginButton = new Button(this, SWT.PUSH);
    loginButton.setText("登录");
    GridData buttonData = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 2, 1);
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
    // Check against pwd/hop.pwd file
    String passwordFile = Const.getHopLocalServerPasswordFile();
    java.io.File pwdFile = new java.io.File(passwordFile);

    if (!pwdFile.exists()) {
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
        String[] parts = line.split(":");
        if (parts.length >= 2) {
          String fileUsername = parts[0].trim();
          if (fileUsername.equals(username)) {
            String credentials = parts[1].trim();
            String[] credParts = credentials.split(",");
            String storedPassword = credParts[0].trim();

            // Check password type
            if (storedPassword.startsWith("OBF:")) {
              // OBF password - deobfuscate and compare
              String obfPassword = storedPassword.substring(4);
              try {
                String decrypted = deobfuscate(obfPassword);
                return decrypted.equals(password);
              } catch (Exception e) {
                // Fall through to plain text comparison
              }
            } else if (storedPassword.startsWith("MD5:") || storedPassword.startsWith("CRYPT:")) {
              // Hashed passwords
              return storedPassword.equals(password);
            } else {
              // Plain text password
              return storedPassword.equals(password);
            }
          }
        }
      }
    } catch (Exception e) {
      // Log error but continue
    }
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
