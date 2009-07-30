package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;

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
    Color background;
    Icon buyAlbumIcon;
    Icon buyTrackIcon;
    Icon downloadAlbumIcon;
    Icon downloadTrackIcon;
    Icon infoIcon;
    Color priceBackground;
    Color priceBorderColor;
    Font priceFont;
    Color priceForeground;
    Font qualityFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color qualityForeground = Color.decode("#313131");
    Icon showTracksIcon;
    Icon streamIcon;
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
    public Icon getInfoIcon() {
        return infoIcon;
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
    public Icon getShowTracksIcon() {
        return showTracksIcon;
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
}
