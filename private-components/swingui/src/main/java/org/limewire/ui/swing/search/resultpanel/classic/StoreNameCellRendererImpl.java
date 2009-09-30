package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;

/**
 * Implementation of StoreNameCellRenderer for all styles.
 */
class StoreNameCellRendererImpl extends StoreNameCellRenderer {

    private JButton tracksButton;
    private JButton streamButton;
    private JButton priceButton;
    private JPanel namePanel;
    
    /**
     * Constructs a store renderer with the specified store style and services.
     */
    public StoreNameCellRendererImpl(
            StoreStyle storeStyle,
            boolean showAudioArtist,
            CategoryIconManager categoryIconManager,
            StoreController storeController) {
        super(storeStyle, showAudioArtist, categoryIconManager, storeController);
    }

    @Override
    protected void initComponents() {
        tracksButton = new IconButton();
        tracksButton.setAction(showTracksAction);
        if (storeStyle.getType() != Type.STYLE_D) {
            tracksButton.setFont(resources.getFont());
        } else {
            tracksButton.setIcon(resources.getAlbumCollapsedIcon());
        }
        
        streamButton = new IconButton();
        streamButton.setAction(streamAction);
        streamButton.setIcon(getPlayIcon());
        
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            priceButton = new PriceButton(getBuyIcon());
            priceButton.setAction(downloadAction);
            break;
        case STYLE_C: case STYLE_D:
        default:
            priceButton = new PriceButton(null);
            break;
        }
        
        // Create container for name and tracks labels.
        namePanel = new JPanel();
        namePanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        namePanel.setOpaque(false);
        namePanel.setLayout(new BorderLayout());
        if (storeStyle.getType() != Type.STYLE_D) {
            namePanel.add(nameLabel, BorderLayout.CENTER);
            namePanel.add(tracksButton, BorderLayout.EAST);
        } else {
            namePanel.add(nameLabel, BorderLayout.CENTER);
        }
        
        // Layout components in renderer.
        renderer.setLayout(new MigLayout("insets 1 2 0 6, gap 0! 0!, novisualpadding"));
        if (storeStyle.getType() == Type.STYLE_D) {
            renderer.add(tracksButton, "aligny 50%, gap 3 3");
        }
        renderer.add(iconLabel, "aligny 50%");
        if (storeStyle.getType() != Type.STYLE_B) {
            renderer.add(streamButton, "aligny 50%, gapleft 6");
        }
        renderer.add(namePanel, "aligny 50%, pushx 200");
        if (storeStyle.getType() == Type.STYLE_B) {
            renderer.add(streamButton, "aligny 50%, gapleft 6");
        }
        renderer.add(priceButton, "alignx right, aligny 50%, gapleft 6");
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
            
            if (vsr.getStoreResult().isAlbum()) {
                if (storeStyle.getType() != Type.STYLE_D) {
                    int trackCount = vsr.getStoreResult().getAlbumResults().size();
                    String trackText = "<html>(<a href=''>show " + trackCount + " tracks</a>)</html>";
                    tracksButton.setText(trackText);
                } else {
                    tracksButton.setText(null);
                }
                tracksButton.setVisible(true);
                
            } else {
                tracksButton.setVisible(false);
            }
        }
        
        return renderer;
    }    
}
