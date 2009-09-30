package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GraphicsUtilities;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * A table cell renderer for displaying Lime Store results in the Name column
 * of the Classic view table.
 */
abstract class StoreNameCellRenderer extends JXPanel implements TableCellRenderer {

    protected final StoreStyle storeStyle;
    protected final boolean showAudioArtist;
    protected final CategoryIconManager categoryIconManager;
    protected final StoreController storeController;
    
    protected final RendererResources resources;
    protected final Action downloadAction;
    protected final Action streamAction;
    protected final Action showTracksAction;
    
    protected JLabel iconLabel;
    protected JLabel nameLabel;
    
    protected VisualStoreResult vsr;
    
    /**
     * Constructs a StoreNameCellRenderer using the specified services and options.
     */
    public StoreNameCellRenderer(
            StoreStyle storeStyle,
            boolean showAudioArtist,
            CategoryIconManager categoryIconManager,
            StoreController storeController) {
        this.storeStyle = storeStyle;
        this.showAudioArtist = showAudioArtist;
        this.categoryIconManager = categoryIconManager;
        this.storeController = storeController;
        
        this.resources = new RendererResources();
        this.downloadAction = new DownloadAction();
        this.streamAction = new StreamAction();
        this.showTracksAction = new ShowTracksAction();
        
        iconLabel = new JLabel();
        nameLabel = new JLabel();
        nameLabel.setFont(resources.getFont());
        
        initComponents();
    }
    
    /**
     * Initializes the components in the renderer.
     */
    protected abstract void initComponents();

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
            vsr = (VisualStoreResult) value;            
            text = vsr.getNameProperty(showAudioArtist);
            icon = getIcon(vsr);
            if (vsr.isSpam()) {
                foreground = resources.getDisabledForegroundColor();
            } else {
                foreground = table.getForeground();
            }
            
        } else if (value != null) {
            vsr = null;
            throw new IllegalArgumentException(value + " must be a VisualStoreResult, not a " + 
                    value.getClass().getCanonicalName());
            
        } else {
            vsr = null;
            icon = null;
            text = "";
        }
        
        // Apply attributes to renderer.
        setBackground(background);
        setForeground(foreground);
        iconLabel.setIcon(icon);
        nameLabel.setText(text);
        
        // Set small minimum width to allow label to shrink when column is made smaller.
        nameLabel.setMinimumSize(new Dimension(15, nameLabel.getMinimumSize().height));
        
        return this;
    }
    
    @Override
    public String getToolTipText(){
        return nameLabel.getText();
    }
    
    /**
     * Returns the buy icon for the price button.
     */
    protected Icon getBuyIcon() {
        Icon buyIcon = storeStyle.getClassicBuyIcon();
        return (buyIcon != null) ? buyIcon : resources.getBuyIcon();
    }
    
    /**
     * Returns the pause icon for the stream button.
     */
    protected Icon getPauseIcon() {
        Icon pauseIcon = storeStyle.getClassicPauseIcon();
        return (pauseIcon != null) ? pauseIcon : resources.getPauseIcon();
    }
    
    /**
     * Returns the play icon for the stream button.
     */
    protected Icon getPlayIcon() {
        Icon playIcon = storeStyle.getClassicPlayIcon();
        return (playIcon != null) ? playIcon : resources.getPlayIcon();
    }
    
    /**
     * Returns the display icon for the specified store result.
     */
    private Icon getIcon(VisualStoreResult vsr) {
        if (vsr.isSpam()) {
            return resources.getSpamIcon();
        }
        
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return resources.getDownloadIcon();
            
        case DOWNLOADED:
        case LIBRARY:
            return resources.getLibraryIcon();
            
        default:
            if (vsr.getStoreResult().isAlbum()) {
                return resources.getAlbumIcon();
            } else if (vsr.getCategory() == Category.AUDIO) {
                return resources.getAudioIcon();
            }
            return categoryIconManager.getIcon(vsr);
        }
    }
    
    /**
     * Action to download store result.
     */
    private class DownloadAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                storeController.download(vsr);
            }
        }
    }
    
    /**
     * Action to stream store result.
     */
    private class StreamAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                storeController.stream(vsr);
            }
        }
    }
    
    /**
     * Action to show or hide album tracks.
     */
    private class ShowTracksAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO implement
            System.out.println("showTracks");
        }
    }
    
    /**
     * A button that displays the price and is rendered using an optional 
     * background image.
     */
    public class PriceButton extends JXButton {
        
        private final Icon bgIcon;
        private BufferedImage bgImage;
        
        public PriceButton(Icon bgIcon) {
            super();
            
            this.bgIcon = bgIcon;
            
            setBorder(BorderFactory.createEmptyBorder(1, 8, 2, 8));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(storeStyle.getClassicPriceFont());
            setForeground(storeStyle.getClassicPriceForeground());
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            if (bgIcon != null) {
                // Create background image if necessary.
                if (bgImage == null) {
                    bgImage = GraphicsUtilities.createCompatibleTranslucentImage(
                            bgIcon.getIconWidth(), bgIcon.getIconHeight());
                    Graphics gImage = bgImage.createGraphics();
                    bgIcon.paintIcon(this, gImage, 0, 0);
                    gImage.dispose();
                }
                
                // Draw background image scaled to clip region.
                Rectangle clipRect = g.getClipBounds();
                g.drawImage(bgImage, clipRect.x, clipRect.y, clipRect.width, clipRect.height, null);
            }
            
            super.paintComponent(g);
        }
    }

    /**
     * Resource container for store renderer.
     */
    public static class RendererResources {
        @Resource(key="StoreRenderer.albumIcon")
        private Icon albumIcon;
        @Resource(key="StoreRenderer.audioIcon")
        private Icon audioIcon;
        @Resource(key="StoreRenderer.albumCollapsedIcon")
        private Icon albumCollapsedIcon;
        @Resource(key="StoreRenderer.albumExpandedIcon")
        private Icon albumExpandedIcon;
        @Resource(key="StoreRenderer.classicBuyIcon")
        private Icon buyIcon;
        @Resource(key="StoreRenderer.classicPauseIcon")
        private Icon pauseIcon;
        @Resource(key="StoreRenderer.classicPlayIcon")
        private Icon playIcon;
        
        @Resource(key="IconLabelRenderer.disabledForegroundColor")
        private Color disabledForegroundColor;
        @Resource(key="IconLabelRenderer.font")
        private Font font;
        @Resource(key="IconLabelRenderer.downloadingIcon")
        private Icon downloadIcon;
        @Resource(key="IconLabelRenderer.libraryIcon")
        private Icon libraryIcon;
        @Resource(key="IconLabelRenderer.spamIcon")
        private Icon spamIcon;
        
        /**
         * Constructs a RendererResources object. 
         */
        RendererResources() {
            GuiUtils.assignResources(this);
        }
        
        public Icon getAlbumIcon() {
            return albumIcon;
        }
        
        public Icon getAudioIcon() {
            return audioIcon;
        }
        
        public Icon getAlbumCollapsedIcon() {
            return albumCollapsedIcon;
        }
        
        public Icon getAlbumExpandedIcon() {
            return albumExpandedIcon;
        }
        
        public Icon getBuyIcon() {
            return buyIcon;
        }
        
        public Icon getPauseIcon() {
            return pauseIcon;
        }
        
        public Icon getPlayIcon() {
            return playIcon;
        }
        
        public Color getDisabledForegroundColor() {
            return disabledForegroundColor;
        }
        
        public Icon getDownloadIcon() {
            return downloadIcon;
        }
        
        public Font getFont() {
            return font;
        }
        
        public Icon getLibraryIcon() {
            return libraryIcon;
        }
        
        public Icon getSpamIcon() {
            return spamIcon;
        }
    }
}
