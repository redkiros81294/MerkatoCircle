package com.merkatocircle.iqub.web;

import com.merkatocircle.iqub.domain.Member;
import com.merkatocircle.iqub.service.MemberService;
import com.merkatocircle.iqub.service.CurrentMemberProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrentMemberProvider currentMemberProvider;

    @MockBean
    private MemberService memberService;

    @MockBean
    private PasswordEncoder passwordEncoder;

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
    void settingsShow_renders() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }

    @Test
    @WithMockUser
    void settingsProfile_validationError_returnsForm() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(post("/settings/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "")
                        .param("phone", "invalid"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }

    @Test
    @WithMockUser
    void settingsProfile_success_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(post("/settings/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName", "New Name")
                        .param("phone", "0712345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));
    }

    @Test
    @WithMockUser
    void settingsPassword_wrongCurrentPassword_returnsForm() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        selam.setPasswordHash("{noop}encoded");
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        mockMvc.perform(post("/settings/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("currentPassword", "wrong")
                        .param("newPassword", "newpassword123"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }

    @Test
    @WithMockUser
    void settingsPassword_success_redirects() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        selam.setPasswordHash("{noop}encoded");
        given(currentMemberProvider.get(any())).willReturn(selam);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

        mockMvc.perform(post("/settings/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("currentPassword", "password123")
                        .param("newPassword", "newpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));
    }

    @Test
    @WithMockUser
    void settingsPassword_blankPassword_validationError() throws Exception {
        Member selam = new Member("Selam", "selam@test.com", "0911223344", "hash", LocalDate.now());
        setId(selam, 1L);
        given(currentMemberProvider.get(any())).willReturn(selam);

        mockMvc.perform(post("/settings/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("currentPassword", "")
                        .param("newPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }
}
