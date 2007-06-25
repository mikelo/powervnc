/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.jftp.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamTokenizer;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JLabel;

import net.sf.jftp.config.Settings;
import net.sf.jftp.system.StringUtils;
import net.sf.jftp.system.logging.Log;

import com.sshtools.common.hosts.DialogHostKeyVerification;
import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.authentication.PublicKeyAuthenticationClient;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.sftp.SftpFile;
import com.sshtools.j2ssh.sftp.SftpFileInputStream;
import com.sshtools.j2ssh.sftp.SftpFileOutputStream;
import com.sshtools.j2ssh.sftp.SftpSubsystemClient;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKey;
import com.sshtools.j2ssh.transport.publickey.SshPrivateKeyFile;


public class SftpConnection implements BasicConnection
{
    public static int smbBuffer = 32000;
    private String path = "";
    private String pwd = "/";
    private Vector listeners = new Vector();
    private String[] files;
    private String[] size = new String[0];
    private int[] perms = null;
    private String user;
    private String pass;
    private String host;
    private String baseFile;
    private int fileCount;
    private boolean isDirUpload = false;
    private boolean shortProgress = false;
    private int RW = SftpSubsystemClient.OPEN_CREATE |
                     SftpSubsystemClient.OPEN_WRITE;
    private int W = SftpSubsystemClient.OPEN_CREATE;
    private int R = SftpSubsystemClient.OPEN_READ;
    private SftpSubsystemClient sftp = null;
    private int port = 22;
    private boolean connected = false;
    private SshClient ssh = null;
    private String keyfile = null;
    
    //private int timeout = 30000;

    //private SessionChannelClient session = null;
    private SshConnectionProperties properties = new SshConnectionProperties();

    public SftpConnection(SshClient ssh)
    {
    	this.ssh = ssh;
//    	ssh.get
    	if(login())
    		System.out.println("sftp ok");
    }

    public SftpConnection(SshConnectionProperties properties, String keyfile)
    {
	this.properties = properties;
        this.keyfile = keyfile;
	this.host = properties.getHost();
	this.port = properties.getPort();
    } 
   
// seem to be broken, use the newer constructors    
/* 
    public SftpConnection(String host)
    {
        this.host = host;
        properties.setHost(host);
    }

    public SftpConnection(String host, int port)
    {
        this.host = host;
        this.port = port;
        properties.setHost(host);
        properties.setPort(port);
    }
    */

    private boolean login()
    {
        try
        {

        	//ssh = new SshClient();
            //ssh.setSocketTimeout(timeout);
            //ssh.setKexTimeout(timeout);
/*            
            Log.debug("Host: "+properties.getHost()+":"+properties.getPort());

            if(!Settings.getEnableSshKeys())
            {
                ssh.connect(properties,
                            new SftpVerification(Settings.sshHostKeyVerificationFile));
            }
            else
            {
                ssh.connect(properties,
                            new DialogHostKeyVerification(new JLabel()));

            }

            int result = -1;

            if(keyfile == null)
            {
                //ssh.connect(host);
                PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
                pwd.setUsername(user);
                pwd.setPassword(pass);

//                result = ssh.authenticate(pwd);
            }
            else
            {
                Log.out("keyfile-auth: " + keyfile);

                PublicKeyAuthenticationClient pk = new PublicKeyAuthenticationClient();
                pk.setUsername(user);

                SshPrivateKeyFile file = SshPrivateKeyFile.parse(new File(keyfile));
                SshPrivateKey key = file.toPrivateKey(pass);

                pk.setKey(key);
//                result = ssh.authenticate(pk);
            }
*/
//            if(result == AuthenticationProtocolState.COMPLETE)
//            {
/*
    		SessionChannelClient session = ssh.openSessionChannel();
                boolean ok = session.startSubsystem("sftp");
            	if(ok)
            		System.out.println("ok");
*/
        	sftp = ssh.openSftpChannel();
        	//        	sftp = new SftpSubsystemClient();

//        	sftp = ssh.op
//        	ssh.op
/*     
        	boolean ok1 = ssh.openChannel(sftp);
            	if(ok1)
            		System.out.println("ok1");

                boolean ok2 = sftp.startSubsystem();
            	if(ok2)
            		System.out.println("ok2");
*/
                //Log.out("sftp sub ok: "+ok+" and "+ok2);
                // newer api
//                 sftp = ssh.openSftpClient();
//        	sftp.close();
        	connected = true;

                return true;
/*
        }
            else
            {
                return false;
            }
*/
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
//            Log.debug("Error: " + ex);

            return false;
        }
    }

    public int removeFileOrDir(String file)
    {
        file = toSFTP(file);

        try
        {
            SftpFile f;

            if(!file.endsWith("/"))
            {
                Log.out(">>>>>>>> remove file: " + file);
                f = sftp.openFile(file, RW);
                f.delete();
            }
            else
            {
                Log.out(">>>>>>>> remove dir: " + file);
                f = sftp.openDirectory(file);
                cleanSftpDir(file, f);
                sftp.closeFile(f);
                sftp.removeDirectory(file);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.debug("Removal failed (" + ex + ").");
            ex.printStackTrace();

            return -1;
        }

        return 1;
    }

    private void cleanSftpDir(String dir, SftpFile f2)
                       throws Exception
    {
        Log.out(">>>>>>>> cleanSftpDir: " + dir);

        Vector v = new Vector();

        while(sftp.listChildren(f2, v) > 0)
        {
            ;
        }

        String[] tmp = new String[v.size()];
        SftpFile[] f = new SftpFile[v.size()];
        Enumeration e = v.elements();
        int x = 0;

        while(e.hasMoreElements())
        {
            f[x] = ((SftpFile) e.nextElement());
            tmp[x] = f[x].getFilename();

            //Log.out("sftp delete: " + tmp[x]);
            //Log.out(">>>>>>>> remove file/dir: " + tmp[x]);
            if(f[x].isDirectory() && !tmp[x].endsWith("/"))
            {
                tmp[x] = tmp[x] + "/";
            }

            //sftp.closeFile(f[x]);
            x++;
        }

        if(tmp == null)
        {
            return;
        }

        for(int i = 0; i < tmp.length; i++)
        {
            if(tmp[i].equals("./") || tmp[i].equals("../"))
            {
                continue;
            }

            SftpFile f3;

            //System.out.println(dir+tmp[i]);
            if(tmp[i].endsWith("/"))
            {
                f3 = sftp.openDirectory(dir + tmp[i]);
            }
            else
            {
                f3 = sftp.openFile(dir + tmp[i], RW);
            }

            Log.out(">>>>>>>> remove file/dir: " + dir + tmp[i]);

            if(f3.isDirectory())
            {
                cleanSftpDir(dir + tmp[i], f3);

                //sftp.closeFile(f3);
                sftp.removeDirectory(dir + tmp[i]);
            }
            else
            {
                f3.delete();
            }
        }
    }

    public void sendRawCommand(String cmd)
    {
    }

    public void disconnect()
    {
        try
        {
            //sftp.stop();
            //session.close();
            ssh.disconnect();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Log.debug("SftpSshClient.disconnect()" + e);
        }

        connected = false;
    }

    public boolean isConnected()
    {
        return connected;
    }

    public String getPWD()
    {
        //Log.debug("PWD: " + pwd);
        return toSFTPDir(pwd);
    }

    public boolean mkdir(String dirName)
    {
        try
        {
            if(!dirName.endsWith("/"))
            {
                dirName = dirName + "/";
            }

            dirName = toSFTP(dirName);

            sftp.makeDirectory(dirName);

            fireDirectoryUpdate();

            return true;
        }
        catch(Exception ex)
        {
            Log.debug("Failed to create directory (" + ex + ").");

            return false;
        }
    }

    public void list() throws IOException
    {
    }

    public boolean chdir(String p)
    {
        return chdir(p, true);
    }

    public boolean chdir(String p, boolean refresh)
    {
        String tmp = toSFTP(p);
//        System.out.println(tmp);
        try
        {
            if(!tmp.endsWith("/"))
            {
                tmp = tmp + "/";
            }

            if(tmp.endsWith("../"))
            {
                return cdup();
            }

            // Log.out("sftp path: "+tmp);
            SftpFile f = sftp.openDirectory(tmp);

            //Log.out("sftp after path...");
            //if(!f.isDirectory()) return false;
            sftp.closeFile(f);

            pwd = toSFTP(f.getAbsolutePath());

            //Log.debug("chdir: " + getPWD());
            if(refresh)
            {
                fireDirectoryUpdate();
            }

            //System.out.println("chdir2: " + getPWD());
            //Log.debug("Changed path to: " + tmp);
            return true;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();

            //System.out.println(tmp);
            Log.debug("Could not change directory (" + ex + ").");

            return false;
        }
    }

    public boolean cdup()
    {
        String tmp = pwd;

        if(pwd.endsWith("/"))
        {
            tmp = pwd.substring(0, pwd.lastIndexOf("/"));
        }

        return chdir(tmp.substring(0, tmp.lastIndexOf("/") + 1));
    }

    public boolean chdirNoRefresh(String p)
    {
        return chdir(p, false);
    }

    public String getLocalPath()
    {
        return path;
    }

    public boolean setLocalPath(String p)
    {
        if(StringUtils.isRelative(p))
        {
            p = path + p;
        }

        p = p.replace('\\', '/');

        //System.out.println(", local 2:" + p);
        File f = new File(p);

        if(f.exists())
        {
            try
            {
                path = f.getCanonicalPath();
                path = path.replace('\\', '/');

                if(!path.endsWith("/"))
                {
                    path = path + "/";
                }

                //System.out.println("localPath: "+path);
            }
            catch(IOException ex)
            {
                Log.debug("Error: can not get pathname (local)!");

                return false;
            }
        }
        else
        {
            Log.debug("(local) No such path: \"" + p + "\"");

            return false;
        }

        return true;
    }

    public String[] sortLs()
    {
        try
        {
            String t = getPWD();

            SftpFile fx = sftp.openDirectory(t);

            //if(fx == null) System.out.println("Sftp: fx null");
            Vector v = new Vector();

            while(sftp.listChildren(fx, v) > 0)
            {
                ;
            }

            String[] tmp = new String[v.size()];
            SftpFile[] f = new SftpFile[v.size()];
            files = new String[tmp.length];
            size = new String[tmp.length];
            perms = new int[tmp.length];

            Enumeration e = v.elements();
            int x = 0;

            while(e.hasMoreElements())
            {
                f[x] = ((SftpFile) e.nextElement());
                tmp[x] = f[x].getFilename();

                size[x] = f[x].getAttributes().getSize().toString();

                //if(f[x].canWrite()) Log.debug(tmp[x]);
                //if(f[x].canWrite()) perms[x] = FtpConnection.W;
                //else
                if(!f[x].canRead())
                {
                    perms[x] = FtpConnection.DENIED;
                }
                else
                {
                    perms[x] = FtpConnection.R;
                }

                //Log.debugRaw(".");
                if(f[x].isDirectory() && !tmp[x].endsWith("/"))
                {
                    tmp[x] = tmp[x] + "/";
                }

                x++;
            }

            sftp.closeFile(fx);

            for(int i = 0; i < tmp.length; i++)
            {
                files[i] = tmp[i];
            }

            //Log.debug(" done.");
            return files;
        }
        catch(Exception ex)
        {
            //ex.printStackTrace();
            //Log.debug(" Error while listing directory: " + ex);
            return new String[0];
        }
    }

    public String[] sortSize()
    {
        return size;
    }

    public int[] getPermissions()
    {
        return perms;
    }

    public int handleUpload(String f)
    {
        if(Settings.getEnableSftpMultiThreading())
        {
            SftpTransfer t = new SftpTransfer(ssh, getLocalPath(), getPWD(),
                                              f, user, pass, listeners,
                                              Transfer.UPLOAD);
        }
        else
        {
            upload(f);
        }

        return 0;
    }

    public int handleDownload(String f)
    {
        if(Settings.getEnableSftpMultiThreading())
        {
            SftpTransfer t = new SftpTransfer(ssh, getLocalPath(), getPWD(),
                                              f, user, pass, listeners,
                                              Transfer.DOWNLOAD);
        }
        else
        {
            download(f);
        }

        return 0;
    }

    public int upload(String f)
    {
        String file = toSFTP(f);

        if(file.endsWith("/"))
        {
            String out = StringUtils.getDir(file);
            uploadDir(file, getLocalPath() + out);
            fireActionFinished(this);
        }
        else
        {
            String outfile = StringUtils.getFile(file);

            //System.out.println("transfer: " + file + ", " + getLocalPath() + outfile);
            work(getLocalPath() + outfile, file, true);
            fireActionFinished(this);
        }

        return 0;
    }

    public int download(String f)
    {
        String file = toSFTP(f);

        if(file.endsWith("/"))
        {
            String out = StringUtils.getDir(file);
            downloadDir(file, getLocalPath() + out);
            fireActionFinished(this);
        }
        else
        {
            String outfile = StringUtils.getFile(file);

            //System.out.println("transfer: " + file + ", " + getLocalPath() + outfile);
            work(file, getLocalPath() + outfile, false);
            fireActionFinished(this);
        }

        return 0;
    }

    private void downloadDir(String dir, String out)
    {
        try
        {
            //System.out.println("downloadDir: " + dir + "," + out);
            fileCount = 0;
            shortProgress = true;
            baseFile = StringUtils.getDir(dir);

            SftpFile f2 = sftp.openDirectory(dir);

            Vector v = new Vector();

            while(sftp.listChildren(f2, v) > 0)
            {
                ;
            }

            String[] tmp = new String[v.size()];
            SftpFile[] f = new SftpFile[v.size()];
            Enumeration e = v.elements();
            int x = 0;

            while(e.hasMoreElements())
            {
                f[x] = ((SftpFile) e.nextElement());
                tmp[x] = f[x].getFilename();
                Log.debugRaw(".");

                if(f[x].isDirectory() && !tmp[x].endsWith("/"))
                {
                    tmp[x] = tmp[x] + "/";
                }

                //sftp.closeFile(f[x]);
                x++;
            }

            //sftp.closeFile(f2);
            File fx = new File(out);
            fx.mkdir();

            for(int i = 0; i < tmp.length; i++)
            {
                if(tmp[i].equals("./") || tmp[i].equals("../"))
                {
                    continue;
                }

                tmp[i] = tmp[i].replace('\\', '/');

                //System.out.println("1: " + dir+tmp[i] + ", " + out +tmp[i]);
                SftpFile f3 = sftp.openFile(dir + tmp[i], R);

                if(f3.isDirectory())
                {
                    if(!tmp[i].endsWith("/"))
                    {
                        tmp[i] = tmp[i] + "/";
                    }

                    downloadDir(dir + tmp[i], out + tmp[i]);
                }
                else
                {
                    fileCount++;
                    fireProgressUpdate(baseFile,
                                       DataConnection.GETDIR + ":" + fileCount,
                                       -1);
                    work(dir + tmp[i], out + tmp[i], false);
                }

                //sftp.closeFile(f3);
            }

            fireProgressUpdate(baseFile,
                               DataConnection.DFINISHED + ":" + fileCount, -1);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println(dir + ", " + out);
            Log.debug("Transfer error: " + ex);
            fireProgressUpdate(baseFile,
                               DataConnection.FAILED + ":" + fileCount, -1);
        }

        shortProgress = false;
    }

    private void uploadDir(String dir, String out)
    {
        try
        {
            //System.out.println("uploadDir: " + dir + "," + out);
            isDirUpload = true;
            fileCount = 0;
            shortProgress = true;
            baseFile = StringUtils.getDir(dir);

            File f2 = new File(out);
            String[] tmp = f2.list();

            if(tmp == null)
            {
                return;
            }

            sftp.makeDirectory(dir);
            sftp.changePermissions(dir, "rwxr--r--");

            for(int i = 0; i < tmp.length; i++)
            {
                if(tmp[i].equals("./") || tmp[i].equals("../"))
                {
                    continue;
                }

                tmp[i] = tmp[i].replace('\\', '/');

                //System.out.println("1: " + dir+tmp[i] + ", " + out +tmp[i]);
                File f3 = new File(out + tmp[i]);

                if(f3.isDirectory())
                {
                    if(!tmp[i].endsWith("/"))
                    {
                        tmp[i] = tmp[i] + "/";
                    }

                    uploadDir(dir + tmp[i], out + tmp[i]);
                }
                else
                {
                    fileCount++;
                    fireProgressUpdate(baseFile,
                                       DataConnection.PUTDIR + ":" + fileCount,
                                       -1);
                    work(out + tmp[i], dir + tmp[i], true);
                }
            }

            fireProgressUpdate(baseFile,
                               DataConnection.DFINISHED + ":" + fileCount, -1);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            System.out.println(dir + ", " + out);
            Log.debug("Transfer error: " + ex);
            fireProgressUpdate(baseFile,
                               DataConnection.FAILED + ":" + fileCount, -1);
        }

        isDirUpload = false;
        shortProgress = true;
    }

    private String toSFTP(String f)
    {
        String file;

        if(f.startsWith("/"))
        {
            file = f;
        }
        else
        {
            file = getPWD() + f;
        }

        file = file.replace('\\', '/');

        //System.out.println("file: "+file);
        return file;
    }

    private String toSFTPDir(String f)
    {
        String file;

        if(f.startsWith("/"))
        {
            file = f;
        }
        else
        {
            file = pwd + f;
        }

        file = file.replace('\\', '/');

        if(!file.endsWith("/"))
        {
            file = file + "/";
        }

        //System.out.println("file: "+file);
        return file;
    }

    private void work(String file, String outfile, boolean up)
    {
        BufferedInputStream in = null;
        BufferedOutputStream out = null;

        try
        {
            SftpFile inf = null;
            SftpFile of = null;
            boolean outflag = false;

            if(up)
            {
                in = new BufferedInputStream(new FileInputStream(file));
            }
            else
            {
                inf = sftp.openFile(file, R);
                in = new BufferedInputStream(new SftpFileInputStream(inf));
            }

            if(up)
            {
                outflag = true;

                //FileAttributes fa = new FileAttributes();
                //fa.setPermissions("rwxr--r--");
                //fa.setSize(new UnsignedInteger64(
                //	new java.math.BigInteger(""+new File(file).length())));
                of = sftp.openFile(outfile, W);
                of.delete();

                of = sftp.openFile(outfile, RW); // , fa);		
                sftp.changePermissions(of, "rwxr--r--");
                out = new BufferedOutputStream(new SftpFileOutputStream(of));
            }
            else
            {
                out = new BufferedOutputStream(new FileOutputStream(outfile));
            }

            //System.out.println("out: " + outfile + ", in: " + file);
            byte[] buf = new byte[smbBuffer];
            int len = 0;
            int reallen = 0;

            //System.out.println(file+":"+getLocalPath()+outfile);
            while(true)
            {
                len = in.read(buf);

                //System.out.print(".");
                if(len == StreamTokenizer.TT_EOF)
                {
                    break;
                }

                out.write(buf, 0, len);
                reallen += len;

                //System.out.println(file + ":" + StringUtils.getFile(file));
                if(outflag)
                {
                    fireProgressUpdate(StringUtils.getFile(outfile),
                                       DataConnection.PUT, reallen);
                }
                else
                {
                    fireProgressUpdate(StringUtils.getFile(file),
                                       DataConnection.GET, reallen);
                }
            }

            fireProgressUpdate(file, DataConnection.FINISHED, -1);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Log.debug("Error with file IO (" + ex + ")!");
            fireProgressUpdate(file, DataConnection.FAILED, -1);
        }
        finally
        {
            try
            {
                out.flush();
                out.close();
                in.close();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public boolean rename(String oldName, String newName)
    {
        try
        {
            oldName = toSFTP(oldName);
            newName = toSFTP(newName);

            SftpFile f = sftp.openFile(oldName, RW);
            f.rename(newName);

            return true;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();

            Log.debug("Could rename file (" + ex + ").");

            return false;
        }
    }

    private void update(String file, String type, int bytes)
    {
        if(listeners == null)
        {
            return;
        }
        else
        {
            for(int i = 0; i < listeners.size(); i++)
            {
                ConnectionListener listener = (ConnectionListener) listeners.elementAt(i);
                listener.updateProgress(file, type, bytes);
            }
        }
    }

    public void addConnectionListener(ConnectionListener l)
    {
        listeners.add(l);
    }

    public void setConnectionListeners(Vector l)
    {
        listeners = l;
    }

    /** remote directory has changed */
    public void fireDirectoryUpdate()
    {
        if(listeners == null)
        {
            return;
        }
        else
        {
            for(int i = 0; i < listeners.size(); i++)
            {
                ((ConnectionListener) listeners.elementAt(i)).updateRemoteDirectory(this);
            }
        }
    }

    public boolean login(String user, String pass)
    {
        this.user = user;
        this.pass = pass;

        if(!login())
        {
            Log.debug("Login failed.");

            return false;
        }
        else
        {
            Log.debug("Authed successfully.");

            //if(!chdir(getPWD())) chdir("/");
        }

        return true;
    }

    /** progress update */
    public void fireProgressUpdate(String file, String type, int bytes)
    {
        if(listeners == null)
        {
            return;
        }

        for(int i = 0; i < listeners.size(); i++)
        {
            ConnectionListener listener = (ConnectionListener) listeners.elementAt(i);

            if(shortProgress && Settings.shortProgress)
            {
                if(type.startsWith(DataConnection.DFINISHED))
                {
                    listener.updateProgress(baseFile,
                                            DataConnection.DFINISHED + ":" +
                                            fileCount, bytes);
                }
                else if(isDirUpload)
                {
                    listener.updateProgress(baseFile,
                                            DataConnection.PUTDIR + ":" +
                                            fileCount, bytes);
                }
                else
                {
                    listener.updateProgress(baseFile,
                                            DataConnection.GETDIR + ":" +
                                            fileCount, bytes);
                }
            }
            else
            {
                listener.updateProgress(file, type, bytes);
            }
        }
    }

    public void fireActionFinished(SftpConnection con)
    {
        if(listeners == null)
        {
            return;
        }
        else
        {
            for(int i = 0; i < listeners.size(); i++)
            {
                ((ConnectionListener) listeners.elementAt(i)).actionFinished(con);
            }
        }
    }

    public int upload(String file, InputStream i)
    {
        BufferedOutputStream out = null;
        BufferedInputStream in = null;

        try
        {
            file = toSFTP(file);

            SftpFile of = sftp.openFile(file, RW);

            //of.delete();
            //of = sftp.openFile(file, RW);  
            sftp.changePermissions(of, "rwxr--r--");

            out = new BufferedOutputStream(new SftpFileOutputStream(of));
            in = new BufferedInputStream(i);

            //Log.debug(getLocalPath() + ":" + file+ ":"+getPWD());
            byte[] buf = new byte[smbBuffer];
            int len = 0;
            int reallen = 0;

            while(true)
            {
                len = in.read(buf);

                //System.out.print(".");
                if(len == StreamTokenizer.TT_EOF)
                {
                    break;
                }

                out.write(buf, 0, len);
                reallen += len;

                fireProgressUpdate(StringUtils.getFile(file),
                                   DataConnection.PUT, reallen);
            }

            //byte[] fin = new byte[1];
            //fin[0] = -1;
            //out.write(fin, 0, 1);

            /*
            try {
            	out.flush();
            	out.close();
            } catch(Exception ex) {
            	Log.out("flush: "+ex);
            }
            */
            
            fireProgressUpdate(file, DataConnection.FINISHED, -1);

            return 0;
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Log.debug("Error with file IO (" + ex + ")!");
            fireProgressUpdate(file, DataConnection.FAILED, -1);

            return -1;
        }
        finally
        {
            try
            {
                out.flush();
                out.close();
                in.close();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    public InputStream getDownloadInputStream(String file)
    {
        try
        {
            file = toSFTP(file);

            SftpFile inf = sftp.openFile(file, R);

            return new SftpFileInputStream(inf);
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Log.debug(ex.toString() +
                      " @SftpConnection::getDownloadInputStream");

            return null;
        }
    }

    public Date[] sortDates()
    {
        return null;
    }
}
