package com.limegroup.gnutella.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Utility class that uses the strategy pattern for <tt>Comparator</tt> 
 * instances.  This is possible because all comparators implement the 
 * <tt>Comparator</tt> interface.  This allows all classes using these 
 * comparators to use the same instances, reducing object creation.  Many of
 * these comparators are only necessary because the Java 1.1.8 versions of 
 * their classes did not implement the <tt>Comparable</tt> interface.
 */
public final class Comparators {

    /**
     * <tt>Comparator</tt> for comparing two <tt>Integer</tt>s.
     */
    private static final Comparator INT_COMPARATOR = new IntComparator();

    /**
     * <tt>Comparator</tt> for comparing two <tt>Long</tt>s.
     */
    private static final Comparator LONG_COMPARATOR = new LongComparator();

    /**
     * Inverse <tt>Comparator</tt> for comparing two <tt>Long</tt>s.
     */
    private static final Comparator INVERSE_LONG_COMPARATOR = 
        new InverseLongComparator();
    
    /**
     * <tt>Comparator</tt> for comparing two <tt>String</tt>s.
     */
    private static final Comparator STRING_COMPARATOR = new StringComparator();
    
    /**
     * <tt>Comparator</tt> for comparing two <tt>File</tt>s.
     */
    private static final Comparator FILE_COMPARATOR = new FileComparator();
    
    /**
     * <tt>Comparator</tt> for comparing two <tt>String</tt>s regardless of
     * case.
     */
    private static final Comparator CASE_INSENSITIVE_STRING_COMPARATOR =
        new CaseInsensitiveStringComparator();
    
    /**
     * Ensure that this class cannot be constructed.
     */
    private Comparators() {}
    
    /**
     * Instance accessor for the <tt>Comparator</tt> for <tt>Integer</tt>s.
     * This is necessary because the <tt>Integer</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>IntComparator</tt> has no state, allowing a single instance to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Integer</tt>s.
     * 
     * @return the <tt>IntComparator</tt> instance
     */
    public static Comparator integerComparator() {
        return INT_COMPARATOR;
    }
    
    /**
     * Instance accessor for the <tt>Comparator</tt> for <tt>Long</tt>s.  This
     * is necessary because the <tt>Long</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>LongComparator</tt> has no state, allowing a single instance to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongComparator</tt> instance
     */
    public static Comparator longComparator() {
        return LONG_COMPARATOR;
    }

    /**
     * Instance accessor for the inverse <tt>Comparator</tt> for <tt>Long</tt>s.  
     * This is necessary because the <tt>Long</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>LongComparator</tt> has no state, allowing a single instance to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongComparator</tt> instance
     */
    public static Comparator inverseLongComparator() {
        return INVERSE_LONG_COMPARATOR;
    }

    /**
     * Instance accessor for the <tt>Comparator</tt> for <tt>String</tt>s.  This
     * is necessary because the <tt>String</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>StringComparator</tt> has no state, allowing a single instance to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>String</tt>s.
     * 
     * @return the <tt>StringComparator</tt> instance
     */
    public static Comparator stringComparator() {
        return STRING_COMPARATOR;
    }

    /**
     * Instance accessor for the <tt>Comparator</tt> for <tt>File</tt>s.  This
     * is necessary because the <tt>File</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>FileComparator</tt> has no state, allowing a single instance to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>File</tt>s.
     * 
     * @return the <tt>FileComparator</tt> instance
     */
    public static Comparator fileComparator() {
        return FILE_COMPARATOR;
    }
    
    /**
     * Instance accessor for the <tt>Comparator</tt> for case insensitive
     * <tt>String</tt>s.  This is an instance because the
     * <tt>CaseInsensitiveStringComparator</tt> has no state, allowing a single
     * instance to be used whenever a <tt>Comparator</tt> is needed.
     *
     * @return the <tt>CaseInsensitiveStringComparator</tt> instance
     */
    public static Comparator caseInsensitiveStringComparator() {
        return CASE_INSENSITIVE_STRING_COMPARATOR;
    }
    
    /**
     * Compares two Integers. 
     */
    private static final class IntComparator implements
        Comparator, Serializable {
        private static final long serialVersionUID = 830281396810831681L;        
            
        public int compare(Object o1, Object o2) {
            return intCompareTo((Integer)o1, (Integer)o2);
        }
    }

    /**
     * Compares two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Java 1.2+ sorted collections classes.  This is 
     * needed because <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Comparable</tt> interface, unlike <tt>Long</tt>s in 1.2+. 
     */
    private static final class LongComparator implements 
        Comparator, Serializable {
        private static final long serialVersionUID = 226428887996180051L;
     
        public int compare(Object o1, Object o2) {
            return longCompareTo((Long)o1, (Long)o2);
        }
    }

    /**
     * Inverse comparison for two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Java 1.2+ sorted collections classes.  This is 
     * needed because <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Comparable</tt> interface, unlike <tt>Long</tt>s in 1.2+. 
     */    
    private static final class InverseLongComparator implements 
        Comparator, Serializable {
        private static final long serialVersionUID = 316426787496198051L;
                                                             
     
        public int compare(Object o1, Object o2) {
            return -longCompareTo((Long)o1, (Long)o2);
        }
    }
    
    /**
     * Compares to <tt>String</tt> objects.  The comparison is done
     * without regard to case.
     */
    public static final class CaseInsensitiveStringComparator implements
        Comparator, Serializable {
        private static final long serialVersionUID = 263123571237995212L;
        
        public int compare(Object o1, Object o2) {
            return StringUtils.compareIgnoreCase((String)o1, (String)o2);
        }
    }
    
    /**
     * Compares two Integer objects numerically.  This function is identical
     * to the Integer compareTo method.  The Integer compareTo method
     * was added in Java 1.2, however, so any app that is 1.1.8 compatible
     * must use this method.
     */
    public static int intCompareTo(Integer thisInt, Integer anotherInt) {
    	int thisVal = thisInt.intValue();
    	int anotherVal = anotherInt.intValue();
    	return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }
    
    /**
     * Compares two <code>Long</code> objects numerically.  This function is
     * identical to the Long compareTo method.  The Long compareTo method was
     * added in Java 1.2, however, so any app that is 1.1.8 compatible must use
     * this method.
     *
     * @param firstLong the first <code>Long</code> to be compared.
     * @param secondLong the second <code>Long</code> to be compared.
     * @return the value <code>0</code> if the first <code>Long</code> 
     *  argument is equal to the second <code>Long</code> argument; a value 
     *  less than <code>0</code> if the first <code>Long</code> argument is  
     *  numerically less than the second <code>Long</code>; and a 
     *  value greater than <code>0</code> if the first <code>Long</code>  
     *  argument is numerically greater than the second <code>Long</code> 
     *  argument (signed comparison).
     */
    public static int longCompareTo(Long firstLong, Long secondLong) {
        long firstVal = firstLong.longValue();
        long secondVal = secondLong.longValue();
        return (firstVal<secondVal ? -1 : (firstVal==secondVal ? 0 : 1));
    }
}
