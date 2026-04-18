package com.telas.services;

import java.io.Serial;
import java.io.Serializable;

public record BoxTailscalePingOutcome(
        boolean attempted,
        boolean reachable,
        String detail)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static BoxTailscalePingOutcome notAttempted(String detail) {
        return new BoxTailscalePingOutcome(false, false, detail);
    }

    public static BoxTailscalePingOutcome attempted(boolean reachable, String detail) {
        return new BoxTailscalePingOutcome(true, reachable, detail);
    }
}
