// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

/**
 * A helper class for launching default applications associated with the specified file or URL arguments.
 */
public class LauncherUtils {
  /**
   * Launches the default browser to display the specified URI. If the default browser is not able to handle the
   * specified URI, the application registered for handling URIs of the specified type is invoked. The applicationis
   * determined from the protocol and path of the URI, asdefined by the URI class.
   *
   * @param url the URL, as string, to be displayed in the user's default browser.
   * @throws URISyntaxException if the specified URL is not formatted strictly according to RFC2396 and cannot be
   *                            converted to a URI.
   * @throws IOException        if the user default browser is not found, or it fails to be launched, or the default
   *                            handler application failed to be launched.
   */
  public static void browse(String url) throws URISyntaxException, IOException {
    browse(new URI(url));
  }

  /**
   * Launches the default browser to display the specified {@link URL}. If the default browser is not able to handle the
   * specified URI, the application registered for handling URIs of the specified type is invoked. The applicationis
   * determined from the protocol and path of the URI, asdefined by the URI class.
   *
   * @param url the {@link URL} to be displayed in the user's default browser.
   * @throws URISyntaxException if the specified URL is not formatted strictly according to RFC2396 and cannot be
   *                            converted to a URI.
   * @throws IOException        if the user default browser is not found, or it fails to be launched, or the default
   *                            handler application failed to be launched.
   */
  public static void browse(URL url) throws URISyntaxException, IOException {
    browse(url.toURI());
  }

  /**
   * Launches the default browser to display the specified {@link URI}. If the default browser is not able to handle the
   * specified URI, the application registered for handling URIs of the specified type is invoked. The applicationis
   * determined from the protocol and path of the URI, asdefined by the URI class.
   *
   * @param url the {@link URI} to be displayed in the user's default browser.
   * @throws URISyntaxException if the specified URL is not formatted strictly according to RFC2396 and cannot be
   *                            converted to a URI.
   * @throws IOException        if the user default browser is not found, or it fails to be launched, or the default
   *                            handler application failed to be launched.
   */
  public static void browse(URI uri) throws IOException {
    try {
      final Desktop desktop = Desktop.getDesktop();
      desktop.browse(uri);
    } catch (UnsupportedOperationException uoe) {
      // fall back to unix-specific method
      if (!xdgOpen(uri.toString())) {
        throw new IOException(uoe);
      }
    }
  }

  /**
   * Launches the associated application to open the specified file.
   *
   * If the specified file is a directory, the file manager of the current platform is launched to open it.
   *
   * @param file the file path, as string, to be opened with the associated application.
   * @throws IOException if the specified file has no associated application or the associated application fails to be
   *                     launched.
   */
  public static void open(String file) throws IOException {
    open(new File(file));
  }

  /**
   * Launches the associated application to open the specified file.
   *
   * If the specified file is a directory, the file manager of the current platform is launched to open it.
   *
   * @param file the file path, as {@link Path}, to be opened with the associated application.
   * @throws IOException if the specified file has no associated application or the associated application fails to be
   *                     launched.
   */
  public static void open(Path file) throws IOException {
    open(file.toFile());
  }

  /**
   * Launches the associated application to open the specified file.
   *
   * If the specified file is a directory, the file manager of the current platform is launched to open it.
   *
   * @param file the file path, as {@link File}, to be opened with the associated application.
   * @throws IOException if the specified file has no associated application or the associated application fails to be
   *                     launched.
   */
  public static void open(File file) throws IOException {
    try {
      final Desktop desktop = Desktop.getDesktop();
      desktop.open(file);
    } catch (UnsupportedOperationException uoe) {
      // fall back to unix-specific method
      if (!xdgOpen(file.getPath())) {
        throw new IOException(uoe);
      }
    }
  }

  /**
   * Attempts to execute the unix-specific {@code xdg-open} command open the specified file path or URL string in the
   * user's preferred application.
   *
   * @param arg File path or URL as string.
   * @return {@code true} if {code xdg-open} was called, {@code false} otherwise.
   * @throws IOException if the process could not be executed.
   */
  private static boolean xdgOpen(String arg) {
    boolean retVal = false;

    String command = null;
    if (Platform.IS_UNIX) {
      command = "xdg-open";
    } else if (Platform.IS_MACOS) {
      command = "open";
    }

    if (command != null) {
      try {
        Process p = new ProcessBuilder(command, arg).start();
        p.waitFor();
        retVal = true;
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }
}
