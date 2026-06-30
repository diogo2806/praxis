package br.com.iforce.praxis.gupy.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.jdbc.Sql;

import org.springframework.test.web.servlet.MockMvc;


import static org.hamcrest.Matchers.hasItem;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Mesmo que uma simulação tenha mais de uma versão PUBLISHED no banco, o catálogo Gupy deve
 * retornar a simulação uma única vez (a versão publicada mais recente).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Sql(scripts = {"/seed-simulation-fixture.sql", "/duplicate-published-version-fixtures.sql"})
class PublishedCatalogDedupTest {

    private static final String AUTHORIZATION = "Bearer empresa1-token";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void catalogReturnsEachSimulationOnceDespiteMultiplePublishedVersions() throws Exception {
        mockMvc.perform(get("/test").header("Authorization", AUTHORIZATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_tests").value(2))
                .andExpect(jsonPath("$.payload[*].id").value(hasItem("sim-atendimento-caos")));
    }
}
