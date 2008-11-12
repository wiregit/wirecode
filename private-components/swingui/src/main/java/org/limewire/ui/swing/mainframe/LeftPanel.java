package org.limewire.ui.swing.mainframe;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LeftPanel extends JPanel {
    public static final String NAME = "Library Panel";
    
    @Inject
    public LeftPanel(final Navigator navigator, LibraryNavigator libraryNavigator) {
    	GuiUtils.assignResources(this);

        setName("LeftPanel");
        
        setLayout(new MigLayout("insets 0 0 0 0, fill, gap 0", "", ""));         

        JScrollPane scrollableNav = new JScrollPane(libraryNavigator);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        add(scrollableNav, "grow");
                    
                
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.insets = new Insets(0, 0, 0, 0);
//        gbc.weighty = 0;
//        gbc.gridy++;
//        add(filesSharingPanel, gbc);

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        add(line, "grow");
    }
    
    public void toggleVisibility() {
        setVisible(!isVisible());
    }
}
