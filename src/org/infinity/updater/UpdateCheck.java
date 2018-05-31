// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.updater;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.text.html.HTMLDocument;

import org.infinity.NearInfinity;
import org.infinity.gui.LinkButton;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;

/**
 * Shows information about available updates and providing options how to deal with them.
 */
public class UpdateCheck extends JDialog
{
  public enum UpdateAction { UPDATE, DOWNLOAD, CANCEL }

  private final JTextField tfCurrentVersion = new JTextField();
  private final JTextField tfCurrentPath = new JTextField();
  private final JTextField tfNewVersion = new JTextField();
  private final JTextField tfNewDate = new JTextField();
  private final JTextField tfNewSize = new JTextField();
  private final JButton bDownload = new JButton("Manual download");
  private final JButton bCancel = new JButton("Cancel");
  private final Listeners listeners = new Listeners();
  private final UpdateInfo updateInfo;

  private UpdateAction retVal = UpdateAction.CANCEL;

  /** Shows update check dialog and returns the action selected by the user. */
  public static UpdateAction showDialog(Window owner, UpdateInfo updateInfo)
  {
    UpdateCheck dlg = null;
    try {
      try {
        dlg = new UpdateCheck(owner, updateInfo);
        dlg.setVisible(true);
        return dlg.retVal;
      } catch (NullPointerException e) {
        JOptionPane.showMessageDialog(owner, "No updates available.", "Check for updates",
                                      JOptionPane.INFORMATION_MESSAGE);
      }
    } finally {
      dlg = null;
    }
    return UpdateAction.CANCEL;
  }


  private UpdateCheck(Window owner, UpdateInfo updateInfo)
  {
    super(owner, "New update available", Dialog.ModalityType.APPLICATION_MODAL);
    if (updateInfo == null) {
      throw new NullPointerException();
    }
    this.updateInfo = updateInfo;
    init();
  }

  private Listeners getListeners() { return listeners; }

  // Returns the currently assigned Configuration object
  private UpdateInfo getUpdateInfo() { return updateInfo; }

  private void init()
  {
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setResizable(true);
    setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();

    // ESC closes dialog
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "ESCAPE");
    getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        cancel();
      }
    });

    // configuring controls
    JLabel lCurrentVersion = new JLabel("Current version:");
    lCurrentVersion.setFont(lCurrentVersion.getFont().deriveFont(Font.PLAIN));
    tfCurrentVersion.setEditable(false);
    tfCurrentVersion.setFont(lCurrentVersion.getFont());
    tfCurrentVersion.setText(NearInfinity.getVersion());

    JLabel lCurrentPath = new JLabel("Current path:");
    lCurrentPath.setFont(lCurrentPath.getFont().deriveFont(Font.PLAIN));
    tfCurrentPath.setEditable(false);
    tfCurrentPath.setFont(lCurrentPath.getFont());
    tfCurrentPath.setText(Utils.getJarFileName(NearInfinity.class));

    JLabel lNewVersion = new JLabel("New version:");
    lNewVersion.setFont(lNewVersion.getFont().deriveFont(Font.BOLD));
    tfNewVersion.setEditable(false);
    tfNewVersion.setFont(lNewVersion.getFont());
    tfNewVersion.setText(getUpdateInfo().getRelease().getVersion());

    JLabel lNewDate = new JLabel("Release date:");
    lNewDate.setFont(lNewDate.getFont().deriveFont(Font.BOLD));
    tfNewDate.setEditable(false);
    tfNewDate.setFont(lNewDate.getFont());
    tfNewDate.setText(getUpdateInfo().getRelease().getTimeStamp().getTime().toString());

    JLabel lNewSize = new JLabel("Download size:");
    lNewSize.setFont(lNewSize.getFont().deriveFont(Font.BOLD));
    tfNewSize.setEditable(false);
    tfNewSize.setFont(lNewDate.getFont());
    int fileSize = -1;
    try {
      WindowBlocker.blockWindow(NearInfinity.getInstance(), true);
      try {
        fileSize = Utils.getFileSizeUrl(new URL(getUpdateInfo().getRelease().getLink()),
                                        Updater.getInstance().getProxy());
      } catch (Exception e) {
      }
    } finally {
      WindowBlocker.blockWindow(NearInfinity.getInstance(), false);
    }
    if (fileSize > 0) {
      tfNewSize.setText(String.format("%.2f MB", (float)fileSize / 1048576.0f));
    } else {
      tfNewSize.setText("n/a");
    }

    bDownload.addActionListener(getListeners());
    bDownload.setEnabled(getUpdateInfo().getRelease().getDownloadLink() != null);

    bCancel.addActionListener(getListeners());

    // creating version overview section
    JPanel pOverview = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pOverview.add(lCurrentVersion, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pOverview.add(tfCurrentVersion, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pOverview.add(lCurrentPath, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pOverview.add(tfCurrentPath, gbc);

    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(24, 0, 0, 0), 0, 0);
    pOverview.add(lNewVersion, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(24, 8, 0, 0), 0, 0);
    pOverview.add(tfNewVersion, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pOverview.add(lNewDate, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pOverview.add(tfNewDate, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    pOverview.add(lNewSize, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 0), 0, 0);
    pOverview.add(tfNewSize, gbc);

    // creating related links section
    JPanel pLinks = new JPanel(new GridBagLayout());
    if (getUpdateInfo().getGeneral().getInformationCount() > 0) {
      pLinks.setBorder(BorderFactory.createTitledBorder("Related links: "));
      for (int i = 0, count = getUpdateInfo().getGeneral().getInformationCount(); i < count; i++) {
        String text = getUpdateInfo().getGeneral().getInformationName(i);
        String url = getUpdateInfo().getGeneral().getInformationLink(i);
        LinkButton lb = new LinkButton(text, url);
        lb.setToolTipText(url);
        lb.setFont(lb.getFont().deriveFont(lb.getFont().getSize2D() + 1.0f));
        gbc = ViewerUtil.setGBC(gbc, 0, i, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 8, 8),
                                0, 0);
        pLinks.add(lb, gbc);
      }
    } else {
      pLinks.setVisible(false);
    }

    // creating changelog section
    JPanel pChangelog = null;
    if (getUpdateInfo().getRelease().hasChangeLog()) {
      pChangelog = new JPanel(new GridBagLayout());

      JTextPane viewer = new JTextPane(new HTMLDocument());
      viewer.setEditable(false);
      viewer.setBackground(getBackground());
      viewer.setContentType("text/html");
      StringBuilder sb = new StringBuilder();
      sb.append("<html><head>");
      sb.append("<style type=\"text/css\">\n");
      sb.append("h1 { margin: 5px 5px 0px 5px; padding: 0px; font-family: Verdana,Arial,sans-serif; font-size: medium; }");
      sb.append("ul { margin: 5px 25px; font-family: Verdana,Arial,sans-serif; font-size: small; }");
      sb.append("li { padding-top: 5px; }");
      sb.append("</style></head>");
      sb.append("<body>");
      sb.append("<h1>").append("Near Infinity ").append(getUpdateInfo().getRelease().getVersion()).append(":</h1>");
      sb.append("<ul>");
      for (Iterator<String> iter = getUpdateInfo().getRelease().getChangelog().iterator(); iter.hasNext();) {
        sb.append("<li>").append(iter.next()).append("</li>");
      }
      sb.append("</ul>");
      sb.append("</body></html>");
      viewer.setText(sb.toString());
      viewer.setCaretPosition(0);

      // prevent viewer to screw up dialog dimensions
      viewer.setPreferredSize(new Dimension(100, 50));
      viewer.setSize(viewer.getPreferredSize());
      JScrollPane sp = new JScrollPane(viewer);

      gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                              GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      pChangelog.add(sp, gbc);
    }

    // putting info sections into tabbed pane
    JPanel pInfo = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pInfo.add(pOverview, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 8), 0, 0);
    pInfo.add(pLinks, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pInfo.add(new JPanel(), gbc);


    JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.addTab("Overview", pInfo);
    if (pChangelog != null) {
      tabPane.addTab("Changelog", pChangelog);
    } else {
      tabPane.addTab("Changelog", new JPanel());
      tabPane.setEnabledAt(1, false);
    }

    // creating dialog button section
    JPanel pButtons = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bDownload, gbc);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bCancel, gbc);
    gbc = ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(new JPanel(), gbc);

    // putting all together
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    add(tabPane, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    add(pButtons, gbc);

    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getOwner());
  }

  // Opens download link in default browser
  private void download()
  {
    boolean bRet = false;
    try {
      String link = getUpdateInfo().getRelease().getDownloadLink();
      if (!Utils.isUrlValid(link)) {
        link = getUpdateInfo().getRelease().getLink();
        if (!Utils.isUrlValid(link)) {
          String msg = "Download link is not available.\n" +
                       "Please visit the official website to download the latest version of Near Infinity.";
          JOptionPane.showMessageDialog(getOwner(), msg, "Unexpected error", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      bRet = Utils.openWebPage(new URL(link));
    } catch (Exception e) {
      // registering error while opening web page
    }
    if (bRet == false) {
      JOptionPane.showMessageDialog(getOwner(), "Error opening download link in browser.",
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
    retVal = UpdateAction.DOWNLOAD;
    setVisible(false);
  }

  // Closes dialog without triggering actions
  private void cancel()
  {
    retVal = UpdateAction.CANCEL;
    setVisible(false);
  }


//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener
  {
    public Listeners() {}

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == bDownload) {
        download();
      } else if (e.getSource() == bCancel) {
        cancel();
      }
    }
  }
}
