package com.telas.services;

import java.util.Optional;

public interface SideApiHealthCheckService {
    SideApiHealthOutcome check(String boxIp);

    record SideApiHealthOutcome(boolean up, String detail, Integer httpStatus) {
        public static SideApiHealthOutcome up(Integer httpStatus) {
            return new SideApiHealthOutcome(true, "UP", httpStatus);
        }

        public static SideApiHealthOutcome down(String detail, Integer httpStatus) {
            String d = Optional.ofNullable(detail).orElse("DOWN");
            return new SideApiHealthOutcome(false, d, httpStatus);
        }
    }
}

