package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.BoardChatMessageRequest;
import com.mangastudio.backend.dto.response.BoardChatMessageResponse;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.BoardChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/board-chat")
public class BoardChatController {

    private final BoardChatService boardChatService;

    public BoardChatController(BoardChatService boardChatService) {
        this.boardChatService = boardChatService;
    }

    @GetMapping("/series/{seriesId}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_EDITORIAL_BOARD', 'ROLE_EDITORIAL BOARD', 'ROLE_ADMIN')")
    public ResponseEntity<List<BoardChatMessageResponse>> getMessages(
            @PathVariable Long seriesId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(boardChatService.getMessages(seriesId, userDetails.getId()));
    }

    @PostMapping("/series/{seriesId}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_EDITORIAL_BOARD', 'ROLE_EDITORIAL BOARD')")
    public ResponseEntity<BoardChatMessageResponse> sendMessage(
            @PathVariable Long seriesId,
            @RequestBody BoardChatMessageRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        BoardChatMessageResponse message = boardChatService.sendMessage(
                seriesId,
                userDetails.getId(),
                request != null ? request.getContent() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
}
