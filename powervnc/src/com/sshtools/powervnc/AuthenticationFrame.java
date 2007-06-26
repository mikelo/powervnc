//Changes (c) CCLRC 2006
/*
 *  Sshtools - SshVNC
 *
 *  Copyright (C) 2002 Lee David Painter.
 *
 *  Written by: 2002 Lee David Painter <lee@sshtools.com>
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU Library General Public
 *  License along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.sshtools.powervnc;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;

import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.common.ui.IconWrapperPanel;
import com.sshtools.common.ui.ResourceIcon;
import com.sshtools.common.ui.StandardAction;
import com.sshtools.j2ssh.SshException;

class AuthenticationFrame
	extends JPanel
    implements ActionListener {
	private PowerVNCPanel grandparent;
	private JPasswordField passwordField;
	private String password; 
	private JButton okButton;
  private JTextField usernameField;
  private JTextField hostField;
  private Image image;
  final static int LEFT = 380;
  final static int TOP = 75;
  private VNCMain parent;
  private SshToolsConnectionProfile profile;
  //
  // Constructor.
  //

  AuthenticationFrame(VNCMain par) {
	  parent = par;
	  grandparent = parent.parent;
	  parent.removeAll();
	  parent.setLayout(new BorderLayout());
	  profile = new SshToolsConnectionProfile();
	  SpringLayout layout = new SpringLayout();
      setLayout(layout);
      final ImageIcon imageIcon = new ResourceIcon("/com/sshtools/powervnc/login.png");
      image = imageIcon.getImage();
	  setOpaque(false);
/*
	  JPanel p = new JPanel(layout) {
          final ImageIcon imageIcon = new ResourceIcon("/com/sshtools/powervnc/login.png");
          Image image = imageIcon.getImage();
          {
        	  setOpaque(false);
        	  }     	  
          public void paintComponent (Graphics g) {
              g.drawImage(image, 0, 0, this);
              super.paintComponent(g);
            }     
      };
 */     
      usernameField = new JTextField(8);
      hostField = new JTextField(8);
      passwordField = new JPasswordField(8);

      okButton = new JButton("Connect");
      okButton.setSize(20, 5);
      okButton.setMnemonic('c');
      okButton.addActionListener(this);
      okButton.setDefaultCapable(true);

      layout.putConstraint(SpringLayout.SOUTH, usernameField, TOP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.WEST, usernameField, LEFT, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.SOUTH, passwordField, TOP + 55, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.WEST, passwordField, LEFT, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.SOUTH, hostField, TOP + 110, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.WEST, hostField, LEFT, SpringLayout.WEST, this);
     layout.putConstraint(SpringLayout.SOUTH, okButton, TOP + 150, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.WEST, okButton, LEFT + 12, SpringLayout.WEST, this);
 
     add(usernameField);
     add(passwordField);
     add(hostField);
     add(okButton);
    parent.add(this, BorderLayout.CENTER);
//     System.out.println(getSize().width+ "x"+getSize().height);

    //setAlwaysOnTop(true);


    // Set the default action for the hitting of the return key
    passwordField.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "Enter");
    passwordField.getActionMap().put("Enter", new SendPasswordAction());
//    repaint();
    parent.validate();
/*
    IconWrapperPanel w = new IconWrapperPanel(
        new ResourceIcon("/com/sshtools/sshvnc/largevnc.png"), t);

    w.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
*/
  }

  public char[] doAuthentication() {
//    System.out.println(getPassword());
    return getPassword();
  }

  private char[] getPassword() {
    return passwordField.getPassword();
  }

  public JButton getDefaultButton() {
    return okButton;
  }

  public void actionPerformed(ActionEvent evt) {
	  if(usernameField.getText().length() > 0 && passwordField.getPassword().length > 0 && hostField.getText().length() > 0) {
//		  profile.conn
//		  grandparent.set
		  password = new String(passwordField.getPassword());
		  grandparent.password = password;
		  profile.setUsername(usernameField.getText());
		  profile.setAllowAgentForwarding(true);
		  profile.setHost(hostField.getText());
		  grandparent.connect(profile, true);
	  }
//	  profile.
	  //	  grandparent.
	  /*	  
	  try {
		  grandparent.connect();
	} catch (SshException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
*/
	  new SendPasswordAction();
  }


  public void paintComponent (Graphics g) {
      g.drawImage(image, 0, 0, this);
      super.paintComponent(g);
    }     

  class SendPasswordAction
      extends StandardAction {

    private final static String NAME_CONFIG = "OK";

    public SendPasswordAction() {
        System.out.println("hello");
    	putValue(Action.NAME, NAME_CONFIG);
    }

    public void actionPerformed(ActionEvent evt) {

    }
  }
}
