package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.search.FilteredTextField;
import org.limewire.ui.swing.sharing.friends.BuddyUpdate;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

/**
 * This displays the header on all the sharing panels. It includes the filter
 * box and name and icon describing what this panel is for.
 * 
 * TODO: This is very similar to SortAndFilterPanel. Once final designs are
 * ironed out these two classes should be merged and subclassed.
 */
public class SharingHeaderPanel extends JXPanel implements BuddyUpdate {

    private static final int FILTER_WIDTH = 10;
    
    private final String staticText;
    
    protected JLabel descriptionLabel;
    protected JTextField filterBox;
    protected ViewSelectionPanel viewSelectionPanel;
    
    public SharingHeaderPanel(Icon icon, String staticText, String name, ViewSelectionPanel viewPanel) {
        GuiUtils.assignResources(this);
        
        setBackground(Color.LIGHT_GRAY);
    
        this.staticText = staticText;
        this.viewSelectionPanel = viewPanel;
        createComponents();
        createComponents(icon, staticText + name);
        layoutComponents();
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    protected void createComponents() {
    }
    
    private void createComponents(Icon icon, String text) {
        descriptionLabel = new JLabel(text, icon, JLabel.LEFT);
        filterBox = new FilteredTextField(FILTER_WIDTH);
    }
    
    protected void layoutComponents() {
        setLayout(new MigLayout());

        add(descriptionLabel, "push");
        add(filterBox);
        add(viewSelectionPanel);
    }
    
    @Override
    public void setBuddyName(String name) {
        descriptionLabel.setText(staticText + name);
    }

    @Override
    public void setEventList(EventList<FileItem> model) {

    }
}
