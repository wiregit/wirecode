package org.limewire.swarm.http.gnutella.interceptor;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.http.SwarmExecutionContext;

import com.limegroup.gnutella.http.ProblemReadingHeaderException;

public class AvailableRangesInterceptor implements HttpResponseInterceptor {
    
    public void process(HttpResponse response, HttpContext context) throws HttpException,
            IOException {
        Header header = response.getFirstHeader("X-Available-Ranges");
        if(header != null && header.getValue() != null) {
            IntervalSet availableRanges = new IntervalSet();
            String line = header.getValue().toLowerCase(Locale.US);
            // start parsing after the word "bytes"
            int start = line.indexOf("bytes") + 6;
            // if start == -1 the word bytes has not been found
            // if start >= line.length we are at the end of the 
            // header line
            while (start != -1 && start < line.length()) {
                // try to parse the number before the dash
                int stop = line.indexOf('-', start);
                // test if this is a valid interval
                if ( stop == -1 )
                    break; 

                // this is the interval to store the available 
                // range we are parsing in.
                Range interval = null;
        
                try {
                    // read number before dash
                    // bytes A-B, C-D
                    //       ^
                    long low = Long.parseLong(line.substring(start, stop).trim());
                    
                    // now moving the start index to the 
                    // character after the dash:
                    // bytes A-B, C-D
                    //         ^
                    start = stop + 1;
                    // we are parsing the number before the comma
                    stop = line.indexOf(',', start);
                    
                    // If we are at the end of the header line, there is no comma 
                    // following.
                    if ( stop == -1 )
                        stop = line.length();
                    
                    // read number after dash
                    // bytes A-B, C-D
                    //         ^
                    long high = Long.parseLong(line.substring(start, stop).trim());

                    // start parsing after the next comma. If we are at the
                    // end of the header line start will be set to 
                    // line.length() +1
                    start = stop + 1;

                    if(low > high)//interval read off network is bad, try next one
                        continue;

                    // this interval should be inclusive at both ends
                    interval = Range.createRange( low, high );
                    
                } catch (NumberFormatException e) {
                    throw new ProblemReadingHeaderException(e);
                }
                availableRanges.add(interval);
            }
            context.setAttribute(SwarmExecutionContext.HTTP_AVAILABLE_RANGES, availableRanges);
        }
    }

}
