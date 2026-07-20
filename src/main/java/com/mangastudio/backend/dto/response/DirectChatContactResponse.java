package com.mangastudio.backend.dto.response;

import java.util.List;

public class DirectChatContactResponse {
    private final Long id;
    private final String fullName;
    private final String username;
    private final String roleName;
    private final List<String> seriesTitles;
    private final long unreadCount;

    public DirectChatContactResponse(Long id, String fullName, String username, String roleName,
                                     List<String> seriesTitles, long unreadCount) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.roleName = roleName;
        this.seriesTitles = seriesTitles;
        this.unreadCount = unreadCount;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getRoleName() { return roleName; }
    public List<String> getSeriesTitles() { return seriesTitles; }
    public long getUnreadCount() { return unreadCount; }
}
