package com.storeops.common.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Confirms the OpenAPI schema and Swagger UI are actually wired up and reachable. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("GET /v3/api-docs describes the API and the three auth headers, and hides Actor")
  void servesOpenApiSchema() throws Exception {
    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("StoreOps API"))
        .andExpect(jsonPath("$.components.securitySchemes.X-User-Id.in").value("header"))
        .andExpect(jsonPath("$.components.securitySchemes.X-Store-Id.in").value("header"))
        .andExpect(jsonPath("$.components.securitySchemes.X-User-Role.in").value("header"))
        .andExpect(jsonPath("$.paths./api/activities.get").exists())
        .andExpect(jsonPath("$.paths./api/activities.get.parameters[*].name")
            .value(org.hamcrest.Matchers.not(hasItem("actor"))));
  }

  @Test
  @DisplayName("GET /swagger-ui/index.html serves the Swagger UI page")
  void servesSwaggerUiPage() throws Exception {
    mockMvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
