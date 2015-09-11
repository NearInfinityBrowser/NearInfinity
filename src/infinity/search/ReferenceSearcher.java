// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.TextString;
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
import infinity.resource.other.EffResource;
import infinity.resource.sav.SavResource;
import infinity.resource.text.PlainTextResource;

import java.awt.Component;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ReferenceSearcher extends AbstractReferenceSearcher
{
  public ReferenceSearcher(ResourceEntry targetEntry, Component parent)
  {
    super(targetEntry, AbstractReferenceSearcher.FILE_TYPES, parent);
  }

  public ReferenceSearcher(ResourceEntry targetEntry, String[] fileTypes, Component parent)
  {
    super(targetEntry, fileTypes, parent);
  }

  public ReferenceSearcher(ResourceEntry targetEntry, String[] fileTypes, boolean[] preselect, Component parent)
  {
    super(targetEntry, fileTypes, preselect, parent);
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
    } else if (resource instanceof PlainTextResource) {
      searchText(entry, (PlainTextResource)resource);
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    } else if (resource instanceof BcsResource) {
      searchScript(entry, (BcsResource)resource);
    }
  }

  private void searchDialog(ResourceEntry entry, AbstractStruct dialog)
  {
    boolean hit = false;
    for (int i = 0; i < dialog.getFieldCount(); i++) {
      StructEntry o = dialog.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString())) {
        addHit(entry, entry.getSearchString(), o);
      }
      else if (o instanceof AbstractCode) {
        AbstractCode sourceCode = (AbstractCode)o;
        try {
          String code = Compiler.getInstance().compileDialogCode(sourceCode.toString(),
                                                                 sourceCode instanceof Action);
          if (Compiler.getInstance().getErrors().size() == 0) {
            if (o instanceof Action) {
              Decompiler.decompileDialogAction(code, true);
            } else {
              Decompiler.decompileDialogTrigger(code, true);
            }
            for (final ResourceEntry resourceUsed : Decompiler.getResourcesUsed()) {
              if (targetEntry.toString().equalsIgnoreCase(resourceUsed.toString())) {
                hit = true;
                addHit(entry, entry.getSearchString(), sourceCode);
              } else if (targetEntry == resourceUsed) {
                // searching for symbolic spell names
                String s = infinity.resource.spl.Viewer.getSymbolicName(targetEntry, false);
                if (s != null && s.equalsIgnoreCase(resourceUsed.toString())) {
                  addHit(entry, s, sourceCode);
                }
              }
            }
            if (!hit && targetEntryName != null) {
              Pattern p = Pattern.compile("\\b" + targetEntryName + "\\b", Pattern.CASE_INSENSITIVE);
              Matcher m = p.matcher(code);
              if (m.find()) {
                addHit(entry, targetEntryName, sourceCode);
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
    boolean hit = false;
    String code = Decompiler.decompile(bcsfile.getCode(), true);
    if (Decompiler.getResourcesUsed().contains(targetEntry)) {
      hit = true;
      addHit(entry, entry.getSearchString(), null);
    }
    if (!hit && targetEntryName != null) {
      Pattern p = Pattern.compile("\\b" + targetEntryName + "\\b", Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(code);
      if (m.find()) {
        addHit(entry, targetEntryName, null);
      }
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (int i = 0; i < struct.getFieldCount(); i++) {
      StructEntry o = struct.getField(i);
      if (o instanceof ResourceRef &&
          ((ResourceRef)o).getResourceName().equalsIgnoreCase(targetEntry.toString())) {
        addHit(entry, entry.getSearchString(), o);
      } else if (o instanceof ProRef && ((ProRef)o).getSelectedEntry() == targetEntry) {
        addHit(entry, entry.getSearchString(), o);
      } else if (o instanceof AbstractStruct) {
        searchStruct(entry, (AbstractStruct)o);
      }
    }

    // special cases
    final String keyword = (targetEntry.toString().lastIndexOf('.') >= 0) ?
        targetEntry.toString().substring(0, targetEntry.toString().lastIndexOf('.')) :
          targetEntry.toString();
    if (struct instanceof EffResource) {
      // checking resource2/3 fields
      final String[] fieldName = {"Resource 2", "Resource 3"};
      for (int i = 0; i < fieldName.length; i++) {
        StructEntry o = struct.getAttribute(fieldName[i]);
        if (o instanceof TextString) {
          if (o.toString().equalsIgnoreCase(keyword)) {
            addHit(entry, entry.getSearchString(), o);
          }
        }
      }
    }
  }

  private void searchBam(ResourceEntry entry, BamResource bam)
  {
    Pattern pattern = Pattern.compile("MOS([0-9]{4,5})\\.PVRZ$", Pattern.CASE_INSENSITIVE);
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
    Pattern pattern = Pattern.compile("MOS([0-9]{4,5})\\.PVRZ$", Pattern.CASE_INSENSITIVE);
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
    Pattern pGeneric = Pattern.compile("MOS[0-9]{4,5}\\.PVRZ$", Pattern.CASE_INSENSITIVE);
    Pattern pArea = Pattern.compile("(.)(.{4})(N?)([0-9]{2})\\.PVRZ$", Pattern.CASE_INSENSITIVE);
    Matcher mGeneric = pGeneric.matcher(getTargetEntry().getResourceName());
    if (!mGeneric.find()) {
      Matcher mArea = pArea.matcher(getTargetEntry().getResourceName());
      if (mArea.find()) {
        String prefix = mArea.group(1);
        String code = mArea.group(2);
        String night = mArea.group(3);
        String page = mArea.group(4);
        Pattern pTis;
        if (night.isEmpty()) {
          pTis = Pattern.compile(String.format("%1$s.%2$s\\.TIS$",
                                               prefix.toUpperCase(Locale.ENGLISH), code),
                                 Pattern.CASE_INSENSITIVE);
        } else {
          pTis = Pattern.compile(String.format("%1$s.%2$sN\\.TIS$",
                                               prefix.toUpperCase(Locale.ENGLISH), code),
                                 Pattern.CASE_INSENSITIVE);
        }
        int index = -1;
        try {
          index = Integer.parseInt(page);
        } catch (NumberFormatException e) {
        }
        if (pTis.matcher(entry.getResourceName()).find() && index >= 0 && index <= 99) {
          if (tis.containsPvrzReference(index)) {
            addHit(entry, null, null);
          }
        }
      }
    }
  }

  private void searchText(ResourceEntry entry, PlainTextResource text)
  {
    String name = getTargetEntry().getResourceName();
    int idx = name.lastIndexOf('.');
    if (idx > 0) {
      name = name.substring(0, idx);
    }
    Pattern p = Pattern.compile("\\b" + name + "\\b", Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(text.getText());
    if (m.find()) {
      addHit(entry, entry.getSearchString(), null);
    }
    if (targetEntryName != null && !targetEntryName.equalsIgnoreCase(name)) {
      p = Pattern.compile("\\b" + targetEntryName + "\\b", Pattern.CASE_INSENSITIVE);
      m = p.matcher(text.getText());
      if (m.find()) {
        addHit(entry, targetEntryName, null);
      }
    }
  }
}

