/*
 *  GSI-SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2005-7 STFC/CCLRC.
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

package com.sshtools.j2ssh.authentication;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.globus.common.CoGProperties;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.GlobusCredentialException;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.X509ExtensionSet;
import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.myproxy.MyProxy;
import org.globus.myproxy.SASLParams;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import uk.ac.rl.esc.browser.Browser;

import com.sshtools.common.configuration.SshToolsConnectionProfile;
import com.sshtools.common.ui.PreferencesStore;
import com.sshtools.common.ui.SshToolsApplicationPanel;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.sshterm.ProxyHelper;
import com.sshtools.sshterm.SshTerminalPanel;

public class UserGridCredential {

    // ************************************************************************************
    //
    // These defaults should not be changed here but in the res/common/default.properties file
    // please see docs/README and src/com/sshtools/common/ui/PreferencesStore.java for details.
    //
    // These are here because we must provide some default.
    //
    public static final String DEFAULT_MYPROXY_SERVER_K = "myproxy-sso.grid-support.ac.uk";
    public static final String DEFAULT_MYPROXY_SERVER = "myproxy.grid-support.ac.uk";
    public static final String DEFAULT_MYPROXY_PORT_K = "7513";
    public static final String DEFAULT_MYPROXY_PORT = "7512";
    private static boolean SAVE_MYPROXY_PROXY=false;
    private static boolean SAVE_GRID_PROXY_INIT_PROXY=false;
    private static boolean SAVE_PKCS12_PROXY=false;
    private static boolean SAVE_BROWSER_PROXY=false;
    //**************************************************************************************

    private static String paramGSSCredential = "";

    private static UserGridCredential thisDummy = new UserGridCredential();

    private static void copyFile(InputStream src, OutputStream dst) throws IOException {
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = src.read(buf)) > 0) {
            dst.write(buf, 0, len);
        }
        src.close();
        dst.close();
    }

    // find the certificates zip file and copy contents to $HOME/.globus/certificate where they do not exist
    private static void checkCACertificates(CoGProperties cogproperties) throws IOException {
        // check the directories exist and create if they don't
        String globusDir = System.getProperty("user.home")+"/.globus";
        if (!(new File(globusDir).exists())) {
            boolean success = (new File(globusDir).mkdir());
            if (!success) {
                   throw new IOException("Couldn't create directory: "+globusDir);
            }
        }
        String caCertLocations = globusDir + "/certificates";
        File caCertLocationsF = new File(caCertLocations);
        if (!caCertLocationsF.exists()) {
            boolean success = (new File(caCertLocations).mkdir());
            if (!success) {
                throw new IOException("Couldn't create directory: "+caCertLocations);
            }
        }
        if(!caCertLocationsF.isDirectory()) {
            throw new IOException("Location: "+caCertLocations+" is not a directory");
        }
        File tmp=null;
        try {
            // save the zipfile temporarily
            tmp = File.createTempFile("certificates", ".zip");
            copyFile(thisDummy.getClass().getResourceAsStream("certificates.zip"), new FileOutputStream(tmp));
            ZipFile zf = new ZipFile(tmp);
	    try {
		Enumeration e = zf.entries();
		while(e.hasMoreElements()) {
		    ZipEntry ze = (ZipEntry) e.nextElement();
		    String name = ze.getName();
		    if (!(new File(caCertLocations+File.separator+name).exists())) {
			copyFile(zf.getInputStream(ze), new FileOutputStream(new File(caCertLocations+File.separator+name)));
		    }
		}
	    } finally {
		if(zf!=null) zf.close();
	    }
        } catch(IOException e) {
            throw new IOException("Couldn't load certificates... "+e);
        } finally {
            // delete temp file
            if(tmp!=null) {
                if(tmp.exists()) tmp.delete();
            }
        }
    }


    public static GSSCredential getUserCredential(SshConnectionProperties properties) throws GSSException, IOException {

	CertUtil.init();

	// Get the proxy saving settings:
	// Now we only have 1 setting
	//SAVE_MYPROXY_PROXY = PreferencesStore.getBoolean(SshTerminalPanel.PREF_SAVE_MYPROXY_PROXY, SAVE_MYPROXY_PROXY);
	//SAVE_GRID_PROXY_INIT_PROXY = PreferencesStore.getBoolean(SshTerminalPanel.PREF_SAVE__GRID_PROXY_INIT_PROXY, SAVE_GRID_PROXY_INIT_PROXY);
	//SAVE_PKCS12_PROXY = PreferencesStore.getBoolean(SshTerminalPanel.PREF_SAVE__PKCS12_PROXY, SAVE_PKCS12_PROXY);
	//SAVE_BROWSER_PROXY = PreferencesStore.getBoolean(SshTerminalPanel.PREF_SAVE__BROWSER_PROXY, SAVE_BROWSER_PROXY);
	SAVE_MYPROXY_PROXY =((SshToolsConnectionProfile)properties).getApplicationPropertyBoolean(SshTerminalPanel.PREF_SAVE_PROXY, SAVE_MYPROXY_PROXY);
	SAVE_GRID_PROXY_INIT_PROXY = ((SshToolsConnectionProfile)properties).getApplicationPropertyBoolean(SshTerminalPanel.PREF_SAVE_PROXY, SAVE_GRID_PROXY_INIT_PROXY);
	SAVE_PKCS12_PROXY = ((SshToolsConnectionProfile)properties).getApplicationPropertyBoolean(SshTerminalPanel.PREF_SAVE_PROXY, SAVE_PKCS12_PROXY);
	SAVE_BROWSER_PROXY = ((SshToolsConnectionProfile)properties).getApplicationPropertyBoolean(SshTerminalPanel.PREF_SAVE_PROXY, SAVE_BROWSER_PROXY);
	myProxyPrompt = MyProxyPrompt.getInstance();
	gridProxyInitPrompt = GridProxyInitPrompt.getInstance();
	gridProxyInitPrompt.setTitle("Enter your grid certificate passphrase");

	CoGProperties cogproperties = CoGProperties.getDefault();
	checkCACertificates(cogproperties);

	int proxyType = GSIConstants.GSI_3_IMPERSONATION_PROXY;
	try {
	    String cur = ((SshToolsConnectionProfile)properties).getApplicationProperty(SshTerminalPanel.PREF_PROXY_TYPE, Integer.toString(GSIConstants.GSI_3_IMPERSONATION_PROXY));
	    proxyType = Integer.parseInt(cur);
	} catch(Exception e) {
	    throw new Error("Programming Error", e);
	}
	int lifetimeHours = 12;
	try {
	    String cur = ((SshToolsConnectionProfile)properties).getApplicationProperty(SshTerminalPanel.PREF_PROXY_LENGTH, "12");
	    lifetimeHours = Integer.parseInt(cur);
	} catch(Exception e) {
	    throw new Error("Programming Error", e);
	}
	log.debug("Loading grid proxy.");
	GSSCredential gsscredential = null;
	do
            {
                if(gsscredential != null)
                    break;
                gsscredential = loadProxyFromParam();
                if (gsscredential == null)
                    gsscredential = loadExistingProxy();
		if(gsscredential == null)
                    gsscredential = saslProxy(properties, lifetimeHours);
                if(gsscredential == null)
                    gsscredential = createProxy(proxyType, lifetimeHours, properties);
		myProxyPrompt.setError("");
                while (gsscredential == null)
                    gsscredential = retrieveRemoteProxy(properties,proxyType, lifetimeHours);
            } while(true);
	return gsscredential;
    }


    private static void rmPass(StringBuffer password) {
	for(int i=0;i<password.length();i++) {
	    password.replace(i,i+1,"*");
	}
	password = new StringBuffer();;
    }
    
    private static class PasswordPrompt implements Browser.PasswordCallback {
	public char [] prompt(String promptString) {
	    StringBuffer passwordstringbuffer = new StringBuffer();
	    
	    boolean flag = MozillaPrompt.getInstance().getGridPassword(properties.getWindow(), passwordstringbuffer, "Mozilla/Firefox");
	    if(flag) return null;
	    char pass[] = passwordstringbuffer.toString().toCharArray();
	    rmPass(passwordstringbuffer);
	    return pass;
	}

	public PasswordPrompt(SshConnectionProperties properties) {
	    this.properties = properties;
	}
	
	final SshConnectionProperties properties;
    }

    private static GSSCredential chooseCert(int proxyType, int lifetimeHours, SshConnectionProperties props) throws IOException, IllegalArgumentException, IllegalStateException {
	String profile = Browser.getCurrentBrowser();
	if(profile==null) {
	    String profiles[] = Browser.getBrowserList();
	    if(profiles==null) return null;
	    if(profiles.length==0) {
		JOptionPane.showMessageDialog(props.getWindow(), "No browsers found", "GSI-SSHTerm Authentication", JOptionPane.ERROR_MESSAGE);
		return null;
	    }
	    if(profiles.length==1) {
		Browser.setBrowser(profiles[0]); //user chooses profile.
	    } else {
		JComboBox combo = new JComboBox(profiles);
		int ret = JOptionPane.showOptionDialog(props.getWindow(), "Please choose browser to use:", "Grid Authentication", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {combo, "OK"},null);
		if(ret==JOptionPane.CLOSED_OPTION) new IOException("Canceled by user.");
		Browser.setBrowser(profiles[combo.getSelectedIndex()]); //user chooses profile.
	    }
	    profile = Browser.getCurrentBrowser();
	}
	String dnlist[]=null;
	try {
	    dnlist = Browser.getDNlist(new PasswordPrompt(props));
	} catch(IOException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
	    log.debug("Could not access keystore in profile: "+profile+" : "+e);
	} catch(KeyStoreException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
	    log.debug("Could not access keystore in profile: "+profile+" : "+e);
	} catch(NoSuchAlgorithmException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
	    log.debug("Could not access keystore in profile: "+profile+" : "+e);
	} catch(CertificateException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
	    log.debug("Could not access keystore in profile: "+profile+" : "+e);
	} catch(InvalidAlgorithmParameterException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
	    log.debug("Could not access keystore in profile: "+profile+" : "+e);
	} catch(javax.security.auth.login.FailedLoginException e) { 
	    JOptionPane.showMessageDialog(props.getWindow(), e.getMessage(),"Incorrect Password", JOptionPane.ERROR_MESSAGE);
	    return null;
	} catch(GeneralSecurityException e) {
	    if(e.getMessage().indexOf("version>=1.5")>=0) {
		JOptionPane.showMessageDialog(props.getWindow(), e.getMessage(), "GSI-SSHTerm Authentication", JOptionPane.ERROR_MESSAGE);
	    } else {
		e.printStackTrace();
		errorReport(props.getWindow(), "Could not access keystore in profile: "+profile, e);
		log.debug("Could not access keystore in profile: "+profile+" : "+e);
	    }
	} 
	if(dnlist==null) return null;
	int index = -1;
	if(dnlist.length==0) {
	    JOptionPane.showMessageDialog(props.getWindow(), "No Certificates found", "GSI-SSHTerm Authentication", JOptionPane.ERROR_MESSAGE);
	    return null;
	}
	if(dnlist.length==1) {
	    index = 0;
	} else {
	    JComboBox dnCombo = new JComboBox(dnlist);

	    int ret = JOptionPane.showOptionDialog(props.getWindow(), "Please choose certificate to use:", "GSI-SSHTerm Authentication", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] {dnCombo, "OK"},null);
	    if(ret==JOptionPane.CLOSED_OPTION) new IOException("Canceled by user.");
	    index = dnCombo.getSelectedIndex();
	}
	try {	    
	    GSSCredential gssproxy =  Browser.getGridProxy(dnlist[index], proxyType, lifetimeHours);

	    if(SAVE_BROWSER_PROXY) {
		GlobusCredential proxy = ((GlobusGSSCredentialImpl)gssproxy).getGlobusCredential();
		ProxyHelper.saveProxy(proxy, props);
	    }
	    return gssproxy;
	} catch(GeneralSecurityException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not load certificate from profile: "+profile, e);
	    log.debug("Could not load certificate from browser: "+e);
	} catch(GlobusCredentialException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not load certificate from profile: "+profile, e);
	    log.debug("Could not load certificate from browser: "+e);
	} catch(GSSException e) {
	    e.printStackTrace();
	    errorReport(props.getWindow(), "Could not load certificate from profile: "+profile, e);
	    log.debug("Could not load certificate from browser: "+e);
	} 
	return null;
    } 
	
    private static void errorReport(final java.awt.Component comp, final String message, final Exception ex) {
	try {
              SwingUtilities.invokeAndWait(new Runnable() {
		      public void run() {
			  SshToolsApplicationPanel.showErrorMessage(comp,
								    message,
								    "GSI-SSHTerm Authentication",
								    ex);
		      }
		  });
	} catch (Exception ex1) {
	    log.info("Failed to invoke message box through SwingUtilities",  ex1);
	}
    }

    private static GSSCredential createProxy(int proxyType, int lifetimeHours, SshConnectionProperties props)
        throws IOException
    {
        GSSCredential gsscredential = null;
        CoGProperties cogproperties = CoGProperties.getDefault();
        if((new File(cogproperties.getUserKeyFile())).exists() && (new File(cogproperties.getUserCertFile())).exists())
        {
	    GlobusCredential globuscredential = null;
	    while(globuscredential == null) {
		StringBuffer stringbuffer = new StringBuffer();
		boolean flag = gridProxyInitPrompt.getGridPassword(props.getWindow(), stringbuffer);
		if(flag)
		    throw new IOException("Canceled by user.");
		if(gridProxyInitPrompt.getUseAnother()) {
		    return null;
		}
		try {
		    globuscredential = createProxy(stringbuffer.toString(), proxyType, lifetimeHours);
		}
		catch(Exception exception) {
		    //JOptionPane.showMessageDialog(props.getWindow(), exception.getMessage(),"Incorrect Password", JOptionPane.ERROR_MESSAGE);
		    errorReport(props.getWindow(), "Could not load certificate your certificate", exception);
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    gridProxyInitPrompt.setTitle(exception.getMessage());
		}
	    }
            if(globuscredential != null)
            {
		if(SAVE_GRID_PROXY_INIT_PROXY) {
		    ProxyHelper.saveProxy(globuscredential, props);
		} 
		try {
		    globuscredential.verify();
		    gsscredential = new GlobusGSSCredentialImpl(globuscredential, 1);
		} catch(Exception exception1) {
		    exception1.printStackTrace();
		    StringWriter stringwriter1 = new StringWriter();
		    exception1.printStackTrace(new PrintWriter(stringwriter1));
		    log.debug(stringwriter1);
		    if(exception1.getMessage().indexOf("Expired credentials")>=0) {
			JOptionPane.showMessageDialog(props.getWindow(), "Your certificate has expired, please renew your certificate or try another method for authentication.", "GSI-SSHTerm Authentication", JOptionPane.ERROR_MESSAGE);
			return null;
		    } else {
			errorReport(props.getWindow(), "Could not load certificate your certificate", exception1);
			return null;
		    }
		}
            }
        }
        return gsscredential;
    }

    private static GSSCredential saslProxy(SshConnectionProperties properties, int lengthHours) throws IOException {
	return saslProxy(properties, null, lengthHours);
    }

    private static GSSCredential saslProxy(SshConnectionProperties properties, String password, int lengthHours) throws IOException {
	String hostname_k=DEFAULT_MYPROXY_SERVER_K;
	hostname_k = PreferencesStore.get(SshTerminalPanel.PREF_KRB5_MYPROXY_HOSTNAME, hostname_k);
	String username=System.getProperty("user.name");
	String realm = System.getenv("USERDNSDOMAIN");
	String kdc = System.getenv("USERDNSDOMAIN");
	String port_S = DEFAULT_MYPROXY_PORT_K;
	boolean use = true;
	if(properties!=null) {
	    if(!(properties instanceof SshToolsConnectionProfile)) return null;
	    SshToolsConnectionProfile profile = (SshToolsConnectionProfile)properties;
	    hostname_k = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_HOSTNAME, hostname_k);
	    username = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_USERNAME, username);
	    realm = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_REALM, realm);
	    kdc = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_KDC, kdc);
	    use = profile.getApplicationPropertyBoolean(SshTerminalPanel.PREF_KRB5_MYPROXY_USE, use);
	}
	use = use && SshTerminalPanel.PREF_KRB5_MYPROXY_ENABLED; // was support compiled in?
	if(!use) return null;
	
	port_S = PreferencesStore.get(SshTerminalPanel.PREF_KRB5_MYPROXY_PORT, port_S);
	int port=7513;
	try {
	    port = Integer.parseInt(port_S);
	} catch(NumberFormatException e) {
	    log.warn("Could not parse the port number from defaults file (property name" + SshTerminalPanel.PREF_KRB5_MYPROXY_PORT+", property value= "+port_S+").");
	}
	GSSCredential gsscredential = null;
	CoGProperties cogproperties = CoGProperties.getDefault();
	CertUtil.init();
	MyProxy myproxy = new MyProxy(hostname_k, port);
	try {
	    SASLParams params = null;
	    if(password!=null) { 
		params = new SASLParams(username,password);
	    } else {
		params = new SASLParams(username);
	    }
	    params.setRealm(realm);
	    params.setLifetime(lengthHours*3600);
	    params.setKDC(kdc);
	    gsscredential = myproxy.getSASL(null, params);
	    if(SAVE_MYPROXY_PROXY) {
		GlobusCredential proxy = ((GlobusGSSCredentialImpl)gsscredential).getGlobusCredential();
		ProxyHelper.saveProxy(proxy, properties);
	    }
	    log.debug("A proxy has been received for user " + username);
	} catch(IllegalArgumentException exception) {
	    exception.printStackTrace();
	    StringWriter stringwriter = new StringWriter();
	    exception.printStackTrace(new PrintWriter(stringwriter));
	    log.debug(stringwriter);
	    myProxyPrompt.setError("MyProxy: "+exception.getMessage());;
	} catch(Exception exception) {
	    exception.printStackTrace();
	    StringWriter stringwriter = new StringWriter();
	    exception.printStackTrace(new PrintWriter(stringwriter));
	    log.debug(stringwriter);
	}
        return gsscredential;
    }

    private static GSSCredential retrieveRemoteProxy(SshConnectionProperties properties, int proxyType, int lifetimeHours)
        throws IOException
    {
        GSSCredential gsscredential = null;
        CoGProperties cogproperties = CoGProperties.getDefault();
        
	String hostname=DEFAULT_MYPROXY_SERVER_K;
	hostname = PreferencesStore.get(SshTerminalPanel.PREF_KRB5_MYPROXY_HOSTNAME, hostname);
	String username=System.getProperty("user.name");
	String realm = System.getenv("USERDNSDOMAIN");
	String kdc = System.getenv("USERDNSDOMAIN");
	boolean use = false;
	if(properties!=null && (properties instanceof SshToolsConnectionProfile) && ((SshToolsConnectionProfile)properties).getApplicationPropertyBoolean(SshTerminalPanel.PREF_KRB5_MYPROXY_USE, use)) {
	    SshToolsConnectionProfile profile = (SshToolsConnectionProfile)properties;
	    hostname = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_HOSTNAME, hostname);
	    username = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_USERNAME, username);
	    realm = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_REALM, realm);
	    kdc = profile.getApplicationProperty(SshTerminalPanel.PREF_KRB5_MYPROXY_KDC, kdc);
	    use = profile.getApplicationPropertyBoolean(SshTerminalPanel.PREF_KRB5_MYPROXY_USE, use);
	}


        {
            boolean flag = false;
            StringBuffer stringbuffer = new StringBuffer();
            StringBuffer stringbuffer1 = new StringBuffer();
            StringBuffer stringbuffer2 = new StringBuffer();
            if(myProxyPrompt != null)
            {
                if(use) {
		    myProxyPrompt.setHost(hostname);
		    myProxyPrompt.setAccountName(username);
		} else {
		    hostname = DEFAULT_MYPROXY_SERVER;
		    hostname = PreferencesStore.get(SshTerminalPanel.PREF_DEFAULT_MYPROXY_HOSTNAME, hostname);
		    username = PreferencesStore.get(SshTerminalPanel.PREF_LAST_MYPROXY_USERNAME, username);
		    myProxyPrompt.setHost(hostname);
		    myProxyPrompt.setAccountName(username);
		}
                boolean flag1 = myProxyPrompt.doGet(properties.getWindow(), stringbuffer, stringbuffer1, stringbuffer2, use);
		myProxyPrompt.setError("");
                if(flag1)
                    throw new IOException("Canceled by user.");
		
		StringBuffer stringbufferF = new StringBuffer();
		StringBuffer stringbufferP = new StringBuffer();
		if(myProxyPrompt.getBrowser()) {
		    return chooseCert(proxyType, lifetimeHours, properties);
		}
		if(myProxyPrompt.keyBased(stringbufferF, stringbufferP)) {
		    try {
			KeyStore store = null;
			String  passphrase = stringbufferP.toString();
			File keyfile = new File(stringbufferF.toString());
			Security.addProvider(new BouncyCastleProvider());
			store = KeyStore.getInstance("PKCS12", "BC");
			FileInputStream in = new FileInputStream(keyfile);
			store.load(in, passphrase.toCharArray());

			Enumeration e = store.aliases();
			if(!e.hasMoreElements()) return null;
			String alias = (String)e.nextElement();
			java.security.cert.Certificate cert = store.getCertificate(alias);
			Key key = store.getKey(alias,passphrase.toCharArray());
	  
			if(!(cert instanceof X509Certificate)) return null;
			if(!(key instanceof PrivateKey)) return null;	
			

	
			BouncyCastleCertProcessingFactory factory =
			    BouncyCastleCertProcessingFactory.getDefault();


			GlobusCredential globuscredential = factory.createCredential(new X509Certificate[] {(X509Certificate)cert},
					(PrivateKey)key,
					cogproperties.getProxyStrength(), 
					lifetimeHours * 3600,
					proxyType,
					(X509ExtensionSet)null);
    
    
			if(globuscredential != null)
			    {
				if(SAVE_PKCS12_PROXY) {
				    ProxyHelper.saveProxy(globuscredential, properties);
				} 
				try {
				    globuscredential.verify();
				    gsscredential = new GlobusGSSCredentialImpl(globuscredential, 1);
				} catch(Exception exception1) {
				    exception1.printStackTrace();
				    StringWriter stringwriter1 = new StringWriter();
				    exception1.printStackTrace(new PrintWriter(stringwriter1));
				    log.debug(stringwriter1);
				    if(exception1.getMessage().indexOf("Expired credentials")>=0) {
					JOptionPane.showMessageDialog(properties.getWindow(), "Your certificate has expired, please renew your certificate or try another method for authentication.", "GSI-SSHTerm Authentication", JOptionPane.ERROR_MESSAGE);
					return null;
				    } else {
					errorReport(properties.getWindow(), "Could not load certificate your certificate", exception1);
					return null;
				    }
				}
				
			    }
			return gsscredential;
		    } catch(java.io.FileNotFoundException exception) {
			exception.printStackTrace();
			    StringWriter stringwriter = new StringWriter();
			    exception.printStackTrace(new PrintWriter(stringwriter));
			    log.debug(stringwriter);
			    myProxyPrompt.setError("Certificate: could not find file");
			    return null;		    
		    } catch(Exception exception) {
			if(exception.getMessage().indexOf("wrong password")>=0) {
			    exception.printStackTrace();
			    StringWriter stringwriter = new StringWriter();
			    exception.printStackTrace(new PrintWriter(stringwriter));
			    log.debug(stringwriter);
			    myProxyPrompt.setError("Certificate: wrong password?");
			    return null;
			} else {
			    exception.printStackTrace();
			    StringWriter stringwriter = new StringWriter();
			    exception.printStackTrace(new PrintWriter(stringwriter));
			    log.debug(stringwriter);
			    errorReport(properties.getWindow(), "Unknown problem while loading certificate your certificate", exception);
			    return null;
			}
		    }
		}
	    }
            CertUtil.init();
	    // save username if changed:
	    if(!stringbuffer1.toString().equals(username)) {
		PreferencesStore.put(SshTerminalPanel.PREF_LAST_MYPROXY_USERNAME, stringbuffer1.toString());
	    }
	    String port_S = DEFAULT_MYPROXY_PORT;	
	    port_S = PreferencesStore.get(SshTerminalPanel.PREF_MYPROXY_PORT, port_S);
	    int port=7512;
	    try {
		port = Integer.parseInt(port_S);
	    } catch(NumberFormatException e) {
		log.warn("Could not parse the port number from defaults file (property name" + SshTerminalPanel.PREF_MYPROXY_PORT+", property value= "+port_S+").");
	    }
	    MyProxy myproxy = null;
	    myproxy = new MyProxy(stringbuffer.toString(), port);
            try
            {
		gsscredential = myproxy.get(null, stringbuffer1.toString(), stringbuffer2.toString(), lifetimeHours*3600);
		
		if(SAVE_MYPROXY_PROXY) {
		    GlobusCredential proxy = ((GlobusGSSCredentialImpl)gsscredential).getGlobusCredential();
		    ProxyHelper.saveProxy(proxy, properties);
		}
                log.debug("A proxy has been received for user " + stringbuffer1 );
            } catch(IllegalArgumentException exception) {
		exception.printStackTrace();
                StringWriter stringwriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringwriter));
                log.debug(stringwriter);
		errorReport(properties.getWindow(), "Unknown problem while loading certificate your certificate", exception);
		return null;
	    }
            catch(Exception exception)
            {
		if(exception.getMessage().indexOf("Credentials do not exist")>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: No credentials on server (wrong username?)");
		} else if(exception.getMessage().indexOf("Bad password")>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: Bad password");
		} else if(exception.getMessage().indexOf("Failed to map username too DN via grid-mapfile CA failed to map user")>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: Bad username/password");
		} else if(exception.getMessage().indexOf("PAM authentication failed")>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: Bad username/password");
		} else if(exception.getMessage().indexOf("credentials have expired")>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: Credentials on server has expired");
		} else if(exception.getMessage().indexOf(stringbuffer.toString())>=0) {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    myProxyPrompt.setError("MyProxy: Could not connect to MyProxy server");
		} else {
		    exception.printStackTrace();
		    StringWriter stringwriter = new StringWriter();
		    exception.printStackTrace(new PrintWriter(stringwriter));
		    log.debug(stringwriter);
		    errorReport(properties.getWindow(), "Unknown problem while loading certificate your certificate", exception);
		    return null;
		}
            }
        }
        return gsscredential;
    }

    public static void setParamGSSCredential(String gsscredential) {
        paramGSSCredential = gsscredential;
    }
    
    private static GSSCredential loadProxyFromParam()
        throws GSSException
    {
        GlobusGSSCredentialImpl globusgsscredentialimpl = null;
	
        if (paramGSSCredential.length() > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(paramGSSCredential.getBytes());
                GlobusCredential globuscredential = new GlobusCredential(bais);
                globusgsscredentialimpl = new GlobusGSSCredentialImpl(globuscredential, 1);
                globuscredential.verify();
            } catch (Exception e) {
                log.debug("Error loading proxy from <param>:"+e.toString());
                globusgsscredentialimpl = null;
            }
        }
	
        return globusgsscredentialimpl;
    }
    
    private static GSSCredential loadExistingProxy()
        throws GSSException
    {
        GlobusGSSCredentialImpl globusgsscredentialimpl = null;
        CoGProperties cogproperties = CoGProperties.getDefault();
        try
        {
	    if (!(new File(cogproperties.getProxyFile())).exists()) {
		return null;
	    }
            GlobusCredential globuscredential = new GlobusCredential(cogproperties.getProxyFile());
            globusgsscredentialimpl = new GlobusGSSCredentialImpl(globuscredential, 1);
            globuscredential.verify();
        }
        catch(GlobusCredentialException globuscredentialexception)
        {	
            globuscredentialexception.printStackTrace();
            StringWriter stringwriter = new StringWriter();
            globuscredentialexception.printStackTrace(new PrintWriter(stringwriter));
            log.debug(stringwriter);
            if(globuscredentialexception.getMessage().indexOf("Expired") >= 0)
            {
                File file = new File(cogproperties.getProxyFile());
                file.delete();
                globusgsscredentialimpl = null;
            }
        }
        return globusgsscredentialimpl;
    }

   public static GlobusCredential createProxy(String pwd, int proxyType, int lifetimeHours)
	throws Exception {   

	CoGProperties props = CoGProperties.getDefault();

	 X509Certificate userCert = CertUtil.loadCertificate(props.getUserCertFile());
	
	OpenSSLKey key = 
	    new BouncyCastleOpenSSLKey(props.getUserKeyFile());
	
	if (key.isEncrypted()) {
	    try {
		key.decrypt(pwd);
	    } catch(GeneralSecurityException e) {
		throw new Exception("Wrong password or other security error");
	    }
	}
	
	PrivateKey userKey = key.getPrivateKey();
	
	BouncyCastleCertProcessingFactory factory =
	    BouncyCastleCertProcessingFactory.getDefault();

	return factory.createCredential(new X509Certificate[] {userCert},
					userKey,
					props.getProxyStrength(), 
					lifetimeHours * 3600,
					proxyType,
					(X509ExtensionSet)null);
    }

    private static Log log;
    private static GridProxyInitPrompt gridProxyInitPrompt;
    private static MyProxyPrompt myProxyPrompt;

    static 
    {
        log = LogFactory.getLog(com.sshtools.j2ssh.authentication.UserGridCredential.class);
    }
}
