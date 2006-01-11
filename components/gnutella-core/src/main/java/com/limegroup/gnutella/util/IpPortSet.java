
// Commented for the Learning branch

package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * An IpPortSet object keeps objects that support the IpPort interface in sorted order with no duplicates.
 * The objects are probably ExtendedEndpoint objects.
 * 
 * IpPortSet extends java.util.TreeSet.
 * A TreeSet has the Set interface, and keeps its data in a TreeMap object.
 * A TreeMap is a list that keeps its contents in sorted order.
 * The Set interface requires that there are no duplicates, checking for them with o.equals(o).
 * It has methods like add(o) and clear().
 * To move through the contents, get an Iterator by calling iterator().
 * 
 * The TreeSet calls IpPort.IpPortComparator.compare(a, b) to sort items and detect duplicates.
 */
public class IpPortSet extends TreeSet {

    /**
     * Make a new IpPortSet object that will keep objects that support the IpPort interface in sorted order with no duplicates.
     * Set up this new TreeSet with the IpPort.IpPortComparator.compare(a, b) method it will use to keep the IpPort objects in sorted order.
     */
    public IpPortSet() {

        // Call the TreeSet(Comparator c) constructor, telling it to use the IpPort.IpPortComparator.compare(a, b) method to keep the IpPort objects sorted
        super(IpPort.COMPARATOR);
    }

    /**
     * Make a new IpPortSet object that will keep objects that support the IpPort interface in sorted order with no duplicates.
     * Sets the IpPortComparator.compare(a, b) method, and adds all the IpPort objects from the Collection c.
     * 
     * @param c A Collection of IpPort objects to add to this new one
     */
    public IpPortSet(Collection c) {

        // Set up this new TreeSet with the IpPort.IpPortComparator.compare(a, b) method it will use to keep the IpPort objects in sorted order.
        this(); // Call the first constructor, the one without any arguments

        addAll(c);
    }

    /**
     * Make a new IpPortSet object that will keep objects that support the IpPort interface in sorted order with no duplicates.
     * Sets the IpPortComparator.compare(a, b) method.
     * 
     * @param c A Comparator that we won't use
     */
    public IpPortSet(Comparator c) {

        /*
         * We're ignoring the given Comparator c.
         * We need to use the normal IpPort.IpPortComparator.compare(a, b) method instead.
         */

        // Set up this new TreeSet with the IpPort.IpPortComparator.compare(a, b) method it will use to keep the IpPort objects in sorted order.
        this(); // Call the first constructor, the one without any arguments
    }
}
