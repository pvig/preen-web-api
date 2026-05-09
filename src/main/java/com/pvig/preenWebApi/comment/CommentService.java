package com.pvig.preenWebApi.comment;

import com.pvig.preenWebApi.patch.PatchEntity;
import com.pvig.preenWebApi.patch.PatchRepository;
import com.pvig.preenWebApi.user.User;
import com.pvig.preenWebApi.user.UserProfileDto;
import com.pvig.preenWebApi.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PatchRepository patchRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CommentDto> getComments(UUID patchId) {
        PatchEntity patch = patchRepository.findById(patchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patch not found"));
        return commentRepository.findByPatch(patch).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CommentDto addComment(UUID patchId, CommentRequestDto request, String currentUserSub) {
        PatchEntity patch = patchRepository.findById(patchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patch not found"));

        User author = userRepository.findBySub(currentUserSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        CommentEntity comment = new CommentEntity();
        comment.setContent(request.content());
        comment.setPatch(patch);
        comment.setAuthor(author);
        comment.setCreatedAt(Instant.now());

        return toDto(commentRepository.save(comment));
    }

    private CommentDto toDto(CommentEntity comment) {
        User author = comment.getAuthor();
        return new CommentDto(
                comment.getId().toString(),
                new UserProfileDto(author.getId().toString(), author.getName(), author.getEmail()),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
