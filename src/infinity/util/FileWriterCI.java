package infinity.util;

import java.io.*;

public class FileWriterCI extends FileWriter {
  public FileWriterCI (String s) throws IOException {
    super(s.toLowerCase());
  }

  public FileWriterCI (File f) throws IOException {
    super(f);
  }
}
