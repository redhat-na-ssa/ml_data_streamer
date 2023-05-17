package com.redhat.na.gtm.ml;

import org.eclipse.microprofile.metrics.annotation.Counted;

public class ValidationException extends Exception {

        private static final long serialVersionUID = 1L;

        public ValidationException() {
            super();
        }

        @Counted(name = "dirtyCSVDataCount", description = "How many incidents of dirty CSV data.")
        public ValidationException(String message){
            super(message);
        }

        public ValidationException(String message, Throwable cause){
            super(message, cause);
        }
    
}
