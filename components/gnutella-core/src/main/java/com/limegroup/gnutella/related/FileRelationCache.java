package com.limegroup.gnutella.related;

import java.util.Set;
import java.util.SortedSet;

import com.limegroup.gnutella.URN;

interface FileRelationCache {

    void addRelations(SortedSet<URN> files);

    void removeAllBrowses(URN file);

    Set<URN> getRelated(URN file);
}
