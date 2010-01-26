package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.activation.ActivationItemComparator;
import org.limewire.ui.swing.activation.ActivationUtilities;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.BasicJXTable;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

class SetupActivationTable extends BasicJXTable {

    private final String[] columnNames = new String[] {I18n.tr("Feature Type"), I18n.tr("Expires")};
    
    @Resource private Color columnNameColor;
    @Resource private Icon checkIcon;
    @Resource private Color headerBackgroundColor;
    @Resource private Font headerFont;
    @Resource private Icon infoIcon;
    
    protected MouseMotionListener mouseOverEditorListener;

    public SetupActivationTable(List<ActivationItem> activationItems) {
        super();
        
        GuiUtils.assignResources(this);

        setModel(new ActivationTableModel(activationItems));
        
        Collections.sort(activationItems, new ActivationItemComparator());

        getTableHeader().setDefaultRenderer(new TableHeaderRenderer());

        getColumn(columnNames[0]).setCellRenderer(new LicenseTypeRendererEditor());
        getColumn(columnNames[0]).setCellEditor(new LicenseTypeRendererEditor());
        getColumn(columnNames[0]).setMinWidth(200);
        getColumn(columnNames[1]).setCellRenderer(new DateRenderer());
        getColumn(columnNames[1]).setMinWidth(100);
        getColumn(columnNames[1]).setMaxWidth(150);
        setBorder(BorderFactory.createEmptyBorder());

        JTableHeader header = getTableHeader();
        header.setMinimumSize(new Dimension(400, 27));
        header.setPreferredSize(new Dimension(400, 27));
        
        initTable();
        setupHighlighters();
    }
    
    public Color getHeaderColor() {
        return headerBackgroundColor;
    }
    
    private void initTable() {
        setRowHeight(29);
        setShowGrid(false, false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnSelectionAllowed(false);
        setFillsViewportHeight(true);
        setSortable(false);
        
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(false);
        
        //so that mouseovers will work within table     
        mouseOverEditorListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Get the table cell that the mouse is over.
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                
                // If the cell is editable and
                // it's not already being edited ...
                if (isCellEditable(row, col) && (row != getEditingRow() || col != getEditingColumn())) {
                    editCellAt(row, col);
                } else {
                    maybeCancelEditing();
                }
            }
        };
        
        addMouseMotionListener(mouseOverEditorListener);
    }
    
    private void setupHighlighters() {
        TableColors tableColors = new TableColors();
        ColorHighlighter unsupportedHighlighter = new ColorHighlighter(new GrayHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        
        addHighlighter(unsupportedHighlighter);
    }
    
    /**
     * Highlights the text of a row gray when there is a problem with that Module.
     */
    private class GrayHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            ActivationItem item = (ActivationItem) getModel().getValueAt(adapter.row, 0);
            return item.getStatus() != Status.ACTIVE;
        }
    }
    
    private class ActivationTableModel extends AbstractTableModel {
        private List<ActivationItem> activationItems;
        
        public ActivationTableModel(final List<ActivationItem> activationItems) {
            this.activationItems = activationItems;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col].toString();
        }
        public int getRowCount() { return activationItems.size(); }

        public int getColumnCount() { return 2; }

        public Object getValueAt(int row, int col) {
            return activationItems.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int col) { 
            return activationItems.get(row).getStatus() != ActivationItem.Status.ACTIVE;
        }
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
        return getColumnModel().getColumn(col).getCellEditor() != null;
    }
    
    //Don't set the cell value when editing is cancelled
    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {          
            removeEditor();
        }
    }
    
    //clears mouseover color
    private void maybeCancelEditing() {
        Point mousePosition = getMousePosition();
        if (getCellEditor() != null && 
                (mousePosition == null || rowAtPoint(mousePosition) == -1 || columnAtPoint(mousePosition) == -1)){
            getCellEditor().cancelCellEditing();
        } 
    }
    
    private class LicenseTypeRendererEditor extends TableRendererEditor {

        private final IconButton checkMarkButton;
        private final JLabel nameLabel;
        private final Component strut1;
        private final Component strut2;
        private final IconButton infoButton;
        private ActivationItem cellEditorValue = null;
        
        public LicenseTypeRendererEditor() {
            nameLabel = new JLabel();
            
            checkMarkButton = new IconButton(checkIcon);
            checkMarkButton.setVisible(false);
            
            strut1 = Box.createHorizontalStrut(1);
            strut1.setVisible(false);

            strut2 = Box.createHorizontalStrut(0);
            strut2.setVisible(false);
            
            infoButton = new IconButton(infoIcon);
            infoButton.addActionListener(new InfoAction(this));
            infoButton.setVisible(false);
            
            setLayout(new MigLayout("filly, insets 0 5 0 5, hidemode 3"));
            add(checkMarkButton, "align 0% 50%");
            add(strut1, "align 0% 50%, hidemode 3");
            add(infoButton, "align 0% 50%, hidemode 3");
            add(strut2, "align 0% 50%, hidemode 3");
            add(nameLabel, "align 0% 50%");

            setBorder(BorderFactory.createEmptyBorder());
        }
        
//        @Override
//        public void setForeground(Color color) {
//            super.setForeground(color);
//            if(nameLabel != null)
//                nameLabel.setForeground(color);
//        }
        
        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            ActivationItem item = (ActivationItem) value;
            
            updateComponents(item);

            return this;
        }
        
        @Override
        protected Component doTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            cellEditorValue = (ActivationItem) value;
            updateComponents(cellEditorValue);

            return this;
        }

        private void updateComponents(ActivationItem item) {
            checkMarkButton.setVisible(item.getStatus() == Status.ACTIVE);
            strut1.setVisible(item.getStatus() != Status.ACTIVE);
            infoButton.setVisible(item.getStatus() != Status.ACTIVE);
            strut2.setVisible(item.getStatus() != Status.ACTIVE);
            
            if (item.getStatus() == Status.ACTIVE) {
                nameLabel.setText(item.getLicenseName());
                nameLabel.setForeground(Color.BLACK);
            } else {
                nameLabel.setText("* " + item.getLicenseName());
                nameLabel.setForeground(Color.GRAY);
            }
        }
        
        @Override
        public ActivationItem getCellEditorValue() {
            return cellEditorValue;
        }
    }
    
    private class InfoAction extends AbstractAction {

        private final LicenseTypeRendererEditor licenseRenderer;

        public InfoAction(LicenseTypeRendererEditor licenseRenderer) {
            this.licenseRenderer = licenseRenderer;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            ActivationItem item = licenseRenderer.getCellEditorValue();
            if (item != null) {
                String message = ActivationUtilities.getStatusMessage(item);
                FocusJOptionPane.showMessageDialog(SetupActivationTable.this.getRootPane().getParent(), message, item.getLicenseName(), JOptionPane.OK_OPTION);
                licenseRenderer.cancelCellEditing();
            }
        }
    }

    private class DateRenderer extends DefaultLimeTableCellRenderer {        
        public DateRenderer() {
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            ActivationItem item = (ActivationItem) value;

            setText(GuiUtils.msec2Date(item.getDateExpired())); 

            return this;
        }
    }
    
    private class TableHeaderRenderer extends JLabel implements TableCellRenderer {        
        public TableHeaderRenderer() {
            setFont(headerFont);
            setForeground(columnNameColor);
            setBackground(headerBackgroundColor);
            setOpaque(true);
            setHorizontalAlignment(JLabel.LEADING);
            setHorizontalTextPosition(JLabel.LEFT);
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText(value.toString());

            return this;
        }
    }

}
