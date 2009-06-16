package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.TablePopupHandler;

import com.google.inject.Provider;

public class SearchPopupHandler implements TablePopupHandler {

    private final ResultsTable<VisualSearchResult> resultsTable;
    private final DownloadHandler downloadHandler;
    private final FileInfoDialogFactory fileInfoFactory;
    private final Provider<RemoteHostActions> remoteHostActions;
    private final SpamManager spamManager;
    private final LibraryMediator libraryMediator;

    public SearchPopupHandler(ResultsTable<VisualSearchResult> resultsTable,
            DownloadHandler downloadHandler, FileInfoDialogFactory fileInfoFactory,
            Provider<RemoteHostActions> remoteHostActions, SpamManager spamManager,
            LibraryMediator libraryMediator) {
        this.resultsTable = resultsTable;
        this.downloadHandler = downloadHandler;
        this.fileInfoFactory = fileInfoFactory;
        this.remoteHostActions = remoteHostActions;
        this.spamManager = spamManager;
        this.libraryMediator = libraryMediator;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        List<Integer> selectedRows = asList(resultsTable.getSelectedRows());
        int popupRow = resultsTable.rowAtPoint(new Point(x, y));
        
        if (selectedRows.size() <= 1 || !selectedRows.contains(popupRow)) {
            selectedRows.clear();
            selectedRows.add(popupRow);
            resultsTable.setRowSelectionInterval(popupRow, popupRow);
        }

        List<VisualSearchResult> selectedItems = new ArrayList<VisualSearchResult>();
        for (Integer row : selectedRows) {
            if (row != -1) {
                VisualSearchResult visualSearchResult = resultsTable.getEventTableModel()
                        .getElementAt(row);
                if (visualSearchResult != null) {
                    selectedItems.add(visualSearchResult);
                }
            }
        }

        SearchResultMenu searchResultMenu = new SearchResultMenu(downloadHandler, selectedItems,
                fileInfoFactory, remoteHostActions, spamManager, libraryMediator, SearchResultMenu.ViewType.Table);
        searchResultMenu.show(component, x, y);
    }

    private List<Integer> asList(int[] array) {
        List<Integer> list = new ArrayList<Integer>();
        for(int i = 0; i < array.length; i++) {
            list.add(array[i]);
        }
        return list;
    }
}