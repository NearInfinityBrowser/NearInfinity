// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.BrowserMenuBar.OverrideMode;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.io.ByteBufferInputStream;
import org.infinity.util.io.StreamUtils;

/**
 * A ResourceEntry based on {@link ByteBuffer} data.
 */
public class BufferedResourceEntry extends ResourceEntry
{
  private final ByteBuffer buffer;
  private final String fileName;

  public BufferedResourceEntry(ByteBuffer buffer, String fileName)
  {
    this.buffer = Objects.requireNonNull(buffer);
    this.fileName = Objects.requireNonNull(fileName);
  }

  @Override
  protected Path getActualPath(boolean ignoreOverride)
  {
    return Paths.get(Profile.getProperty(Profile.Key.GET_GAME_ROOT_FOLDER),
                     Profile.getProperty(Profile.Key.GET_GLOBAL_OVERRIDE_NAME),
                     getResourceName());
  }

  @Override
  public long getResourceSize(boolean ignoreOverride)
  {
    return buffer.limit();
  }

  @Override
  public String getExtension()
  {
    return getResourceName().substring(getResourceName().lastIndexOf('.') + 1).toUpperCase(Locale.ENGLISH);
  }

  @Override
  public ByteBuffer getResourceBuffer(boolean ignoreOverride) throws Exception
  {
    return buffer;
  }

  @Override
  public InputStream getResourceDataAsStream(boolean ignoreOverride) throws Exception
  {
    return new ByteBufferInputStream(getResourceBuffer());
  }

  @Override
  public int[] getResourceInfo(boolean ignoreOverride) throws Exception
  {
    ByteBuffer buffer = getResourceBuffer();
    String sig = "", ver = "";
    if (getResourceSize() >= 8) {
      sig = StreamUtils.readString(buffer, 0, 4);
      ver = StreamUtils.readString(buffer, 4, 4);
    }
    if ("TIS ".equals(sig) && "V1  ".equals(ver)) {
      if (getResourceSize() > 16) {
        int v1 = buffer.getInt(8);
        int v2 = buffer.getInt(12);
        return new int[] { v1, v2 };
      } else {
        throw new Exception("Unexpected end of file");
      }
    } else {
      return new int[] { (int)getResourceSize() };
    }
  }

  @Override
  public String getResourceName()
  {
    return fileName;
  }

  @Override
  public String getResourceRef()
  {
    String fileName = getResourceName();
    int pos = fileName.lastIndexOf('.');
    if (pos >= 0)
      fileName = fileName.substring(0, pos);
    return fileName;
  }

  @Override
  public String getTreeFolderName()
  {
    if (BrowserMenuBar.getInstance() != null) {
      final OverrideMode mode = BrowserMenuBar.getInstance().getOverrideMode();
      final Keyfile keyfile = ResourceFactory.getKeyfile();

      if (keyfile.getExtensionType(getExtension()) != -1) {
        if (mode == OverrideMode.InTree) {
          return getExtension();
        } else if (mode == OverrideMode.Split &&
                   keyfile.getResourceEntry(getResourceName()) != null) {
          return getExtension();
        }
      }
    }
    return Profile.getOverrideFolderName();
  }

  @Override
  public ResourceTreeFolder getTreeFolder()
  {
    return ResourceFactory.getResourceTreeModel().getFolder(getTreeFolderName());
  }

  @Override
  public boolean hasOverride()
  {
    return false;
  }

}
