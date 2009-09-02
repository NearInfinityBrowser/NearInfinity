// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.resource.Writeable;
import infinity.util.*;

import java.io.*;

public final class BIFFResourceEntry extends ResourceEntry implements Writeable
{
  private final String resourceName;
  private final int type;
  private boolean hasOverride = false;
  private int locator;

  public BIFFResourceEntry(BIFFEntry bifentry, String resourcename, int offset)
  {
    this.resourceName = resourcename;
    type = ResourceFactory.getKeyfile().getExtensionType(resourcename.substring(resourcename.indexOf((int)'.') + 1));
    int bifindex = bifentry.getIndex();
    locator = bifindex << 20;
    if (type == 0x3eb) // TIS
      locator |= offset << 14;
    else
      locator |= offset;
  }

  public BIFFResourceEntry(byte buffer[], int offset, int stringLength)
  {
    StringBuffer sb = new StringBuffer(Byteconvert.convertString(buffer, offset, stringLength));
    type = (int)Byteconvert.convertShort(buffer, offset + stringLength);
    locator = Byteconvert.convertInt(buffer, offset + stringLength + 2);
    resourceName = sb.append('.').append(getExtension()).toString();
  }

// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    Filewriter.writeString(os, resourceName.substring(0, resourceName.lastIndexOf((int)'.')), 8);
    Filewriter.writeShort(os, (short)type);
    Filewriter.writeInt(os, locator);
  }

// --------------------- End Interface Writeable ---------------------

  public boolean equals(Object o)
  {
    if (!(o instanceof BIFFResourceEntry))
      return false;
    BIFFResourceEntry other = (BIFFResourceEntry)o;
    return locator == other.locator && resourceName.equals(other.resourceName) && type == other.type;
  }

  public String toString()
  {
    return resourceName;
  }

  public void deleteOverride()
  {
    File override = new File(ResourceFactory.getRootDir(),
                             ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName);
    if (override != null && override.exists())
      override.delete();
    hasOverride = false;
  }

  public File getActualFile(boolean ignoreoverride)
  {
    if (!ignoreoverride) {
      File override = new File(ResourceFactory.getRootDir(),
                               ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName);
      if (override.exists())
        return override;
    }
    try {
      return ResourceFactory.getKeyfile().getBIFFFile(getBIFFEntry()).getFile();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public BIFFEntry getBIFFEntry()
  {
    int sourceindex = locator >> 20 & 0xfff;
    return ResourceFactory.getKeyfile().getBIFFEntry(sourceindex);
  }

  public String getExtension()
  {
    String ext = ResourceFactory.getKeyfile().getExtension(type);
    if (ext == null)
      return "Unknown (" + Integer.toHexString(type) + "h)";
    return ext;
  }

  public int getLocator()
  {
    return locator;
  }

  public byte[] getResourceData(boolean ignoreoverride) throws Exception
  {
    if (!ignoreoverride) {
      File override = new File(ResourceFactory.getRootDir(),
                               ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName);
      if (override.exists()) {
        InputStream is = new BufferedInputStream(new FileInputStream(override));
        byte buffer[] = Filereader.readBytes(is, (int)override.length());
        is.close();
        return buffer;
      }
    }
    BIFFArchive biff = ResourceFactory.getKeyfile().getBIFFFile(getBIFFEntry());
    if (type == 0x3eb) // TIS
      return biff.getResource(locator >> 14 & 0x3f, true);
    return biff.getResource(locator & 0x3fff, false);
  }

  public InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
  {
    if (!ignoreoverride) {
      File override = new File(ResourceFactory.getRootDir(),
                               ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName);
      if (override.exists()) {
        return new BufferedInputStream(new FileInputStream(override));
      }
    }
    BIFFArchive biff = ResourceFactory.getKeyfile().getBIFFFile(getBIFFEntry());
    if (type == 0x3eb) // TIS
      return biff.getResourceAsStream(locator >> 14 & 0x3f, true);
    return biff.getResourceAsStream(locator & 0x3fff, false);
  }

  public int[] getResourceInfo(boolean ignoreoverride) throws IOException
  {
    if (!ignoreoverride) {
      File override = new File(ResourceFactory.getRootDir(),
                               ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName);
      if (override.exists())
        return getLocalFileInfo(override);
    }
    BIFFArchive biff = ResourceFactory.getKeyfile().getBIFFFile(getBIFFEntry());
    if (type == 0x3eb) // TIS
      return biff.getResourceInfo(locator >> 14 & 0x3f, true);
    return biff.getResourceInfo(locator & 0x3fff, false);
  }

  public String getResourceName()
  {
    return resourceName;
  }

  public String getTreeFolder()
  {
    if (BrowserMenuBar.getInstance() != null &&
        BrowserMenuBar.getInstance().getOverrideMode() == BrowserMenuBar.OVERRIDE_IN_OVERRIDE &&
        hasOverride())
      return ResourceFactory.OVERRIDEFOLDER;
    return getExtension();
  }

  public int getType()
  {
    return type;
  }

  public boolean hasOverride()
  {
    if (!BrowserMenuBar.getInstance().cacheOverride())
      hasOverride = new File(ResourceFactory.getRootDir(),
                             ResourceFactory.OVERRIDEFOLDER + File.separatorChar + resourceName).exists();
    return hasOverride;
  }

  public void setOverride(boolean hasOverride)
  {
    this.hasOverride = hasOverride;
  }

  void adjustSourceIndex(int index)
  {
    int sourceindex = locator >> 20 & 0xfff;
    if (sourceindex > index) {
      sourceindex--;
      locator = sourceindex << 20 | locator & 0xfffff;
    }
  }
}

