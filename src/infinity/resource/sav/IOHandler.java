// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sav;

import infinity.datatype.TextString;
import infinity.resource.Profile;
import infinity.resource.Writeable;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.io.FileNI;
import infinity.util.io.FileOutputStreamNI;
import infinity.util.io.FileWriterNI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IOHandler implements Writeable
{
  private final ResourceEntry entry;
  private final TextString header;
  private File tempfolder;
  private List<SavResourceEntry> fileentries;

  public IOHandler(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte buffer[] = entry.getResourceData(true); // ignoreOverride - no real effect
    header = new TextString(buffer, 0, 8, null);
    if (!header.toString().equalsIgnoreCase("SAV V1.0"))
      throw new Exception("Unsupported version: " + header);
    fileentries = new ArrayList<SavResourceEntry>();
    int offset = 8;
    while (offset < buffer.length) {
      SavResourceEntry fileentry = new SavResourceEntry(buffer, offset);
      fileentries.add(fileentry);
      offset = fileentry.getEndOffset();
    }
    Collections.sort(fileentries);
  }

// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    header.write(os);
    for (int i = 0; i < fileentries.size(); i++)
      fileentries.get(i).write(os);
  }

// --------------------- End Interface Writeable ---------------------

  public void close()
  {
    if (tempfolder == null)
      return;
    File files[] = tempfolder.listFiles();
    for (final File file : files)
      file.delete();
    tempfolder.delete();
    tempfolder = null;
  }

  public void compress(List<? extends ResourceEntry> entries) throws Exception
  {
    fileentries.clear();
    for (int i = 0; i < entries.size(); i++)
      fileentries.add(new SavResourceEntry(entries.get(i)));
    close();
  }

  public List<ResourceEntry> decompress() throws Exception
  {
    List<ResourceEntry> entries = new ArrayList<ResourceEntry>(fileentries.size());
    tempfolder = createTempFolder();
    if (tempfolder == null)
      throw new Exception("Unable to create temp folder");
    tempfolder.mkdir();
    for (int i = 0; i < fileentries.size(); i++) {
      SavResourceEntry fentry = fileentries.get(i);
      File file = new FileNI(tempfolder, fentry.toString());
      OutputStream os = new BufferedOutputStream(new FileOutputStreamNI(file));
      FileWriterNI.writeBytes(os, fentry.decompress());
      os.close();
      entries.add(new FileResourceEntry(file));
    }
    return entries;
  }

  public List<? extends ResourceEntry> getFileEntries()
  {
    return fileentries;
  }

  public File getTempFolder()
  {
    return tempfolder;
  }

  // Create a unique temp folder for current baldur.sav
  private File createTempFolder()
  {
    for (int idx = 0; idx < Integer.MAX_VALUE; idx++) {
      File f = new FileNI(Profile.getHomeRoot(), String.format("%1$s.%2$03d", entry.getTreeFolder(), idx));
      if (!f.exists()) {
        return f;
      }
    }
    return null;
  }
}

