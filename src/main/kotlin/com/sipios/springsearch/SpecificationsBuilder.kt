package com.sipios.springsearch

import com.sipios.springsearch.anotation.SearchSpec
import com.sipios.springsearch.predicate.PredicateBuilder
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

class SpecificationsBuilder<U>(
    searchSpecAnnotation: SearchSpec,
    predicateBuilder: PredicateBuilder<U>
) {

    private var specs: Specification<U> = NullSpecification()
    private var parser: CriteriaParser<U> = CriteriaParser(searchSpecAnnotation, predicateBuilder)

    fun withSearch(search: String): SpecificationsBuilder<U> {
        specs = parser.parse(search)

        return this
    }

    /**
     * This function expect a search string to have been provided.
     * The search string has been transformed into an Expression Queue with the format: [OR, value>100, AND, value<1000, label:*MONO*]
     *
     * @return A list of specification used to filter the underlying object using JPA specifications
     */
    fun build(): Specification<U> {
        return specs
    }

    constructor(searchSpecAnnotation: SearchSpec) : this(searchSpecAnnotation, PredicateBuilder())
}

class NullSpecification<T> : Specification<T> {
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, criteriaBuilder: CriteriaBuilder): Predicate? {
        return null
    }
}
