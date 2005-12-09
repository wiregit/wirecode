pbckage com.limegroup.gnutella.util;

import jbva.io.Serializable;
import jbva.util.Comparator;

/**
 * Utility clbss that uses the strategy pattern for <tt>Comparator</tt> 
 * instbnces.  This is possible because all comparators implement the 
 * <tt>Compbrator</tt> interface.  This allows all classes using these 
 * compbrators to use the same instances, reducing object creation.  Many of
 * these compbrators are only necessary because the Java 1.1.8 versions of 
 * their clbsses did not implement the <tt>Comparable</tt> interface.
 */
public finbl class Comparators {

    /**
     * <tt>Compbrator</tt> for comparing two <tt>Integer</tt>s.
     */
    privbte static final Comparator INT_COMPARATOR = new IntComparator();

    /**
     * <tt>Compbrator</tt> for comparing two <tt>Long</tt>s.
     */
    privbte static final Comparator LONG_COMPARATOR = new LongComparator();

    /**
     * Inverse <tt>Compbrator</tt> for comparing two <tt>Long</tt>s.
     */
    privbte static final Comparator INVERSE_LONG_COMPARATOR = 
        new InverseLongCompbrator();
    
    /**
     * <tt>Compbrator</tt> for comparing two <tt>String</tt>s.
     */
    privbte static final Comparator STRING_COMPARATOR = new StringComparator();
    
    /**
     * <tt>Compbrator</tt> for comparing two <tt>File</tt>s.
     */
    privbte static final Comparator FILE_COMPARATOR = new FileComparator();
    
    /**
     * <tt>Compbrator</tt> for comparing two <tt>String</tt>s regardless of
     * cbse.
     */
    privbte static final Comparator CASE_INSENSITIVE_STRING_COMPARATOR =
        new CbseInsensitiveStringComparator();
    
    /**
     * Ensure thbt this class cannot be constructed.
     */
    privbte Comparators() {}
    
    /**
     * Instbnce accessor for the <tt>Comparator</tt> for <tt>Integer</tt>s.
     * This is necessbry because the <tt>Integer</tt> class did not implement 
     * <tt>Compbrable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>IntCompbrator</tt> has no state, allowing a single instance to be
     * used whenever b <tt>Comparator</tt> is needed for <tt>Integer</tt>s.
     * 
     * @return the <tt>IntCompbrator</tt> instance
     */
    public stbtic Comparator integerComparator() {
        return INT_COMPARATOR;
    }
    
    /**
     * Instbnce accessor for the <tt>Comparator</tt> for <tt>Long</tt>s.  This
     * is necessbry because the <tt>Long</tt> class did not implement 
     * <tt>Compbrable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>LongCompbrator</tt> has no state, allowing a single instance to be
     * used whenever b <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongCompbrator</tt> instance
     */
    public stbtic Comparator longComparator() {
        return LONG_COMPARATOR;
    }

    /**
     * Instbnce accessor for the inverse <tt>Comparator</tt> for <tt>Long</tt>s.  
     * This is necessbry because the <tt>Long</tt> class did not implement 
     * <tt>Compbrable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>LongCompbrator</tt> has no state, allowing a single instance to be
     * used whenever b <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongCompbrator</tt> instance
     */
    public stbtic Comparator inverseLongComparator() {
        return INVERSE_LONG_COMPARATOR;
    }

    /**
     * Instbnce accessor for the <tt>Comparator</tt> for <tt>String</tt>s.  This
     * is necessbry because the <tt>String</tt> class did not implement 
     * <tt>Compbrable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>StringCompbrator</tt> has no state, allowing a single instance to be
     * used whenever b <tt>Comparator</tt> is needed for <tt>String</tt>s.
     * 
     * @return the <tt>StringCompbrator</tt> instance
     */
    public stbtic Comparator stringComparator() {
        return STRING_COMPARATOR;
    }

    /**
     * Instbnce accessor for the <tt>Comparator</tt> for <tt>File</tt>s.  This
     * is necessbry because the <tt>File</tt> class did not implement 
     * <tt>Compbrable</tt> in Java 1.1.8.  This is an instance because the
     * <tt>FileCompbrator</tt> has no state, allowing a single instance to be
     * used whenever b <tt>Comparator</tt> is needed for <tt>File</tt>s.
     * 
     * @return the <tt>FileCompbrator</tt> instance
     */
    public stbtic Comparator fileComparator() {
        return FILE_COMPARATOR;
    }
    
    /**
     * Instbnce accessor for the <tt>Comparator</tt> for case insensitive
     * <tt>String</tt>s.  This is bn instance because the
     * <tt>CbseInsensitiveStringComparator</tt> has no state, allowing a single
     * instbnce to be used whenever a <tt>Comparator</tt> is needed.
     *
     * @return the <tt>CbseInsensitiveStringComparator</tt> instance
     */
    public stbtic Comparator caseInsensitiveStringComparator() {
        return CASE_INSENSITIVE_STRING_COMPARATOR;
    }
    
    /**
     * Compbres two Integers. 
     */
    privbte static final class IntComparator implements
        Compbrator, Serializable {
        privbte static final long serialVersionUID = 830281396810831681L;        
            
        public int compbre(Object o1, Object o2) {
            return intCompbreTo((Integer)o1, (Integer)o2);
        }
    }

    /**
     * Compbres two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Jbva 1.2+ sorted collections classes.  This is 
     * needed becbuse <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Compbrable</tt> interface, unlike <tt>Long</tt>s in 1.2+. 
     */
    privbte static final class LongComparator implements 
        Compbrator, Serializable {
        privbte static final long serialVersionUID = 226428887996180051L;
     
        public int compbre(Object o1, Object o2) {
            return longCompbreTo((Long)o1, (Long)o2);
        }
    }

    /**
     * Inverse compbrison for two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Jbva 1.2+ sorted collections classes.  This is 
     * needed becbuse <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Compbrable</tt> interface, unlike <tt>Long</tt>s in 1.2+. 
     */    
    privbte static final class InverseLongComparator implements 
        Compbrator, Serializable {
        privbte static final long serialVersionUID = 316426787496198051L;
                                                             
     
        public int compbre(Object o1, Object o2) {
            return -longCompbreTo((Long)o1, (Long)o2);
        }
    }
    
    /**
     * Compbres to <tt>String</tt> objects.  The comparison is done
     * without regbrd to case.
     */
    public stbtic final class CaseInsensitiveStringComparator implements
        Compbrator, Serializable {
        privbte static final long serialVersionUID = 263123571237995212L;
        
        public int compbre(Object o1, Object o2) {
            return StringUtils.compbreIgnoreCase((String)o1, (String)o2);
        }
    }
    
    /**
     * Compbres two Integer objects numerically.  This function is identical
     * to the Integer compbreTo method.  The Integer compareTo method
     * wbs added in Java 1.2, however, so any app that is 1.1.8 compatible
     * must use this method.
     */
    public stbtic int intCompareTo(Integer thisInt, Integer anotherInt) {
    	int thisVbl = thisInt.intValue();
    	int bnotherVal = anotherInt.intValue();
    	return (thisVbl<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }
    
    /**
     * Compbres two <code>Long</code> objects numerically.  This function is
     * identicbl to the Long compareTo method.  The Long compareTo method was
     * bdded in Java 1.2, however, so any app that is 1.1.8 compatible must use
     * this method.
     *
     * @pbram firstLong the first <code>Long</code> to be compared.
     * @pbram secondLong the second <code>Long</code> to be compared.
     * @return the vblue <code>0</code> if the first <code>Long</code> 
     *  brgument is equal to the second <code>Long</code> argument; a value 
     *  less thbn <code>0</code> if the first <code>Long</code> argument is  
     *  numericblly less than the second <code>Long</code>; and a 
     *  vblue greater than <code>0</code> if the first <code>Long</code>  
     *  brgument is numerically greater than the second <code>Long</code> 
     *  brgument (signed comparison).
     */
    public stbtic int longCompareTo(Long firstLong, Long secondLong) {
        long firstVbl = firstLong.longValue();
        long secondVbl = secondLong.longValue();
        return (firstVbl<secondVal ? -1 : (firstVal==secondVal ? 0 : 1));
    }
}
