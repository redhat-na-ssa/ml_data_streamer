package com.redhat.na.gtm.ml;

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;

import org.jboss.logging.Logger;

public class SimpleCSVAggregator implements AggregationStrategy {

    private static Logger log = Logger.getLogger(SimpleCSVAggregator.class);

    boolean csvContainersHeader = true;

    public SimpleCSVAggregator(boolean csvContainersHeader){
        this.csvContainersHeader = csvContainersHeader;
        if(!csvContainersHeader){
            log.info("SimpleCSVAggregator() configured for CSVs without headers");
        }
    }

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        String body = newExchange.getIn().getBody(String.class);
        if(oldExchange == null) {

            // Set Exchange body as a Set and add first CSV file
            Set<String>  set = new HashSet<String>();
            set.add(body);
            newExchange.getIn().setBody(set);
            return newExchange;
        }else {

            // Strip header line from all subsequent files
            if(csvContainersHeader){
                int index = body.indexOf(System.lineSeparator());
                body = body.substring(index+1);
            }

            // Add csv to Exchange body set
            Set<String> set = oldExchange.getIn().getBody(Set.class);
            set.add(body);
            return oldExchange;
        }
    }

}
