package com.limegroup.gnutella.related;

interface Cache<K> {

    boolean add(K key);

    boolean remove(K key);

    boolean contains(K key);
}
