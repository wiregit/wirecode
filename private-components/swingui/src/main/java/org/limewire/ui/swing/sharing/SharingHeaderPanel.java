package org.limewire.ui.swing.sharing;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.FilteredTextField;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This displays the header on all the sharing panels. It includes the filter
 * box and name and icon describing what this panel is for.
 * 
 * TODO: This is very similar to SortAndFilterPanel. Once final designs are
 * ironed out these two classes should be merged and subclassed.
 */
public class SharingHeaderPanel extends JXPanel {

    private static final int FILTER_WIDTH = 10;
    
    private JLabel descriptionLabel;
    private JTextField filterBox;
    
    public SharingHeaderPanel(Icon icon, String text) {
        GuiUtils.assignResources(this);
        
        setBackground(Color.LIGHT_GRAY);
    
        createComponents(icon, text);
        layoutComponents();
    }
    
    public JTextField getFilterBox() {
        return filterBox;
    }
    
    private void createComponents(Icon icon, String text) {
        descriptionLabel = new JLabel(text, icon, JLabel.LEFT);
        filterBox = new FilteredTextField(FILTER_WIDTH);
    }
    
    private void layoutComponents() {
        setLayout(new MigLayout());

        add(descriptionLabel, "push");
        add(filterBox);
    }
}
