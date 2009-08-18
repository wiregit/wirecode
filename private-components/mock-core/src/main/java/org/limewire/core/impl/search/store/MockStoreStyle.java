package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.limewire.core.api.search.store.StoreStyle;

/**
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {
    private static final String DOWNLOAD_BIG_ICON = "download-btn-1big.png";
    private static final String DOWNLOAD_SMALL_ICON = "download-btn-1small.png";
    private static final String STREAM_ICON = "stream-btn.png";

    private static final String BUY_ALBUM_A_ICON = "style1-buy-album-btn.png";
    private static final String BUY_TRACK_A_ICON = "style1-buy-song-btn.png";
    
    private static final String BUY_ALBUM_B_ICON = "style2-buy-album-btn.png";
    private static final String BUY_TRACK_B_ICON = "style2-buy-btn.png";
    
    private static final String BUY_C_ICON = "style3-buy-icon.png";
    private static final String DOWNLOAD_C_ICON = "style3-download-icon.png";

    private final Type type;
    
    Font albumFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color albumForeground = Color.decode("#313131");
    Font albumLengthFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color albumLengthForeground = Color.decode("#313131");
    Font artistFont = new Font(Font.DIALOG, Font.PLAIN, 13);
    Color artistForeground = Color.decode("#2152a6");
    Color background = Color.decode("#e8f2f6");
    Icon buyAlbumIcon;
    Icon buyTrackIcon;
    Icon downloadAlbumIcon;
    Icon downloadTrackIcon;
    Font infoFont = new Font(Font.DIALOG, Font.BOLD, 8);
    Color infoForeground = Color.decode("#2152a6");
    Color priceBackground = Color.decode("#f5f5f5");
    Color priceBorderColor = Color.decode("#9e9b9b");
    Font priceFont = new Font(Font.DIALOG, Font.PLAIN, 10);
    Color priceForeground = Color.decode("#2152a6");
    Font qualityFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color qualityForeground = Color.decode("#313131");
    Font showTracksFont = new Font(Font.DIALOG, Font.BOLD, 8);
    Color showTracksForeground = Color.decode("#2152a6");
    Icon streamIcon;
    Font trackFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color trackForeground = Color.decode("#313131");
    Font trackLengthFont = new Font(Font.DIALOG, Font.PLAIN, 11);
    Color trackLengthForeground = Color.decode("#313131");
    
    boolean downloadButtonVisible = true;
    boolean priceButtonVisible = true;
    boolean priceVisible = true;
    boolean showInfoOnHover = false;
    boolean showTracksOnHover = false;
    boolean streamButtonVisible = true;
    
    /**
     * Constructs a StoreStyle with the specified type.
     */
    public MockStoreStyle(Type type) {
        this.type = type;
        
        streamIcon = getIcon(STREAM_ICON);
        
        switch (type) {
        case STYLE_A:
            buyAlbumIcon = getIcon(BUY_ALBUM_A_ICON);
            buyTrackIcon = getIcon(BUY_TRACK_A_ICON);
            downloadAlbumIcon = getIcon(DOWNLOAD_BIG_ICON);
            downloadTrackIcon = getIcon(DOWNLOAD_SMALL_ICON);
            break;
            
        case STYLE_B:
            buyAlbumIcon = getIcon(BUY_ALBUM_B_ICON);
            buyTrackIcon = getIcon(BUY_TRACK_B_ICON);
            downloadAlbumIcon = getIcon(DOWNLOAD_SMALL_ICON);
            downloadTrackIcon = getIcon(DOWNLOAD_SMALL_ICON);
            break;
            
        case STYLE_C:
        case STYLE_D:
            buyAlbumIcon = getIcon(BUY_C_ICON);
            buyTrackIcon = getIcon(BUY_C_ICON);
            downloadAlbumIcon = getIcon(DOWNLOAD_C_ICON);
            downloadTrackIcon = getIcon(DOWNLOAD_C_ICON);
            break;
        }
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
    
    /**
     * Retrieves the icon using the specified image file name.
     */
    private Icon getIcon(String name) {
        return new ImageIcon(getClass().getResource(name));
    }
}
