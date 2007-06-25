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

package com.sshtools.j2ssh.connection;

import java.io.IOException;

import com.sshtools.j2ssh.io.ByteArrayReader;
import com.sshtools.j2ssh.io.ByteArrayWriter;
import com.sshtools.j2ssh.transport.InvalidMessageException;
import com.sshtools.j2ssh.transport.SshMessage;

/**
 *
 *
 * @author $author$
 * @version $Revision$
 */
public class SshMsgChannelExtendedData
    extends SshMessage {
  /**  */
  public final static int SSH_MSG_CHANNEL_EXTENDED_DATA = 95;

  /**  */
  public final static int SSH_EXTENDED_DATA_STDERR = 1;
  private byte[] channelData;
  private int dataTypeCode;
  private long recipientChannel;

  /**
   * Creates a new SshMsgChannelExtendedData object.
   *
   * @param recipientChannel
   * @param dataTypeCode
   * @param channelData
   */
  public SshMsgChannelExtendedData(long recipientChannel, int dataTypeCode,
                                   byte[] channelData) {
    super(SSH_MSG_CHANNEL_EXTENDED_DATA);

    this.recipientChannel = recipientChannel;
    this.dataTypeCode = dataTypeCode;
    this.channelData = channelData;
  }

  /**
   * Creates a new SshMsgChannelExtendedData object.
   */
  public SshMsgChannelExtendedData() {
    super(SSH_MSG_CHANNEL_EXTENDED_DATA);
  }

  /**
   *
   *
   * @return
   */
  public byte[] getChannelData() {
    return channelData;
  }

  /**
   *
   *
   * @return
   */
  public int getDataTypeCode() {
    return dataTypeCode;
  }

  /**
   *
   *
   * @return
   */
  public String getMessageName() {
    return "SSH_MSG_CHANNEL_EXTENDED_DATA";
  }

  /**
   *
   *
   * @return
   */
  public long getRecipientChannel() {
    return recipientChannel;
  }

  /**
   *
   *
   * @param baw
   *
   * @throws InvalidMessageException
   */
  protected void constructByteArray(ByteArrayWriter baw) throws
      InvalidMessageException {
    try {
      baw.writeInt(recipientChannel);
      baw.writeInt(dataTypeCode);

      if (channelData != null) {
        baw.writeBinaryString(channelData);
      }
      else {
        baw.writeString("");
      }
    }
    catch (IOException ioe) {
      throw new InvalidMessageException("Invalid message data");
    }
  }

  /**
   *
   *
   * @param bar
   *
   * @throws InvalidMessageException
   */
  protected void constructMessage(ByteArrayReader bar) throws
      InvalidMessageException {
    try {
      recipientChannel = bar.readInt();
      dataTypeCode = (int) bar.readInt();

      if (bar.available() > 0) {
        channelData = bar.readBinaryString();
      }
    }
    catch (IOException ioe) {
      throw new InvalidMessageException("Invalid message data");
    }
  }
}
