package com.nbadal.ktlint

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class KtlintTest {
    @Test
    fun `test Case sensitive relocated fastutil classes can be loaded correctly`() {
        val classLoader = KtlintTest::class.java.classLoader

        val lowerClass = classLoader.loadClass("shadow.org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.o")
        val upperClass = classLoader.loadClass("shadow.org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.O")
        val annotationRuleClass = Class.forName("com.pinterest.ktlint.ruleset.standard.V1_8_0.rules.AnnotationRule")

        assertThat(lowerClass.name).isEqualTo("shadow.org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.o")
        assertThat(upperClass.name).isEqualTo("shadow.org.jetbrains.kotlin.it.unimi.dsi.fastutil.ints.O")
        assertThat(annotationRuleClass).isNotNull()
    }
}

