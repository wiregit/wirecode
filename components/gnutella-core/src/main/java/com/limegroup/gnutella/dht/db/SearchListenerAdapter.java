package com.limegroup.gnutella.dht.db;

public class SearchListenerAdapter<Result> implements SearchListener<Result> {

    private static final SearchListener NULL_LISTENER = new SearchListenerAdapter();
    
    @SuppressWarnings({ "unchecked", "cast" })
    public static final <T> SearchListener<T> nullListener() {
        return (SearchListener<T>)NULL_LISTENER;
    }
    
    public static final <T> SearchListener<T> nonNullListener(SearchListener<T> listener) {
        if (listener != null) {
            return listener;
        } else {
            return nullListener();
        }
    }
    
    public void handleResult(Result result) {
    }
    
    public void handleSearchDone(boolean success) {
    }

}
