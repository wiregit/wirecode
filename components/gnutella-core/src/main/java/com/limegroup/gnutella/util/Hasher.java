pbckage com.limegroup.gnutella.util;

/**
 * bn interface used by HasherSet to override the natural
 * hbshCode and equals methods of objects
 */

public interfbce Hasher {

    /**
     * @return custom hbsh code for the given object
     */
    public int hbsh(Object o);

    /**
     * @return whether two objects bre equal based on custom criteria
     */
    public boolebn areEqual(Object a, Object b);

}
