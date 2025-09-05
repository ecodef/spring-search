package com.sipios.springsearch

import com.sipios.springsearch.anotation.SearchSpec
import com.sipios.springsearch.strategies.ParsingStrategy
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.ManagedType
import java.util.ArrayList
import kotlin.reflect.KClass
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Implementation of the JPA Specification based on a Search Criteria
 *
 * @see Specification
 *
 * @param <T>The class on which the specification will be applied</T>
 * */
class SpecificationImpl<T>(private val criteria: SearchCriteria, private val searchSpecAnnotation: SearchSpec) :
    Specification<T> {
    @Throws(ResponseStatusException::class)
    override fun toPredicate(
        root: Root<T>,
        query: CriteriaQuery<*>,
        builder: CriteriaBuilder
    ): Predicate? {
        val nestedKey = criteria.key.split(".")
        val nestedRoot = getNestedRoot(root, nestedKey)
        val criteriaKey = nestedKey[nestedKey.size - 1]
        val attribute = getAttributeForField(nestedRoot, criteriaKey)
        val attributeName = attribute?.name ?: criteriaKey
        val fieldClass = getPathForField(nestedRoot, attribute).javaType.kotlin
        val isCollectionField = isCollectionType(nestedRoot.javaType, attributeName)
        val strategy = ParsingStrategy.getStrategy(fieldClass, searchSpecAnnotation, isCollectionField)
        val value = parseValue(strategy, fieldClass, attributeName, criteria.value)
        return strategy.buildPredicate(builder, nestedRoot, attributeName, criteria.operation, value)
    }

    private fun getNestedRoot(
        root: Root<T>,
        nestedKey: List<String>
    ): Path<*> {
        val prefix = ArrayList(nestedKey)
        prefix.removeAt(nestedKey.size - 1)
        var path: Path<*> = root
        for (s in prefix) {
            path = getPathForField(path, s)
        }
        return path
    }

    private fun <T> getPathForField(path: Path<T>, field: String?): Path<T> {
        val attribute = getAttributeForField(path, field)
        return path[attribute?.name]
    }

    private fun <T> getPathForField(path: Path<T>, attribute: Attribute<*, *>?): Path<T> {
        return path[attribute?.name]
    }

    private fun <T> getAttributeForField(
        path: Path<T>,
        field: String?
    ): Attribute<in T, *>? {
        if (path.model !is ManagedType<*>) {
            return null
        }

        val model = path.model as ManagedType<T>
        val attributes = model.attributes
        val attribute = attributes.find { a -> a.name.contentEquals(field, true) }

        if (attribute == null) {
            throw NoSuchFieldException(String.format("Field %s not found on %s", field, path.toString()))
        }

        return attribute
    }

    private fun isCollectionType(clazz: Class<*>, fieldName: String): Boolean {
        try {
            val field = clazz.getDeclaredField(fieldName)
            val type = field.type
            return Collection::class.java.isAssignableFrom(type) || type.isArray
        } catch (e: NoSuchFieldException) {
            return false
        }
    }

    private fun parseValue(
        strategy: ParsingStrategy,
        fieldClass: KClass<out Any>,
        criteriaKey: String,
        value: Any?
    ): Any? {
        return try {
            if (value is List<*>) {
                strategy.parse(value, fieldClass)
            } else {
                strategy.parse(value?.toString(), fieldClass)
            }
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Could not parse input for the field $criteriaKey as a ${fieldClass.simpleName}"
            )
        }
    }
}
