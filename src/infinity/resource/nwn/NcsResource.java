// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn;

import infinity.gui.BrowserMenuBar;
import infinity.resource.Resource;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.util.IntegerHashMap;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class NcsResource implements Resource
{
  private static final IntegerHashMap<String> ARG_TYPES = new IntegerHashMap<String>();
  private final List<NcsByteCode> byteCodes = new ArrayList<NcsByteCode>();
  private final ResourceEntry entry;

  static {
    ARG_TYPES.put(0x03, "I");
    ARG_TYPES.put(0x04, "F");
    ARG_TYPES.put(0x05, "S");
    ARG_TYPES.put(0x06, "O");
    ARG_TYPES.put(0x10, "Effect");
    ARG_TYPES.put(0x11, "Event");
    ARG_TYPES.put(0x12, "Location");
    ARG_TYPES.put(0x13, "Talen");
    ARG_TYPES.put(0x20, "II");
    ARG_TYPES.put(0x21, "FF");
    ARG_TYPES.put(0x22, "OO");
    ARG_TYPES.put(0x23, "SS");
    ARG_TYPES.put(0x24, "TT");
    ARG_TYPES.put(0x25, "IF");
    ARG_TYPES.put(0x26, "FI");
    ARG_TYPES.put(0x30, "Effect,Effect");
    ARG_TYPES.put(0x31, "Event,Event");
    ARG_TYPES.put(0x32, "Location,Location");
    ARG_TYPES.put(0x33, "Talent,Talen");
    ARG_TYPES.put(0x3A, "VV");
    ARG_TYPES.put(0x3B, "VF");
    ARG_TYPES.put(0x3C, "FV");
  }

  private static int convertBigInt(byte buffer[], int offset)
  {
    int value = 0;
    for (int i = 0; i < 4; i++)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return value;
  }

  private static short convertBigShort(byte buffer[], int offset)
  {
    int value = 0;
    for (int i = 0; i < 2; i++)
      value = value << 8 | buffer[offset + i] & 0xFF;
    return (short)value;
  }

  public NcsResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte buffer[] = entry.getResourceData();

    String fileType = new String(buffer, 0, 4);
    String fileVersion = new String(buffer, 4, 4);

    int offset = 8;
    while (offset < buffer.length) {
      NcsByteCode byteCode = new NcsByteCode(buffer, offset);
      offset += byteCode.length;
      byteCodes.add(byteCode);
    }
  }

// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < byteCodes.size(); i++)
      sb.append(byteCodes.get(i).toString()).append('\n');

    JTextArea ta = new JTextArea(sb.toString());
    ta.setEditable(false);
    ta.setFont(BrowserMenuBar.getInstance().getScriptFont());
    return new JScrollPane(ta);
  }

// --------------------- End Interface Viewable ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class NcsByteCode
  {
    int length;
    final String code;

    private NcsByteCode(byte buffer[], int offset)
    {
      switch (buffer[offset]) {
        case 0x01:
          code = "CPDOWNSP Offset:" + convertBigInt(buffer, offset + 2) +
                 " Size:" + convertBigShort(buffer, offset + 6);
          length = 8;
          break;
        case 0x02:
          code = "RSADD" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x03:
          code = "CPTOPSP Offset:" + convertBigInt(buffer, offset + 2) +
                 " Size:" + convertBigShort(buffer, offset + 6);
          length = 8;
          break;
        case 0x04:
          if (buffer[offset + 1] == 0x03) {
            code = "CONSTI Integer:" + convertBigInt(buffer, offset + 2);
            length = 6;
          }
          else if (buffer[offset + 1] == 0x04) {
            code = "CONSTF Float:" + Float.intBitsToFloat(convertBigInt(buffer, offset + 2));
            length = 6;
          }
          else if (buffer[offset + 1] == 0x05) {
            int slen = convertBigShort(buffer, offset + 2);
            code = "CONSTS String:" + new String(buffer, offset + 4, slen);
            length = 4 + slen;
          }
          else if (buffer[offset + 1] == 0x06) {
            code = "CONSTO Object:" + convertBigInt(buffer, offset + 2);
            length = 6;
          }
          else
            code = "CONST Unknown";
          break;
        case 0x05:
          code = "ACTION Routine#:" + convertBigShort(buffer, offset + 2) +
                 " #arg:" + buffer[offset + 4];
          length = 5;
          break;
        case 0x06:
          code = "LOGANDII";
          length = 2;
          break;
        case 0x07:
          code = "LOGORII";
          length = 2;
          break;
        case 0x08:
          code = "INCORII";
          length = 2;
          break;
        case 0x09:
          code = "EXCORII";
          length = 2;
          break;
        case 0x0A:
          code = "BOOLANDII";
          length = 2;
          break;
        case 0x0B:
          code = "EQUAL" + ARG_TYPES.get(buffer[offset + 1]) + " Size:" + convertBigShort(buffer, offset + 2);
          length = 2;
          if (buffer[offset + 1] == 0x24)
            length = 4;
          break;
        case 0x0C:
          if (buffer[offset + 1] == 0x24) {
            code = "NEQUALTT Size:" + convertBigShort(buffer, offset + 2);
            length = 4;
          }
          else {
            code = "NEQUAL" + ARG_TYPES.get(buffer[offset + 1]);
            length = 2;
          }
          break;
        case 0x0D:
          code = "GEQ" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x0E:
          code = "GT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x0F:
          code = "LT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x10:
          code = "LEQ" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x11:
          code = "SHLEFT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x12:
          code = "SHRIGHT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x13:
          code = "USHRIGHT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x14:
          code = "ADD" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x15:
          code = "SUB" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x16:
          code = "MUL" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x17:
          code = "DIB" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x18:
          code = "MOD" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x19:
          code = "NEG" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x1A:
          code = "COMP" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x1B:
          code = "MOVSP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x1C:
          code = "STORE_STATEALL Offset:" + buffer[offset + 1];
          length = 2;
          break;
        case 0x1D:
          code = "JMP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x1E:
          code = "JSR Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x1F:
          code = "JZ Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x20:
          code = "RETN";
          length = 2;
          break;
        case 0x21:
          code = "DESTRUCT Size:" + convertBigShort(buffer, offset + 2) +
                 " Offset:" + convertBigShort(buffer, offset + 4) +
                 " Size:" + convertBigShort(buffer, offset + 6);
          length = 8;
          break;
        case 0x22:
          code = "NOT" + ARG_TYPES.get(buffer[offset + 1]);
          length = 2;
          break;
        case 0x23:
          code = "DECISP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x24:
          code = "INCISP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x25:
          code = "JNZ Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x26:
          code = "CPDOWNBP Offset:" + convertBigInt(buffer, offset + 2) +
                 " Size:" + convertBigShort(buffer, offset + 6);
          length = 8;
          break;
        case 0x27:
          code = "CPTOPBP Offset:" + convertBigInt(buffer, offset + 2) +
                 " Size:" + convertBigShort(buffer, offset + 6);
          length = 8;
          break;
        case 0x28:
          code = "DECIBP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x29:
          code = "INCIBP Offset:" + convertBigInt(buffer, offset + 2);
          length = 6;
          break;
        case 0x2A:
          code = "SAVEBP";
          length = 2;
          break;
        case 0x2B:
          code = "RESTOREBP";
          length = 2;
          break;
        case 0x2C:
          code = "STORE_STATE Size:" + convertBigInt(buffer, offset + 2) +
                 " Size:" + convertBigInt(buffer, offset + 6);
          length = 10;
          break;
        case 0x2D:
          code = "NOP";
          length = 2;
          break;
        case 0x42:
          code = "T Size:" + convertBigInt(buffer, offset + 1);
          length = 5;
          break;
        default:
          code = "Unknown byte code = " + Integer.toHexString(buffer[offset]);
          length = 2; // Probably wrong
      }
    }

    public String toString()
    {
      return code;
    }
  }
}

