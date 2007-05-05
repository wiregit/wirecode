package com.limegroup.gnutella.util;

import java.math.BigInteger;
import java.util.ArrayList;
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
        BigInteger sum3 = BigInteger.valueOf(0);
        BigInteger sum4 = BigInteger.valueOf(0);
        for (BigInteger bi : l) {
            BigInteger dist = bi.subtract(ret.avg);
            BigInteger dist2 = dist.multiply(dist);
            BigInteger dist3 = dist2.multiply(dist);
            BigInteger dist4 = dist2.multiply(dist2);
            sum = sum.add(dist2);
            sum3 = sum3.add(dist3);
            sum4 = sum4.add(dist4);
        }
        BigInteger div = BigInteger.valueOf(l.size() - 1);
        ret.m2 = sum.divide(div);
        ret.m3 = sum3.divide(div);
        ret.m4 = sum4.divide(div);
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
        double sum3 = 0;
        double sum4 = 0;
        for (double i : l) {
            double dist = i - ret.avg;
            double dist2 = dist * dist; 
            double dist3 = dist2 * dist;
            sum += dist2;
            sum3 += dist3;
            sum4 += (dist2 * dist2);
        }
        int div = l.size() - 1;
        ret.m2 = sum / div;
        ret.m3 = sum3 / div;
        ret.m4 = sum4 / div;
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
     * Creates a histogram of the provided data.
     * 
     * @param data list of observations
     * @param breaks # of breaks in the histogram
     * @return List of integer values size of the breaks
     */
    public static List<Integer> getHistogram(List<Double> data, int breaks) {
        List<Integer> ret = new ArrayList<Integer>(breaks);
        for (int i = 0; i < breaks; i++)
            ret.add(0);
        double min = Collections.min(data);
        double range = Collections.max(data) - min + 1;
        double step = range / breaks;
        for (double point : data) {
            // Math.min necessary because rounding error -> AIOOBE
            int index = Math.min((int)((point - min) / step), breaks - 1);
            ret.set(index, ret.get(index)+1);
        }
        return ret;
    }
    
    /**
     * Same as getHistogram but operates on BigIntegers.
     */
    public static List<Integer> getHistogramBigInt(List<BigInteger> data, int breaks) {
        List<Integer> ret = new ArrayList<Integer>(breaks);
        for (int i = 0; i < breaks; i++)
            ret.add(0);
        BigInteger min = Collections.min(data);
        BigInteger max = Collections.max(data);
        BigInteger range = max.subtract(min).add(BigInteger.valueOf(1));
        BigInteger step = range.divide(BigInteger.valueOf(breaks));
        for (BigInteger point : data) {
            int index = point.subtract(min).divide(step).intValue();
            // Math.min necessary because rounding error -> AIOOBE
            index = Math.min(index, breaks - 1);
            ret.set(index, ret.get(index)+1);
        }
        return ret;
    }
    
    /**
     * A stats object holding the minimum, maximum, median
     * average, quartiles 1 and 3 and second, third and fourth central moments
     */
    public abstract static class Stats {
        
        /*
         * first versioned version of this class.
         * previous iterations used variance ("var") which is now "m2"
         */ 
        private static final int VERSION = 1;
        
        /** The number of elements described in this */
        int number;
        
        /**
         * @return a Map object ready for bencoding.
         */
        public final Map<String, Object> getMap() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("ver", VERSION);
            ret.put("num", number);
            if (number < 2) // too small for stats
                return ret;
            ret.put("min", getMin());
            ret.put("max", getMax());
            ret.put("med", getMed());
            ret.put("avg", getAvg());
            ret.put("M2", getM2());
            ret.put("M3", getM3());
            ret.put("M4", getM4());
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
        public abstract Object getM2();
        public abstract Object getM3();
        public abstract Object getM4();
    }
    
    /**
     * Implementation of <tt>Stats</tt> using the double primitive. 
     */
    public static class DoubleStats extends Stats {
        DoubleStats() {}
        double min, max, med, q1, q3, avg, m2, m3, m4;
        public Object getMin() {
            return doubleToBytes(min);
        }
        public Object getMax() {
            return doubleToBytes(max);
        }
        public Object getMed() {
            return doubleToBytes(med);
        }
        public Object getQ1() {
            return doubleToBytes(q1);
        }
        public Object getQ3() {
            return doubleToBytes(q3);
        }
        public Object getAvg() {
            return doubleToBytes(avg);
        }
        public Object getM2() {
            return doubleToBytes(m2);
        }
        public Object getM3() {
            return doubleToBytes(m3);
        }
        public Object getM4() {
            return doubleToBytes(m4);
        }
        
        /**
         * @return byte [] representation of a double, compatible
         * with DataInputStream.readLong()
         */
        private byte [] doubleToBytes(double d) {
            byte [] writeBuffer = new byte[8];
            long v = Double.doubleToLongBits(d);
            writeBuffer[0] = (byte)(v >>> 56);
            writeBuffer[1] = (byte)(v >>> 48);
            writeBuffer[2] = (byte)(v >>> 40);
            writeBuffer[3] = (byte)(v >>> 32);
            writeBuffer[4] = (byte)(v >>> 24);
            writeBuffer[5] = (byte)(v >>> 16);
            writeBuffer[6] = (byte)(v >>>  8);
            writeBuffer[7] = (byte)(v >>>  0);
            return writeBuffer;
        }
    }

    /**
     * Implementation of <tt>Stats</tt> using <tt>BigInteger</tt> 
     */
    public static class BigIntStats extends Stats {
        BigIntStats(){}
        BigInteger min, max, med, q1, q3, avg, m2, m3, m4;
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
        public Object getM2() {
            return m2.toByteArray();
        }
        public Object getM3() {
            return m3.toByteArray();
        }
        public Object getM4() {
            return m4.toByteArray();
        }
    }
}
