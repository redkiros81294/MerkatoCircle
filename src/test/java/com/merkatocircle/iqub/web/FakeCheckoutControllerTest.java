package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.service.FakePaymentGateway;
import com.merkatocircle.iqub.service.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class FakeCheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FakePaymentGateway fakePaymentGateway;

    @Test
    void showCheckout_renders() throws Exception {
        mockMvc.perform(get("/test/fake-checkout")
                        .param("tx_ref", "iqub-test-1")
                        .param("amount", "500"))
                .andExpect(status().isOk())
                .andExpect(view().name("fake-checkout"));
    }

    @Test
    void simulate_success() throws Exception {
        mockMvc.perform(post("/test/fake-checkout/simulate")
                        .with(csrf())
                        .param("tx_ref", "test-ref")
                        .param("outcome", "success"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/return?tx_ref=test-ref"));
    }

    @Test
    void simulate_failure() throws Exception {
        mockMvc.perform(post("/test/fake-checkout/simulate")
                        .with(csrf())
                        .param("tx_ref", "test-ref")
                        .param("outcome", "failed"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/payments/return?tx_ref=test-ref"));
    }
}
