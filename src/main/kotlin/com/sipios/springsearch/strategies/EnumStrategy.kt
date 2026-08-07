package com.sipios.springsearch.strategies

import com.sipios.springsearch.SearchOperation
import java.util.Locale
import kotlin.reflect.KClass

class EnumStrategy : ParsingStrategy {
    override fun parse(value: String?, fieldClass: KClass<out Any>): Any? {
        if (value == SearchOperation.NULL) return value
        return try {
            Class
                .forName(fieldClass.qualifiedName)
                .getMethod("valueOf", String::class.java)
                .invoke(null, value?.uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            value?.uppercase(Locale.getDefault())
        }
    }
}
