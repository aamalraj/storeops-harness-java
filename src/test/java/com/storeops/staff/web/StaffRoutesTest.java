package com.storeops.staff.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeops.support.ActorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Route tests for the read-only staff module. */
@SpringBootTest
@AutoConfigureMockMvc
class StaffRoutesTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET /api/staff lists the authenticated store's staff")
  void listsStaffForStore() throws Exception {
    mockMvc.perform(get("/api/staff").headers(ActorHeaders.associate()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].fullName").value("Ada Okafor"))
        .andExpect(jsonPath("$[0].role").value("STORE_MANAGER"));
  }

  @Test
  @DisplayName("GET /api/staff refuses another store to a non-manager with 403")
  void associateMayNotReadAnotherStore() throws Exception {
    mockMvc.perform(get("/api/staff")
            .headers(ActorHeaders.associate())
            .param("storeId", ActorHeaders.OTHER_STORE))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("The staff module exposes no write endpoint")
  void offersNoWriteEndpoint() throws Exception {
    mockMvc.perform(post("/api/staff").headers(ActorHeaders.manager()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }
}
