package com.kfarms.service;

import com.kfarms.dto.SupportAssistantChatRequestDto;
import com.kfarms.dto.SupportTicketCreateRequestDto;
import com.kfarms.dto.SupportTicketMessageRequestDto;
import com.kfarms.dto.SupportTicketStatusUpdateRequestDto;

import java.util.Map;

public interface SupportService {

    Map<String, Object> getResources();

    Map<String, Object> getTickets();

    Map<String, Object> getPlatformTickets(String search, String status, String lane, int page, int size);

    Map<String, Object> createTicket(SupportTicketCreateRequestDto request, String actor);

    Map<String, Object> addTicketReply(String ticketId, SupportTicketMessageRequestDto request, String actor);

    Map<String, Object> addPlatformTicketReply(String ticketId, SupportTicketMessageRequestDto request, String actor);

    Map<String, Object> updateTicketStatus(String ticketId, SupportTicketStatusUpdateRequestDto request, String actor);

    Map<String, Object> updatePlatformTicketStatus(String ticketId, SupportTicketStatusUpdateRequestDto request, String actor);

    Map<String, Object> getAssistantConversation(String actor);

    Map<String, Object> resetAssistantConversation(String actor);

    Map<String, Object> chat(SupportAssistantChatRequestDto request, String actor);
}
