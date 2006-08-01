package com.limegroup.mojito.event.exceptions;

public class NotBootstrappedException extends RuntimeException{
    
    public NotBootstrappedException(String failedOperation) {
        super("Attempting to execute a "+failedOperation+" while not bootstrapped to the network");
    }
}
