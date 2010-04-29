package com.limegroup.gnutella.related;

interface CountCache<K> extends Cache<K> {

    int get(K key);

    int increment(K key);

    int total();
}
