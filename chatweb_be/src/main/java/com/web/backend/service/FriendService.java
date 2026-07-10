package com.web.backend.service;

import org.springframework.lang.NonNull;

import com.web.backend.controller.response.PageResponse;
import com.web.backend.controller.response.UserSummaryResponse;

public interface FriendService {

    void sendFriendRequest(String requesterUsername, String addresseeUsername);

    void acceptFriendRequest(String acceptorUsername, String requesterUsername);

    PageResponse<UserSummaryResponse> getPendingRequests(String currentUsername, int page, int size, String sortBy);

    PageResponse<UserSummaryResponse> getSentRequests(String currentUsername, int page, int size);

    PageResponse<UserSummaryResponse> getFriendsList(String currentUsername, int page, int size, String sortBy);

    void deleteFriendship(String currentUsername, String targetUsername);

    void blockUser(String blockerUsername, String targetUsername);

    boolean isFriend(@NonNull String user1, @NonNull String user2);
}
