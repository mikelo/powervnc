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

package com.sshtools.common.keygen;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public class KeygenPanel2
    extends JPanel {
  //  Actions

  /**  */
  public final static int GENERATE_KEY_PAIR = 0;

  /**  */
  public final static int CONVERT_IETF_SECSH_TO_OPENSSH = 1;

  /**  */
  public final static int CONVERT_OPENSSH_TO_IETF_SECSH = 2;

  /**  */
  public final static int CHANGE_PASSPHRASE = 3;

  //  Private instance variables
  private JButton browseInput;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel jPanel1 = new JPanel();
  JTabbedPane key1Panel = new JTabbedPane();
  JPanel key1GeneratePanel = new JPanel();
  JPanel file = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JPanel key1DataPanel = new JPanel();
  JScrollPane jScrollPane1 = new JScrollPane();
  JTextArea jTextArea1 = new JTextArea();
  GridLayout gridLayout1 = new GridLayout();
  TitledBorder titledBorder1;
  TitledBorder titledBorder2;
  GridBagLayout gridBagLayout1 = new GridBagLayout();
  JLabel jLabel1 = new JLabel();
  JTextField jTextField1 = new JTextField();
  JButton jButton1 = new JButton();
  JButton jButton2 = new JButton();

  /**
   * Creates a new KeygenPanel2 object.
   */
  public KeygenPanel2() {
    super();

    try {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void jbInit() throws Exception {
    titledBorder1 = new TitledBorder("Data");
    titledBorder2 = new TitledBorder("Key 1");
    this.setLayout(borderLayout1);
    jPanel1.setLayout(borderLayout2);
    jTextArea1.setText("jTextArea1");
    key1DataPanel.setLayout(gridLayout1);
    key1DataPanel.setBorder(titledBorder1);
    jPanel1.setBorder(titledBorder2);
    file.setLayout(gridBagLayout1);
    jLabel1.setText("File:");
    jTextField1.setText("jTextField1");
    jButton1.setText("Open");
    jButton2.setText("Browse");
    this.add(jPanel1, BorderLayout.CENTER);
    jPanel1.add(key1Panel, BorderLayout.NORTH);
    key1Panel.add(key1GeneratePanel, "Generate");
    key1Panel.add(file, "File");
    jPanel1.add(key1DataPanel, BorderLayout.CENTER);
    key1DataPanel.add(jScrollPane1, null);
    jScrollPane1.getViewport().add(jTextArea1, null);
    file.add(jLabel1,
             new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER,
                                    GridBagConstraints.HORIZONTAL,
                                    new Insets(2, 2, 0, 3), 0, 0));
    file.add(jTextField1,
             new GridBagConstraints(1, 0, 1, 1, 1.0, 0.0,
                                    GridBagConstraints.CENTER,
                                    GridBagConstraints.HORIZONTAL,
                                    new Insets(2, 2, 0, 0), 0, 0));
    file.add(jButton1,
             new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER,
                                    GridBagConstraints.HORIZONTAL,
                                    new Insets(2, 2, 0, 2), 0, 0));
    file.add(jButton2,
             new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER,
                                    GridBagConstraints.HORIZONTAL,
                                    new Insets(2, 0, 0, 2), 0, 0));
  }
}
