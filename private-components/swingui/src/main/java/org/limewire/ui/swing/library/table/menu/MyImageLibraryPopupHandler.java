package org.limewire.ui.swing.library.table.menu;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.library.image.LibraryImageSubPanel;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.TablePopupHandler;

public class MyImageLibraryPopupHandler implements TablePopupHandler {
    private LibraryImageSubPanel imagePanel;

    private MyLibraryPopupMenu popupMenu;

    public MyImageLibraryPopupHandler(LibraryImageSubPanel imagePanel,
            ShareWidgetFactory shareWidgetFactory, LibraryManager libraryManager, 
            PropertiesFactory<LocalFileItem> localFilePropFactory, LibraryNavigator libraryNavigator) {
        this.imagePanel = imagePanel;
        this.popupMenu = new MyLibraryPopupMenu(Category.IMAGE,
                libraryManager, shareWidgetFactory, localFilePropFactory, libraryNavigator);

    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        int popupRow = imagePanel.getImageList().locationToIndex(new Point(x, y));
        if(popupRow < 0)
            return;
        
        LocalFileItem selectedItem = (LocalFileItem) imagePanel.getImageList().getModel().getElementAt(popupRow);
        
        List<LocalFileItem> selectedItems = new ArrayList<LocalFileItem>(imagePanel.getSelectedItems());

        if (selectedItems.size() <= 1 || !selectedItems.contains(selectedItem)) {
            selectedItems.clear();
            imagePanel.setSelectedIndex(popupRow);
            selectedItems.add(selectedItem);
        } 

        popupMenu.setFileItems(selectedItems);
        popupMenu.show(component, x, y);
    }
}
