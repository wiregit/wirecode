package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.sharing.FilesSharingSummaryPanel;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LeftPanel extends JPanel {
    
    @Inject
    public LeftPanel(final Navigator navigator, LibraryNavigator libraryNavigator, FilesSharingSummaryPanel filesSharingPanel) {
    	GuiUtils.assignResources(this);
//        setMinimumSize(new Dimension(150, 0));
//        setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
//        setPreferredSize(new Dimension(150, 700));
        setName("LeftPanel");
             
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JScrollPane scrollableNav = new JScrollPane(libraryNavigator);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        add(scrollableNav, gbc);                       
                
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weighty = 0;
        gbc.gridy++;
        add(filesSharingPanel, gbc);

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridheight = gbc.gridy+1;
        gbc.gridy = 0;
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        add(line, gbc);
    }
}
