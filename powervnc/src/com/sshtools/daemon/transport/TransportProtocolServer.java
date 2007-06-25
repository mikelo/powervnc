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

package com.sshtools.daemon.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sshtools.daemon.configuration.ServerConfiguration;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.transport.AlgorithmInitializationException;
import com.sshtools.j2ssh.transport.AlgorithmNotAgreedException;
import com.sshtools.j2ssh.transport.AlgorithmNotSupportedException;
import com.sshtools.j2ssh.transport.AlgorithmOperationException;
import com.sshtools.j2ssh.transport.MessageAlreadyRegisteredException;
import com.sshtools.j2ssh.transport.Service;
import com.sshtools.j2ssh.transport.SshMessage;
import com.sshtools.j2ssh.transport.SshMessageListener;
import com.sshtools.j2ssh.transport.SshMsgDisconnect;
import com.sshtools.j2ssh.transport.SshMsgKexInit;
import com.sshtools.j2ssh.transport.SshMsgServiceRequest;
import com.sshtools.j2ssh.transport.TransportProtocolCommon;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.cipher.SshCipher;
import com.sshtools.j2ssh.transport.cipher.SshCipherFactory;
import com.sshtools.j2ssh.transport.compression.SshCompression;
import com.sshtools.j2ssh.transport.compression.SshCompressionFactory;
import com.sshtools.j2ssh.transport.hmac.SshHmac;
import com.sshtools.j2ssh.transport.hmac.SshHmacFactory;
import com.sshtools.j2ssh.transport.kex.KeyExchangeException;
import com.sshtools.j2ssh.transport.kex.SshKeyExchange;
import com.sshtools.j2ssh.transport.publickey.SshKeyPairFactory;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public class TransportProtocolServer
    extends TransportProtocolCommon implements SshMessageListener {
  private static Log log = LogFactory.getLog(TransportProtocolServer.class);
  private Map acceptServices = new HashMap();
  private ServerConfiguration config;
  private boolean refuse = false;
  private Vector services = new Vector();

  /**
   * Creates a new TransportProtocolServer object.
   *
   * @throws IOException
   */
  public TransportProtocolServer() throws IOException {
    config = (ServerConfiguration) ConfigurationLoader.getConfiguration(
        ServerConfiguration.class);
  }

  /**
   * Creates a new TransportProtocolServer object.
   *
   * @param refuse
   *
   * @throws IOException
   */
  public TransportProtocolServer(boolean refuse) throws IOException {
    this();
    this.refuse = refuse;
  }

  /**
   *
   */
  protected void onDisconnect() {
    acceptServices.clear();

    // Make sure that the services have been stopped
    for(Iterator it = services.iterator();
        it.hasNext(); ) {
      Service service = (Service)it.next();
      service.stop();
    }

    services.clear();
  }

  /**
   *
   *
   * @param service
   *
   * @throws IOException
   */
  public void acceptService(Service service) throws IOException {
    acceptServices.put(service.getServiceName(), service);
  }

  /**
   *
   *
   * @throws IOException
   */
  public void refuseConnection() throws IOException {
    log.info("Refusing connection");

    // disconnect with max_connections reason
    sendDisconnect(SshMsgDisconnect.TOO_MANY_CONNECTIONS,
                   "Too many connections");
  }

  /**
   *
   *
   * @throws MessageAlreadyRegisteredException
   */
  public void registerTransportMessages() throws
      MessageAlreadyRegisteredException {
    messageStore.registerMessage(SshMsgServiceRequest.SSH_MSG_SERVICE_REQUEST,
                                 SshMsgServiceRequest.class,
                                 this);
  }

  /**
   *
   *
   * @throws IOException
   */
  protected void startBinaryPacketProtocol() throws IOException {
    if (refuse) {
      sendKeyExchangeInit();

      //sshIn.open();
      refuseConnection();
    }
    else {
      super.startBinaryPacketProtocol();
    }
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getDecryptionAlgorithm() throws AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedCSEncryption(),
                              serverKexInit.getSupportedCSEncryption());
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getEncryptionAlgorithm() throws AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedSCEncryption(),
                              serverKexInit.getSupportedSCEncryption());
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getInputStreamCompAlgorithm() throws
      AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedCSComp(),
                              serverKexInit.getSupportedCSComp());
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getInputStreamMacAlgorithm() throws
      AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedCSMac(),
                              serverKexInit.getSupportedCSMac());
  }

  /**
   *
   */
  protected void setLocalIdent() {
    serverIdent = "SSH-" + PROTOCOL_VERSION + "-"
        + SOFTWARE_VERSION_COMMENTS + " [SERVER]";
  }

  /**
   *
   *
   * @return
   */
  public String getLocalId() {
    return serverIdent;
  }

  /**
   *
   *
   * @param msg
   */
  protected void setLocalKexInit(SshMsgKexInit msg) {
    log.debug(msg.toString());
    serverKexInit = msg;
  }

  /**
   *
   *
   * @return
   */
  protected SshMsgKexInit getLocalKexInit() {
    return serverKexInit;
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getOutputStreamCompAlgorithm() throws
      AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedSCComp(),
                              serverKexInit.getSupportedSCComp());
  }

  /**
   *
   *
   * @return
   *
   * @throws AlgorithmNotAgreedException
   */
  protected String getOutputStreamMacAlgorithm() throws
      AlgorithmNotAgreedException {
    return determineAlgorithm(clientKexInit.getSupportedSCMac(),
                              serverKexInit.getSupportedSCMac());
  }

  /**
   *
   *
   * @param ident
   */
  protected void setRemoteIdent(String ident) {
    clientIdent = ident;
  }

  /**
   *
   *
   * @return
   */
  public String getRemoteId() {
    return clientIdent;
  }

  /**
   *
   *
   * @param msg
   */
  protected void setRemoteKexInit(SshMsgKexInit msg) {
    log.debug(msg.toString());
    clientKexInit = msg;
  }

  /**
   *
   *
   * @return
   */
  protected SshMsgKexInit getRemoteKexInit() {
    return clientKexInit;
  }

  /**
   *
   *
   * @return
   *
   * @throws IOException
   * @throws TransportProtocolException
   */
  protected SshMsgKexInit createLocalKexInit() throws IOException {
    SshMsgKexInit msg = new SshMsgKexInit(properties);
    Map keys = config.getServerHostKeys();

    if (keys.size() > 0) {
      Iterator it = keys.entrySet().iterator();
      List available = new ArrayList();

      while (it.hasNext()) {
        Map.Entry entry = (Map.Entry) it.next();

        if (SshKeyPairFactory.supportsKey(entry.getKey().toString())) {
          available.add(entry.getKey());
        }
        else {
          log.warn("Server host key algorithm '"
                   + entry.getKey().toString() + "' not supported");
        }
      }

      if (available.size() > 0) {
        msg.setSupportedPK(available);
      }
      else {
        throw new TransportProtocolException(
            "No server host keys available");
      }
    }
    else {
      throw new TransportProtocolException(
          "There are no server host keys available");
    }

    return msg;
  }

  /**
   *
   *
   * @throws IOException
   */
  protected void onStartTransportProtocol() throws IOException {
  }

  /**
   *
   *
   * @param kex
   *
   * @throws IOException
   * @throws KeyExchangeException
   */
  protected void performKeyExchange(SshKeyExchange kex,
                                    boolean firstPacketFollows,
                                    boolean useFirstPacket) throws IOException {
    // Determine the public key algorithm and obtain an instance
    String keyType = determineAlgorithm(clientKexInit
                                        .getSupportedPublicKeys(),
                                        serverKexInit.getSupportedPublicKeys());

    // Create an instance of the public key from the factory
    //SshKeyPair pair = SshKeyPairFactory.newInstance(keyType);
    // Get the configuration and get the relevant host key
    Map keys = config.getServerHostKeys();
    Iterator it = keys.entrySet().iterator();
    SshPrivateKey pk; //privateKeyFile = null;

    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();

      if (entry.getKey().equals(keyType)) {
        pk = (SshPrivateKey) entry.getValue();
        kex.performServerExchange(clientIdent, serverIdent,
                                  clientKexInit.toByteArray(),
                                  serverKexInit.toByteArray(), pk,
                                  firstPacketFollows,
                                  useFirstPacket);

        return;
      }
    }

    throw new KeyExchangeException(
        "No host key available for the determined public key algorithm");
  }

  /**
   *
   *
   * @param msg
   *
   * @throws IOException
   */
  public void onMessageReceived(SshMessage msg) throws IOException {
    switch (msg.getMessageId()) {
      case SshMsgServiceRequest.SSH_MSG_SERVICE_REQUEST: {
        onMsgServiceRequest( (SshMsgServiceRequest) msg);
        break;
      }
    }
  }

  /**
   *
   *
   * @param encryptCSKey
   * @param encryptCSIV
   * @param encryptSCKey
   * @param encryptSCIV
   * @param macCSKey
   * @param macSCKey
   *
   * @throws AlgorithmNotAgreedException
   * @throws AlgorithmOperationException
   * @throws AlgorithmNotSupportedException
   * @throws AlgorithmInitializationException
   */
  protected void setupNewKeys(byte[] encryptCSKey, byte[] encryptCSIV,
                              byte[] encryptSCKey, byte[] encryptSCIV,
                              byte[] macCSKey,
                              byte[] macSCKey) throws
      AlgorithmNotAgreedException, AlgorithmOperationException,
      AlgorithmNotSupportedException, AlgorithmInitializationException {
    // Setup the encryption cipher
    SshCipher sshCipher = SshCipherFactory.newInstance(encryptionInUse);
    sshCipher.init(SshCipher.ENCRYPT_MODE, encryptSCIV, encryptSCKey);
    algorithmsOut.setCipher(sshCipher);

    // Setup the decryption cipher
    sshCipher = SshCipherFactory.newInstance(decryptionInUse);
    sshCipher.init(SshCipher.DECRYPT_MODE, encryptCSIV, encryptCSKey);
    algorithmsIn.setCipher(sshCipher);

    // Create and put our macs into operation
    SshHmac hmac = SshHmacFactory.newInstance(outgoingMacInUse);
    hmac.init(macSCKey);
    algorithmsOut.setHmac(hmac);
    hmac = SshHmacFactory.newInstance(incomingMacInUse);
    hmac.init(macCSKey);
    algorithmsIn.setHmac(hmac);

    SshCompression compression = SshCompressionFactory.newInstance(
        incomingCompInUse);
    if (compression != null) {
      compression.init(SshCompression.INFLATER, 1);
      algorithmsIn.setCompression(compression);
    }

    compression = SshCompressionFactory.newInstance(outgoingCompInUse);
    if (compression != null) {
      compression.init(SshCompression.DEFLATER, 1);
      algorithmsIn.setCompression(compression);
    }

  }

  private void onMsgServiceRequest(SshMsgServiceRequest msg) throws IOException {
    if (acceptServices.containsKey(msg.getServiceName())) {
      Service service = (Service) acceptServices.get(msg.getServiceName());
      service.init(Service.ACCEPTING_SERVICE, this);
      service.start();
      services.add(service);
    }
    else {
      this.sendDisconnect(SshMsgDisconnect.SERVICE_NOT_AVAILABLE,
                          msg.getServiceName() + " is not available");
    }
  }


  public void addService(Service service) {
    services.add(service);
  }
}
