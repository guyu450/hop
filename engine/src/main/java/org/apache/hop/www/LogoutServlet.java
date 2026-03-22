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
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.apache.hop.core.logging.ILogChannel;
import org.apache.hop.core.logging.LogChannel;

/** Servlet to handle user logout. Invalidates the session and redirects to login page. */
public class LogoutServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;
  private static final ILogChannel log = new LogChannel("LogoutServlet");
  public static final String CONTEXT_PATH = "/logout";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    HttpSession session = request.getSession(false);
    if (session != null) {
      String username = (String) session.getAttribute("username");
      session.invalidate();

      if (log.isBasic()) {
        log.logBasic("User '" + username + "' logged out");
      }
    }

    // Redirect to login page with logout message
    response.sendRedirect(request.getContextPath() + "/login/login.html?error=logout");
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  @Override
  public String toString() {
    return "Logout Servlet";
  }
}
