package com.limegroup.gnutella.filters;

/**
 * Manages a file containing blacklisted URNs, which is updated periodically
 * via HTTP. The manager's <code>iterator()</code> method can be used to read
 * the URNs from disk as base32-encoded strings.
 */
public interface URNBlacklistManager extends Iterable<String> {
    /** The maximum length of the blacklist in bytes (20 bytes per URN). */
    public static final int MAX_LENGTH = 200000; // Ten thousand URNs
}
