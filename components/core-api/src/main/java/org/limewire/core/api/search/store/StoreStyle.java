package org.limewire.core.api.search.store;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;

/**
 * Defines a style for Lime Store results.
 */
public interface StoreStyle {
    public enum Type {
        STYLE_A, STYLE_B, STYLE_C, STYLE_D
    }
    
    Font getAlbumFont();
    
    Color getAlbumForeground();
    
    Font getAlbumLengthFont();
    
    Color getAlbumLengthForeground();
    
    Font getArtistFont();
    
    Color getArtistForeground();
    
    Color getBackground();
    
    Icon getBuyAlbumIcon();
    
    Icon getBuyTrackIcon();
    
    Icon getDownloadAlbumIcon();
    
    Icon getDownloadTrackIcon();
    
    Font getInfoFont();
    
    Color getInfoForeground();
    
    Font getPriceFont();
    
    Color getPriceForeground();

    Color getPriceBackground();
    
    Color getPriceBorderColor();
    
    Font getQualityFont();
    
    Color getQualityForeground();

    Font getShowTracksFont();

    Color getShowTracksForeground();
    
    Icon getStreamIcon();
    
    Font getTrackFont();
    
    Color getTrackForeground();
    
    Font getTrackLengthFont();
    
    Color getTrackLengthForeground();

    Type getType();
    
    boolean isDownloadButtonVisible();
    
    boolean isPriceVisible();

    boolean isPriceButtonVisible();
    
    boolean isShowInfoOnHover();
    
    boolean isShowTracksOnHover();
}
