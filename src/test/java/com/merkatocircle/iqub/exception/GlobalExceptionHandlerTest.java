package com.merkatocircle.iqub.exception;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MembershipService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IqubRepository iqubRepository;

    @MockBean
    private CurrentMemberProvider currentMemberProvider;

    @MockBean
    private MembershipService membershipService;

    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Trigger a business-rule exception (NotAuthorizedException from requireMember)
    // with a referer header to cover the "referer != null && !referer.isBlank()" true branch.
    @Test
    @WithMockUser
    void handleBusinessRuleViolation_refererRedirectsBack() throws Exception {
        Member member = new Member("Test", "test@test.com", "0712345678", "hash", LocalDate.now());
        setId(member, 1L);
        Member organizer = new Member("Org", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 2L);
        Iqub iqub = new Iqub("Test Group", new BigDecimal("500"), 7, 20, LocalDate.now());
        iqub.setOrganizer(organizer);
        setId(iqub, 1L);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(iqub));
        given(currentMemberProvider.get(any())).willReturn(member);
        given(membershipService.alreadyMember(iqub, member)).willReturn(false);

        mockMvc.perform(get("/iqubs/1/rounds/1")
                        .header("Referer", "http://localhost/iqubs/1/rounds"))
                .andExpect(status().is3xxRedirection());
    }

    // Trigger a business-rule exception without a referer header to cover
    // the "referer == null" → redirect to /dashboard branch.
    @Test
    @WithMockUser
    void handleBusinessRuleViolation_noReferer_redirectsToDashboard() throws Exception {
        Member member = new Member("Test", "test@test.com", "0712345678", "hash", LocalDate.now());
        setId(member, 1L);
        Member organizer = new Member("Org", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 2L);
        Iqub iqub = new Iqub("Test Group", new BigDecimal("500"), 7, 20, LocalDate.now());
        iqub.setOrganizer(organizer);
        setId(iqub, 1L);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(iqub));
        given(currentMemberProvider.get(any())).willReturn(member);
        given(membershipService.alreadyMember(iqub, member)).willReturn(false);

        mockMvc.perform(get("/iqubs/1/rounds/1"))
                .andExpect(status().is3xxRedirection());
    }

    // Test with blank referer header to cover the "!referer.isBlank()" false branch
    @Test
    @WithMockUser
    void handleBusinessRuleViolation_blankReferer_redirectsToDashboard() throws Exception {
        Member member = new Member("Test", "test@test.com", "0712345678", "hash", LocalDate.now());
        setId(member, 1L);
        Member organizer = new Member("Org", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 2L);
        Iqub iqub = new Iqub("Test Group", new BigDecimal("500"), 7, 20, LocalDate.now());
        iqub.setOrganizer(organizer);
        setId(iqub, 1L);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(iqub));
        given(currentMemberProvider.get(any())).willReturn(member);
        given(membershipService.alreadyMember(iqub, member)).willReturn(false);

        mockMvc.perform(get("/iqubs/1/rounds/1")
                        .header("Referer", ""))
                .andExpect(status().is3xxRedirection());
    }
}
