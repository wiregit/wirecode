package com.limegroup.gnutella.security;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;

public class SequencedExpectations extends Expectations {
    
    private final Sequence sequence;
    
    private final AtomicBoolean firstTime = new AtomicBoolean(true);

    public SequencedExpectations(Mockery context) {
        sequence = context.sequence("internal-sequence");
    }
    
    @Override
    public <T extends Object> T one(T mockObject) {
        if (!firstTime.compareAndSet(true, false)) {
            inSequence(sequence);
        }
        return super.one(mockObject);
    }

}
