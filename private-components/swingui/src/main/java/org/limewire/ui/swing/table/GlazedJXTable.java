package org.limewire.ui.swing.table;

import java.util.Vector;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * A JXTable for use with glazed lists event models. See
 * http://sites.google.com/site/glazedlists/documentation/swingx for issues with
 * SwingX. 
 */
public class GlazedJXTable extends BasicJXTable {

    public GlazedJXTable() {
        super();
        initialize();
    }

    public GlazedJXTable(int numRows, int numColumns) {
        super(numRows, numColumns);
        initialize();
    }

    public GlazedJXTable(Object[][] rowData, Object[] columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        initialize();
    }

    public GlazedJXTable(TableModel dm, TableColumnModel cm) {
        super(dm, cm);
        initialize();
    }

    public GlazedJXTable(TableModel dm) {
        super(dm);
        initialize();
    }

    public GlazedJXTable(Vector rowData, Vector columnNames) {
        super(rowData, columnNames);
        initialize();
    }

    private void initialize() {
        getSelectionMapper().setEnabled(false);
    }
    
}
