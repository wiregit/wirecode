package org.limewire.promotion.search;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Implementation of StoreStyle for the live core.
 */
public class StoreStyleAdapter implements StoreStyle {
    
    private static final Log LOG = LogFactory.getLog(StoreStyleAdapter.class);

    private final Type type;
    private final long timestamp;
    
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
    private final Icon streamPauseIcon;
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
        timestamp = getTimestamp(jsonObj);
        
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
        streamPauseIcon = getIcon(jsonObj, "streamPauseIcon");
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
    
    public StoreStyleAdapter(StoreStyle.Type type, Map<String, String> data)  throws IOException {
        this.type = type;
        
        timestamp = getTimestamp(data.get("timestamp"));
        background = getColor(data.get("background"));
        buyAlbumIcon = getIcon(data.get("buyAlbumIcon"));
        buyTrackIcon = getIcon(data.get("buyTrackIcon"));
        classicBuyIcon = getIcon(data.get("classicBuyIcon"));
        classicPauseIcon = getIcon(data.get("classicPauseIcon"));
        classicPlayIcon = getIcon(data.get("classicPlayIcon"));
        classicPriceFont = getFont(data.get("classicPriceFont"));
        classicPriceForeground = getColor(data.get("classicPriceForeground"));
        downloadAlbumIcon = getIcon(data.get("downloadAlbumIcon"));
        downloadTrackIcon = getIcon(data.get("downloadTrackIcon"));
        headingFont = getFont(data.get("headingFont"));
        headingForeground = getColor(data.get("headingForeground"));
        infoFont = getFont(data.get("infoFont"));
        infoForeground = getColor(data.get("infoForeground"));
        priceBackground = getColor(data.get("priceBackground"));
        priceBorderColor = getColor(data.get("priceBorderColor"));
        priceFont = getFont(data.get("priceFont"));
        priceForeground = getColor(data.get("priceForeground"));
        showTracksFont = getFont(data.get("showTracksFont"));
        showTracksForeground = getColor(data.get("showTracksForeground"));
        streamIcon = getIcon(data.get("streamIcon"));
        streamPauseIcon = getIcon(data.get("streamPauseIcon"));
        subHeadingFont = getFont(data.get("subHeadingFont"));
        subHeadingForeground = getColor(data.get("subHeadingForeground"));
        trackFont = getFont(data.get( "trackFont"));
        trackForeground = getColor(data.get("trackForeground"));
        trackLengthFont = getFont(data.get("trackLengthFont"));
        trackLengthForeground = getColor(data.get("trackLengthForeground"));
        
        downloadButtonVisible = getBoolean(data.get("downloadButtonVisible"));
        priceButtonVisible = getBoolean(data.get("priceButtonVisible"));
        priceVisible = getBoolean(data.get("priceVisible"));
        showInfoOnHover = getBoolean(data.get("showInfoOnHover"));
        showTracksOnHover = getBoolean(data.get("showTracksOnHover"));
        streamButtonVisible = getBoolean(data.get("streamButtonVisible"));    
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
    public Icon getStreamPauseIcon() {
        return streamPauseIcon;
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
    public long getTimestamp() {
        return timestamp;
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
     * Returns the boolean value for the specified value.
     */
    private boolean getBoolean(String value) {
        return Boolean.valueOf(value);
    }
    
    /**
     * Returns the color for the specified property key.
     */
    private Color getColor(JSONObject jsonObj, String propertyKey) {
        String value = jsonObj.optString(propertyKey);
        return getColor(value);
    }
    
    /**
     * Returns the color for the specified value.
     */
    private Color getColor(String color) {
        return (color != null) ? Color.decode(color) : null;
    }
    
    /**
     * Returns the font for the specified property key.
     */
    private Font getFont(JSONObject jsonObj, String propertyKey) {
        String value = jsonObj.optString(propertyKey);
        return getFont(value);
    }
    
    /**
     * Returns the font for the specified value.
     */
    private Font getFont(String font) {
        return (font != null) ? Font.decode(font) : null;
    }
    
    /**
     * Retrieves the icon for the specified property key.
     */
    private Icon getIcon(JSONObject jsonObj, String propertyKey) throws MalformedURLException {
        String value = jsonObj.optString(propertyKey);
        return getIcon(value);
    }
    
    /**
     * Retrieves the icon for the specified property key.
     */
    private Icon getIcon(String icon) throws MalformedURLException {
        return (icon != null) ? new ImageIcon(getClass().getResource(icon)) : null;
    }
    
    /**
     * Retrieves the timestamp from the specified JSON object.
     */
    private long getTimestamp(JSONObject jsonObj) throws JSONException {
        return getTimestamp(jsonObj.optString("timestamp"));
    }
    
    /**
     * Retrieves the timestamp from the specified timestamp.
     */
    private long getTimestamp(String timestamp) {
        if(timestamp == null) {
            return 0;
        }
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(timestamp).getTime();            
        } catch (ParseException ex) {
            LOG.debugf(ex, "couldn't parse timestamp {0}", timestamp);
            return 0;
        }
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