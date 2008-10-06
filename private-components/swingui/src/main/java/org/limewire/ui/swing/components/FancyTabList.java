package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.util.GuiUtils;

/** 
 * A horizontal list of {@link FancyTab FancyTabs}.
 * TODO: Support vertical too, if we need it.
 */
public class FancyTabList extends JXPanel {
    
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private int vizStartIdx = -1;
    
    private final Action closeOtherAction;
    private final Action closeAllAction;
        
    private FancyTabProperties props;
    private int maxTotalTabs = 10;
    private int maxVisibleTabs;
        
    @Resource
    private Icon moreTriangle;
    
    public FancyTabList(Iterable<? extends TabActionMap> actionMaps) {
        GuiUtils.assignResources(this);
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, filly, hidemode 2"));  
                
        maxVisibleTabs = Integer.MAX_VALUE;
        
        props = new FancyTabProperties();
        closeOtherAction = new CloseOther();
        closeAllAction = new CloseAll();
        
        setTabActionMaps(actionMaps);
    }
    
    public FancyTabList(TabActionMap... actionMaps) {
        this(Arrays.asList(actionMaps));
    }

    /** Adds a new tab based on the given action at the specified index. */
    public void addTabActionMapAt(TabActionMap actionMap, int i) {
        int size = tabs.size();
        if (size == maxTotalTabs) {
            tabs.remove(size - 1); // remove the last tab
        }

        FancyTab tab = createAndPrepareTab(actionMap);
        tabs.add(i, tab);
        layoutTabs();
    }

    private FancyTab createAndPrepareTab(TabActionMap actionMap) {
        final FancyTab tab = new FancyTab(actionMap, tabGroup, props);
        tab.addRemoveActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeTab(tab);
            }
        });
        actionMap.setRemoveAll(closeAllAction);
        actionMap.setRemoveOthers(closeOtherAction);
        actionMap.getMainAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        layoutTabs();
                    }
                }
            }
        });
        return tab;
    }
    
    /**
     * Returns the tabs that *should* be visible, based on the currently visible
     * tabs, and the currently selected tab.  This keeps state and assumes the
     * tabs it returns will become visible.
     */
    private List<FancyTab> getPendingVisibleTabs() {
        List<FancyTab> vizTabs;
        if (maxVisibleTabs >= tabs.size()) {
            vizStartIdx = 0;
            vizTabs = tabs;
        } else {        
            FancyTab selectedTab = getSelectedTab();
            if (tabs.size() - vizStartIdx < maxVisibleTabs) {
                vizStartIdx = tabs.size() - maxVisibleTabs;
            }
            vizTabs = tabs.subList(vizStartIdx, vizStartIdx + maxVisibleTabs);
            if (!vizTabs.contains(selectedTab)) {
                int selIdx = tabs.indexOf(selectedTab);
                if (vizStartIdx > selIdx) { // We have to shift left
                    vizStartIdx = selIdx;
                } else { // We have to shift right
                    vizStartIdx = selIdx - maxVisibleTabs + 1;
                }
                vizTabs = tabs.subList(vizStartIdx, vizStartIdx+maxVisibleTabs);
            }
        }
        return vizTabs;
    }
    
    /**
     * Returns the currently selected tab.
     */
    public FancyTab getSelectedTab() {
        for (FancyTab tab : tabs) {
            if (tab.isSelected()) {
                return tab;
            }
        }
        return null;
    }

    /**
     * Returns all tabs.
     */
    public List<FancyTab> getTabs() {
        return Collections.unmodifiableList(tabs);
    }

    private void recreateTabs() {
        List<TabActionMap> actionMaps = new ArrayList<TabActionMap>(tabs.size());
        for (FancyTab tab : tabs) {
            actionMaps.add(tab.getTabActionMap());
        }
        setTabActionMaps(actionMaps);
    }
    
    /**
     * Removes the tab from the layout.
     * This does not trigger any listeners on the tab's removal.
     */
    private void removeTab(FancyTab tab) {
        boolean selected = tab.isSelected();
        int idx = tabs.indexOf(tab);
        assert idx != -1;
        tabs.remove(tab);
        tab.removeFromGroup(tabGroup);
        
        // Shift the selection to the tab to the left (or right, if idx==0)
        if (selected && !tabs.isEmpty()) {
            // Selecting a tab will trigger a layout.
            if (idx == 0 && tabs.size() > 0) {
                tabs.get(0).getTabActionMap().getMainAction().putValue(Action.SELECTED_KEY, true);
            } else if (idx > 0 && tabs.size() > 0) {
                tabs.get(idx - 1).getTabActionMap().getMainAction().putValue(Action.SELECTED_KEY, true);
            } // else empty, no need to layout.
        } else {            
            layoutTabs();
        }
    }

    /**
     * Removes the tab based on the given action. This will not trigger an
     * action from the {@link TabActionMap#getRemoveAction()} action.
     */
    public void removeTabActionMap(TabActionMap actionMap) {
        for (Iterator<FancyTab> iter = tabs.iterator(); iter.hasNext(); ) {
            FancyTab tab = iter.next();
            if (tab.getTabActionMap().equals(actionMap)) {
                tab.removeFromGroup(tabGroup);
                iter.remove();
                break;
            }
        }
        layoutTabs();
    }
    public void setMaxTotalTabs(int max) {
        this.maxTotalTabs = max;
    }
    
    /** Sets the maximum number of tabs to render at once. */
    public void setMaxVisibleTabs(int max) {
        this.maxVisibleTabs = max;
        layoutTabs();
    }
    
    /** Sets whether or not the tabs should render a 'remove' icon. */
    public void setRemovable(boolean removable) {
        props.setRemovable(removable);
        recreateTabs();
    }

    /**
     * Sets a new list of tabs based on the given actions.
     */
    public void setTabActionMaps(Iterable<? extends TabActionMap> newActionMaps) {
        for (FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();

        for (TabActionMap actions : newActionMaps) {
            FancyTab tab = createAndPrepareTab(actions);
            tabs.add(tab);
        }
        
        layoutTabs();
    }
    
    /**
     * Sets a new list of tabs based on the given actions.
     */
    public void setTabActionMaps(TabActionMap... actionMaps) {
        setTabActionMaps(Arrays.asList(actionMaps));
    }
    
    /**
     * Set the visibility of all the tabs.
     * @param visible true to make visible; false otherwise
     */
    public void setTabsVisible(boolean visible) {
        for (FancyTab tab : tabs) {
            tab.setVisible(visible);
        }
    }

    /** Removes all visible tabs and lays them out again. */
    private void layoutTabs() {
        removeAll();      
        for (FancyTab tab : getPendingVisibleTabs()) {
            add(tab, "growy");
        }        
        if (tabs.size() > maxVisibleTabs) {
            JComponent more = new FancyTabMoreButton(tabs, moreTriangle, props);
            add(more);
        }
        revalidate();
        repaint();
    }
    
    public void setCloseAllText(String closeAllText) {
        props.setCloseAllText(closeAllText);
        closeAllAction.putValue(Action.NAME, closeAllText);
    }

    public void setCloseOneText(String closeOneText) {
        props.setCloseOneText(closeOneText);
    }

    public void setCloseOtherText(String closeOtherText) {
        props.setCloseOtherText(closeOtherText);
        closeOtherAction.putValue(Action.NAME, closeOtherText);
    }
    
    public void setUnderlineColor(Color underlineColor) {
        props.setUnderlineColor(underlineColor);
    }
    
    public void setUnderlineHoverColor(Color underlineHoverColor) {
        props.setUnderlineHoverColor(underlineHoverColor);
    }

    /**
     * Sets the painter to be used when the tab is rolled over.
     */
    public void setHighlightPainter(Painter<JXButton> highlightPainter) {
        for (FancyTab tab : tabs) {
            if (tab.isHighlighted()) {
                tab.setBackgroundPainter(highlightPainter);
            }
        }
        props.setHighlightPainter(highlightPainter);
    }
    
    /** Sets the painter to be used when the tab is selected. */
    public void setSelectionPainter(Painter<JXButton> selectedPainter) {
        for (FancyTab tab : tabs) {
            if (tab.isHighlighted()) {
                tab.setBackgroundPainter(selectedPainter);
            }
        }
        props.setSelectedPainter(selectedPainter);
    }
    
    /** Sets the color used to render the tab's text when it is not selected. */
    public void setTabTextColor(Color normalColor) {
        for (FancyTab tab : tabs) {
            if (!tab.isSelected()) {
                tab.setButtonForeground(normalColor);
            }
        }
        props.setNormalColor(normalColor);
    }
    
    /** Sets the color used to render the tab's text when it is selected. */
    public void setTabTextSelectedColor(Color selectionColor) {
        for (FancyTab tab : tabs) {
            if (tab.isSelected()) {
                tab.setButtonForeground(selectionColor);
            }
        }
        props.setSelectionColor(selectionColor);
    }
    
    /** Sets the font used to render the tab's text. */
    public void setTextFont(Font font) {
        for (FancyTab tab : tabs) {
            tab.setTextFont(font);
        }
        props.setTextFont(font);
    }
    
    private class CloseAll extends AbstractAction {
        public CloseAll() {
            super(props.getCloseAllText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            while(!tabs.isEmpty()) {
                tabs.get(0).remove();
            }
        }
    }
    
    private class CloseOther extends AbstractAction {
        public CloseOther() {
            super(props.getCloseOtherText());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            while(tabs.size() > 1) {
                FancyTab tab = tabs.get(0);
                if (isFrom(tab, (Component)e.getSource())) {
                    tab = tabs.get(1);
                }
                tab.remove();
            }
        }
        
        private boolean isFrom(JComponent parent, Component child) {
            while(child.getParent() != null) {
                child = child.getParent();
                if (child instanceof JPopupMenu) {
                    child = ((JPopupMenu)child).getInvoker();
                }
                
                if (child == parent) {
                    return true;
                }
            }
            return false;
        }
    }
}
