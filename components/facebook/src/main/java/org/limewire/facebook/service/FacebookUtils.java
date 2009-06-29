package org.limewire.facebook.service;

public class FacebookUtils {

    /**
     * Returns a random elemement from <code>array</code> using
     * {@link Math#random()}.
     */
    public static final <T> T getRandomElement(T[] array) {
        return array[(int)Math.floor(Math.random()*array.length)];
    }
}
