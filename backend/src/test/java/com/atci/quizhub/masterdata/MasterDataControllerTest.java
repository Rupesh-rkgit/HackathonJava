package com.atci.quizhub.masterdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MasterDataControllerTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void listsStacks() throws Exception {
        mvc.perform(get("/api/masterdata/stacks"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/masterdata/stacks"))
           .andExpect(status().isForbidden());
    }
}
