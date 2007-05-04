package com.limegroup.gnutella.util;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsUtils {
    private StatsUtils() {}
    
    private enum Quartile {
        Q1(1), MED(2), Q3(3);
        private final int type;
        Quartile(int type) {
            this.type = type;
        }
        public int getType() {
            return type;
        }
    }
    
    /**
     * @return the number, average, variance, min, median and max of a
     * list of BigIntegers
     */
    public static Map<String, Object> quickStats(List<BigInteger> l) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("num",l.size());
        
        if (l.size() < 2) // too small for stats
            return ret;
        
        Collections.sort(l);
        
        ret.put("min",l.get(0).toByteArray());
        ret.put("max",l.get(l.size() -1).toByteArray());
        ret.put("med", getQuartile(Quartile.MED, l).toByteArray());
        
        if (l.size() > 6) {
            // big enough to find outliers 
            ret.put("Q1", getQuartile(Quartile.Q1, l).toByteArray());
            ret.put("Q3", getQuartile(Quartile.Q3, l).toByteArray());
        }
        
        BigInteger sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) 
            sum = sum.add(bi);
        
        BigInteger avg = sum.divide(BigInteger.valueOf(l.size()));
        ret.put("avg",avg.toByteArray());
        
        sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) {
            BigInteger dist = bi.subtract(avg);
            dist = dist.multiply(dist);
            sum = sum.add(dist);
        }
        BigInteger variance = sum.divide(BigInteger.valueOf(l.size() - 1));
        ret.put("var",variance.toByteArray());
        return ret;
    }
    
    /**
     * @return the number, average, variance, min, median and max of a
     * list of Integers
     */
    public static Map<String, Object> quickStatsInt(List<Integer> l) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("num",l.size());
        
        if (l.size() < 2) // too small for stats
            return ret;
        
        Collections.sort(l);
        
        ret.put("min",l.get(0));
        ret.put("max",l.get(l.size() -1));
        ret.put("med", getQuartile(Quartile.MED, l));
        
        if (l.size() > 6) {
            // big enough to find outliers 
            ret.put("Q1", getQuartile(Quartile.Q1, l));
            ret.put("Q3", getQuartile(Quartile.Q3, l));
        }
        
        int sum = 0;
        for (int i : l) 
            sum += i;

        double avg = sum / l.size();
        ret.put("avg", Double.doubleToLongBits(avg));
        
        sum = 0;
        for (int i : l) {
            double dist = i - avg;
            dist *= dist;
            sum += dist;
        }
        
        ret.put("var",Double.doubleToLongBits((double)sum / (l.size() - 1)));
        return ret;
    }
    
    /**
     * the a specified quartile of a list of BigIntegers. It uses
     * type 6 of the quantile() function in R as explained in the
     * R help: 
     * 
     * "Type 6: p(k) = k / (n + 1). Thus p(k) = E[F(x[k])]. 
     * This is used by Minitab and by SPSS." 
     */
    private static BigInteger getQuartile(Quartile quartile, List<BigInteger> l) {
        double q1 = (l.size()+1) * (quartile.getType() / 4.0);
        int q1i = (int)q1;
        if (q1 - q1i == 0) 
            return l.get(q1i - 1);
        
        int quart = (int)(4 * (q1 - q1i));
        BigInteger q1a = l.get(q1i - 1);
        BigInteger q1b = l.get(q1i);
        q1b = q1b.subtract(q1a);
        q1b = q1b.multiply(BigInteger.valueOf(quart)); //1st multiply, then divide
        q1b = q1b.divide(BigInteger.valueOf(4)); // less precision is lost that way
        q1a = q1a.add(q1b);
        return q1a;
    }
    
    /**
     * the a specified quartile of a list of Integers. It uses
     * type 6 of the quantile() function in R as explained in the
     * R help: 
     * 
     * "Type 6: p(k) = k / (n + 1). Thus p(k) = E[F(x[k])]. 
     * This is used by Minitab and by SPSS."
     * 
     *  The return value is a long of the double value multiplied by Integer.MAX_VALUE
     *  so that as much precision is possible while transferring over network.
     */
    private static long getQuartile(Quartile quartile, List<Integer> l) {
        double q1 = (l.size()+1) * (quartile.getType() / 4.0);
        int q1i = (int)q1;
        if (q1 - q1i == 0) 
            return l.get(q1i - 1);
        
        int q1a = l.get(q1i - 1);
        double q1b = Double.valueOf(l.get(q1i));
        q1b = q1a - q1b;
        q1b = q1b * quartile.getType() / 4;
        return Double.doubleToLongBits(q1a+q1b);
    }
}
