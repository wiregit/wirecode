package org.limewire.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.limewire.listener.TypedEvent;

/**
 * A matcher to use for {@link TypedEvent}, to validate that the received event
 * is the proper class & the type within it is the proper type.
 */
public class EventTypeMatcher<E extends TypedEvent<T>, T> extends BaseMatcher<E> {
    
    private final Matcher<Object> instanceMatcher;
    private final Matcher<T> equalsMatcher;
    
    public EventTypeMatcher(Class<? extends E> eventClass, T type) {
        instanceMatcher = IsInstanceOf.instanceOf(eventClass);
        equalsMatcher = IsEqual.equalTo(type);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void describeTo(Description description) {
        description.appendValueList("(", " and ", ")", instanceMatcher, equalsMatcher);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(Object item) {
        if(instanceMatcher.matches(item)) {            
            TypedEvent<T> typed = (TypedEvent<T>)item;
            T type = typed.getType();
            return equalsMatcher.matches(type);
        } else {
            return false;
        }
    }
    
    public static <E extends TypedEvent<T>, T> EventTypeMatcher<E, T> eventTypeMatches(Class<? extends E> eventClass, T type) {
        return new EventTypeMatcher<E, T>(eventClass, type);
    }

}
