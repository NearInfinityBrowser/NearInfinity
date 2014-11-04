package infinity.datatype;

import java.io.IOException;
import java.io.OutputStream;

import infinity.resource.StructEntry;
import infinity.util.IdsMapEntry;

/**
 * Specialized IdsBitmap type for properly handling KIT.IDS in BG and BG2.
 * @author argent77
 */
public class KitIdsBitmap extends IdsBitmap
{

  public KitIdsBitmap(byte buffer[], int offset, String name)
  {
    this(null, buffer, offset, name);
  }

  public KitIdsBitmap(StructEntry parent, byte buffer[], int offset, String name)
  {
    super(parent, buffer, offset, 4, name, "KIT.IDS");
    init();
  }

  public KitIdsBitmap(byte buffer[], int offset, String name, int idsStart)
  {
    this(null, buffer, offset, name, idsStart);
  }

  public KitIdsBitmap(StructEntry parent, byte buffer[], int offset, String name, int idsStart)
  {
    super(parent, buffer, offset, 4, name, "KIT.IDS", idsStart);
    init();
  }

//--------------------- Begin Interface Writeable ---------------------

 @Override
 public void write(OutputStream os) throws IOException
 {
   super.writeLong(os, swapWords(value));
 }

//--------------------- End Interface Writeable ---------------------

  private void init()
  {
    // adding "No Kit" value if needed
    addIdsMapEntry(new IdsMapEntry(0L, "NO_KIT", null));

    // fixing word order of kit id value
    value = swapWords(value);
  }

  // Swaps position of the two lower words
  private static long swapWords(long value)
  {
    return ((value >>> 16) & 0xffffL) | ((value & 0xffffL) << 16);
  }
}
