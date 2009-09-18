package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualStoreResult;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A table cell renderer that delegates to the appropriate component to display
 * the Name column in the Classic view table for search results.
 */
public class NameRendererDelegate implements TableCellRenderer {

    private final TableCellRenderer defaultRenderer;
    private final boolean showAudioArtist;
    private final StoreNameCellRendererFactory storeNameRendererFactory;
    
    private TableCellRenderer storeRenderer;
    
    /**
     * Constructs a NameRendererDelegate with the specified default renderer
     * and store renderer factory.
     */
    @Inject
    public NameRendererDelegate(
            @Assisted TableCellRenderer defaultRenderer, 
            @Assisted boolean showAudioArtist,
            StoreNameCellRendererFactory storeNameRendererFactory) {
        this.defaultRenderer = defaultRenderer;
        this.showAudioArtist = showAudioArtist;
        this.storeNameRendererFactory = storeNameRendererFactory;
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        
        if ((value instanceof VisualStoreResult) && (storeRenderer != null)) {
            return storeRenderer.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
        } else {
            return defaultRenderer.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
        }
    }
    
    /**
     * Applies the specified store style to the delegate.
     */
    public void setStoreStyle(StoreStyle storeStyle) {
        storeRenderer = storeNameRendererFactory.create(storeStyle, showAudioArtist);
    }
}
