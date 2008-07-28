package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Group;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

/** A horizontal list of {@link FancyTab FancyTabs}. */
public class FancyTabList extends JXPanel {
    
    private final List<TabActionMap> actions = new ArrayList<TabActionMap>();
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final ActionListener tabRemoveListener = new RemoveListener();
    
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
        
        minimumWidth = 30;
        maximumWidth = 150;
        preferredWidth = 150;
        layoutStyle = LayoutStyle.FIXED;
        
        props = new FancyTabProperties();
        maxTabs = Integer.MAX_VALUE;
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
        setActions(actions);
    }

    /** Adds a new tab based on the given action at the specified index. */
    public void addActionAt(TabActionMap action, int i) {
        this.actions.add(i, action);
        FancyTab tab = new FancyTab(action, tabGroup, props);
        tab.addRemoveActionListener(tabRemoveListener);
        this.tabs.add(i, tab);
        layoutTabs();
    }

    /**
     * Removes the tab based on the given action. This will not trigger an
     * action from the {@link TabActionMap#getRemoveAction()} action.
     */
    public void removeAction(TabActionMap action) {
        actions.remove(action);
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
        actions.clear();
        
        for(FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();

        for(TabActionMap action : newActions) {
            this.actions.add(action);
            FancyTab tab = new FancyTab(action, tabGroup, props);
            tab.addRemoveActionListener(tabRemoveListener);
            tabs.add(tab);
        }
        
        layoutTabs();
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
     * Renders the tabs in a flowed layout, placing each tab next to each other,
     * using the given insets around each tab.
     */
    private void layoutFlowed() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = tabInsets;
        int i = 0;
        for(FancyTab tab : tabs) {
            if(i < maxTabs) {
                add(tab, gbc);
            }
            i++;
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
        
        Group seqGroup = layout.createSequentialGroup();
        layout.setHorizontalGroup(seqGroup);
        
        Group verGroup = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(verGroup));
        
        int i = 0;
        for(FancyTab tab : tabs) {
            if(i < maxTabs) {
                seqGroup.addComponent(tab, minimumWidth, preferredWidth, maximumWidth);
                verGroup.addComponent(tab);
            }
            i++;
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
    }
    
    private class RemoveListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            FancyTab tab = (FancyTab)e.getSource();
            boolean selected = tab.isSelected();
            int idx = actions.indexOf(tab.getAction());
            assert idx != -1;
            actions.remove(idx);
            tabs.remove(tab);
            tab.removeFromGroup(tabGroup);
            layoutTabs();
            
            // Shift the selection to the tab to the left (or right, if idx==0)
            if(selected) {
                if(idx == 0 && actions.size() > 0) {
                    actions.get(0).getSelectAction().putValue(Action.SELECTED_KEY, true);
                } else  if(idx > 0 && actions.size() > 0) {
                    actions.get(idx - 1).getSelectAction().putValue(Action.SELECTED_KEY, true);
                }
            }
        }
    }
}
