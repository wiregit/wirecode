package org.limewire.ui.swing.mainframe;

import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LeftPanel extends JXCollapsiblePane {
    public static final String NAME = "Library Panel";
    
    @Inject
    public LeftPanel(final Navigator navigator, LibraryNavigator libraryNavigator) {
        super(Direction.LEFT);
    	GuiUtils.assignResources(this);

        setName("LeftPanel");
        
        JXPanel panel = new JXPanel(new MigLayout("insets 0 0 0 0, fill, gap 0", "", ""));         

        JScrollPane scrollableNav = new JScrollPane(libraryNavigator);
        scrollableNav.setOpaque(false);
        scrollableNav.getViewport().setOpaque(false);
        scrollableNav.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollableNav.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollableNav.setBorder(null);
        panel.add(scrollableNav, "grow");
                    
                
//        gbc.fill = GridBagConstraints.HORIZONTAL;
//        gbc.insets = new Insets(0, 0, 0, 0);
//        gbc.weighty = 0;
//        gbc.gridy++;
//        add(filesSharingPanel, gbc);

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        panel.add(line, "grow");
        
        setContentPane(panel);
    }
    
    public void toggleVisibility() {
        setCollapsed(!isCollapsed());
    }
}
