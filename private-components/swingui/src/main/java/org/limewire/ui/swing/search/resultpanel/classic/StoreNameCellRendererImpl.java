package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.core.api.search.store.StoreStyle.Type;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;

/**
 * Implementation of StoreNameCellRenderer for all styles.
 */
class StoreNameCellRendererImpl extends StoreNameCellRenderer {

    private JButton tracksButton;
    private JButton streamButton;
    private PriceButton priceButton;
    private JPanel namePanel;
    
    /**
     * Constructs a store renderer with the specified store style and services.
     */
    public StoreNameCellRendererImpl(
            StoreStyle storeStyle,
            boolean showAudioArtist,
            CategoryIconManager categoryIconManager,
            StoreRendererResourceManager storeResourceManager,
            MousePopupListener popupListener,
            StoreController storeController) {
        super(storeStyle, showAudioArtist, categoryIconManager, storeResourceManager,
                popupListener, storeController);
    }

    @Override
    protected void initComponents() {
        tracksButton = new IconButton(showTracksAction);
        tracksButton.setFont(storeResourceManager.getFont());
        tracksButton.setMinimumSize(new Dimension(
                storeResourceManager.getAlbumExpandedIcon().getIconWidth(),
                storeResourceManager.getAlbumExpandedIcon().getIconHeight()));
        
        streamButton = new IconButton(streamAction);
        
        priceButton = new PriceButton();
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            priceButton.setAction(downloadAction);
            break;
        default:
            break;
        }
        
        applyStyle();
        
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
                    // Update tracks button text.
                    long trackCount = vsr.getStoreResult().getTrackCount();
                    String trackText = vsr.isShowTracks() ?
                            I18n.trn("hide {0} track", "hide {0} tracks", trackCount, trackCount) :
                            I18n.trn("show {0} track", "show {0} tracks", trackCount, trackCount);
                    String trackHtml = "<html> (<a href=''>" + trackText + "</a>)</html>";
                    tracksButton.setText(trackHtml);
                    tracksButton.setIcon(null);
                    
                } else {
                    // Update tracks button icon.
                    tracksButton.setIcon(vsr.isShowTracks() ? 
                            storeResourceManager.getAlbumExpandedIcon() : 
                            storeResourceManager.getAlbumCollapsedIcon());
                    tracksButton.setText(null);
                }
                
                tracksButton.setVisible(true);
                
            } else {
                tracksButton.setVisible(false);
            }
        }
        
        return renderer;
    }
    
    @Override
    protected void applyStyle() {
        streamButton.setIcon(getPlayIcon());
        
        switch (storeStyle.getType()) {
        case STYLE_A: case STYLE_B:
            priceButton.setBackgroundIcon(getBuyIcon());
            break;
        default:
            break;
        }
    }
}
