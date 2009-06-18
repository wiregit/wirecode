package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Plays the given file in the limewire player, 
 * or launches it depending on if it is supported.
 */
class PlayAction extends AbstractAction {
    
//    private final LibraryMediator libraryMediator;
////    private final Catalog catalog;
//    private final LocalFileItem localFile;
    
    @Inject
    public PlayAction() {//LibraryMediator libraryMediator, //Catalog catalog, 
//            LocalFileItem localFile) {
        super(I18n.tr("Play"));
//        this.libraryMediator = libraryMediator;
//        this.catalog = catalog;
//        this.localFile = localFile;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Set active catalog.
//        libraryMediator.setActiveCatalog(catalog);
        // Play file.
//        PlayerUtils.playOrLaunch(localFile.getFile());
    }
}