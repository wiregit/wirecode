package org.limewire.ui.swing.search.resultpanel;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

import ca.odell.glazedlists.gui.WritableTableFormat;


/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 */
public abstract class ResultsTableFormat<E> extends AbstractAdvancedTableFormat<E> implements WritableTableFormat<E> {

    protected VisualSearchResult vsr;
    private int lastVisibleColumnIndex;

    protected ResultsTableFormat(int lastVisibleColumnIndex, String...columnNames) {
        super(columnNames);
        this.lastVisibleColumnIndex = lastVisibleColumnIndex;
    }

    @Override
    public Class getColumnClass(int index) {
        return String.class;
    }

    public Comparator getColumnComparator(int index) {
        return null;
    }


    /**
     * Gets the initial column width of the column with a given index.
     * @param index the column index
     * @return the initial column width
     */
    public abstract int getInitialColumnWidth(int index);

    /**
     * Gets the index of the last column that should be visible by default.
     * All columns past this start out hidden and
     * can be shown by right-clicking on the column header
     * and selecting the corresponding checkbox.
     * @return the column index
     */
    public int getLastVisibleColumnIndex() {
        return lastVisibleColumnIndex;
    }

    /**
     * Gets the value of a given property.
     * @param key the property key or name
     * @return the property value
     */
    protected Object getProperty(FilePropertyKey key) {
        return vsr.getProperty(key);
    }

    /**
     * Gets the String value of a given property.
     * @param key the property key or name
     * @return the String property value
     */
    protected String getString(FilePropertyKey key) {
        Object value = vsr.getProperty(key);
        return value == null ? "?" : value.toString();
    }
    
    abstract public int getNameColumn();

    abstract public boolean isEditable(VisualSearchResult vsr, int column);// {
//        return false;
//        return index == actionColumnIndex;
//    }

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }

}