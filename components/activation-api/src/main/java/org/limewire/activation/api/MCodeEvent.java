package org.limewire.activation.api;

import org.limewire.listener.DefaultDataEvent;

/**
 * This event is fired by the ActivationManager when the mcode (the string representing which pro features a user has) is ready to to be used.
 * The mcode is ready immediately at start-up if the user is not a pro user.
 * If they are a pro user, it's ready once it's been returned by the activation server or the activation server has been determined
 * to be unreachable due to a communication error.
 */
public class MCodeEvent extends DefaultDataEvent<String> {

    public MCodeEvent(String mcode) {
        super(mcode);
    }

}
