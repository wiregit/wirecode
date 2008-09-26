package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import com.google.inject.Inject;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Comparator;
import javax.swing.Icon;
import javax.swing.JLabel;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;

/**
 * This class is the base class for each of the TableFormat classes
 * that describe the various table views of search results.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public abstract class ResultsTableFormat<E>
implements AdvancedTableFormat<E>, WritableTableFormat<E> {

    private IconManager iconManager;
    protected String[] columnNames;
    protected VisualSearchResult vsr;
    protected int actionColumnIndex;
    private int lastVisibleColumnIndex;

    ResultsTableFormat(int actionColumnIndex, int lastVisibleColumnIndex) {
        this.actionColumnIndex = actionColumnIndex;
        this.lastVisibleColumnIndex = lastVisibleColumnIndex;
    }

    /**
     * Gets the index for the column that contains the three "action" buttons.
     * @return the column index
     */
    public int getActionColumnIndex() {
        return actionColumnIndex;
    }

    public Class getColumnClass(int index) {
        return index == actionColumnIndex ?
            VisualSearchResult.class : String.class;
    }

    public Comparator getColumnComparator(int index) {
        return null;
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public String getColumnName(int index) {
        return columnNames[index];
    }

    protected Component getIconLabel(VisualSearchResult vsr) {
        String ext = vsr.getFileExtension();
        Icon icon = iconManager.getIconForExtension(ext);

        String name = (String) getProperty(PropertyKey.NAME);

        JLabel label = new JLabel(name, icon, JLabel.LEFT);

        Font font = label.getFont().deriveFont(Font.PLAIN);
        label.setFont(font);

        label.setOpaque(false);

        JXPanel panel = new JXPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(label);

        panel.setAlpha(vsr.isSpam() ? 0.2f : 1.0f);

        return panel;
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
    protected Object getProperty(PropertyKey key) {
        return vsr.getProperty(key);
    }

    /**
     * Gets the String value of a given property.
     * @param key the property key or name
     * @return the String property value
     */
    protected String getString(PropertyKey key) {
        Object value = vsr.getProperty(key);
        return value == null ? "?" : value.toString();
    }

    public boolean isEditable(VisualSearchResult vsr, int index) {
        return index == actionColumnIndex;
    }

    public VisualSearchResult setColumnValue(
        VisualSearchResult vsr, Object value, int index) {
        // do nothing with the new value
        return vsr;
    }

    @Inject
    public void setIconManager(IconManager iconManager) {
        this.iconManager = iconManager;
    }
}