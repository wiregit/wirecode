package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import java.util.Comparator;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public abstract class ResultsTableFormat<E>
implements AdvancedTableFormat<E>, WritableTableFormat<E> {

    protected String[] columnNames;
    protected VisualSearchResult vsr;
    protected int actionColumnIndex;

    /**
     * Gets the index for the column that contains the three "action" buttons.
     * @return
     */
    public int getActionButtonColumnIndex() {
        return actionColumnIndex;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    protected String get(PropertyKey key) {
        Object value = vsr.getProperties().get(key);
        return value == null ? "?" : value.toString();
    }

    public Class getColumnClass(int index) {
        return index == actionColumnIndex ? VisualSearchResult.class : String.class;
    }

    public Comparator getColumnComparator(int index) {
        return null;
    }

    public boolean isEditable(VisualSearchResult vsr, int index) {
        return index == actionColumnIndex;
    }

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }
}