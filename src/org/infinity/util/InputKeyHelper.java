// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.event.KeyEvent;

/**
 * A helper class that identifies and returns the stored but normally inaccessible virtual key codes from
 * {@link KeyEvent} objects.
 *
 * <p><strong>Note:</strong> Functionality is currently only available on Windows platforms.</p>
 */
public class InputKeyHelper {
  /** Left mouse button */
  public static final int VK_LBUTTON              = 0x01;
  /** Right mouse button */
  public static final int VK_RBUTTON              = 0x02;
  /** Control-break processing */
  public static final int VK_CANCEL               = 0x03;
  /** Middle mouse button */
  public static final int VK_MBUTTON              = 0x04;
  /** X1 mouse button */
  public static final int VK_XBUTTON1             = 0x05;
  /** X2 mouse button */
  public static final int VK_XBUTTON2             = 0x06;
  /** BACKSPACE key */
  public static final int VK_BACK                 = 0x08;
  /** TAB key */
  public static final int VK_TAB                  = 0x09;
  /** CLEAR key */
  public static final int VK_CLEAR                = 0x0C;
  /** ENTER key */
  public static final int VK_RETURN               = 0x0D;
  /** SHIFT key */
  public static final int VK_SHIFT                = 0x10;
  /** CTRL key */
  public static final int VK_CONTROL              = 0x11;
  /** ALT key */
  public static final int VK_MENU                 = 0x12;
  /** ALT key */
  public static final int VK_ALT                  = VK_MENU;
  /** PAUSE key */
  public static final int VK_PAUSE                = 0x13;
  /** CAPS LOCK key */
  public static final int VK_CAPITAL              = 0x14;
  /** IME Kana mode */
  public static final int VK_KANA                 = 0x15;
  /** IME Hangul mode */
  public static final int VK_HANGUL               = 0x15;
  /** IME On */
  public static final int VK_IME_ON               = 0x16;
  /** IME Junja mode */
  public static final int VK_JUNJA                = 0x17;
  /** IME final mode */
  public static final int VK_FINAL                = 0x18;
  /** IME Hanja mode */
  public static final int VK_HANJA                = 0x19;
  /** IME Kanji mode */
  public static final int VK_KANJI                = 0x19;
  /** IME Off */
  public static final int VK_IME_OFF              = 0x1A;
  /** ESC key */
  public static final int VK_ESCAPE               = 0x1B;
  /** IME convert */
  public static final int VK_CONVERT              = 0x1C;
  /** IME nonconvert */
  public static final int VK_NONCONVERT           = 0x1D;
  /** IME accept */
  public static final int VK_ACCEPT               = 0x1E;
  /** IME mode change request */
  public static final int VK_MODECHANGE           = 0x1F;
  /** SPACEBAR */
  public static final int VK_SPACE                = 0x20;
  /** PAGE UP key */
  public static final int VK_PRIOR                = 0x21;
  public static final int VK_PAGE_UP              = VK_PRIOR;
  /** PAGE DOWN key */
  public static final int VK_NEXT                 = 0x22;
  public static final int VK_PAGE_DOWN            = VK_NEXT;
  /** END key */
  public static final int VK_END                  = 0x23;
  /** HOME key */
  public static final int VK_HOME                 = 0x24;
  /** LEFT ARROW key */
  public static final int VK_LEFT                 = 0x25;
  /** UP ARROW key */
  public static final int VK_UP                   = 0x26;
  /** RIGHT ARROW key */
  public static final int VK_RIGHT                = 0x27;
  /** DOWN ARROW key */
  public static final int VK_DOWN                 = 0x28;
  /** SELECT key */
  public static final int VK_SELECT               = 0x29;
  /** PRINT key */
  public static final int VK_PRINT                = 0x2A;
  /** EXECUTE key */
  public static final int VK_EXECUTE              = 0x2B;
  /** PRINT SCREEN key */
  public static final int VK_SNAPSHOT             = 0x2C;
  /** INS key */
  public static final int VK_INSERT               = 0x2D;
  /** DEL key */
  public static final int VK_DELETE               = 0x2E;
  /** HELP key */
  public static final int VK_HELP                 = 0x2F;
  /** Left WINDOWS key */
  public static final int VK_LWIN                 = 0x5B;
  /** Right WINDOWS key */
  public static final int VK_RWIN                 = 0x5C;
  /** APPLICATIONS key */
  public static final int VK_APPS                 = 0x5D;
  /** Computer SLEEP key */
  public static final int VK_SLEEP                = 0x5F;
  /** Numeric keypad 0 key */
  public static final int VK_NUMPAD0              = 0x60;
  /** Numeric keypad 1 key */
  public static final int VK_NUMPAD1              = 0x61;
  /** Numeric keypad 2 key */
  public static final int VK_NUMPAD2              = 0x62;
  /** Numeric keypad 3 key */
  public static final int VK_NUMPAD3              = 0x63;
  /** Numeric keypad 4 key */
  public static final int VK_NUMPAD4              = 0x64;
  /** Numeric keypad 5 key */
  public static final int VK_NUMPAD5              = 0x65;
  /** Numeric keypad 6 key */
  public static final int VK_NUMPAD6              = 0x66;
  /** Numeric keypad 7 key */
  public static final int VK_NUMPAD7              = 0x67;
  /** Numeric keypad 8 key */
  public static final int VK_NUMPAD8              = 0x68;
  /** Numeric keypad 9 key */
  public static final int VK_NUMPAD9              = 0x69;
  /** Multiply key */
  public static final int VK_MULTIPLY             = 0x6A;
  /** Add key */
  public static final int VK_ADD                  = 0x6B;
  /** Separator key */
  public static final int VK_SEPARATOR            = 0x6C;
  /** Subtract key */
  public static final int VK_SUBTRACT             = 0x6D;
  /** Decimal key */
  public static final int VK_DECIMAL              = 0x6E;
  /** Divide key */
  public static final int VK_DIVIDE               = 0x6F;
  /** F1 key */
  public static final int VK_F1                   = 0x70;
  /** F2 key */
  public static final int VK_F2                   = 0x71;
  /** F3 key */
  public static final int VK_F3                   = 0x72;
  /** F4 key */
  public static final int VK_F4                   = 0x73;
  /** F5 key */
  public static final int VK_F5                   = 0x74;
  /** F6 key */
  public static final int VK_F6                   = 0x75;
  /** F7 key */
  public static final int VK_F7                   = 0x76;
  /** F8 key */
  public static final int VK_F8                   = 0x77;
  /** F9 key */
  public static final int VK_F9                   = 0x78;
  /** F10 key */
  public static final int VK_F10                  = 0x79;
  /** F11 key */
  public static final int VK_F11                  = 0x7A;
  /** F12 key */
  public static final int VK_F12                  = 0x7B;
  /** F13 key */
  public static final int VK_F13                  = 0x7C;
  /** F14 key */
  public static final int VK_F14                  = 0x7D;
  /** F15 key */
  public static final int VK_F15                  = 0x7E;
  /** F16 key */
  public static final int VK_F16                  = 0x7F;
  /** F17 key */
  public static final int VK_F17                  = 0x80;
  /** F18 key */
  public static final int VK_F18                  = 0x81;
  /** F19 key */
  public static final int VK_F19                  = 0x82;
  /** F20 key */
  public static final int VK_F20                  = 0x83;
  /** F21 key */
  public static final int VK_F21                  = 0x84;
  /** F22 key */
  public static final int VK_F22                  = 0x85;
  /** F23 key */
  public static final int VK_F23                  = 0x86;
  /** F24 key */
  public static final int VK_F24                  = 0x87;
  /** NUM LOCK key */
  public static final int VK_NUMLOCK              = 0x90;
  /** SCROLL LOCK key */
  public static final int VK_SCROLL               = 0x91;
  /** Left SHIFT key */
  public static final int VK_LSHIFT               = 0xA0;
  /** Right SHIFT key */
  public static final int VK_RSHIFT               = 0xA1;
  /** Left CTRL key */
  public static final int VK_LCONTROL             = 0xA2;
  /** Right CTRL key */
  public static final int VK_RCONTROL             = 0xA3;
  /** Left ALT key */
  public static final int VK_LMENU                = 0xA4;
  public static final int VK_LALT                 = VK_LMENU;
  /** Right ALT key */
  public static final int VK_RMENU                = 0xA5;
  public static final int VK_RALT                 = VK_RMENU;
  /** Browser Back key */
  public static final int VK_BROWSER_BACK         = 0xA6;
  /** Browser Forward key */
  public static final int VK_BROWSER_FORWARD      = 0xA7;
  /** Browser Refresh key */
  public static final int VK_BROWSER_REFRESH      = 0xA8;
  /** Browser Stop key */
  public static final int VK_BROWSER_STOP         = 0xA9;
  /** Browser Search key */
  public static final int VK_BROWSER_SEARCH       = 0xAA;
  /** Browser Favorites key*/
  public static final int VK_BROWSER_FAVORITES    = 0xAB;
  /** Browser Start and Home key */
  public static final int VK_BROWSER_HOME         = 0xAC;
  /** Volume Mute key */
  public static final int VK_VOLUME_MUTE          = 0xAD;
  /** Volume Down key */
  public static final int VK_VOLUME_DOWN          = 0xAE;
  /** Volume Up key */
  public static final int VK_VOLUME_UP            = 0xAF;
  /** Next Track key */
  public static final int VK_MEDIA_NEXT_TRACK     = 0xB0;
  /** Previous Track key */
  public static final int VK_MEDIA_PREV_TRACK     = 0xB1;
  /** Stop Media key */
  public static final int VK_MEDIA_STOP           = 0xB2;
  /** Play/Pause Media key */
  public static final int VK_MEDIA_PLAY_PAUSE     = 0xB3;
  /** Start Mail key */
  public static final int VK_LAUNCH_MAIL          = 0xB4;
  /** Select Media key */
  public static final int VK_LAUNCH_MEDIA_SELECT  = 0xB5;
  /** Start Application 1 key */
  public static final int VK_LAUNCH_APP1          = 0xB6;
  /** Start Application 2 key */
  public static final int VK_LAUNCH_APP2          = 0xB7;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code ;:} key */
  public static final int VK_OEM_1                = 0xBA;
  /** For any country/region, the {@code +} key */
  public static final int VK_OEM_PLUS             = 0xBB;
  /** For any country/region, the {@code ,} key */
  public static final int VK_OEM_COMMA            = 0xBC;
  /** For any country/region, the {@code -} key */
  public static final int VK_OEM_MINUS            = 0xBD;
  /** For any country/region, the {@code .} key */
  public static final int VK_OEM_PERIOD           = 0xBE;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code /?} key */
  public static final int VK_OEM_2                = 0xBF;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code `~} key */
  public static final int VK_OEM_3                = 0xC0;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code [{} key */
  public static final int VK_OEM_4                = 0xDB;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code \} key */
  public static final int VK_OEM_5                = 0xDC;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code ]}} key */
  public static final int VK_OEM_6                = 0xDD;
  /** Used for miscellaneous characters; it can vary by keyboard. For the US standard keyboard, the {@code '"} key */
  public static final int VK_OEM_7                = 0xDE;
  /** Used for miscellaneous characters; it can vary by keyboard. */
  public static final int VK_OEM_8                = 0xDF;
  /** The {@code <>} keys on the US standard keyboard, or the {@code \|} key on the non-US 102-key keyboard */
  public static final int VK_OEM_102              = 0xE2;
  /** IME PROCESS key */
  public static final int VK_PROCESSKEY           = 0xE5;
  /**
   * Used to pass Unicode characters as if they were keystrokes. The VK_PACKET key is the low word of a 32-bit Virtual
   * Key value used for non-keyboard input methods.
   */
  public static final int VK_PACKET               = 0xE7;
  /** Attn key */
  public static final int VK_ATTN                 = 0xF6;
  /** CrSel key */
  public static final int VK_CRSEL                = 0xF7;
  /** ExSel key */
  public static final int VK_EXSEL                = 0xF8;
  /** Erase EOF key */
  public static final int VK_EREOF                = 0xF9;
  /** Play key */
  public static final int VK_PLAY                 = 0xFA;
  /** Zoom key */
  public static final int VK_ZOOM                 = 0xFB;
  /** Reserved */
  public static final int VK_NONAME               = 0xFC;
  /** PA1 key */
  public static final int VK_PA1                  = 0xFD;
  /** Clear key */
  public static final int VK_OEM_CLEAR            = 0xFE;

  // TODO: check if adding Linux or macOS support is possible
  private static boolean enabled = Platform.IS_WINDOWS;

  /** Returns whether raw keycode support is enabled on this platform. */
  public static boolean isEnabled() {
    return enabled;
  }

  /** Overrides the autodetected raw keycode support. */
  public static void setEnabled(boolean b) {
    enabled = b;
  }

  /** Restores autodetected raw keycode support for this platform. */
  public static void setAutodetect() {
    enabled = Platform.IS_WINDOWS;
  }

  /**
   * Returns the virtual key code from the specified KeyEvent object.
   *
   * @param e {@link KeyEvent} object with the code to extract.
   * @return The extracted virtual key code. Returns 0 if the code could not be determined.
   */
  public static int getExtendedKeyCode(KeyEvent e) {
    int retVal = 0;
    if (e != null && isEnabled()) {
      // extracting value from KeyEvent::toString() output
      final String value = getValue(e.toString(), "rawCode");
      if (!value.isEmpty()) {
        try {
          retVal = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
          Logger.debug(ex);
        }
      }
    }
    return retVal;
  }

  /** Internal helper method that extracts the value associated with {@code key} in the specified {@code input}. */
  private static String getValue(String input, String key) {
    String retVal = "";
    if (input != null && key != null && !key.isEmpty()) {
      int spos = input.indexOf(key);
      if (spos >= 0) {
        spos += key.length() + 1;
        int epos1 = input.indexOf(',', spos);
        if (epos1 < 0) {
          epos1 = Integer.MAX_VALUE;
        }
        int epos2 = input.indexOf(']', spos);
        if (epos2 < 0) {
          epos2 = Integer.MAX_VALUE;
        }
        int epos = Math.min(epos1, epos2);
        if (epos >= spos && epos < Integer.MAX_VALUE) {
          retVal = input.substring(spos, epos);
        }
      }
    }
    return retVal;
  }
}
