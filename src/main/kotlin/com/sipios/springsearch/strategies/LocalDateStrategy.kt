package com.sipios.springsearch.strategies

import com.sipios.springsearch.SearchOperation
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.reflect.KClass

class LocalDateStrategy : ParsingStrategy {
    companion object {
        val LENGTH_LOCAL_DATE = LocalDate.now().toString().length
        val LENGTH_LOCAL_DATE_TIME = LocalDateTime.now().toString().length
    }

    override fun buildPredicate(
        builder: CriteriaBuilder,
        path: Path<*>,
        fieldName: String,
        ops: SearchOperation?,
        value: Any?
    ): Predicate? {
        return when (ops) {
            SearchOperation.GREATER_THAN -> builder.greaterThan(path[fieldName], value as LocalDate)
            SearchOperation.LESS_THAN -> builder.lessThan(path[fieldName], value as LocalDate)
            SearchOperation.GREATER_THAN_EQUALS -> builder.greaterThanOrEqualTo(path[fieldName], value as LocalDate)
            SearchOperation.LESS_THAN_EQUALS -> builder.lessThanOrEqualTo(path[fieldName], value as LocalDate)
            else -> super.buildPredicate(builder, path, fieldName, ops, value)
        }
    }

    override fun parse(value: String?, fieldClass: KClass<out Any>): Any? {
        if (value == SearchOperation.NULL) return value

        if (value?.length == LENGTH_LOCAL_DATE) {
            return LocalDate.parse(value)
        }

        if (value?.length == LENGTH_LOCAL_DATE_TIME || value?.length == LENGTH_LOCAL_DATE_TIME - 10) {
            return LocalDateTime.parse(value).toLocalDate()
        }

        val instant = Instant.parse(value)
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
}
