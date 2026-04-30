package com.kfarms.platform.service;

import com.kfarms.platform.dto.PlatformAnnouncementRequest;

import java.util.Map;

public interface PlatformAnnouncementService {

    Map<String, Object> sendAnnouncement(PlatformAnnouncementRequest request);
}
