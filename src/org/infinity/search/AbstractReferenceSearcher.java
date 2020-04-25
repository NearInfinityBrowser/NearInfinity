// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;

abstract class AbstractReferenceSearcher extends AbstractSearcher implements Runnable, ActionListener
{
  protected static final String[] FILE_TYPES = {"2DA", "ARE", "BCS", "CHR", "CHU", "CRE", "DLG",
                                                "EFF", "GAM", "INI", "ITM", "MENU", "PRO", "SAV",
                                                "SPL", "STO", "VEF", "VVC", "WED", "WMP"};

  /** Searched entry. */
  protected final ResourceEntry targetEntry;

  private final ChildFrame selectframe = new ChildFrame("References", true);
  /** Selector of file types in which search must be performed. */
  private final FileTypeSelector selector;
  private final JButton bStart = new JButton("Search", Icons.getIcon(Icons.ICON_FIND_16));
  private final JButton bCancel = new JButton("Cancel", Icons.getIcon(Icons.ICON_DELETE_16));

  /** Window with results of search. */
  private final ReferenceHitFrame hitFrame;
  /** Actual list of resources in which perform search. */
  private List<ResourceEntry> files;

  AbstractReferenceSearcher(ResourceEntry targetEntry, String filetypes[], Component parent)
  {
    this(targetEntry, filetypes, setSelectedFileTypes(targetEntry, filetypes), parent);
  }

  AbstractReferenceSearcher(ResourceEntry targetEntry, String filetypes[], boolean[] preselect, Component parent)
  {
    super(SEARCH_MULTI_TYPE_FORMAT, parent);
    this.targetEntry = targetEntry;

    hitFrame = new ReferenceHitFrame(targetEntry, parent);
    if (filetypes.length == 1) {
      selector = null;
      files = new ArrayList<>();
      files.addAll(ResourceFactory.getResources(filetypes[0]));
      if (!files.isEmpty()) {
        new Thread(this).start();
      }
    }
    else {
      selector = new FileTypeSelector("Select files to search:", getTargetExtension(), filetypes, preselect);
      bStart.setMnemonic('s');
      bCancel.setMnemonic('c');
      bStart.addActionListener(this);
      bCancel.addActionListener(this);
      selectframe.getRootPane().setDefaultButton(bStart);
      selectframe.setIconImage(Icons.getIcon(Icons.ICON_FIND_16).getImage());

      final JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      bpanel.add(bStart);
      bpanel.add(bCancel);

      final JPanel pane = (JPanel)selectframe.getContentPane();
      pane.setLayout(new BorderLayout());
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      pane.add(selector, BorderLayout.CENTER);
      pane.add(bpanel, BorderLayout.SOUTH);

      selectframe.pack();
      Center.center(selectframe, parent.getBounds());
      selectframe.setVisible(true);
    }
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bStart) {
      selectframe.setVisible(false);
      files = selector.getResources(getTargetExtension());
      if (!files.isEmpty()) {
        new Thread(this).start();
      }
    }
    else if (event.getSource() == bCancel) {
      selectframe.setVisible(false);
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    // executing multithreaded search
    if (runSearch("Searching", files)) {
      hitFrame.close();
      return;
    }
    hitFrame.setVisible(true);
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource != null) {
        search(entry, resource);
      }
      advanceProgress();
    };
  }

  /**
   * Registers match hit.
   *
   * @param entry Resource in which match is found.
   * @param name Localized name of the matched resource.
   * @param ref Field in the matched resource that contains found object.
   */
  synchronized void addHit(ResourceEntry entry, String name, StructEntry ref)
  {
    hitFrame.addHit(entry, name, ref);
  }

  /**
   * Registers textual match hit.
   *
   * @param entry Resource in which match is found.
   * @param line Text content of line where match is found.
   * @param lineNr Line number of the match.
   */
  synchronized void addHit(ResourceEntry entry, String line, int lineNr)
  {
    hitFrame.addHit(entry, line, lineNr);
  }

  /**
   * Performs actual search necessary information in the resource. Search procedure
   * must register their results by calling method {@link #addHit}.
   *
   * @param entry Pointer to the resource in which search is performed
   * @param resource Loaded resource that corresponds to the {@code entry}
   */
  abstract void search(ResourceEntry entry, Resource resource);

  ResourceEntry getTargetEntry()
  {
    return targetEntry;
  }

  private String getTargetExtension()
  {
    return (targetEntry != null) ? targetEntry.getExtension() : "";
  }

  /**
   * Determines default set of extensions in which search must be performed based
   * on the {@code entry} extension.
   *
   * @param entry Searched entry. If {@code null}, method returns {@code null}
   * @param filetypes List of the resource extensions in which search can be performed.
   *        Must not be {@code null}
   * @return An array with the same size as {@code filetypes}
   */
  private static boolean[] setSelectedFileTypes(ResourceEntry entry, String[] filetypes)
  {
    if (entry == null) { return null; }

    final boolean[] retVal = new boolean[filetypes.length];
    String[] selectedExt = null;
    final String ext = entry.getExtension();
    if ("2DA".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"BCS", "CRE", "DLG", "EFF", "ITM", "SPL"};
    } else if ("ARE".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "BCS", "DLG", "GAM", "WMP"};
    } else if ("BAM".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "CRE", "DLG", "EFF", "GAM",
                                 "INI", "ITM", "MENU", "PRO", "SPL", "VEF", "VVC", "WMP"};
    } else if ("BMP".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "CRE", "DLG", "EFF", "GAM",
                                 "INI", "ITM", "PRO", "SPL", "VEF", "VVC"};
    } else if ("CRE".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "DLG", "EFF", "GAM", "INI", "ITM", "SPL"};
    } else if ("DLG".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "BCS", "CRE", "DLG", "GAM"};
    } else if ("EFF".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"CRE", "EFF", "GAM", "ITM", "SPL"};
    } else if ("FNT".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"CHU"};
    } else if ("ITM".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "ITM",
                                 "SPL", "STO"};
    } else if ("MOS".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "DLG", "MENU", "WMP"};
    } else if ("MVE".equalsIgnoreCase(ext) || "WBM".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "ITM", "SPL"};
    } else if ("PNG".equalsIgnoreCase(ext)) {
      // TODO: confirm!
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CHU", "CRE", "DLG", "EFF", "GAM",
                                 "INI", "ITM", "MENU", "PRO", "SPL", "VEF", "VVC"};
    } else if ("PRO".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"ARE", "CRE", "EFF", "GAM", "ITM", "SPL"};
    } else if ("PVRZ".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"BAM", "MOS", "TIS"};
    } else if ("SPL".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM",
                                 "ITM", "SPL"};
    } else if ("STO".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"BCS", "DLG"};
    } else if ("TIS".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"WED"};
    } else if ("VVC".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"ARE", "BCS", "DLG", "EFF", "GAM", "INI", "ITM", "PRO",
                                 "SPL", "VEF", "VVC"};
    } else if ("WAV".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"2DA", "ARE", "BCS", "CRE", "DLG", "EFF", "GAM", "INI",
                                 "ITM", "PRO", "SPL", "VEF", "VVC"};
    } else if ("WED".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"ARE"};
    } else if ("WMP".equalsIgnoreCase(ext)) {
      selectedExt = new String[]{"BCS", "DLG"};
    }

    // defining preselection
    if (selectedExt != null && selectedExt.length > 0) {
      for (int i = 0; i < retVal.length; i++) {
        for (String e : selectedExt) {
          if (e.equalsIgnoreCase(filetypes[i])) {
            retVal[i] = true;
            break;
          }
        }
      }
    } else {
      // select all by default
      Arrays.fill(retVal, true);
    }
    return retVal;
  }
}
