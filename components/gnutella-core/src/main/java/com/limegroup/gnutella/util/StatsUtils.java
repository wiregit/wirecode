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
    public static BigIntStats quickStatsBigInt(List<BigInteger> l) {
        BigIntStats ret = new BigIntStats();
        ret.number = l.size();
        if (ret.number < 2)
            return ret;
        Collections.sort(l);
        ret.min = l.get(0);
        ret.max = l.get(l.size() - 1);
        ret.med = getQuartile(Quartile.MED, l);
        if (ret.number > 6) {
            ret.q1 = getQuartile(Quartile.Q1, l);
            ret.q3 = getQuartile(Quartile.Q3, l);
        }
        
        BigInteger sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) 
            sum = sum.add(bi);
        
        ret.avg = sum.divide(BigInteger.valueOf(l.size()));
        
        sum = BigInteger.valueOf(0);
        for (BigInteger bi : l) {
            BigInteger dist = bi.subtract(ret.avg);
            dist = dist.multiply(dist);
            sum = sum.add(dist);
        }
        ret.var = sum.divide(BigInteger.valueOf(l.size() - 1));
        return ret;
    }
    
    /**
     * @return the number, average, variance, min, median and max of a
     * list of Integers
     */
    public static DoubleStats quickStatsDouble(List<Double> l) {
        DoubleStats ret = new DoubleStats();
        ret.number = l.size();
        if (ret.number < 2)
            return ret;
        
        Collections.sort(l);
        ret.min = l.get(0);
        ret.max = l.get(l.size() - 1);
        ret.med = getQuartile(Quartile.MED, l);
        if (ret.number > 6) {
            ret.q1 = getQuartile(Quartile.Q1, l);
            ret.q3 = getQuartile(Quartile.Q3, l);
        }
        
        double sum = 0;
        for (double i : l) 
            sum += i;

        ret.avg = sum / l.size();
        
        sum = 0;
        for (double i : l) {
            double dist = i - ret.avg;
            dist *= dist;
            sum += dist;
        }
        ret.var = sum / (l.size() - 1);
        
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
    private static double getQuartile(Quartile quartile, List<Double> l) {
        double q1 = (l.size()+1) * (quartile.getType() / 4.0);
        int q1i = (int)q1;
        if (q1 - q1i == 0) 
            return l.get(q1i - 1);
        
        double q1a = l.get(q1i - 1);
        double q1b = l.get(q1i);
        q1b = q1b - q1a;
        q1b = q1b * quartile.getType() / 4;
        return q1a+q1b;
    }
    
    /**
     * A stats object holding the minimum, maximum, median
     * average, variance and quartiles 1 and 3.
     */
    public abstract static class Stats {
        
        /** The number of elements described in this */
        int number;
        
        /**
         * @return a Map object ready for bencoding.
         */
        public final Map<String, Object> getMap() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("num", number);
            if (number < 2) // too small for stats
                return ret;
            ret.put("min", getMin());
            ret.put("max", getMax());
            ret.put("med", getMed());
            ret.put("avg", getAvg());
            ret.put("var", getVar());
            if (number > 6) {
                ret.put("Q1", getQ1());
                ret.put("Q3", getQ3());
            }
            return ret;
        }
        
        public final int getNumber() {
            return number;
        }
        
        public abstract Object getMin();
        public abstract Object getMax();
        public abstract Object getMed();
        public abstract Object getAvg();
        public abstract Object getQ1();
        public abstract Object getQ3();
        public abstract Object getVar();
    }
    
    /**
     * Implementation of <tt>Stats</tt> using the double primitive. 
     */
    public static class DoubleStats extends Stats {
        DoubleStats() {}
        double min, max, med, q1, q3, avg, var;
        public Object getMin() {
            return Double.doubleToLongBits(min);
        }
        public Object getMax() {
            return Double.doubleToLongBits(max);
        }
        public Object getMed() {
            return Double.doubleToLongBits(med);
        }
        public Object getQ1() {
            return Double.doubleToLongBits(q1);
        }
        public Object getQ3() {
            return Double.doubleToLongBits(q3);
        }
        public Object getAvg() {
            return Double.doubleToLongBits(avg);
        }
        public Object getVar() {
            return Double.doubleToLongBits(var);
        }
    }

    /**
     * Implementation of <tt>Stats</tt> using <tt>BigInteger</tt> 
     */
    public static class BigIntStats extends Stats {
        BigIntStats(){}
        BigInteger min, max, med, q1, q3, avg, var;
        public Object getMin() {
            return min.toByteArray();
        }
        public Object getMax() {
            return max.toByteArray();
        }
        public Object getMed() {
            return med.toByteArray();
        }
        public Object getQ1() {
            return q1.toByteArray();
        }
        public Object getQ3() {
            return q3.toByteArray();
        }
        public Object getAvg() {
            return avg.toByteArray();
        }
        public Object getVar() {
            return var.toByteArray();
        }
    }
}
