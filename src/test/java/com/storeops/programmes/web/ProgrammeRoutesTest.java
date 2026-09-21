package com.storeops.programmes.web;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End-to-end route tests for the three programme endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
class ProgrammeRoutesTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /api/programmes creates a DRAFT programme owned by the caller")
  void createsProgramme() throws Exception {
    mockMvc.perform(post("/api/programmes")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Winter reset\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.name").value("Winter reset"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.ownerId").value(ActorHeaders.MANAGER))
        .andExpect(jsonPath("$.storeId").value(ActorHeaders.STORE))
        .andExpect(jsonPath("$.members.length()").value(0));
  }

  @Test
  @DisplayName("POST /api/programmes rejects a blank name with 400")
  void rejectsBlankName() throws Exception {
    mockMvc.perform(post("/api/programmes")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.details[0]").value("name: must not be blank"));
  }

  @Test
  @DisplayName("POST /api/programmes refuses to create an ARCHIVED programme with 400")
  void rejectsArchivedOnCreate() throws Exception {
    mockMvc.perform(post("/api/programmes")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"Already over\",\"status\":\"ARCHIVED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("GET /api/programmes lists only the authenticated store's programmes")
  void listsForAuthenticatedStoreOnly() throws Exception {
    String name = "Scoped " + UUID.randomUUID();
    createProgramme(name);

    mockMvc.perform(get("/api/programmes").headers(ActorHeaders.manager()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == '" + name + "')]").isNotEmpty());

    mockMvc.perform(get("/api/programmes")
            .headers(ActorHeaders.of(
                ActorHeaders.OTHER_STORE_ASSOCIATE, ActorHeaders.OTHER_STORE, "ASSOCIATE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == '" + name + "')]").isEmpty());
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members adds a staff member")
  void addsMember() throws Exception {
    String id = createProgramme("Membership " + UUID.randomUUID());

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"staffId\":\"" + ActorHeaders.ASSOCIATE + "\",\"role\":\"LEAD\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.members.length()").value(1))
        .andExpect(jsonPath("$.members[0].staffId").value(ActorHeaders.ASSOCIATE))
        .andExpect(jsonPath("$.members[0].role").value("LEAD"));
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members defaults the role to CONTRIBUTOR")
  void defaultsMemberRole() throws Exception {
    String id = createProgramme("Default role " + UUID.randomUUID());

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"staffId\":\"" + ActorHeaders.SUPERVISOR + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.members[0].role").value("CONTRIBUTOR"));
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members rejects a duplicate with 409")
  void rejectsDuplicateMember() throws Exception {
    String id = createProgramme("Duplicate " + UUID.randomUUID());
    String body = "{\"staffId\":\"" + ActorHeaders.ASSOCIATE + "\"}";

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members rejects staff from another store with 400")
  void rejectsForeignStaff() throws Exception {
    String id = createProgramme("Foreign staff " + UUID.randomUUID());

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"staffId\":\"" + ActorHeaders.OTHER_STORE_ASSOCIATE + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members refuses a non-owner associate with 403")
  void rejectsNonOwner() throws Exception {
    String id = createProgramme("Owner only " + UUID.randomUUID());

    mockMvc.perform(post("/api/programmes/" + id + "/members")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"staffId\":\"" + ActorHeaders.SUPERVISOR + "\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  @DisplayName("POST /api/programmes/{id}/members returns 404 for an unknown programme")
  void rejectsUnknownProgramme() throws Exception {
    mockMvc.perform(post("/api/programmes/does-not-exist/members")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"staffId\":\"" + ActorHeaders.SUPERVISOR + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  /** Creates a programme as the store manager and returns its id. */
  private String createProgramme(String name) throws Exception {
    String response = mockMvc.perform(post("/api/programmes")
            .headers(ActorHeaders.manager())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of("name", name))))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return objectMapper.readTree(response).get("id").asText();
  }
}
