package com.limegroup.gnutella.related.berkeley;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
class Count {

    @PrimaryKey
    String key;
    int count;

    Count() {}

    Count(String key) {
        this.key = key;
    }
}
