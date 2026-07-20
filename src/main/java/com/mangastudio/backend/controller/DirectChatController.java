package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.DirectChatMessageRequest;
import com.mangastudio.backend.dto.response.DirectChatMessageResponse;
import com.mangastudio.backend.dto.response.DirectChatContactResponse;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.DirectChatService;
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
@RequestMapping("/api/v1/direct-chat")
public class DirectChatController {

    private final DirectChatService directChatService;

    public DirectChatController(DirectChatService directChatService) {
        this.directChatService = directChatService;
    }

    @GetMapping("/contacts")
    @PreAuthorize("hasAnyAuthority('ROLE_MANGAKA', 'ROLE_ASSISTANT', 'ROLE_TANTOU_EDITOR', 'ROLE_TANTOU EDITOR')")
    public ResponseEntity<List<DirectChatContactResponse>> getContacts(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(directChatService.getContacts(userDetails.getId()));
    }

    @GetMapping("/users/{otherUserId}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_MANGAKA', 'ROLE_ASSISTANT', 'ROLE_TANTOU_EDITOR', 'ROLE_TANTOU EDITOR')")
    public ResponseEntity<List<DirectChatMessageResponse>> getMessages(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(directChatService.getMessages(userDetails.getId(), otherUserId));
    }

    @PostMapping("/users/{recipientId}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_MANGAKA', 'ROLE_ASSISTANT', 'ROLE_TANTOU_EDITOR', 'ROLE_TANTOU EDITOR')")
    public ResponseEntity<DirectChatMessageResponse> sendMessage(
            @PathVariable Long recipientId,
            @RequestBody DirectChatMessageRequest request,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        DirectChatMessageResponse saved = directChatService.sendMessage(
                userDetails.getId(),
                recipientId,
                request != null ? request.getContent() : null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
