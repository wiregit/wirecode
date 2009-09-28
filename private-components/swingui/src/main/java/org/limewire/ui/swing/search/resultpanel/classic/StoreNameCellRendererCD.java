package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;

/**
 * Implementation of StoreNameCellRenderer for styles C and D.
 */
class StoreNameCellRendererCD extends StoreNameCellRenderer {

    private JButton streamButton;
    private JButton priceButton;
    
    /**
     * Constructs a store renderer with the specified store style and services.
     */
    public StoreNameCellRendererCD(
            StoreStyle storeStyle,
            boolean showAudioArtist,
            CategoryIconManager categoryIconManager,
            StoreController storeController) {
        super(storeStyle, showAudioArtist, categoryIconManager, storeController);
    }

    @Override
    protected void initComponents() {
        streamButton = new IconButton();
        streamButton.setAction(streamAction);
        streamButton.setIcon(storeStyle.getStreamIcon());
        
        priceButton = new PriceButton();
        
        setLayout(new MigLayout("insets 1 2 0 6, gap 0! 0!, novisualpadding, hidemode 3"));
        add(iconLabel, "aligny 50%");
        add(streamButton, "aligny 50%, gapleft 6");
        add(nameLabel, "aligny 50%, gapleft 8, pushx 200");
        add(priceButton, "alignx right, aligny 50%, gaptop 1, gapleft 6");
    }
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        Component renderer = super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);
        
        if (value instanceof VisualStoreResult) {
            // Set attributes for store result.
            VisualStoreResult vsr = (VisualStoreResult) value;
            priceButton.setText(vsr.getStoreResult().getPrice());
        }
        
        return renderer;
    }    
}
