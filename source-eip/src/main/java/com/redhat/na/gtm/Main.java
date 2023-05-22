package com.redhat.na.gtm;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

import org.apache.camel.quarkus.main.CamelMainApplication;
import org.jboss.logging.Logger;

import com.redhat.na.gtm.ml.Util;

@QuarkusMain
public class Main {

    private static Logger log = Logger.getLogger(Main.class);
    public static void main(String... args) {
        if(!propOrEnvVarExists(Util.CSV_DIR_PROP_NAME)){
            System.setProperty(Util.CSV_DIR_PROP_NAME, Util.DEFAULT_CSV_DIR);
        }
        if(!propOrEnvVarExists(Util.TOPIC_PROP_NAME)){
            System.setProperty(Util.TOPIC_PROP_NAME, Util.DEFAULT_TOPIC_NAME);
        }
        Quarkus.run(CamelMainApplication.class);
    }

    private static boolean propOrEnvVarExists(String name) {
        if((System.getProperty(name) == null) && (System.getenv(name) == null))
            return false;
        else
            return true;
    }
     
}

