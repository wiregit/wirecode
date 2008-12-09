package org.limewire.ui.swing.library.image;

import java.io.File;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.library.sharing.LibrarySharePanel;
import org.limewire.ui.swing.library.table.menu.MyImageLibraryPopupHandler.ImageLibraryPopupParams;

import ca.odell.glazedlists.EventList;

/**
 * Creates an appropriate subpanel for images with the correct renderers/editors set
 */
public interface LibraryImageSubPanelFactory {

    public LibraryImageSubPanel createMyLibraryImageSubPanel(File parentFolder, EventList<LocalFileItem> eventList, 
            LocalFileList fileList, ImageLibraryPopupParams params, LibrarySharePanel sharePanel);

    public LibraryImageSubPanel createSharingLibraryImageSubPanel(File parentFolder, EventList<LocalFileItem> eventList, 
            LocalFileList fileList, ImageLibraryPopupParams params, LocalFileList currentFriendList);
}
