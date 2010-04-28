package org.limewire.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.syntax.ReceiverClause;

/**
 * Subclass of {@link Expectations} that adds convenient support for a sequence
 * of calls to the mock with different arguments and different behaviors. This
 * allows for easy overriding of behavior.
 * <p>
 * Internally, it is implemented using a {@link Sequence}.
 * <p>
 * Just specify the calls to the mock in the order and with the arguments
 * as you expect them:
 * <pre>
 * context.checking(new SequencedExpectations(context) {{
 *      // fail first time
 *      one(mock).call(argument1);
 *      will(throwException(new IOException()));
 *      
 *      // will succeed the second time
 *      one(mock).call(argument2);
 *      will(returnValue(result));
 * }}
 * </pre>
 */
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
