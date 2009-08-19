package org.limewire.ui.swing.search.resultpanel.list;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.Icon;

import org.limewire.core.api.search.store.StoreStyle;

/**
 * Default implementation of StoreStyle for the List view.  This style is used
 * when the current store style is not available.
 */
public class DefaultListStoreStyle implements StoreStyle {

    private final Type type;
    
    private Color background = Color.decode("#e8f2f6");
    private Icon buyAlbumIcon;
    private Icon buyTrackIcon;
    private Icon downloadAlbumIcon;
    private Icon downloadTrackIcon;
    private Font headingFont = new Font(Font.DIALOG, Font.PLAIN, 13);
    private Color headingForeground = Color.decode("#2152a6");
    private Font infoFont = new Font(Font.DIALOG, Font.BOLD, 8);
    private Color infoForeground = Color.decode("#2152a6");
    private Color priceBackground = Color.decode("#f5f5f5");
    private Color priceBorderColor = Color.decode("#9e9b9b");
    private Font priceFont = new Font(Font.DIALOG, Font.PLAIN, 10);
    private Color priceForeground = Color.decode("#2152a6");
    private Font showTracksFont = new Font(Font.DIALOG, Font.BOLD, 8);
    private Color showTracksForeground = Color.decode("#2152a6");
    private Icon streamIcon = new StreamIcon();
    private Font subHeadingFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    private Color subHeadingForeground = Color.decode("#313131");
    private Font trackFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    private Color trackForeground = Color.decode("#313131");
    private Font trackLengthFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    private Color trackLengthForeground = Color.decode("#313131");
    
    private boolean downloadButtonVisible = true;
    private boolean priceButtonVisible = false;
    private boolean priceVisible = false;
    private boolean showInfoOnHover = false;
    private boolean showTracksOnHover = false;
    private boolean streamButtonVisible = true;
    
    /**
     * Constructs a DefaultListStoreStyle.
     */
    public DefaultListStoreStyle() {
        type = Type.STYLE_A;
        
        if ((type == Type.STYLE_A) || (type == Type.STYLE_B)) {
            downloadAlbumIcon = new DownloadAlbumIcon(32);
            downloadTrackIcon = new DownloadTrackIcon(24);
        } else {
            downloadAlbumIcon = new DownloadAlbumIcon(20);
            downloadTrackIcon = new DownloadTrackIcon(20);
        }
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public Icon getBuyAlbumIcon() {
        return buyAlbumIcon;
    }

    @Override
    public Icon getBuyTrackIcon() {
        return buyTrackIcon;
    }

    @Override
    public Icon getDownloadAlbumIcon() {
        return downloadAlbumIcon;
    }

    @Override
    public Icon getDownloadTrackIcon() {
        return downloadTrackIcon;
    }

    @Override
    public Font getHeadingFont() {
        return headingFont;
    }

    @Override
    public Color getHeadingForeground() {
        return headingForeground;
    }

    @Override
    public Font getInfoFont() {
        return infoFont;
    }

    @Override
    public Color getInfoForeground() {
        return infoForeground;
    }

    @Override
    public Color getPriceBackground() {
        return priceBackground;
    }

    @Override
    public Color getPriceBorderColor() {
        return priceBorderColor;
    }

    @Override
    public Font getPriceFont() {
        return priceFont;
    }

    @Override
    public Color getPriceForeground() {
        return priceForeground;
    }

    @Override
    public Font getShowTracksFont() {
        return showTracksFont;
    }

    @Override
    public Color getShowTracksForeground() {
        return showTracksForeground;
    }

    @Override
    public Icon getStreamIcon() {
        return streamIcon;
    }

    @Override
    public Font getSubHeadingFont() {
        return subHeadingFont;
    }

    @Override
    public Color getSubHeadingForeground() {
        return subHeadingForeground;
    }

    @Override
    public Font getTrackFont() {
        return trackFont;
    }

    @Override
    public Color getTrackForeground() {
        return trackForeground;
    }

    @Override
    public Font getTrackLengthFont() {
        return trackLengthFont;
    }

    @Override
    public Color getTrackLengthForeground() {
        return trackLengthForeground;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isDownloadButtonVisible() {
        return downloadButtonVisible;
    }
    
    @Override
    public boolean isPriceButtonVisible() {
        return priceButtonVisible;
    }

    @Override
    public boolean isPriceVisible() {
        return priceVisible;
    }

    @Override
    public boolean isShowInfoOnHover() {
        return showInfoOnHover;
    }

    @Override
    public boolean isShowTracksOnHover() {
        return showTracksOnHover;
    }
    
    @Override
    public boolean isStreamButtonVisible() {
        return streamButtonVisible;
    }
    
    public static class StreamIcon implements Icon {
        private final int size = 20;

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Create graphics.
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Set graphics to use anti-aliasing for smoothness.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set line color and thickness.
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(1.0f));

            // Create shape.
            Shape circle = new Ellipse2D.Double(0, 0, size - 1, size - 1);
            
            // Draw shape centered in icon.
            g2d.draw(circle);

            // Dispose graphics.
            g2d.dispose();
        }
    }
    
    public static class DownloadAlbumIcon implements Icon {
        private final int size;

        public DownloadAlbumIcon(int size) {
            this.size = size;
        }
        
        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Create graphics.
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Set graphics to use anti-aliasing for smoothness.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set line color and thickness.
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(1.0f));

            // Create shape.
            Shape circle = new Ellipse2D.Double(0, 0, size - 1, size - 1);
            
            // Draw shape centered in icon.
            g2d.draw(circle);

            // Dispose graphics.
            g2d.dispose();
        }
    }
    
    public static class DownloadTrackIcon implements Icon {
        private final int size;

        public DownloadTrackIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            // Create graphics.
            Graphics2D g2d = (Graphics2D) g.create();
            
            // Set graphics to use anti-aliasing for smoothness.
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Set line color and thickness.
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(1.0f));

            // Create shape.
            Shape circle = new Ellipse2D.Double(0, 0, size - 1, size - 1);
            
            // Draw shape centered in icon.
            g2d.draw(circle);

            // Dispose graphics.
            g2d.dispose();
        }
    }
}
