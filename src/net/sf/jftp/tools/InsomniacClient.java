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

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.jftp.tools;

import net.sf.jftp.*;
import net.sf.jftp.config.*;
import net.sf.jftp.gui.*;
import net.sf.jftp.gui.base.dir.DirCellRenderer;
import net.sf.jftp.gui.base.dir.DirEntry;
import net.sf.jftp.gui.framework.*;
import net.sf.jftp.net.*;
import net.sf.jftp.system.logging.Log;
import net.sf.jftp.util.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;


public class InsomniacClient extends HPanel implements Runnable, ActionListener
{
    private HTextField host = new HTextField("URL:",
                                             "http://canofsleep.com:8180/Insomniac/test.jsp?keywords=",
                                             40);
    private HTextField search = new HTextField("Search term:", "mp3", 20);
    private HTextField user = new HTextField("User:", "guest", 10);
    private HPasswordField pass = new HPasswordField("Password:", "none");
    private HTextField dir = new HTextField("Download to:",
                                            Settings.defaultWorkDir, 20);
    private JPanel p1 = new JPanel();
    private JPanel p2 = new JPanel();
    private JButton ok = new JButton("Search");
    private JList list = new JList();
    private JButton load = new JButton("Download file");
    private Vector files;
    private Vector sizes;
    private Thread runner;
    private boolean doSearch;
    private Vector fileNames;

    public InsomniacClient()
    {
        setLayout(new BorderLayout());

        JPanel x1 = new JPanel();
        x1.setLayout(new FlowLayout(FlowLayout.LEFT));
        x1.add(host);
        x1.add(search);

        JPanel x2 = new JPanel();
        x2.setLayout(new FlowLayout(FlowLayout.LEFT));
        x2.add(search);
        x2.add(dir);

        JPanel x3 = new JPanel();
        x3.setLayout(new FlowLayout(FlowLayout.LEFT));
        x3.add(user);
        x3.add(pass);

        p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
        p1.add(x1);
        p1.add(x2);
        p1.add(x3);

        JPanel okP = new JPanel();
        okP.setLayout(new FlowLayout(FlowLayout.CENTER));
        okP.add(ok);
        okP.add(new JLabel("   "));
        okP.add(load);

        //p1.add(okP);
        //list.setHeight(300);
        list.setFixedCellWidth(720);
        list.setVisibleRowCount(12);
        list.setCellRenderer(new DirCellRenderer());

        JScrollPane pane = new JScrollPane(list);
        p2.add(new JScrollPane(pane));

        add("North", p1);
        add("Center", p2);
        add("South", okP);

        //JPanel p3 = new JPanel();
        //p3.setLayout(new FlowLayout(FlowLayout.CENTER));
        //p3.add(load);
        load.addActionListener(this);
        load.setEnabled(false);

        //add("South", p3);
        ok.addActionListener(this);

        setVisible(true);
        validate();
        doLayout();
    }

    public void actionPerformed(ActionEvent e)
    {
        if((e.getSource() == ok) || e.getSource() instanceof DirEntry)
        {
            doSearch = true;
            runner = new Thread(this);
            runner.start();
        }
        else if(e.getSource() == load)
        {
            doSearch = false;
            runner = new Thread(this);
            runner.start();
        }
    }

    public void run()
    {
        if(doSearch)
        {
            search.setEnabled(false);

            String url = host.getText().trim() + search.getText().trim();
            search(url);
            load.setEnabled(true);
            search.setEnabled(true);
        }
        else
        {
            download((String) fileNames.elementAt(list.getSelectedIndex()));
        }
    }

    private void download(String url)
    {
        try
        {
            Log.debug("Insomniac: Download started: " + url);

            String host = url.substring(6);
            host = host.substring(0, host.indexOf("/"));
            Log.debug("Insomniac: Trying to connect to remote host: " + host);
            JFtp.statusP.jftp.ensureLogging();

            SmbConnection con = new SmbConnection(host, user.getText().trim(),
                                                  pass.getText().trim(),
                                                  "WORKGROUP",
                                                  ((ConnectionListener) JFtp.remoteDir));
            Log.debug("Insomniac: Connected, downloading to: " +
                      Settings.defaultWorkDir);
            JFtp.statusP.jftp.ensureLogging();
            con.setLocalPath(dir.getText().trim());

            if(con.download(url) >= 0)
            {
                Log.debug("Insomniac: Finished download.");
            }
            else
            {
                Log.debug("Insomniac: Download failed.");
            }

            JFtp.statusP.jftp.ensureLogging();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.debug("Insomniac: Error: " + ex);
        }
    }

    private void search(String url)
    {
        files = new Vector();
        sizes = new Vector();
        fileNames = new Vector();

        try
        {
            URL u = new URL(url);
            DataInputStream in = new DataInputStream(u.openStream());

            String tmp = null;

            while(true)
            {
                tmp = in.readLine();

                if(tmp == null)
                {
                    return;
                }
                else
                {
                    if(tmp.indexOf("X-File") >= 0)
                    {
                        tmp = parse(tmp);
                        fileNames.add(tmp);

                        DirEntry entry = new DirEntry(tmp.substring(tmp.lastIndexOf("/") +
                                                                    1), this);
                        entry.setFile();

                        tmp = in.readLine();

                        if(tmp.indexOf("X-Size") >= 0)
                        {
                            tmp = parse(tmp);
                            entry.setFileSize(Long.parseLong(tmp));
                        }
                        else if(tmp == null)
                        {
                            return;
                        }

                        files.add(entry);

                        list.setListData(files);
                    }
                }
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            Log.debug("Insomniac: Error: " + ex);
        }
    }

    private String parse(String tmp)
    {
        if(tmp == null)
        {
            return "-1";
        }

        tmp.trim();
        tmp = tmp.substring(8);
        tmp = tmp.substring(0, tmp.length() - 1);

        return tmp;
    }

    public Insets getInsets()
    {
        Insets in = super.getInsets();

        return new Insets(in.top + 5, in.left + 5, in.bottom + 5, in.right + 5);
    }
}
