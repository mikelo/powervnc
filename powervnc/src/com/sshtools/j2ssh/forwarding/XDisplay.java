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
 *  Changes:
 *  (c) 2005-2006 CCLRC.  David Spence: Split up VNC and X code: new file VNCDisplay.java
 *
 *
 */

package com.sshtools.j2ssh.forwarding;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public class XDisplay {

  private static Log log = LogFactory.getLog(XDisplay.class);

    private void processDisplay(String display) {
	String os = System.getProperty("os.name");
	String arch = System.getProperty("os.arch");
	boolean linux = os.equals("Linux") && arch.equals("i386");
	/*
	 * Check if it is a unix domain socket.  Unix domain displays are in
	 * one of the following formats: unix:d[.s], :d[.s], ::d[.s]
	 *
	 * At the moment as we are using a native library we only support 
	 * x86 linux... The module could be compiled to run with other UNIXes
	 * and architectures if required.
	 */
	if (linux && (display.startsWith("unix:") || display.charAt(0) == ':')) {
	    /* Connect to the unix domain socket. */
	    isLocal=true;
	    String rest = display.substring(display.indexOf(":")+1);
	    if(rest.charAt(0)==':') rest = rest.substring(1);
	    try {
		if(rest.indexOf(".")>=0) rest = rest.substring(0,rest.indexOf("."));
		this.display = Integer.parseInt(rest);
	    } catch(NumberFormatException e) {
		throw new IllegalArgumentException("Could not parse display number from DISPLAY: "+display);
	    }
	    host=null;
	} else {
	    /*
	     * Connect to an inet socket.  The DISPLAY value is supposedly
	     * hostname:d[.s], where hostname may also be numeric IP address.
	     */
	    isLocal=false;
	    if(display.indexOf(":")==-1) throw new IllegalArgumentException("Could not find ':' in DISPLAY: "+display);
	    host = display.substring(0, display.indexOf(":"));
	    String rest = display.substring(display.indexOf(":")+1);
	    try {
		if(rest.indexOf(".")>=0) rest = rest.substring(0,rest.indexOf("."));
		this.display = Integer.parseInt(rest);
	    } catch(NumberFormatException e) {
		throw new IllegalArgumentException("Could not parse display number from DISPLAY: "+display);
	    }
	}
    }



    private String host;
    private int display;
    private boolean isLocal;

    /**
     * Creates a new XDisplay object.
     *
     * @param string
     */
    public XDisplay() {
	try {
	    String display = System.getenv("DISPLAY");
	    if(display==null) {
		log.warn("Display not set, trying localhost:0.0");
		processDisplay("localhost:0.0");
	    } else {
		processDisplay(display);
	    }
	} catch(Error e) { // in 1.4 System.getenv was depricated, it is back in 1.5
	    if(!System.getProperty("os.name").startsWith("Window")) {
		try {
		    String cmd = "env";
		    log.debug("Executing " + cmd);
		    Process process = Runtime.getRuntime().exec(cmd);
		    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		    String line = reader.readLine();
		    while(line!=null) {
			if(line.startsWith("DISPLAY=")) { 
			    processDisplay(line.substring(8)); 
			    return;
			}
			line = reader.readLine();
		    }
		    reader.close();
		    processDisplay(":0.0");
		    return;
		} catch(IllegalArgumentException e1) {
		    e1.printStackTrace();
		    processDisplay(":0.0");
		    return;
		} catch(IOException e1) {
		    e1.printStackTrace();
		    processDisplay(":0.0");
		    return;
		}
	    } else {
		processDisplay("localhost:0.0");
	    }
	}
    }
    
    /**
     * Creates a new XDisplay object.
     *
     * @param string
     * @param portOffset
     */
    public XDisplay(String string) {
	processDisplay(string);
    }
    

    /**
     *
     *
     * @param string
     */
    public void setString(String string) {
	processDisplay(string);
    }
    
    /**
     *
     *
     * @return
     */
    public int getPort() {
	return (getDisplay() + 6000);
    }

    /**
     *
     *
     * @param host if null then connect to local UNIX domain socket
     */
    public void setHost(String host) {
	if(host==null) {
	    host=null;
	    isLocal=true;
	} else {
	    this.host = host;
	    isLocal=false;
	}
    }
    
    /**
     *
     *
     * @param display
     */
    public void setDisplay(int display) {
	this.display = display;
    }
    
    /**
     *
     *
     * @return
     */
    public boolean isLocalUNIXSocket() {
	return isLocal;
    }

    /**
     *
     *
     * @return
     */
    public String getHost() {
	return host;
    }

    /**
     *
     *
     * @return
     */
    public int getDisplay() {
	return display;
    }
    
    /**
     *
     *
     * @return
     */
    public String toString() {
	if(isLocal) 
	    return ":"+getDisplay(); 
	else
	    return getHost() + ":" + getDisplay();
    }
}
