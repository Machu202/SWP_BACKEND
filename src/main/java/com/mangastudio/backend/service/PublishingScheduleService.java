package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.PublishingScheduleRequest;
import com.mangastudio.backend.entity.PublishingSchedule;

import java.util.List;

public interface PublishingScheduleService {
    PublishingSchedule createSchedule(Long currentUserId, PublishingScheduleRequest request);
    List<PublishingSchedule> getSchedulesBySeries(Long seriesId);
    PublishingSchedule updateSchedule(Long scheduleId, Long currentUserId, PublishingScheduleRequest request);
    void deleteSchedule(Long scheduleId, Long currentUserId);
}