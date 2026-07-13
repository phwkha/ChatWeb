package com.web.backend.repository.specification;

import java.util.ArrayList;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.web.backend.controller.response.UserDetailResponse;
import com.web.backend.mapper.UserMapper;
import com.web.backend.model.AddressEntity;
import com.web.backend.model.UserEntity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserMapper userMapper;

    public List<UserDetailResponse> searchUserByCriteriaWithJoin(Pageable pageable, String[] user,
            String[] address) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserEntity> query = criteriaBuilder.createQuery(UserEntity.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);
        Join<AddressEntity, UserEntity> addressRoot = userRoot.join("addresses");

        List<Predicate> userPreList = new ArrayList<>();
        Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(\\*?)(.*?)(\\*?)");
        for (String u : user) {
            Matcher matcher = pattern.matcher(u);
            if (matcher.find()) {
                SpecSearchCriteria searchCriteria = new SpecSearchCriteria(matcher.group(1), matcher.group(2),
                        matcher.group(3), matcher.group(4), matcher.group(5));
                userPreList.add(toUserPredicate(userRoot, criteriaBuilder, searchCriteria));
            }
        }

        List<Predicate> addressPreList = new ArrayList<>();
        for (String a : address) {
            Matcher matcher = pattern.matcher(a);
            if (matcher.find()) {
                SpecSearchCriteria searchCriteria = new SpecSearchCriteria(matcher.group(1), matcher.group(2),
                        matcher.group(3), matcher.group(4), matcher.group(5));
                addressPreList.add(toAddressPredicate(addressRoot, criteriaBuilder, searchCriteria));
            }
        }

        Predicate userPre = criteriaBuilder.or(userPreList.toArray(new Predicate[0]));
        Predicate addPre = criteriaBuilder.or(addressPreList.toArray(new Predicate[0]));
        Predicate finalPre = criteriaBuilder.and(userPre, addPre);

        query.where(finalPre);

        return entityManager.createQuery(query)
                .setFirstResult(pageable.getPageNumber())
                .setMaxResults(pageable.getPageSize())
                .getResultList().stream().map(userMapper::toUserDetailResponse).collect(Collectors.toList());

    }

    public long countUserJoinAddress(String[] user, String[] address) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<UserEntity> userRoot = query.from(UserEntity.class);
        Join<AddressEntity, UserEntity> addressRoot = userRoot.join("addresses");

        List<Predicate> userPreList = new ArrayList<>();

        Pattern pattern = Pattern.compile("(\\w+?)([<:>~!])(\\*?)(.*?)(\\*?)");
        for (String u : user) {
            Matcher matcher = pattern.matcher(u);
            if (matcher.find()) {
                SpecSearchCriteria searchCriteria = new SpecSearchCriteria(matcher.group(1), matcher.group(2),
                        matcher.group(3), matcher.group(4), matcher.group(5));
                userPreList.add(toUserPredicate(userRoot, builder, searchCriteria));
            }
        }

        List<Predicate> addressPreList = new ArrayList<>();
        for (String a : address) {
            Matcher matcher = pattern.matcher(a);
            if (matcher.find()) {
                SpecSearchCriteria searchCriteria = new SpecSearchCriteria(matcher.group(1), matcher.group(2),
                        matcher.group(3), matcher.group(4), matcher.group(5));
                addressPreList.add(toAddressPredicate(addressRoot, builder, searchCriteria));
            }
        }

        Predicate userPre = builder.or(userPreList.toArray(new Predicate[0]));
        Predicate addPre = builder.or(addressPreList.toArray(new Predicate[0]));
        Predicate finalPre = builder.and(userPre, addPre);

        query.select(builder.count(userRoot));
        query.where(finalPre);

        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate toUserPredicate(Root<UserEntity> root, CriteriaBuilder builder, SpecSearchCriteria criteria) {
        log.info("-------------- toUserPredicate --------------");
        return switch (criteria.getOperation()) {
            case EQUALITY -> builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case NEGATION -> builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
            case GREATER_THAN -> builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LIKE -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue().toString() + "%");
            case STARTS_WITH -> builder.like(root.get(criteria.getKey()), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue());
            case CONTAINS -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
        };
    }

    private Predicate toAddressPredicate(Join<AddressEntity, UserEntity> root, CriteriaBuilder builder,
            SpecSearchCriteria criteria) {
        log.info("-------------- toAddressPredicate --------------");
        return switch (criteria.getOperation()) {
            case EQUALITY -> builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case NEGATION -> builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
            case GREATER_THAN -> builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LESS_THAN -> builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString());
            case LIKE -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue().toString() + "%");
            case STARTS_WITH -> builder.like(root.get(criteria.getKey()), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue());
            case CONTAINS -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
        };
    }
}
