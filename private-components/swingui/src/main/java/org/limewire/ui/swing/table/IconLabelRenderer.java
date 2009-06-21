package org.limewire.ui.swing.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * Renders a table cell with a string and the system icon representing that
 * file type.
 */
public class IconLabelRenderer extends JXPanel implements TableCellRenderer {

    private final Provider<IconManager> iconManager;
    private final CategoryIconManager categoryIconManager;
    private final boolean showAudioArtist;
    
    private final JLabel label;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    @Resource private Color disabledForegroundColor;
    @Resource private Font font;
    
    @Inject
    public IconLabelRenderer(Provider<IconManager> iconManager, 
            CategoryIconManager categoryIconManager,
            @Assisted boolean showAudioArtist) {
        super(new BorderLayout());
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        this.showAudioArtist = showAudioArtist;
        
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        label = new JLabel();        
        label.setIconTextGap(5);
        label.setFont(font);
        
        add(label);
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        
        if (table.getSelectedRow() == row) {
            this.setBackground(table.getSelectionBackground());
            this.setForeground(table.getSelectionForeground());
        } else {
            this.setBackground(table.getBackground());
            this.setForeground(table.getForeground());
        }
        
        if (value instanceof FileItem) {
            
            FileItem item = (FileItem) value;            
            if(item instanceof LocalFileItem) {
                label.setIcon(iconManager.get().getIconForFile(((LocalFileItem) item).getFile()));
                LocalFileItem localFileItem = (LocalFileItem) item;
                if(localFileItem.isIncomplete()) {
                    label.setText(I18n.tr("{0} (downloading)", item.getFileName()));
                } else {
                    label.setText(item.getFileName());
                }
            } else {
                label.setText(item.getFileName());
            }
            
        } else if (value instanceof VisualSearchResult){
            
            VisualSearchResult vsr = (VisualSearchResult)value;
            
            String name = vsr.getNameProperty(showAudioArtist);
            label.setText(name);
            label.setIcon(getIcon(vsr));

            if(vsr.isSpam()) {
                label.setForeground(disabledForegroundColor);
            }
            else {
                label.setForeground(table.getForeground());
            }
        } else if (value != null) {
            throw new IllegalArgumentException(value + " must be a FileItem or VisualSearchResult, not a " + value.getClass().getCanonicalName());
        }
        
        return this;
    }
    
    @Override
    public String getToolTipText(){
        return label.getText();
    }
    
    private Icon getIcon(VisualSearchResult vsr) {
        if (vsr.isSpam()) {
            return spamIcon;
        } 
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return downloadingIcon;
        case DOWNLOADED:
        case LIBRARY:
            return libraryIcon;
        }
        return categoryIconManager.getIcon(vsr);
    }
}
