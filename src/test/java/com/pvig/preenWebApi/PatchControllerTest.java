package com.pvig.preenWebApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvig.preenWebApi.auth.JwtService;
import com.pvig.preenWebApi.comment.CommentRepository;
import com.pvig.preenWebApi.patch.PatchEntity;
import com.pvig.preenWebApi.patch.PatchRepository;
import com.pvig.preenWebApi.user.User;
import com.pvig.preenWebApi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PatchControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired PatchRepository patchRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;

    private User user1;
    private User user2;
    private String token1;
    private String token2;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        patchRepository.deleteAll();
        userRepository.deleteAll();

        user1 = new User();
        user1.setEmail("user1@test.com");
        user1.setPassword(passwordEncoder.encode("password"));
        user1.setName("User One");
        user1 = userRepository.save(user1);
        token1 = jwtService.generateAccessToken(user1);

        user2 = new User();
        user2.setEmail("user2@test.com");
        user2.setPassword(passwordEncoder.encode("password"));
        user2.setName("User Two");
        user2 = userRepository.save(user2);
        token2 = jwtService.generateAccessToken(user2);
    }

    private PatchEntity createPatch(User author) {
        PatchEntity patch = new PatchEntity();
        patch.setName("Test Patch");
        patch.setDescription("A test description");
        patch.setTags(List.of("bass"));
        patch.setAuthor(author);
        patch.setCreatedAt(Instant.now());
        patch.setPatchData("{\"freq\":440}");
        return patchRepository.save(patch);
    }

    @Test
    void getPatches_noToken_returns200() throws Exception {
        mockMvc.perform(get("/api/patches"))
                .andExpect(status().isOk());
    }

    @Test
    void postPatch_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/patches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","patchData":{"freq":440}}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postPatch_withToken_returns200() throws Exception {
        mockMvc.perform(post("/api/patches")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Patch","description":"Cool","tags":["bass"],"patchData":{"freq":440}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("My Patch"))
                .andExpect(jsonPath("$.author.email").value("user1@test.com"))
                .andExpect(jsonPath("$.patchData.freq").value(440));
    }

    @Test
    void deletePatch_byOwner_returns204() throws Exception {
        PatchEntity patch = createPatch(user1);
        mockMvc.perform(delete("/api/patches/" + patch.getId())
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePatch_byOtherUser_returns403() throws Exception {
        PatchEntity patch = createPatch(user1);
        mockMvc.perform(delete("/api/patches/" + patch.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }

    @Test
    void postComment_withToken_returns200() throws Exception {
        PatchEntity patch = createPatch(user1);
        mockMvc.perform(post("/api/patches/" + patch.getId() + "/comments")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Great patch!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value("Great patch!"));
    }

    @Test
    void postPatch_invalidTag_returns400() throws Exception {
        mockMvc.perform(post("/api/patches")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"My Patch","tags":["invalidtag"],"patchData":{"freq":440}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postPatch_emptyName_returns400() throws Exception {
        mockMvc.perform(post("/api/patches")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","patchData":{"freq":440}}
                                """))
                .andExpect(status().isBadRequest());
    }
}
