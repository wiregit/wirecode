package org.limewire.ui.swing.library.nav;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

class NavList extends JXPanel {
    
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();
    
    private final NavPanelMoveAction panelMoveUpAction;
    private final NavPanelMoveAction panelMoveDownAction;
    
    private final JXLabel titleLabel;
    private final JXCollapsiblePane collapsablePanels;
    private final JXPanel panelContainer;
    
    NavList() {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("gap 0, insets 0, fill"));
        
        this.panelMoveUpAction = new NavPanelMoveAction(false);
        this.panelMoveDownAction = new NavPanelMoveAction(true);
        
        this.titleLabel = new ActionLabel(new AbstractAction("Nav List Title") {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapsablePanels.setCollapsed(!collapsablePanels.isCollapsed());
            }
        }, false);
        FontUtils.bold(titleLabel);
        titleLabel.setName("LibraryNavigator.NavListTitle");
        add(titleLabel, "gapleft 5, alignx left, growx, wrap");
        
        panelContainer = new JXPanel(new MigLayout("gap 0, insets 0, fillx"));
        collapsablePanels = new JXCollapsiblePane();
        collapsablePanels.setContentPane(panelContainer);
        add(collapsablePanels, "alignx left, aligny top, growx, wrap");
        
        checkVisibility();
    }
    
    private void checkVisibility() {
        if(navPanels.isEmpty()) {
            setVisible(false);
        } else {
            setVisible(true);
        }
        
        invalidate();
        repaint();
    }
    
    void setTitleText(String text) {
        titleLabel.setText(text);
    }

    List<NavPanel> clear() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        for(NavPanel panel : oldPanels) {
            removePanel(panel);
        }
        return oldPanels;
    }
    
    List<NavPanel> clearFriends() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        List<NavPanel> removed = new ArrayList<NavPanel>();
        for(NavPanel panel : oldPanels) {
            if(!panel.getFriend().isAnonymous()) {
                removePanel(panel);
                removed.add(panel);
            }
        }
        return removed;
    }
    
    NavPanel updateNavPanelForFriend(Friend friend, LibraryState state, EventList<RemoteFileItem> eventList) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            panel.updateLibraryState(state);
            panel.updateLibrary(eventList, state);
        }
        return panel;
    }
    
    NavPanel ensureFriendVisible(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            collapsablePanels.setCollapsed(false);
            collapsablePanels.scrollRectToVisible(panel.getBounds());
        }
        return panel;
        
    }
    
    NavPanel getPanelForFriend(Friend friend) {
        for(NavPanel panel : navPanels) {
            if(panel.getFriend().getId().equals(friend.getId())) {
                return panel;
            }
        }
        return null;
    }    

    void addNavPanel(NavPanel panel) {
        // Find the index where to insert.
        int idx = Collections.binarySearch(navPanels, panel, new Comparator<NavPanel>() {
            @Override
            public int compare(NavPanel o1, NavPanel o2) {
                Friend f1 = o1.getFriend();
                Friend f2 = o2.getFriend();
                if(o1 == o2) {
                    return 0;
                } else if(f1.isAnonymous() && !f2.isAnonymous()) {
                    return 1;
                } else if(f2.isAnonymous() && !f1.isAnonymous()) {
                    return -1;
                } else {           
                    return f1.getRenderName().compareToIgnoreCase(f2.getRenderName());
                }
            }
        });
        int insertIdx = idx >= 0 ? idx : -(idx+1);
        navPanels.add(insertIdx, panel);
        panelContainer.add(panel, "alignx left, aligny top, growx, wrap", insertIdx);

        panel.getActionMap().put(NavKeys.MOVE_DOWN, panelMoveDownAction);
        panel.getActionMap().put(NavKeys.MOVE_UP, panelMoveUpAction);
        
        checkVisibility();
    }
       
    private NavPanel moveDown() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                if(iter.hasNext()) {
                    panel = iter.next();
                    panel.select();
                    return panel;
                }
                break;
            }
        }
        return null;
    }
    
    private void moveDownFromThis() {
        // If we couldn't move down, tell the whole list to move
        Action action = NavList.this.getActionMap().get(NavKeys.MOVE_DOWN);
        if(action != null) {
            action.actionPerformed(null);
        }
    }
    
    private void moveUpFromThis() {
        // If we couldn't move up, tell the whole list to move.
        Action action = NavList.this.getActionMap().get(NavKeys.MOVE_UP);
        if(action != null) {
            action.actionPerformed(null);
        }
    }
     
    private NavPanel moveUp() {
        ListIterator<NavPanel> iter = navPanels.listIterator();
        while(iter.hasNext()) {
            NavPanel panel = iter.next();
            if(panel.hasSelection()) {
                iter.previous();
                if(iter.hasPrevious()) {
                    panel = iter.previous();
                    panel.select();
                    return panel;
                }
                break;
            }
        }
        return null;
    }
    
    void removePanel(NavPanel panel) {
        navPanels.remove(panel);
        panelContainer.remove(panel);
        checkVisibility();
    }
    
    NavPanel removePanelForFriend(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            removePanel(panel);
        }
        return panel;
    }
    
    NavPanel selectFirst() {
        if(!navPanels.isEmpty()) {
            NavPanel panel = navPanels.get(0);
            panel.select();
            return panel;
        } else {
            moveDownFromThis();
            return null;
        }
    }

    NavPanel selectLast() {
        if(!navPanels.isEmpty()) {
            NavPanel panel = navPanels.get(navPanels.size()-1);
            panel.select();
            return panel;
        } else {
            moveUpFromThis();
            return null;
        }
    }
    
    NavPanel selectFriendLibrary(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            panel.select();
        }
        return panel;
    } 
    
    private class NavPanelMoveAction extends AbstractAction {
        private final boolean moveDown;
        
        NavPanelMoveAction(boolean down) {
            this.moveDown = down;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(moveDown) {
                if(moveDown() == null) {
                    moveDownFromThis();
                }
            } else {
                if(moveUp() == null) {
                    moveUpFromThis();
                }
            }
        }
    }

}
