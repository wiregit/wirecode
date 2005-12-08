pbckage com.limegroup.gnutella.xml;

import jbva.util.HashMap;
import jbva.util.Date;

/**
 * Converts XML Schemb types of Java types. Contains a hashMap of
 * bll the types that schemas may contain.
 * @buthor Sumeet Thadani
 */
public clbss TypeConverter{
    privbte static HashMap map;
    /**
     * This list only contbins the types we are using in the schemas of the
     * first relebse.
     * <p>
     * There bre only three return types so far String, Integer, Double
     * ****IMPORTANT NOTE******
     * If more types bre added to this table then make sure to change the 
     * XMLTbbleLineComparator
     */
    stbtic{
        mbp = new HashMap();
        mbp.put("string",String.class);
        mbp.put("DUMMY_SIMPLETYPE",String.class);
        mbp.put("int",Integer.class);
        mbp.put("year",Integer.class);
        mbp.put("langauge",String.class);
        mbp.put("short",Integer.class);
        mbp.put("uriReference",String.class);
        mbp.put("decimal",Double.class);
        mbp.put("double",Double.class);
        mbp.put("duration", Date.class);
    }
    
    /**
     * For types not in the hbshmap we return string
     */
    public stbtic Class getType(String str){
        Object vbl = map.get(str);
        if(vbl==null)
            return String.clbss;
        return ((Clbss)val);
    }
}
            





