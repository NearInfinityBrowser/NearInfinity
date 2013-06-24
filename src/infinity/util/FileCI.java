package infinity.util;

import java.io.*;

public class FileCI extends File {
  public FileCI (String s) {
    super(s.toLowerCase());
  }

  public FileCI (String s1, String s2) {
    super(s1.toLowerCase(),s2.toLowerCase());
  }

  public FileCI (File f, String s) {
    super(f,s.toLowerCase());
  }
}
