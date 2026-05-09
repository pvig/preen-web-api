package com.pvig.preenWebApi.comment;

import com.pvig.preenWebApi.user.UserProfileDto;

import java.time.Instant;

public record CommentDto(
        String id,
        UserProfileDto author,
        String content,
        Instant createdAt
) {
}
