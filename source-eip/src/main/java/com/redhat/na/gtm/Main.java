package com.redhat.na.gtm;

import io.quarkus.runtime.Quarkus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import picocli.CommandLine;

import org.jboss.logging.Logger;

@CommandLine.Command(mixinStandardHelpOptions = true)
public class Main implements Runnable {

    private static Logger log = Logger.getLogger(Main.class);

    @CommandLine.Option(names = {"-b", "--kafkaBootstrapServer"}, description = "Kafka Bootstreap Server", defaultValue = "rht:9092")
    String kafkaBootstrapServer;

    @CommandLine.Option(names = {"-d", "--csvSourceDir"}, description = "CSV Source Dir", defaultValue = "/tmp/com_rht_na_gtm_source")
    String csvSourceDir;

    @CommandLine.Option(names = {"-t", "--kafkaTopic"}, description = "Kafka Topic Name", defaultValue = "csv-financials")
    String csvKafkaTopic;

    @Override
    public void run() {
        log.infov("csvSourcedir = {0}, csvKafkaName =  {1}, kafkaBootstrapServer = {2}", csvSourceDir, csvKafkaTopic, kafkaBootstrapServer);
        Quarkus.waitForExit();
    }
    
    /*
     * TO-DO:  Figure out how to capture command line arguments first and use values to populate properties of the Camel Route.
     @Produces
     @ApplicationScoped
     AppRoutes cRoutes(CommandLine.ParseResult parseResult){
         log.info("cRoutes() now setting system properties based on command-line args");
         //System.setProperty("kafka.bootstrap.servers", kafkaBootstrapServer);
         //System.setProperty("com.rht.na.gtm.source.location", csvSourceDir);
         //System.setProperty("com.rht.na.gtm.topic.name", csvKafkaTopic);
         AppRoutes route = new AppRoutes();
         return route;
     }
     */
     
}

