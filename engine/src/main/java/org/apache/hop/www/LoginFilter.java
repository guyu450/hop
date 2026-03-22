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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;

/**
 * Filter to check if user is authenticated via session. Redirects to login page if not
 * authenticated.
 */
public class LoginFilter implements Filter {

  private static final Class<?> PKG = LoginFilter.class;
  private static final ILogChannel log = new LogChannel("LoginFilter");

  // Paths that don't require authentication
  private static final Set<String> EXCLUDE_PATHS =
      new HashSet<>(
          Arrays.asList(
              "/login", // Login page and servlet
              "/static", // Static resources (CSS, JS, images)
              "/api")); // REST API (uses Basic Auth)

  private FilterConfig filterConfig;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    this.filterConfig = filterConfig;
    log.logBasic("LoginFilter initialized");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    String requestUri = httpRequest.getRequestURI();
    String contextPath = httpRequest.getContextPath();

    // Get the path without context path
    String path = requestUri.substring(contextPath.length());

    // Check if path should be excluded from authentication
    if (isExcludedPath(path)) {
      chain.doFilter(request, response);
      return;
    }

    // Check for existing session
    HttpSession session = httpRequest.getSession(false);
    if (session != null && session.getAttribute("authenticated") != null) {
      Boolean authenticated = (Boolean) session.getAttribute("authenticated");
      if (Boolean.TRUE.equals(authenticated)) {
        // User is authenticated, continue
        chain.doFilter(request, response);
        return;
      }
    }

    // Check for Basic Auth header (for API clients)
    String authHeader = httpRequest.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Basic ")) {
      // Let the container handle Basic Auth for API requests
      chain.doFilter(request, response);
      return;
    }

    // Not authenticated, redirect to login page
    if (log.isDebug()) {
      log.logDebug("Unauthenticated request to " + requestUri + ", redirecting to login");
    }

    httpResponse.sendRedirect(contextPath + "/login/login.html");
  }

  private boolean isExcludedPath(String path) {
    // Exact match
    if (EXCLUDE_PATHS.contains(path)) {
      return true;
    }

    // Path starts with excluded prefix
    for (String excludePath : EXCLUDE_PATHS) {
      if (path.startsWith(excludePath + "/") || path.equals(excludePath)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void destroy() {
    // Cleanup if needed
    log.logBasic("LoginFilter destroyed");
  }
}
