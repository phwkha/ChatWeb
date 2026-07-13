package com.web.backend.repository.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.web.backend.model.UserEntity;

@Getter
@AllArgsConstructor
public class UserSpecification implements Specification<UserEntity> {

    private SpecSearchCriteria criteria;

    @Override
    public Predicate toPredicate(@NonNull final Root<UserEntity> root, @Nullable final CriteriaQuery<?> query,
            @NonNull final CriteriaBuilder builder) {
        return switch (criteria.getOperation()) {
            case EQUALITY -> builder.equal(root.get(criteria.getKey()), criteria.getValue());
            case NEGATION -> builder.notEqual(root.get(criteria.getKey()), criteria.getValue());
            case GREATER_THAN ->
                builder.greaterThan(root.get(criteria.getKey()), criteria.getValue().toString().toLowerCase());
            case LESS_THAN ->
                builder.lessThan(root.get(criteria.getKey()), criteria.getValue().toString().toLowerCase());
            case LIKE ->
                builder.like(root.get(criteria.getKey()), "%" + criteria.getValue().toString().toLowerCase() + "%");
            case STARTS_WITH -> builder.like(root.get(criteria.getKey()), criteria.getValue() + "%");
            case ENDS_WITH -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue());
            case CONTAINS -> builder.like(root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
        };
    }
}
