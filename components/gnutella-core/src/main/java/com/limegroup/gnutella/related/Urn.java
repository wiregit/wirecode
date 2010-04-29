package com.limegroup.gnutella.related;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
class Urn {

    @PrimaryKey
    String urn;
    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    long accessTime;

    Urn() {}

    Urn(String urn) {
        this.urn = urn;
        accessTime = System.currentTimeMillis();
    }
}
