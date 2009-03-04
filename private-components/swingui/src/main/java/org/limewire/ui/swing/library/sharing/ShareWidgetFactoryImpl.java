package org.limewire.ui.swing.library.sharing;

import java.io.File;


import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.friends.login.FriendActions;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.impl.ThreadSafeList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class ShareWidgetFactoryImpl implements ShareWidgetFactory {
    
    private ShareWidget<File> fileShareWidget;
    
    private ShareWidget<Category> categoryShareWidget;

    private ShareWidget<LocalFileItem[]> multiFileShareWidget;
    
    private ShareWidget<LocalFileItem[]> multiFileUnshareWidget;

    private ShareListManager shareListManager;

    private ThreadSafeList<SharingTarget> allFriendsThreadSafe;

    private ShapeDialog shapeDialog;
    
    private FriendActions friendActions;
    
    
    @Inject
    public ShareWidgetFactoryImpl(ShareListManager shareListManager, ShapeDialog shapeDialog, FriendActions friendActions){
        this.shareListManager = shareListManager;
        allFriendsThreadSafe = GlazedListsFactory.threadSafeList(new BasicEventList<SharingTarget>());
        this.shapeDialog = shapeDialog;
        this.friendActions = friendActions;
    }

    @Override
    public ShareWidget<File> createFileShareWidget() {
        if(fileShareWidget == null){
            fileShareWidget = new FileShareWidget(shareListManager, allFriendsThreadSafe, shapeDialog, friendActions);
        }
        return fileShareWidget;
    }

    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileShareWidget() {
        if(multiFileShareWidget == null){
            multiFileShareWidget = new MultiFileShareWidget(shareListManager, allFriendsThreadSafe, shapeDialog, friendActions);
        }
        return multiFileShareWidget;
    }
    
    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileFriendOnlyShareWidget() {
        if(multiFileShareWidget == null){
            multiFileShareWidget = new MultiFileShareWidget(shareListManager, allFriendsThreadSafe, shapeDialog, friendActions, false);
        }
        return multiFileShareWidget;
    }
    
    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileUnshareWidget() {
        if(multiFileUnshareWidget == null){
            multiFileUnshareWidget = new MultiFileUnshareWidget(shareListManager, allFriendsThreadSafe, shapeDialog, friendActions);
        }
        return multiFileUnshareWidget;
    }
    
    @Override
    public ShareWidget<Category> createCategoryShareWidget() {
        if(categoryShareWidget == null){
            categoryShareWidget = new CategoryShareWidget(shareListManager, allFriendsThreadSafe, shapeDialog, friendActions);
        }
        return categoryShareWidget;
    }
    
    @Inject
    void register(@Named("known") ListenerSupport<FriendEvent> knownListeners) {

        knownListeners.addListener(new EventListener<FriendEvent>() {
            @Override
            public void handleEvent(FriendEvent event) {
                switch (event.getType()) {
                case ADDED:
                    allFriendsThreadSafe.add(new SharingTarget(event.getSource()));
                    break;
                case REMOVED:
                    allFriendsThreadSafe.remove(new SharingTarget(event.getSource()));
                    break;
                }
            }
        });

    }

}
