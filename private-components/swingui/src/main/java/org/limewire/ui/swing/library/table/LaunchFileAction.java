package org.limewire.ui.swing.library.table;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.LibraryInspectionUtils;
import org.limewire.ui.swing.library.LibrarySelected;
import org.limewire.ui.swing.library.navigator.LibraryNavigatorPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Tries to safely launch the given file.
 * If it is an allowed file type it will be launched, 
 * otherwise explorer will be opened to the files location
 */
class LaunchFileAction extends AbstractAction {
    private final Provider<List<LocalFileItem>> selectedLocalFileItems;
    private final Provider<LibraryNavigatorPanel> libraryNavProvider;
    private final CategoryManager categoryManager;
    
    @InspectablePrimitive(value = "public shared launches", category = DataCategory.USAGE)
    private static volatile int publicSharedLaunches = 0;
    
    @InspectablePrimitive(value = "library launches", category = DataCategory.USAGE)
    private static volatile int nonPublicSharedLaunches = 0;
    
    @Inject
    public LaunchFileAction(@LibrarySelected Provider<List<LocalFileItem>> selectedLocalFileItems,
        Provider<LibraryNavigatorPanel> libraryNavProvider,
        CategoryManager categoryManager) {
        super(I18n.tr("Play/Open"));

        this.selectedLocalFileItems = selectedLocalFileItems;
        this.libraryNavProvider = libraryNavProvider;
        this.categoryManager = categoryManager;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LocalFileItem> localFileItems = new ArrayList<LocalFileItem>(selectedLocalFileItems.get());
        if (localFileItems.size() > 0) {
            LibraryInspectionUtils.fileLaunched();
            
            // Get first selected item.
            LocalFileItem fileItem = localFileItems.get(0);
            NativeLaunchUtils.safeLaunchFile(fileItem.getFile(), categoryManager);

            if (libraryNavProvider.get().getSelectedNavItem().getType() == NavType.PUBLIC_SHARED) {
                publicSharedLaunches++;
            } else {
                nonPublicSharedLaunches++;
            }
        }
    }
    
}
