package com.web.backend.service.impl;

import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.web.backend.common.UserStatus;
import com.web.backend.controller.response.PageResponse;
import com.web.backend.controller.response.UserDetailResponse;
import com.web.backend.controller.response.UserSummaryResponse;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.specification.SearchRepository;
import com.web.backend.repository.specification.UserSpecificationsBuilder;
import com.web.backend.service.SearchUserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchUserServiceImpl implements SearchUserService {

    private final UserRepository userRepository;

    private final SearchRepository searchRepository;

    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String keyword, int page, int size, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "id"));

        Page<UserEntity> pageResult = userRepository.searchUsersByKeyword(keyword, UserStatus.INACTIVE, pageable);

        List<UserSummaryResponse> content = pageResult.getContent().stream()
                .map(userMapper::toUserSummaryResponse)
                .collect(Collectors.toList());

        return PageResponse.<UserSummaryResponse>builder()
                .content(content)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Override
    public PageResponse<UserDetailResponse> advanceSearchWithSpecifications(Pageable pageable, String[] user,
            String[] address) {

        if (user != null && address != null) {
            List<UserDetailResponse> content = searchRepository.searchUserByCriteriaWithJoin(pageable, user, address);
            Long totalPage = searchRepository.countUserJoinAddress(user, address);
            return PageResponse.<UserDetailResponse>builder()
                    .content(content)
                    .pageNo(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .totalElements(totalPage)
                    .totalPages((int) Math.ceil((double) totalPage / pageable.getPageSize()))
                    .last(content.size() < pageable.getPageSize())
                    .build();

        } else if (user != null) {
            UserSpecificationsBuilder userSpecificationsBuilder = new UserSpecificationsBuilder();
            Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(\\*?)(.*?)(\\*?)");
            for (String str : user) {
                Matcher matcher = pattern.matcher(str);
                if (matcher.find()) {
                    userSpecificationsBuilder.with(matcher.group(1), matcher.group(2), matcher.group(4),
                            matcher.group(3), matcher.group(5));
                }
            }
            Specification<UserEntity> spec = userSpecificationsBuilder.build();
            if (spec == null) {
                return convertToPageResponse(userRepository.findAll(Objects.requireNonNull(pageable)));
            }
            Page<UserEntity> users = userRepository.findAll(spec, Objects.requireNonNull(pageable));

            return convertToPageResponse(users);
        }
        return convertToPageResponse(userRepository.findAll(Objects.requireNonNull(pageable)));
    }

    private PageResponse<UserDetailResponse> convertToPageResponse(Page<UserEntity> pageResult) {
        List<UserDetailResponse> content = pageResult.getContent().stream()
                .map(userMapper::toUserDetailResponse)
                .collect(Collectors.toList());
        return PageResponse.<UserDetailResponse>builder()
                .content(content)
                .pageNo(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }
}
