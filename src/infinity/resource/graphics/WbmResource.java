
package infinity.resource.graphics;

import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

public final class WbmResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private JButton bexport;
  private JPanel panel;

  public WbmResource(ResourceEntry entry)
  {
    this.entry = entry;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry,panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------

// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new FlowLayout());
    bpanel.add(bexport);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(bpanel, BorderLayout.SOUTH);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------
}
