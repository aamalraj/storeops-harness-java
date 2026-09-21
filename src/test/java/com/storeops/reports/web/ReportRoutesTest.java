package com.storeops.reports.web;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Route tests for the report endpoints.
 *
 * <p>Also covers the read-only query ports: the figures here can only be non-zero if reports
 * successfully read the activities and programmes modules through their {@code api} packages.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReportRoutesTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET /api/reports/store aggregates activity and programme counts")
  void reportsStoreMetrics() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Metrics fixture\",\"category\":\"COMPLIANCE\"}"))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/reports/store").headers(ActorHeaders.manager()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(ActorHeaders.STORE))
        .andExpect(jsonPath("$.activitiesByStatus.PLANNED").exists())
        .andExpect(jsonPath("$.openActivities").exists())
        .andExpect(jsonPath("$.totalProgrammes").exists())
        .andExpect(jsonPath("$.generatedAt").exists());
  }

  @Test
  @DisplayName("GET /api/reports/store refuses another store to a non-manager with 403")
  void associateMayNotReadAnotherStore() throws Exception {
    mockMvc.perform(get("/api/reports/store")
            .headers(ActorHeaders.associate())
            .param("storeId", ActorHeaders.OTHER_STORE))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("GET /api/reports/region rolls up the region for a manager")
  void reportsRegionMetrics() throws Exception {
    mockMvc.perform(get("/api/reports/region").headers(ActorHeaders.manager()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.regionId").value(ActorHeaders.REGION))
        .andExpect(jsonPath("$.stores.length()").value(2))
        .andExpect(jsonPath("$.openActivities").exists());
  }

  @Test
  @DisplayName("GET /api/reports/region refuses a non-manager with 403")
  void associateMayNotReadRegion() throws Exception {
    mockMvc.perform(get("/api/reports/region").headers(ActorHeaders.associate()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("GET /api/reports/region returns 404 for an unknown region")
  void unknownRegionIsNotFound() throws Exception {
    mockMvc.perform(get("/api/reports/region")
            .headers(ActorHeaders.manager())
            .param("regionId", "region-atlantis"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }
}
