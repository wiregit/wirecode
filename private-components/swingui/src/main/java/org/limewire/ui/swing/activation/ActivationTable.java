package org.limewire.ui.swing.activation;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.activation.api.ActivationSettingsController;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.CalendarRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableCellHeaderRenderer;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

/**
 * Displays information about the Modules that are associated with the 
 * given License Key.
 */
class ActivationTable extends MouseableTable {

    @Resource
    private Icon infoIcon;
    @Resource
    private Color expiredColor;
    @Resource
    private Font rendererFont;
    @Resource
    private int columnWidth;
    
    private final DefaultEventTableModel<ActivationItem> model;
    private final Application application;

    public ActivationTable(EventList<ActivationItem> eventList, CalendarRenderer calendarRenderer, Application application) {
        GuiUtils.assignResources(this);

        model = new DefaultEventTableModel<ActivationItem>(new SortedList<ActivationItem>(eventList, new ActivationItemComparator()), new ActivationTableFormat());
        setModel(model);

        this.application = application;

        // the column widths need to be wider for osx to support the error messages.
        if(OSUtils.isMacOSX())
            columnWidth += 10;
        
        initTable();
               
        getColumnExt(ActivationTableFormat.DATE_REGISTERED_INDEX).setMinWidth(columnWidth);
        getColumnExt(ActivationTableFormat.DATE_REGISTERED_INDEX).setMaxWidth(columnWidth);
        
        getColumnExt(ActivationTableFormat.DATE_EXPIRE_INDEX).setMinWidth(columnWidth);
        getColumnExt(ActivationTableFormat.DATE_EXPIRE_INDEX).setMaxWidth(columnWidth);
        
        getColumn(ActivationTableFormat.MODULE_TYPE_INDEX).setCellRenderer(new LicenseTypeEditorRenderer());
        getColumn(ActivationTableFormat.MODULE_TYPE_INDEX).setCellEditor(new LicenseTypeEditorRenderer());
        
        getColumn(ActivationTableFormat.DATE_REGISTERED_INDEX).setCellRenderer(calendarRenderer);
        
        getColumn(ActivationTableFormat.DATE_EXPIRE_INDEX).setCellRenderer(new ExpiredRendererEditor());
        ExpiredRendererEditor expiredRenderer = new ExpiredRendererEditor();
        expiredRenderer.addActionListener(new ActivationInfoAction(expiredRenderer, this, application));
        getColumn(ActivationTableFormat.DATE_EXPIRE_INDEX).setCellEditor(expiredRenderer);
        
        setupHighlighters();
    }
    
    @Override
    protected void setTableHeaderRenderer() {
        JTableHeader th = getTableHeader();
        TableCellHeaderRenderer renderer = new TableCellHeaderRenderer();
        renderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        th.setDefaultRenderer(renderer);
    }
    
    @Override
    public String getToolTipText(MouseEvent event) {
        return null;    
    }
    
    private void initTable() {
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(false);
        setCellSelectionEnabled(false);
        setFillsViewportHeight(true);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(false);
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
            
            ActivationItem item = model.getElementAt(adapter.row);
            return item.getStatus() == Status.EXPIRED || item.getStatus() == Status.UNAVAILABLE || item.getStatus() == Status.UNUSEABLE_LW || item.getStatus() == Status.UNUSEABLE_OS;
        }
    }
    
    /**
     * TableFormat for an ActivationTable.
     */
    private class ActivationTableFormat extends AbstractTableFormat<ActivationItem> {

        private static final int MODULE_TYPE_INDEX = 0;
        private static final int DATE_REGISTERED_INDEX = 1;
        private static final int DATE_EXPIRE_INDEX = 2;
        
        public ActivationTableFormat() {
            super(I18n.tr("Feature"), I18n.tr("Registered"), I18n.tr("Expires"));
        }
        
        @Override
        public Object getColumnValue(ActivationItem baseObject, int column) {
            switch(column) {
            case MODULE_TYPE_INDEX:
                return baseObject;
            case DATE_REGISTERED_INDEX:
                return baseObject.getDatePurchased();
            case DATE_EXPIRE_INDEX:
                return baseObject;
            }
            throw new IllegalStateException("Unknown column: " + column);
        }
    }

    /**
     * Renderers the Name of a the Module in the Table. If no Modules
     * exist will render a hyperlink to purchase Pro.
     */
    private class LicenseTypeEditorRenderer extends TableRendererEditor {

        private final JLabel nameLabel;
        private final HyperlinkButton licenseButton;
        private final UrlAction licenseAction;
        
        public LicenseTypeEditorRenderer() {
            nameLabel = new JLabel();
            nameLabel.setVisible(false);
            nameLabel.setFont(rendererFont);
            String accountSettingsUrl = application.addClientInfoToUrl(ActivationSettingsController.ACCOUNT_SETTINGS_URL);
            licenseAction = new UrlAction(I18n.tr("Lost your license?"), accountSettingsUrl);
            licenseButton = new HyperlinkButton(licenseAction);
            licenseButton.setVisible(false);
            
            setLayout(new MigLayout("fill, insets 0 5 0 5, hidemode 3"));
            add(nameLabel);
            add(licenseButton);
        }
        
        /**
         * This will update the foreground color to use any Highlighter that
         * may be in effect.
         */
        @Override
        public void setForeground(Color bg) {
            if(nameLabel != null)
                nameLabel.setForeground(bg);
        }
        
        @Override
        protected Component doTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            return updateComponent(table, value, isSelected, false, row, column);
        }

        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return updateComponent(table, value, isSelected, hasFocus, row, column);
        }
        
        private Component updateComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            if(value instanceof LostLicenseItem) {
                LostLicenseItem item = (LostLicenseItem) value;
                nameLabel.setVisible(false);
                licenseButton.setText(item.getLicenseName());
                licenseAction.setURL(item.getURL());
                licenseButton.setVisible(true);
            } else if(value instanceof ActivationItem) {
                nameLabel.setText(getText((ActivationItem) value));
                nameLabel.setVisible(true);
                licenseButton.setVisible(false);
            } else {
                nameLabel.setVisible(false);
                licenseButton.setVisible(false);
            }
            return this;
        }
        
        private String getText(ActivationItem item) {
            if(item.getStatus() == Status.ACTIVE) {
                return item.getLicenseName();
            } else {
                return "<html><s>" + item.getLicenseName() + "</s></html>";
            }
        }
    }
    
    /**
     * Renderers the date a Module expires. If there is a problem with this
     * module will also renderer clickable items. 
     */
    private class ExpiredRendererEditor extends TableRendererEditor {
        private final JLabel dateLabel;
        private final IconButton iconButton;
        private final JLabel expiredLabel;

        private ActivationItem cellEditorValue = null;
        
        public ExpiredRendererEditor() {
            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            dateLabel = new JLabel();
            dateLabel.setFont(rendererFont);
            iconButton = new IconButton(infoIcon);
            expiredLabel = new JLabel(I18n.tr("Expired"));
            expiredLabel.setForeground(expiredColor);
            expiredLabel.setFont(rendererFont);

            iconButton.setVisible(false);
            expiredLabel.setVisible(false);
            FontMetrics metrics = getFontMetrics(rendererFont);
            int fontWidth = metrics.stringWidth("12/30/30");
            
            setLayout(new MigLayout("fill, insets 0, hidemode 3", "[][grow]", "[]"));

            add(dateLabel, "alignx left, width " + fontWidth + "!, aligny 50%");
            add(iconButton, "split 2, align 50%");
            add(expiredLabel, "align 50%");
        }
        
        public void addActionListener(ActionListener listener) {
            iconButton.addActionListener(listener);
        }

        @Override
        public ActivationItem getCellEditorValue() {
            return cellEditorValue;
        }
        
        @Override
        public void setForeground(Color bg) {
            if(dateLabel != null)
                dateLabel.setForeground(bg);
        }
        
        @Override
        protected Component doTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if(value instanceof ActivationItem && ((ActivationItem) value).getDateExpired() != null) {
                cellEditorValue = (ActivationItem) value;
            } else {
                cellEditorValue = null;
            }
            
            return updateComponent(table, value, isSelected, row, column);
        }

        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return updateComponent(table, value, isSelected, row, column);
        }
        
        private Component updateComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if(value instanceof ActivationItem && ((ActivationItem) value).getDateExpired() != null) {
                ActivationItem item = (ActivationItem) value;
                dateLabel.setText(GuiUtils.date2String(item.getDateExpired())); 
                iconButton.setVisible(item.getStatus() == Status.UNAVAILABLE || item.getStatus() == Status.UNUSEABLE_LW || item.getStatus() == Status.UNUSEABLE_OS);
                expiredLabel.setVisible(item.getStatus() == Status.EXPIRED);
            } else {
                dateLabel.setText("");
                iconButton.setVisible(false);
                expiredLabel.setVisible(false);
            }
            return this;
        }
    }
}
