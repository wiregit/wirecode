package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.swing.Icon;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreConnectionFactory;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.util.StringUtils;

/**
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {

    private final StoreSearchListener storeSearchListener;
    private final StoreConnectionFactory storeConnectionFactory;
    
    private final Type type;
    private final long timestamp;
    
    private final Color background;
    private final String buyAlbumIconUri;
    private final String buyTrackIconUri;
    private final String classicBuyIconUri;
    private final String classicPauseIconUri;
    private final String classicPlayIconUri;
    private final Font classicPriceFont;
    private final Color classicPriceForeground;
    private final String downloadAlbumIconUri;
    private final String downloadTrackIconUri;
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
    private final String streamIconUri;
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
    
    private Icon buyAlbumIcon;
    private Icon buyTrackIcon;
    private Icon classicBuyIcon;
    private Icon classicPauseIcon;
    private Icon classicPlayIcon;
    private Icon downloadAlbumIcon;
    private Icon downloadTrackIcon;
    private Icon streamIcon;
    
    private boolean iconsRequested;
    
    /**
     * Constructs a StoreStyle using the specified JSON object.
     */
    public MockStoreStyle(JSONObject jsonObj, 
            StoreSearchListener storeSearchListener,
            StoreConnectionFactory storeConnectionFactory) throws JSONException {
        this.storeSearchListener = storeSearchListener;
        this.storeConnectionFactory = storeConnectionFactory;
        
        type = getType(jsonObj);
        timestamp = getTimestamp(jsonObj);
        
        background = getColor(jsonObj, "background");
        buyAlbumIconUri = jsonObj.optString("buyAlbumIcon");
        buyTrackIconUri = jsonObj.optString("buyTrackIcon");
        classicBuyIconUri = jsonObj.optString("classicBuyIcon");
        classicPauseIconUri = jsonObj.optString("classicPauseIcon");
        classicPlayIconUri = jsonObj.optString("classicPlayIcon");
        classicPriceFont = getFont(jsonObj, "classicPriceFont");
        classicPriceForeground = getColor(jsonObj, "classicPriceForeground");
        downloadAlbumIconUri = jsonObj.optString("downloadAlbumIcon");
        downloadTrackIconUri = jsonObj.optString("downloadTrackIcon");
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
        streamIconUri = jsonObj.optString("streamIcon");
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
        
        loadIcons();
    }
    
    /**
     * Loads style icons.  The store search listener is notified when icons 
     * are available.
     */
    private void loadIcons() {
        if (!iconsRequested) {
            iconsRequested = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                    
                    // Create store connection and load icons.
                    StoreConnection storeConnection = storeConnectionFactory.create();
                    
                    if (!StringUtils.isEmpty(buyAlbumIconUri)) {
                        buyAlbumIcon = storeConnection.loadIcon(buyAlbumIconUri);
                    }
                    if (!StringUtils.isEmpty(buyTrackIconUri)) {
                        buyTrackIcon = storeConnection.loadIcon(buyTrackIconUri);
                    }
                    if (!StringUtils.isEmpty(classicBuyIconUri)) {
                        classicBuyIcon = storeConnection.loadIcon(classicBuyIconUri);
                    }
                    if (!StringUtils.isEmpty(classicPauseIconUri)) {
                        classicPauseIcon = storeConnection.loadIcon(classicPauseIconUri);
                    }
                    if (!StringUtils.isEmpty(classicPlayIconUri)) {
                        classicPlayIcon = storeConnection.loadIcon(classicPlayIconUri);
                    }
                    if (!StringUtils.isEmpty(downloadAlbumIconUri)) {
                        downloadAlbumIcon = storeConnection.loadIcon(downloadAlbumIconUri);
                    }
                    if (!StringUtils.isEmpty(downloadTrackIconUri)) {
                        downloadTrackIcon = storeConnection.loadIcon(downloadTrackIconUri);
                    }
                    if (!StringUtils.isEmpty(streamIconUri)) {
                        streamIcon = storeConnection.loadIcon(streamIconUri);
                    }
                    
                    // Fire search event to update style.
                    storeSearchListener.styleUpdated(MockStoreStyle.this);
                }
            }).start();
        }
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
    
//    /**
//     * Retrieves the icon for the specified property key.
//     */
//    private Icon getIcon(JSONObject jsonObj, String propertyKey) {
//        String value = jsonObj.optString(propertyKey);
//        return (value != null) ? new ImageIcon(getClass().getResource(value)) : null;
//    }
    
    /**
     * Retrieves the timestamp from the specified JSON object.
     */
    private long getTimestamp(JSONObject jsonObj) throws JSONException {
        String value = jsonObj.getString("timestamp");
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(value).getTime();
            
        } catch (ParseException ex) {
            ex.printStackTrace();
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
