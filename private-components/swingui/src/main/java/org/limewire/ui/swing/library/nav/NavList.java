package org.limewire.ui.swing.library.nav;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.library.FriendLibraryMediatorFactory;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

class NavList extends JXPanel {
    
    private final List<NavPanel> navPanels = new ArrayList<NavPanel>();
    private final ShareListManager shareListManager;
    private final FriendLibraryMediatorFactory friendLibraryBaseFactory;
    private final NavPanelFactory navPanelFactory;
    
    private final Navigator navigator;
    
    private final NavPanelMoveAction panelMoveUpAction;
    private final NavPanelMoveAction panelMoveDownAction;
    
    private final JXLabel titleLabel;
    private final JXCollapsiblePane collapsablePanels;
    private final JXPanel panelContainer;
    
    @Inject NavList(Navigator navigator,
            ShareListManager shareListManager,
            FriendLibraryMediatorFactory friendLibraryBaseFactory,
            NavPanelFactory navPanelFactory,
            LibraryNavigator libraryNavigator) {
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("gap 0, insets 0, fill"));
        
        this.navigator = navigator;
        this.shareListManager = shareListManager;
        this.friendLibraryBaseFactory = friendLibraryBaseFactory;
        this.navPanelFactory = navPanelFactory;
        
        this.panelMoveUpAction = new NavPanelMoveAction(false);
        this.panelMoveDownAction = new NavPanelMoveAction(true);
        
        this.titleLabel = new ActionLabel(new AbstractAction("Nav List Title") {
            @Override
            public void actionPerformed(ActionEvent e) {
                collapsablePanels.setCollapsed(!collapsablePanels.isCollapsed());
            }
        }, false);
        titleLabel.setName("LibraryNavigator.NavListTitle");
        add(titleLabel, "gapleft 5, alignx left, growx, wrap");
        
        panelContainer = new JXPanel(new MigLayout("gap 0, insets 0")) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = NavList.this.getWidth();
                return d;
            }
        };
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

    void clear() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        for(NavPanel panel : oldPanels) {
            removeNavPanelForFriend(panel.getFriend());
        }
    }
    
    void clearFriends() {
        List<NavPanel> oldPanels = new ArrayList<NavPanel>(navPanels);
        for(NavPanel panel : oldPanels) {
            if(!panel.getFriend().isAnonymous()) {
                removeNavPanelForFriend(panel.getFriend());
            }
        }
    }
    
    NavPanel addOrUpdateNavPanelForFriend(Friend friend, EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        if (!containsFriend(friend)) {
            FriendLibraryMediator component = friendLibraryBaseFactory.createFriendLibraryBasePanel(friend);
            NavPanel navPanel = navPanelFactory.createNavPanel(createAction(navigator, friend, component), 
                    friend, component, libraryState);

            navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
            navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
            navPanel.setTransferHandler(new FriendLibraryNavTransferHandler(friend, shareListManager));
            
            addNavPanel(navPanel);
            if(eventList != null) {
                navPanel.updateLibrary(eventList, libraryState);
            }
            
            return navPanel;
        } else {
            return updateNavPanelForFriend(friend, libraryState, eventList);
        }
    }
    
    NavPanel updateNavPanelForFriend(Friend friend, LibraryState state, EventList<RemoteFileItem> eventList) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
            panel.updateLibraryState(state);
            panel.updateLibrary(eventList, state);
        }
        return panel;
    }
    
    private NavPanel ensureFriendVisible(Friend friend) {
        NavPanel panel = getPanelForFriend(friend);
        if(panel != null) {
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
    
    private boolean containsFriend(Friend friend) {
        return getPanelForFriend(friend) != null;
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
//                    boolean f1HasLibrary = remoteLibraryManager.hasFriendLibrary(f1);
//                    boolean f2HasLibrary = remoteLibraryManager.hasFriendLibrary(f2);
//                    // show buddies that are logged into limewire first by alphabetical order, than everyone else
//                    if(f1HasLibrary && !f2HasLibrary) {
//                        return -1;
//                    } else if(!f1HasLibrary && f2HasLibrary) {
//                        return 1;
//                    } else                   
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
    
    void removeNavPanelForFriend(Friend friend) {
        for(Iterator<NavPanel> i = navPanels.iterator(); i.hasNext(); ) {
            NavPanel panel = i.next();
            if(panel.getFriend().getId().equals(friend.getId())) {
                i.remove();
                panelContainer.remove(panel);
                navigator.getNavItem(NavCategory.LIBRARY, friend.getId()).remove();
                break;
            }
        }
        checkVisibility();
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
    
    private Action createAction(Navigator navigator, Friend friend, JComponent component) {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, friend.getId(), component);
        Action action = NavigatorUtils.getNavAction(navItem);
        return decorateAction(action, navItem, (Disposable)component, friend);
    }
    
    private Action decorateAction(Action action, NavItem navItem, final Disposable disposable, final Friend friend) {        
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {
                disposable.dispose();
            }
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    ensureFriendVisible(friend);
                    collapsablePanels.setCollapsed(false);
                }
            }
        });
        return action;
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
