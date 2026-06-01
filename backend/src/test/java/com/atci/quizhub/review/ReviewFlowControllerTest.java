package com.atci.quizhub.review;

import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewFlowControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired McqService mcqService;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId; Long mcqId;

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
        mcqId = mcqService.create(new McqRequest("Q?","a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND),
            "gaurav.a.bhola").id();
    }

    @Test
    @WithMockUser(username = "birendra.kumar.singh", roles = {"ADMIN"})
    void adminCanListBankAndAssignReviewer() throws Exception {
        mvc.perform(get("/api/admin/mcqs"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray());

        mvc.perform(get("/api/admin/mcqs/" + mcqId + "/eligible-reviewers"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].enterpriseId").exists());

        mvc.perform(post("/api/admin/mcqs/" + mcqId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("reviewerEnterpriseId","divya.madhanasekar"))))
           .andExpect(status().isOk());

        assertEquals(McqStatus.UNDER_REVIEW, mcqService.getEntity(mcqId).getStatus());
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void smeCannotAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/mcqs"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "divya.madhanasekar", roles = {"SME"})
    void blankRejectionCommentIsBadRequest() throws Exception {
        mvc.perform(post("/api/reviews/" + mcqId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("comments",""))))
           .andExpect(status().isBadRequest());
    }
}
