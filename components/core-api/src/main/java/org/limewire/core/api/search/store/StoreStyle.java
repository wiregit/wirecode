package org.limewire.core.api.search.store;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;

/**
 * Defines a style for Lime Store results.
 */
public interface StoreStyle {
    public enum Type {
        A, B, C, D
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
    
    Icon getInfoIcon();
    
    Font getPriceFont();
    
    Color getPriceForeground();

    Color getPriceBackground();
    
    Color getPriceBorderColor();
    
    Font getQualityFont();
    
    Color getQualityForeground();

    Icon getShowTracksIcon();
    
    Icon getStreamIcon();
    
    Font getTrackFont();
    
    Color getTrackForeground();
    
    Font getTrackLengthFont();
    
    Color getTrackLengthForeground();

    Type getType();

    boolean isBuyAlbumVisible();
    
    boolean isBuyTrackVisible();

    boolean isDownloadAlbumVisible();
    
    boolean isDownloadTrackVisible();
    
    boolean isInfoOnHover();
    
    boolean isPriceVisible();

    boolean isPriceButtonVisible();
    
    boolean isShowTracksOnHover();
}
