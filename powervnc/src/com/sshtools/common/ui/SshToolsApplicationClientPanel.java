//Changes (c) STFC/CCLRC 2006-2007
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

import java.awt.Frame;
import java.awt.LayoutManager;
import java.io.EOFException;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;
//import javax.tools.JavaFileManager;

//import net.sf.jftp.JFtp;

import net.sf.jftp.JFtp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.rl.esc.browser.Browser;

import com.sshtools.common.authentication.AuthenticationDialog;
import com.sshtools.common.authentication.BannerDialog;
import com.sshtools.common.authentication.KBIRequestHandlerDialog;
import com.sshtools.common.authentication.PasswordAuthenticationDialog;
import com.sshtools.common.authentication.PasswordChange;
import com.sshtools.common.authentication.PublicKeyAuthenticationPrompt;
import com.sshtools.common.automate.RemoteIdentification;
import com.sshtools.common.automate.RemoteIdentificationException;
import com.sshtools.common.automate.RemoteIdentificationFactory;
import com.sshtools.common.configuration.InvalidProfileFileException;
import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.common.hosts.DialogKnownHostsKeyVerification;
import com.sshtools.common.util.BrowserLauncher;
import com.sshtools.j2ssh.ScpClient;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.SshException;
import com.sshtools.j2ssh.SshThread;
import com.sshtools.j2ssh.agent.AgentAuthenticationClient;
import com.sshtools.j2ssh.agent.AgentNotAvailableException;
import com.sshtools.j2ssh.agent.SshAgentClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.EKEYXAuthenticationClient;
import com.sshtools.j2ssh.authentication.KBIAuthenticationClient;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClient;
import com.sshtools.j2ssh.authentication.SshAuthenticationClientFactory;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.IgnoreHostKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.MessageStoreEOFException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.TransportProtocolState;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.powua.Powua;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public abstract class SshToolsApplicationClientPanel
    extends SshToolsApplicationPanel {
  /**  */
  public final static String PREF_CONNECTION_FILE_DIRECTORY =
      "sshapps.connectionFile.directory";

  //

  /**  */
  public final static int BANNER_TIMEOUT = 200;

  /**  */
  protected static AbstractKnownHostsKeyVerification ver;

  //
  public String password;

  public String username;
  /**  */
  protected Log log = LogFactory.getLog(SshToolsApplicationClientPanel.class);

  /**  */
  protected HostKeyVerification hostKeyVerification;

  /**  */
  protected File currentConnectionFile;

  /**  */
  protected boolean needSave;

  /**  */
  protected SshToolsConnectionProfile currentConnectionProfile;

  /**  */
  protected javax.swing.filechooser.FileFilter connectionFileFilter = new
      ConnectionFileFilter();

  /**  */
  protected SshClient ssh;

  /**
   * Creates a new SshToolsApplicationClientPanel object.
   */
  public SshToolsApplicationClientPanel() {
    super();
  }

  /**
   * Creates a new SshToolsApplicationClientPanel object.
   *
   * @param mgr
   */
  public SshToolsApplicationClientPanel(LayoutManager mgr) {
    super(mgr);
  }

  /**
   *
   *
   * @return
   */
  public abstract SshToolsConnectionTab[] getAdditionalConnectionTabs();

  /**
   *
   *
   * @return
   */
  public HostKeyVerification getHostKeyVerification() {
    return hostKeyVerification;
  }

  /**
   *
   *
   * @param hostKeyVerification
   */
  public void setHostHostVerification(HostKeyVerification hostKeyVerification) {
    this.hostKeyVerification = hostKeyVerification;
  }

  /**
   *
   *
   * @param application
   *
   * @throws SshToolsApplicationException
   */
  public void init(SshToolsApplication application) throws
      SshToolsApplicationException {
	  super.init(application);
    //    setRequestFocusEnabled(requestFocusEnabled);
/*
    try {
      //if (ver == null) {
      //ver = new DialogKnownHostsKeyVerification(this);

      //}
      //setHostHostVerification(ver);

      if (ver.isHostFileWriteable()) {
        application.addAdditionalOptionsTab(new HostsTab(ver));
      }
    }
    catch (InvalidHostFileException uhfe) {
      log.warn("Host key verification will be DISABLED.", uhfe);
    }
*/
  }

  /**
   *
   */
  public void editConnection() {
    // Create a file chooser with the current directory set to the
    // application home
    JFileChooser fileDialog = new JFileChooser(PreferencesStore.get(
        PREF_CONNECTION_FILE_DIRECTORY,
        System.getProperty("sshtools.home",
                           System.getProperty("user.home"))));
    fileDialog.setFileFilter(connectionFileFilter);

    // Show it
    int ret = fileDialog.showOpenDialog(this);

    // If we've approved the selection then process
    if (ret == fileDialog.APPROVE_OPTION) {
      PreferencesStore.put(PREF_CONNECTION_FILE_DIRECTORY,
                           fileDialog.getCurrentDirectory().getAbsolutePath());

      // Get the file
      File f = fileDialog.getSelectedFile();

      // Load the profile
      SshToolsConnectionProfile p = new SshToolsConnectionProfile();

      try {
        p.open(f);

        if (editConnection(p)) {
          saveConnection(false, f, p);
        }
      }
      catch (IOException ioe) {
        showErrorMessage(this, "Failed to load connection profile.",
                         "Error", ioe);
      }
    }
  }

  /**
   *
   *
   * @param profile
   *
   * @return
   */
  public SshToolsConnectionProfile newConnectionProfile(
      SshToolsConnectionProfile profile) {
    return SshToolsConnectionPanel.showConnectionDialog(
        SshToolsApplicationClientPanel.this,
        profile, getAdditionalConnectionTabs());
  }

  /**
   *
   */
  public void open() {
    // Create a file chooser with the current directory set to the
    // application home
    String prefsDir = super.getApplication().getApplicationPreferencesDirectory().
        getAbsolutePath();
    JFileChooser fileDialog = new JFileChooser(prefsDir);

    fileDialog.setFileFilter(connectionFileFilter);

    // Show it
    int ret = fileDialog.showOpenDialog(this);

    // If we've approved the selection then process
    if (ret == fileDialog.APPROVE_OPTION) {
      PreferencesStore.put(PREF_CONNECTION_FILE_DIRECTORY,
                           fileDialog.getCurrentDirectory().getAbsolutePath());

      // Get the file
      File f = fileDialog.getSelectedFile();
      open(f);
    }
  }

  /**
   *
   *
   * @param f
   */
  public void open(File f) {
    log.debug("Opening connection file " + f);

    // Make sure a connection is not already open
    if (isConnected()) {
      Option optNew = new Option("New", "New create a new window", 'n');
      Option optClose = new Option("Close", "Close current connection",
                                   'l');
      Option optCancel = new Option("Cancel",
                                    "Cancel the opening of this connection",
                                    'c');
      OptionsDialog dialog = OptionsDialog.createOptionDialog(this,
          new Option[] {optNew, optClose, optCancel}
          ,
          "You already have a connection open. Select\n"
          + "Close to close the current connection, New\n"
          + "to create a new terminal or Cancel to abort.",
          "Existing connection", optNew, null,
          UIManager.getIcon("OptionPane.warningIcon"));
      UIUtil.positionComponent(SwingConstants.CENTER, dialog);
      dialog.setVisible(true);

      Option opt = dialog.getSelectedOption();

      if ( (opt == null) || (opt == optCancel)) {
        return;
      }
      else if (opt == optNew) {
        try {
          SshToolsApplicationContainer c = (SshToolsApplicationContainer)
              application
              .newContainer();
          ( (SshToolsApplicationClientPanel) c.getApplicationPanel())
              .open(f);

          return;
        }
        catch (SshToolsApplicationException stae) {
          log.error(stae);
        }
      }
      else {
        closeConnection(true);
      }
    }

    // Save to MRU
    if (getApplication().getMRUModel() != null) {
      getApplication().getMRUModel().add(f);
    }

    // Make sure its not invalid
    if (f != null) {
      // Create a new connection properties object
      SshToolsConnectionProfile profile = new SshToolsConnectionProfile();

      try {
        // Open the file
        profile.open(f.getAbsolutePath());
        setNeedSave(false);
        currentConnectionFile = f;
        //setContainerTitle(f);

        // Connect with the new details.
        connect(profile, false);
      }
      catch (InvalidProfileFileException fnfe) {
        showExceptionMessage(fnfe.getMessage(), "Open Connection");
      }
      catch (SshException e) {
        e.printStackTrace();
        showExceptionMessage("An unexpected error occured!",
                             "Open Connection");
      }
    }
    else {
      showExceptionMessage("Invalid file specified", "Open Connection");
    }
  }

  /**
   *
   *
   * @param profile
   * @param newProfile
   */
  public void connect(final SshToolsConnectionProfile profile,
                      final boolean newProfile) {
    // We need to connect
    ssh = new SshClient();

    // Set the current connection properties
    setCurrentConnectionProfile(profile);
//    IgnoreHostKeyVerification();
//    setHostHostVerification(Powua.HOST);
//    setHostHostVerification(IgnoreHostKeyVerification);
    // We'll do the threading rather than j2ssh as we want to get errors
    Runnable r = new Runnable() {
      public void run() {
        // Update the status bar
        getStatusBar().setStatusText("Connecting");
        getStatusBar().setHost(getCurrentConnectionProfile()
                               .getHost(),
                               getCurrentConnectionProfile().getPort());
        getStatusBar().setUser(getCurrentConnectionProfile()
                               .getUsername());

        //
        try {
          log.info("Connecting to "
                   + getCurrentConnectionProfile().getHost() + " as "
                   + getCurrentConnectionProfile().getUsername());

	  SshToolsConnectionProfile cProfile = getCurrentConnectionProfile();
//	  cProfile.set
//	  ssh.se
	  cProfile.setWindow(getContainer().getWindow());
	  ssh.connect(cProfile,
                      (getHostKeyVerification() == null)
                      ? new SinkHostKeyVerification()
                      : getHostKeyVerification());


          // Set the remote id if we can find on
          try {
            RemoteIdentification rid = RemoteIdentificationFactory
                .getInstance(ssh.getServerId(),
                             ssh.getConnectionProperties().getHost());
            getStatusBar().setRemoteId(rid.getName(
                ssh.getServerId()));
          }
          catch (RemoteIdentificationException ex) {
            getStatusBar().setRemoteId("Unknown");
          }

          if (postConnection()) {
            if (!authenticateUser(newProfile)) {
              closeConnection(false);
            }
            else {
              setAvailableActions();
            }
          }
        }
        catch (IOException sshe) {
	  log.error("Thrown (IOException): "+sshe);
          ssh = null;
	  if(sshe.getMessage().indexOf("Canceled by user")<0) {
          showExceptionMessage("Connection Error",
              "Could not establish a connection to host: \n\n " +
              sshe.getMessage());
	  }
          SshToolsApplicationClientPanel.this.closeConnection(false);
        }
        catch (SecurityException se) {
	  log.error("Thrown (SecurityException): "+se);
          ssh = null;
          showErrorMessage(SshToolsApplicationClientPanel.this,
                           "Error", se);
          SshToolsApplicationClientPanel.this.closeConnection(false);
        } catch(Throwable t) {
	    log.error("Thrown (Thowable): "+t);
	    StringWriter sbw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sbw);
	    pw.println("Error: ");
	    t.printStackTrace(pw);
	    log.error(sbw.toString());
	} 
      }
    };

    Thread thread = new SshThread(r,
                                  application.getApplicationName() +
                                  " connection", true);
    thread.start();
  }

  /**
   *
   *
   * @param ssh
   * @param profile
   *
   * @throws IOException
   */
  public void connect(SshClient ssh, SshToolsConnectionProfile profile) throws
      IOException {
    this.ssh = ssh;

    // Set the current connection properties
    setCurrentConnectionProfile(profile);

    if (!ssh.isAuthenticated()) {
      authenticateUser(false);
    }

    authenticationComplete(false);
  }

  /**
   *
   *
   * @param newProfile
   *
   * @return
   *
   * @throws IOException
   */
    protected boolean authenticateUser(boolean newProfile) throws IOException {
	try {
	    // We should now authenticate
	    int result = AuthenticationProtocolState.READY;

	    // Our authenticated flag
	    boolean authenticated = false;

	    // Get the supported authentication methods
	    java.util.List auths = SshAuthenticationClientFactory
		.getSupportedMethods();

	    // Get the available methods
	    java.util.List supported = null;
	    supported = ssh.getAvailableAuthMethods(getCurrentConnectionProfile()
						    .getUsername());

	    if(supported==null) throw new IOException("There are no authentication methods which both the server and client understand.\n Cannot connect.");

	    // If the server supports external key exchange then we SHOULD use it 
	    if (supported.contains("external-keyx")) {

		EKEYXAuthenticationClient aa = new EKEYXAuthenticationClient();
		aa.setProperties(getCurrentConnectionProfile());
		aa.setUsername(getCurrentConnectionProfile().getUsername());
		result = ssh.authenticate(aa,getCurrentConnectionProfile().getHost());

		if (result == AuthenticationProtocolState.COMPLETE) {
		    authenticationComplete(newProfile);
		    return true;
		}
	    }
	    // If the server supports public key lets look for an agent and try
	    // some of his keys
	    if (supported.contains("publickey")) {
		if (System.getProperty("sshtools.agent") != null) {
		    try {
			SshAgentClient agent = SshAgentClient.connectLocalAgent("SSHTerm",
										System.getProperty("sshtools.agent") /*, 5*/);

			AgentAuthenticationClient aac = new AgentAuthenticationClient();
			aac.setAgent(agent);
			aac.setUsername(getCurrentConnectionProfile().getUsername());
			result = ssh.authenticate(aac,getCurrentConnectionProfile().getHost());

			agent.close();
		    }
		    catch (AgentNotAvailableException ex) {
			log.info("No agent was available for authentication");

			// Just continue
		    }

		    if (result == AuthenticationProtocolState.COMPLETE) {
			authenticationComplete(newProfile);

			return true;
		    }
		}
	    }

	    // Create a list for display that will contain only the
	    // supported and available methods
	    java.util.List display = new java.util.ArrayList();

	    // Did we receive a banner from the remote computer
	    final String banner = ssh.getAuthenticationBanner(BANNER_TIMEOUT);

	    if (banner != null) {
		if (!banner.trim().equals("")) {
		    try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
				    BannerDialog.showBannerDialog(SshToolsApplicationClientPanel.this,
								  banner);
				}
			    });
		    }
		    catch (Exception e) {
			log.error("Failed to invoke and wait on BannerDialog", e);
		    }
		}
	    }

	    // Are there any authentication methods within the properties file?
	    // Iterate through selecting only the supported and available
	    Iterator it = supported.iterator();

	    LinkedList allowed = new LinkedList();

	    while (it.hasNext() && !authenticated) {
		Object obj = it.next();

		if (auths.contains(obj)) {
		    display.add(obj);
		    allowed.add(obj);
		    //System.out.println(obj);
		}
	    }

	    // First look to see if we have any authenticaiton methods available
	    // in the profile properties object as this will overide a manual selection
	    java.util.Map authMethods = (Map) ( (HashMap) getCurrentConnectionProfile()
						.getAuthenticationMethods())
		.clone();
	    it = authMethods.entrySet().iterator();

	    //Iterator it2 = null;
	    java.util.List selected;

	    // Loop until the user either cancels or completes
	    boolean completed = false;
	    SshAuthenticationClient auth;
	    Map.Entry entry;
	    String msg = null;

	    while (!completed
		   &&
		   (ssh.getConnectionState().getValue() !=
		    TransportProtocolState.DISCONNECTED)) {
		auth = null;
		auth = SshAuthenticationClientFactory.newInstance("password", getCurrentConnectionProfile());
		auth.setUsername(getCurrentConnectionProfile().getUsername());
		// Select an authentication method from the properties file or
		// prompt the user to choose

/*
		if (it.hasNext()) {
		    Object obj = it.next();
		    System.out.println(obj.toString());
		    if (obj instanceof Map.Entry) {
			entry = (Map.Entry) obj;
			auth = (SshAuthenticationClient) entry.getValue();
		    }
		    else if (obj instanceof String) {
			auth = SshAuthenticationClientFactory.newInstance( (String) obj, getCurrentConnectionProfile());
			auth.setUsername(getCurrentConnectionProfile().getUsername());
		    }
		    else {
			closeConnection(true);
			throw new IOException(
					      "Iterator of Map or List of String expected");
		    }
		}
		else {
		    selected = AuthenticationDialog.showAuthenticationDialog(this,
									     display, ( (msg == null) ? "" : msg));
		    System.out.println(selected.toString());
		    if (selected.size() > 0) {
			it = selected.iterator();
		    }
		    else {
			closeConnection(true);

			return false;
		    }
		}
*/
		if(auth!=null && !allowed.contains(auth.getMethodName())) auth=null;
		if (auth != null) {
		    // The password authentication client can act upon requests to change the password

		    /* if (auth instanceof PasswordAuthenticationClient) {
		       PasswordAuthenticationDialog dialog = new PasswordAuthenticationDialog(SshTerminalPanel.this);
		       ((PasswordAuthenticationClient) auth).setAuthenticationPrompt(dialog);
		       ( (PasswordAuthenticache:http://cationClient) auth)
		       .setPasswordChangePrompt(PasswordChange.getInstance());
		       PasswordChange.getInstance().setParentComponent(
		       SshTerminalPanel.this);
		       }*/

		    // Show the implementations dialog
		    // if(auth.showAuthenticationDialog()) {
		    // Authentication with the details supplied
		    try {
		    	PasswordAuthenticationClient pac;
		    	if(auth instanceof PasswordAuthenticationClient) {
		    		pac = (PasswordAuthenticationClient) auth;
					pac.setHostname(getCurrentConnectionProfile().getHost());
					pac.setUsername(auth.getUsername());
					pac.setPassword(password);
					result = ssh.authenticate(pac, getCurrentConnectionProfile().getHost());
				}
//			result = showAuthenticationPrompt(auth); //ssh.authenticate(auth);

		    } catch(IllegalArgumentException e) {// Do not use this authentication method!
			allowed.remove(auth);
		    }

		    if (result == AuthenticationProtocolState.FAILED) {
			msg = auth.getMethodName()
			    + " authentication failed, try again?";
		    }

		    // If the result returned partial success then continue
		    if (result == AuthenticationProtocolState.PARTIAL) {
			// We succeeded so add to the connections authenticaiton
			// list and continue on to the next one
			getCurrentConnectionProfile().addAuthenticationMethod(auth);
			msg = auth.getMethodName()
			    + " authentication succeeded but another is required";
		    }

		    if (result == AuthenticationProtocolState.COMPLETE) {
			authenticated = true;
//			ssh.getActiveSftpClient();
//			Browser br = new Browser();
//			SftpClient ftp = new SftpClient(ssh);
			//If successfull add to the connections list so we can save later
//			SftpClient ftp = ssh.openSftpClient();
//			ssh.
			//			ScpClient sclient = ssh.openScpClient(new File("/home/" + getCurrentConnectionProfile().getUsername()));
//			sclient.
			//			ftp.get
//			MainFrame m = new MainFrame();
//			List l = ftp.ls();
//			ftp.
//			ftp.
//			JFtp jftp = new JFtp(ssh);
//			ftp.quit();

//			JavaFileManager fm = ftp.
//			getCurrentConnectionProfile().addAuthenticationMethod(auth);

			// Set the completed flag
			completed = true;
			authenticationComplete(newProfile);
//		    getContainer().setSize(800, 800);
		    }

		    if (result == AuthenticationProtocolState.CANCELLED) {
			ssh.disconnect();

			return false;
		    }

		    //   }
		    //  else {
		    // User has cancelled the authenticaiton
		    //       closeConnection(true);
		    //       return false;
		    //  }
		}

		// end of if
	    }

	    // end of while
	    return authenticated;
	} catch(EOFException e) {
	    throw new IOException("The remote host has closed the connection.\n\nAs you are authenticating this probably means the server has given up authenticating you.");
	} catch(MessageStoreEOFException e) {
	    throw new IOException("The remote host has closed the connection.\n\nAs you are authenticating this probably means the server has given up authenticating you.");
	}
    }

  /**
   *
   *
   * @param instance
   *
   * @return
   *
   * @throws IOException
   */
  protected int showAuthenticationPrompt(SshAuthenticationClient instance) throws
      IOException, IllegalArgumentException {
    instance.setUsername(getCurrentConnectionProfile().getUsername());

    if (instance instanceof PasswordAuthenticationClient) {
      PasswordAuthenticationDialog dialog = new PasswordAuthenticationDialog( (
          Frame) SwingUtilities
          .getAncestorOfClass(Frame.class,
                              SshToolsApplicationClientPanel.this));
      instance.setAuthenticationPrompt(dialog);
      ( (PasswordAuthenticationClient) instance).setPasswordChangePrompt(
          PasswordChange
          .getInstance());
      PasswordChange.getInstance().setParentComponent(
          SshToolsApplicationClientPanel.this);
    }
    else if (instance instanceof PublicKeyAuthenticationClient) {
      PublicKeyAuthenticationPrompt prompt = new PublicKeyAuthenticationPrompt(
          SshToolsApplicationClientPanel.this);
      instance.setAuthenticationPrompt(prompt);
    }
    else if (instance instanceof KBIAuthenticationClient) {
      KBIAuthenticationClient kbi = new KBIAuthenticationClient();
      ( (KBIAuthenticationClient) instance).setKBIRequestHandler(new
          KBIRequestHandlerDialog(
          (Frame) SwingUtilities.getAncestorOfClass(Frame.class,
          SshToolsApplicationClientPanel.this)));
    }
    return ssh.authenticate(instance, getCurrentConnectionProfile().getHost());
  }

  /**
   *
   *
   * @return
   */
  public abstract boolean postConnection();

  /**
   *
   *
   * @param newProfile
   *
   * @throws SshException
   * @throws IOException
   */
  public abstract void authenticationComplete(boolean newProfile) throws
      SshException, IOException;

  /**
   *
   *
   * @param file
   */
  public void setContainerTitle(String host) {
    String verString = ConfigurationLoader.getVersionString(application
        .getApplicationName(), application.getApplicationVersion());

    if (container != null) {
      container.setContainerTitle( (host == null) ? verString
                                  : (verString + " ["
                                     + host + "]"));
    }
  }

  /**
   *
   *
   * @param needSave
   */
  public void setNeedSave(boolean needSave) {
    if (needSave != this.needSave) {
      this.needSave = needSave;
      setAvailableActions();
    }
  }

  /**
   *
   *
   * @param file
   */
  public void setCurrentConnectionFile(File file) {
    currentConnectionFile = file;
  }

  /**
   *
   *
   * @return
   */
  public File getCurrentConnectionFile() {
    return currentConnectionFile;
  }

  /**
   *
   *
   * @param profile
   */
  public void setCurrentConnectionProfile(SshToolsConnectionProfile profile) {
    currentConnectionProfile = profile;
  }

  /**
   *
   *
   * @return
   */
  public SshToolsConnectionProfile getCurrentConnectionProfile() {
    return currentConnectionProfile;
  }

  /**
   *
   *
   * @return
   */
  public boolean isNeedSave() {
    return needSave;
  }

  /**
   *
   *
   * @return
   */
  public boolean isConnected() {
    return (ssh != null) && ssh.isConnected();
  }

  /**
   *
   *
   * @throws SshException
   */
  public void connect() throws SshException {
    if (getCurrentConnectionProfile() == null) {
      throw new SshException(
          "Can't connect, no connection profile have been set.");
    }

    //  There isn't anywhere to store this setting yet
    connect(getCurrentConnectionProfile(), false);
  }

  /**
   *
   *
   * @param disconnect
   */
  public void closeConnection(boolean disconnect) {
    //
      if (isNeedSave() && (currentConnectionFile != null)) {  // Stop save dialog box when not using a pre-existing profile.
      //  Only allow saving of files if allowed by the security manager
      try {
        if (System.getSecurityManager() != null) {
          AccessController.checkPermission(new FilePermission(
              "<<ALL FILES>>", "write"));

          if (JOptionPane.showConfirmDialog(this,
              "You have unsaved changes to the connection "
              + ( (currentConnectionFile == null)
                 ? "<Untitled>" : currentConnectionFile.getName())
              + ".\nDo you want to save the changes now?",
              "Unsaved changes", JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            saveConnection(false, getCurrentConnectionFile(),
                           getCurrentConnectionProfile());
            setNeedSave(false);
          }
        }
      }
      catch (AccessControlException ace) {
        log.warn(
            "Changes made to connection, but security manager won't allow saving of files.");
      }
    }

    //setCurrentConnectionFile(null);
  }

  /**
   *
   *
   * @return
   */
  protected boolean allowConnectionSettingsEditing() {
    return true;
  }

  /**
   *
   *
   * @param profile
   *
   * @return
   */
  public boolean editConnection(SshToolsConnectionProfile profile) {
    final SshToolsConnectionPanel panel = new SshToolsConnectionPanel(
        allowConnectionSettingsEditing());
    SshToolsConnectionTab[] tabs = getAdditionalConnectionTabs();

    for (int i = 0; (tabs != null) && (i < tabs.length); i++) {
      tabs[i].setConnectionProfile(profile);
      panel.addTab(tabs[i]);
    }

    panel.setConnectionProfile(profile);

    final Option ok = new Option("Ok",
                                 "Apply the settings and close this dialog",
                                 'o');
    final Option cancel = new Option("Cancel",
        "Close this dialog without applying the settings", 'c');
    OptionCallback callback = new OptionCallback() {
      public boolean canClose(OptionsDialog dialog, Option option) {
        if (option == ok) {
          return panel.validateTabs();
        }

        return true;
      }
    };

    OptionsDialog od = OptionsDialog.createOptionDialog(
        SshToolsApplicationClientPanel.this,
        new Option[] {ok, cancel}
        , panel, "Connection Settings", ok,
        callback, null);
    od.pack();
    UIUtil.positionComponent(SwingConstants.CENTER, od);
    od.setVisible(true);

    if (od.getSelectedOption() == ok) {
      panel.applyTabs();

      if (profile == getCurrentConnectionProfile()) {
        setNeedSave(true);
      }

      return true;
    }

    return false;
  }

  /**
   *
   *
   * @param saveAs
   * @param file
   * @param profile
   *
   * @return
   */
  public File saveConnection(boolean saveAs, File file,
                             SshToolsConnectionProfile profile) {
    if (profile != null) {
      if ( (file == null) || saveAs) {
        String prefsDir = super.getApplication().
            getApplicationPreferencesDirectory().
            getAbsolutePath();
        JFileChooser fileDialog = new JFileChooser(prefsDir);

        fileDialog.setFileFilter(connectionFileFilter);

        int ret = fileDialog.showSaveDialog(this);

        if (ret == fileDialog.CANCEL_OPTION) {
          return null;
        }

        file = fileDialog.getSelectedFile();

        if (!file.getName().toLowerCase().endsWith(".xml")) {
          file = new File(file.getAbsolutePath() + ".xml");
        }
      }

      try {
        if (saveAs && file.exists()) {
          if (JOptionPane.showConfirmDialog(this,
              "File already exists. Are you sure?",
              "File exists", JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
            return null;
          }
        }

        // Check to make sure its valid
        if (file != null) {
          // Save the connection details to file
          log.debug("Saving connection to " + file.getAbsolutePath());
          profile.save(file.getAbsolutePath());

          if (profile == getCurrentConnectionProfile()) {
            log.debug(
                "Current connection saved, disabling save action.");
            setNeedSave(false);
          }

          return file;
        }
        else {
          showExceptionMessage("The file specified is invalid!",
                               "Save Connection");
        }
      }
      catch (InvalidProfileFileException e) {
        showExceptionMessage(e.getMessage(), "Save Connection");
      }
    }

    return null;
  }

  public static class ActionMenu
      implements Comparable {
    int weight;
    int mnemonic;
    String name;
    String displayName;

    public ActionMenu(String name, String displayName, int mnemonic,
                      int weight) {
      this.name = name;
      this.displayName = displayName;
      this.mnemonic = mnemonic;
      this.weight = weight;
    }

    public int compareTo(Object o) {
      int i = new Integer(weight).compareTo(new Integer(
          ( (ActionMenu) o).weight));

      return (i == 0)
          ? displayName.compareTo( ( (ActionMenu) o).displayName) : i;
    }
  }

  class ToolBarActionComparator
      implements Comparator {
    public int compare(Object o1, Object o2) {
      int i = ( (Integer) ( (StandardAction) o1).getValue(StandardAction.
          TOOLBAR_GROUP))
          .compareTo( (Integer) ( (StandardAction) o2).getValue(
          StandardAction.TOOLBAR_GROUP));

      return (i == 0)
          ?
          ( (Integer) ( (StandardAction) o1).getValue(StandardAction.
          TOOLBAR_WEIGHT))
          .compareTo( (Integer) ( (StandardAction) o2).getValue(
          StandardAction.TOOLBAR_WEIGHT)) : i;
    }
  }

  class MenuItemActionComparator
      implements Comparator {
    public int compare(Object o1, Object o2) {
      int i = ( (Integer) ( (StandardAction) o1).getValue(StandardAction.
          MENU_ITEM_GROUP))
          .compareTo( (Integer) ( (StandardAction) o2).getValue(
          StandardAction.MENU_ITEM_GROUP));

      return (i == 0)
          ?
          ( (Integer) ( (StandardAction) o1).getValue(StandardAction.
          MENU_ITEM_WEIGHT))
          .compareTo( (Integer) ( (StandardAction) o2).getValue(
          StandardAction.MENU_ITEM_WEIGHT)) : i;
    }
  }

  class ConnectionFileFilter
      extends javax.swing.filechooser.FileFilter {
    public boolean accept(File f) {
      return f.isDirectory()
          || f.getName().toLowerCase().endsWith(".xml");
    }

    public String getDescription() {
      return "Connection files (*.xml)";
    }
  }

  class SinkHostKeyVerification
      implements HostKeyVerification {
    public boolean verifyHost(String host, SshPublicKey pk) throws
        TransportProtocolException {
      log.warn("Accepting host " + host
               + " as host key verification is disabled.");

      return true;
    }
  }
}
