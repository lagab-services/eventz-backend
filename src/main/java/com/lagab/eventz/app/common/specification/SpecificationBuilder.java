package com.lagab.eventz.app.common.specification;

import org.springframework.data.jpa.domain.Specification;

public class SpecificationBuilder<T> {

    private Specification<T> specification;

    public SpecificationBuilder() {
        this.specification = null;
    }

    public SpecificationBuilder<T> with(Specification<T> spec) {
        if (specification == null) {
            specification = spec;
        } else {
            specification = specification.and(spec);
        }
        return this;
    }

    public SpecificationBuilder<T> withOptional(Specification<T> spec, boolean condition) {
        if (condition) {
            return with(spec);
        }
        return this;
    }

    public Specification<T> build() {
        return specification;
    }
}
