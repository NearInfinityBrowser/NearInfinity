// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.State;
import org.infinity.resource.dlg.Transition;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;

/** Performs checking {@link DlgResource DLG} resources. */
public final class DialogChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private final boolean checkOnlyOverride;
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private JTabbedPane tabbedPane;
  /** List of the {@link Problem} objects with compiler errors in dialog actions. */
  private SortableTable errorTable;
  /** List of the {@link Problem} objects with compiler warnings in dialog actions. */
  private SortableTable warningTable;

  public DialogChecker(boolean checkOnlyOverride, Component parent)
  {
    super(CHECK_ONE_TYPE_FORMAT, parent);
    this.checkOnlyOverride = checkOnlyOverride;
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    SortableTable table = errorTable;
    if (tabbedPane.getSelectedIndex() == 1)
      table = warningTable;
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer().selectEntry(
                (String)table.getValueAt(row, 1));
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
      }
    }
    else if (event.getSource() == bsave) {
      final String type = table == errorTable ? "Errors" : "Warnings";
      table.saveCheckResult(resultFrame, type + " in dialogues");
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (tabbedPane.getSelectedIndex() == 0)
      bopen.setEnabled(errorTable.getSelectedRowCount() > 0);
    else
      bopen.setEnabled(warningTable.getSelectedRowCount() > 0);
    bopennew.setEnabled(bopen.isEnabled());
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (tabbedPane.getSelectedIndex() == 0)
      bopen.setEnabled(errorTable.getSelectedRowCount() > 0);
    else
      bopen.setEnabled(warningTable.getSelectedRowCount() > 0);
    bopennew.setEnabled(bopen.isEnabled());
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> dlgFiles = ResourceFactory.getResources("DLG");
      if (checkOnlyOverride) {
        for (Iterator<ResourceEntry> i = dlgFiles.iterator(); i.hasNext();) {
          ResourceEntry resourceEntry = i.next();
          if (!resourceEntry.hasOverride())
            i.remove();
        }
      }

      final Class<?>[] colClasses = {ResourceEntry.class, String.class, String.class, Integer.class};
      errorTable = new SortableTable(
          new String[]{"Dialogue", "Field", "Error message", "Line"},
          colClasses,
          new Integer[]{50, 100, 350, 10});
      warningTable = new SortableTable(
          new String[]{"Dialogue", "Field", "Warning", "Line"},
          colClasses,
          new Integer[]{50, 100, 350, 10});

      if (runSearch("Checking dialogues", dlgFiles)) {
        return;
      }

      if (errorTable.getRowCount() + warningTable.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        errorTable.tableComplete();
        warningTable.tableComplete();
        resultFrame = new ChildFrame("Result of dialogues check", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
        JScrollPane scrollErrorTable = new JScrollPane(errorTable);
        scrollErrorTable.getViewport().setBackground(errorTable.getBackground());
        JScrollPane scrollWarningTable = new JScrollPane(warningTable);
        scrollWarningTable.getViewport().setBackground(warningTable.getBackground());
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Errors (" + errorTable.getRowCount() + ')', scrollErrorTable);
        tabbedPane.addTab("Warnings (" + warningTable.getRowCount() + ')', scrollWarningTable);
        tabbedPane.addChangeListener(this);
        bopen.setMnemonic('o');
        bopennew.setMnemonic('n');
        bsave.setMnemonic('s');
        resultFrame.getRootPane().setDefaultButton(bopennew);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bopen);
        panel.add(bopennew);
        panel.add(bsave);
        JPanel pane = (JPanel)resultFrame.getContentPane();
        pane.setLayout(new BorderLayout(0, 3));
        pane.add(tabbedPane, BorderLayout.CENTER);
        pane.add(panel, BorderLayout.SOUTH);
        bopen.setEnabled(false);
        bopennew.setEnabled(false);
        errorTable.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        errorTable.setRowHeight(errorTable.getFontMetrics(errorTable.getFont()).getHeight() + 1);
        errorTable.getSelectionModel().addListSelectionListener(this);
        warningTable.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        warningTable.setRowHeight(warningTable.getFontMetrics(warningTable.getFont()).getHeight() + 1);
        warningTable.getSelectionModel().addListSelectionListener(this);
        MouseListener listener = new MouseAdapter()
        {
          @Override
          public void mouseReleased(MouseEvent event)
          {
            if (event.getClickCount() == 2) {
              SortableTable table = (SortableTable)event.getSource();
              int row = table.getSelectedRow();
              if (row != -1) {
                ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
                Resource resource = ResourceFactory.getResource(resourceEntry);
                new ViewFrame(resultFrame, resource);
                ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
              }
            }
          }
        };
        errorTable.addMouseListener(listener);
        warningTable.addMouseListener(listener);
        bopen.addActionListener(this);
        bopennew.addActionListener(this);
        bsave.addActionListener(this);
        pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        resultFrame.setSize(700, 600);
        Center.center(resultFrame, NearInfinity.getInstance().getBounds());
        resultFrame.setVisible(true);
      }
    } finally {
      blocker.setBlocked(false);
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      try {
        final DlgResource dialog = new DlgResource(entry);
        for (final StructEntry o : dialog.getFields()) {
          if (o instanceof AbstractCode) {
            checkCode(entry, (AbstractCode)o);
          } else
          if (o instanceof State) {
            checkState(dialog, (State)o);
          }
        }
      } catch (Exception e) {
        synchronized (System.err) {
          e.printStackTrace();
        }
      }
      advanceProgress();
    };
  }

  /**
   * Performs code checking. This method can be called from several threads
   *
   * @param entry Pointer to dialog resource for check. Never {@code null}
   * @param code Code of action or trigger in dialog. Never {@code null}
   */
  private void checkCode(ResourceEntry entry, AbstractCode code) {
    final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
    final Compiler compiler = new Compiler(code.getText(), type);
    compiler.compile();
    synchronized (errorTable) {
      for (final ScriptMessage sm : compiler.getErrors()) {
        errorTable.addTableItem(new Problem(
          entry, code, sm.getLine(), sm.getMessage(), Problem.Type.ERROR
        ));
      }
    }
    synchronized (warningTable) {
      for (final ScriptMessage sm : compiler.getWarnings()) {
        warningTable.addTableItem(new Problem(
          entry, code, sm.getLine(), sm.getMessage(), Problem.Type.WARNING
        ));
      }
    }
  }

  /**
   * Performs checking of the dialogue state. Reports error, if state:
   * <ol>
   * <li>has more that one response</li>
   * <li>has response without associated text</li>
   * <li>has another response without trigger</li>
   * </ol>
   * Error reported because responses without text not shown in the dialogue and
   * not available to select by PC from one hand, and from another hand if more
   * that 2 responses not filtered by triggers, dialogue stops and requires PC
   * to select one... even if all of them without text (in that case there just
   * no options to select).
   * <p>
   * Original PS:T files contains at least 4 errors of this type
   *
   * @param dlg Dialogue that owns {@code state}
   * @param state State for checking. Never {@code null}
   */
  private void checkState(DlgResource dlg, State state)
  {
    final int count = state.getTransCount();
    if (count < 2) return;

    final int start = state.getFirstTrans();
    for (int i = start; i < start + count; ++i) {
      final Transition trans = dlg.getTransition(i);
      if (trans.hasAssociatedText()) continue;

      final String message = String.format(
        "Response has no trigger, but parent state %d has response %d without text - this response won't be accessible to the player",
        state.getNumber(),
        trans.getNumber()
      );

      // If state has response without associated text, trigger error on all responses
      // without triggers (excludes response without text)
      for (int j = start; j < start + count; ++j) {
        if (i == j) continue;

        final Transition t = dlg.getTransition(j);
        if (t.getTriggerIndex() >= 0) continue;

        synchronized (errorTable) {
          errorTable.addTableItem(new Problem(dlg.getResourceEntry(), t, null, message, Problem.Type.ERROR));
        }
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class Problem implements TableItem
  {
    public enum Type {
      ERROR,
      WARNING,
    }

    /** Resource in which problem is found. */
    private final ResourceEntry resourceEntry;
    /** Entry in resource, in which problem is found. */
    private final StructEntry problemEntry;
    /** If problem in code block, then this is line with problem, otherwize {@code null}. */
    private final Integer lineNr;
    /** Description of a problem. */
    private final String message;
    /** Problem severity. */
    private final Type type;

    private Problem(ResourceEntry resourceEntry, StructEntry problemEntry, Integer lineNr,
                                  String message, Type type)
    {
      this.resourceEntry = resourceEntry;
      this.problemEntry = problemEntry;
      this.lineNr = lineNr;
      this.message = message;
      this.type = type;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      switch (columnIndex) {
        case 0: return resourceEntry;
        case 1: return problemEntry.getName();
        case 2: return message;
        default: return lineNr;
      }
    }

    @Override
    public String toString()
    {
      final String type = (this.type == Type.ERROR) ? "Error" : "Warning";
      if (lineNr == null) {
        return String.format("File: %s, Owner: %s, %s: %s",
                             resourceEntry.getResourceName(), problemEntry.getName(), type, message);
      }
      return String.format("File: %s, Line: %d, Owner: %s, %s: %s",
                           resourceEntry.getResourceName(), lineNr, problemEntry.getName(), type, message);
    }
  }
}
