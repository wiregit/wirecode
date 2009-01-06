package org.limewire.ui.swing.library;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class FriendLibraryMediator extends LibraryMediator implements EventListener<FriendEvent> {

    private MessageComponent emptyPanelMessage;
    
    private final EmptyLibraryFactory emptyFactory;
    private final FriendLibraryFactory factory;
    private final FriendSharingPanelFactory sharingFactory;
    private final LibraryManager libraryManager;
    private final ShareListManager shareListManager;

    private final Friend friend;
    private final FriendFileList friendFileList;
    private EventList<RemoteFileItem> eventList;
    
    private ListenerSupport<FriendEvent> availListeners;

    @AssistedInject
    public FriendLibraryMediator(@Assisted Friend friend, FriendLibraryFactory factory, EmptyLibraryFactory emptyFactory,
            FriendSharingPanelFactory sharingFactory, LibraryManager libraryManager, ShareListManager shareListManager,
            @Named("available") ListenerSupport<FriendEvent> availListeners) {
        this.factory = factory;
        this.friend = friend;        
        this.sharingFactory = sharingFactory;
        this.emptyFactory = emptyFactory;
        this.libraryManager = libraryManager;
        this.shareListManager = shareListManager;
        this.friendFileList = shareListManager.getOrCreateFriendShareList(friend);
        this.availListeners = availListeners;
        this.availListeners.addListener(this);
        
        createEmptyCard();
        showEmptyCard();
    }
    
    /**
	 * Constructs the empty panel. This panel is constructed once per friend and the
	 * notification message gets updated as necesarry.
	 */
    private void createEmptyCard() {
    	// if their already online during signon, set appropriate message, otherwise their offline
        if(!friend.getFriendPresences().isEmpty()) {
            emptyPanelMessage = new MessageComponent(friendFileList.getSwingModel(), MessageTypes.ONLINE);
        } else {
            emptyPanelMessage = new MessageComponent(friendFileList.getSwingModel(), MessageTypes.OFFLINE);
        }
        setEmptyCard(emptyFactory.createEmptyLibrary(friend, friendFileList, FriendLibraryMediator.this, emptyPanelMessage));
    }
    
    /**
	 * ShowLibraryCard is called anytime a friend is selected in the left hand nav.
	 * Must ensure the appropriate empty panel/library is shown depending on
	 * the state of the friend.
	 */
    @Override
    public void showLibraryCard() {
        if(eventList == null || eventList.size() == 0) 
            showEmptyCard();
        else
            super.showLibraryCard();
    }
    
    public void updateLibraryPanel(EventList<RemoteFileItem> eventList, LibraryState libraryState) {
        if(!disposed) {
            switch(libraryState) { 
            case FAILED_TO_LOAD:
                this.eventList = null;
                emptyPanelMessage.setMessageType(MessageTypes.LW_CONNECTION_ERROR);
                showEmptyCard();
                break;
            case LOADED:
                if(eventList.size() == 0) {
                    emptyPanelMessage.setMessageType(MessageTypes.LW_NO_FILES);
                    showEmptyCard();
                } else {
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    super.showLibraryCard();
                }
                break;
            case LOADING:
                if(this.eventList != eventList) {
                    this.eventList = eventList;
                } 
                if(eventList.size() == 0) {
	                emptyPanelMessage.setMessageType(MessageTypes.LW_LOADING);
                    showEmptyCard();
                } else {
                    setLibraryCard(factory.createFriendLibrary(friend, friendFileList, eventList, this));
                    super.showLibraryCard();
                }
                break;
            }
        }
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(FriendEvent event) {
        if(!disposed) {
            switch(event.getType()) {
            case ADDED:
                //if friend signed on, show online view
                if(event.getSource().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.ONLINE);
                    showEmptyCard();
                }
                break;
            case REMOVED:
                //if this friend signed off, show offline view
                if(event.getSource().getId().equals(friend.getId())) {
                    emptyPanelMessage.setMessageType(MessageTypes.OFFLINE);
                    showEmptyCard();
                }
                break;
            }
        }
    }
    
    @Override
    public void dispose() {
        availListeners.removeListener(this);
        super.dispose();
    }
       
    /**
     * Various states the EmptyPanel can exist in. 
     */
    enum MessageTypes {
        OFFLINE, ONLINE, LW_LOADING, LW_NO_FILES, LW_CONNECTION_ERROR;
    };
    
    /**
	 * Hover over message panel in the EmptyLibrary. This displays feedback to the user as to
	 * what state their friend is in. 
	 */
    private class MessageComponent extends JXPanel implements ListEventListener<LocalFileItem>, Disposable {
        private EventList<LocalFileItem> friendList;
        private MessageTypes messageType;
        private final JLabel firstLabel;
        private final JLabel secondLabel;
        
        public MessageComponent(EventList<LocalFileItem> friendList, MessageTypes messageType) {
            this.friendList = friendList;
            this.friendList.addListEventListener(this);
            this.messageType = messageType;
            
            setLayout(new MigLayout("insets 16"));
            
            firstLabel = new JLabel();
            FontUtils.bold(firstLabel);
            secondLabel = new JLabel();
            setMessage();
            
            add(firstLabel, "wrap");
            add(secondLabel, "gaptop 10");
            
            setMessage();
        }
        
        /**
	     * Update the message about this friend. 
		 */
        public void setMessageType(MessageTypes messageType) {
            this.messageType = messageType;
            setMessage();
        }
        
        private void setMessage() {
            switch(messageType) {
                case OFFLINE: setOfflineMessage(); break;
                case ONLINE: setOnlineMessage(); break;
                case LW_LOADING: setLWLoading(); break;
                case LW_NO_FILES: setLWNoFiles(); break;
                case LW_CONNECTION_ERROR: setLWConnectionError(); break;
            }
        }
        
        /**
		 * Friend is offline.
		 */
        private void setOfflineMessage() {
            firstLabel.setText(I18n.tr("{0} is offline", friend.getFirstName()));
            if(friendList.size() > 0)
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            else
                secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
        }
        
        /**
		 * Friend is online but not on LimeWire.
		 */
        private void setOnlineMessage() {
            firstLabel.setText(I18n.tr("{0} is online", friend.getFirstName()));
            if(friendList.size() > 0)
                secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
            else
                secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
        }
        
        /**
		 * Friend signed onto LimeWire and currently retrieving their Library.
		 */
        private void setLWLoading() {
            firstLabel.setText(I18n.tr("{0}'s files are currently loading", friend.getFirstName()));
            if(!friend.isAnonymous()) {
                if(friendList.size() > 0)
                    secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
                else
                    secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
            } else
                secondLabel.setText("");
        }
        
        /**
		 * Friend signed onto LimeWire, browse completed and not sharing any files with you.
		 */
        private void setLWNoFiles() {
            firstLabel.setText(I18n.tr("{0} is on LimeWire but not sharing any files with you.", friend.getFirstName()));
            if(!friend.isAnonymous()) {
                if(friendList.size() > 0)
                    secondLabel.setText(I18n.tr("You're sharing {0} files with {1}.", friendFileList.size(), friend.getFirstName()));
                else
                    secondLabel.setText(I18n.tr("Share with {0} for when they sign on LimeWire.", friend.getFirstName()));
            } else
                secondLabel.setText("");
        }
        
        /**
		 * Friend signed onto LimeWire, browse failed.
		 */
        private void setLWConnectionError() {
            firstLabel.setText(I18n.tr("{0} is on LimeWire but there were problems viewing their library", friend.getFirstName()));
            if(!friend.isAnonymous()) {
                if(friendList.size() > 0)
                    secondLabel.setText(I18n.tr("You're sharing {0} files with {1}. Chat about signing into LimeWire 5.", friendFileList.size(), friend.getFirstName()));
                else
                    secondLabel.setText(I18n.tr("Share files with {0} and chat about signing into LimeWire 5.", friend.getFirstName()));
            } else
                secondLabel.setText("");
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
        if(!disposed) {
            if(!isSharingCardSet()) {
                setSharingCard(sharingFactory.createPanel(this, friend, 
                        libraryManager.getLibraryManagedList().getSwingModel(),
                        shareListManager.getOrCreateFriendShareList(friend)));
            }
            super.showSharingCard();            
        }
    }

}
