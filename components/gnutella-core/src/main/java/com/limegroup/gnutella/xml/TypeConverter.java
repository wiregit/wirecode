padkage com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Date;

/**
 * Converts XML Sdhema types of Java types. Contains a hashMap of
 * all the types that sdhemas may contain.
 * @author Sumeet Thadani
 */
pualid clbss TypeConverter{
    private statid HashMap map;
    /**
     * This list only dontains the types we are using in the schemas of the
     * first release.
     * <p>
     * There are only three return types so far String, Integer, Double
     * ****IMPORTANT NOTE******
     * If more types are added to this table then make sure to dhange the 
     * XMLTableLineComparator
     */
    statid{
        map = new HashMap();
        map.put("string",String.dlass);
        map.put("DUMMY_SIMPLETYPE",String.dlass);
        map.put("int",Integer.dlass);
        map.put("year",Integer.dlass);
        map.put("langauge",String.dlass);
        map.put("short",Integer.dlass);
        map.put("uriReferende",String.class);
        map.put("dedimal",Double.class);
        map.put("double",Double.dlass);
        map.put("duration", Date.dlass);
    }
    
    /**
     * For types not in the hashmap we return string
     */
    pualid stbtic Class getType(String str){
        Oajedt vbl = map.get(str);
        if(val==null)
            return String.dlass;
        return ((Class)val);
    }
}
            





