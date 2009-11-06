package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A table cell editor/renderer used to display the Name column in the Classic
 * view table for search results.  NameRendererDelegate delegates rendering to
 * the appropriate component based on the value type.
 */
public class NameRendererDelegate extends AbstractCellEditor 
    implements TableCellRenderer, TableCellEditor {

    private final TableCellRenderer defaultRenderer;
    private final MousePopupListener storePopupListener;
    private final StoreController storeController;
    private final boolean showAudioArtist;
    private final StoreNameCellRendererFactory storeNameRendererFactory;
    
    private StoreNameCellRenderer storeRenderer;
    
    /**
     * Constructs a NameRendererDelegate with the specified default renderer
     * and store renderer factory.
     */
    @Inject
    public NameRendererDelegate(
            @Assisted TableCellRenderer defaultRenderer,
            @Assisted MousePopupListener storePopupListener,
            @Assisted StoreController storeController,
            @Assisted boolean showAudioArtist,
            StoreNameCellRendererFactory storeNameRendererFactory) {
        this.defaultRenderer = defaultRenderer;
        this.storePopupListener = storePopupListener;
        this.storeController = storeController;
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

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, 
            boolean isSelected, int row, int column) {
        return getTableCellRendererComponent(table, value, isSelected, true, row, column);
    }

    @Override
    public Object getCellEditorValue() {
        return null;
    }
    
    /**
     * Applies the specified store style to the delegate.
     */
    public void setStoreStyle(StoreStyle storeStyle) {
        // Update store renderer.  If style is current, then update renderer;
        // otherwise, create new renderer for style.
        if ((storeRenderer != null) && storeRenderer.isCurrentStyle(storeStyle)) {
            storeRenderer.updateStyle(storeStyle);
        } else {
            storeRenderer = storeNameRendererFactory.create(storeStyle, 
                    storePopupListener, storeController, showAudioArtist);
        }
    }
}
