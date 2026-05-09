package com.pvig.preenWebApi.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patches/{patchId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable("patchId") UUID patchId) {
        return ResponseEntity.ok(commentService.getComments(patchId));
    }

    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @PathVariable("patchId") UUID patchId,
            @Valid @RequestBody CommentRequestDto request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(commentService.addComment(patchId, request, auth.getName()));
    }
}
