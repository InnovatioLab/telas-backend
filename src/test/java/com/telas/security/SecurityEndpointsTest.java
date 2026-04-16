package com.telas.security;

import com.telas.controllers.impl.ClientControllerImpl;
import com.telas.infra.config.CorsConfig;
import com.telas.infra.config.SecurityConfig;
import com.telas.infra.security.controllers.impl.AuthControllerImpl;
import com.telas.services.ClientService;
import com.telas.infra.security.services.AuthService;
import com.telas.services.ApplicationLogService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ClientControllerImpl.class, AuthControllerImpl.class})
@Import({SecurityConfig.class, CorsConfig.class, TestSecuritySupportConfig.class})
@ContextConfiguration(classes = SecurityTestApplication.class)
@TestPropertySource(properties = {
    "server.servlet.context-path=/api",
    "front.base.url=http://localhost:4200",
    "cors.allowed-origins=http://localhost:4200",
    "rate.limit.enabled=false",
    "api.security.token.secret=test-secret",
    "monitoring.api.key=test-monitoring-key"
})
class SecurityEndpointsTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ClientService clientService;
  @MockBean private AuthService authService;
  @MockBean private ApplicationLogService applicationLogService;

  @Test
  void publicEndpoints_shouldNotRequireAuthentication() throws Exception {
    mockMvc
        .perform(post("/api/clients").contentType("application/json").content("{}"))
        .andExpect(status().is4xxClientError());

    mockMvc
        .perform(post("/api/auth/login").contentType("application/json").content("{}"))
        .andExpect(status().is4xxClientError());
  }

  @Test
  void protectedEndpoints_shouldRequireAuthentication() throws Exception {
    mockMvc
        .perform(get("/api/clients/authenticated"))
        .andExpect(status().is4xxClientError());
  }
}

