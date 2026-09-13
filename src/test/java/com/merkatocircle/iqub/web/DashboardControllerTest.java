package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import java.time.LocalDate;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private IqubRepository iqubRepository;

    @MockBean
    private CurrentMemberProvider currentMemberProvider;

    private static void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @WithMockUser
    void dashboard_rendersWithMemberships() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new java.math.BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        setId(i, 1L);
        Membership m = new Membership(selam, i, LocalDate.now(), MembershipStatus.ACTIVE);

        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipRepository.findByMember(selam)).willReturn(List.of(m));
        given(iqubRepository.findAll()).willReturn(List.of(i));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser
    void dashboard_showsAvailableToJoin() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub joinedIqub = new Iqub("Joined", new java.math.BigDecimal("500"), 7, 20, LocalDate.now());
        setId(joinedIqub, 1L);
        joinedIqub.setOrganizer(selam);
        Iqub otherIqub = new Iqub("Other Group", new java.math.BigDecimal("500"), 7, 20, LocalDate.now());
        setId(otherIqub, 2L);
        Membership m = new Membership(selam, joinedIqub, LocalDate.now(), MembershipStatus.ACTIVE);

        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipRepository.findByMember(selam)).willReturn(List.of(m));
        given(iqubRepository.findAll()).willReturn(List.of(joinedIqub, otherIqub));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }
}
