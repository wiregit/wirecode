package org.limewire.ui.swing.search.resultpanel;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.SearchResultMenu.ViewType;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

/**
 * A mouse listener installed on cell editor components in ResultsTable.
 * ResultsTableEditorListener handles editor mouse events to update the 
 * selection and display a popup menu.  When a JTable is in edit mode, it
 * forwards mouse events to the cell editor so we need to provide our own
 * logic to update the selection on mouse presses.
 */
class ResultsTableEditorListener extends MousePopupListener {

    private final ResultsTable table;
    private final ViewType viewType;
    private final SearchResultMenuFactory popupMenuFactory;
    private final DownloadHandler downloadHandler;
    
    /**
     * Constructs a ResultsTableEditorListener for the specified table, view
     * type, and services.
     */
    public ResultsTableEditorListener(ResultsTable table, ViewType viewType,
            SearchResultMenuFactory popupMenuFactory,
            DownloadHandler downloadHandler) {
        this.table = table;
        this.viewType = viewType;
        this.popupMenuFactory = popupMenuFactory;
        this.downloadHandler = downloadHandler;
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            updateSelection(e);
        }
        super.mousePressed(e);
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        VisualSearchResult vsr = getEditValue();
        if ((vsr != null) && (e.getClickCount() == 2) && SwingUtilities.isLeftMouseButton(e)) {  
            downloadHandler.download(vsr);
        } else {
            super.mouseClicked(e);
        }
    }
    
    @Override
    public void handlePopupMouseEvent(MouseEvent e) {
        // Update selection if mouse is not in selected row.
        if (table.isEditing()) {
            int editRow = table.getEditingRow();
            if (!table.isRowSelected(editRow)) {
                updateSelection(e);
            }
        }
        
        // Create list of selected results.
        List<VisualSearchResult> selectedResults = new ArrayList<VisualSearchResult>();
        DefaultEventTableModel model = table.getEventTableModel();
        int[] selectedRows = table.getSelectedRows();
        for (int row : selectedRows) {
            Object element = model.getElementAt(row);
            if (element instanceof VisualSearchResult) {
                selectedResults.add((VisualSearchResult) element);
            }
        }
        
        // If nothing selected, use current result.
        VisualSearchResult vsr = getEditValue();
        if (selectedResults.size() == 0) {
            selectedResults.add(vsr);
        }
        
        // Display context menu.
        SearchResultMenu searchResultMenu = popupMenuFactory.create(
                downloadHandler, selectedResults, viewType);
        searchResultMenu.show(e.getComponent(), e.getX()+3, e.getY()+3);
    }
    
    /**
     * Returns the value being edited.
     */
    private VisualSearchResult getEditValue() {
        if (table.isEditing()) {
            int editRow = table.getEditingRow();
            return (VisualSearchResult) table.getEventTableModel().getElementAt(editRow);
        }
        return null;
    }
    
    /**
     * Updates the table row selection based on the specified mouse event.
     */
    private void updateSelection(MouseEvent e) {
        if (table.isEditing()) {
            // Get cell being edited by this editor.
            int editRow = table.getEditingRow();
            int editCol = table.getEditingColumn();
            
            // Update the selection.  We also prepare the editor to apply
            // the selection colors to the current editor component.
            if ((editRow > -1) && (editRow < table.getRowCount())) {
                table.changeSelection(editRow, editCol, e.isControlDown(), e.isShiftDown());
                table.prepareEditor(table.getCellEditor(), editRow, editCol);
            }
        }
        
        // Request focus so Enter key can be handled.
        e.getComponent().requestFocusInWindow();
    }
}
