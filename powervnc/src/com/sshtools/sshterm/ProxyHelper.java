/*
 *  GSI-SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2007 STFC/CCLRC.
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

/**
 *
 * This class provides some helpful routines for proxy handling
 */

package com.sshtools.sshterm;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.globus.common.CoGProperties;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.util.Util;

import com.sshtools.j2ssh.configuration.SshConnectionProperties;

public class ProxyHelper implements Runnable {


    public static boolean proxyExists() {
	String filename = CoGProperties.getDefault().getProxyFile();
	if(filename==null) return false;
	File file = new File(filename);
	return file.exists() && file.isFile();
    }

    public static void saveProxy(GlobusCredential theProxy, SshConnectionProperties props) {
	String proxyFile = CoGProperties.getDefault().getProxyFile();			 
        OutputStream out = null;
        try {
            File file = Util.createFile(proxyFile);
            if (!Util.setOwnerAccessOnly(proxyFile)) {
                System.out.println("Warning: could not set permissions on proxy file.");
		//message("Warning: could not set permissions on proxy file.");
            }
            out = new FileOutputStream(file);
            theProxy.save(out);
	    com.sshtools.common.util.ShutdownHooks.runOnExit(new ProxyHelper());
       } catch (SecurityException e) {
            System.out.println("Failed to save proxy to proxy file: " + e.getMessage());
	    message(props, "Failed to save proxy to proxy file: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Failed to save proxy to proxy file: " + e.getMessage());
	    message(props, "Failed to save proxy to proxy file: " + e.getMessage());
        } finally {
            if (out != null) {
                try { 
		    out.close(); 
		} catch(Exception e) {}
            }
        }
    }


    public void run() {
	if(proxyExists()) {
	    int ret = JOptionPane.showConfirmDialog(null, "You have a proxy certificate stored on a disk which may have been created by this terminal, do you want to delete it?", "GSI-SSHTerm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
	    if(ret==JOptionPane.YES_OPTION) destroyProxy();
	}
    }

    private static void message(SshConnectionProperties props, String s) {
	JOptionPane.showMessageDialog(props.getWindow(), s, "Problem saving proxy", JOptionPane.ERROR_MESSAGE);
		
    }

    public static void showProxyInfo(java.awt.Component parent) {
	String identity="", subject="", issuer="", lifetime="", storage="", type="";
	CoGProperties cogproperties = CoGProperties.getDefault();

	try {
	    if (!(new File(cogproperties.getProxyFile())).exists()) {
		storage = "No local proxy.";
	    } else {
		GlobusCredential globuscredential = new GlobusCredential(cogproperties.getProxyFile());
		globuscredential.verify();
		storage = "Local File: "+cogproperties.getProxyFile();
		subject = CertUtil.toGlobusID(globuscredential.getSubject(),true);
		issuer = CertUtil.toGlobusID(globuscredential.getIssuer(),true);
		identity = CertUtil.toGlobusID(globuscredential.getIdentityCertificate().getSubjectDN().toString(),true);
		long seconds = globuscredential.getTimeLeft();
		long days = seconds /(60*60*24);
		seconds = (seconds - (days*60*60*24));
		long hours = seconds / (60*60);
		seconds = (seconds - (hours*60*60));
		long mins = seconds / 60;
		seconds = (seconds - (mins*60));
		lifetime = days+" days "+hours+" hours "+mins+" minutes "+seconds+" seconds.";
	        int typeI = globuscredential.getProxyType();
		type = (typeI == -1) ? "failed to determine certificate type" : CertUtil.getProxyTypeAsString(typeI);
		if(typeI==GSIConstants.EEC) type = "end entity certificate";
	    } 
	} catch(GlobusCredentialException globuscredentialexception) {
	    if(globuscredentialexception.getMessage().indexOf("Expired") >= 0) {
		File file = new File(cogproperties.getProxyFile());
		file.delete();
		storage = "Expired local proxy.";
	    } else {
		storage = "No local proxy (Error).";			
	    }
	}
	JComponent panel = getInfoPanel(identity, subject, issuer, lifetime, storage, type);
	JOptionPane.showMessageDialog(parent, panel, "Disk Proxy Information", JOptionPane.PLAIN_MESSAGE);
    }

    public static void destroyProxy() {
	String filename = CoGProperties.getDefault().getProxyFile();
	if(filename!=null) {
	    Util.destroy(new File(filename));
	}
    }


    private static JComponent getInfoPanel(String identity, String subject, String issuer, String lifetime, String storage, String type) {
	JPanel infoResultPanel = new JPanel();
	infoResultPanel.setLayout(new GridBagLayout());
	GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
	gridBagConstraints1.anchor = GridBagConstraints.NORTHWEST;
	gridBagConstraints1.gridx = 0;
	gridBagConstraints1.gridy = 0;
	gridBagConstraints1.insets = new Insets(20, 20, 0, 0);
	JLabel infoResulTitleLabel = new JLabel();
	infoResulTitleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
	infoResulTitleLabel.setText("Proxy Information:");
	GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
	gridBagConstraints2.gridy = 1;
	infoResultPanel.setLayout(new GridBagLayout());
	infoResultPanel.add(getInfoResultMainPanel(identity, subject, issuer, lifetime, storage, type), gridBagConstraints2);
	infoResultPanel.add(infoResulTitleLabel, gridBagConstraints1);
	return infoResultPanel;
    }



    private static JPanel getInfoResultMainPanel(String identity, String subject, String issuer, String lifetime, String storage, String type) {
	JPanel infoResultMainPanel = new JPanel();
	GridBagConstraints gridBagConstraintsA = new GridBagConstraints();
	gridBagConstraintsA.fill = GridBagConstraints.VERTICAL;
	gridBagConstraintsA.weightx = 1.0;
	gridBagConstraintsA.insets = new Insets(3, 3, 3, 3);
	gridBagConstraintsA.ipadx = 0;
	gridBagConstraintsA.gridx = 2;
	gridBagConstraintsA.gridy = 1;
	GridBagConstraints gridBagConstraintsB = new GridBagConstraints();
	gridBagConstraintsB.insets = new Insets(3, 3, 3, 3);
	gridBagConstraintsB.anchor = GridBagConstraints.WEST;
	gridBagConstraintsB.gridx = 1;
	gridBagConstraintsB.gridy = 1;

	JLabel infoResultTypeLabel = new JLabel();
	infoResultTypeLabel.setText("Type:");
	JLabel infoResultIdentityLabel = new JLabel();
	infoResultIdentityLabel.setText("Identity: ");
	JLabel infoResultLifetimeLabel = new JLabel();
	infoResultLifetimeLabel.setText("Lifetime: ");
	JLabel infoResultIssuerLabel = new JLabel();
	infoResultIssuerLabel.setText("Issuer: ");
	JLabel infoResultSubjectLabel = new JLabel();
	infoResultSubjectLabel.setText("Subject: ");
	JLabel infoResultStorageLabel = new JLabel();
	infoResultStorageLabel.setText("Storage: ");
	
	JTextField infoResultIdentityTextField = new JTextField(identity);
	infoResultIdentityTextField.setEditable(false);
	infoResultIdentityTextField.setColumns(50);
	JTextField infoResultSubjectTextField = new JTextField(subject);
	infoResultSubjectTextField.setEditable(false);
	infoResultSubjectTextField.setColumns(50);
	JTextField infoResultStorageTextField = new JTextField(storage);
	infoResultStorageTextField.setEditable(false);
	infoResultStorageTextField.setColumns(50);
	JTextField infoResultIssuerTextField = new JTextField(issuer);
	infoResultIssuerTextField.setEditable(false);
	infoResultIssuerTextField.setColumns(50);
	JTextField infoResultTypeTextField = new JTextField(type);
	infoResultTypeTextField.setEditable(false);
	infoResultTypeTextField.setColumns(50);
	JTextField infoResultLifetimeTextField = new JTextField(lifetime);
	infoResultLifetimeTextField.setEditable(false);
	infoResultLifetimeTextField.setColumns(50);

	infoResultMainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
	infoResultMainPanel.setLayout(new GridBagLayout());

	infoResultMainPanel.add(infoResultStorageLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultStorageTextField, gridBagConstraintsA);
	gridBagConstraintsB.gridy = 2;
	gridBagConstraintsA.gridy = 2;
	
	infoResultMainPanel.add(infoResultIdentityLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultIdentityTextField, gridBagConstraintsA);

	gridBagConstraintsB.gridy = 3;
	gridBagConstraintsA.gridy = 3;
	infoResultMainPanel.add(infoResultSubjectLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultSubjectTextField, gridBagConstraintsA);

	gridBagConstraintsB.gridy = 4;
	gridBagConstraintsA.gridy = 4;
	infoResultMainPanel.add(infoResultIssuerLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultIssuerTextField, gridBagConstraintsA);

	gridBagConstraintsB.gridy = 5;
	gridBagConstraintsA.gridy = 5;
	infoResultMainPanel.add(infoResultLifetimeLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultLifetimeTextField, gridBagConstraintsA);

	gridBagConstraintsB.gridy = 6;
	gridBagConstraintsA.gridy = 6;
	infoResultMainPanel.add(infoResultTypeLabel, gridBagConstraintsB);
	infoResultMainPanel.add(infoResultTypeTextField, gridBagConstraintsA);
	
	return infoResultMainPanel;
    }
}
