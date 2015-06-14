package net.sf.jftp.tools;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.jftp.JFtp;
import net.sf.jftp.config.Settings;
import net.sf.jftp.gui.framework.HFrame;
import net.sf.jftp.net.SftpVerification;
import net.sf.jftp.system.logging.Log;

import com.sshtools.common.hosts.DialogHostKeyVerification;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.configuration.ConfigurationLoader;
import com.sshtools.j2ssh.configuration.SshConnectionProperties;
import com.sshtools.j2ssh.session.SessionChannelClient;


public class SshShell extends JFrame implements Runnable
{
    BufferedOutputStream out;
    BufferedInputStream in;
    BufferedInputStream err;
    JTextArea text = new JTextArea(25, 101);

    //JTextField input = new JTextField();
    long off;
    Thread runner;
    JScrollPane textP;
    int port;
    SshConnectionProperties properties;
    String input = "";
    SshClient ssh;
    Vector commands = new Vector();
    int currCmd = 0;

    public SshShell(SshConnectionProperties properties, String user, String pass, int port)
    {
        this.port = port;
        this.properties = properties;

        try
        {
            init(user, pass);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Log.debug("ERROR: " + e.getMessage());
        }
    }

    public SshShell()
    {
        try
        {
            String host = JOptionPane.showInternalInputDialog(JFtp.desktop,
                                                              "Please enter a host:");
            String user = JOptionPane.showInternalInputDialog(JFtp.desktop,
                                                              "Please enter your username:");
            String pass = JOptionPane.showInternalInputDialog(JFtp.desktop,
                                                              "Please enter your password:");

            if((host != null) && (user != null) && (pass != null))
            {
                init(user, pass);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Log.debug("ERROR: " + e.getMessage());
        }
    }

    public void init(String user, String pass) throws Exception
    {
        setTitle("SSH Shell");

        //setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    ssh.disconnect();
                    dispose();
                }
            });

        //setLocation(150, 150);
        HFrame.fixLocation(this);

        textP = new JScrollPane(text);
        text.setFont(new Font("Monospaced", Font.TRUETYPE_FONT, 10));

        //text.setCaretColor(Color.black);
        /*
                text.setCaret(new DefaultCaret()
                {
                             public void focusGained(FocusEvent e)
                             {
                                 super.focusGained(e);
                                  setVisible(true);
                              }

                             public void focusLost(FocusEvent e)
                             {
                                 super.focusLost(e);
                                  setVisible(true);
                              }

                });
        */
        getContentPane().setLayout(new BorderLayout(5, 5));
        getContentPane().add("Center", textP);

        //getContentPane().add("South", input);
        text.setEditable(false);
        text.setLineWrap(true);
        
        setBackground(text.getBackground());

        text.addKeyListener(new KeyAdapter()
            {
                public void keyPressed(KeyEvent e)
                {
                    if((e.getKeyCode() == KeyEvent.VK_BACK_SPACE) &&
                           (input.length() > 0))
                    {
                        input = input.substring(0, input.length() - 1);

                        String t = text.getText();
                        t = t.substring(0, t.length() - 1);
                        text.setText(t);
                    }
                    else if(e.getKeyCode() == KeyEvent.VK_UP)
                    {
                        String t = text.getText();
                        t = t.substring(0, t.length() - input.length());

                        if((currCmd <= commands.size()) && (currCmd > 0))
                        {
                            currCmd--;

                            String cmd = (String) commands.get(currCmd);
                            input = cmd.substring(0, cmd.length() - 1);
                            text.setText(t + input);
                        }
                    }
                    else if(e.getKeyCode() == KeyEvent.VK_DOWN)
                    {
                        String t = text.getText();
                        t = t.substring(0, t.length() - input.length());

                        if(((currCmd + 1) < commands.size()) && (currCmd >= 0))
                        {
                            currCmd++;

                            String cmd = (String) commands.get(currCmd);
                            input = cmd.substring(0, cmd.length() - 1);
                            text.setText(t + input);
                        }
                    }
                    else if(e.getKeyCode() != KeyEvent.VK_SHIFT)
                    {
                        //Char c = new Char(e.getKeyChar());
                        if(!e.isActionKey())
                        {
                            input += e.getKeyChar();
                            text.append("" + e.getKeyChar());
                        }
                    }

                    if(e.getKeyCode() == KeyEvent.VK_ENTER)
                    {
                        send();
                    }
                }
            });

        ConfigurationLoader.initialize(false);

        ssh = new SshClient();
        ssh.setSocketTimeout(30000);

        //SshConnectionProperties properties = new SshConnectionProperties();
        //properties.setHost(host);
        //properties.setPort(port);		
        //properties.setPrefPublicKey("ssh-dss");
        if(Settings.getEnableSshKeys())
        {
            ssh.connect(properties, new DialogHostKeyVerification(this));
        }
        else
        {
            ssh.connect(properties,
                        new SftpVerification(Settings.sshHostKeyVerificationFile));
        }

        PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
        pwd.setUsername(user);
        pwd.setPassword(pass);

        int result = 0;
        	//ssh.authenticate(pwd);

        if(result == AuthenticationProtocolState.COMPLETE)
        {
            SessionChannelClient session = ssh.openSessionChannel();

            if(!session.requestPseudoTerminal("vt100", 80, 24, 0, 0, ""))
            {
                Log.debug("ERROR: Could not open terminal");
            }

            if(session.startShell())
            {
                out = new BufferedOutputStream(session.getOutputStream());
                in = new BufferedInputStream(session.getInputStream());
                err = new BufferedInputStream(session.getStderrInputStream());

                //session.getState().waitForState(ChannelState.CHANNEL_CLOSED);
            }
            else
            {
                Log.debug("ERROR: Could not start shell");
            }
        }
        else
        {
            ssh.disconnect();
        }

        pack();
        setVisible(true);

        runner = new Thread(this);
        runner.start();

        toFront();
        text.requestFocus();
    }

    public void run()
    {
        try
        {
            byte[] b = new byte[4096];
            int i;

            while((i = in.read(b)) != StreamTokenizer.TT_EOF)
            {
                text.append(new String(b, 0, i));

                //Log.out("recv: "+i+" -> "+new String(b));
                while(err.available() > 0)
                {
                    err.read(b);
                    text.append(new String(b, 0, i));
                }

                while(text.getRows() > 500)
                {
                    String t = text.getText();
                    t = t.substring(250);

                    text.setText(t);
                }

                try
                {
                    Thread.sleep(100);
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                }

                JScrollBar bar = textP.getVerticalScrollBar();
                bar.setValue(bar.getMaximum());
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.debug("ERROR: " + ex.getMessage());
            this.dispose();
        }
    }

    private void send()
    {
        try
        {
            String msg = input;
            input = "";

            out.write(msg.getBytes());
            out.flush();

            commands.add(msg);
            currCmd = commands.size();

            //Log.out("send: "+msg);		
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
            Log.debug("ERROR: " + ex.getMessage());
            this.dispose();
        }
    }

    public static void main(String[] args)
    {
        new SshShell();
    }
}
