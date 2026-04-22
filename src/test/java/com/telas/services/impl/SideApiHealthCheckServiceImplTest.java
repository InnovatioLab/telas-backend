package com.telas.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.services.SideApiHealthCheckService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SideApiHealthCheckServiceImplTest {

    @Test
    void returnsDownWhenMissingIp() {
        SideApiHealthCheckServiceImpl svc = new SideApiHealthCheckServiceImpl(new ObjectMapper());
        try {
            var f = SideApiHealthCheckServiceImpl.class.getDeclaredField("enabled");
            f.setAccessible(true);
            f.setBoolean(svc, true);
        } catch (Exception e) {
            fail(e);
        }
        SideApiHealthCheckService.SideApiHealthOutcome out = svc.check(null);
        assertFalse(out.up());
        assertEquals("missing_ip", out.detail());
    }

    @Test
    void parsesUpStatusJson() {
        ObjectMapper om = new ObjectMapper();
        assertTrue(SideApiHealthCheckServiceImpl.isUpJson(om, "{\"status\":\"UP\"}"));
        assertTrue(SideApiHealthCheckServiceImpl.isUpJson(om, "{\"status\":\"up\"}"));
        assertFalse(SideApiHealthCheckServiceImpl.isUpJson(om, "{\"status\":\"DOWN\"}"));
        assertFalse(SideApiHealthCheckServiceImpl.isUpJson(om, "{\"ok\":true}"));
        assertFalse(SideApiHealthCheckServiceImpl.isUpJson(om, "not-json"));
    }
}

