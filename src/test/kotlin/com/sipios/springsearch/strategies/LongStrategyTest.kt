package com.sipios.springsearch.strategies

import com.sipios.springsearch.SearchOperation
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Path
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class LongStrategyTest {
    private val strategy = LongStrategy()

    @Test
    fun parsesLongValuesAndNull() {
        Assertions.assertEquals(42L, strategy.parse("42", Long::class))
        Assertions.assertEquals(SearchOperation.NULL, strategy.parse(SearchOperation.NULL, Long::class))
        Assertions.assertNull(strategy.parse(null as String?, Long::class))
    }

    @Test
    fun buildsComparisonPredicates() {
        val builder = Mockito.mock(CriteriaBuilder::class.java)
        val path = Mockito.mock(Path::class.java)

        strategy.buildPredicate(builder, path, "value", SearchOperation.GREATER_THAN, 1L)
        strategy.buildPredicate(builder, path, "value", SearchOperation.LESS_THAN, 2L)
        strategy.buildPredicate(builder, path, "value", SearchOperation.GREATER_THAN_EQUALS, 3L)
        strategy.buildPredicate(builder, path, "value", SearchOperation.LESS_THAN_EQUALS, 4L)

        Mockito.verify(builder).greaterThan(path["value"], 1L)
        Mockito.verify(builder).lessThan(path["value"], 2L)
        Mockito.verify(builder).greaterThanOrEqualTo(path["value"], 3L)
        Mockito.verify(builder).lessThanOrEqualTo(path["value"], 4L)
    }
}
