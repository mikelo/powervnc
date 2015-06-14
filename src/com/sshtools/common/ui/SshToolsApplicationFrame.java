//Changes (c) STFC/CCLRC 2007
/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */

package com.sshtools.common.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.powervnc.PowerVNC;
import com.sun.org.apache.xerces.internal.impl.RevalidationHandler;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public class SshToolsApplicationFrame
    extends JFrame
    implements SshToolsApplicationContainer {
  //  Preference names

  /**  */
  public final static String PREF_LAST_FRAME_GEOMETRY =
      "application.lastFrameGeometry";

  /**  */
  protected Log log = LogFactory.getLog(SshToolsApplicationFrame.class);

  /**  */
  protected StandardAction exitAction;

  /**  */
  protected StandardAction aboutAction;

  /**  */
  protected StandardAction newWindowAction;

  /**  */
  protected JSeparator toolSeparator;

  //
  private SshToolsApplicationPanel panel;
  private SshToolsApplication application;

  private boolean showAboutBox = true;
  private boolean showExitAction = true;
  private boolean showNewWindowAction = true;
  private boolean showMenu = false;

  public void showAboutBox(boolean showAboutBox) {
    this.showAboutBox = showAboutBox;
  }

  public void showExitAction(boolean showExitAction) {
    this.showExitAction = showExitAction;
  }

  public void showNewWindowAction(boolean showNewWindowAction) {
    this.showNewWindowAction = showNewWindowAction;
  }


  public void actualize() {
	  setSize(800, 8000);
  }
  
  
  
  
  /**
   *
   *
   * @param application
   * @param panel
   *
   * @throws SshToolsApplicationException
   */
  public void init(final SshToolsApplication application,
                   SshToolsApplicationPanel panel) throws
      SshToolsApplicationException {
    this.panel = panel;
    this.application = application;

    if (application != null) {
      setTitle(ConfigurationLoader.getVersionString(
          application.getApplicationName(),
          application.getApplicationVersion())); // + " " + application.getApplicationVersion());
    }

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    // Register the File menu
    panel.registerActionMenu(new SshToolsApplicationPanel.ActionMenu(
        "File", "File", 'f', 0));
    // Register the Exit action
    if (showExitAction && application != null) {
      panel.registerAction(exitAction = new ExitAction(application, this));

      // Register the New Window Action
    }
    if (showNewWindowAction && application != null) {
      panel.registerAction(newWindowAction = new NewWindowAction(application));

      // Register the Help menu
    }
    panel.registerActionMenu(new SshToolsApplicationPanel.ActionMenu(
        "Help", "Help", 'h', 99));

    // Register the About box action
    if (showAboutBox && application != null) {
      panel.registerAction(aboutAction = new AboutAction(this, application));

    }

    getApplicationPanel().rebuildActionComponents();

    JPanel p = new JPanel(new BorderLayout());

    if (panel.getJMenuBar() != null && showMenu) {
      setJMenuBar(panel.getJMenuBar());
    }

    if (panel.getToolBar() != null) {
      JPanel t = new JPanel(new BorderLayout());
      t.add(panel.getToolBar(), BorderLayout.NORTH);
      t.add(toolSeparator = new JSeparator(JSeparator.HORIZONTAL),
            BorderLayout.SOUTH);
      toolSeparator.setVisible(panel.getToolBar().isVisible());

      final SshToolsApplicationPanel pnl = panel;
      panel.getToolBar().addComponentListener(new ComponentAdapter() {
        public void componentHidden(ComponentEvent evt) {
          log.debug("Tool separator is now "
                    + pnl.getToolBar().isVisible());
          toolSeparator.setVisible(pnl.getToolBar().isVisible());
        }
      });
      p.add(t, BorderLayout.NORTH);
    }

    p.add(panel, BorderLayout.CENTER);

    if (panel.getStatusBar() != null) {
      p.add(panel.getStatusBar(), BorderLayout.SOUTH);
    }

//    getContentPane().setLayout(new GridLayout(1, 1));
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(p);

    // Watch for the frame closing
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        if (application != null) {
          application.closeContainer(SshToolsApplicationFrame.this);
        }
        else {
          int confirm = JOptionPane.showOptionDialog(
                  SshToolsApplicationFrame.this,
                  "Close " + getTitle() + "?",
                  "Close Operation",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE,
                  null, null, null);
              if (confirm == 0) {
                hide();
              }

        }
      }
    });

    // If this is the first frame, center the window on the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    boolean found = false;

    if (application != null && application.getContainerCount() != 0) {
      for (int i = 0; (i < application.getContainerCount()) && !found;
           i++) {
        SshToolsApplicationContainer c = application.getContainerAt(i);
        System.out.println("ci 6?");

        if (c instanceof SshToolsApplicationFrame) {
          SshToolsApplicationFrame f = (SshToolsApplicationFrame) c;
//         setSize(800, 800);

//          setSize(557, 326);
//          f.setResizable(false);
//          setBounds(new Rectangle(1000, 1000));   //PreferencesStore.getRectangle(PREF_LAST_FRAME_GEOMETRY, getBounds()));
         Point newLocation = new Point(f.getX(), f.getY());
          newLocation.x += 48;
          newLocation.y += 48;

          if (newLocation.x > (screenSize.getWidth() - 64)) {
            newLocation.x = 0;
          }

          if (newLocation.y > (screenSize.getHeight() - 64)) {
            newLocation.y = 0;
          }

          setLocation(newLocation);
          found = true;
        }
      }
    }

    if (!found) {
      // Is there a previous stored geometry we can use?
 //     if (PreferencesStore.preferenceExists(PREF_LAST_FRAME_GEOMETRY)) {
//    	Integer w = new Integer(screenSize.getWidth())
//    	setBounds(Powua.w, Powua.h, 200, 300);//(int) screenSize.getWidth(), (int) screenSize.getHeight());   //PreferencesStore.getRectangle(PREF_LAST_FRAME_GEOMETRY, getBounds()));
        setResizable(false);
//        int width = Toolkit.getDefaultToolkit().getScreenSize().width;
//        int height = Toolkit.getDefaultToolkit().getScreenSize().height;
//        setBounds(0, 0, screenSize.width, screenSize.height);
        setSize(PowerVNC.w, PowerVNC.h);
  	  System.out.println("eccomi");
//                panel.setSize(Powua.w, Powua.h);
    }
      else {
        pack();
//          setBounds(new Rectangle(1000, 1000));   //PreferencesStore.getRectangle(PREF_LAST_FRAME_GEOMETRY, getBounds()));
//          setPreferredSize(new Dimension(1000, 1000));
//    	  panel.setSize(1000, 1000);
//          panel.revalidate();
//          doLayout();
//          validate();
//        setSize(new Dimension(1000, 1000));
//        panel.revalidate();
//        hide();
//    	  System.out.println("eccomi");
    	  UIUtil.positionComponent(SwingConstants.CENTER, this);
//      }
    }
  }

  /**
   *
   *
   * @param title
   */
  public void setContainerTitle(String title) {
    setTitle(title);
  }

  /**
   *
   *
   * @return
   */
  public SshToolsApplication getApplication() {
    return application;
  }

  /**
   *
   *
   * @param visible
   */
  public void setContainerVisible(boolean visible) {
    setVisible(visible);
  }

  /**
   *
   *
   * @return
   */
  public boolean isContainerVisible() {
    return isVisible();
  }

  /**
   *
   *
   * @return
   */
  public SshToolsApplicationPanel getApplicationPanel() {
    return panel;
  }

  /**
   *
   */
  public void closeContainer() {
    /*  If this is the last frame to close, then store its geometry for use
        when the next frame opens */
    if (application != null && application.getContainerCount() == 1) {
      PreferencesStore.putRectangle(PREF_LAST_FRAME_GEOMETRY, getBounds());
    }

    dispose();
    getApplicationPanel().deregisterAction(newWindowAction);
    getApplicationPanel().deregisterAction(exitAction);
    getApplicationPanel().deregisterAction(aboutAction);
    getApplicationPanel().rebuildActionComponents();
  }

    public java.awt.Component getWindow() { return this; }

}
