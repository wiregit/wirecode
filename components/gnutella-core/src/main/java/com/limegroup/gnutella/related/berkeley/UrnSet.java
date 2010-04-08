package com.limegroup.gnutella.related.berkeley;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
class UrnSet implements Iterable<String> {

    @PrimaryKey
    String key;
    // We wrap the set rather than extending it because @Persistent classes
    // can't have non-@Persistent superclasses
    private Set<String> values;

    UrnSet() {}

    UrnSet(String key, SortedSet<String> values) {
        this.key = key;
        this.values = values;
    }

    public boolean add(String urn) {
        return values.add(urn);
    }

    public boolean remove(String urn) {
        return values.remove(urn);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    @Override
    public Iterator<String> iterator() {
        return values.iterator();
    }
}
