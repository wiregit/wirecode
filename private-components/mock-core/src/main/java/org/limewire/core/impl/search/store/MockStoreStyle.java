package org.limewire.core.impl.search.store;

import java.awt.Color;
import java.awt.Font;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import javax.swing.Icon;

import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreSearchListener;
import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.util.StringUtils;

/**
 * Implementation of StoreStyle for the mock core.
 */
public class MockStoreStyle implements StoreStyle {

    private final StoreSearchListener storeSearchListener;
    private final MockStoreConnection storeConnection;

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
    private final String streamPauseIconUri;
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
    private Icon streamPauseIcon;
    
    private boolean iconsRequested;
    
    /**
     * Constructs a StoreStyle using the specified JSON object.
     */
    public MockStoreStyle(Properties properties, 
            StoreSearchListener storeSearchListener,
            MockStoreConnection connection) {
        this.storeSearchListener = storeSearchListener;
        this.storeConnection = connection;

        type = Type.valueOf(properties.getProperty("type"));
        timestamp = System.currentTimeMillis();
        
        background = getColor(properties, "background");
        buyAlbumIconUri = properties.getProperty("buyAlbumIcon");
        buyTrackIconUri = properties.getProperty("buyTrackIcon");
        classicBuyIconUri = properties.getProperty("classicBuyIcon");
        classicPauseIconUri = properties.getProperty("classicPauseIcon");
        classicPlayIconUri = properties.getProperty("classicPlayIcon");
        classicPriceFont = getFont(properties, "classicPriceFont");
        classicPriceForeground = getColor(properties, "classicPriceForeground");
        downloadAlbumIconUri = properties.getProperty("downloadAlbumIcon");
        downloadTrackIconUri = properties.getProperty("downloadTrackIcon");
        headingFont = getFont(properties, "headingFont");
        headingForeground = getColor(properties, "headingForeground");
        infoFont = getFont(properties, "infoFont");
        infoForeground = getColor(properties, "infoForeground");
        priceBackground = getColor(properties, "priceBackground");
        priceBorderColor = getColor(properties, "priceBorderColor");
        priceFont = getFont(properties, "priceFont");
        priceForeground = getColor(properties, "priceForeground");
        showTracksFont = getFont(properties, "showTracksFont");
        showTracksForeground = getColor(properties, "showTracksForeground");
        streamIconUri = properties.getProperty("streamIcon");
        streamPauseIconUri = properties.getProperty("streamIcon");
        subHeadingFont = getFont(properties, "subHeadingFont");
        subHeadingForeground = getColor(properties, "subHeadingForeground");
        trackFont = getFont(properties, "trackFont");
        trackForeground = getColor(properties, "trackForeground");
        trackLengthFont = getFont(properties, "trackLengthFont");
        trackLengthForeground = getColor(properties, "trackLengthForeground");
        
        downloadButtonVisible = getBoolean(properties, "downloadButtonVisible");
        priceButtonVisible = getBoolean(properties, "priceButtonVisible");
        priceVisible = getBoolean(properties, "priceVisible");
        showInfoOnHover = getBoolean(properties, "showInfoOnHover");
        showTracksOnHover = getBoolean(properties, "showTracksOnHover");
        streamButtonVisible = getBoolean(properties, "streamButtonVisible");
        
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
                    if (!StringUtils.isEmpty(streamPauseIconUri)) {
                        streamPauseIcon = storeConnection.loadIcon(streamPauseIconUri);
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
    private boolean getBoolean(Properties properties, String propertyKey) {
        return Boolean.valueOf(properties.getProperty(propertyKey));
    }
    
    /**
     * Returns the color for the specified property key.
     */
    private Color getColor(Properties properties, String propertyKey) {
        String value = properties.getProperty(propertyKey);
        return (value != null) ? Color.decode(value) : null;
    }
    
    /**
     * Returns the font for the specified property key.
     */
    private Font getFont(Properties properties, String propertyKey) {
        String value = properties.getProperty(propertyKey);
        return (value != null) ? Font.decode(value) : null;
    }
    
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
