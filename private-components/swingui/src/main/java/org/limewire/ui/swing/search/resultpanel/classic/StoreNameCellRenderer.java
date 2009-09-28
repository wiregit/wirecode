package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

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
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.util.CategoryIconManager;
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
    protected final Action streamAction;
    
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
        this.streamAction = new StreamAction();
        
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
     * Returns the display icon for the specified search result.
     */
    private Icon getIcon(VisualSearchResult vsr) {
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
            return categoryIconManager.getIcon(vsr);
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
     * A button that displays the price and downloads the file.
     */
    public class PriceButton extends JXButton {
        
        public PriceButton() {
            super();
            
            setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            setFocusPainted(false);
            setFont(storeStyle.getPriceFont());
            setForeground(storeStyle.getPriceForeground());
            
            if (storeStyle.isPriceButtonVisible()) {
                setContentAreaFilled(true);
                setBackgroundPainter(new RectanglePainter<JXButton>(0, 0, 0, 0, 15, 15, true,
                        storeStyle.getPriceBackground(), 1.0f, storeStyle.getPriceBorderColor()));
            } else {
                setContentAreaFilled(false);
            }
        }
    }

    /**
     * Resource container for renderer.
     */
    public static class RendererResources {
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
