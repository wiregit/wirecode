package org.limewire.listener;

public class AbstractSourcedEvent<S> {
    
    private final S source;
    
    public AbstractSourcedEvent(S source) {
        this.source = source;
    }
    
    public S getSource() {
        return source;
    }

}
