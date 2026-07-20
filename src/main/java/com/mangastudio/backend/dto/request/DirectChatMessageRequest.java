package com.mangastudio.backend.dto.request;

public class DirectChatMessageRequest {
    private String content;

    public DirectChatMessageRequest() {
    }

    public DirectChatMessageRequest(String content) {
        this.content = content;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
