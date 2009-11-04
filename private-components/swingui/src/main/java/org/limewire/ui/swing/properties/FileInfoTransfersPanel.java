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
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadSourceInfo;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTransfersPanel implements FileInfoPanel {

    @Resource private Icon lockIcon;
    @Resource private Color foreground;
    @Resource private Font smallFont;
    
    private final JPanel component;
    private final FileInfoType type;
    private final DownloadItem download;
    private DownloadStatusListener downloadStatus;
    
    private final JXTable infoTable;
    
    private final Timer refreshTimer;
    private final JLabel leechersLabel;
    private final JLabel seedersLabel;
    
    public FileInfoTransfersPanel(FileInfoType type, PropertiableFile propertiableFile, TableDecorator tableDecorator) {
        
        if (!(propertiableFile instanceof DownloadItem)) {
            throw new UnsupportedOperationException("Can't create a transfers panel on anything other than a Download");
        }
        
        this.type = type;
        this.download = (DownloadItem) propertiableFile;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx, gap 0"));
        
        infoTable = new JXTable();
        
        tableDecorator.decorate(infoTable);
        
        infoTable.setSortable(false);
        infoTable.setRowSelectionAllowed(false);
        infoTable.setColumnSelectionAllowed(false);
        infoTable.setCellSelectionEnabled(false);
        infoTable.setShowGrid(false, false);
        
        component.add(new JScrollPane(infoTable), "gaptop 10, span, grow, wrap");
        
        component.add(createBoldLabel(I18n.tr("Total Completed:")), "split 2, gaptop 10");
        JLabel percentLabel = createPlainLabel("");
        component.add(percentLabel, "wrap");
        downloadStatus = new DownloadStatusListener(percentLabel);
        ((DownloadItem)propertiableFile).addPropertyChangeListener(downloadStatus);

        if (download.getDownloadItemType() ==  DownloadItemType.BITTORRENT) {
            component.add(createBoldLabel(I18n.tr("Total Leechers:")), "split 2");
            leechersLabel = createPlainLabel("");
            component.add(leechersLabel, "wrap");
        
            component.add(createBoldLabel(I18n.tr("Total Seeders:")), "split 2");
            seedersLabel = createPlainLabel("");
            component.add(seedersLabel, "wrap");
        } 
        else {
            leechersLabel = null;
            seedersLabel = null;
        }
        
        
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
        
        if(downloadStatus != null) {
            download.removePropertyChangeListener(downloadStatus);
        }
    }
    
    private void init() {
        
        switch(type) {
        case DOWNLOADING_FILE:

            DefaultTableModel model = new DefaultTableModel();

            model.setColumnIdentifiers(new Object[]{tr("Address"),
                    "", tr("Client"),
                    tr("Upload"), tr("Download")});
                  
            for( DownloadSourceInfo info : download.getSourcesDetails() ) {
                model.addRow(new Object[] {info.getIPAddress(),
                        info.isEncyrpted(), 
                        info.getClientName(),
                        GuiUtils.rate2UnitSpeed(info.getUploadSpeed()),
                        GuiUtils.rate2UnitSpeed(info.getDownloadSpeed())});
            }
                
            infoTable.setModel(model);
                
            TableColumn column = infoTable.getColumn(1);
            column.setCellRenderer(new LockRenderer());
            column.setMaxWidth(12);
            column.setMinWidth(12);
            column.setWidth(12);
            
            if (download.getDownloadItemType() ==  DownloadItemType.BITTORRENT) {
                Torrent torrent = (Torrent) download.getDownloadProperty(DownloadPropertyKey.TORRENT);
                TorrentStatus status = torrent.getStatus();
                seedersLabel.setText((status.getNumComplete() < 0) ? "?" : (""+status.getNumComplete()));
                leechersLabel.setText((status.getNumIncomplete() < 0) ? "?" : (""+status.getNumIncomplete()));
            }
            
            break;
        }
    }
    
    private JLabel createBoldLabel(String text) {
        JLabel label = createPlainLabel(text);
        FontUtils.bold(label);        
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
                    label.setText(tr("{0}%", download.getPercentComplete()));
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
