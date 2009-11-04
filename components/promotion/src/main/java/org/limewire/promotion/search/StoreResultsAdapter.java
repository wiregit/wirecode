package org.limewire.promotion.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.StoreResult;
import org.limewire.core.api.search.store.StoreResults;

public class StoreResultsAdapter implements StoreResults {
    private final String renderStyle;
    private final List<StoreResult> items;
    
    public StoreResultsAdapter(JSONObject jsonObject, StoreConnection connection) throws JSONException, IOException {
        renderStyle = jsonObject.optString("renderStyle");
        items = new ArrayList<StoreResult>();
        JSONArray resultsArr = jsonObject.getJSONArray("items");
        
        for (int i = 0, len = resultsArr.length(); i < len; i++) {
            JSONObject resultObj = resultsArr.getJSONObject(i);
            items.add(new StoreResultAdapter(resultObj, connection));
        }
    }

    @Override
    public String getRenderStyle() {
        return "STYLE_" + renderStyle;
    }

    @Override
    public List<StoreResult> getItems() {
        return items;
    }
}
