package org.limewire.ui.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import org.jdesktop.swingx.table.TableColumnExt;
import org.limewire.ui.swing.table.MouseableTable;

/**
 * This class is a subclass of JTable that adds features from Glazed Lists.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ConfigurableTable<E> extends MouseableTable {

    private EventList<E> eventList;
    private EventTableModel<E> tableModel;
    private JMenuItem disabledMenuItem;
    private JPopupMenu headerPopup;
    private TableFormat<E> tableFormat;

    public ConfigurableTable() {
        // Configure so resizing a column resizes the table, not other columns.
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        // Set up table headers to have a context menu
        // that configures which columns are visible.
        final JTableHeader header = getTableHeader();
        header.setToolTipText("Right-click to select columns to display");
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only honor right-clicks.
                // TODO: Why doesn't the following line work?
                //if (!e.isPopupTrigger()) return;
                if (e.getButton() != 3) return;

                // If a header popup menu was created ...
                if (headerPopup != null) {
                    // Display the header popup menu where the user clicked.
                    headerPopup.show(
                        ConfigurableTable.this,
                        e.getX(), e.getY() - header.getHeight());
                }
            }
        });

        // Size all the columns.
        TableColumnModel tcm = getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(100);
        }
    }

    /**
     * Makes the header popup menu.
     */
    private void makeHeaderPopup() {
        // Create a listener that toggles the visibility
        // of the selected table column.
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String name = e.getActionCommand();
                TableColumnExt selectedColumn =
                    ConfigurableTable.this.getColumnExt(name);
                boolean visible = selectedColumn.isVisible();

                selectedColumn.setVisible(!visible);

                managePopup();
            }
        };

        // Get the column headings in sorted order.
        Set<String> headerSet = new TreeSet<String>();
        int columnCount = tableFormat.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            headerSet.add(tableFormat.getColumnName(i));
        }

        headerPopup = new JPopupMenu();
        int margin = 5;
        headerPopup.setBorder(
            //BorderFactory.createEmptyBorder(margin, margin, margin, margin));
            BorderFactory.createLineBorder(Color.BLACK, 1));

        // Create a checkbox menu item for each column heading.
        for (String headerName : headerSet) {
            JMenuItem item = new JCheckBoxMenuItem(headerName, true);
            item.addActionListener(listener);
            headerPopup.add(item);
        }
    }

    /**
     * Handle disabling and enabling header popup menu items
     * based on the number of visible columns.
     */
    private void managePopup() {
        int visibleColumnCount = getColumnCount(false);

        // If there is only one visible column in the table ...
        if (visibleColumnCount == 1) {
            // Find the menu item for the lone visible column and
            // disable it for now so the user can't ask to hide it.
            int allColumnCount = getColumnCount(true);
            TableColumnExt column = null;
            for (int i = 0; i < allColumnCount; i++) {
                column = getColumnExt(i);
                if (column.isVisible()) break;
            }

            String headerName = column.getTitle();

            // Find the header popup menu item this this name.
            Component[] components = headerPopup.getComponents();
            for (Component component : components) {
                JMenuItem item = (JMenuItem) component; 
                if (item.getText().equals(headerName)) {
                    item.setEnabled(false);
                    disabledMenuItem = item;
                    break;
                }
            }
        } else if (disabledMenuItem != null) {
            // Reenable the previously disabled menu item
            // because it's column is no longer the only one visible.
            disabledMenuItem.setEnabled(true);
        }
    }

    /**
     * Sets the objects whose data should be displayed in this table.
     * @param objects the objects
     */
    public void setData(Collection<E> objects) {
        eventList = new BasicEventList<E>();
        for (E object : objects) eventList.add(object);
    }

    /**
     * Sets the EventList table model from which
     * the data to be displayed is obtained.
     * @param eventList the EventList
     */
    public void setEventList(EventList<E> eventList) {
        this.eventList = eventList;
    }

    /**
     * Sets the width of a given column.
     * @param columnIndex the column index
     * @param width the width
     */
    public void setColumnWidth(int columnIndex, int width) {
        getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
    }

    /**
     * Set the object that controls the columns displayed in this table.
     * @param tableFormat the TableFormat
     */
    public void setTableFormat(TableFormat<E> tableFormat) {
        if (tableFormat == null) {
            throw new IllegalArgumentException("tableFormat can't be null");
        }

        this.tableFormat = tableFormat;
        tableModel = new EventTableModel<E>(eventList, tableFormat);
        setModel(tableModel);

        makeHeaderPopup();
    }

    /**
     * This is overridden to prevent a java.lang.IllegalStateException
     * that occurs because a reverse mapping function must be specified
     * to support this operation.
     * @param aValue the new value
     * @param row the row
     * @param column the column
     */
    @Override
    public void setValueAt(Object aValue, int row, int column) {
    }
}