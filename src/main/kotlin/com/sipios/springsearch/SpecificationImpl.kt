package com.sipios.springsearch

import com.sipios.springsearch.anotation.SearchSpec
import com.sipios.springsearch.predicate.PredicateBuilder
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.web.server.ResponseStatusException

/**
 * Implementation of the JPA Specification based on a Search Criteria
 *
 * @see Specification
 *
 * @param <T>The class on which the specification will be applied</T>
 * */
class SpecificationImpl<T>(
    private val criteria: SearchCriteria,
    private val searchSpecAnnotation: SearchSpec,
    private val predicateBuilder: PredicateBuilder<T>
) : Specification<T> {

    @Throws(ResponseStatusException::class)
    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        builder: CriteriaBuilder
    ): Predicate? {
        return this.predicateBuilder.toPredicate(criteria, searchSpecAnnotation, root, query, builder)
    }
}
