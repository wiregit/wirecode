package com.limegroup.gnutella;

import java.util.List;

import org.limewire.core.api.Category;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.limegroup.gnutella.messages.QueryRequest;

public class MediaTypeAggregatorStub extends QueryCategoryFilterer {

    public MediaTypeAggregatorStub() {
        super(null);
    }

    @Override
    public Predicate<String> getPredicateForQuery(QueryRequest query) {
        return Predicates.alwaysTrue();
    }

    @Override
    public List<Category> getRequestedCategories(QueryRequest query) {
        return null;
    }

}
