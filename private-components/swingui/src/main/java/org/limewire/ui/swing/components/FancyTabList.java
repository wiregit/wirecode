package org.limewire.ui.swing.components;

import java.util.Arrays;
import java.util.List;

import net.miginfocom.swing.MigLayout;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/** 
 * A horizontal list of {@link FancyTab FancyTabs}.
 */
public class FancyTabList extends AbstractTabList {
    
    @AssistedInject
    FancyTabList(@Assisted Iterable<? extends TabActionMap> actionMaps) {
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, filly, hidemode 2"));  
        
        setTabActionMaps(actionMaps);
    }
    
    @AssistedInject
    FancyTabList(@Assisted TabActionMap... actionMaps) {
        this(Arrays.asList(actionMaps));
    }
    
    /**
     * Set the visibility of all the tabs.
     * @param visible true to make visible; false otherwise
     */
    public void setTabsVisible(boolean visible) {
        List<FancyTab> tabs = getTabs();
        for (FancyTab tab : tabs) {
            tab.setVisible(visible);
        }
    }

    /** Removes all visible tabs and lays them out again. */
    @Override
    protected void layoutTabs() {
        removeAll();      
        for (FancyTab tab : getTabs()) {
            add(tab, "growy");
        }        

        revalidate();
        repaint();
    }

    public void setUnderlineEnabled(boolean enabled) {
        List<FancyTab> tabs = getTabs();
        for (FancyTab tab : tabs) {
            tab.setUnderlineEnabled(enabled);
        }
        getTabProperties().setUnderlineEnabled(enabled);
    }
}
