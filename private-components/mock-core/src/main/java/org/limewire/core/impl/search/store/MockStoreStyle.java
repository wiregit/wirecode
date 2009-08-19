package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.limewire.core.api.search.store.StoreStyle;

/**
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {

    private final Type type;
    private final Properties properties;
    
    private Font albumFont;
    private Color albumForeground;
    private Font albumLengthFont;
    private Color albumLengthForeground;
    private Font artistFont;
    private Color artistForeground;
    private Color background;
    private Icon buyAlbumIcon;
    private Icon buyTrackIcon;
    private Icon downloadAlbumIcon;
    private Icon downloadTrackIcon;
    private Font infoFont;
    private Color infoForeground;
    private Color priceBackground;
    private Color priceBorderColor;
    private Font priceFont;
    private Color priceForeground;
    private Font qualityFont;
    private Color qualityForeground;
    private Font showTracksFont;
    private Color showTracksForeground;
    private Icon streamIcon;
    private Font trackFont;
    private Color trackForeground;
    private Font trackLengthFont;
    private Color trackLengthForeground;
    
    private boolean downloadButtonVisible;
    private boolean priceButtonVisible;
    private boolean priceVisible;
    private boolean showInfoOnHover;
    private boolean showTracksOnHover;
    private boolean streamButtonVisible;
    
    /**
     * Constructs a StoreStyle with the specified type.
     */
    public MockStoreStyle(Type type) {
        this.type = type;
        this.properties = new Properties();
        loadProperties();
    }
    
    /**
     * Loads style attributes from properties file.
     */
    private void loadProperties() {
        try {
            URL url = getClass().getResource("MockStoreStyle.properties");
            properties.load(url.openStream());
            
            albumFont = getFont("albumFont");
            albumForeground = getColor("albumForeground");
            albumLengthFont = getFont("albumLengthFont");
            albumLengthForeground = getColor("albumLengthForeground");
            artistFont = getFont("artistFont");
            artistForeground = getColor("artistForeground");
            background = getColor("background");
            buyAlbumIcon = getIcon("buyAlbumIcon");
            buyTrackIcon = getIcon("buyTrackIcon");
            downloadAlbumIcon = getIcon("downloadAlbumIcon");
            downloadTrackIcon = getIcon("downloadTrackIcon");
            infoFont = getFont("infoFont");
            infoForeground = getColor("infoForeground");
            priceBackground = getColor("priceBackground");
            priceBorderColor = getColor("priceBorderColor");
            priceFont = getFont("priceFont");
            priceForeground = getColor("priceForeground");
            qualityFont = getFont("qualityFont");
            qualityForeground = getColor("qualityForeground");
            showTracksFont = getFont("showTracksFont");
            showTracksForeground = getColor("showTracksForeground");
            streamIcon = getIcon("streamIcon");
            trackFont = getFont("trackFont");
            trackForeground = getColor("trackForeground");
            trackLengthFont = getFont("trackLengthFont");
            trackLengthForeground = getColor("trackLengthForeground");
            
            downloadButtonVisible = getBoolean("downloadButtonVisible");
            priceButtonVisible = getBoolean("priceButtonVisible");
            priceVisible = getBoolean("priceVisible");
            showInfoOnHover = getBoolean("showInfoOnHover");
            showTracksOnHover = getBoolean("showTracksOnHover");
            streamButtonVisible = getBoolean("streamButtonVisible");
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
     * Returns the color for the specified property key.
     */
    private boolean getBoolean(String propertyKey) {
        return Boolean.parseBoolean(properties.getProperty(type + "." + propertyKey));
    }
    
    /**
     * Returns the color for the specified property key.
     */
    private Color getColor(String propertyKey) {
        return Color.decode(properties.getProperty(type + "." + propertyKey));
    }
    
    /**
     * Returns the font for the specified property key.
     */
    private Font getFont(String propertyKey) {
        return Font.decode(properties.getProperty(type + "." + propertyKey));
    }
    
    /**
     * Retrieves the icon for the specified property key.
     */
    private Icon getIcon(String propertyKey) {
        return new ImageIcon(getClass().getResource(properties.getProperty(type + "." + propertyKey)));
    }
}
