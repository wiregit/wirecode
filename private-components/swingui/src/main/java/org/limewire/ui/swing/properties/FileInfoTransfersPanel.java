package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentPeer;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.io.Address;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTransfersPanel implements FileInfoPanel {

    @Resource private Color foreground;
    @Resource private Font smallFont;
    @Resource private Font headerFont;
    
    private final JPanel component;
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private DownloadStatusListener downloadStatus;
    
    private final MouseableTable infoTable;
    
    private final Timer refreshTimer;
    
    public FileInfoTransfersPanel(FileInfoType type, PropertiableFile propertiableFile) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        component.add(createHeaderLabel(I18n.tr("Downloading from")),"push");
        
        final JLabel percentLabel = createPlainLabel("");            
        component.add(percentLabel, "alignx right, wrap");
                
        downloadStatus = new DownloadStatusListener(percentLabel);
        ((DownloadItem)propertiableFile).addPropertyChangeListener(downloadStatus);
        
        infoTable = new MouseableTable();
        component.add(new JScrollPane(infoTable), "span, grow, wrap");
        
        init();
        refreshTimer = new Timer(1000, new ActionListener() {
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
            ReadOnlyTableModel model = new ReadOnlyTableModel();
            infoTable.setModel(model);

            DownloadItem download = ((DownloadItem)propertiableFile);
            if (download.getDownloadItemType() == DownloadItemType.GNUTELLA) {
                model.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
                for(Address source : download.getSources()) {
                    model.addRow(new Object[] {source.getAddressDescription(), 
                            download.getDownloadingFile().getName() });
                }
            }
            else if (download.getDownloadItemType() ==  DownloadItemType.BITTORRENT) {
                    
           //     Torrent torrent = (Torrent) download.getDownloadProperty(DownloadPropertyKey.TORRENT);
                    
                model.setColumnIdentifiers(new Object[]{tr("Address"),
                        tr("Encyption"), tr("Client"),
                        tr("Upload"), tr("Download")});
                  
                // TODO: Reimplement general solution that will work with gnutella.
                /*for( TorrentPeer source : torrent.getTorrentPeers() ) {
                    model.addRow(new Object[] {source.getIPAddress(),
                            source.isEncyrpted(),
                            source.getClientName(),
                            source.getUploadSpeed(),
                            source.getDownloadSpeed()});
                }*/
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
    
    private static class ReadOnlyTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
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
}
