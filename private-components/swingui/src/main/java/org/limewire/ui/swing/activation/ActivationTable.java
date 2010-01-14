package org.limewire.ui.swing.activation;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItemComparator;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.table.AbstractTableFormat;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

class ActivationTable extends MouseableTable {

    @Resource
    private Icon infoIcon;
    
    private final DefaultEventTableModel<ActivationItem> model;
    
    public ActivationTable(EventList<ActivationItem> eventList) {
        GuiUtils.assignResources(this);

        model = new DefaultEventTableModel<ActivationItem>(new SortedList<ActivationItem>(eventList, new ActivationItemComparator()), new ActivationTableFormat());
        setModel(model);

        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setColumnSelectionAllowed(false);
        
        getColumnExt(ActivationTableFormat.LICENSE_TYPE_INDEX).setMinWidth(195);
        getColumnExt(ActivationTableFormat.LICENSE_TYPE_INDEX).setMaxWidth(195);
        
        getColumnExt(ActivationTableFormat.DATE_REGISTERED_INDEX).setMinWidth(108);
        getColumnExt(ActivationTableFormat.DATE_REGISTERED_INDEX).setMaxWidth(108);
        
//        getColumnExt(ActivationTableFormat.DATE_EXPIRE_INDEX).setMinWidth(135);
//        getColumnExt(ActivationTableFormat.DATE_EXPIRE_INDEX).setMaxWidth(135);
        
//        getColumn(ActivationTableFormat.LICENSE_TYPE_INDEX).setCellRenderer(new DefaultTableRenderer(new LicenseTest()));
        getColumn(ActivationTableFormat.LICENSE_TYPE_INDEX).setCellRenderer(new LicenseTypeEditorRenderer());
        getColumn(ActivationTableFormat.LICENSE_TYPE_INDEX).setCellEditor(new LicenseTypeEditorRenderer());
        
        getColumn(ActivationTableFormat.DATE_REGISTERED_INDEX).setCellRenderer(new DateRenderer());
        
        getColumn(ActivationTableFormat.DATE_EXPIRE_INDEX).setCellRenderer(new ExpiredRenderer());
        ExpiredRenderer expiredRenderer = new ExpiredRenderer();
        expiredRenderer.addActionListener(new InfoAction(expiredRenderer));
        getColumn(ActivationTableFormat.DATE_EXPIRE_INDEX).setCellEditor(expiredRenderer);
        
        setupHighlighters();
    }
    
    private void setupHighlighters() {
        TableColors tableColors = new TableColors();
        ColorHighlighter unsupportedHighlighter = new ColorHighlighter(new GrayHighlightPredicate(), 
                null, tableColors.getDisabledForegroundColor(), 
                null, tableColors.getDisabledForegroundColor());
        
        addHighlighter(unsupportedHighlighter);
    }
    
    private class GrayHighlightPredicate implements HighlightPredicate {
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            
            ActivationItem item = model.getElementAt(adapter.row);
            return item.getStatus() == Status.EXPIRED || item.getStatus() == Status.UNAVAILABLE || item.getStatus() == Status.UNUSEABLE_LW || item.getStatus() == Status.UNUSEABLE_OS;
//            LibraryNavItem navItem = libraryNavigatorPanel.getSelectedNavItem();
//            LocalFileItem item = libraryTable.getLibraryTableModel().getElementAt(adapter.row);
//            if( navItem.getType() == NavType.PUBLIC_SHARED || (navItem.getType() == NavType.LIST && ((SharedFileList)navItem.getLocalFileList()).getFriendIds().size() > 0))
//                return !item.isShareable(); 
//
//            return !item.isLoaded();
        }
    }
    
    private class ActivationTableFormat extends AbstractTableFormat<ActivationItem> { //implements AdvancedTableFormat<ActivationItem> {

        private static final int LICENSE_TYPE_INDEX = 0;
        private static final int DATE_REGISTERED_INDEX = 1;
        private static final int DATE_EXPIRE_INDEX = 2;
        
        public ActivationTableFormat() {
            super(I18n.tr("License Type"), I18n.tr("Date Registered"), I18n.tr("Expires"));
        }
        
        @Override
        public Object getColumnValue(ActivationItem baseObject, int column) {
            switch(column) {
            case LICENSE_TYPE_INDEX:
                return baseObject;
            case DATE_REGISTERED_INDEX:
                return baseObject.getDatePurchased();
            case DATE_EXPIRE_INDEX:
                return baseObject;
            }
            throw new IllegalStateException("Unknown column: " + column);
        }
//
//        @Override
//        public Class getColumnClass(int column) {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//        @Override
//        public Comparator getColumnComparator(int column) {
//            // TODO Auto-generated method stub
//            return null;
//        }
        
    }
    
//    private class LicenseTest extends ComponentProvider<JXPanel> {
//
//        JXPanel panel;
//        private JLabel nameLabel;
//        private HyperlinkButton licenseButton;
//        
//        @Override
//        protected void configureState(CellContext context) {
//            getDefaultVisuals().configureVisuals(panel, context);
////            System.out.println("\t" + context.getComponent().getForeground());
//        }
//
//        @Override
//        protected JXPanel createRendererComponent() { WrappingIconPanel pane;
//            panel = new JXPanel(new MigLayout("fill, insets 0 5 0 5, hidemode 3")){
//                @Override
//                public void setForeground(Color bg) {
//                    if(nameLabel != null)
//                        nameLabel.setForeground(bg);
////                    System.out.println("update " + bg);
//                }
//            };
////            panel.setFocusP
//            panel.setBorder(BorderFactory.createEmptyBorder());
//            nameLabel = new JLabel();
//            nameLabel.setBorder(BorderFactory.createEmptyBorder());
//            nameLabel.setVisible(false);
////            nameLabel.setFocusP
//            licenseButton = new HyperlinkButton(I18n.tr("Lost your license?"));
//            licenseButton.setVisible(false);
//            
//            
//            panel.add(nameLabel);
//            panel.add(licenseButton);
//            
//            return panel;
//        }
//
//        @Override
//        protected void format(CellContext context) {
////            System.out.println(context.getComponent().getForeground());
//            Object value = context.getValue();
//            if(value instanceof ActivationItem) {
//                nameLabel.setText(getText((ActivationItem) value));
//                nameLabel.setVisible(true);
//                nameLabel.setForeground(getForeground());
//                licenseButton.setVisible(false);
//                
//            } else {
//                nameLabel.setVisible(false);
//                licenseButton.setVisible(false);
//            }
//        }
//        
//        private String getText(ActivationItem item) {
//            if(item.isActiveVersion()) {
//                return item.getLicenseName();
//            } else {
//                return "<html><s>" + item.getLicenseName() + "</s></html>";
//            }
//        }
//
//    }
    
    private class LicenseTypeEditorRenderer extends TableRendererEditor {

        private final JLabel nameLabel;
        private final HyperlinkButton licenseButton;
        private final UrlAction licenseAction;
        
        public LicenseTypeEditorRenderer() {
            nameLabel = new JLabel();
            nameLabel.setVisible(false);
            licenseAction = new UrlAction(I18n.tr("Lost your license?"), "http://www.limewire.com/client_redirect/?page=gopro");
            licenseButton = new HyperlinkButton(licenseAction);
            licenseButton.setVisible(false);
            
            setLayout(new MigLayout("fill, insets 0 5 0 5, hidemode 3"));
            add(nameLabel);
            add(licenseButton);
        }
        
        @Override
        public void setForeground(Color bg) {
            if(nameLabel != null)
                nameLabel.setForeground(bg);
//            System.out.println("update " + bg);
        }
        
        @Override
        protected Component doTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            if(value instanceof LostLicenseItem) {
                LostLicenseItem item = (LostLicenseItem) value;
                nameLabel.setVisible(false);
                licenseButton.setText(item.getLicenseName());
                licenseButton.setVisible(true);
            } else if(value instanceof ActivationItem) {
                nameLabel.setText(getText((ActivationItem) value));
                nameLabel.setVisible(true);
                nameLabel.setForeground(getForeground());
                licenseButton.setVisible(false);
            } else {
                nameLabel.setVisible(false);
                licenseButton.setVisible(false);
            }
            return this;
        }

        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof LostLicenseItem) {
                LostLicenseItem item = (LostLicenseItem) value;
                nameLabel.setVisible(false);
                licenseButton.setText(item.getLicenseName());
                licenseAction.setURL(item.getLicenseName());
                licenseButton.setVisible(true);
            } else if(value instanceof ActivationItem) {
                nameLabel.setText(getText((ActivationItem) value));
                nameLabel.setVisible(true);
//                nameLabel.setForeground(getForeground());
                licenseButton.setVisible(false);
                
            } else {
                nameLabel.setVisible(false);
                licenseButton.setVisible(false);
            }
            return this;
        }
        
        private String getText(ActivationItem item) {
            if(item.getStatus() == Status.ACTIVE || item.getStatus() == Status.EXPIRED) {
                return item.getLicenseName();
            } else {
                return "<html><s>" + item.getLicenseName() + "</s></html>";
            }
        }
    }
    
    private class DateRenderer extends DefaultLimeTableCellRenderer {        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if(!(value instanceof Date)) {
                setText("");
            } else {
                setText(GuiUtils.msec2Date((Date)value)); 
            }
            return this;
        }
    }
    
    private class ExpiredRenderer extends TableRendererEditor {

        private final JLabel dateLabel;
        private final IconButton iconButton;
        private final HyperlinkButton renewButton;
        private final UrlAction renewAction;
        
        private ActivationItem cellEditorValue = null;
        
        public ExpiredRenderer() {
            dateLabel = new JLabel();
            iconButton = new IconButton(infoIcon);
            renewAction = new UrlAction(I18n.tr("Renew"), "http://www.limewire.com/client_redirect/?page=gopro");
            renewButton = new HyperlinkButton(renewAction);

            iconButton.setVisible(false);
//            IconButton.setIconButtonProperties(iconButton);
            renewButton.setVisible(false);
            
            setLayout(new MigLayout("fill, insets 0 5 0 5, hidemode 3"));
            add(dateLabel);
            add(iconButton);
            add(renewButton);
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
                ActivationItem item = (ActivationItem) value;
                dateLabel.setText(GuiUtils.msec2Date(item.getDateExpired())); 
                renewAction.setURL(item.getURL());
                iconButton.setVisible(item.getStatus() == Status.UNAVAILABLE || item.getStatus() == Status.UNUSEABLE_LW || item.getStatus() == Status.UNUSEABLE_OS);
                renewButton.setVisible(item.getStatus() == Status.EXPIRED);
                cellEditorValue = item;
            } else {
                dateLabel.setText("");
                iconButton.setVisible(false);
                renewButton.setVisible(false);
                cellEditorValue = null;
            }
            
            return this;
        }

        @Override
        protected Component doTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if(value instanceof ActivationItem && ((ActivationItem) value).getDateExpired() != null) {
                ActivationItem item = (ActivationItem) value;
                dateLabel.setText(GuiUtils.msec2Date(item.getDateExpired())); 
                iconButton.setVisible(item.getStatus() == Status.UNAVAILABLE || item.getStatus() == Status.UNUSEABLE_LW || item.getStatus() == Status.UNUSEABLE_OS);
                renewButton.setVisible(item.getStatus() == Status.EXPIRED);
            } else {
                dateLabel.setText("");
                iconButton.setVisible(false);
                renewButton.setVisible(false);
            }
            return this;
        }
    }
    
    private class InfoAction extends AbstractAction {

        private final ExpiredRenderer expiredRenderer;

        public InfoAction(ExpiredRenderer expiredRenderer) {
            this.expiredRenderer = expiredRenderer;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            ActivationItem item = expiredRenderer.getCellEditorValue();
            if (item != null) {
                String message = ActivationUtilities.getStatusMessage(item);
                FocusJOptionPane.showMessageDialog(ActivationTable.this.getRootPane().getParent(), message, item.getLicenseName(), JOptionPane.OK_OPTION);
                expiredRenderer.cancelCellEditing();
            }
        }
    }
}
