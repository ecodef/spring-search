package com.sipios.springsearch.predicate

import com.sipios.springsearch.SearchCriteria
import com.sipios.springsearch.anotation.SearchSpec
import com.sipios.springsearch.strategies.ParsingStrategy
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Path
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.persistence.metamodel.Attribute
import jakarta.persistence.metamodel.ManagedType
import org.hibernate.metamodel.model.domain.PersistentAttribute
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.ArrayList
import kotlin.reflect.KClass

open class PredicateBuilder<T> {

    open fun toPredicate(
        criteria: SearchCriteria,
        searchSpec: SearchSpec,
        root: Root<T>,
        query: CriteriaQuery<*>,
        builder: CriteriaBuilder
    ): Predicate? {
        val nestedKey = criteria.key.split(".")
        val nestedRoots = getNestedRoots(root, nestedKey)
        val nestedRoot = nestedRoots.last()
        val criteriaKey = nestedKey[nestedKey.size - 1]
        val attribute = getAttributeForField(nestedRoot, criteriaKey)
        val attributeName = attribute?.name ?: criteriaKey
        val fieldClass = getPathForField(nestedRoot, attribute).javaType.kotlin
        val isCollectionField = isCollectionType(nestedRoot.javaType, attributeName)
        val strategy = ParsingStrategy.Companion.getStrategy(fieldClass, searchSpec, isCollectionField)
        val value = parseValue(strategy, fieldClass, attributeName, criteria.value)
        return strategy.buildPredicate(builder, nestedRoot, attributeName, criteria.operation, value)
    }

    protected fun getNestedRoots(
        root: Root<T>,
        key: List<String>
    ): List<Path<*>> {
        val prefix = ArrayList(key)
        prefix.removeAt(key.size - 1)
        val paths = mutableListOf<Path<*>>()
        var path: Path<*> = root

        paths.add(path)
        for (s in prefix) {
            path = getPathForField(path, s)
            paths.add(path)
        }

        return paths
    }

    protected fun <T> getPathForField(path: Path<T>, field: String?): Path<T> {
        val attribute = getAttributeForField(path, field)
        return path[attribute?.name]
    }

    protected fun <T> getPathForField(path: Path<T>, attribute: Attribute<*, *>?): Path<T> {
        return path[attribute?.name]
    }

    protected fun <T> getAttributeForField(
        path: Path<T>,
        field: String?
    ): Attribute<in T, *>? {
        val model = getModel(path) ?: return null
        val attributes = model.attributes
        val attribute = attributes.find { a -> a.name.contentEquals(field, true) }

        if (attribute == null) {
            throw NoSuchFieldException(String.format("Field %s not found on %s", field, path.toString()))
        }

        return attribute
    }

    protected fun <T> getModel(path: Path<T>): ManagedType<T>? {
        if (path.model is ManagedType<*>) {
            @Suppress("UNCHECKED_CAST")
            return path.model as ManagedType<T>
        }

        if (path.model is PersistentAttribute<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return (path.model as PersistentAttribute<*, *>).valueGraphType as ManagedType<T>
        }

        return null
    }

    protected fun isCollectionType(clazz: Class<*>, fieldName: String): Boolean {
        try {
            val field = clazz.getDeclaredField(fieldName)
            val type = field.type
            return Collection::class.java.isAssignableFrom(type) || type.isArray
        } catch (e: NoSuchFieldException) {
            return false
        }
    }

    protected fun parseValue(
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
                "Could not parse input for the field $criteriaKey as a ${fieldClass.simpleName}",
                e
            )
        }
    }

}