package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
    
    private final List<Action> actions = new ArrayList<Action>();
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    
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
    
    public FancyTabList(Iterable<? extends Action> actions) {
        setOpaque(false);
        
        minimumWidth = 30;
        maximumWidth = 150;
        preferredWidth = 150;
        layoutStyle = LayoutStyle.FIXED;
        
        props = new FancyTabProperties();
        maxTabs = Integer.MAX_VALUE;
        setActions(actions);
    }
    
    public FancyTabList(Action... actions) {
        this(Arrays.asList(actions));
    }
    
    public void setMaxTabs(int max) {
        this.maxTabs = max;
        layoutTabs();
    }
    
    public void setRemovable(boolean removable) {
        props.setRemovable(removable);
        setActions(actions);
    }

    public void addActionAt(Action action, int i) {
        this.actions.add(i, action);
        this.tabs.add(i, new FancyTab(action, tabGroup, props));
        layoutTabs();
    }

    public void removeAction(Action action) {
        this.actions.remove(action);
        for(Iterator<FancyTab> iter = tabs.iterator(); iter.hasNext(); ) {
            if(iter.next().getAction().equals(action)) {
                iter.remove();
                break;
            }
        }
        layoutTabs();
    }
    
    
    public void setActions(Iterable<? extends Action> actions) {
        this.actions.clear();
        this.tabs.clear();

        for(Action action : actions) {
            this.actions.add(action);
            tabs.add(new FancyTab(action, tabGroup, props));
        }
        
        layoutTabs();
    }
    
    public void setActions(Action... actions) {
        setActions(Arrays.asList(actions));
    }
    
    public List<Action> getActions() {
        return new ArrayList<Action>(actions);
    }
    
    public void setFixedLayout(int min, int pref, int max) {
        this.layoutStyle = LayoutStyle.FIXED;
        this.minimumWidth = min;
        this.maximumWidth = max;
        this.preferredWidth = pref;
        layoutTabs();
    }
    
    public void setFlowedLayout(Insets insets) {
        this.layoutStyle = LayoutStyle.FLOWED;
        if(insets != null) {
            this.tabInsets = insets;
        } else {
            this.tabInsets = new Insets(0, 1, 5, 1);
        }
        layoutTabs();
    }
    
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
    
    public void setHighlightPainter(Painter<?> highlightPainter) {
        for(FancyTab tab : tabs) {
            if(tab.isHighlighted()) {
                tab.setBackgroundPainter(highlightPainter);
            }
        }
        props.setHighlightPainter(highlightPainter);
    }
    
    public void setSelectionPainter(Painter<?> selectedPainter) {
        for(FancyTab tab : tabs) {
            if(tab.isHighlighted()) {
                tab.setBackgroundPainter(selectedPainter);
            }
        }
        props.setSelectedPainter(selectedPainter);
    }
    
    public void setActionForeground(Color normalColor) {
        for(FancyTab tab : tabs) {
            if(!tab.isSelected()) {
                tab.setButtonForeground(normalColor);
            }
        }
        props.setNormalColor(normalColor);
    }
    
    public void setSelectionColor(Color selectionColor) {
        for(FancyTab tab : tabs) {
            if(tab.isSelected()) {
                tab.setButtonForeground(selectionColor);
            }
        }
        props.setSelectionColor(selectionColor);
    }
    
    public void setTextFont(Font font) {
        for(FancyTab tab : tabs) {
            tab.setFont(font);
        }
    }
}
