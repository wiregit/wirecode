package com.limegroup.gnutella.xml;

import com.sun.java.util.collections.*;

/**
 * Converts XML Schema types of Java types. Contains a hashMap of
 * all the types that schemas may contain.
 * @author Sumeet Thadani
 */
public class TypeConverter{
    private static HashMap map;
    /**
     * This list only contains the types we are using in the schemas of the
     * first release.
     * <p>
     * There are only three return types so far String, Integer, Double
     * ****IMPORTANT NOTE******
     * If more types are added to this table then make sure to change the 
     * XMLTableLineComparator
     */
    static{
        map = new HashMap();
        map.put("string",String.class);
        map.put("DUMMY_SIMPLETYPE",String.class);
        map.put("int",Integer.class);
        map.put("year",Integer.class);
        map.put("langauge",String.class);
        map.put("short",Integer.class);
        map.put("uriReference",String.class);
        map.put("decimal",Double.class);
        map.put("double",Double.class);
    }
    
    /**
     * For types not in the hashmap we return string
     */
    public static Class getType(String str){
        Object val = map.get(str);
        if(val==null)
            return String.class;
        return ((Class)val);
    }
}
            





