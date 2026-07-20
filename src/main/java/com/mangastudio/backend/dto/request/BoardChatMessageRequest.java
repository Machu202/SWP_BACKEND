package com.mangastudio.backend.dto.request;

public class BoardChatMessageRequest {
    private String content;

    public BoardChatMessageRequest() {
    }

    public BoardChatMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
