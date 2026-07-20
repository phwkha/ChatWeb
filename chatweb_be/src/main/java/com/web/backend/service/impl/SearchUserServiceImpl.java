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
import com.web.backend.repository.specification.AddressSpecification;
import com.web.backend.repository.specification.SearchSpecificationsBuilder;
import com.web.backend.repository.specification.UserSpecification;
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

    private final UserMapper userMapper;

    private static final String DESC_STRING = "desc";

    private static final String USERNAME_STRING = "username";

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> searchUsers(String keyword, int page, int size, String sortDir) {
        Sort.Direction direction = sortDir.equalsIgnoreCase(DESC_STRING) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, USERNAME_STRING));

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

        Specification<UserEntity> finalSpec = Specification.unrestricted();
        Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(\\*?)(.*?)(\\*?)");

        if (user != null && user.length > 0) {
            SearchSpecificationsBuilder userBuilder = new SearchSpecificationsBuilder();
            for (String str : user) {
                Matcher matcher = pattern.matcher(str);
                if (matcher.find()) {
                    userBuilder.with(matcher.group(1), matcher.group(2), matcher.group(4),
                            matcher.group(3), matcher.group(5));
                }
            }
            if (!userBuilder.params.isEmpty()) {
                finalSpec = finalSpec.and(new UserSpecification(userBuilder.params));
            }
        }

        if (address != null && address.length > 0) {
            SearchSpecificationsBuilder addressBuilder = new SearchSpecificationsBuilder();
            for (String str : address) {
                Matcher matcher = pattern.matcher(str);
                if (matcher.find()) {
                    addressBuilder.with(matcher.group(1), matcher.group(2), matcher.group(4),
                            matcher.group(3), matcher.group(5));
                }
            }
            if (!addressBuilder.params.isEmpty()) {
                finalSpec = finalSpec.and(new AddressSpecification(addressBuilder.params));
            }
        }
        Page<UserEntity> users = userRepository.findAll(finalSpec, Objects.requireNonNull(pageable));
        return convertToPageResponse(users);
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
