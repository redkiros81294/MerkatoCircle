package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MembershipService;
import com.merkatocircle.iqub.service.NotificationService;
import com.merkatocircle.iqub.service.RoundService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IqubRepository iqubRepository;

    @MockBean
    private CommandLineRunner dataSeeder;

    @MockBean
    private MembershipService membershipService;

    @MockBean
    private RoundService roundService;

    @MockBean
    private ContributionService contributionService;

    @MockBean
    private NotificationService notificationService;

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
    void getIqubHome_memberSeesPage() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(i, selam)).willReturn(true);
        given(roundService.getCurrentRound(i)).willReturn(round);
        given(contributionService.findForRoundAndMember(round, selam)).willReturn(Optional.empty());
        given(notificationService.unreadCount(selam)).willReturn(0L);

        mockMvc.perform(get("/iqubs/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("group"));
    }

    @Test
    @WithMockUser
    void getIqubHome_nonMember_redirects() throws Exception {
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 2L);
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(outsider);
        given(membershipService.alreadyMember(i, outsider)).willReturn(false);

        mockMvc.perform(get("/iqubs/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postJoin_alreadyMember_redirectsToDashboard() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(i, selam)).willReturn(true);

        mockMvc.perform(post("/iqubs/1/join")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser
    void postJoin_newMember_joinsSuccessfully() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Membership membership = new Membership(selam, i, LocalDate.now(), MembershipStatus.ACTIVE);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(i, selam)).willReturn(false);
        given(membershipService.join(any(Iqub.class), any(Member.class), any(LocalDate.class))).willReturn(membership);

        mockMvc.perform(post("/iqubs/1/join")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/1"));
    }

    @Test
    @WithMockUser
    void getMembersPage_rendersForMember() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Membership m = new Membership(selam, i, LocalDate.now(), MembershipStatus.ACTIVE);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(i, selam)).willReturn(true);
        given(membershipService.membersOf(i)).willReturn(List.of(m));

        mockMvc.perform(get("/iqubs/1/members"))
                .andExpect(status().isOk())
                .andExpect(view().name("members"));
    }

    @Test
    @WithMockUser
    void getGroupsPage_renders() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Membership m = new Membership(selam, i, LocalDate.now(), MembershipStatus.ACTIVE);

        given(iqubRepository.findAll()).willReturn(List.of(i));
        given(membershipService.membersOf(i)).willReturn(List.of(m));
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(get("/groups"))
                .andExpect(status().isOk())
                .andExpect(view().name("groups"));
    }

    @Test
    @WithMockUser
    void postCreateGroup_success_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        Iqub saved = new Iqub("New Circle", new BigDecimal("500"), 7, 20, LocalDate.now());
        saved.setOrganizer(selam);
        setId(saved, 99L);

        given(currentMemberProvider.get(any())).willReturn(selam);
        given(iqubRepository.save(any(Iqub.class))).willReturn(saved);
        given(membershipService.join(any(), any(), any())).willReturn(new Membership(selam, saved, LocalDate.now(), MembershipStatus.ACTIVE));

        mockMvc.perform(post("/groups")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "New Circle")
                        .param("contributionAmount", "500")
                        .param("roundIntervalDays", "7")
                        .param("maxMembers", "20")
                        .param("auctionMode", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/99"));
    }

    @Test
    @WithMockUser
    void getIqubHome_notFound_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(iqubRepository.findById(999L)).willReturn(Optional.empty());
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(get("/iqubs/999"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postJoin_ioobNotFound_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(iqubRepository.findById(999L)).willReturn(Optional.empty());
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(post("/iqubs/999/join").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postRemove_notAuthorized_redirects() throws Exception {
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 1L);
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 2L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(organizer);
        Membership target = new Membership(outsider, i, LocalDate.now(), MembershipStatus.ACTIVE);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(outsider);

        mockMvc.perform(post("/iqubs/10/members/5/remove").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
