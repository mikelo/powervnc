package net.sf.jftp.net;

import net.sf.jftp.system.logging.Log;

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;


public class SftpVerification extends AbstractKnownHostsKeyVerification
{
    public SftpVerification(String tmp) throws InvalidHostFileException
    {
        super(tmp);
    }

    public void onDeniedHost(final String host)
                      throws TransportProtocolException
    {
        Log.debug("Access to '" + host +
                  "' is denied.\nVerify the access granted/denied in the allowed hosts file.");
    }

    public void onHostKeyMismatch(final String host,
                                  SshPublicKey recordedFingerprint,
                                  SshPublicKey actualFingerprint)
                           throws TransportProtocolException
    {
        try
        {
            allowHost(host, actualFingerprint, false);
        }
        catch(Exception ex)
        {
            Log.debug("Error: " + ex);
        }
    }

    public void onUnknownHost(final String host, SshPublicKey fingerprint)
                       throws TransportProtocolException
    {
        try
        {
            allowHost(host, fingerprint, false);
        }
        catch(Exception ex)
        {
            Log.debug("Error: " + ex);
        }
    }

    private void showExceptionMessage(Exception ex)
    {
        Log.debug("Error: " + ex);
    }
}
