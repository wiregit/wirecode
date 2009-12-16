package org.limewire.ui.swing.properties;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTable;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentTracker;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadPropertyKey;
import org.limewire.core.api.download.UploadPropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.decorators.TableDecorator;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.table.DefaultLimeTableCellRenderer;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTrackersPanel implements FileInfoPanel {

    private static final int URL_COLUMN = 0;
    private static final int TIER_COLUMN = 1;
    
    private final Torrent torrent;
    private final JPanel component;
    
    private List<TorrentTracker> trackerList = null;
    
    
    public FileInfoTrackersPanel(FileInfoType type, PropertiableFile propertiableFile, TableDecorator tableDecorator) {
        component = new JPanel(new MigLayout("fillx, gap 0"));
        
        if (propertiableFile instanceof DownloadItem) {
            torrent = (Torrent)((DownloadItem)propertiableFile).getDownloadProperty(DownloadPropertyKey.TORRENT);
        }
        else if (propertiableFile instanceof UploadItem) {
            torrent = (Torrent)((UploadItem)propertiableFile).getUploadProperty(UploadPropertyKey.TORRENT);
        } 
        else {
            torrent = null;
            return;
        }
        
        trackerList = torrent.getTrackers();
        
        AbstractTableModel model = new AbstractTableModel() {
            
            @Override
            public String getColumnName(int columnIndex) {
                if (columnIndex == URL_COLUMN) { 
                    return I18n.tr("URL");
                }
                else if (columnIndex == TIER_COLUMN) {
                    return I18n.tr("Tier");
                }
                
                throw new IllegalArgumentException("Invalid Column Used");
            }
            
            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == URL_COLUMN) { 
                    return trackerList.get(rowIndex).getURL();
                }
                else if (columnIndex == TIER_COLUMN) {
                    return trackerList.get(rowIndex).getTier();
                }
                
                throw new IllegalArgumentException("Invalid Column Used");
            }
            @Override
            public int getRowCount() {
                return trackerList.size();
            }
            @Override
            public int getColumnCount() {
                return 2;
            }
        };
        
        JXTable table = new JXTable(model);
        
        tableDecorator.decorate(table);
        
        table.setCellSelectionEnabled(false);
        table.setShowGrid(false, false);
        table.setEditable(false);
        
        TableColumn tierColumn = table.getColumn(TIER_COLUMN);
        tierColumn.setMaxWidth(45);
        tierColumn.setMinWidth(25);
        tierColumn.setWidth(40);
        tierColumn.setPreferredWidth(40);
        
        DefaultLimeTableCellRenderer tierRenderer = new DefaultLimeTableCellRenderer();
        tierRenderer.setHorizontalAlignment(JLabel.CENTER);
        tierColumn.setCellRenderer(tierRenderer);
        
        component.add(new JScrollPane(table), "gaptop 10, span, grow, wrap");
    }
    
    @Override
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public void updatePropertiableFile(PropertiableFile file) {
    }

    @Override
    public void dispose() {
    }

}
