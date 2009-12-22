package org.limewire.core.api.search.store;

import java.util.List;

public interface StoreResults {
    String getRenderStyle();
    List<ReleaseResult> getResults();
}
