package com.redhat.na.gtm.ml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.camel.Exchange;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;

@ApplicationScoped
public class CSVPayloadProcessor {

    private static Logger log = Logger.getLogger(CSVPayloadProcessor.class);
    private static DateFormat dfObj = new SimpleDateFormat();
    private static long zeroLong = 0L;

    @Counted(name = "csvProcessed", description = "How many csv payloads have been processed.")
    @Timed(name = "csvProcessingTimer", description = "A measure of how long it takes to process a CSV file.", unit = MetricUnits.MILLISECONDS)
    public void process(Exchange exchange){
      
        try{
            byte[] fileNameHeaderBytes = (byte[])exchange.getIn().getHeader(Util.FILE_NAME_HEADER);
            String fHeader = new String(fileNameHeaderBytes);
            log.info("process() fHeader = "+fHeader);

        }catch(Throwable x){
            x.printStackTrace();
        }finally {
            
        }

    }
}
