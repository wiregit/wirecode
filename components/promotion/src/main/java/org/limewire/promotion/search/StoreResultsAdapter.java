package org.limewire.promotion.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.core.api.search.store.StoreConnection;
import org.limewire.core.api.search.store.ReleaseResult;
import org.limewire.core.api.search.store.StoreResults;
import org.limewire.core.api.URNFactory;
import org.limewire.concurrent.ScheduledListeningExecutorService;

class StoreResultsAdapter implements StoreResults {
    private final String renderStyle;
    private final List<ReleaseResult> items;
    
    StoreResultsAdapter(JSONObject jsonObject, StoreConnection connection,
                        ScheduledListeningExecutorService executorService,
                        URNFactory urnFactory) throws JSONException, IOException {
        renderStyle = jsonObject.optString("renderStyle");
        items = new ArrayList<ReleaseResult>();
        JSONArray resultsArr = jsonObject.getJSONArray("items");
        
        for (int i = 0, len = resultsArr.length(); i < len; i++) {
            JSONObject resultObj = resultsArr.getJSONObject(i);
            items.add(new ReleaseResultAdapter(resultObj, connection, executorService, urnFactory));
        }
    }

    @Override
    public String getRenderStyle() {
        return "STYLE_" + renderStyle;
    }

    @Override
    public List<ReleaseResult> getResults() {
        return items;
    }
}
