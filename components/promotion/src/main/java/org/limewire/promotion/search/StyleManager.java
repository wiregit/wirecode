package org.limewire.promotion.search;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.limewire.core.api.search.store.StoreStyle;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class StyleManager {
    
    private static final Log LOG = LogFactory.getLog(StyleManager.class);
    
    private final Map<StoreStyle.Type, StoreStyle> styles = Collections.synchronizedMap(new HashMap<StoreStyle.Type, StoreStyle>());
    
    @Inject
    StyleManager() {
        try {
            Properties serializedDefaultStyles = new Properties();
            serializedDefaultStyles.load(getClass().getClassLoader().getResourceAsStream("org/limewire/promotion/search/styles.properties"));
            Map<StoreStyle.Type, Map<String, String>> defaultStyles = new HashMap<StoreStyle.Type, Map<String, String>>();
            for(String property : serializedDefaultStyles.stringPropertyNames()) {
                int dot = property.indexOf('.');
                if(dot > 0) {
                    String styleTypeName = property.substring(0, dot);
                    try {
                        StoreStyle.Type styleType = StoreStyle.Type.valueOf(String.valueOf(styleTypeName));
                        Map<String, String> style = defaultStyles.get(styleType);
                        if(style == null) {
                            style = new HashMap<String, String>();
                            defaultStyles.put(styleType, style);                             
                        }
                        style.put(property.substring(dot + 1), serializedDefaultStyles.getProperty(property));
                    } catch (IllegalArgumentException e) {
                        LOG.debugf(e, "illegal style name {0}", styleTypeName);
                    }
                }
            }
            for(StoreStyle.Type type : defaultStyles.keySet()) {
                try {
                    StoreStyle style = new StoreStyleAdapter(type, defaultStyles.get(type));
                    styles.put(type, style);
                } catch (IOException e) {
                    LOG.debugf(e, "failed to load default style {0}", type);    
                }
            }
        } catch (IOException e) {
            LOG.debugf("failed to load default styles", e);
        }
    }
    
    void updateStyle(StoreStyle storeStyle) {
        styles.put(storeStyle.getType(), storeStyle);        
    }
    
    StoreStyle getStyle(StoreStyle.Type storeStyleType) {
        return styles.get(storeStyleType);    
    }
    
    StoreStyle getDefaultStyle() {
        return styles.get(StoreStyle.Type.STYLE_A);
    }
}
