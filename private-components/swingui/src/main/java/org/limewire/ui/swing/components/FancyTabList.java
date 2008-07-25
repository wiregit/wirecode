package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Action;
import javax.swing.ButtonGroup;

import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;

public class FancyTabList extends JXPanel {
    
    private final List<Action> actions = new ArrayList<Action>();
    private final List<FancyTab> tabs = new ArrayList<FancyTab>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    
    private FancyTabProperties props;
    private Insets tabInsets;
    private int maxActionsToList;
    
    public FancyTabList(Iterable<? extends Action> actions) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        props = new FancyTabProperties();
        tabInsets = new Insets(0, 1, 5, 1);
        maxActionsToList = 10;
        setActions(actions);
    }
    
    public FancyTabList(Action... actions) {
        this(Arrays.asList(actions));
    }
    
    public void setMaxActionsToList(int max) {
        this.maxActionsToList = max;
        setActions(actions);
    }
    
    public void setButtonInsets(Insets insets) {
        tabInsets = insets;
        removeAll();
        addTabs();
    }
    
    public void setActions(Iterable<? extends Action> actions) {
        this.actions.clear();
        clearTab();

        int i = 0;
        for(Action action : actions) {
            this.actions.add(action);
            if(i < maxActionsToList) {
                tabs.add(new FancyTab(action, tabGroup, props));
            }
            i++;
        }
        addTabs();
    }
    
    public void setActions(Action... actions) {
        setActions(Arrays.asList(actions));
    }
    
    private void addTabs() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = tabInsets;
        gbc.ipadx = 50;
        for(FancyTab tab : tabs) {
          //  tab.setPreferredSize(new Dimension(200, 50));
            add(tab, gbc);
        }
        
//        if(actions.size() > maxActionsToList) {
//            add(new ButtonBed())
//        }
    }
    
    public void clearTab() {
        removeAll();        
        for(FancyTab tab : tabs) {
            tab.removeFromGroup(tabGroup);
        }
        tabs.clear();
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
