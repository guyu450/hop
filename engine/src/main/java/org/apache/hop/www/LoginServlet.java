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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.hop.core.Const;
import org.apache.hop.core.encryption.Encr;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.util.Utils;
import org.eclipse.jetty.util.security.Password;

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
      return false;
    }

    // First check HopServerMeta configuration
    if (serverConfig != null && serverConfig.getHopServer() != null) {
      String configUsername = serverConfig.getHopServer().getUsername();
      String configPassword = serverConfig.getHopServer().getPassword();

      if (!Utils.isEmpty(configPassword)) {
        // Validate against HopServerMeta
        String decryptedPassword = Encr.decryptPasswordOptionallyEncrypted(configPassword);
        if (username.equals(configUsername) && password.equals(decryptedPassword)) {
          return true;
        }
        // If HopServerMeta has password configured, don't check pwd file
        return false;
      }
    }

    // Check pwd/hop.pwd file
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
        String[] parts = line.split(":");
        if (parts.length >= 2) {
          String fileUsername = parts[0].trim();
          if (fileUsername.equals(username)) {
            String credentials = parts[1].trim();
            // Split password and roles
            String[] credParts = credentials.split(",");
            String storedPassword = credParts[0].trim();

            // Check if password is obfuscated (OBF:, MD5:, CRYPT:)
            if (storedPassword.startsWith("OBF:")) {
              // Jetty OBF password - deobfuscate and compare
              Password pwd = new Password(storedPassword);
              return pwd.toString().equals(password);
            } else if (storedPassword.startsWith("MD5:") || storedPassword.startsWith("CRYPT:")) {
              // Hashed passwords - use Jetty Password to check
              Password pwd = new Password(storedPassword);
              return pwd.equals(password);
            } else {
              // Plain text password
              return storedPassword.equals(password);
            }
          }
        }
      }
    } catch (IOException e) {
      log.logError("Error reading password file: " + passwordFile, e);
    }

    return false;
  }

  @Override
  public String toString() {
    return "Login Servlet";
  }
}
