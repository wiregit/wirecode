package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
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
import org.limewire.core.api.download.SourceInfo;
import org.limewire.core.api.download.UploadPropertyKey;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
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
    private final DownloadItem download;
    private final UploadItem upload;
    
    private DownloadStatusListener downloadStatus;
    
    private JXTable infoTable;
    
    private Timer refreshTimer;
    private JLabel leechersLabel;
    private JLabel seedersLabel;
    
    private Torrent torrent;
    
    public FileInfoTransfersPanel(FileInfoType type, PropertiableFile file, TableDecorator tableDecorator) {
        
        component = new JPanel(new MigLayout("fillx, gap 0"));
        
        if (file instanceof DownloadItem) {
            download = (DownloadItem) file;
            upload = null;
        }
        else if (file instanceof UploadItem) {
            download = null;
            upload = (UploadItem) file;
        } else {
            download = null;
            upload = null;
            return;
        }
        
        GuiUtils.assignResources(this);
        
        infoTable = new JXTable();
        
        tableDecorator.decorate(infoTable);
        
        infoTable.setSortable(false);
        infoTable.setCellSelectionEnabled(false);
        infoTable.setShowGrid(false, false);
        infoTable.setEditable(false);
        
        component.add(new JScrollPane(infoTable), "gaptop 10, span, grow, wrap");
        
        if (download != null) {
            component.add(createBoldLabel(I18n.tr("Total Completed:")), "split 2, gaptop 10");
            JLabel percentLabel = createPlainLabel("");
            component.add(percentLabel, "wrap");
            
            downloadStatus = new DownloadStatusListener(percentLabel);
            download.addPropertyChangeListener(downloadStatus);
        } 

        if ((download != null && download.getDownloadItemType() ==  DownloadItemType.BITTORRENT) 
            || (upload != null && upload.getUploadItemType() ==  UploadItemType.BITTORRENT)) {
            
            if (download != null) {
                torrent = (Torrent)download.getDownloadProperty(DownloadPropertyKey.TORRENT);
            } 
            else {
                torrent = (Torrent)upload.getUploadProperty(UploadPropertyKey.TORRENT);
            }
            
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
        DefaultTableModel model = new DefaultTableModel();

        model.setColumnIdentifiers(new Object[]{tr("Address"),
                "", tr("Client"),
                tr("Upload"), tr("Download")});
                 
        List<SourceInfo> sources;
        if (download != null) {
            sources = download.getSourcesDetails();
        } else {
            sources = upload.getSourcesDetails();
        }
                
        for( SourceInfo info : sources ) {
            model.addRow(new Object[] {info.getIPAddress(),
                    info.isEncyrpted(), 
                    info.getClientName(),
                    GuiUtils.formatUnitFromBytesPerSec(Math.round(info.getUploadSpeed())),
                    GuiUtils.formatUnitFromBytesPerSec(Math.round(info.getDownloadSpeed()))});
        }
                
        infoTable.setModel(model);
                
        TableColumn column = infoTable.getColumn(1);
        column.setCellRenderer(new LockRenderer());
        column.setMaxWidth(12);
        column.setMinWidth(12);
        column.setWidth(12);
            
        // Add leecher/seeder info if BT
        if (torrent != null) {
            TorrentStatus status = torrent.getStatus();
            seedersLabel.setText((status.getNumComplete() < 0) ? "?" : (""+status.getNumComplete()));
            leechersLabel.setText((status.getNumIncomplete() < 0) ? "?" : (""+status.getNumIncomplete()));
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

        public LockRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 0));
        }
        
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
