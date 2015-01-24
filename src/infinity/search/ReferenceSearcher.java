// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.bcs.BcsResource;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.dlg.AbstractCode;
import infinity.resource.dlg.Action;
import infinity.resource.dlg.DlgResource;
import infinity.resource.graphics.BamResource;
import infinity.resource.graphics.MosResource;
import infinity.resource.graphics.TisResource;
import infinity.resource.key.ResourceEntry;
import infinity.resource.sav.SavResource;

import java.awt.Component;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReferenceSearcher extends AbstractReferenceSearcher
{
  private static final String[] FILE_TYPES = {"ARE", "BCS", "CHR", "CHU", "CRE", "DLG",
                                              "EFF", "GAM", "ITM", "PRO", "SAV", "SPL",
                                              "STO", "VEF", "VVC", "WED", "WMP"};

  public ReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    this(targetEntry, FILE_TYPES, parent);
  }

  public ReferenceSearcher(ResourceEntry targetEntry, String[] fileTypes, Component parent)
  {
    super(targetEntry, fileTypes, parent);
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof DlgResource) {
      searchDialog(entry, (AbstractStruct)resource);
    } else if (resource instanceof SavResource) {
      searchSave(entry, (SavResource)resource);
    } else if (resource instanceof BamResource) {
      searchBam(entry, (BamResource)resource);
    } else if (resource instanceof MosResource) {
      searchMos(entry, (MosResource)resource);
    } else if (resource instanceof TisResource) {
      searchTis(entry, (TisResource)resource);
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    } else {
      searchScript(entry, (BcsResource)resource);
    }
  }

  private void searchDialog(ResourceEntry entry, AbstractStruct dialog)
  {
    for (int i = 0; i < dialog.getFieldCount(); i++) {
      StructEntry o = dialog.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractCode) {
        AbstractCode sourceCode = (AbstractCode)o;
        try {
          String code = Compiler.getInstance().compileDialogCode(sourceCode.toString(),
                                                                 sourceCode instanceof Action);
          if (Compiler.getInstance().getErrors().size() == 0) {
            if (o instanceof Action)
              Decompiler.decompileDialogAction(code, true);
            else
              Decompiler.decompileDialogTrigger(code, true);
            for (final ResourceEntry resourceUsed : Decompiler.getResourcesUsed()) {
              if (targetEntry.toString().equalsIgnoreCase(resourceUsed.toString())) {
                addHit(entry, entry.getSearchString(), sourceCode);
              } else if (targetEntry == resourceUsed) {
                // searching for symbolic spell names
                String s = infinity.resource.spl.Viewer.getSymbolicName(targetEntry, false);
                if (s != null && s.equalsIgnoreCase(resourceUsed.toString())) {
                  addHit(entry, s, sourceCode);
                }
              }
            }
          }
        } catch (Exception e) {
          System.out.println("Exception in " + dialog.getName() + " - " + sourceCode.getName());
          e.printStackTrace();
        }
      }
      else if (o instanceof AbstractStruct)
        searchDialog(entry, (AbstractStruct)o);
    }
  }

  private void searchSavStruct(ResourceEntry entry, ResourceEntry saventry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, saventry.toString(), o);
      else if (o instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)o);
    }
  }

  private void searchSave(ResourceEntry entry, SavResource savfile)
  {
    List<? extends ResourceEntry> entries = savfile.getFileHandler().getFileEntries();
    for (int i = 0; i < entries.size(); i++) {
      ResourceEntry saventry = entries.get(i);
      Resource resource = ResourceFactory.getResource(saventry);
      if (resource instanceof AbstractStruct)
        searchSavStruct(entry, saventry, (AbstractStruct)resource);
    }
  }

  private void searchScript(ResourceEntry entry, BcsResource bcsfile)
  {
    Decompiler.decompile(bcsfile.getCode(), true);
    for (final ResourceEntry resourceUsed : Decompiler.getResourcesUsed()) {
      if (resourceUsed == targetEntry) {
        addHit(entry, null, null);
      }
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString()))
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof ProRef && ((ProRef)o).getSelectedEntry() == targetEntry)
        addHit(entry, entry.getSearchString(), o);
      else if (o instanceof AbstractStruct)
        searchStruct(entry, (AbstractStruct)o);
    }
  }

  private void searchBam(ResourceEntry entry, BamResource bam)
  {
    final String regexGeneric = "[Mm][Oo][Ss]([0-9]{4,5})\\.[Pp][Vv][Rr][Zz]$";
    Pattern pattern = Pattern.compile(regexGeneric);
    Matcher matcher = pattern.matcher(getTargetEntry().getResourceName());
    if (matcher.find()) {
      int index = -1;
      try {
        index = Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException e) {
      }
      if (index >= 0 && index <= 99999) {
        if (bam.containsPvrzReference(index)) {
          addHit(entry, null, null);
        }
      }
    }
  }

  private void searchMos(ResourceEntry entry, MosResource mos)
  {
    final String regexGeneric = "[Mm][Oo][Ss]([0-9]{4,5})\\.[Pp][Vv][Rr][Zz]$";
    Pattern pattern = Pattern.compile(regexGeneric);
    Matcher matcher = pattern.matcher(getTargetEntry().getResourceName());
    if (matcher.find()) {
      int index = -1;
      try {
        index = Integer.parseInt(matcher.group(1));
      } catch (NumberFormatException e) {
      }
      if (index >= 0 && index <= 99999) {
        if (mos.containsPvrzReference(index)) {
          addHit(entry, null, null);
        }
      }
    }
  }

  private void searchTis(ResourceEntry entry, TisResource tis)
  {
    final String regexGeneric = "[Mm][Oo][Ss][0-9]{4,5}\\.[Pp][Vv][Rr][Zz]$";
    final String regexArea = "(.)(.{4})([Nn]?)([0-9]{2})\\.[Pp][Vv][Rr][Zz]$";
    if (!Pattern.matches(regexGeneric, getTargetEntry().getResourceName())) {
      Pattern pattern = Pattern.compile(regexArea);
      Matcher matcher = pattern.matcher(getTargetEntry().getResourceName());
      if (matcher.find()) {
        String prefix = matcher.group(1);
        String code = matcher.group(2);
        String night = matcher.group(3);
        String page = matcher.group(4);
        String patternTis;
        if (night.isEmpty()) {
          patternTis = String.format("[%1$s%2$s].%3$s\\.[Tt][Ii][Ss]$",
              prefix.toUpperCase(Locale.ENGLISH),
              prefix.toLowerCase(Locale.ENGLISH),
              code);
        } else {
          patternTis = String.format("[%1$s%2$s].%3$s[nN]\\.[Tt][Ii][Ss]$",
              prefix.toUpperCase(Locale.ENGLISH),
              prefix.toLowerCase(Locale.ENGLISH),
              code);
        }
        int index = -1;
        try {
          index = Integer.parseInt(page);
        } catch (NumberFormatException e) {
        }
        if (Pattern.matches(patternTis, entry.getResourceName())
            && index >= 0 && index <= 99) {
          if (tis.containsPvrzReference(index)) {
            addHit(entry, null, null);
          }
        }
      }
    }
    // TODO
  }

}

