package com.kfarms.platform.service;

import com.kfarms.dto.LoginResponse;
import com.kfarms.platform.dto.AcceptPlatformInviteRequest;
import com.kfarms.platform.dto.CreatePlatformInviteRequest;
import com.kfarms.platform.dto.CreatePlatformUserRequest;
import com.kfarms.platform.dto.PlatformInvitePreviewDto;
import com.kfarms.platform.dto.PlatformUserInviteDto;
import com.kfarms.platform.dto.PlatformUserListItemDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlatformUserService {

    Page<PlatformUserListItemDto> searchUsers(String search, boolean platformOnly, Pageable pageable);

    PlatformUserListItemDto createPlatformUser(CreatePlatformUserRequest request);

    PlatformUserInviteDto createPlatformInvite(CreatePlatformInviteRequest request, String inviterIdentity);

    PlatformInvitePreviewDto resolvePlatformInvite(String token);

    LoginResponse acceptPlatformInvite(AcceptPlatformInviteRequest request);

    void setPlatformAdmin(Long userId, boolean value);

    void setUserEnabled(Long userId, boolean value);
}
