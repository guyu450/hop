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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.apache.hop.core.extension.ExtensionPointHandler;
import org.apache.hop.core.extension.HopExtensionPoint;
import org.apache.hop.ui.core.PropsUi;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.client.service.StartupParameters;
import org.eclipse.rap.rwt.widgets.WidgetUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class HopWebEntryPoint extends AbstractEntryPoint {

  private boolean loginDialogShown = false;

  @Override
  protected void createContents(Composite parent) {
    // Check authentication first
    if (!HopLoginDialog.isAuthenticated()) {
      showLoginDialog(parent);
      return;
    }

    // User is authenticated, proceed with normal UI
    createMainContents(parent);
  }

  private void showLoginDialog(Composite parent) {
    if (loginDialogShown) {
      return; // Prevent multiple dialogs
    }
    loginDialogShown = true;

    HopLoginDialog loginDialog =
        new HopLoginDialog(
            parent,
            () -> {
              // Login callback - recreate the UI with authenticated session
              // Execute synchronously to ensure proper widget state
              loginDialogShown = false;

              // Clear all children and create main contents
              Control[] children = parent.getChildren();
              for (Control child : children) {
                if (!child.isDisposed()) {
                  child.dispose();
                }
              }
              createMainContents(parent);
              parent.layout(true, true);
            });
  }

  private void createMainContents(Composite parent) {
    // Transferring Widget Data for client-side canvas drawing instructions
    WidgetUtil.registerDataKeys("props");
    WidgetUtil.registerDataKeys("mode");
    WidgetUtil.registerDataKeys("nodes");
    WidgetUtil.registerDataKeys("hops");
    WidgetUtil.registerDataKeys("notes");
    // WidgetUtil.registerDataKeys("svg");

    //  The following options are session specific.
    //
    StartupParameters serviceParams = RWT.getClient().getService(StartupParameters.class);
    List<String> args = new ArrayList<>();
    String[] options = {"user", "pass", "file"};
    for (String option : options) {
      if (serviceParams.getParameter(option) != null) {
        args.add("-" + option + "=" + serviceParams.getParameter(option));
      }
    }

    // Execute Spoon.createContents
    HopGui.getInstance().setCommandLineArguments(args);
    HopGui.getInstance().setShell(parent.getShell());
    HopGui.getInstance().setProps(PropsUi.getInstance());
    try {
      ExtensionPointHandler.callExtensionPoint(
          HopGui.getInstance().getLog(),
          HopGui.getInstance().getVariables(),
          HopExtensionPoint.HopGuiInit.id,
          HopGui.getInstance());
    } catch (Exception e) {
      HopGui.getInstance()
          .getLog()
          .logError("Error calling extension point plugin(s) for HopGuiInit", e);
    }

    HopGui.getInstance().open();

    // Add logout button to the main toolbar after HopGui is initialized
    addLogoutButtonToToolbar();
  }

  /** Add logout button to HopGui's main toolbar (right side) */
  private void addLogoutButtonToToolbar() {
    try {
      // Get the mainToolbar field from HopGui using reflection
      Field mainToolbarField = HopGui.class.getDeclaredField("mainToolbar");
      mainToolbarField.setAccessible(true);
      ToolBar mainToolbar = (ToolBar) mainToolbarField.get(HopGui.getInstance());

      if (mainToolbar != null && !mainToolbar.isDisposed()) {
        // Add a separator first (at the end)
        new ToolItem(mainToolbar, SWT.SEPARATOR);

        // Create logout tool item at the end of the toolbar (right side)
        ToolItem logoutItem = new ToolItem(mainToolbar, SWT.PUSH);
        logoutItem.setText("退出");

        // Get current username
        String username = HopLoginDialog.getUsername();
        logoutItem.setToolTipText("退出登录 (当前用户: " + (username != null ? username : "未知") + ")");

        logoutItem.addSelectionListener(
            new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                handleLogout();
              }
            });

        // Repack the toolbar to reflect changes
        mainToolbar.pack();
      }
    } catch (Exception e) {
      // Log error but don't break the application
      System.err.println("Error adding logout button to toolbar: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Handle logout action - redirect to /ui to show login dialog */
  private void handleLogout() {
    // Clear session
    HopLoginDialog.logout();

    // Use JavaScript to redirect to /ui to show login dialog
    String js = "window.location.href = window.location.origin + '/ui'";
    org.eclipse.rap.rwt.client.service.JavaScriptExecutor jsExecutor =
        RWT.getClient().getService(org.eclipse.rap.rwt.client.service.JavaScriptExecutor.class);
    jsExecutor.execute(js);
  }
}
