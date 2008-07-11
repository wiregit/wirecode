package org.limewire.ui.swing.search.resultpanel;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JList;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.FontUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventListModel;

public class AllResultsPanel extends JXPanel {
    
    
    public AllResultsPanel(EventList<VisualSearchResult> visualSearchResults) {
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 0);
        JLabel title = new JLabel("All Results from Everyone");
        FontUtils.changeFontSize(title, 5);
        FontUtils.changeStyle(title, Font.BOLD);
        add(title, gbc);
                
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 5, 5, 0);
        add(new JList(new EventListModel<VisualSearchResult>(visualSearchResults)), gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 5, 0, 5);
        add(new JLabel("sponsored results"), gbc);
    }

}
