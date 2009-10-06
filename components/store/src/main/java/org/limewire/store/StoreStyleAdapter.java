package org.limewire.store;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreStyle;

/**
 * Implementation of StoreStyle for the live core.
 */
public class StoreStyleAdapter implements StoreStyle {

    private final Type type;
    
    private final Color background;
    private final Icon buyAlbumIcon;
    private final Icon buyTrackIcon;
    private final Icon classicBuyIcon;
    private final Icon classicPauseIcon;
    private final Icon classicPlayIcon;
    private final Font classicPriceFont;
    private final Color classicPriceForeground;
    private final Icon downloadAlbumIcon;
    private final Icon downloadTrackIcon;
    private final Font headingFont;
    private final Color headingForeground;
    private final Font infoFont;
    private final Color infoForeground;
    private final Color priceBackground;
    private final Color priceBorderColor;
    private final Font priceFont;
    private final Color priceForeground;
    private final Font showTracksFont;
    private final Color showTracksForeground;
    private final Icon streamIcon;
    private final Font subHeadingFont;
    private final Color subHeadingForeground;
    private final Font trackFont;
    private final Color trackForeground;
    private final Font trackLengthFont;
    private final Color trackLengthForeground;
    
    private final boolean downloadButtonVisible;
    private final boolean priceButtonVisible;
    private final boolean priceVisible;
    private final boolean showInfoOnHover;
    private final boolean showTracksOnHover;
    private final boolean streamButtonVisible;
    
    /**
     * Constructs a StoreStyleAdapter using the specified JSON object.
     */
    public StoreStyleAdapter(JSONObject jsonObj) throws IOException, JSONException {
        type = getType(jsonObj);
        
        background = getColor(jsonObj, "background");
        buyAlbumIcon = getIcon(jsonObj, "buyAlbumIcon");
        buyTrackIcon = getIcon(jsonObj, "buyTrackIcon");
        classicBuyIcon = getIcon(jsonObj, "classicBuyIcon");
        classicPauseIcon = getIcon(jsonObj, "classicPauseIcon");
        classicPlayIcon = getIcon(jsonObj, "classicPlayIcon");
        classicPriceFont = getFont(jsonObj, "classicPriceFont");
        classicPriceForeground = getColor(jsonObj, "classicPriceForeground");
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
    public Icon getClassicBuyIcon() {
        return classicBuyIcon;
    }

    @Override
    public Icon getClassicPauseIcon() {
        return classicPauseIcon;
    }

    @Override
    public Icon getClassicPlayIcon() {
        return classicPlayIcon;
    }
    
    @Override
    public Font getClassicPriceFont() {
        return classicPriceFont;
    }

    @Override
    public Color getClassicPriceForeground() {
        return classicPriceForeground;
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
    private boolean getBoolean(JSONObject jsonObj, String propertyKey) {
        return jsonObj.optBoolean(propertyKey);
    }
    
    /**
     * Returns the color for the specified property key.
     */
    private Color getColor(JSONObject jsonObj, String propertyKey) {
        String value = jsonObj.optString(propertyKey);
        return (value != null) ? Color.decode(value) : null;
    }
    
    /**
     * Returns the font for the specified property key.
     */
    private Font getFont(JSONObject jsonObj, String propertyKey) {
        String value = jsonObj.optString(propertyKey);
        return (value != null) ? Font.decode(value) : null;
    }
    
    /**
     * Retrieves the icon for the specified property key.
     */
    private Icon getIcon(JSONObject jsonObj, String propertyKey) throws MalformedURLException {
        String value = jsonObj.optString(propertyKey);
        return (value != null) ? new ImageIcon(new URL(value)) : null;
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
