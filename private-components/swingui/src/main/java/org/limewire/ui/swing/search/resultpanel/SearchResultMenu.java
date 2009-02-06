package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the popup menu that is displayed when the user 
 * right-clicks on a search result.
 */
public class SearchResultMenu extends JPopupMenu {
    
    /** Defines the available display types. */
    public enum ViewType {
        List,
        Table
    }
    
    /**
     * Constructs a SearchResultMenu using the specified download handler,
     * list of selected results, properties factory, and display type.
     */
    public SearchResultMenu(final DownloadHandler downloadHandler,
        final List<VisualSearchResult> selectedItems,
        final PropertiesFactory<VisualSearchResult> propertiesFactory,
        ViewType viewType) {

        final VisualSearchResult firstItem = selectedItems.get(0);
        
        // Determine if download is enabled.
        boolean downloadEnabled = false;
        for (VisualSearchResult visualSearchResult : selectedItems) {
            if (visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                downloadEnabled = true;
                break;
            }
        }
        
        // Determine indicators to enable menu items.
        boolean showHideSimilarFileVisible = selectedItems.size() == 1 && firstItem.getSimilarResults().size() > 0 && viewType == ViewType.List;
        boolean showHideSimilarFileEnabled = selectedItems.size() == 1 && firstItem.getDownloadState() == BasicDownloadState.NOT_STARTED;
        boolean viewFileInfoEnabled = selectedItems.size() == 1;

        // Add Download menu item.
        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                for (VisualSearchResult visualSearchResult : selectedItems) {
                    if (visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                        downloadHandler.download(visualSearchResult);
                    }
                }
            }
        }).setEnabled(downloadEnabled);

        // Add Mark/Unmark as Spam menu item.
        add(new AbstractAction(firstItem.isSpam() ? tr("Unmark as spam") : tr("Mark as spam")) {
            public void actionPerformed(ActionEvent e) {
                boolean spam = !firstItem.isSpam();
                for (VisualSearchResult visualSearchResult : selectedItems) {
                    visualSearchResult.setSpam(spam);
                }
            }
        });

        addSeparator();

        // Add optional item for Similar Files.
        if (showHideSimilarFileVisible) {
            add(new AbstractAction(firstItem.isChildrenVisible() ? tr("Hide Similar Files") : tr("Show Similar Files")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    firstItem.toggleChildrenVisibility();
                }
            }).setEnabled(showHideSimilarFileEnabled);
            
            addSeparator();
        }

        // Add View File Info menu item.
        add(new AbstractAction(tr("View File Info...")) {
            public void actionPerformed(ActionEvent e) {
                propertiesFactory.newProperties().showProperties(firstItem);
            }
        }).setEnabled(viewFileInfoEnabled);
    }
}
