// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.util.FileCI;
import infinity.util.FileInputStreamCI;
import infinity.util.Filereader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class FileResourceEntry extends ResourceEntry
{
  private final boolean override;
  private File file;

  public FileResourceEntry(File file)
  {
    this(file, false);
  }

  public FileResourceEntry(File file, boolean override)
  {
    this.file = file;
    this.override = override;
  }

  @Override
  public String toString()
  {
    return file.getName().toUpperCase();
  }

  public void deleteFile()
  {
    file.delete();
  }

  @Override
  public File getActualFile(boolean ignoreoverride)
  {
    return file;
  }

  @Override
  public String getExtension()
  {
    return file.getName().substring(file.getName().lastIndexOf(".") + 1).toUpperCase();
  }

  @Override
  public byte[] getResourceData(boolean ignoreoverride) throws IOException
  {
    InputStream is = new BufferedInputStream(new FileInputStreamCI(file));
    byte buffer[] = Filereader.readBytes(is, (int)file.length());
    is.close();
    return buffer;
  }

  @Override
  public InputStream getResourceDataAsStream(boolean ignoreoverride) throws IOException
  {
    return new BufferedInputStream(new FileInputStreamCI(file));
  }

  @Override
  public int[] getResourceInfo(boolean ignoreoverride)
  {
    return getLocalFileInfo(file);
  }

  @Override
  public String getResourceName()
  {
    return file.getName();
  }

  @Override
  public String getTreeFolder()
  {
    if (BrowserMenuBar.getInstance() != null &&
        BrowserMenuBar.getInstance().getOverrideMode() == BrowserMenuBar.OVERRIDE_IN_THREE &&
        hasOverride() &&
        ResourceFactory.getKeyfile().getExtensionType(getExtension()) != -1)
      return getExtension();
    else {
      if (hasOverride())
        return ResourceFactory.OVERRIDEFOLDER;
      return file.getParentFile().getName();
    }
  }

  @Override
  public boolean hasOverride()
  {
    return override;
  }

  public void renameFile(String newname)
  {
    File newFile = new FileCI(file.getParentFile(), newname);
    file.renameTo(newFile);
    file = newFile;
  }
}

