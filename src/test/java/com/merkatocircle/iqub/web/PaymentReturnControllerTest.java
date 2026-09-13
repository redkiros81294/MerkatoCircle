package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentReturnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandLineRunner dataSeeder;

    @MockBean
    private ContributionService contributionService;

    @MockBean
    private CurrentMemberProvider currentMemberProvider;

    @Test
    void paymentReturn_withTxRef_confirmsAndRenders() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        Iqub i = new Iqub("Test", BigDecimal.valueOf(500), 7, 20, LocalDate.now());
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        Contribution c = new Contribution(round, selam, BigDecimal.valueOf(500));
        Contribution confirmed = new Contribution(round, selam, BigDecimal.valueOf(500));
        confirmed.markPaid(com.merkatocircle.iqub.domain.ContributionStatus.PAID, BigDecimal.valueOf(500), LocalDate.now(), BigDecimal.ZERO);

        given(contributionService.findByTxRef("test-ref")).willReturn(confirmed);
        given(contributionService.confirmPayment("test-ref")).willReturn(confirmed);

        mockMvc.perform(get("/payments/return")
                        .param("tx_ref", "test-ref"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-return"));
    }

    @Test
    void paymentReturn_withContributionId_renders() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        Iqub i = new Iqub("Test", BigDecimal.valueOf(500), 7, 20, LocalDate.now());
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        Contribution c = new Contribution(round, selam, BigDecimal.valueOf(500));
        c.markAwaitingPayment("some-ref"); // set txRef

        given(contributionService.findById(1L)).willReturn(c);
        given(contributionService.confirmPayment("some-ref")).willReturn(c);

        mockMvc.perform(get("/payments/return")
                        .param("contribution_id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-return"));
    }

    @Test
    void paymentReturn_withContributionId_noTxRef_renders() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        Iqub i = new Iqub("Test", BigDecimal.valueOf(500), 7, 20, LocalDate.now());
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        Contribution c = new Contribution(round, selam, BigDecimal.valueOf(500));
        // txRef is null - the else-if branch won't be taken

        given(contributionService.findById(1L)).willReturn(c);

        mockMvc.perform(get("/payments/return")
                        .param("contribution_id", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-return"));
    }
}
