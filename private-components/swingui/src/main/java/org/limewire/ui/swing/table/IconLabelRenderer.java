package org.limewire.ui.swing.table;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

/**
 * Renders a table cell with a string and the system icon representing that
 * file type.
 */
public class IconLabelRenderer extends JXPanel implements TableCellRenderer {

    private final IconManager iconManager;
    private final CategoryIconManager categoryIconManager;
    private final JLabel label;
    @Resource private Icon spamIcon;
    @Resource private Icon downloadingIcon;
    @Resource private Icon libraryIcon;
    
    public IconLabelRenderer(IconManager iconManager, CategoryIconManager categoryIconManager) {
        super(new BorderLayout());
        this.iconManager = iconManager;
        this.categoryIconManager = categoryIconManager;
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        label = new JLabel();        
        label.setIconTextGap(5);
        
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

            if (item instanceof RemoteFileItem) {
                label.setIcon(iconManager.getIconForExtension(FileUtils.getFileExtension(item.getFileName())));
            } else {
                label.setIcon(iconManager.getIconForFile(((LocalFileItem) item).getFile()));
            }
            label.setText(item.getFileName());
            
        } else if (value instanceof VisualSearchResult){
            
            VisualSearchResult vsr = (VisualSearchResult)value;
            
            String name = vsr.getPropertyString(FilePropertyKey.NAME);
            String title = vsr.getPropertyString(FilePropertyKey.TITLE);
            if(vsr.getCategory().equals(Category.AUDIO) && !StringUtils.isEmpty(title)) {
                name = title;
            }
            label.setText(name);
            label.setIcon(getIcon(vsr));

            setAlpha(vsr.isSpam() ? 0.2f : 1.0f);
        } else if (value != null) {
            throw new IllegalArgumentException(value + " must be a FileItem or VisualSearchResult");
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
        return categoryIconManager.getIcon(vsr, iconManager);
    }
}
