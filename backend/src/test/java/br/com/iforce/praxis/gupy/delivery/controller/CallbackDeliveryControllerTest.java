package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.service.CallbackDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CallbackDeliveryControllerTest {

    @Mock
    private CallbackDeliveryService callbackDeliveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CallbackDeliveryController(callbackDeliveryService))
                .build();
    }

    @Test
    void listsLegacyCallbackHistoryAsReadOnlyData() throws Exception {
        when(callbackDeliveryService.listDeliveries(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/gupy/callback-deliveries"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(callbackDeliveryService).listDeliveries(null);
    }

    @Test
    void doesNotExposeServerSideCallbackReprocessing() throws Exception {
        mockMvc.perform(post("/api/v1/gupy/callback-deliveries/1/reprocess"))
                .andExpect(status().isNotFound());

        verifyNoInteractions(callbackDeliveryService);
    }
}
