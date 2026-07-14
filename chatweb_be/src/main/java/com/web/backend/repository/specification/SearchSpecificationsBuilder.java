package com.web.backend.repository.specification;

import java.util.ArrayList;
import java.util.List;

import static com.web.backend.repository.specification.SearchOperation.*;

public final class SearchSpecificationsBuilder {

    public final List<SpecSearchCriteria> params;

    public SearchSpecificationsBuilder() {
        params = new ArrayList<>();
    }

    public SearchSpecificationsBuilder with(final String key, final String operation, final Object value,
            final String prefix, final String suffix) {
        return with(null, key, operation, value, prefix, suffix);
    }

    public SearchSpecificationsBuilder with(final String orPredicate, final String key, final String operation,
            final Object value, final String prefix, final String suffix) {
        SearchOperation searchOperation = SearchOperation.getSimpleOperation(operation.charAt(0));
        if (searchOperation != null) {
            if (searchOperation == EQUALITY) {
                final boolean startWithAsterisk = prefix != null && prefix.contains(ZERO_OR_MORE_REGEX);
                final boolean endWithAsterisk = suffix != null && suffix.contains(ZERO_OR_MORE_REGEX);

                if (startWithAsterisk && endWithAsterisk) {
                    searchOperation = CONTAINS;
                } else if (startWithAsterisk) {
                    searchOperation = ENDS_WITH;
                } else if (endWithAsterisk) {
                    searchOperation = STARTS_WITH;
                }
            }
            params.add(new SpecSearchCriteria(orPredicate, key, searchOperation, value));
        }
        return this;
    }
}
