package org.limewire.ui.swing.library.table;

import javax.swing.JPopupMenu;


import com.google.inject.Inject;
import com.google.inject.Provider;

public class LibraryPopupMenu extends JPopupMenu {

    //TODO: a lot of these actions are not correct anymore
    private final Provider<PlayAction> playAction;
    private final Provider<LaunchFileAction> launchAction;
    private final Provider<LocateFileAction> locateAction;
    private final Provider<RemoveAction> removeAction;
    private final Provider<DeleteAction> deleteAction;
    private final Provider<ViewFileInfoAction> fileInfoAction;
    
    @Inject
    public LibraryPopupMenu(Provider<LaunchFileAction> launchAction, Provider<LocateFileAction> locateAction,
            Provider<PlayAction> playAction, Provider<RemoveAction> removeAction, Provider<DeleteAction> deleteAction,
            Provider<ViewFileInfoAction> fileInfoAction) {
        this.launchAction = launchAction;
        this.locateAction = locateAction;
        this.playAction = playAction;
        this.removeAction = removeAction;
        this.deleteAction = deleteAction;
        this.fileInfoAction = fileInfoAction;
        
        init();
    }
    
    private void init() {
        //TODO: check for multi-file select
        add(launchAction.get());
        add(playAction.get());
        addSeparator();
        add(locateAction.get());
        add(removeAction.get());
        add(deleteAction.get());
        addSeparator();
        add(fileInfoAction.get());
    }
}
