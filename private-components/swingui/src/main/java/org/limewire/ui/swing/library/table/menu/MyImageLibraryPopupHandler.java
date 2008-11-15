package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.library.image.LibraryImageSubPanel;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class MyImageLibraryPopupHandler implements TablePopupHandler {
    private int popupRow = -1;

    private LibraryImageSubPanel imagePanel;

    private MyLibraryPopupMenu popupMenu;
    private MyLibraryMultipleSelectionPopupMenu multiSelectPopupMenu;

    public MyImageLibraryPopupHandler(LibraryImageSubPanel imagePanel, ImageLibraryPopupParams params) {
        this.imagePanel = imagePanel;
        this.popupMenu = new MyLibraryPopupMenu(Category.IMAGE, params.libraryManager, params.shareListManager, 
                params.magnetFactory, imagePanel, params.friendList, params.propertiesFactory);
        this.multiSelectPopupMenu = new MyLibraryMultipleSelectionPopupMenu(Category.IMAGE, params.libraryManager, params.shareListManager, imagePanel, params.friendList);

    }

    @Override
    public boolean isPopupShowing(int row) {
        return popupMenu.isVisible() && row == popupRow;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {
        popupRow = imagePanel.getImageList().locationToIndex(new Point(x, y));
        LocalFileItem selectedItem = (LocalFileItem)imagePanel.getImageList().getModel().getElementAt(popupRow);
        if (!selectedItem.isIncomplete()) {
            List<LocalFileItem> items = imagePanel.getSelectedItems();
            if (items.contains(selectedItem) && items.size() > 1) {
                multiSelectPopupMenu.setFileItems(items);
                multiSelectPopupMenu.show(component, x, y);
            } else {
                imagePanel.getImageList().setSelectionInterval(popupRow, popupRow);
                popupMenu.setFileItem(selectedItem);
                popupMenu.show(component, x, y);
            }
        }
    }
    
    public static class ImageLibraryPopupParams {
        private LibraryManager libraryManager;
        private ShareListManager shareListManager;
        private MagnetLinkFactory magnetFactory;
        private List<SharingTarget> friendList;
        private PropertiesFactory<LocalFileItem> propertiesFactory;

        public ImageLibraryPopupParams(LibraryManager libraryManager, ShareListManager shareListManager, 
                MagnetLinkFactory magnetFactory, List<SharingTarget> friendList, PropertiesFactory<LocalFileItem> propertiesFactory){
            this.libraryManager = libraryManager;
            this.shareListManager = shareListManager;
            this.magnetFactory = magnetFactory;
            this.friendList = friendList;            
            this.propertiesFactory = propertiesFactory;
            
        }
    }
}
