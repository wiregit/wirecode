package org.limewire.ui.swing.library.table.menu;

import java.awt.Color;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.table.menu.actions.DeleteAction;
import org.limewire.ui.swing.library.table.menu.actions.LaunchFileAction;
import org.limewire.ui.swing.library.table.menu.actions.LocateFileAction;
import org.limewire.ui.swing.library.table.menu.actions.PlayAction;
import org.limewire.ui.swing.library.table.menu.actions.RemoveAction;
import org.limewire.ui.swing.library.table.menu.actions.SharingActionFactory;
import org.limewire.ui.swing.library.table.menu.actions.ViewFileInfoAction;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

/**
 * Popup menu implementation for MyLibrary
 */
public class MyLibraryPopupMenu extends JPopupMenu {

    private final LibraryManager libraryManager;
    private final Category category;
    private final PropertiesFactory<LocalFileItem> propertiesFactory;
    private final XMPPService xmppService;
    private final SharingActionFactory sharingActionFactory;   

    private SelectAllable<LocalFileItem> librarySelectable;
    
    @Resource
    private Color disabledColor;
    
    public MyLibraryPopupMenu(Category category, LibraryManager libraryManager,
            SharingActionFactory sharingActionFactory, PropertiesFactory<LocalFileItem> propertiesFactory,
            XMPPService xmppService) {
        this.libraryManager = libraryManager;
        this.sharingActionFactory = sharingActionFactory;
        this.category = category;
        this.propertiesFactory = propertiesFactory;
        this.xmppService = xmppService;
        
        GuiUtils.assignResources(this);
    }
    
    public void setSelectable(SelectAllable<LocalFileItem> librarySelectable) {
        this.librarySelectable = librarySelectable;
        initialize();
    }
    
    private JMenuItem decorateDisabledfItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(disabledColor);
        return item;
    }

    private void initialize() {
        List<LocalFileItem> fileItems = librarySelectable.getSelectedItems();
        boolean singleFile = fileItems.size() == 1;

        LocalFileItem firstItem = librarySelectable.getSelectedItems().get(0);

        boolean playActionEnabled = singleFile;
        boolean launchActionEnabled = singleFile;
        boolean locateActionEnabled = singleFile && !firstItem.isIncomplete();
        boolean viewFileInfoEnabled = singleFile;
        boolean shareActionEnabled = false;
        boolean removeActionEnabled = false;
        boolean deleteActionEnabled = false;

        for (LocalFileItem localFileItem : fileItems) {
            if (localFileItem.isShareable()) {
                shareActionEnabled = true;
                break;
            }
        }
        
        for(LocalFileItem localFileItem : fileItems) {
            if(!localFileItem.isIncomplete()) {
                removeActionEnabled = true;
                deleteActionEnabled = true;
                break;
            }
        }

        removeAll();
        switch (category) {
        case AUDIO:
        case VIDEO:
            add(new PlayAction(firstItem)).setEnabled(playActionEnabled);
            break;
        case IMAGE:
        case DOCUMENT:
            add(new LaunchFileAction(I18n.tr("View"), firstItem)).setEnabled(launchActionEnabled);
            break;
        case PROGRAM:
        case OTHER:
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }
        addSeparator();
        
        boolean isDocumentSharingAllowed = isGnutellaShareAllowed(category) & shareActionEnabled;
        add(sharingActionFactory.createShareGnutellaAction(false, librarySelectable)).setEnabled(isDocumentSharingAllowed);
        add(sharingActionFactory.createUnshareGnutellaAction(false, librarySelectable)).setEnabled(isDocumentSharingAllowed);
        
        addSeparator();
        
        if(xmppService.isLoggedIn()) {
            add(sharingActionFactory.createShareFriendAction(false, librarySelectable)).setEnabled(shareActionEnabled);
            add(sharingActionFactory.createUnshareFriendAction(false, librarySelectable)).setEnabled(shareActionEnabled);
        } else {
            add(decorateDisabledfItem(sharingActionFactory.createDisabledFriendAction(I18n.tr("Share with Friend"))));
            add(decorateDisabledfItem(sharingActionFactory.createDisabledFriendAction(I18n.tr("Unshare with Friend"))));
        }

        addSeparator();
        if (category != Category.PROGRAM && category != Category.OTHER) {
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }

        add(new RemoveAction(fileItems.toArray(new LocalFileItem[fileItems.size()]), libraryManager)).setEnabled(removeActionEnabled);
        
        add(new DeleteAction(fileItems.toArray(new LocalFileItem[fileItems.size()]), libraryManager)).setEnabled(deleteActionEnabled);

        addSeparator();
        add(new ViewFileInfoAction(firstItem, propertiesFactory)).setEnabled(viewFileInfoEnabled);
    }
    
    private boolean isGnutellaShareAllowed(Category category) {
        if(category != Category.DOCUMENT)
            return true;
        return LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue();
    }
}
