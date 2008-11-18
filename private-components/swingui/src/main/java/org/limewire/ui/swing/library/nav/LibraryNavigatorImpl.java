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

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.dnd.FriendLibraryNavTransferHandler;
import org.limewire.ui.swing.dnd.MyLibraryNavTransferHandler;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.library.Disposable;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.library.FriendLibraryMediatorFactory;
import org.limewire.ui.swing.library.MyLibraryMediator;
import org.limewire.ui.swing.library.SharingLibraryFactory;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavItemListener;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.RosterEvent;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
// TODO: Public only for EventBus -- this should be package-private
public class LibraryNavigatorImpl extends JXPanel implements RegisteringEventListener<RosterEvent>, LibraryNavigator {

    private static final String NAME = "__@internal@__";

    private final SectionHeading titleLabel;
    
    private final NavPanel myLibrary;
    private final NavList browseList;
    private final NavList restList;
    
    private final Navigator navigator;
    private final SharingLibraryFactory sharingFactory;
    private final MyLibraryMediator myLibraryMediator;
    private final LibraryManager libraryManager;
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
            SharingLibraryFactory sharingFactory,
            Provider<NavList> navListProvider,
            NavPanelFactory navPanelFactory,
            FriendLibraryMediatorFactory friendLibraryMediatorFactory) {
        
        EventAnnotationProcessor.subscribe(this);
        
        this.sharingFactory = sharingFactory;
        this.myLibraryMediator = myLibraryMediator;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.browseList = navListProvider.get();
        this.restList = navListProvider.get();
        this.navPanelFactory = navPanelFactory;
        this.friendLibraryMediatorFactory = friendLibraryMediatorFactory;
        this.navigator = navigator;
        
        browseList.setTitleText(I18n.tr("On LimeWire"));
        restList.setTitleText(I18n.tr("Not On LimeWire"));
        
        setOpaque(false);
        setScrollableTracksViewportHeight(false);
        this.titleLabel = new SectionHeading(I18n.tr("Libraries"));
        titleLabel.setName("LibraryNavigator.titleLabel");
        
        LibraryFileList libraryList = libraryManager.getLibraryManagedList();
        myLibraryMediator.setMainCardEventList(libraryList.getSwingModel());
        myLibrary = navPanelFactory.createNavPanel(createMyLibraryAction(), Me.ME, null, libraryList.getState());
        myLibrary.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryNavigatorImpl.this.myLibraryMediator.showMainCard();
            }
        });
        
        myLibrary.setTransferHandler(new MyLibraryNavTransferHandler(downloadListManager, libraryManager));
        
        myLibrary.getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(browseList, true));
        myLibrary.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        
        browseList.getActionMap().put(NavKeys.MOVE_UP, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LibraryNavigatorImpl.this.myLibraryMediator.showMainCard();
                myLibrary.select();
            }
        });
        browseList.getActionMap().put(NavKeys.MOVE_DOWN, new MoveAction(restList, true));
        restList.getActionMap().put(NavKeys.MOVE_UP, new MoveAction(browseList, false));
        
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
        add(browseList, "growx, alignx left, aligny top, wrap");
        add(restList, "growx, alignx left, aligny top, wrap");

        new AbstractListEventListener<FriendLibrary>() {
            @Override
            protected void itemAdded(FriendLibrary item) {
                NavPanel panel = restList.getPanelForFriend(item.getFriend());
                if(panel != null) {
                    restList.removePanel(panel);
                    browseList.addNavPanel(panel);
                    panel.updateLibraryState(item.getState());
                    panel.updateLibrary(item.getSwingModel(), item.getState());
                } else {
                    NavPanel navPanel = createFriendNavPanel(item.getFriend(), item.getSwingModel(), item.getState());
                    browseList.addNavPanel(navPanel);
                }
            }
            
            @Override
            protected void itemRemoved(FriendLibrary item) {
                Friend friend = item.getFriend();
                if(friend.isAnonymous()) {
                    browseList.removeNavPanelForFriend(item.getFriend());
                } else {
                    NavPanel panel = browseList.getPanelForFriend(item.getFriend());
                    if(panel != null) {
                        browseList.removePanel(panel);
                        restList.addNavPanel(panel);
                        panel.removeBrowse();
                    } // else probably signed off & cleared the lists.
                }
            }
            
            @Override
            protected void itemUpdated(FriendLibrary item) {
                browseList.updateNavPanelForFriend(item.getFriend(), item.getState(), item.getSwingModel());
            }
        }.install(remoteLibraryManager.getSwingFriendLibraryList());
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
    
    public void selectFriendShareList(Friend friend) {
        JComponent component = sharingFactory.createSharingLibrary(myLibraryMediator, friend, 
            libraryManager.getLibraryManagedList().getSwingModel(),
            shareListManager.getFriendShareList(friend));

        myLibraryMediator.setAuxCard(component);
        myLibraryMediator.showAuxCard();

        myLibrary.select();
    }    
    
    @Override
    public void selectFriendLibrary(Friend friend) {
        browseList.selectFriendLibrary(friend);
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
    
    @Override
    @SwingEDTEvent
    public void handleEvent(RosterEvent event) {
        Friend friend = event.getSource();
        switch (event.getType()) {
        case USER_ADDED:
            NavPanel panel = browseList.getPanelForFriend(friend);
            if(panel == null) {
                restList.addNavPanel(createFriendNavPanel(friend, null, null));
            }
            break;
        case USER_REMOVED:
            browseList.removeNavPanelForFriend(friend);
            restList.removeNavPanelForFriend(friend);
            break;
        }
    }
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        browseList.clearFriends();
        restList.clear();
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    private void ensureFriendVisible(Friend friend) {
        if(browseList.ensureFriendVisible(friend) == null) {
            restList.ensureFriendVisible(friend);
        }
    }
    
    private NavPanel createFriendNavPanel(Friend friend, EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        FriendLibraryMediator component = friendLibraryMediatorFactory.createFriendLibraryBasePanel(friend);
        NavPanel navPanel = navPanelFactory.createNavPanel(createFriendAction(navigator, friend, component), 
                friend, component, libraryState);

        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NavKeys.MOVE_DOWN);
        navPanel.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NavKeys.MOVE_UP);
        navPanel.setTransferHandler(new FriendLibraryNavTransferHandler(friend, shareListManager));
        
        if(eventList != null) {
            navPanel.updateLibrary(eventList, libraryState);
        }
        
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
            return I18n.tr("Me");
        }

        @Override
        public void setName(String name) {
        }

        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, Presence> getPresences() {
            return Collections.emptyMap();
        }
    }
    
}
