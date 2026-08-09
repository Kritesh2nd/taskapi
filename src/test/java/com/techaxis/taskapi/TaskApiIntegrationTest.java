package com.techaxis.taskapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techaxis.taskapi.dto.TaskRequest;
import com.techaxis.taskapi.dto.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createUser_thenCreateTask_thenFetchIt() throws Exception {
        UserRequest userRequest = new UserRequest("Ada Lovelace", "ada@example.com");

        String userJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(userJson).get("id").asLong();

        TaskRequest taskRequest = new TaskRequest(
                "Write the first algorithm", "Analytical engine notes", null, null, userId);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(taskRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.ownerName").value("Ada Lovelace"));

        mockMvc.perform(get("/api/v1/tasks").param("ownerId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void createUser_withDuplicateEmail_returns409() throws Exception {
        UserRequest userRequest = new UserRequest("Grace Hopper", "grace@example.com");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void createUser_withInvalidEmail_returns400() throws Exception {
        UserRequest userRequest = new UserRequest("Bad Email", "not-an-email");

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void getTask_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/999999"))
                .andExpect(status().isNotFound());
    }
}
