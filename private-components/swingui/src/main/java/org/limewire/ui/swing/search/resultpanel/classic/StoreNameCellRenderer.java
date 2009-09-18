package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.application.Resource;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A table cell renderer for displaying Lime Store results in the Name column
 * of the Classic view table.
 */
class StoreNameCellRenderer extends DefaultTableCellRenderer {

    private final StoreStyle storeStyle;
    private final boolean showAudioArtist;
    private final CategoryIconManager categoryIconManager;
    
    @Resource(key="IconLabelRenderer.disabledForegroundColor")
    private Color disabledForegroundColor;
    @Resource(key="IconLabelRenderer.font")
    private Font font;
    @Resource(key="IconLabelRenderer.downloadingIcon")
    private Icon downloadingIcon;
    @Resource(key="IconLabelRenderer.libraryIcon")
    private Icon libraryIcon;
    @Resource(key="IconLabelRenderer.spamIcon")
    private Icon spamIcon;
    
    /**
     * Constructs a StoreNameCellRenderer using the specified services and options.
     */
    public StoreNameCellRenderer(
            StoreStyle storeStyle,
            CategoryIconManager categoryIconManager,
            boolean showAudioArtist) {
        this.storeStyle = storeStyle;
        this.showAudioArtist = showAudioArtist;
        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this);
        
        setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
        setIconTextGap(5);
        setFont(font);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        Color background;
        Color foreground;
        Icon icon;
        String text;
        
        // Set colors based on row selection.
        if (table.getSelectedRow() == row) {
            background = table.getSelectionBackground();
            foreground = table.getSelectionForeground();
        } else {
            background = table.getBackground();
            foreground = table.getForeground();
        }
        
        if (value instanceof VisualStoreResult) {
            // Set attributes for store result.
            VisualStoreResult vsr = (VisualStoreResult) value;            
            text = vsr.getNameProperty(showAudioArtist);
            icon = getIcon(vsr);
            if (vsr.isSpam()) {
                foreground = disabledForegroundColor;
            } else {
                foreground = table.getForeground();
            }
            
        } else if (value != null) {
            throw new IllegalArgumentException(value + " must be a VisualStoreResult, not a " + 
                    value.getClass().getCanonicalName());
            
        } else {
            icon = null;
            text = "";
        }
        
        // Apply attributes to renderer.
        setBackground(background);
        setForeground(foreground);
        setIcon(icon);
        setText(text);
        
        return this;
    }
    
    @Override
    public String getToolTipText(){
        return getText();
    }
    
    /**
     * Returns the display icon for the specified search result.
     */
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
            
        default:
            return categoryIconManager.getIcon(vsr);
        }
    }
}
