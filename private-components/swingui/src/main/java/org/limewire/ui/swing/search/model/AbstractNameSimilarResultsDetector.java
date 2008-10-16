package org.limewire.ui.swing.search.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class AbstractNameSimilarResultsDetector extends AbstractSimilarResultsDetector {

    protected final CleanStringCache nameCache;
    protected final Map<String, VisualSearchResult> matchCache;
    
    public AbstractNameSimilarResultsDetector(Pattern pattern) {
        this.nameCache = new CleanStringCache(pattern, "");
        this.matchCache = new HashMap<String, VisualSearchResult>();
    }

    @Override
    public void detectSimilarResult(VisualSearchResult visualSearchResult) {
        if (!visualSearchResult.isSpam()) {
            Set<String> names = getCleanIdentifyingStrings(visualSearchResult);
            VisualSearchResult parent = null;
            for (String name : names) {
                parent = matchCache.get(name);
                if (parent == null) {
                    matchCache.put(name, visualSearchResult);
                } else if (parent != visualSearchResult){
                    parent = update(parent, visualSearchResult);
                    matchCache.put(name, parent);
                }
            }
        }
    }

    public CleanStringCache getNameCache() {
        return nameCache;
    }

    public abstract Set<String> getCleanIdentifyingStrings(VisualSearchResult visualSearchResult);

}