// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.NearInfinity;
import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.resource.cre.CreResource;
import infinity.resource.itm.ItmResource;
import infinity.resource.spl.SplResource;
import infinity.resource.sto.StoResource;
import infinity.util.Byteconvert;
import infinity.util.Filereader;

import javax.swing.*;
import java.io.*;

public abstract class ResourceEntry implements Comparable<ResourceEntry>
{
  private String searchString;

  static int[] getLocalFileInfo(File file)
  {
    if (file.getName().toUpperCase().endsWith(".TIS")) {
      try {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        byte data[] = Filereader.readBytes(is, 24);
        is.close();
        if (!new String(data, 0, 4).equalsIgnoreCase("TIS ")) {
          int tilesize = 64 * 64 + 4 * 256;
          int tilecount = (int)file.length() / tilesize;
          return new int[]{tilecount, tilesize};
        }
        return new int[]{Byteconvert.convertInt(data, 8), Byteconvert.convertInt(data, 12)};
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
    return new int[]{(int)file.length()};
  }

// --------------------- Begin Interface Comparable ---------------------

  public int compareTo(ResourceEntry entry)
  {
    return getResourceName().compareToIgnoreCase(entry.getResourceName());
  }

// --------------------- End Interface Comparable ---------------------

  public File getActualFile()
  {
    return getActualFile(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public ImageIcon getIcon()
  {
    return ResourceFactory.getKeyfile().getIcon(getExtension());
  }

  public byte[] getResourceData() throws Exception
  {
    return getResourceData(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public InputStream getResourceDataAsStream() throws Exception
  {
    return getResourceDataAsStream(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public int[] getResourceInfo() throws Exception
  {
    return getResourceInfo(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public String getSearchString()
  {
    if (searchString == null) {
      try {
        String extension = getExtension();
        if (extension.equalsIgnoreCase("CRE"))
          searchString = CreResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("ITM"))
          searchString = ItmResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("SPL"))
          searchString = SplResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("STO"))
          searchString = StoResource.getSearchString(getResourceData());
      } catch (Exception e) {
        if (NearInfinity.getInstance() != null && !BrowserMenuBar.getInstance().ignoreReadErrors())
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error reading " + toString(), "Error",
                                        JOptionPane.ERROR_MESSAGE);
        searchString = "Error";
        e.printStackTrace();
      }
    }
    return searchString;
  }

  protected abstract File getActualFile(boolean ignoreoverride);

  public abstract String getExtension();

  public abstract byte[] getResourceData(boolean ignoreoverride) throws Exception;

  protected abstract InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception;

  public abstract int[] getResourceInfo(boolean ignoreoverride) throws Exception;

  public abstract String getResourceName();

  public abstract String getTreeFolder();

  public abstract boolean hasOverride();
}