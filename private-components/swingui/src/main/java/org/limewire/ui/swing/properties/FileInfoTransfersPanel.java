package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
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
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentStatus;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.transfer.SourceInfo;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadPropertyKey;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
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
    private FileInfoTableModel infoModel;
    
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
        
        infoModel = new FileInfoTableModel();
        infoTable = new JXTable(infoModel);
        
        tableDecorator.decorate(infoTable);
        
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
                refresh();
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
        TableColumn column = infoTable.getColumn(FileInfoTableModel.ENCRYPTED);
        column.setCellRenderer(new LockRenderer());
        column.setMaxWidth(12);
        column.setMinWidth(12);
        column.setWidth(12);

        infoTable.getColumnExt(FileInfoTableModel.IP).setComparator(IpPort.IP_COMPARATOR);
        TableColumn ipColumn = infoTable.getColumn(FileInfoTableModel.IP);
        ipColumn.setCellRenderer(new IPRenderer());
        
        TableColumn uploadColumn = infoTable.getColumn(FileInfoTableModel.UPLOAD_SPEED);
        uploadColumn.setCellRenderer(new SpeedRenderer());
        
        TableColumn downloadColumn = infoTable.getColumn(FileInfoTableModel.DOWNLOAD_SPEED);
        downloadColumn.setCellRenderer(new SpeedRenderer());
    }
    
    private void refresh() {
        infoModel.clear();
                
        if (download != null) {
            infoModel.addAll(download.getSourcesDetails());
        } else {
            infoModel.addAll(upload.getTransferDetails());
        }
            
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
    
    private static class FileInfoTableModel extends AbstractTableModel {

        public static final int IP = 0;
        public static final int ENCRYPTED = 1;
        public static final int CLIENT_NAME = 2;
        public static final int UPLOAD_SPEED = 3;
        public static final int DOWNLOAD_SPEED = 4;
        
        private List<SourceInfo> sources = new ArrayList<SourceInfo>();
        
        private String[] columnNames = new String[]{tr("Address"),
                "", tr("Client"), tr("Upload"), tr("Download")};
        
        public void clear(){
            sources.clear();
        }
        
        public void addAll(Collection<SourceInfo> info){
            sources.addAll(info);
            fireTableDataChanged();
        }
       
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            if(column < columnNames.length){
                return columnNames[column];
            }
            return null;
        }


        @Override
        public int getRowCount() {
            return sources.size();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= getRowCount()){
            return null;
            }
            return getColumnValue(sources.get(rowIndex), columnIndex);
        }    
        
        private Object getColumnValue(SourceInfo info, int column) {
            switch (column){
            case IP:
                try {
                    return new IpPortImpl(info.getIPAddress(), 0);
                } catch (UnknownHostException e) {
                    // This only thrown when no IP address can be found for a host or and with global 
                    //IPv6 addresses that have a scope_id.  
                    return null;
                }
            case ENCRYPTED:
                return info.isEncyrpted();
            case CLIENT_NAME:
                return info.getClientName();
            case UPLOAD_SPEED:
                return Long.valueOf(Math.round(info.getUploadSpeed()));
            case DOWNLOAD_SPEED:
                return Long.valueOf(Math.round(info.getDownloadSpeed()));
            }
            return null;
        }  
        
    }

    private static class SpeedRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null){
                value = GuiUtils.formatUnitFromBytesPerSec((Long)value);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        
    }
    private static class IPRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null){
                value = ((IpPort)value).getAddress();
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
        
    }
    
}
