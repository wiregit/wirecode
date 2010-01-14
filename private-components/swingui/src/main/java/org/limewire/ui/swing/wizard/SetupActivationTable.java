package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationItemComparator;
import org.limewire.activation.api.ActivationItem.Status;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class SetupActivationTable extends JPanel {

    private final String[] columnNames = new String[] {I18n.tr("License Type"), I18n.tr("Expires")};
    
    @Resource private Color columnNameColor;
    @Resource private Icon checkIcon;
    @Resource private Color headerBackgroundColor;
    @Resource private Font headerFont;
    
    private JTable table;
    
    private WizardPage wizardPage;

    public SetupActivationTable(WizardPage wizardPage, List<ActivationItem> activationItems) {
        GuiUtils.assignResources(this);

        this.wizardPage = wizardPage;
        
        Collections.sort(activationItems, new ActivationItemComparator());

        table = new JTable(new ActivationTableModel(activationItems));

        table.getTableHeader().setDefaultRenderer(new TableHeaderRenderer());
        table.getTableHeader().setMinimumSize(new Dimension(100, 50));

        table.getColumn(columnNames[0]).setCellRenderer(new LicenseTypeRenderer());
        table.getColumn(columnNames[0]).setMinWidth(200);
        table.getColumn(columnNames[1]).setCellRenderer(new DateRenderer());
        table.getColumn(columnNames[1]).setMinWidth(100);
        table.getColumn(columnNames[1]).setMaxWidth(150);
        table.setRowHeight( 29 );

        JTableHeader header = table.getTableHeader();
        header.setMinimumSize(new Dimension(350, 27));
        header.setPreferredSize(new Dimension(350, 27));
        setLayout(new BorderLayout()); 
        add(header, BorderLayout.NORTH); 
        add(table, BorderLayout.CENTER); 
        add(Box.createVerticalStrut(10), BorderLayout.SOUTH); 
        
        setBorder(BorderFactory.createLineBorder(columnNameColor));

        setMinimumSize(new Dimension(350, 27 + activationItems.size() * 29 + 10));
        setPreferredSize(new Dimension(350, 27 + activationItems.size() * 29 + 10));
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
            return false; 
        }
    }

    private class LicenseTypeRenderer extends JPanel implements TableCellRenderer {

        private final IconButton checkMark;
        private final JLabel nameLabel;
        private final Component strut;
        
        public LicenseTypeRenderer() {
            nameLabel = wizardPage.createAndDecorateMultiLine("");
            nameLabel.setVisible(true);
            
            checkMark = new IconButton(checkIcon);
            checkMark.setVisible(false);
            
            strut = Box.createHorizontalStrut(17);
            strut.setVisible(false);
            
            setLayout(new MigLayout("filly, insets 0 5 0 5, hidemode 3"));
            add(checkMark, "align 0% 50%");
            add(strut, "align 0% 50%");
            add(nameLabel, "align 0% 50%");
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, 
                                                       int row, int column) {
            ActivationItem item = (ActivationItem) value;
            
            checkMark.setVisible(item.getStatus() == Status.ACTIVE);
            strut.setVisible(item.getStatus() != Status.ACTIVE);
            nameLabel.setEnabled(item.getStatus() == Status.ACTIVE);
            nameLabel.setText(item.getLicenseName());
            if (item.getStatus() != Status.ACTIVE) {
                nameLabel.setForeground(Color.LIGHT_GRAY);
            }

            return this;
        }
    }

    private class DateRenderer extends JPanel implements TableCellRenderer {        
        private final JLabel nameLabel;

        public DateRenderer() {
            nameLabel = wizardPage.createAndDecorateMultiLine("");
            nameLabel.setVisible(true);

            setLayout(new MigLayout("fill, insets 0 5 0 5, hidemode 3"));
            add(nameLabel, "align 0% 50%");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            ActivationItem item = (ActivationItem) value;

            nameLabel.setText(GuiUtils.msec2Date(item.getDateExpired())); 
            nameLabel.setEnabled(item.getStatus() == Status.ACTIVE);

            if (item.getStatus() != Status.ACTIVE) {
                nameLabel.setForeground(Color.LIGHT_GRAY);
            }

            return this;
        }
    }
    
    private class TableHeaderRenderer extends JPanel implements TableCellRenderer {        
        JLabel nameLabel;

        public TableHeaderRenderer() {
            nameLabel = wizardPage.createAndDecorateHeader("");
            nameLabel.setFont(headerFont);
            nameLabel.setForeground(columnNameColor);
            
            setLayout(new MigLayout("fill, insets 0 5 0 5, hidemode 3"));
            add(nameLabel);

            setBackground(headerBackgroundColor);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (column == 0) {
                nameLabel.setText(value.toString());
            } else {
                nameLabel.setText(value.toString());
            }

            return this;
        }
    }

}
