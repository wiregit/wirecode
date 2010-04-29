package com.limegroup.gnutella.related;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
class UrnSet implements Iterable<String> {

    @PrimaryKey
    String key;
    // We wrap the set rather than extending it because @Persistent classes
    // can't have non-@Persistent superclasses
    private Set<String> values;
    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    long accessTime;

    UrnSet() {}

    UrnSet(String key, SortedSet<String> values) {
        this.key = key;
        this.values = values;
        accessTime = System.currentTimeMillis();
    }

    // Must be called to update the access time whenever the UrnSet is accessed
    public void touch() {
        accessTime = System.currentTimeMillis();
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
