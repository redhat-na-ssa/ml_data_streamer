package com.redhat.na.gtm;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

import org.apache.camel.quarkus.main.CamelMainApplication;
import org.jboss.logging.Logger;

@QuarkusMain
public class Main {

    private static Logger log = Logger.getLogger(Main.class);
    public static void main(String... args) {
        Quarkus.run(CamelMainApplication.class);
    }
     
}

