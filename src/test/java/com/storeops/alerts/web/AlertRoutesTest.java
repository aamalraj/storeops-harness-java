package com.storeops.alerts.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeops.support.ActorHeaders;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Route tests for GET /api/alerts.
 *
 * <p>These double as the integration test for the event bus: nothing here calls the alerts module,
 * so an alert can only appear because the activities module published a {@code DomainEvent} that
 * the alerts subscriber picked up.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AlertRoutesTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("Creating an activity raises an alert for the caller, delivered via the event bus")
  void activityCreationRaisesAlert() throws Exception {
    HttpHeaders caller = freshCaller();
    String activityId = createActivity(caller, null);

    mockMvc.perform(get("/api/alerts").headers(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].sourceEventType").value("ACTIVITY_CREATED"))
        .andExpect(jsonPath("$[0].sourceAggregateId").value(activityId))
        .andExpect(jsonPath("$[0].storeId").value(ActorHeaders.STORE))
        .andExpect(jsonPath("$[0].severity").value("INFO"))
        .andExpect(jsonPath("$[0].acknowledged").value(false));
  }

  @Test
  @DisplayName("A critical activity raises a WARNING alert")
  void criticalActivityRaisesWarning() throws Exception {
    HttpHeaders caller = freshCaller();

    mockMvc.perform(post("/api/activities")
            .headers(caller)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Freezer failure\",\"category\":\"MAINTENANCE\","
                + "\"priority\":\"CRITICAL\"}"))
        .andExpect(status().isCreated());

    mockMvc.perform(get("/api/alerts").headers(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].severity").value("WARNING"));
  }

  @Test
  @DisplayName("An alert is routed to the assignee rather than the caller when one is named")
  void alertGoesToAssignee() throws Exception {
    HttpHeaders caller = freshCaller();
    String activityId = createActivity(caller, ActorHeaders.SUPERVISOR);

    mockMvc.perform(get("/api/alerts").headers(caller))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sourceAggregateId == '" + activityId + "')]").isEmpty());

    mockMvc.perform(get("/api/alerts")
            .headers(ActorHeaders.of(ActorHeaders.SUPERVISOR, "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sourceAggregateId == '" + activityId + "')]").isNotEmpty());
  }

  @Test
  @DisplayName("Alerts are private to their recipient")
  void alertsArePrivate() throws Exception {
    HttpHeaders caller = freshCaller();
    String activityId = createActivity(caller, null);

    mockMvc.perform(get("/api/alerts").headers(freshCaller()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sourceAggregateId == '" + activityId + "')]").isEmpty());
  }

  @Test
  @DisplayName("GET /api/alerts can be narrowed to unacknowledged alerts")
  void filtersUnacknowledged() throws Exception {
    HttpHeaders caller = freshCaller();
    createActivity(caller, null);

    mockMvc.perform(get("/api/alerts").headers(caller).param("unacknowledgedOnly", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  @DisplayName("GET /api/alerts requires an authenticated caller")
  void requiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/alerts"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  /** A caller with a userId no other test uses, so their alert list is theirs alone. */
  private static HttpHeaders freshCaller() {
    return ActorHeaders.of("watcher-" + UUID.randomUUID(), "SUPERVISOR");
  }

  private String createActivity(HttpHeaders caller, String assigneeId) throws Exception {
    java.util.Map<String, String> body = new java.util.LinkedHashMap<>();
    body.put("title", "Check aisle end");
    body.put("category", "REPLENISHMENT");
    if (assigneeId != null) {
      body.put("assigneeId", assigneeId);
    }
    String response = mockMvc.perform(post("/api/activities")
            .headers(caller)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }
}
