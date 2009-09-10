package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreStyle;

/**
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {

    private final Type type;
    
    private Color background;
    private Icon buyAlbumIcon;
    private Icon buyTrackIcon;
    private Icon downloadAlbumIcon;
    private Icon downloadTrackIcon;
    private Font headingFont;
    private Color headingForeground;
    private Font infoFont;
    private Color infoForeground;
    private Color priceBackground;
    private Color priceBorderColor;
    private Font priceFont;
    private Color priceForeground;
    private Font showTracksFont;
    private Color showTracksForeground;
    private Icon streamIcon;
    private Font subHeadingFont;
    private Color subHeadingForeground;
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
     * Constructs a StoreStyle using the specified JSON object.
     */
    public MockStoreStyle(JSONObject jsonObj) throws JSONException {
        type = getType(jsonObj);
        
        background = getColor(jsonObj, "background");
        buyAlbumIcon = getIcon(jsonObj, "buyAlbumIcon");
        buyTrackIcon = getIcon(jsonObj, "buyTrackIcon");
        downloadAlbumIcon = getIcon(jsonObj, "downloadAlbumIcon");
        downloadTrackIcon = getIcon(jsonObj, "downloadTrackIcon");
        headingFont = getFont(jsonObj, "headingFont");
        headingForeground = getColor(jsonObj, "headingForeground");
        infoFont = getFont(jsonObj, "infoFont");
        infoForeground = getColor(jsonObj, "infoForeground");
        priceBackground = getColor(jsonObj, "priceBackground");
        priceBorderColor = getColor(jsonObj, "priceBorderColor");
        priceFont = getFont(jsonObj, "priceFont");
        priceForeground = getColor(jsonObj, "priceForeground");
        showTracksFont = getFont(jsonObj, "showTracksFont");
        showTracksForeground = getColor(jsonObj, "showTracksForeground");
        streamIcon = getIcon(jsonObj, "streamIcon");
        subHeadingFont = getFont(jsonObj, "subHeadingFont");
        subHeadingForeground = getColor(jsonObj, "subHeadingForeground");
        trackFont = getFont(jsonObj, "trackFont");
        trackForeground = getColor(jsonObj, "trackForeground");
        trackLengthFont = getFont(jsonObj, "trackLengthFont");
        trackLengthForeground = getColor(jsonObj, "trackLengthForeground");
        
        downloadButtonVisible = getBoolean(jsonObj, "downloadButtonVisible");
        priceButtonVisible = getBoolean(jsonObj, "priceButtonVisible");
        priceVisible = getBoolean(jsonObj, "priceVisible");
        showInfoOnHover = getBoolean(jsonObj, "showInfoOnHover");
        showTracksOnHover = getBoolean(jsonObj, "showTracksOnHover");
        streamButtonVisible = getBoolean(jsonObj, "streamButtonVisible");
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
    
    /**
     * Returns the boolean value for the specified property key.
     */
    private boolean getBoolean(JSONObject jsonObj, String propertyKey) throws JSONException {
        return jsonObj.getBoolean(propertyKey);
    }
    
    /**
     * Returns the color for the specified property key.
     */
    private Color getColor(JSONObject jsonObj, String propertyKey) throws JSONException {
        return Color.decode(jsonObj.getString(propertyKey));
    }
    
    /**
     * Returns the font for the specified property key.
     */
    private Font getFont(JSONObject jsonObj, String propertyKey) throws JSONException {
        return Font.decode(jsonObj.getString(propertyKey));
    }
    
    /**
     * Retrieves the icon for the specified property key.
     */
    private Icon getIcon(JSONObject jsonObj, String propertyKey) throws JSONException {
        return new ImageIcon(getClass().getResource(jsonObj.getString(propertyKey)));
    }
    
    /**
     * Retrieves the type from the specified JSON object.
     */
    private Type getType(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.getString("type");
        for (Type type : Type.values()) {
            if (type.toString().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new JSONException("Invalid style type");
    }
}
