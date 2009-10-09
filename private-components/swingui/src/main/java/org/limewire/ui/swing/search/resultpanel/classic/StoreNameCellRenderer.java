package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.components.RolloverCursorListener;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GraphicsUtilities;

/**
 * A table cell renderer for displaying Lime Store results in the Name column
 * of the Classic view table.
 */
abstract class StoreNameCellRenderer implements TableCellRenderer {

    protected final boolean showAudioArtist;
    private final CategoryIconManager categoryIconManager;
    protected final StoreRendererResourceManager storeResourceManager;
    private final MouseListener popupListener;
    private final StoreController storeController;
    
    protected final Action downloadAction;
    protected final Action streamAction;
    protected final ShowTracksAction showTracksAction;
    
    protected final JXPanel renderer;
    protected final JLabel iconLabel;
    protected final JLabel nameLabel;
    
    protected StoreStyle storeStyle;
    protected VisualStoreResult vsr;
    
    private JTable table;
    private int row;
    private int col;
    
    /**
     * Constructs a StoreNameCellRenderer using the specified services and 
     * options.
     */
    public StoreNameCellRenderer(
            StoreStyle storeStyle,
            boolean showAudioArtist,
            CategoryIconManager categoryIconManager,
            StoreRendererResourceManager storeResourceManager,
            MousePopupListener popupListener,
            StoreController storeController) {
        this.storeStyle = storeStyle;
        this.showAudioArtist = showAudioArtist;
        this.categoryIconManager = categoryIconManager;
        this.storeResourceManager = storeResourceManager;
        this.popupListener = popupListener;
        this.storeController = storeController;
        
        downloadAction = new DownloadAction();
        streamAction = new StreamAction();
        showTracksAction = new ShowTracksAction();
        
        renderer = new JXPanel();
        iconLabel = new JLabel();
        nameLabel = new JLabel();
        nameLabel.setFont(storeResourceManager.getFont());
        
        installPopupListener(renderer);
        installPopupListener(iconLabel);
        installPopupListener(nameLabel);
        
        initComponents();
    }
    
    /**
     * Initializes the components in the renderer.
     */
    protected abstract void initComponents();
    
    /**
     * Installs the popup and selection listener on the specified component.
     */
    protected void installPopupListener(Component component) {
        component.addMouseListener(popupListener);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, 
            boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.row = row;
        this.col = column;
        
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
                foreground = storeResourceManager.getDisabledForegroundColor();
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
        renderer.setBackground(background);
        renderer.setForeground(foreground);
        iconLabel.setIcon(icon);
        nameLabel.setText(text);
        nameLabel.setToolTipText(text);
        
        // Set small minimum width to allow label to shrink in MigLayout when
        // column is made smaller.
        nameLabel.setMinimumSize(new Dimension(15, nameLabel.getMinimumSize().height));
        
        return renderer;
    }
    
    /**
     * Returns the buy icon for the price button.
     */
    protected Icon getBuyIcon() {
        Icon buyIcon = storeStyle.getClassicBuyIcon();
        return (buyIcon != null) ? buyIcon : storeResourceManager.getBuyIcon();
    }
    
    /**
     * Returns the pause icon for the stream button.
     */
    protected Icon getPauseIcon() {
        Icon pauseIcon = storeStyle.getClassicPauseIcon();
        return (pauseIcon != null) ? pauseIcon : storeResourceManager.getPauseIcon();
    }
    
    /**
     * Returns the play icon for the stream button.
     */
    protected Icon getPlayIcon() {
        Icon playIcon = storeStyle.getClassicPlayIcon();
        return (playIcon != null) ? playIcon : storeResourceManager.getPlayIcon();
    }
    
    /**
     * Returns the display icon for the specified store result.
     */
    private Icon getIcon(VisualStoreResult vsr) {
        if (vsr.isSpam()) {
            return storeResourceManager.getSpamIcon();
        }
        
        switch (vsr.getDownloadState()) {
        case DOWNLOADING:
            return storeResourceManager.getDownloadIcon();
            
        case DOWNLOADED:
        case LIBRARY:
            return storeResourceManager.getLibraryIcon();
            
        default:
            if (vsr.getStoreResult().isAlbum()) {
                return storeResourceManager.getAlbumIcon();
            } else if (vsr.getCategory() == Category.AUDIO) {
                return storeResourceManager.getAudioIcon();
            }
            return categoryIconManager.getIcon(vsr);
        }
    }
    
    /**
     * Returns true if the current style matches the specified style.
     */
    public boolean isCurrentStyle(StoreStyle storeStyle) {
        return (this.storeStyle.getType() == storeStyle.getType());
    }
    
    /**
     * Updates the renderer using the specified StoreStyle.  The type of the
     * specified style should match the type of the current style.
     */
    public void updateStyle(StoreStyle storeStyle) {
        if (isCurrentStyle(storeStyle)) {
            this.storeStyle = storeStyle;
            applyStyle();
        }
    }
    
    /**
     * Applies the current style to the renderer.  This method is called when
     * the style is updated with new icons while in use.
     */
    protected abstract void applyStyle();
    
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
    public class ShowTracksAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (vsr != null) {
                // Toggle indicator.
                vsr.setShowTracks(!vsr.isShowTracks());
                
                // Post event to update cell editor.
                if (table != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (row >= 0) {
                                table.editCellAt(row, col);
                            }
                        }
                    });
                }
            }
        }
    }
    
    /**
     * A button that displays the price and is rendered using an optional 
     * background image.
     */
    public class PriceButton extends JXButton {
        
        private Icon bgIcon;
        private BufferedImage bgImage;
        private RolloverCursorListener rolloverListener;
        
        public PriceButton() {
            super();
            
            setBorder(BorderFactory.createEmptyBorder(1, 8, 2, 8));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setFont(storeStyle.getClassicPriceFont());
            setForeground(storeStyle.getClassicPriceForeground());
        }
        
        public void setBackgroundIcon(Icon bgIcon) {
            this.bgIcon = bgIcon;
        }
        
        @Override
        public void setAction(Action action) {
            super.setAction(action);
            
            // Display hand cursor only if action is defined.
            if ((action != null) && (rolloverListener == null)) {
                rolloverListener = new RolloverCursorListener();
                rolloverListener.install(this);
            } else if ((action == null) && (rolloverListener != null)) {
                rolloverListener.uninstall(this);
                rolloverListener = null;
            }
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
}
