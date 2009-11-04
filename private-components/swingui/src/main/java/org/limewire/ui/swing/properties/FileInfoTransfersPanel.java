package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadSourceInfo;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.io.Address;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTransfersPanel implements FileInfoPanel {

    @Resource private Icon lockIcon;
    @Resource private Color foreground;
    @Resource private Font smallFont;
    @Resource private Font headerFont;
    
    private final JPanel component;
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private DownloadStatusListener downloadStatus;
    
    private final JXTable infoTable;
    
    private final Timer refreshTimer;
    private final DefaultTableModel model;
    
    public FileInfoTransfersPanel(FileInfoType type, PropertiableFile propertiableFile, TableDecorator tableDecorator) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        component.add(createHeaderLabel(I18n.tr("Downloading from")),"push");
        
        final JLabel percentLabel = createPlainLabel("");            
        component.add(percentLabel, "alignx right, wrap");
                
        downloadStatus = new DownloadStatusListener(percentLabel);
        ((DownloadItem)propertiableFile).addPropertyChangeListener(downloadStatus);
        
        model = new DefaultTableModel();
        infoTable = new JXTable(model);
        
        tableDecorator.decorate(infoTable);
        
        infoTable.setSortable(false);
        infoTable.setRowSelectionAllowed(false);
        infoTable.setColumnSelectionAllowed(false);
        infoTable.setCellSelectionEnabled(false);
        infoTable.setShowGrid(false, false);
        
        
        
        component.add(new JScrollPane(infoTable), "span, grow, wrap");
        
        init();
        refreshTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                init();
            }
        });
        
        // Update by polling to avoid putting too much of a burden on the ui
        //  with an active torrent.
        refreshTimer.start();
    }
    
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        //component never changes state
    }    

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
        //do nothing
    }
    
    @Override
    public void dispose() {
        
        refreshTimer.stop();
        
        if(downloadStatus != null && propertiableFile instanceof DownloadItem) {
            ((DownloadItem)propertiableFile).removePropertyChangeListener(downloadStatus);
        }
    }
    
    private void init() {
        
        switch(type) {
        case DOWNLOADING_FILE:
            for ( int i=0 ; i<model.getRowCount() ; i++ ) {
                model.removeRow(0);
            }

            DownloadItem download = ((DownloadItem)propertiableFile);
            
            // TODO: remove this if when gnutella source info is complete
            if (download.getDownloadItemType() == DownloadItemType.GNUTELLA) {
                model.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
                for(Address source : download.getSources()) {
                    model.addRow(new Object[] {source.getAddressDescription(), 
                            download.getDownloadingFile().getName() });
                }
            }
            else if (download.getDownloadItemType() ==  DownloadItemType.BITTORRENT) {
                    
                model.setColumnIdentifiers(new Object[]{tr("Address"),
                        "", tr("Client"),
                        tr("Upload"), tr("Download")});
                  
                for( DownloadSourceInfo info : download.getSourcesDetails() ) {
                    model.addRow(new Object[] {info.getIPAddress(),
                            info.isEncyrpted(), 
                            info.getClientName(),
                            I18n.tr("{0} KB/s", info.getUploadSpeed()),
                            I18n.tr("{0} KB/s", info.getDownloadSpeed())});
                }
                
                TableColumn column = infoTable.getColumn(1);
                column.setCellRenderer(new LockRenderer());
                column.setMaxWidth(10);
                column.setMinWidth(10);
                column.setWidth(12);
            }
            break;
        }
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        label.setForeground(foreground);
        return label;
    }
    
    private JLabel createPlainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont);
        label.setForeground(foreground);
        return label;
    }
    
    /**
     * Listens for changes to the download status and updates the dialog.
     */
    private class DownloadStatusListener implements PropertyChangeListener {
        private final JLabel label;
        
        public DownloadStatusListener(JLabel label) {
            this.label = label;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    label.setText(tr("{0}% complete", ((DownloadItem)propertiableFile).getPercentComplete()));
                } 
            });
        }
    }
    
    private class LockRenderer extends DefaultLimeTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (value == Boolean.TRUE) {
                setIcon(lockIcon);
            }
            else {
                setIcon(null);
            }
  
            return this;
            
        }
    }
}
