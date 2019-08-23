
package com.alivex.connector.rabbitmq.exception;

public class RejectAndDontRequeueException extends RuntimeException {

    public RejectAndDontRequeueException(String message) {
        super(message);
    }

    public RejectAndDontRequeueException(Throwable cause) {
        super(cause);
    }
}
