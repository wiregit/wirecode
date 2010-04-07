package org.limewire.ui.swing.search;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.FilePropertyKeyUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * A search query with field-specific data that will be used for an advanced
 * search.
 */
class SmartQuery {

    private final Map<FilePropertyKey, String> queryData;
    private final List<FilePropertyKey> queryKeys;
    private final SearchCategory searchCategory;
    
    /**
     * Constructs a SmartQuery for the specified search category.
     */
    public SmartQuery(SearchCategory searchCategory) {
        this.searchCategory = searchCategory;
        this.queryData = new EnumMap<FilePropertyKey, String>(FilePropertyKey.class);
        this.queryKeys = new ArrayList<FilePropertyKey>();
    }
    
    /**
     * Returns a new instance of SmartQuery with the specified field data.
     */
    public static SmartQuery newInstance(SearchCategory searchCategory, 
            FilePropertyKey key, String data) {
        SmartQuery query = new SmartQuery(searchCategory);
        query.addData(key, data);
        return query;
    }
    
    /**
     * Returns a new instance of SmartQuery with the specified field data.
     */
    public static SmartQuery newInstance(SearchCategory searchCategory, 
            FilePropertyKey key1, String data1, FilePropertyKey key2, String data2) {
        SmartQuery query = new SmartQuery(searchCategory);
        query.addData(key1, data1);
        query.addData(key2, data2);
        return query;
    }
    
    /**
     * Returns a new instance of SmartQuery with the specified field data.
     */
    public static SmartQuery newInstance(SearchCategory searchCategory, 
            FilePropertyKey key1, String data1, FilePropertyKey key2, String data2,
            FilePropertyKey key3, String data3) {
        SmartQuery query = new SmartQuery(searchCategory);
        query.addData(key1, data1);
        query.addData(key2, data2);
        query.addData(key3, data3);
        return query;
    }
    
    /**
     * Adds the specified property key and value to the query.
     */
    public void addData(FilePropertyKey key, String data) {
        queryData.put(key, data);
        queryKeys.add(key);
    }
    
    /**
     * Returns a map containing the query values.
     */
    public Map<FilePropertyKey, String> getQueryData() {
        return queryData;
    }
    
    /**
     * Returns the display text for the query.
     */
    @Override
    public String toString() {
        if (queryKeys.size() == 1) {
            FilePropertyKey key = queryKeys.get(0);
            String name = FilePropertyKeyUtils.getUntraslatedDisplayName(key, searchCategory);
            return I18n.tr("{0} called \"{1}\"", name, queryData.get(key));
            
        } else if (queryKeys.size() == 2) {
            FilePropertyKey key1 = queryKeys.get(0);
            FilePropertyKey key2 = queryKeys.get(1);
            String name1 = FilePropertyKeyUtils.getUntraslatedDisplayName(key1, searchCategory);
            String name2 = FilePropertyKeyUtils.getUntraslatedDisplayName(key2, searchCategory);
            return I18n.tr("{0} \"{1}\" - {2} \"{3}\"", name1, queryData.get(key1), 
                    name2, queryData.get(key2));
            
        } else if (queryKeys.size() > 2) {
            FilePropertyKey key1 = queryKeys.get(0);
            FilePropertyKey key2 = queryKeys.get(1);
            FilePropertyKey key3 = queryKeys.get(2);
            String name1 = FilePropertyKeyUtils.getUntraslatedDisplayName(key1, searchCategory);
            String name2 = FilePropertyKeyUtils.getUntraslatedDisplayName(key2, searchCategory);
            String name3 = FilePropertyKeyUtils.getUntraslatedDisplayName(key3, searchCategory);
            return I18n.tr("{0} \"{1}\" - {2} \"{3}\" - {4} \"{5}\"", name1, queryData.get(key1), 
                    name2, queryData.get(key2), name3, queryData.get(key3));
            
        } else {
            return "";
        }
    }
}
