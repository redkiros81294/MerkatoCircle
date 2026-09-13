package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Bid;
import com.merkatocircle.iqub.domain.Contribution;
import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.PayoutMode;
import com.merkatocircle.iqub.domain.Round;
import com.merkatocircle.iqub.repository.IqubRepository;
import com.merkatocircle.iqub.repository.MembershipRepository;
import com.merkatocircle.iqub.service.BidService;
import com.merkatocircle.iqub.service.ContributionService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import com.merkatocircle.iqub.service.MembershipService;
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
class RoundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IqubRepository iqubRepository;

    @MockBean
    private MembershipRepository membershipRepository;

    @MockBean
    private CommandLineRunner dataSeeder;

    @MockBean
    private MembershipService membershipService;

    @MockBean
    private RoundService roundService;

    @MockBean
    private ContributionService contributionService;

    @MockBean
    private CurrentMemberProvider currentMemberProvider;

    @MockBean
    private BidService bidService;

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
    void getRounds_noMemberships_redirectsToDashboard() throws Exception {
        Member yonas = new Member("Yonas", "yonas@test.com", "0911223344", "hash", LocalDate.now());
        setId(yonas, 4L);
        given(currentMemberProvider.get(any())).willReturn(yonas);
        given(membershipService.alreadyMember(any(), any())).willReturn(false);

        mockMvc.perform(get("/rounds"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser
    void getRounds_withMemberships_rendersRounds() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(any(), any())).willReturn(true);
        given(iqubRepository.findAll()).willReturn(List.of(i));
        Membership mem = new Membership(selam, i, LocalDate.now(), MembershipStatus.ACTIVE);
        setId(mem, 1L);
        given(membershipRepository.findByMember(selam)).willReturn(List.of(mem));
        given(membershipService.membersOf(i)).willReturn(List.of(mem));
        given(roundService.getAllRounds(i)).willReturn(List.of(round));

        mockMvc.perform(get("/rounds"))
                .andExpect(status().isOk())
                .andExpect(view().name("rounds"));
    }

    @Test
    @WithMockUser
    void getRoundDetail_memberSeesDetail() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);
        Contribution c = new Contribution(round, selam, new BigDecimal("500.00"));

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(contributionService.findForRoundAndMember(round, selam)).willReturn(Optional.of(c));
        given(roundService.getEligibleMembers(round)).willReturn(List.of(selam));
        given(bidService.myBid(round, selam)).willReturn(Optional.empty());
        given(bidService.allBidsFor(round)).willReturn(List.of());
        given(roundService.getContributions(round)).willReturn(List.of(c));
        given(membershipService.alreadyMember(i, selam)).willReturn(true);

        mockMvc.perform(get("/iqubs/1/rounds/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("round-detail"));
    }

    @Test
    @WithMockUser
    void postDraw_organizerCanDraw() throws Exception {
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(organizer);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        Round closedRound = new Round(i, 1, LocalDate.now().plusDays(7));
        closedRound.closeWithWinner(organizer, new BigDecimal("1500.00"), LocalDate.now());
        given(roundService.runDraw(round)).willReturn(closedRound);
        given(currentMemberProvider.get(any())).willReturn(organizer);

        mockMvc.perform(post("/iqubs/10/rounds/1/draw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/10/rounds/1"));
    }

    @Test
    @WithMockUser
    void postBid_memberCanBid() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(selam);
        Bid bid = new Bid(round, selam, new BigDecimal("10"), LocalDate.now());
        setId(bid, 1L);
        given(bidService.submitBid(round, selam, new BigDecimal("10"))).willReturn(bid);
        given(membershipService.alreadyMember(i, selam)).willReturn(true);

        mockMvc.perform(post("/iqubs/1/rounds/1/bid")
                        .with(csrf())
                        .param("discountPercent", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/1/rounds/1"));
    }

    @Test
    @WithMockUser
    void getRoundDetail_nonMember_redirects() throws Exception {
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 2L);
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(outsider);
        given(membershipService.alreadyMember(i, outsider)).willReturn(false);

        mockMvc.perform(get("/iqubs/1/rounds/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postBid_nonMember_redirects() throws Exception {
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 2L);
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(outsider);
        given(membershipService.alreadyMember(i, outsider)).willReturn(false);

        mockMvc.perform(post("/iqubs/1/rounds/1/bid")
                        .with(csrf())
                        .param("discountPercent", "10"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postDraw_notAuthorized_redirects() throws Exception {
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 1L);
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 3L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(organizer);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(outsider);

        mockMvc.perform(post("/iqubs/10/rounds/1/draw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void getRoundDetail_iqbNotFound_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(iqubRepository.findById(999L)).willReturn(Optional.empty());
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(get("/iqubs/999/rounds/1"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void getRoundDetail_auctionMode_rendersWithBids() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        i.setPayoutMode(PayoutMode.AUCTION);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);
        Contribution c = new Contribution(round, selam, new BigDecimal("500.00"));
        Bid myBid = new Bid(round, selam, new BigDecimal("10"), LocalDate.now());

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(contributionService.findForRoundAndMember(round, selam)).willReturn(Optional.of(c));
        given(roundService.getEligibleMembers(round)).willReturn(List.of(selam));
        given(bidService.myBid(round, selam)).willReturn(Optional.of(myBid));
        given(bidService.allBidsFor(round)).willReturn(List.of(myBid));
        given(roundService.getContributions(round)).willReturn(List.of(c));
        given(membershipService.alreadyMember(i, selam)).willReturn(true);

        mockMvc.perform(get("/iqubs/1/rounds/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("round-detail"));
    }

    @Test
    @WithMockUser
    void getIqubRounds_memberSeesRounds() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);

        Round round1 = new Round(i, 1, LocalDate.now().plusDays(7));
        round1.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);
        Round round2 = new Round(i, 2, LocalDate.now().plusDays(14));
        round2.setStatus(com.merkatocircle.iqub.domain.RoundStatus.CLOSED);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(roundService.getAllRounds(i)).willReturn(List.of(round1, round2));
        given(membershipService.alreadyMember(i, selam)).willReturn(true);

        mockMvc.perform(get("/iqubs/10/rounds"))
                .andExpect(status().isOk())
                .andExpect(view().name("rounds"));
    }

    @Test
    @WithMockUser
    void getIqubRounds_nonMember_redirects() throws Exception {
        Member outsider = new Member("Outsider", "out@test.com", "0911223344", "hash", LocalDate.now());
        setId(outsider, 2L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(outsider);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(currentMemberProvider.get(any())).willReturn(outsider);
        given(membershipService.alreadyMember(i, outsider)).willReturn(false);

        mockMvc.perform(get("/iqubs/10/rounds"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void postDraw_adminOnNullOrganizer_iqbCanDraw() throws Exception {
        Member admin = new Member("Admin", "admin@test.com", "0912345678", "hash", LocalDate.now());
        admin.setPlatformRole(com.merkatocircle.iqub.domain.PlatformRole.ADMIN);
        setId(admin, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        // organizer is null - tests the isOrganizerOrAdmin null organizer branch
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        Round closedRound = new Round(i, 1, LocalDate.now().plusDays(7));
        closedRound.closeWithWinner(admin, new BigDecimal("1500.00"), LocalDate.now());
        given(roundService.runDraw(round)).willReturn(closedRound);
        given(currentMemberProvider.get(any())).willReturn(admin);

        mockMvc.perform(post("/iqubs/10/rounds/1/draw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/10/rounds/1"));
    }

    @Test
    @WithMockUser
    void pay_nonDefaultPort_redirectsToCheckout() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(selam);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));

        given(iqubRepository.findById(1L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(membershipService.alreadyMember(i, selam)).willReturn(true);
        given(contributionService.initiatePayment(any(), any(), any(), any()))
                .willReturn(new com.merkatocircle.iqub.service.PaymentInitiation("https://checkout.example.com/pay", "test-tx-ref"));

        mockMvc.perform(post("/iqubs/1/rounds/1/pay")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("https://checkout.example.com/pay"));
    }

    @Test
    @WithMockUser
    void postDraw_adminCanDraw() throws Exception {
        Member admin = new Member("Admin", "admin@test.com", "0912345678", "hash", LocalDate.now());
        admin.setPlatformRole(com.merkatocircle.iqub.domain.PlatformRole.ADMIN);
        setId(admin, 1L);
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        setId(organizer, 99L);
        Iqub i = new Iqub("Test Iqub", new BigDecimal("500"), 7, 20, LocalDate.now());
        i.setOrganizer(organizer);
        Round round = new Round(i, 1, LocalDate.now().plusDays(7));
        round.setStatus(com.merkatocircle.iqub.domain.RoundStatus.OPEN);

        given(iqubRepository.findById(10L)).willReturn(Optional.of(i));
        given(roundService.getById(1L)).willReturn(round);
        Round closedRound = new Round(i, 1, LocalDate.now().plusDays(7));
        closedRound.closeWithWinner(organizer, new BigDecimal("1500.00"), LocalDate.now());
        given(roundService.runDraw(round)).willReturn(closedRound);
        given(currentMemberProvider.get(any())).willReturn(admin);

        mockMvc.perform(post("/iqubs/10/rounds/1/draw")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/iqubs/10/rounds/1"));
    }
}
