package com.whiteboard.backend.board.access;

import com.whiteboard.backend.board.boardmember.BoardPermission;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BoardAccessPolicyCachingService {

    private final StringRedisTemplate redisTemplate;
    private final BoardAccessPolicy boardAccessPolicy;

    public BoardAccessPolicyCachingService(
            StringRedisTemplate redisTemplate,
            BoardAccessPolicy boardAccessPolicy
    ) {
        this.redisTemplate = redisTemplate;
        this.boardAccessPolicy = boardAccessPolicy;
    }

    public BoardPermission cachePermission(
            UUID boardId,
            Long userId
    ) {
        BoardPermission permission =
                boardAccessPolicy.getBoardPermission(boardId, userId);

        String key = "board:" + boardId + ":permissions";

        redisTemplate.opsForHash().put(
                key,
                userId.toString(),
                permission.name()
        );

        return permission;
    }

    public BoardPermission getPermission(
            UUID boardId,
            Long userId
    ) {
        String key = "board:" + boardId + ":permissions";

        Object value = redisTemplate
                .opsForHash()
                .get(key, userId.toString());

        if (value == null) {
            return null;
        }

        return BoardPermission.valueOf(value.toString());
    }

    public void removePermission(
            UUID boardId,
            Long userId
    ) {
        String key = "board:" + boardId + ":permissions";

        redisTemplate.opsForHash().delete(
                key,
                userId.toString()
        );
    }

    public boolean canEdit(
            UUID boardId,
            Long userId
    ) {
        return getPermission(boardId, userId)
                == BoardPermission.EDITOR;
    }

    public boolean canView(
            UUID boardId,
            Long userId
    ) {
        BoardPermission permission =
                getPermission(boardId, userId);

        return permission == BoardPermission.VIEWER
                || permission == BoardPermission.EDITOR;
    }
}