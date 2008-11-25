package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LeftPanel extends JXCollapsiblePane implements VisibleComponent {
    public static final String NAME = "Library Panel";

    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();

    @Inject
    public LeftPanel(Navigator navigator, LibraryNavigator libraryNavigator) {
        super(Direction.LEFT);
        GuiUtils.assignResources(this);

        setName("LeftPanel");

        JXPanel panel = new JXPanel(new MigLayout("insets 0, fill, gap 0"));
        panel.setMinimumSize(new Dimension(150, 0));
        panel.setMaximumSize(new Dimension(150, 999));
        panel.setPreferredSize(new Dimension(150, 999));
        
        JXPanel libraryNav = libraryNavigator.getComponent();
        panel.add(libraryNav, "grow");

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        panel.add(line, "grow");

        setContentPane(panel);
    }

    public void toggleVisibility() {
        setVisibility(!isCollapsed());
    }

    @Override
    public void addVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.addVisibilityListener(listener);

    }

    @Override
    public void removeVisibilityListener(VisibilityListener listener) {
        visibilityListenerList.removeVisibilityListener(listener);
    }

    @Override
    public void setVisibility(boolean visible) {
        setCollapsed(visible);
        visibilityListenerList.visibilityChanged(!visible);
    }
}
