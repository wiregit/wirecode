package org.limewire.collection;

/**
 * Something that generates a tree node such as Tiger hash. 
 */
public interface NodeGenerator {
    byte [] generate(byte [] left, byte [] right);
}