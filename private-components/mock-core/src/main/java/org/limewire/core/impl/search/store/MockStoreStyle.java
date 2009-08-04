package org.limewire.core.impl.search.store;

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
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {

    private final Type type;
    
    Font albumFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color albumForeground = Color.decode("#313131");
    Font albumLengthFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color albumLengthForeground = Color.decode("#313131");
    Font artistFont = new Font(Font.DIALOG, Font.PLAIN, 13);
    Color artistForeground = Color.decode("#2152a6");
    Color background = Color.decode("#f5f5f5");
    Icon buyAlbumIcon;
    Icon buyTrackIcon;
    Icon downloadAlbumIcon = new DownloadAlbumIcon();
    Icon downloadTrackIcon = new DownloadTrackIcon();
    Font infoFont = new Font(Font.DIALOG, Font.BOLD, 8);
    Color infoForeground = Color.decode("#2152a6");
    Color priceBackground;
    Color priceBorderColor;
    Font priceFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color priceForeground = Color.decode("#2152a6");
    Font qualityFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color qualityForeground = Color.decode("#313131");
    Font showTracksFont = new Font(Font.DIALOG, Font.BOLD, 8);
    Color showTracksForeground = Color.decode("#2152a6");
    Icon streamIcon = new StreamIcon();
    Font trackFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color trackForeground = Color.decode("#313131");
    Font trackLengthFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color trackLengthForeground = Color.decode("#313131");
    
    boolean buyAlbumVisible;
    boolean buyTrackVisible;
    boolean downloadAlbumVisible;
    boolean downloadTrackVisible;
    boolean infoOnHover;
    boolean priceButtonVisible;
    boolean priceVisible;
    boolean showTracksOnHover;
    
    /**
     * Constructs a StoreStyle with the specified type.
     */
    public MockStoreStyle(Type type) {
        this.type = type;
    }
    
    @Override
    public Font getAlbumFont() {
        return albumFont;
    }

    @Override
    public Color getAlbumForeground() {
        return albumForeground;
    }

    @Override
    public Font getAlbumLengthFont() {
        return albumLengthFont;
    }

    @Override
    public Color getAlbumLengthForeground() {
        return albumLengthForeground;
    }

    @Override
    public Font getArtistFont() {
        return artistFont;
    }

    @Override
    public Color getArtistForeground() {
        return artistForeground;
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
    public Font getQualityFont() {
        return qualityFont;
    }

    @Override
    public Color getQualityForeground() {
        return qualityForeground;
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
    public boolean isBuyAlbumVisible() {
        return buyAlbumVisible;
    }

    @Override
    public boolean isBuyTrackVisible() {
        return buyTrackVisible;
    }

    @Override
    public boolean isDownloadAlbumVisible() {
        return downloadAlbumVisible;
    }

    @Override
    public boolean isDownloadTrackVisible() {
        return downloadTrackVisible;
    }

    @Override
    public boolean isInfoOnHover() {
        return infoOnHover;
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
    public boolean isShowTracksOnHover() {
        return showTracksOnHover;
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
        private final int size = 32;

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
        private final int size = 24;

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
