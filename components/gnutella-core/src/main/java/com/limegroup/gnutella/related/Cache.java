package com.limegroup.gnutella.related;

public interface Cache<K> {

    boolean add(K key);

    boolean remove(K key);

    boolean contains(K key);

    int get(K key);

    int increment(K key);

    int total();
}
