package com.redhat.na.gtm.ml;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.redhat.na.gtm.S3LifecycleProcessor;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Camel route definitions.
 */
@ApplicationScoped
public class Routes extends RouteBuilder {

    private static Logger log = Logger.getLogger(Routes.class);  
    
    @Inject
    CSVPayloadProcessor csvPayLoadProcessor;

    @Inject
    S3LifecycleProcessor s3LifecycleProcessor;

    @ConfigProperty(name="com.redhat.na.gtm.ml.dump_headers", defaultValue = "false")
    boolean dumpHeaders = false;
    
    public Routes() {
    }

    @Override
    public void configure() throws Exception {

        //restConfiguration().bindingMode(RestBindingMode.json);

        /*****                Consume from HTTP           *************/
        rest("/sanityCheck")
            .post().to("seda:s3lifecycle")
            .get().to("direct:sanity");
                
        from("direct:sanity")
            .setBody().constant("Good To Go!");

        from("seda:s3lifecycle")
            .setBody().constant("Good To Go!")
            .process(e -> {
                s3LifecycleProcessor.postBody(e);
            })
            .end();

        /************               Consume from Kafka          *****************/
        from("kafka:{{com.rht.na.gtm.topic.name}}?brokers={{kafka.bootstrap.servers}}&groupId=rht&autoOffsetReset=earliest&consumersCount={{com.rht.na.gtm.kafka.consumer.count}}")
            .doTry()
                .process(new CSVPayloadValidator())
                .process(e -> {
                    s3LifecycleProcessor.postBody(e);
                })
            .doCatch(ValidationException.class)
                .log(LoggingLevel.ERROR, exceptionMessage().toString())
            .end();


    }

    class CSVPayloadValidator implements Processor {

        @Override
        public void process(Exchange exchange) throws ValidationException {

            if(dumpHeaders) {
                Map<String, Object> headers = exchange.getIn().getHeaders();
                for(Entry<String, Object> hEntry : headers.entrySet()){
                    log.info("headers: "+hEntry.getKey()+" :"+hEntry.getValue());
                }
            }

            Object concatFileNum = exchange.getIn().getHeader(Util.CONCANTENATED_FILE_NUM);
            if(concatFileNum == null)
              throw new ValidationException("000002 Must pass kafka header of: "+Util.CONCANTENATED_FILE_NUM);

            byte[] fHeaderBytes = (byte[])exchange.getIn().getHeader(Util.FILE_NAME_HEADER);
            if(fHeaderBytes != null) {
                String fHeader = new String(fHeaderBytes);
                if(!fHeader.endsWith(Util.CSV)){
                    throw new ValidationException("0000035 Invalid header suffix for file: "+fHeader);
                }
            }else {
                throw new ValidationException("0000030 No header in message: "+Util.FILE_NAME_HEADER );
            }
            

            Object bObj = exchange.getIn().getBody();
            if(!bObj.getClass().getName().equals(String.class.getName()))
              throw new ValidationException("000004 Payload not of type String : "+bObj.getClass().getName());

        }
    }
}
