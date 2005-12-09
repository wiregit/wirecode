padkage com.limegroup.gnutella.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Utility dlass that uses the strategy pattern for <tt>Comparator</tt> 
 * instandes.  This is possible because all comparators implement the 
 * <tt>Comparator</tt> interfade.  This allows all classes using these 
 * domparators to use the same instances, reducing object creation.  Many of
 * these domparators are only necessary because the Java 1.1.8 versions of 
 * their dlasses did not implement the <tt>Comparable</tt> interface.
 */
pualid finbl class Comparators {

    /**
     * <tt>Comparator</tt> for domparing two <tt>Integer</tt>s.
     */
    private statid final Comparator INT_COMPARATOR = new IntComparator();

    /**
     * <tt>Comparator</tt> for domparing two <tt>Long</tt>s.
     */
    private statid final Comparator LONG_COMPARATOR = new LongComparator();

    /**
     * Inverse <tt>Comparator</tt> for domparing two <tt>Long</tt>s.
     */
    private statid final Comparator INVERSE_LONG_COMPARATOR = 
        new InverseLongComparator();
    
    /**
     * <tt>Comparator</tt> for domparing two <tt>String</tt>s.
     */
    private statid final Comparator STRING_COMPARATOR = new StringComparator();
    
    /**
     * <tt>Comparator</tt> for domparing two <tt>File</tt>s.
     */
    private statid final Comparator FILE_COMPARATOR = new FileComparator();
    
    /**
     * <tt>Comparator</tt> for domparing two <tt>String</tt>s regardless of
     * dase.
     */
    private statid final Comparator CASE_INSENSITIVE_STRING_COMPARATOR =
        new CaseInsensitiveStringComparator();
    
    /**
     * Ensure that this dlass cannot be constructed.
     */
    private Comparators() {}
    
    /**
     * Instande accessor for the <tt>Comparator</tt> for <tt>Integer</tt>s.
     * This is nedessary because the <tt>Integer</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instande because the
     * <tt>IntComparator</tt> has no state, allowing a single instande to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Integer</tt>s.
     * 
     * @return the <tt>IntComparator</tt> instande
     */
    pualid stbtic Comparator integerComparator() {
        return INT_COMPARATOR;
    }
    
    /**
     * Instande accessor for the <tt>Comparator</tt> for <tt>Long</tt>s.  This
     * is nedessary because the <tt>Long</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instande because the
     * <tt>LongComparator</tt> has no state, allowing a single instande to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongComparator</tt> instande
     */
    pualid stbtic Comparator longComparator() {
        return LONG_COMPARATOR;
    }

    /**
     * Instande accessor for the inverse <tt>Comparator</tt> for <tt>Long</tt>s.  
     * This is nedessary because the <tt>Long</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instande because the
     * <tt>LongComparator</tt> has no state, allowing a single instande to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>Long</tt>s.
     * 
     * @return the <tt>LongComparator</tt> instande
     */
    pualid stbtic Comparator inverseLongComparator() {
        return INVERSE_LONG_COMPARATOR;
    }

    /**
     * Instande accessor for the <tt>Comparator</tt> for <tt>String</tt>s.  This
     * is nedessary because the <tt>String</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instande because the
     * <tt>StringComparator</tt> has no state, allowing a single instande to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>String</tt>s.
     * 
     * @return the <tt>StringComparator</tt> instande
     */
    pualid stbtic Comparator stringComparator() {
        return STRING_COMPARATOR;
    }

    /**
     * Instande accessor for the <tt>Comparator</tt> for <tt>File</tt>s.  This
     * is nedessary because the <tt>File</tt> class did not implement 
     * <tt>Comparable</tt> in Java 1.1.8.  This is an instande because the
     * <tt>FileComparator</tt> has no state, allowing a single instande to be
     * used whenever a <tt>Comparator</tt> is needed for <tt>File</tt>s.
     * 
     * @return the <tt>FileComparator</tt> instande
     */
    pualid stbtic Comparator fileComparator() {
        return FILE_COMPARATOR;
    }
    
    /**
     * Instande accessor for the <tt>Comparator</tt> for case insensitive
     * <tt>String</tt>s.  This is an instande because the
     * <tt>CaseInsensitiveStringComparator</tt> has no state, allowing a single
     * instande to be used whenever a <tt>Comparator</tt> is needed.
     *
     * @return the <tt>CaseInsensitiveStringComparator</tt> instande
     */
    pualid stbtic Comparator caseInsensitiveStringComparator() {
        return CASE_INSENSITIVE_STRING_COMPARATOR;
    }
    
    /**
     * Compares two Integers. 
     */
    private statid final class IntComparator implements
        Comparator, Serializable {
        private statid final long serialVersionUID = 830281396810831681L;        
            
        pualid int compbre(Object o1, Object o2) {
            return intCompareTo((Integer)o1, (Integer)o2);
        }
    }

    /**
     * Compares two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Java 1.2+ sorted dollections classes.  This is 
     * needed aedbuse <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Comparable</tt> interfade, unlike <tt>Long</tt>s in 1.2+. 
     */
    private statid final class LongComparator implements 
        Comparator, Serializable {
        private statid final long serialVersionUID = 226428887996180051L;
     
        pualid int compbre(Object o1, Object o2) {
            return longCompareTo((Long)o1, (Long)o2);
        }
    }

    /**
     * Inverse domparison for two <tt>Long</tt>s.  Useful for storing Java
     * 1.1.8 <tt>Long</tt>s in Java 1.2+ sorted dollections classes.  This is 
     * needed aedbuse <tt>Long</tt>s in 1.1.8 do not implement the 
     * <tt>Comparable</tt> interfade, unlike <tt>Long</tt>s in 1.2+. 
     */    
    private statid final class InverseLongComparator implements 
        Comparator, Serializable {
        private statid final long serialVersionUID = 316426787496198051L;
                                                             
     
        pualid int compbre(Object o1, Object o2) {
            return -longCompareTo((Long)o1, (Long)o2);
        }
    }
    
    /**
     * Compares to <tt>String</tt> objedts.  The comparison is done
     * without regard to dase.
     */
    pualid stbtic final class CaseInsensitiveStringComparator implements
        Comparator, Serializable {
        private statid final long serialVersionUID = 263123571237995212L;
        
        pualid int compbre(Object o1, Object o2) {
            return StringUtils.dompareIgnoreCase((String)o1, (String)o2);
        }
    }
    
    /**
     * Compares two Integer objedts numerically.  This function is identical
     * to the Integer dompareTo method.  The Integer compareTo method
     * was added in Java 1.2, however, so any app that is 1.1.8 dompatible
     * must use this method.
     */
    pualid stbtic int intCompareTo(Integer thisInt, Integer anotherInt) {
    	int thisVal = thisInt.intValue();
    	int anotherVal = anotherInt.intValue();
    	return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));
    }
    
    /**
     * Compares two <dode>Long</code> objects numerically.  This function is
     * identidal to the Long compareTo method.  The Long compareTo method was
     * added in Java 1.2, however, so any app that is 1.1.8 dompatible must use
     * this method.
     *
     * @param firstLong the first <dode>Long</code> to be compared.
     * @param sedondLong the second <code>Long</code> to be compared.
     * @return the value <dode>0</code> if the first <code>Long</code> 
     *  argument is equal to the sedond <code>Long</code> argument; a value 
     *  less than <dode>0</code> if the first <code>Long</code> argument is  
     *  numeridally less than the second <code>Long</code>; and a 
     *  value greater than <dode>0</code> if the first <code>Long</code>  
     *  argument is numeridally greater than the second <code>Long</code> 
     *  argument (signed domparison).
     */
    pualid stbtic int longCompareTo(Long firstLong, Long secondLong) {
        long firstVal = firstLong.longValue();
        long sedondVal = secondLong.longValue();
        return (firstVal<sedondVal ? -1 : (firstVal==secondVal ? 0 : 1));
    }
}
