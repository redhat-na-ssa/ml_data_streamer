package com.redhat.na.gtm.ml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;

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
    
    public Routes() {
    }

    @Override
    public void configure() throws Exception {

        restConfiguration().bindingMode(RestBindingMode.json);

        /*****                Consume from HTTP           *************/
        rest("/sanityCheck")
                .get()
                .to("direct:sanity");

        // curl -v -X POST -F "data=@src/test/himss/nogood/AM3X-365115-1002-1-2021285.txt" localhost:8180/gzippedFiles
        // curl -v -X POST -F "data=@src/test/himss/nogood/AM3X-365115-2021285.tgz" localhost:8180/gzippedFiles
        // curl -v -X POST -F "data=@src/test/himss/good/DETM-NKI7I92LX7P-5221-6-20000907.tgz" localhost:8180/gzippedFiles
        // curl -v -X POST -F "data=@src/test/himss/good/AM3X-343411-2247-2-20000502.tgz" localhost:8180/gzippedFiles
        // curl -v -X POST -F "data=@src/test/himss/good/DETM-4Z40Y7E029Q-2125-6-19920525.tgz" -F "data=@src/test/himss/good/AM3X-034540-6636-2-19710723.tgz" localhost:8180/gzippedFiles
        rest("/gzippedFiles")
          .description("Consume Gzipped Files")
          .consumes("multipart/form-data")
          .post()
          .to("direct:verifyPayload");

        from("direct:sanity")
            .setBody().constant("Good To Go!");

        
        from("direct:verifyPayload")
          .routeId("verifyPayload")
          .setHeader(Util.RESPONSE_HEADER, constant("ALL FILES PERSISTED"))
          .doTry()
                .process(new GzipPayloadValidator())
                .process(e -> {
                    Map<String, DataHandler> attachments = e.getIn(AttachmentMessage.class).getAttachments();
                    Collection<byte[]> aList = new ArrayList<byte[]>();
                    for(Entry<String, DataHandler> attachment  : attachments.entrySet()) {
                        byte[] bytes = attachment.getValue().getInputStream().readAllBytes();
                        aList.add(bytes);
                    }
                    e.getIn().setBody(aList);
                })
                .to("seda:writeVerifiedPayloadToFilesystem")
            
          .doCatch(ValidationException.class)
                .setHeader("CamelHttpResponseCode").constant("415")
                .setHeader(Util.RESPONSE_HEADER, exceptionMessage())
                .log(LoggingLevel.ERROR, exceptionMessage().toString())
          .doFinally()
                .setBody().header(Util.RESPONSE_HEADER)
          .end();
            
        from("seda:writeVerifiedPayloadToFilesystem")
            .routeId("direct:writeVerifiedPayloadToFilesystem")
            .split(body())
            .to("file:{{himss.scm.gzip.location}}");




    
        /*****                Consume from filesystem           *************/
        from("file:{{himss.scm.gzip.location}}?initialDelay=0&delay=1000&autoCreate=true&delete=false")
            .routeId("direct:unpackGzip")
                //.unmarshal()
                //.log("file = ${header.CamelFileName}}")
                .split(body())
                  .streaming()
                  .process(e -> {
                      String originalFileName = (String)e.getIn().getHeader("CamelFileName");
                      int lastDirIndex = originalFileName.lastIndexOf("/");
                      String parsedFileName = originalFileName.substring(lastDirIndex+1);
                      log.debug("prepKafkaProducer()  originalFileName = "+originalFileName+" : parsedFileName = "+parsedFileName);
                      e.getIn().setHeader(Util.FILE_NAME_HEADER, parsedFileName);
                  })
                  //.to("direct:processTextFile")
                  .to("kafka:{{himss.scm_topic_name}}?brokers={{kafka.bootstrap.servers}}")
                .end();

        from("direct:processTextFile")
            .routeId("direct:processTextFile")
            .log("file = ${header.CamelFileName}}")
            .end();

    }

    class GzipPayloadValidator implements Processor {

        @Override
        public void process(Exchange exchange) throws ValidationException {

            AttachmentMessage attMsg = exchange.getIn(AttachmentMessage.class);
            Set<String> attachmentNames = attMsg.getAttachmentNames();
            for(String aName : attachmentNames) {
                log.info("GzipPayloadValidator.process()   fileName = "+aName);
                if(!aName.endsWith("tgz") && !aName.endsWith("tar.gz"))
                  throw new ValidationException("000001 Invalid file suffix: "+aName);
            }
        }
    }

}
