package com.pvig.preenWebApi.patch;

import com.fasterxml.jackson.databind.JsonNode;
import com.pvig.preenWebApi.user.UserProfileDto;

import java.time.Instant;
import java.util.List;

public record PublishedPatchDto(
        String id,
        String name,
        String description,
        List<String> tags,
        UserProfileDto author,
        JsonNode patchData,
        Instant createdAt,
        long commentsCount
) {
}
