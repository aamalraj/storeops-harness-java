package com.storeops.activities.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storeops.support.ActorHeaders;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end route tests for the five activity endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class ActivityRoutesTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /api/activities creates an activity owned by the caller")
  void createsActivity() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Reset end cap\",\"category\":\"MERCHANDISING\","
                + "\"priority\":\"HIGH\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.title").value("Reset end cap"))
        .andExpect(jsonPath("$.status").value("PLANNED"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.storeId").value(ActorHeaders.STORE))
        .andExpect(jsonPath("$.ownerId").value(ActorHeaders.ASSOCIATE));
  }

  @Test
  @DisplayName("POST /api/activities rejects a blank title with 400 and field details")
  void rejectsBlankTitle() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"  \",\"category\":\"MERCHANDISING\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[0]").value("title: must not be blank"));
  }

  @Test
  @DisplayName("POST /api/activities rejects an unknown category with 400 naming the options")
  void rejectsUnknownCategory() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Reset end cap\",\"category\":\"KNITTING\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value(containsString("MERCHANDISING")));
  }

  @Test
  @DisplayName("POST /api/activities rejects an assignee from another store")
  void rejectsForeignAssignee() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\":\"Reset end cap\",\"category\":\"MERCHANDISING\","
                + "\"assigneeId\":\"" + ActorHeaders.OTHER_STORE_ASSOCIATE + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("GET /api/activities/{id} returns the activity, and 404 for an unknown id")
  void readsActivityById() throws Exception {
    String id = createActivity("Face up aisle 4", null);

    mockMvc.perform(get("/api/activities/" + id).headers(ActorHeaders.associate()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.title").value("Face up aisle 4"));

    mockMvc.perform(get("/api/activities/does-not-exist").headers(ActorHeaders.associate()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @DisplayName("GET /api/activities/{id} hides activities belonging to another store")
  void doesNotLeakAcrossStores() throws Exception {
    String id = createActivity("Store one only", null);

    mockMvc.perform(get("/api/activities/" + id)
            .headers(ActorHeaders.of(
                ActorHeaders.OTHER_STORE_ASSOCIATE, ActorHeaders.OTHER_STORE, "ASSOCIATE")))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/activities filters by programme and status")
  void filtersList() throws Exception {
    String programme = "programme-" + UUID.randomUUID();
    String first = createActivity("Count backroom", programme);
    createActivity("Count shop floor", programme);

    mockMvc.perform(get("/api/activities")
            .headers(ActorHeaders.associate())
            .param("programme", programme))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    mockMvc.perform(patch("/api/activities/" + first)
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/api/activities")
            .headers(ActorHeaders.associate())
            .param("programme", programme)
            .param("status", "IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(first));
  }

  @Test
  @DisplayName("GET /api/activities rejects an unknown status filter with 400")
  void rejectsUnknownStatusFilter() throws Exception {
    mockMvc.perform(get("/api/activities")
            .headers(ActorHeaders.associate())
            .param("status", "SOMEDAY"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("PATCH /api/activities/{id} updates the supplied fields only")
  void patchesActivity() throws Exception {
    String id = createActivity("Check chiller temperatures", null);

    mockMvc.perform(patch("/api/activities/" + id)
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"BLOCKED\",\"priority\":\"CRITICAL\","
                + "\"assigneeId\":\"" + ActorHeaders.SUPERVISOR + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOCKED"))
        .andExpect(jsonPath("$.priority").value("CRITICAL"))
        .andExpect(jsonPath("$.assigneeId").value(ActorHeaders.SUPERVISOR))
        .andExpect(jsonPath("$.category").value("MERCHANDISING"));
  }

  @Test
  @DisplayName("PATCH /api/activities/{id} rejects an empty payload with 400")
  void rejectsEmptyPatch() throws Exception {
    String id = createActivity("Rotate stock", null);

    mockMvc.perform(patch("/api/activities/" + id)
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("PATCH /api/activities/{id} refuses to modify a closed activity with 409")
  void rejectsPatchOfClosedActivity() throws Exception {
    String id = createActivity("Archive planogram", null);

    mockMvc.perform(patch("/api/activities/" + id)
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"DONE\"}"))
        .andExpect(status().isOk());

    mockMvc.perform(patch("/api/activities/" + id)
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"priority\":\"LOW\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  @DisplayName("DELETE /api/activities/{id} allows the owner")
  void ownerMayDelete() throws Exception {
    String id = createActivity("Tidy queue line", null);

    mockMvc.perform(delete("/api/activities/" + id).headers(ActorHeaders.associate()))
        .andExpect(status().isNoContent());

    mockMvc.perform(get("/api/activities/" + id).headers(ActorHeaders.associate()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("DELETE /api/activities/{id} allows a store manager who does not own it")
  void managerMayDelete() throws Exception {
    String id = createActivity("Escalate spillage", null);

    mockMvc.perform(delete("/api/activities/" + id).headers(ActorHeaders.manager()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /api/activities/{id} refuses a non-owner who is not a manager with 403")
  void otherAssociateMayNotDelete() throws Exception {
    String id = createActivity("Restock bakery", null);

    mockMvc.perform(delete("/api/activities/" + id)
            .headers(ActorHeaders.of(ActorHeaders.SUPERVISOR, "SUPERVISOR")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  /** Creates an activity as the default associate and returns its id. */
  private String createActivity(String title, String programmeId) throws Exception {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("title", title);
    body.put("category", "MERCHANDISING");
    if (programmeId != null) {
      body.put("programmeId", programmeId);
    }
    String response = mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }
}
