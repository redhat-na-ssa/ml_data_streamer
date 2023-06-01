package com.redhat.na.gtm.ml;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.ApplicationScoped;
import picocli.CommandLine;

import org.jboss.logging.Logger;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.FileConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AppRoutes extends RouteBuilder {

    private static Logger log = Logger.getLogger(AppRoutes.class);
    private static DateFormat dfObj = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    @ConfigProperty(name="com.redhat.na.gtm.ml.csv_contains_header", defaultValue = "True")
    boolean csvContainsHeader=true;

    @ConfigProperty(name="com.redhat.na.gtm.ml.aggregator_max_expected_files", defaultValue = "5")
    int maxExpectedFiles;

    @ConfigProperty(name="com.redhat.na.gtm.ml.aggregator_completion_timeout_millis", defaultValue = "2000")
    int completionTimeoutMillis;

    CommandLine.ParseResult parseResult;

    public AppRoutes() {
    }

    @Override
    public void configure() throws Exception {


        //restConfiguration().bindingMode(RestBindingMode.json);

        /*****                Consume from HTTP           *************/
        rest("/sanityCheck")
            .get()
            .to("direct:sanity");

        from("direct:sanity")
            .routeId("direct:sanity")
            .setBody().constant("Good To Go!");

    
        /*****                Consume from filesystem           *************/
        from("file:{{com.rht.na.gtm.source.location}}?initialDelay=0&delay=1000&autoCreate=true&delete=false")
            .routeId("direct:readCSVs")
                .log(LoggingLevel.DEBUG, "file = ${header.CamelFileName}}")
                .aggregate(header(FileConstants.FILE_PARENT), new SimpleCSVAggregator(csvContainsHeader))
                .completionSize(maxExpectedFiles)
                .completionTimeout(completionTimeoutMillis)
                .process(new PostCSVAggregatorProcessor())
                //.to("direct:processTextFile")
                .to("kafka:{{com.rht.na.gtm.topic.name}}?brokers={{kafka.bootstrap.servers}}&clientId=source&groupId=rht")
                .end();

        from("direct:processTextFile")
            .routeId("direct:processTextFile")
            .process(e -> {
                log.info("processToFile() CamelFileName = "+e.getIn().getHeader(FileConstants.FILE_NAME));
            })
            .to("file:/tmp/testOutput")
            .end();

    }

    class PostCSVAggregatorProcessor implements Processor {

        @Override
        public void process(Exchange e) throws ValidationException {
            List<String> csvSet = (List<String>)e.getIn().getBody(List.class);
            e.getIn().setHeader(Util.CONCANTENATED_FILE_NUM, csvSet.size());

            // Set Exchange body from Set of CSVs to a single file
            StringBuilder sBuilder = new StringBuilder();
            for(String csv : csvSet){
                sBuilder.append(csv);
            }
            e.getIn().setBody(sBuilder.toString());

            log.infov("readCSVs() # CSVs = {0} ; Total records = {1}", csvSet.size(), newLineCounter(sBuilder.toString()));

            // Set a file name for aggregated CSV
            String originalFileName = (String)e.getIn().getHeader("CamelFileName");
            int lastDirIndex = originalFileName.lastIndexOf("/");
            String parsedFileName = dfObj.format(new Date())+"-"+originalFileName.substring(lastDirIndex+1);
            log.debug("prepKafkaProducer()  originalFileName = "+originalFileName+" : parsedFileName = "+parsedFileName);
            e.getIn().setHeader(Util.FILE_NAME_HEADER, parsedFileName);
        }

        public int newLineCounter(String input) {
            Matcher m = Pattern.compile("\r\n|\r|\n").matcher(input);
            int lines = 1;
            while (m.find()){
                lines ++;
            }
            return lines;
        }
    }
}
