package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Iqub;
import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.domain.Membership;
import com.merkatocircle.iqub.domain.MembershipStatus;
import com.merkatocircle.iqub.domain.PlatformRole;
import com.merkatocircle.iqub.exception.DuplicateEmailException;
import com.merkatocircle.iqub.service.MemberService;
import com.merkatocircle.iqub.service.MembershipService;
import com.merkatocircle.iqub.service.TheIqub;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommandLineRunner dataSeeder;

    @MockBean
    private MemberService memberService;

    @MockBean
    private MembershipService membershipService;

    @MockBean
    private TheIqub theIqub;

    @Test
    void getRegister_rendersForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_validationErrors_returnsForm() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "")
                        .param("email", "invalid")
                        .param("phone", "123")
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void getRegister_withExistingForm_rendersForm() throws Exception {
        // When redirecting back after validation error, the form attribute already exists
        mockMvc.perform(get("/register")
                        .flashAttr("form", new RegistrationController.RegistrationForm()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_passwordMismatch_validForm_returnsForm() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "Test User")
                        .param("email", "test@test.com")
                        .param("phone", "0712345678")
                        .param("password", "password123")
                        .param("confirmPassword", "differentpassword"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_duplicateEmail_returnsForm() throws Exception {
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        Iqub defaultIqub = new Iqub("Default", new BigDecimal("500"), 7, 20, LocalDate.now());
        defaultIqub.setOrganizer(organizer);
        given(theIqub.get()).willReturn(defaultIqub);
        given(memberService.register(any(), any(), any(), any()))
                .willThrow(new DuplicateEmailException("Email already in use"));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "Test")
                        .param("email", "dup@test.com")
                        .param("phone", "0712345678")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void postRegister_success_redirectsToLogin() throws Exception {
        Member newUser = new Member("New User", "new@test.com", "0712345678", "hash", LocalDate.now());
        Member organizer = new Member("Organizer", "org@test.com", "0911223344", "hash", LocalDate.now());
        Iqub defaultIqub = new Iqub("Default", new BigDecimal("500"), 7, 20, LocalDate.now());
        defaultIqub.setOrganizer(organizer);

        given(theIqub.get()).willReturn(defaultIqub);
        given(memberService.register(any(), any(), any(), any())).willReturn(newUser);
        given(membershipService.join(any(), any(), any())).willReturn(new Membership(newUser, defaultIqub, LocalDate.now(), MembershipStatus.ACTIVE));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "New User")
                        .param("email", "new@test.com")
                        .param("phone", "0712345678")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}
