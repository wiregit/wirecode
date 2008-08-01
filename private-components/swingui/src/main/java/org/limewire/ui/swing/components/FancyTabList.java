package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.GroupLayout.Group;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.ui.swing.util.GuiUtils;

/** A horizontal list of {@link FancyTab FancyTabs}. */
public class FancyTabList extends JXPanel {
    
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private int vizStartIdx = -1;
        
    private FancyTabProperties props;
    private int maxTabs;
    
    private LayoutStyle layoutStyle;

    private int minimumWidth;
    private int maximumWidth;
    private int preferredWidth;
    
    private Insets tabInsets;
        
    @Resource
    private Icon moreTriangle;
    
    public static enum LayoutStyle {
        FIXED, FLOWED;
    }
    
    public FancyTabList(Iterable<? extends TabActionMap> actionMaps) {
        GuiUtils.assignResources(this);
        setOpaque(false);
                
        minimumWidth = 30;
        maximumWidth = 150;
        preferredWidth = 150;
        layoutStyle = LayoutStyle.FIXED;
        maxTabs = Integer.MAX_VALUE;
        
        props = new FancyTabProperties();
        setTabActionMaps(actionMaps);
    }
    
    public FancyTabList(TabActionMap... actionMaps) {
        this(Arrays.asList(actionMaps));
    }

    /** Sets the maximum number of tabs to render at once. */
    public void setMaxTabs(int max) {
        this.maxTabs = max;
        layoutTabs();
    }
    
    /** Sets whether or not the tabs should render a 'remove' icon. */
    public void setRemovable(boolean removable) {
        props.setRemovable(removable);
        recreateTabs();
    }

    /** Adds a new tab based on the given action at the specified index. */
    public void addTabActionMapAt(TabActionMap actionMap, int i) {
        FancyTab tab = createAndPrepareTab(actionMap);
        this.tabs.add(i, tab);
        layoutTabs();
    }

    /**
     * Removes the tab based on the given action. This will not trigger an
     * action from the {@link TabActionMap#getRemoveAction()} action.
     */
    public void removeTabActionMap(TabActionMap actionMap) {
        for(Iterator<FancyTab> iter = tabs.iterator(); iter.hasNext(); ) {
            FancyTab tab = iter.next();
            if(tab.getTabActionMap().equals(actionMap)) {
                tab.removeFromGroup(tabGroup);
                iter.remove();
                break;
            }
        }
        layoutTabs();
    }
    
    /**
     * Sets a new list of tabs based on the given actions.
     */
    public void setTabActionMaps(Iterable<? extends TabActionMap> newActionMaps) {
        for(FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();

        for(TabActionMap actions : newActionMaps) {
            FancyTab tab = createAndPrepareTab(actions);
            tabs.add(tab);
        }
        
        layoutTabs();
    }
    
    private void recreateTabs() {
        List<TabActionMap> actionMaps = new ArrayList<TabActionMap>(tabs.size());
        for(FancyTab tab : tabs) {
            actionMaps.add(tab.getTabActionMap());
        }
        setTabActionMaps(actionMaps);
    }
    
    private FancyTab createAndPrepareTab(TabActionMap actionMaps) {
        final FancyTab tab = new FancyTab(actionMaps, tabGroup, props);
        tab.addRemoveActionListener(new RemoveListener(tab));
        actionMaps.getSelectAction().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if(evt.getNewValue().equals(Boolean.TRUE)) {
                        layoutTabs();
                    }
                }
            }
        });
        return tab;
    }

    /**
     * Sets a new list of tabs based on the given actions.
     */
    public void setTabActionMaps(TabActionMap... actionMaps) {
        setTabActionMaps(Arrays.asList(actionMaps));
    }
    
    /**
     * Renders the tabs in a fixed layout, using the given minimum, preferred
     * and maximum width.
     */
    public void setFixedLayout(int min, int pref, int max) {
        this.layoutStyle = LayoutStyle.FIXED;
        this.minimumWidth = min;
        this.maximumWidth = max;
        this.preferredWidth = pref;
        layoutTabs();
    }
    
    /**
     * Renders the tabs in a flowed layout, placing each tab next to each other,
     * using the given insets around each tab.
     */
    public void setFlowedLayout(Insets insets) {
        this.layoutStyle = LayoutStyle.FLOWED;
        if(insets != null) {
            this.tabInsets = insets;
        } else {
            this.tabInsets = new Insets(0, 1, 5, 1);
        }
        layoutTabs();
    }
    
    /** Removes all visible tabs and lays them out again. */
    private void layoutTabs() {
        removeAll();
        
        switch(layoutStyle) {
        case FIXED:
            layoutFixed();
            break;
        case FLOWED:
            layoutFlowed();
            break;
        }
    }
    
    /**
     * Returns the currently selected tab.
     */
    private FancyTab getSelectedTab() {
        for(FancyTab tab : tabs) {
            if(tab.isSelected()) {
                return tab;
            }
        }
        return null;
    }
    
    /**
     * Returns the tabs that *should* be visible, based on the currently visible
     * tabs, and the currently selected tab.  This keeps state and assumes the
     * tabs it returns will become visible.
     */
    private List<FancyTab> getPendingVisibleTabs() {
        List<FancyTab> vizTabs;
        if(maxTabs >= tabs.size()) {
            vizStartIdx = 0;
            vizTabs = tabs;
        } else {        
            FancyTab selectedTab = getSelectedTab();
            if(tabs.size() - vizStartIdx < maxTabs) {
                vizStartIdx = tabs.size() - maxTabs;
            }
            vizTabs = tabs.subList(vizStartIdx, vizStartIdx + maxTabs);
            if(!vizTabs.contains(selectedTab)) {
                int selIdx = tabs.indexOf(selectedTab);
                if (vizStartIdx > selIdx) { // We have to shift left
                    vizStartIdx = selIdx;
                } else { // We have to shift right
                    vizStartIdx = selIdx-maxTabs+1;
                }
                vizTabs = tabs.subList(vizStartIdx, vizStartIdx+maxTabs);
            }
        }
        return vizTabs;
    }
    
    /**
     * Renders the tabs in a flowed layout, placing each tab next to each other,
     * using the given insets around each tab.
     */
    private void layoutFlowed() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = tabInsets;
        for(FancyTab tab : getPendingVisibleTabs()) {
            add(tab, gbc);
        }

        if(tabs.size() > maxTabs) {
            add(new FancyTabMoreButton(tabs, moreTriangle, props), gbc);
        }
    }

    /**
     * Renders the tabs in a fixed layout, using the given minimum, preferred
     * and maximum width.
     */
    private void layoutFixed() {
        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        
        Group horGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(horGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.CENTER);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(verGroup));
        
        for(FancyTab tab : getPendingVisibleTabs()) {
            horGroup.addComponent(tab, minimumWidth, preferredWidth, maximumWidth);
            verGroup.addComponent(tab, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        }
        
        if(tabs.size() > maxTabs) {
            JComponent more = new FancyTabMoreButton(tabs, moreTriangle, props);
            horGroup.addComponent(more);
            verGroup.addComponent(more);
        }
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
        for(FancyTab tab : tabs) {
            if(tab.isHighlighted()) {
                tab.setBackgroundPainter(selectedPainter);
            }
        }
        props.setSelectedPainter(selectedPainter);
    }
    
    /** Sets the color used to render the tab's text when it is not selected. */
    public void setTabTextColor(Color normalColor) {
        for(FancyTab tab : tabs) {
            if(!tab.isSelected()) {
                tab.setButtonForeground(normalColor);
            }
        }
        props.setNormalColor(normalColor);
    }
    
    /** Sets the color used to render the tab's text when it is selected. */
    public void setTabTextSelectedColor(Color selectionColor) {
        for(FancyTab tab : tabs) {
            if(tab.isSelected()) {
                tab.setButtonForeground(selectionColor);
            }
        }
        props.setSelectionColor(selectionColor);
    }
    
    /** Sets the font used to render the tab's text. */
    public void setTextFont(Font font) {
        for(FancyTab tab : tabs) {
            tab.setFont(font);
        }
        props.setTextFont(font);
    }
    
    private class RemoveListener implements ActionListener {
        private final FancyTab tab;
        
        public RemoveListener(FancyTab tab) {
            this.tab = tab;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean selected = tab.isSelected();
            int idx = tabs.indexOf(tab);
            assert idx != -1;
            tabs.remove(tab);
            tab.removeFromGroup(tabGroup);
            
            // Shift the selection to the tab to the left (or right, if idx==0)
            if(selected && !tabs.isEmpty()) {
                if(idx == 0 && tabs.size() > 0) {
                    tabs.get(0).getTabActionMap().getSelectAction().putValue(Action.SELECTED_KEY, true);
                } else if(idx > 0 && tabs.size() > 0) {
                    tabs.get(idx - 1).getTabActionMap().getSelectAction().putValue(Action.SELECTED_KEY, true);
                }
            } else {            
                layoutTabs();
            }
        }
    }
}
