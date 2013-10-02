// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.util.NIFile;
import infinity.util.StreamDiscarder;
import infinity.util.FileCI;

import java.io.File;
import java.io.IOException;

/**
 * @author Fredrik Lindgren <lindgren.fredrik@gmail.com>
 * @since  2012-08-20
 */
@Deprecated
public class AudioConverter
{
  public static final String ACM2WAV = "acm2wav.exe";
  public static final String ACMTOOL = "acmtool";
  private File acm2wav;
  private String decoder;

  private static final StreamDiscarder outReader = new StreamDiscarder();
  private static final StreamDiscarder errReader = new StreamDiscarder();
  private static final Thread outThread = new Thread(outReader);
  private static final Thread errThread = new Thread(errReader);

  /**
   * Retrieves the selected converter from the gui and binds it to an instance variable
   *
   * If the converter is acm2wav, the existence of the file acm2wav.exe is tested for in the root game directory and in the
   * current working directory. If the file exists it is bound to the decoder instance variable. If the converter is acmtool,
   * that is bound to the decoder variable instead.
   */
  public AudioConverter()
  {
    String d = BrowserMenuBar.getInstance().getAudioDecoder();
    if (d.equalsIgnoreCase(ACM2WAV)) {
      if (acm2wav == null || !acm2wav.exists())
        acm2wav = NIFile.getFile(ResourceFactory.getRootDirs(), ACM2WAV);
      if (!acm2wav.exists())
        acm2wav = new FileCI(ACM2WAV);
      if (acm2wav.exists())
        decoder = ACM2WAV;
    }
    else if (d.equalsIgnoreCase(ACMTOOL)) {
      decoder = ACMTOOL;
    }
  }

  /**
   * Invokes the external audio converter
   *
   * @param  acmfile the acm file to be converted
   * @param  wavfile the output file of the conversion
   * @param  isMono  whether the converter should force mono sound
   */
  public void convert(File acmfile, File wavfile, boolean isMono) throws IOException
  {
    try {
      ProcessBuilder pb;
      if (decoder.equalsIgnoreCase(ACM2WAV)) {
        // acm2wav infile (-m) -> outfile in CWD
        pb = new ProcessBuilder(acm2wav.getAbsolutePath(), acmfile.getPath());
        if (isMono) {
          appendArgument(pb, "-m");
        }
        pb.directory(wavfile.getAbsoluteFile().getParentFile());
      }
      else if (decoder.equalsIgnoreCase(ACMTOOL)) {
        // acmtool -d -o outfile (-m) infile
        pb = new ProcessBuilder("acmtool", "-d", "-o" + wavfile.getPath());
        if (isMono) {
          appendArgument(pb, "-m");
        }
        appendArgument(pb, acmfile.getPath());
      }
      else
        return;
      Process p = pb.start();
      outReader.setStream(p.getInputStream());
      errReader.setStream(p.getErrorStream());
      outThread.run();
      errThread.run();
      p.waitFor();
    } catch (InterruptedException e) {}
  }

  /**
   * Appends an argument to a ProcessBuilder's list of arguments
   *
   * @param pb the ProcessBuilder whose command the argument should be appended to
   * @param argument the argument that should be appended
   */
  private void appendArgument(ProcessBuilder pb, String argument)
  {
    java.util.List<String> command = pb.command();
    command.add(argument);
    pb.command(command);
  }

  /**
   * Tests whether a converter is available
   *
   * @return true if the converter is acm2wav and the file acm2wav.exe exists; true if the converter is acmtool; otherwise false
   */
  public boolean converterExists()
  {
    if (decoder == null)
      return false;
    if (decoder.equalsIgnoreCase(ACM2WAV))
      return acm2wav.exists();
    return true;
  }
}
