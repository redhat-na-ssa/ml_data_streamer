package com.redhat.na.gtm.ml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * Camel route definitions.
 */
@ApplicationScoped
public class Routes extends RouteBuilder {

    private static Logger log = Logger.getLogger(Routes.class);  
    
    @Inject
    CSVPayloadProcessor csvPayLoadProcessor;
    
    public Routes() {
    }

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        /*****                Consume from HTTP           *************/
        rest("/sanityCheck")
                .get()
                .to("direct:sanity");
        
        from("direct:sanity")
            .setBody().constant("Good To Go!");

        /************               Consume from Kafka          *****************/
        from("kafka:{{com.rht.na.gtm.topic.name}}?brokers={{kafka.bootstrap.servers}}&groupId=rht&autoOffsetReset=earliest&consumersCount={{com.rht.na.gtm.kafka.consumer.count}}")
            .doTry()
                .process(new CSVPayloadValidator())
                .process(e -> {
                    csvPayLoadProcessor.process(e);
                })
            .doCatch(ValidationException.class)
                .log(LoggingLevel.ERROR, exceptionMessage().toString())
            .end();


    }

    class CSVPayloadValidator implements Processor {

        @Override
        public void process(Exchange exchange) throws ValidationException {

            Object fileNameHeaderObj = exchange.getIn().getHeader(Util.FILE_NAME_HEADER);
            if(fileNameHeaderObj == null)
              throw new ValidationException("000002 Must pass kafka header of: "+Util.FILE_NAME_HEADER);

            String fHeader = new String((byte[])fileNameHeaderObj);
            if(!fHeader.endsWith(Util.CSV))
              throw new ValidationException("000003 Invalid file suffix: "+fHeader);
            

            Object bObj = exchange.getIn().getBody();
            if(!bObj.getClass().getName().equals(String.class.getName()))
              throw new ValidationException("000004 Payload not of type String : "+bObj.getClass().getName());

        }
    }
}
