package com.web.backend.service.impl;

import com.web.backend.common.FriendshipStatus;
import com.web.backend.common.NotificationsStatus;
import com.web.backend.controller.response.NotificationMessageResponse;
import com.web.backend.controller.response.PageResponse;
import com.web.backend.controller.response.UserSummaryResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.event.FriendshipEvent;
import com.web.backend.exception.*;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.FriendshipEntity;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.FriendshipRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FRIEND-SERVICE")
public class FriendServiceImpl implements FriendService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public void sendFriendRequest(String requesterUsername, String addresseeUsername) {
        if (requesterUsername.equals(addresseeUsername)) throw new InvalidDataException("Lỗi: Tự kết bạn");

        UserEntity requester = getUser(requesterUsername);
        UserEntity addressee = getUser(addresseeUsername);

        Optional<FriendshipEntity> existingRelation = friendshipRepository.findByUsers(requester, addressee);

        if (existingRelation.isPresent()) {
            FriendshipEntity f = existingRelation.get();
            if (f.getStatus() == FriendshipStatus.BLOCKED) {
                throw new AccessForbiddenException("Không thể gửi lời mời (Đang bị chặn hoặc đã chặn).");
            }
            if (f.getStatus() == FriendshipStatus.ACCEPTED || f.getStatus() == FriendshipStatus.PENDING) {
                throw new ResourceConflictException("Đã tồn tại lời mời hoặc quan hệ bạn bè");
            }
        }

        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        NotificationMessageResponse data = NotificationMessageResponse.builder()
                .status(NotificationsStatus.FRIEND_REQUEST)
                .relatedUsername((requester.getFirstName() != null ? requester.getFirstName() : requester.getUsername()))
                .build();
        SocketResponse<NotificationMessageResponse> response = SocketResponse.notifications("Lời mời kết bạn mới", data);
        eventPublisher.publishEvent(new FriendshipEvent(this, addresseeUsername, "/queue/notifications", response));
    }

    @Override
    @Transactional
    public void acceptFriendRequest(String acceptorUsername, String requesterUsername) {
        UserEntity acceptor = getUser(acceptorUsername);
        UserEntity requester = getUser(requesterUsername);

        FriendshipEntity friendship = friendshipRepository.findByUsers(acceptor, requester)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời"));

        if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
            throw new ResourceConflictException("Đã là bạn bè");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        redisTemplate.opsForSet().add("friends:" + acceptorUsername, requesterUsername);
        redisTemplate.opsForSet().add("friends:" + requesterUsername, acceptorUsername);

        NotificationMessageResponse data = NotificationMessageResponse.builder()
                .status(NotificationsStatus.FRIEND_ACCEPTED)
                .relatedUsername((acceptor.getFirstName() != null ? acceptor.getFirstName() : acceptor.getUsername()))
                .build();
        SocketResponse<NotificationMessageResponse> response = SocketResponse.notifications("Đã chấp nhận kết bạn", data);


        eventPublisher.publishEvent(new FriendshipEvent(this, requesterUsername, "/queue/notifications", response));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getSentRequests(String currentUsername, int page, int size) {
        UserEntity currentUser = getUser(currentUsername);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createAt").descending());

        Page<FriendshipEntity> pageResult = friendshipRepository.findByRequesterAndStatus(currentUser, FriendshipStatus.PENDING, pageable);

        List<UserSummaryResponse> content = pageResult.getContent().stream()
                .map(f -> userMapper.toUserSummaryResponse(f.getAddressee()))
                .collect(Collectors.toList());

        return buildPageResponse(pageResult, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getPendingRequests(String currentUsername, int page, int size, String sortBy) {
        UserEntity currentUser = getUser(currentUsername);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createAt"));

        Page<FriendshipEntity> pageResult = friendshipRepository.findByAddresseeAndStatus(currentUser, FriendshipStatus.PENDING, pageable);

        List<UserSummaryResponse> content = pageResult.getContent().stream()
                .map(f -> userMapper.toUserSummaryResponse(f.getRequester()))
                .collect(Collectors.toList());

        return buildPageResponse(pageResult, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> getFriendsList(String currentUsername, int page, int size, String sortBy) {
        UserEntity currentUser = getUser(currentUsername);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy != null ? sortBy : "createAt"));

        Page<FriendshipEntity> pageResult = friendshipRepository.findAllAcceptedFriendships(currentUser, pageable);

        List<UserSummaryResponse> content = pageResult.getContent().stream()
                .map(f -> {
                    UserEntity friend = f.getRequester().getUsername().equals(currentUsername)
                            ? f.getAddressee() : f.getRequester();
                    return userMapper.toUserSummaryResponse(friend);
                })
                .collect(Collectors.toList());

        return buildPageResponse(pageResult, content);
    }

    @Override
    @Transactional
    public void deleteFriendship(String currentUsername, String targetUsername) {
        UserEntity user1 = getUser(currentUsername);
        UserEntity user2 = getUser(targetUsername);

        FriendshipEntity friendship = friendshipRepository.findByUsers(user1, user2)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ"));

        boolean isAccepted = friendship.getStatus() == FriendshipStatus.ACCEPTED;
        boolean isRequester = friendship.getRequester().getUsername().equals(currentUsername);

        redisTemplate.opsForSet().remove("friends:" + currentUsername, targetUsername);
        redisTemplate.opsForSet().remove("friends:" + targetUsername, currentUsername);

        friendshipRepository.delete(friendship);

        if (isAccepted) {
            NotificationMessageResponse data = NotificationMessageResponse.builder()
                    .status(NotificationsStatus.UNFRIENDED)
                    .relatedUsername(currentUsername)
                    .build();

            eventPublisher.publishEvent(new FriendshipEvent(this, targetUsername, "/queue/notifications",
                    SocketResponse.notifications("Đã hủy kết bạn", data)));

        } else {
            if (isRequester) {
                NotificationMessageResponse data = NotificationMessageResponse.builder()
                        .status(NotificationsStatus.REQUEST_CANCELLED)
                        .relatedUsername(currentUsername)
                        .build();

                eventPublisher.publishEvent(new FriendshipEvent(this, targetUsername, "/queue/notifications",
                        SocketResponse.notifications("Đã rút lại lời mời kết bạn", data)));

            } else {
                NotificationMessageResponse data = NotificationMessageResponse.builder()
                        .status(NotificationsStatus.REQUEST_REJECTED)
                        .relatedUsername(currentUsername)
                        .build();

                eventPublisher.publishEvent(new FriendshipEvent(this, targetUsername, "/queue/notifications",
                        SocketResponse.notifications("Đã từ chối lời mời kết bạn", data)));
            }
        }
    }

    @Override
    @Transactional
    public void blockUser(String blockerUsername, String targetUsername) {
        UserEntity blocker = getUser(blockerUsername);
        UserEntity target = getUser(targetUsername);

        FriendshipEntity friendship = friendshipRepository.findByUsers(blocker, target)
                .orElse(new FriendshipEntity());

        friendship.setRequester(blocker);
        friendship.setAddressee(target);
        friendship.setStatus(FriendshipStatus.BLOCKED);

        friendshipRepository.save(friendship);

        redisTemplate.opsForSet().remove("friends:" + blockerUsername, targetUsername);
        redisTemplate.opsForSet().remove("friends:" + targetUsername, blockerUsername);

        log.info("User {} blocked {}", blockerUsername, targetUsername);
    }

    @Override
    public boolean isFriend(String user1, String user2) {

        Boolean isMember = redisTemplate.opsForSet().isMember("friends:" + user1, user2);
        if (Boolean.TRUE.equals(isMember)) return true;

        boolean existsInDb = friendshipRepository.existsFriendship(user1, user2);

        if (existsInDb) {
            redisTemplate.opsForSet().add("friends:" + user1, user2);
            redisTemplate.opsForSet().add("friends:" + user2, user1);
        }
        return existsInDb;
    }

    private UserEntity getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
    }

    private <T> PageResponse<T> buildPageResponse(Page<?> pageResult, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }
}