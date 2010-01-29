package com.limegroup.gnutella.security;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.syntax.ReceiverClause;

public class SequencedExpectations extends Expectations {
    
    private final Sequence sequence;
    
    private final AtomicBoolean firstTime = new AtomicBoolean(true);

    public SequencedExpectations(Mockery context) {
        sequence = context.sequence("internal-sequence");
    }
    
    @Override
    public ReceiverClause exactly(int count) {
        if (!firstTime.compareAndSet(true, false)) {
            inSequence(sequence);
        }
        return super.exactly(count);
    }
}
