package com.nbadal.ktlint

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

internal const val ANNOTATED_EXPRESSION = "ANNOTATED_EXPRESSION"
internal const val ANNOTATION = "ANNOTATION"
internal const val ANNOTATION_ENTRY = "ANNOTATION_ENTRY"
internal const val BLOCK = "BLOCK"
internal const val CONSTRUCTOR_CALLEE = "CONSTRUCTOR_CALLEE"
internal const val FILE = "kotlin.FILE"
internal const val FILE_ANNOTATION_LIST = "FILE_ANNOTATION_LIST"
internal const val FUN = "FUN"
internal const val IMPORT_KEYWORD = "import"
internal const val MODIFIER_LIST = "MODIFIER_LIST"
internal const val SCRIPT = "SCRIPT"
internal const val VALUE_ARGUMENT_LIST = "VALUE_ARGUMENT_LIST"

@Suppress("UnstableApiUsage")
internal fun PsiElement.elementTypeName() = elementType?.debugName

internal fun PsiElement.findElementType(debugName: String) = children.firstOrNull { it.elementTypeName() == debugName }
