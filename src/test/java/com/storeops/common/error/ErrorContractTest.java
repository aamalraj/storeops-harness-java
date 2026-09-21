package com.storeops.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.storeops.support.ActorHeaders;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Asserts that every failure — domain, framework or unexpected — comes back in the single
 * {@link ErrorResponse} shape with a code from the {@link AppError} hierarchy.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ErrorContractTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @DisplayName("A request with no identity headers is 401 UNAUTHORIZED")
  void missingIdentityIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/activities"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.path").value("/api/activities"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  @DisplayName("A request missing only the store header is 401 UNAUTHORIZED")
  void missingStoreHeaderIsUnauthorized() throws Exception {
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-User-Id", ActorHeaders.ASSOCIATE);

    mockMvc.perform(get("/api/activities").headers(headers))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("An unknown role is 401 UNAUTHORIZED")
  void unknownRoleIsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/activities")
            .headers(ActorHeaders.of(ActorHeaders.ASSOCIATE, "REGIONAL_OVERLORD")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  @DisplayName("Malformed JSON is 400 VALIDATION_FAILED, not 500")
  void malformedJsonIsBadRequest() throws Exception {
    mockMvc.perform(post("/api/activities")
            .headers(ActorHeaders.associate())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"title\": "))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("An unmapped path is 404 NOT_FOUND, not 500")
  void unmappedPathIsNotFound() throws Exception {
    mockMvc.perform(get("/api/nope").headers(ActorHeaders.associate()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"));
  }

  @Test
  @DisplayName("An unsupported verb on a known path is 405 METHOD_NOT_ALLOWED")
  void wrongVerbIsMethodNotAllowed() throws Exception {
    mockMvc.perform(put("/api/activities").headers(ActorHeaders.associate()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  @DisplayName("The error body never carries a stack trace")
  void errorBodyHasNoStackTrace() throws Exception {
    mockMvc.perform(get("/api/activities/unknown-id").headers(ActorHeaders.associate()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.trace").doesNotExist())
        .andExpect(jsonPath("$.exception").doesNotExist())
        .andExpect(jsonPath("$.details").isArray());
  }
}
