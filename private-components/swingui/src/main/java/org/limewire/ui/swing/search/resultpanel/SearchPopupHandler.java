package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.ConfigurableTable;
import org.limewire.ui.swing.table.TablePopupHandler;

public class SearchPopupHandler implements TablePopupHandler {

    private final ConfigurableTable<VisualSearchResult> configTable;
    private final BaseResultPanel baseResultPanel;
    private final PropertiesFactory<VisualSearchResult> properties;

    public SearchPopupHandler(ConfigurableTable<VisualSearchResult> configTable,
            BaseResultPanel baseResultPanel, PropertiesFactory<VisualSearchResult> properties) {
        this.configTable = configTable;
        this.baseResultPanel = baseResultPanel;
        this.properties = properties;
    }

    @Override
    public boolean isPopupShowing(int row) {
        return false;
    }

    @Override
    public void maybeShowPopup(Component component, int x, int y) {

        int[] selectedRows = configTable.getSelectedRows();

        if (selectedRows.length <= 1) {
            int popupRow = configTable.rowAtPoint(new Point(x, y));
            selectedRows = new int[] { popupRow };
            configTable.setRowSelectionInterval(popupRow, popupRow);
        }

        List<VisualSearchResult> selectedItems = new ArrayList<VisualSearchResult>();
        for (int rowIndex = 0; rowIndex < selectedRows.length; rowIndex++) {
            int row = selectedRows[rowIndex];
            if (row != -1) {
                VisualSearchResult visualSearchResult = configTable.getEventTableModel()
                        .getElementAt(row);
                if (visualSearchResult != null) {
                    selectedItems.add(visualSearchResult);
                }
            }
        }

        SearchResultMenu searchResultMenu = new SearchResultMenu(baseResultPanel, selectedItems,
                properties);
        searchResultMenu.show(component, x, y);
    }
}