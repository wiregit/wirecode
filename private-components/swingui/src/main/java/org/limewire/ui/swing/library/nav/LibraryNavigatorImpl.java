package org.limewire.ui.swing.library.nav;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.library.FriendLibraryMediatorFactory;
import org.limewire.ui.swing.library.MyLibraryMediator;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
class LibraryNavigatorImpl extends JXPanel implements LibraryNavigator {

    private static final String NAME = "__@internal@__";

    private final SectionHeading titleLabel;
    
    private final NavPanel myLibrary;
    private final NavList limewireList;
    private final NavList onlineList;
    private final NavList offlineList;
    private final NavList[] allLists;
    
    private final Navigator navigator;
    private final MyLibraryMediator myLibraryMediator;
    private final ShareListManager shareListManager;
    private final FriendLibraryMediatorFactory friendLibraryMediatorFactory;
    private final NavPanelFactory navPanelFactory;
    

    @Inject
    LibraryNavigatorImpl(Navigator navigator,
            LibraryManager libraryManager,
            RemoteLibraryManager remoteLibraryManager,
            DownloadListManager downloadListManager,
            ShareListManager shareListManager,
            MyLibraryMediator myLibraryMediator,
            NavPanelFactory navPanelFactory,
            FriendLibraryMediatorFactory friendLibraryMediatorFactory) {
        
        this.myLibraryMediator = myLibraryMediator;
        this.shareListManager = shareListManager;
        this.limewireList = new NavList();
        this.onlineList = new NavList();
        this.offlineList = new NavList();
        this.allLists = new NavList[] { limewireList, onlineList, offlineList };
        this.navPanelFactory = navPanelFactory;
        this.friendLibraryMediatorFactory = friendLibraryMediatorFactory;
        this.navigator = navigator;
        
        limewireList.setTitleText(I18n.tr("On LimeWire"));
        onlineList.setTitleText(I18n.tr("Online"));
        offlineList.setTitleText(I18n.tr("Offline"));
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");
        
        LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        myLibraryMediator.setMainCardEventList(Me.ME, libraryList.getSwingModel());
        myLibrary = navPanelFactory.createNavPanel(createMyLibraryAction(), Me.ME, null);
        myLibrary.updateLibraryState(libraryList.getState());
        myLibrary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryNavigatorImpl.this.myLibraryMediator.showLibraryCard();
            }
        });
        
        myLibrary.setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, libraryManager));
        myLibrary.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        
        libraryList.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("state")) {
                    myLibrary.updateLibraryState((LibraryState)evt.getNewValue());
                }
            }
        });

        setLayout(new MigLayout("insets 0, gap 0, hidemode 2"));
        add(titleLabel, "growx, alignx left, aligny top, wrap");
        add(myLibrary, "growx, alignx left, aligny top, wrap"); 
        
        // Add all the navlists and hook up the actions.
        myLibrary.getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(allLists[0], true));        
        for(int i = 0; i < allLists.length; i++) {
            // Move up action goes to My Library if first
            if(i == 0) {
                allLists[i].getActionMap().put(NavKeys.MOVE_UP, new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
		                LibraryNavigatorImpl.this.myLibraryMediator.showLibraryCard();
                        myLibrary.select();
                    }
                });
            } else {
                // Otherwise it goes to the prior list.
                allLists[i].getActionMap().put(NavKeys.MOVE_UP, new MoveAction(allLists[i-1], false));
            }
            
            // If this isn't the last list, down goes to the next
            if(i < allLists.length - 1) {
                allLists[i].getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(allLists[i+1], true));
            }
            
            add(allLists[i], "growx, alignx left, aligny top, wrap");
        }

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                NavPanel panel = getPanelForFriend(item.getFriend());
                if(panel != null) {
                    panel.getParentList().removePanel(panel);
                } else {
                    panel = createFriendNavPanel(item.getFriend());
                }
                
                limewireList.addNavPanel(panel);
                updatePanel(item, panel);
            }
            
            @Override
            protected void itemRemoved(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    NavPanel panel = limewireList.removePanelForFriend(item.getFriend());
                    if(panel != null) {
                        disposeNavPanel(panel);
                    }
                } else {
                    NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                    if(panel != null) {
                        limewireList.removePanel(panel);
                        panel.removeBrowse();
                        onlineList.addNavPanel(panel); // Assume still online.
                    } // else probably signed off & cleared the lists.
                }
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                NavPanel panel = limewireList.getPanelForFriend(item.getFriend());
                if(panel != null) {
                    updatePanel(item, panel);
                }
            }
            
            void updatePanel(FriendLibrary item, NavPanel panel) {
                panel.updateLibraryState(item.getState());
                panel.updateLibrary(item.getSwingModel(), item.getState());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
    }
    
    @Inject void register(@Named("known") ListenerSupport<FriendEvent> knownListeners,
                          @Named("available") ListenerSupport<FriendEvent> availListeners) {
        
        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getSource());
                        offlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        panel.getParentList().removePanel(panel);
                        disposeNavPanel(panel);
                    }
                }
            }
        });
        
        availListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendEvent event) {
                NavPanel panel = getPanelForFriend(event.getSource());
                switch(event.getType()) {
                case ADDED:
                    if(panel == null) {
                        panel = createFriendNavPanel(event.getSource());
                        onlineList.addNavPanel(panel);
                    } else if(panel.getParentList() == offlineList) {
                        offlineList.removePanel(panel);
                        onlineList.addNavPanel(panel);
                    }
                    break;
                case REMOVED:
                    if(panel != null) {
                        panel.getParentList().removePanel(panel);
                        offlineList.addNavPanel(panel);
                    }
                }
            }
        });
        
    }
    
    private NavPanel getPanelForFriend(Friend friend) {
        for(NavList list : allLists) {
            NavPanel panel = list.getPanelForFriend(friend);
            if(panel != null) {
                return panel;
            }
        }
        return null;
    }
    
    @Override
    public Component getComponent() {
        return this;
    }
    
    private Action createMyLibraryAction() {
        NavItem navItem = navigator.createNavItem(NavCategory.LIBRARY, NAME, myLibraryMediator);
        Action action = NavigatorUtils.getNavAction(navItem);  
        navItem.addNavItemListener(new NavItemListener() {
            @Override
            public void itemRemoved() {}
            
            @Override
            public void itemSelected(boolean selected) {
                if(selected) {
                    scrollRectToVisible(myLibrary.getBounds());
                }
            }
        });
        return action;
    }
    
    @Override
    public void selectFriendLibrary(Friend friend) {
        for(NavList list : allLists) {
            if(list.selectFriendLibrary(friend) != null) {
                break;
            }
        }
    }
    
    @Override
    public void selectInLibrary(URN urn, Category category) {
        throw new RuntimeException("TODO: select in library");
//        navigator.getNavItem(NavCategory.LIBRARY,
//                NAME_PREFIX + propertiable.getCategory()).select(
//                new NavSelectable<URN>() {
//                    @Override
//                    public URN getNavSelectionId() {
//                        return ;
//                    }
//                });   
    }
    
    private void ensureFriendVisible(Friend friend) {
        for(NavList list : allLists) {
            if(list.ensureFriendVisible(friend) != null) {
                break;
            }
        }
    }
    
    private void disposeNavPanel(NavPanel navPanel) {
        navPanel.removeBrowse();
        navigator.getNavItem(NavCategory.LIBRARY, navPanel.getFriend().getId()).remove();
    }    
    
    private NavPanel createFriendNavPanel(Friend friend) {
        final FriendLibraryMediator component = friendLibraryMediatorFactory.createFriendLibraryBasePanel(friend);
        NavPanel navPanel = navPanelFactory.createNavPanel(createFriendAction(navigator, friend, component), 
                friend, component);
        navPanel.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                component.showLibraryCard();
            }
        });
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        navPanel.setTransferHandler(new FriendLibraryNavTransferHandler(friend, shareListManager));
        
        return navPanel;
    }
    
    private Action createFriendAction(Navigator navigator, Friend friend, JComponent component) {
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
                }
            }
        });
        return action;
    }    
    
    private class MoveAction extends AbstractAction {
        private final NavList navList;
        private final boolean selectFirst;
        
        MoveAction(NavList navList, boolean selectFirst) {
            this.navList = navList;
            this.selectFirst = selectFirst;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if(selectFirst) {
                navList.selectFirst();
            } else {
                navList.selectLast();
            }
        }
    }

    private static class Me implements Friend {
        private static final Me ME = new Me();
        
        @Override
        public boolean isAnonymous() {
            return false;
        }

        @Override
        public String getId() {
            return NAME;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String getRenderName() {
            return I18n.tr("My Library");
        }

        @Override
        public void setName(String name) {
        }

        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, FriendPresence> getFriendPresences() {
            return Collections.emptyMap();
        }
    }
    
}
