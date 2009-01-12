package org.limewire.ui.swing.library.sharing;

import java.io.File;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShareWidgetFactoryImpl implements ShareWidgetFactory {
    
    private ShareWidget<File> fileShareWidget;
    
    private ShareWidget<Category> categoryShareWidget;

    private ShareWidget<LocalFileItem[]> multiFileShareWidget;
    
    private ShareWidget<LocalFileItem[]> multiFileUnshareWidget;

    private ShareListManager shareListManager;

    private EventList<SharingTarget> allFriendsNotSwingSafe;

    private ShapeDialog shapeDialog;
    
    private EventListener<RosterEvent> addRemoveListener;
    private EventListener<XMPPConnectionEvent> disconnectListener;
    
    @Inject
    public ShareWidgetFactoryImpl(ShareListManager shareListManager, ShapeDialog shapeDialog){
        this.shareListManager = shareListManager;
        allFriendsNotSwingSafe = GlazedListsFactory.threadSafeList(new BasicEventList<SharingTarget>());
        this.shapeDialog = shapeDialog;
    }

    @Override
    public ShareWidget<File> createFileShareWidget() {
        if(fileShareWidget == null){
            fileShareWidget = new FileShareWidget(shareListManager, allFriendsNotSwingSafe, shapeDialog);
        }
        return fileShareWidget;
    }

    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileShareWidget() {
        if(multiFileShareWidget == null){
            multiFileShareWidget = new MultiFileShareWidget(shareListManager, allFriendsNotSwingSafe, shapeDialog);
        }
        return multiFileShareWidget;
    }

    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileUnshareWidget() {
        if(multiFileUnshareWidget == null){
            multiFileUnshareWidget = new MultiFileUnshareWidget(shareListManager, allFriendsNotSwingSafe, shapeDialog);
        }
        return multiFileUnshareWidget;
    }
    
    @Override
    public ShareWidget<Category> createCategoryShareWidget() {
        if(categoryShareWidget == null){
            categoryShareWidget = new CategoryShareWidget(shareListManager, allFriendsNotSwingSafe, shapeDialog);
        }
        return categoryShareWidget;
    }
    
    @Inject void register(ListenerSupport<RosterEvent> rosterListeners,
            ListenerSupport<XMPPConnectionEvent> connectionListeners) {

        addRemoveListener = new EventListener<RosterEvent>() {
            @Override
            public void handleEvent(RosterEvent event) {
                switch (event.getType()) {
                case USER_ADDED:
                    allFriendsNotSwingSafe.add(new SharingTarget(event.getSource()));
                    break;
                case USER_DELETED:
                    allFriendsNotSwingSafe.remove(new SharingTarget(event.getSource()));
                    break;
                }
            }
        };
        rosterListeners.addListener(addRemoveListener);

        disconnectListener = new EventListener<XMPPConnectionEvent>() {
            @Override
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case DISCONNECTED:
                    allFriendsNotSwingSafe.clear();
                    break;
                }
            }
        };
        connectionListeners.addListener(disconnectListener);
    }

}
