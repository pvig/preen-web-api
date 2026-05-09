package com.pvig.preenWebApi.patch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pvig.preenWebApi.comment.CommentRepository;
import com.pvig.preenWebApi.user.User;
import com.pvig.preenWebApi.user.UserProfileDto;
import com.pvig.preenWebApi.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatchService {

    private static final int MAX_PAGE_SIZE = 50;

    private static final Set<String> ALLOWED_TAGS = Set.of(
            "bass", "lead", "pad", "pluck", "keys", "organ", "brass", "strings",
            "percussion", "fx", "ambient", "acid", "arp", "bell", "noise", "drone", "experimental"
    );

    private final PatchRepository patchRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public Page<PublishedPatchDto> getPatches(int page, int size, String search, String tags) {
        size = Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<PatchEntity> spec = null;

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Specification<PatchEntity> searchSpec = (root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), pattern);
            spec = searchSpec;
        }

        if (tags != null && !tags.isBlank()) {
            List<String> tagList = Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .filter(t -> !t.isEmpty())
                    .toList();
            if (!tagList.isEmpty()) {
                Specification<PatchEntity> tagSpec = (root, query, cb) -> {
                    query.distinct(true);
                    Join<PatchEntity, String> tagJoin = root.join("tags", JoinType.INNER);
                    return tagJoin.in(tagList);
                };
                spec = (spec == null) ? tagSpec : spec.and(tagSpec);
            }
        }

        return patchRepository.findAll(spec, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public PublishedPatchDto getPatchById(UUID id) {
        PatchEntity patch = patchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patch not found"));
        return toDto(patch);
    }

    @Transactional
    public PublishedPatchDto createPatch(PublishPatchRequestDto request, String currentUserSub) {
        validateTags(request.tags());

        User author = userRepository.findBySub(currentUserSub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        PatchEntity patch = new PatchEntity();
        patch.setName(request.name());
        patch.setDescription(request.description());
        patch.setTags(request.tags() != null ? request.tags() : List.of());
        patch.setAuthor(author);
        patch.setCreatedAt(Instant.now());

        try {
            patch.setPatchData(objectMapper.writeValueAsString(request.patchData()));
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid patchData");
        }

        return toDto(patchRepository.save(patch));
    }

    @Transactional
    public void deletePatch(UUID id, String currentUserSub) {
        PatchEntity patch = patchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patch not found"));

        if (!patch.getAuthor().getEmail().equals(currentUserSub)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this patch");
        }

        patchRepository.delete(patch);
    }

    private void validateTags(List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            if (!ALLOWED_TAGS.contains(tag)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid tag: '" + tag + "'. Allowed values: " + ALLOWED_TAGS);
            }
        }
    }

    private PublishedPatchDto toDto(PatchEntity patch) {
        User author = patch.getAuthor();
        UserProfileDto authorDto = new UserProfileDto(
                author.getId().toString(),
                author.getName(),
                author.getEmail()
        );

        JsonNode patchDataNode;
        try {
            patchDataNode = objectMapper.readTree(patch.getPatchData());
        } catch (JsonProcessingException e) {
            patchDataNode = objectMapper.nullNode();
        }

        long commentsCount = commentRepository.countByPatchId(patch.getId());

        return new PublishedPatchDto(
                patch.getId().toString(),
                patch.getName(),
                patch.getDescription(),
                new ArrayList<>(patch.getTags()),
                authorDto,
                patchDataNode,
                patch.getCreatedAt(),
                commentsCount
        );
    }
}
