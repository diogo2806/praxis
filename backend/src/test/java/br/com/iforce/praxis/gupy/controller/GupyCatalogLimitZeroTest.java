package br.com.iforce.praxis.gupy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = "/seed-simulation-fixture.sql")
class GupyCatalogLimitZeroTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsEmptyPageWithoutChangingOfficialLimitZero() throws Exception {
        mockMvc.perform(get("/test")
                        .header("Authorization", "Bearer empresa1-token")
                        .param("offset", "0")
                        .param("limit", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.limit").value(0))
                .andExpect(jsonPath("$.offset").value(0))
                .andExpect(jsonPath("$.total_tests").value(2))
                .andExpect(jsonPath("$.payload").isEmpty());
    }
}
