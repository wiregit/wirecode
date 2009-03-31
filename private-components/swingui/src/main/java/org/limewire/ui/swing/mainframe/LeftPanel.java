package org.limewire.ui.swing.mainframe;

import javax.swing.JComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.EnabledListener;
import org.limewire.ui.swing.util.EnabledListenerList;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.VisibilityListener;
import org.limewire.ui.swing.util.VisibilityListenerList;
import org.limewire.ui.swing.util.VisibleComponent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LeftPanel extends JXPanel implements VisibleComponent {
    public static final String NAME = "Library Panel";

    private final VisibilityListenerList visibilityListenerList = new VisibilityListenerList();
    private final EnabledListenerList enabledListenerList = new EnabledListenerList();
    
    @Inject
    public LeftPanel(Navigator navigator, LibraryNavigator libraryNavigator) {
        GuiUtils.assignResources(this);

        setName("LeftPanel");

        setLayout(new MigLayout("insets 0, fill, gap 0"));
        JXPanel libraryNav = libraryNavigator.getComponent();
        libraryNav.setName("LeftPanel.contents");
        add(libraryNav, "top, grow");

        Line line = Line.createVerticalLine();
        line.setName("LeftPanel.rightBorder");
        add(line, "grow");
        
        navigator.addNavigationListener(new LeftPanelNavigationListener());
    }

    public void toggleVisibility() {
        setVisibility(!isVisible());
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
        setVisible(visible);
        visibilityListenerList.visibilityChanged(visible);
    }

    @Override
    public void addEnabledListener(EnabledListener listener) {
        enabledListenerList.addEnabledListener(listener);
    }

    @Override
    public void removeEnabledListener(EnabledListener listener) {
        enabledListenerList.removeEnabledListener(listener);
    }

    /**
     * Returns true if the component is enabled for use.  Always true. 
     */
    @Override
    public boolean isActionEnabled() {
        return true;
    }
    
    private class LeftPanelNavigationListener implements NavigationListener {

        @Override
        public void itemSelected(NavCategory category, NavItem navItem, NavSelectable selectable, JComponent panel) {
            setVisible(false);//(category == NavCategory.LIBRARY);
        }

        @Override
        public void categoryAdded(NavCategory category) {
            // Do nothing
        }

        @Override
        public void categoryRemoved(NavCategory category) {
            // Do nothing
        }

        @Override
        public void itemAdded(NavCategory category, NavItem navItem, JComponent panel) {
            // Do nothing
        }

        @Override
        public void itemRemoved(NavCategory category, NavItem navItem, JComponent panel) {
            // Do nothing
        }
    }

}
