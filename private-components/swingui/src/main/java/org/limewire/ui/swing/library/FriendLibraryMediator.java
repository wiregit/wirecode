package org.limewire.ui.swing.library;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class FriendLibraryMediator extends LibraryMediator {

    private final EmptyLibraryFactory emptyFactory;
    private final FriendLibraryFactory factory;
    private final FriendSharingPanelFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;

    private final Friend friend;
    private final FriendFileList friendFileList;
    private EventList<RemoteFileItem> eventList;
    
    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
            FriendSharingPanelFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.emptyFactory = emptyFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.friendFileList = shareListManager.getOrCreateFriendShareList(friend);
        setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, this, new OffLineMessageComponent(friendFileList.getSwingModel())));
    }
    
    public void showLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        switch(libraryState) {
        case FAILED_TO_LOAD:
            this.eventList = null;
            setLibraryCard(emptyFactory.createEmptyLibrary(friend, friendFileList, this, new ConnectionErrorComponent(friendFileList.getSwingModel())));
            showLibraryCard();
            break;
        case LOADED:
        case LOADING:
            if(this.eventList != eventList) {
                this.eventList = eventList;
                setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                showLibraryCard();
            }
            break;
        }
    }        
    
    /**
     * Message to display when a friend is offline
     */
    public class OffLineMessageComponent extends JXPanel implements ListEventListener<LocalFileItem>, Disposable {
        private EventList<LocalFileItem> friendList;
        private JLabel secondLabel;
        
        public OffLineMessageComponent(EventList<LocalFileItem> friendList) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            setLayout(new MigLayout("insets 16")); 
            
            JLabel label = new JLabel(I18n.tr("{0} is offline", friend.getFirstName()));
            FontUtils.bold(label);
            secondLabel = new JLabel();
            setMessage();
            
            add(label, "wrap");
            add(secondLabel, "gaptop 10");
        }
        
        private void setMessage() {
            if(friendList.size() > 0) {
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            } else {
                secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
            }  
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setMessage();
            super.revalidate();
        }

        @Override
        public void dispose() {
            friendList.removeListEventListener(this);
        }
    }
    
    /**
     * Message to display when a friend is on LW but couldn't perform a browse
     */
    private class ConnectionErrorComponent extends JXPanel implements ListEventListener<LocalFileItem>, Disposable {
        private EventList<LocalFileItem> friendList;
        private JLabel secondLabel;
        
        public ConnectionErrorComponent(EventList<LocalFileItem> friendList) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            setLayout(new MigLayout("insets 16"));
            
            JLabel label = new JLabel(I18n.tr("{0} is on LimeWire but there were problems viewing their library", friend.getFirstName()));
            FontUtils.bold(label);
            secondLabel = new JLabel();
            setMessage();
            
            add(label, "wrap");
            add(secondLabel, "gaptop 10");
        }
        
        private void setMessage() {
            if(friendList.size() > 0) {
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}. Chat about signing into LimeWire 5.", friendFileList.size(), friend.getFirstName()));
            } else {
                secondLabel.setText(I18n.tr("Share files with {0} and chat about signing into LimeWire 5.", friend.getFirstName()));
            }  
        }

        @Override
        public void listChanged(ListEvent<LocalFileItem> listChanges) {
            setMessage();
        }

        @Override
        public void dispose() {
            friendList.removeListEventListener(this);
        }
    }

    @Override
    public void showSharingCard() {
        if(!isSharingCardSet()) {
            setSharingCard(sharingFactory.createPanel(this, friend, 
                    libraryManager.getLibraryManagedList().getSwingModel(),
                    shareListManager.getOrCreateFriendShareList(friend)));
        }
        super.showSharingCard();
    }
}
