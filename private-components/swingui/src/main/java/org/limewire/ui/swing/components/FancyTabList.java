package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
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

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.GroupLayout.Group;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

/** A horizontal list of {@link FancyTab FancyTabs}. */
public class FancyTabList extends JXPanel {
    
    private final List<TabActionMap> actionMaps = new ArrayList<TabActionMap>();
    private final List<FancyTab> visibleTabs = new ArrayList<FancyTab>();
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final ActionListener tabRemoveListener = new RemoveListener();
    
    private final JPanel view;
    private final AbstractButton moreOption;
    
    private FancyTabProperties props;
    private int maxTabs;
    
    private LayoutStyle layoutStyle;

    private int minimumWidth;
    private int maximumWidth;
    private int preferredWidth;
    
    private Insets tabInsets;
    
    public static enum LayoutStyle {
        FIXED, FLOWED;
    }
    
    public FancyTabList(Iterable<? extends TabActionMap> actions) {
        setOpaque(false);
        
        view = new JXPanel();
        view.setOpaque(false);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(view, gbc);
        
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        moreOption = new JXButton("more>>");
        moreOption.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean found = false;
                boolean moved = false;
                for(TabActionMap actions : actionMaps) {
                    if(found) {
                        moved = true;
                        actions.getSelectAction().putValue(Action.SELECTED_KEY, true);
                        break;
                    }
                    
                    if(actions.getSelectAction().getValue(Action.SELECTED_KEY).equals(Boolean.TRUE)) {
                        found = true;
                    }
                }
                if(!moved && !actionMaps.isEmpty()) {
                    actionMaps.get(0).getSelectAction().putValue(Action.SELECTED_KEY, true);
                }
                
            }
        });
        add(moreOption, gbc);
        moreOption.setVisible(false);
        
        minimumWidth = 30;
        maximumWidth = 150;
        preferredWidth = 150;
        layoutStyle = LayoutStyle.FIXED;
        maxTabs = Integer.MAX_VALUE;
        
        props = new FancyTabProperties();
        setActions(actions);
    }
    
    public FancyTabList(TabActionMap... actions) {
        this(Arrays.asList(actions));
    }

    /** Sets the maximum number of tabs to render at once. */
    public void setMaxTabs(int max) {
        this.maxTabs = max;
        layoutTabs();
    }
    
    /** Sets whether or not the tabs should render a 'remove' icon. */
    public void setRemovable(boolean removable) {
        props.setRemovable(removable);
        setActions(actionMaps);
    }

    /** Adds a new tab based on the given action at the specified index. */
    public void addActionAt(TabActionMap action, int i) {
        this.actionMaps.add(i, action);
        FancyTab tab = createAndPrepareTab(action);
        this.tabs.add(i, tab);
        layoutTabs();
    }

    /**
     * Removes the tab based on the given action. This will not trigger an
     * action from the {@link TabActionMap#getRemoveAction()} action.
     */
    public void removeAction(TabActionMap action) {
        actionMaps.remove(action);
        for(Iterator<FancyTab> iter = tabs.iterator(); iter.hasNext(); ) {
            FancyTab tab = iter.next();
            if(tab.getAction().equals(action)) {
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
    public void setActions(Iterable<? extends TabActionMap> newActions) {
        actionMaps.clear();
        
        for(FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();

        for(TabActionMap actions : newActions) {
            this.actionMaps.add(actions);
            FancyTab tab = createAndPrepareTab(actions);
            tabs.add(tab);
        }
        
        layoutTabs();
    }
    
    private FancyTab createAndPrepareTab(TabActionMap actions) {
        final FancyTab tab = new FancyTab(actions, tabGroup, props);
        tab.addRemoveActionListener(tabRemoveListener);
        actions.getSelectAction().addPropertyChangeListener(new PropertyChangeListener() {
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
    public void setActions(TabActionMap... actions) {
        setActions(Arrays.asList(actions));
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
        view.removeAll();
        
        switch(layoutStyle) {
        case FIXED:
            layoutFixed();
            break;
        case FLOWED:
            layoutFlowed();
            break;
        }
        
        visibleTabs.clear();
        for(int i = 0; i < view.getComponentCount(); i++) {
            visibleTabs.add((FancyTab)view.getComponent(i));
        }
        
        if(tabs.size() >= maxTabs) {
            moreOption.setVisible(true);
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
     * tabs, and the currently selected tab.
     * 
     * @return
     */
    private List<FancyTab> getPendingVisibleTabs() {
        if(maxTabs >= tabs.size()) {
            return tabs;
        } else {        
            FancyTab selectedTab = getSelectedTab();
            if(visibleTabs.contains(selectedTab)) {
                return visibleTabs;
            } else {
                int vizIdx = tabs.indexOf(visibleTabs.get(0));
                int selIdx = tabs.indexOf(selectedTab);
                if (vizIdx > selIdx) { // We have to shift left
                    return tabs.subList(selIdx, selIdx+maxTabs);
                } else { // We have to shift right
                    return tabs.subList(selIdx-maxTabs+1, selIdx+1);
                }
                
            }
        }
    }
    
    /**
     * Renders the tabs in a flowed layout, placing each tab next to each other,
     * using the given insets around each tab.
     */
    private void layoutFlowed() {
        view.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = tabInsets;
        for(FancyTab tab : getPendingVisibleTabs()) {
            view.add(tab, gbc);
        }
    }

    /**
     * Renders the tabs in a fixed layout, using the given minimum, preferred
     * and maximum width.
     */
    private void layoutFixed() {
        GroupLayout layout = new GroupLayout(view);
        view.setLayout(layout);

        layout.setAutoCreateGaps(false);
        layout.setAutoCreateContainerGaps(false);
        
        Group horGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(horGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(verGroup));
        
        for(FancyTab tab : getPendingVisibleTabs()) {
            horGroup.addComponent(tab, minimumWidth, preferredWidth, maximumWidth);
            verGroup.addComponent(tab);
        }
    }
    
    /**
     * Sets the painter to be used when the tab is rolled over.
     */
    public void setHighlightPainter(Painter<?> highlightPainter) {
        for (FancyTab tab : tabs) {
            if (tab.isHighlighted()) {
                tab.setBackgroundPainter(highlightPainter);
            }
        }
        props.setHighlightPainter(highlightPainter);
    }
    
    /** Sets the painter to be used when the tab is selected. */
    public void setSelectionPainter(Painter<?> selectedPainter) {
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
        @Override
        public void actionPerformed(ActionEvent e) {
            FancyTab tab = (FancyTab)e.getSource();
            boolean selected = tab.isSelected();
            int idx = actionMaps.indexOf(tab.getAction());
            assert idx != -1;
            actionMaps.remove(idx);
            tabs.remove(tab);
            tab.removeFromGroup(tabGroup);
            layoutTabs();
            
            // Shift the selection to the tab to the left (or right, if idx==0)
            if(selected) {
                if(idx == 0 && actionMaps.size() > 0) {
                    actionMaps.get(0).getSelectAction().putValue(Action.SELECTED_KEY, true);
                } else  if(idx > 0 && actionMaps.size() > 0) {
                    actionMaps.get(idx - 1).getSelectAction().putValue(Action.SELECTED_KEY, true);
                }
            }
        }
    }
}
