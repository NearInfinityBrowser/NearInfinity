// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource;

import java.awt.Window;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.EnumMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import infinity.gui.NewChrSettings;
import infinity.gui.NewProSettings;
import infinity.gui.NewResSettings;
import infinity.util.ResourceStructure;

// Create different pre-initialized IE game resources from scratch and writing them to disk.
public final class StructureFactory
{
  // Supported resource types
  public static enum ResType {
    RES_2DA, RES_ARE, RES_BCS, RES_BIO, RES_CHR, RES_CRE, RES_DLG, RES_EFF,
    RES_IDS, RES_INI, RES_ITM, RES_MUS, RES_PRO, RES_RES, RES_SPL, RES_SRC,
    RES_STO, RES_VEF, RES_VVC, RES_WED, RES_WFX, RES_WMAP
  }

  private static final EnumMap<ResType, String> resExt = new EnumMap<ResType, String>(ResType.class);
  private static StructureFactory sfactory;

  static {
    resExt.put(ResType.RES_2DA, "2DA");
    resExt.put(ResType.RES_ARE, "ARE");
    resExt.put(ResType.RES_BCS, "BCS");
    resExt.put(ResType.RES_BIO, "BIO");
    resExt.put(ResType.RES_CHR, "CHR");
    resExt.put(ResType.RES_CRE, "CRE");
    resExt.put(ResType.RES_DLG, "DLG");
    resExt.put(ResType.RES_EFF, "EFF");
    resExt.put(ResType.RES_IDS, "IDS");
    resExt.put(ResType.RES_INI, "INI");
    resExt.put(ResType.RES_ITM, "ITM");
    resExt.put(ResType.RES_MUS, "MUS");
    resExt.put(ResType.RES_PRO, "PRO");
    resExt.put(ResType.RES_RES, "RES");
    resExt.put(ResType.RES_SPL, "SPL");
    resExt.put(ResType.RES_SRC, "SRC");
    resExt.put(ResType.RES_STO, "STO");
    resExt.put(ResType.RES_VEF, "VEF");
    resExt.put(ResType.RES_VVC, "VVC");
    resExt.put(ResType.RES_WED, "WED");
    resExt.put(ResType.RES_WFX, "WFX");
    resExt.put(ResType.RES_WMAP, "WMP");
  };


  public static StructureFactory getFactory()
  {
    if (sfactory == null)
      sfactory = new StructureFactory();
    return sfactory;
  }

  // Write a new resource of specified type to disk
  public void newResource(ResType type, Window parent)
  {
    File savedir = new File(ResourceFactory.getRootDir(), ResourceFactory.OVERRIDEFOLDER);
    if (!savedir.exists())
      savedir = ResourceFactory.getRootDir();
    JFileChooser fc = new JFileChooser(savedir);
    fc.setDialogTitle("Create new " + resExt.get(type) + " resource");
    fc.setFileFilter(new FileNameExtensionFilter(resExt.get(type) + " files", resExt.get(type).toLowerCase()));
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), "UNTITLED." + resExt.get(type)));
    if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
      File output = fc.getSelectedFile();
      boolean fileExists = output.exists();
      if (fileExists) {
        final String options[] = {"Overwrite", "Cancel"};
        if (JOptionPane.showOptionDialog(parent, output + "exists. Overwrite?",
                                         "Create new " + resExt.get(type) + " resource",
                                         JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                         null, options, options[0]) == 1)
          return;
      }
      try {
        try {
          ResourceStructure struct = createStructure(type, output.getName(), parent);
          if (struct != null) {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output));
            if (!struct.isEmpty())
              bos.write(struct.getBytes());
            bos.close();
            JOptionPane.showMessageDialog(parent, "File " + output + " created successfully.",
                                          "Information", JOptionPane.INFORMATION_MESSAGE);
          } else
            unsupported();
        } catch (StructureException e) {
          switch (e.getReason()) {
            case UNSUPPORTED_TYPE:
              JOptionPane.showMessageDialog(parent, "Game doesn't support " + resExt.get(e.getType()) + " format!",
                                            "Error", JOptionPane.ERROR_MESSAGE);
              return;
            case CANCELLED_OPERATION:
              JOptionPane.showMessageDialog(parent, "Operation cancelled!",
                                            "Information", JOptionPane.INFORMATION_MESSAGE);
              return;
            default:
               throw e;
          }
        }
      } catch (Exception e) {
        JOptionPane.showMessageDialog(parent, "Error while creating " + output.getName(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
  }

  // Create new structure of specified type
  public ResourceStructure createStructure(ResType type, String fileName, Window parent) throws StructureException
  {
    // simplifying game id
    int game = ResourceFactory.ID_UNKNOWNGAME;
    switch (ResourceFactory.getGameID()) {
      case ResourceFactory.ID_BG1:
      case ResourceFactory.ID_BG1TOTSC:
        game = ResourceFactory.ID_BG1;
        break;
      case ResourceFactory.ID_TORMENT:
        game = ResourceFactory.ID_TORMENT;
        break;
      case ResourceFactory.ID_ICEWIND:
      case ResourceFactory.ID_ICEWINDHOW:
      case ResourceFactory.ID_ICEWINDHOWTOT:
        game = ResourceFactory.ID_ICEWIND;
        break;
      case ResourceFactory.ID_BG2:
      case ResourceFactory.ID_BG2TOB:
      case ResourceFactory.ID_TUTU:
        game = ResourceFactory.ID_BG2;
        break;
      case ResourceFactory.ID_ICEWIND2:
        game = ResourceFactory.ID_ICEWIND2;
        break;
      default:
        return unsupportedGame();
    }

    switch (type) {
      case RES_2DA:  return create2DA(game);
      case RES_ARE:  return createARE(game, fileName);
      case RES_BCS:  return createBCS(game);
      case RES_BIO:  return createRES(game, parent);
      case RES_CHR:  return createCHR(game, parent);
      case RES_CRE:  return createCRE(game);
      case RES_DLG:  return createDLG(game);
      case RES_EFF:  return createEFF(game);
      case RES_IDS:  return createIDS(game);
      case RES_ITM:  return createITM(game);
      case RES_INI:  return createINI(game);
      case RES_MUS:  return createMUS(game, fileName);
      case RES_PRO:  return createPRO(game, parent);
      case RES_RES:  return createRES(game, parent);
      case RES_SPL:  return createSPL(game);
      case RES_SRC:  return createSRC(game);
      case RES_STO:  return createSTO(game);
      case RES_VEF:  return createVEF(game);
      case RES_VVC:  return createVVC(game);
      case RES_WED:  return createWED(game);
      case RES_WFX:  return createWFX(game);
      case RES_WMAP: return createWMAP(game);
      default:       return createUnknown();
    }
  }

  private StructureFactory()
  {
    super();
  }

  private ResourceStructure create2DA(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_2da = new ResourceStructure();
      final String s = "2DA V1.0\r\n0\r\n        COLUMN1\r\nROW1    0\r\n"; //.replaceAll("\r\n", System.getProperty("line.separator"));
      s_2da.add(ResourceStructure.ID_STRING, s);

      return s_2da;
    } else
       return unsupportedFormat(ResType.RES_2DA);
  }

  private ResourceStructure createARE(int id, String fileName) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_are = new ResourceStructure();

      String fileBase = extractFileBase(fileName);
      if (fileBase.length() > 8)
        fileBase = fileBase.substring(0, 8);

      s_are.add(ResourceStructure.ID_STRING, 4, "AREA");      // Signature
      s_are.add(ResourceStructure.ID_STRING, 4,
          (id == ResourceFactory.ID_ICEWIND2) ? "V9.1" : "V1.0");   // Version
      s_are.add(ResourceStructure.ID_RESREF, fileBase);       // Area WED (replaced with actual WED filename)
      s_are.add(ResourceStructure.ID_ARRAY,
          (id == ResourceFactory.ID_ICEWIND2) ? 84 : 68);     // block of zero
      int ofs = (id == ResourceFactory.ID_ICEWIND2) ? 0x12c : 0x11c;
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Actors offset
      s_are.add(ResourceStructure.ID_DWORD);                  // 2x zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Regions offset
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Spawn points offset
      s_are.add(ResourceStructure.ID_DWORD);                  // zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Entrances offset
      s_are.add(ResourceStructure.ID_DWORD);                  // zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Containers offset
      s_are.add(ResourceStructure.ID_DWORD);                  // 2x zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Items offset
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Vertices offset
      s_are.add(ResourceStructure.ID_DWORD);                  // 2x zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Ambients offset
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Variables offset
      s_are.add(ResourceStructure.ID_ARRAY, 20);              // block of zeros
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Explored bitmask offset
      s_are.add(ResourceStructure.ID_DWORD);                  // zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Doors offset
      s_are.add(ResourceStructure.ID_DWORD);                  // zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Animations offset
      s_are.add(ResourceStructure.ID_DWORD);                  // zero
      s_are.add(ResourceStructure.ID_DWORD, ofs);             // Tiled objects offset
      s_are.add(ResourceStructure.ID_ARRAY, 8);               // block of zeros
      s_are.add(ResourceStructure.ID_DWORD,
          (id == ResourceFactory.ID_TORMENT) ? -1 : 0);       // Automap notes offset (except PST)
      s_are.add(ResourceStructure.ID_DWORD, 0);               // PST: Automap notes offset
      s_are.add(ResourceStructure.ID_ARRAY, 80);              // block of zero

      return s_are;
    } else
      return unsupportedFormat(ResType.RES_ARE);
  }

  private ResourceStructure createBCS(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_bcs = new ResourceStructure();
      final String s = "SC\r\nSC\r\n";
      s_bcs.add(ResourceStructure.ID_STRING, s);

      return s_bcs;
    } else
      return unsupportedFormat(ResType.RES_BCS);
  }

  private ResourceStructure createCHR(int id, Window parent) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      NewChrSettings dlg = new NewChrSettings(parent);
      if (dlg.isAccepted()) {
        String name = dlg.getConfig().getName();
        ResourceStructure s_chr = new ResourceStructure();
        ResourceStructure s_cre = createCRE(id);

        s_chr.add(ResourceStructure.ID_STRING, 4, "CHR ");          // Signature
        if (id == ResourceFactory.ID_ICEWIND2)
          s_chr.add(ResourceStructure.ID_STRING, 4, "V2.2");        // Version
        else if (id == ResourceFactory.ID_BG2)
          s_chr.add(ResourceStructure.ID_STRING, 4, "V2.0");        // Version
        else if (id == ResourceFactory.ID_TORMENT)
          s_chr.add(ResourceStructure.ID_STRING, 4, "V1.2");        // Version
        else
          s_chr.add(ResourceStructure.ID_STRING, 4, "V1.0");        // Version
        s_chr.add(ResourceStructure.ID_STRING, 32, name);           // Name of Protagonist/Player
        if (id == ResourceFactory.ID_ICEWIND2)
          s_chr.add(ResourceStructure.ID_DWORD, 0x0224);            // Offset to CRE structure
        else
          s_chr.add(ResourceStructure.ID_DWORD, 0x0064);            // Offset to CRE structure
        s_chr.add(ResourceStructure.ID_DWORD, s_cre.size());        // Length of the CRE structure
        if (id == ResourceFactory.ID_ICEWIND2)
          s_chr.add(ResourceStructure.ID_ARRAY, 500);               // block of zeros
        else
          s_chr.add(ResourceStructure.ID_ARRAY, 52);                // block of zeros
        s_chr.add(ResourceStructure.ID_ARRAY, s_cre.size(), s_cre.getBytes());    // CRE structure

        return s_chr;
      } else
        return cancelOperation();
    } else
      return unsupportedFormat(ResType.RES_CHR);
  }

  private ResourceStructure createCRE(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      final String[] version = {"V1.0", "V1.2", "V2.2", "V9.0"};
      final int[] ofs = {0x2d4, 0x378, 0, 0x33c};
      final int[] count = {38, 46, 50, 38};
      int idx = (id == ResourceFactory.ID_ICEWIND) ? 3 : (id == ResourceFactory.ID_ICEWIND2) ? 2 :
        (id == ResourceFactory.ID_TORMENT) ? 1 : 0;
      ResourceStructure s_cre = new ResourceStructure();
      s_cre.add(ResourceStructure.ID_STRING, 4, "CRE ");        // Signature
      s_cre.add(ResourceStructure.ID_STRING, 4, version[idx]);  // Version
      s_cre.add(ResourceStructure.ID_STRREF, -1);               // Long name strref
      s_cre.add(ResourceStructure.ID_STRREF, -1);               // Short name strref
      s_cre.add(ResourceStructure.ID_ARRAY, 28);                // block of zeros
      s_cre.add(ResourceStructure.ID_ARRAY, 8,
          new byte[]{30, 37, 57, 12, 23, 28, 0, 1});            // Color indices and EFF structure version
      s_cre.add(ResourceStructure.ID_ARRAY, 112);               // block of zeros
      if (id == ResourceFactory.ID_ICEWIND2)
        s_cre.add(ResourceStructure.ID_ARRAY, 8);               // block of zeros
      for (int i = 0; i < (id == ResourceFactory.ID_ICEWIND2 ? 64 : 100); i++)
        s_cre.add(ResourceStructure.ID_DWORD, -1);              // Char-related strrefs
      if (id == ResourceFactory.ID_ICEWIND2)
        s_cre.add(ResourceStructure.ID_ARRAY, 182);             // block of zeros
      s_cre.add(ResourceStructure.ID_ARRAY, 4,
          new byte[]{0, 0, 0, 1});                              // last byte: Gender
      if (id == ResourceFactory.ID_ICEWIND)
        s_cre.add(ResourceStructure.ID_ARRAY, 172);             // block of zeros
      else if (id == ResourceFactory.ID_ICEWIND2)
        s_cre.add(ResourceStructure.ID_ARRAY, 298);             // block of zeros
      else if (id == ResourceFactory.ID_TORMENT)
        s_cre.add(ResourceStructure.ID_ARRAY, 92);              // block of zeros
      else
        s_cre.add(ResourceStructure.ID_ARRAY, 68);              // block of zeros
      if (id == ResourceFactory.ID_TORMENT) {
        s_cre.add(ResourceStructure.ID_DWORD, 0x3d8);           // Overlays offset
        s_cre.add(ResourceStructure.ID_ARRAY, 136);             // block of zeros
      }
      s_cre.add(ResourceStructure.ID_WORD, -1);                 // Global identifier
      s_cre.add(ResourceStructure.ID_WORD, -1);                 // Local identifier
      s_cre.add(ResourceStructure.ID_ARRAY, 32);                // block of zeros
      if (id == ResourceFactory.ID_ICEWIND2)
        s_cre.add(ResourceStructure.ID_ARRAY, 6);               // block of zeros
      if (id == ResourceFactory.ID_ICEWIND2) {
        for (int i = 0; i < 63; i++)
          s_cre.add(ResourceStructure.ID_DWORD, 0x62e + i*8);   // Spell levels offsets
        s_cre.add(ResourceStructure.ID_ARRAY, 252);             // blocks of zeros
        for (int i = 0; i < 9; i++)
          s_cre.add(ResourceStructure.ID_DWORD, 0x826 + i*8);   // Domain spells offsets
        s_cre.add(ResourceStructure.ID_ARRAY, 36);              // blocks of zeros
        for (int i = 0; i < 3; i++) {
          s_cre.add(ResourceStructure.ID_DWORD, 0x86e + i*8);   // Spell levels offsets
          s_cre.add(ResourceStructure.ID_DWORD);                // zero
        }
        s_cre.add(ResourceStructure.ID_DWORD, 0x886);           // Item slots offset
        s_cre.add(ResourceStructure.ID_DWORD, 0x886);           // Item offset
        s_cre.add(ResourceStructure.ID_DWORD);                  // zero
        s_cre.add(ResourceStructure.ID_DWORD, 0x886);           // Effects offset
        s_cre.add(ResourceStructure.ID_ARRAY, 612);             // block of zeros
      } else {
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Known spells offset
        s_cre.add(ResourceStructure.ID_DWORD);                  // zero
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Memorization info offset
        s_cre.add(ResourceStructure.ID_DWORD);                  // zero
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Memorized spells offset
        s_cre.add(ResourceStructure.ID_DWORD);                  // zero
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Item slots offset
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Item offset
        s_cre.add(ResourceStructure.ID_DWORD);                  // zero
        s_cre.add(ResourceStructure.ID_DWORD, ofs[idx]);        // Effects offset
        s_cre.add(ResourceStructure.ID_ARRAY, 12);              // block of zeros
      }
      for (int i = 0; i < count[idx]; i++)                      // item slots
        s_cre.add(ResourceStructure.ID_WORD, -1);
      s_cre.add(ResourceStructure.ID_WORD, 1000);               // Weapon slot selected
      s_cre.add(ResourceStructure.ID_WORD);                     // zero

      return s_cre;
    } else
      return unsupportedFormat(ResType.RES_CRE);
  }

  private ResourceStructure createDLG(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_dlg = new ResourceStructure();
      s_dlg.add(ResourceStructure.ID_STRING, 4, "DLG ");    // Signature
      s_dlg.add(ResourceStructure.ID_STRING, 4, "V1.0");    // Version
      int ofs = (id == ResourceFactory.ID_BG2) ? 0x34 : 0x30;
      s_dlg.add(ResourceStructure.ID_DWORD);                // Number of states
      s_dlg.add(ResourceStructure.ID_DWORD, ofs);           // States offset
      s_dlg.add(ResourceStructure.ID_DWORD);                // Number of responses
      s_dlg.add(ResourceStructure.ID_DWORD, ofs);           // Responses offset
      s_dlg.add(ResourceStructure.ID_DWORD, ofs);           // State triggers offset
      s_dlg.add(ResourceStructure.ID_DWORD);                // Number of state triggers
      s_dlg.add(ResourceStructure.ID_DWORD, ofs);           // Response triggers offset
      s_dlg.add(ResourceStructure.ID_DWORD);                // Number of response triggers
      s_dlg.add(ResourceStructure.ID_DWORD, ofs);           // Actions offset
      s_dlg.add(ResourceStructure.ID_DWORD);                // Number of actions
      if (id == ResourceFactory.ID_BG2)
        s_dlg.add(ResourceStructure.ID_DWORD);              // Threat response

      return s_dlg;
    } else
      return unsupportedFormat(ResType.RES_DLG);
  }

  private ResourceStructure createEFF(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2) {
      ResourceStructure s_eff = new ResourceStructure();
      s_eff.add(ResourceStructure.ID_STRING, 4, "EFF ");    // Signature
      s_eff.add(ResourceStructure.ID_STRING, 4, "V2.0");    // Version
      s_eff.add(ResourceStructure.ID_STRING, 4, "EFF ");    // Signature 2
      s_eff.add(ResourceStructure.ID_STRING, 4, "V2.0");    // Version 2
      s_eff.add(ResourceStructure.ID_ARRAY, 256);           // block of zeros

      return s_eff;
    } else
      return unsupportedFormat(ResType.RES_EFF);
  }

  private ResourceStructure createIDS(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_ids = new ResourceStructure();
      final String s = "1\r\n0 Identifier1\r\n";
      s_ids.add(ResourceStructure.ID_STRING, s);

      return s_ids;
    } else
      return unsupportedFormat(ResType.RES_IDS);
  }

  private ResourceStructure createINI(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_TORMENT || id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_ini = new ResourceStructure();
      final String s = "[locals]\r\n\r\n[spawn_main]\r\n";
      s_ini.add(ResourceStructure.ID_STRING, s);

      return s_ini;
    } else
      return unsupportedFormat(ResType.RES_INI);
  }

  private ResourceStructure createITM(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      final String[] version = {"V1  ", "V1.1", "V2.0"};
      final int[] ofs = {0x72, 0x9a, 0x82};
      final int[] count = {4, 44, 20};
      int idx = (id == ResourceFactory.ID_ICEWIND2) ? 2 : (id == ResourceFactory.ID_TORMENT) ? 1 : 0;
      ResourceStructure s_itm = new ResourceStructure();
      s_itm.add(ResourceStructure.ID_STRING, 4, "ITM ");          // Signature
      s_itm.add(ResourceStructure.ID_STRING, 4, version[idx]);    // Version
      s_itm.add(ResourceStructure.ID_STRREF, -1);                 // Unidentified name
      s_itm.add(ResourceStructure.ID_STRREF, -1);                 // Identified name
      s_itm.add(ResourceStructure.ID_ARRAY, 64);                  // block of zeros
      s_itm.add(ResourceStructure.ID_STRREF, -1);                 // Unidentified description
      s_itm.add(ResourceStructure.ID_STRREF, -1);                 // Identified description
      s_itm.add(ResourceStructure.ID_ARRAY, 12);                  // block of zeros
      s_itm.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Abilities offset
      s_itm.add(ResourceStructure.ID_WORD);                       // zero
      s_itm.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Effects offset
      if (id == ResourceFactory.ID_TORMENT) {
        s_itm.add(ResourceStructure.ID_ARRAY, 12);                // block of zeros
        s_itm.add(ResourceStructure.ID_STRREF, -1);               // Conversable label
        s_itm.add(ResourceStructure.ID_ARRAY, count[idx] - 16);   // block of zeros
      } else
        s_itm.add(ResourceStructure.ID_ARRAY, count[idx]);        // block of zeros

      return s_itm;
    } else
      return unsupportedFormat(ResType.RES_ITM);
  }

  private ResourceStructure createMUS(int id, String fileName) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      String fileBase = extractFileBase(fileName).toUpperCase();
      if (fileBase.length() > 8)
        fileBase = fileBase.substring(0, 8);

      ResourceStructure s_mus = new ResourceStructure();
      String s = fileBase + "\r\n1\r\nA   " + fileBase + " A  \r\n";
      s_mus.add(ResourceStructure.ID_STRING, s);

      return s_mus;
    } else
      return unsupportedFormat(ResType.RES_MUS);
  }

  private ResourceStructure createPRO(int id, Window parent) throws StructureException
  {
    if (id == ResourceFactory.ID_BG2) {
      NewProSettings dlg = new NewProSettings(parent, 2);
      if (dlg.isAccepted()) {
        int type = dlg.getConfig().getProjectileType();
        ResourceStructure s_pro = new ResourceStructure();
        s_pro.add(ResourceStructure.ID_STRING, 4, "PRO ");        // Signature
        s_pro.add(ResourceStructure.ID_STRING, 4, "V1.0");        // Version
        s_pro.add(ResourceStructure.ID_WORD, type);               // Projectile type (default: area effect)
        s_pro.add(ResourceStructure.ID_ARRAY, type*256 - 10);     // block of zeros

        return s_pro;
      } else
        return cancelOperation();
    } else
      return unsupportedFormat(ResType.RES_PRO);
  }

  private ResourceStructure createRES(int id, Window parent) throws StructureException
  {
    if (id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      NewResSettings dlg = new NewResSettings(parent);
      if (dlg.isAccepted()) {
        String text = dlg.getConfig().getText();
        ResourceStructure s_res = new ResourceStructure();
        if (text.length() > 0)
          s_res.add(ResourceStructure.ID_STRING, text);

        return s_res;
      } else
        return cancelOperation();
    } else
      return unsupportedFormat(ResType.RES_RES);
  }

  private ResourceStructure createSPL(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      final String[] version = {"V1  ", "V2.0"};
      final int[] ofs = {0x72, 0x82};
      final int[] count = {4, 20};
      int idx = (id == ResourceFactory.ID_ICEWIND2) ? 1 : 0;
      ResourceStructure s_spl = new ResourceStructure();
      s_spl.add(ResourceStructure.ID_STRING, 4, "SPL ");        // Signature
      s_spl.add(ResourceStructure.ID_STRING, 4, version[idx]);  // Version
      s_spl.add(ResourceStructure.ID_STRREF, -1);               // Unidentified spell name
      s_spl.add(ResourceStructure.ID_STRREF, -1);               // Identified spell name
      s_spl.add(ResourceStructure.ID_ARRAY, 40);                // block of zeros
      if (id == ResourceFactory.ID_TORMENT || id == ResourceFactory.ID_ICEWIND)
        s_spl.add(ResourceStructure.ID_WORD, 1);                // always set?
      else
        s_spl.add(ResourceStructure.ID_WORD, 0);                // zero
      s_spl.add(ResourceStructure.ID_ARRAY, 22);                // block of zeros
      s_spl.add(ResourceStructure.ID_STRREF, -1);               // Unidentified spell description
      s_spl.add(ResourceStructure.ID_STRREF, -1);               // Identified spell description
      s_spl.add(ResourceStructure.ID_ARRAY, 12);                // block of zeros
      s_spl.add(ResourceStructure.ID_DWORD, ofs[idx]);          // Abilities offset
      s_spl.add(ResourceStructure.ID_WORD);                     // block of zeros
      s_spl.add(ResourceStructure.ID_DWORD, ofs[idx]);          // Effects offset
      s_spl.add(ResourceStructure.ID_ARRAY, count[idx]);        // block of zeros

      return s_spl;
    } else
      return unsupportedFormat(ResType.RES_SPL);
  }

  private ResourceStructure createSRC(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_TORMENT) {
      ResourceStructure s_src = new ResourceStructure();
      s_src.add(ResourceStructure.ID_DWORD, 1);         // strref entry count
      s_src.add(ResourceStructure.ID_STRREF, -1);       // strref
      s_src.add(ResourceStructure.ID_DWORD, 1);         // always 1?

      return s_src;
    } else if (id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_src = new ResourceStructure();
      final String s = "Placeholder text...";
      s_src.add(ResourceStructure.ID_STRING, s);

      return s_src;
    } else
      return unsupportedFormat(ResType.RES_SRC);
  }

  private ResourceStructure createSTO(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      final String[] version = {"V1.0", "V1.1", "V9.0"};
      final int[] ofs = {0x9c, 0x9c, 0xf0};
      final int[] count = {40, 40, 124};
      int idx = (id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) ? 2 :
        (id == ResourceFactory.ID_TORMENT) ? 1 : 0;
      ResourceStructure s_sto = new ResourceStructure();
      s_sto.add(ResourceStructure.ID_STRING, 4, "STOR");          // Signature
      s_sto.add(ResourceStructure.ID_STRING, 4, version[idx]);    // Version
      s_sto.add(ResourceStructure.ID_DWORD);                      // zero
      s_sto.add(ResourceStructure.ID_STRREF, -1);                 // name
      s_sto.add(ResourceStructure.ID_ARRAY, 28);                  // block of zeros
      s_sto.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Items purchased offset
      s_sto.add(ResourceStructure.ID_DWORD);                      // zero
      s_sto.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Items for sale offset
      s_sto.add(ResourceStructure.ID_ARRAY, 20);                  // block of zeros
      s_sto.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Drinks offset
      s_sto.add(ResourceStructure.ID_ARRAY, 32);                  // block of zeros
      s_sto.add(ResourceStructure.ID_DWORD, ofs[idx]);            // Cures offset
      s_sto.add(ResourceStructure.ID_ARRAY, count[idx]);          // block of zeros

      return s_sto;
    } else
      return unsupportedFormat(ResType.RES_STO);
  }

  private ResourceStructure createVEF(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG2) {
      ResourceStructure s_vef = new ResourceStructure();
      s_vef.add(ResourceStructure.ID_STRING, 4, "VEF ");    // Signature
      s_vef.add(ResourceStructure.ID_STRING, 4, "V1.0");    // Version
      s_vef.add(ResourceStructure.ID_DWORD, 0x18);          // Component1 offset
      s_vef.add(ResourceStructure.ID_DWORD);                // block of zeros
      s_vef.add(ResourceStructure.ID_DWORD, 0x18);          // Component2 offset
      s_vef.add(ResourceStructure.ID_DWORD);                // block of zeros

      return s_vef;
    } else
      return unsupportedFormat(ResType.RES_VEF);
  }

  private ResourceStructure createVVC(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG2) {
      ResourceStructure s_vvc = new ResourceStructure();
      s_vvc.add(ResourceStructure.ID_STRING, 4, "VVC ");    // Signature
      s_vvc.add(ResourceStructure.ID_STRING, 4, "V1.0");    // Version
      s_vvc.add(ResourceStructure.ID_ARRAY, 484);           // block of zeros

      return s_vvc;
    } else
      return unsupportedFormat(ResType.RES_VVC);
  }

  private ResourceStructure createWED(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_wed = new ResourceStructure();
      s_wed.add(ResourceStructure.ID_STRING, 4, "WED ");    // Signature
      s_wed.add(ResourceStructure.ID_STRING, 4, "V1.3");    // Version
      s_wed.add(ResourceStructure.ID_DWORD, 5);             // Overlays count
      s_wed.add(ResourceStructure.ID_DWORD);                // Doors count
      s_wed.add(ResourceStructure.ID_DWORD, 0x20);          // Overlays offset
      s_wed.add(ResourceStructure.ID_DWORD, 0x98);          // Second header offset
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Doors offset
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Door tilemap loopup offset
      for (int i = 0; i < 5; i++) {                         // 5x Overlays
        s_wed.add(ResourceStructure.ID_ARRAY, 16);
        s_wed.add(ResourceStructure.ID_DWORD, 0xac);
        s_wed.add(ResourceStructure.ID_DWORD, 0xac);
      }
      s_wed.add(ResourceStructure.ID_DWORD, 0);             // Wall poly count
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Wall poly offset
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Vertices offset
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Wall groups offset
      s_wed.add(ResourceStructure.ID_DWORD, 0xac);          // Wall poly lookup offset

      return s_wed;
    } else
      return unsupportedFormat(ResType.RES_WED);
  }

  private ResourceStructure createWFX(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG2) {
      ResourceStructure s_wfx = new ResourceStructure();
      s_wfx.add(ResourceStructure.ID_STRING, 4, "WFX ");    // Signature
      s_wfx.add(ResourceStructure.ID_STRING, 4, "V1.0");    // Version
      s_wfx.add(ResourceStructure.ID_ARRAY, 256);           // block of zeros

      return s_wfx;
    } else
      return unsupportedFormat(ResType.RES_WFX);
  }

  private ResourceStructure createWMAP(int id) throws StructureException
  {
    if (id == ResourceFactory.ID_BG1 || id == ResourceFactory.ID_BG2 || id == ResourceFactory.ID_TORMENT ||
        id == ResourceFactory.ID_ICEWIND || id == ResourceFactory.ID_ICEWIND2) {
      ResourceStructure s_wmp = new ResourceStructure();
      s_wmp.add(ResourceStructure.ID_STRING, 4, "WMAP");    // Signature
      s_wmp.add(ResourceStructure.ID_STRING, 4, "V1.0");    // Version
      s_wmp.add(ResourceStructure.ID_DWORD);                // Worlmap entries count
      s_wmp.add(ResourceStructure.ID_DWORD, 0x10);          // Worlmap entries offset

      return s_wmp;
    } else
      return unsupportedFormat(ResType.RES_WMAP);
  }

  // create empty structure
  private ResourceStructure createUnknown()
  {
    return new ResourceStructure();
  }

  private void unsupported() throws StructureException
  {
    throw new StructureException();
  }

  private ResourceStructure unsupportedFormat(ResType type) throws StructureException
  {
    throw new StructureException(type);
  }

  private ResourceStructure unsupportedGame() throws StructureException
  {
    throw new StructureException(StructureException.Reason.UNSUPPORTED_GAME);
  }

  private ResourceStructure cancelOperation() throws StructureException
  {
    throw new StructureException(StructureException.Reason.CANCELLED_OPERATION);
  }


  // returns path without trailing path separator (except for root on unix-based filesystems)
  private String extractFilePath(String fileName)
  {
    if (fileName.length() > 0) {
      int idx = fileName.lastIndexOf(File.separatorChar);
      if (idx == fileName.length() - 1)
        return fileName.substring(0, fileName.length() - 1);
      else if (idx > 0)
        return fileName.substring(0, idx);
    }
    return fileName;
  }

  private String extractFileName(String fileName)
  {
    String[] s = fileName.split("[\\\\/]");
    if (s.length > 0)
      return s[s.length - 1];
    else
      return fileName;
  }

  // returns filename without extension
  private String extractFileBase(String fileName)
  {
    String name = extractFileName(fileName);
    if (name.length() > 0) {
      int idx = name.lastIndexOf('.');
      if (idx >= 0)
        return name.substring(0, idx);
      else
        return name;
    }
    return "";
  }

  // returns file extension without separator
  private String extractFileExt(String fileName)
  {
    String name = extractFileName(fileName);
    if (name.length() > 0) {
      int idx = name.lastIndexOf('.');
      if (idx >= 0 && idx < name.length() - 1)
        return name.substring(idx + 1, name.length());
    }
    return "";
  }


//-------------------------- INNER CLASSES --------------------------

  public static class StructureException extends Exception
  {
    public static enum Reason {
      UNSPECIFIED, CANCELLED_OPERATION, UNSUPPORTED_TYPE, UNSUPPORTED_GAME
    }

    private final ResType resType;
    private final Reason reason;

    public StructureException()
    {
      super();
      this.reason = Reason.UNSPECIFIED;
      this.resType = ResType.RES_2DA;
    }

    public StructureException(Reason reason)
    {
      super();
      this.reason = reason;
      this.resType = ResType.RES_2DA;
    }

    // Specialized ctor for 'unsupported resource type'
    public StructureException(ResType type)
    {
      super();
      this.reason = Reason.UNSUPPORTED_TYPE;
      this.resType = type;
    }

    public Reason getReason()
    {
      return reason;
    }

    // valid only if reason == UNSUPPORTED_TYPE
    public ResType getType()
    {
      return resType;
    }
  }
}
