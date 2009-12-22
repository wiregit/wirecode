package org.limewire.core.api.search.store;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;

/**
 * Defines a style for Lime Store results.
 * 
 * Icons are loaded eagerly in a background thread before instances
 * are made available to the UI (i.e. EDT will not block)
 */
public interface StoreStyle {
    public enum Type {
        STYLE_A, STYLE_B, STYLE_C, STYLE_D
    }
    
    Color getBackground();
    
    Icon getBuyAlbumIcon();
    
    Icon getBuyTrackIcon();
    
    Icon getClassicBuyIcon();
    
    Icon getClassicPauseIcon();
    
    Icon getClassicPlayIcon();
    
    Font getClassicPriceFont();
    
    Color getClassicPriceForeground();
    
    Icon getDownloadAlbumIcon();
    
    Icon getDownloadTrackIcon();
    
    Font getHeadingFont();
  
    Color getHeadingForeground();
    
    Font getInfoFont();
    
    Color getInfoForeground();
    
    Font getPriceFont();
    
    Color getPriceForeground();

    Color getPriceBackground();
    
    Color getPriceBorderColor();
    
    Font getShowTracksFont();

    Color getShowTracksForeground();
    
    Icon getStreamIcon();
    
    Icon getStreamPauseIcon();
    
    Font getSubHeadingFont();
  
    Color getSubHeadingForeground();

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

    boolean isStreamButtonVisible();
}
