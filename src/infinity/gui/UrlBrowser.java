package infinity.gui;

import java.awt.Desktop;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JOptionPane;

import infinity.NearInfinity;

/**
 * Browses for the provided URI on mouse clicks
 *
 * @author Fredrik Lindgren
 */
class UrlBrowser implements MouseListener
{
  private final URI url;

  UrlBrowser(String urlText)
  {
    url = URI.create(urlText);
  }

  private void showErrorMessage()
  {
    final String errorMessage = "I can't open an url on this system";
    final String errorTitle = "Attention";
    JOptionPane.showMessageDialog(NearInfinity.getInstance(), errorMessage,
                                  errorTitle, JOptionPane.PLAIN_MESSAGE);
  }

// --------------------- Begin Interface MouseListener ---------------------

  public void mouseClicked(MouseEvent event)
  {

    if (!Desktop.isDesktopSupported()) {
      showErrorMessage();
    } else {
      try {
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
          showErrorMessage();
        } else {
          desktop.browse(url);
        }
      } catch (java.io.IOException e) {
        e.printStackTrace();
      }
    }
  }

  public void mousePressed(MouseEvent e)
  {
  }

  public void mouseReleased(MouseEvent e)
  {
  }

  public void mouseEntered(MouseEvent e)
  {
  }

  public void mouseExited(MouseEvent e)
  {
  }

// --------------------- End Interface MouseListener ---------------------

}
