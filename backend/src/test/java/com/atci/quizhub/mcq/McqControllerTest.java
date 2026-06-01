package com.atci.quizhub.mcq;

import com.atci.quizhub.masterdata.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class McqControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    private Map<String,Object> body(String stem) {
        Map<String,Object> b = new HashMap<>();
        b.put("questionStem", stem);
        b.put("optionA","a"); b.put("optionB","b"); b.put("optionC","c"); b.put("optionD","d");
        b.put("correctAnswer","A"); b.put("difficulty","EASY");
        b.put("stackId", stackId); b.put("topicId", topicId); b.put("mode","SAVE");
        return b;
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void createDraftThenListMine() throws Exception {
        mvc.perform(post("/api/mcqs").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body("What is DI?"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(get("/api/mcqs/mine"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void validationFailsForBlankStem() throws Exception {
        mvc.perform(post("/api/mcqs").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body(""))))
           .andExpect(status().isBadRequest());
    }
}
