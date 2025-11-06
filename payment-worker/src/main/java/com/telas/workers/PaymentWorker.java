package com.telas.workers;

public interface PaymentWorker {
    void processEvent(String eventJson);
}
