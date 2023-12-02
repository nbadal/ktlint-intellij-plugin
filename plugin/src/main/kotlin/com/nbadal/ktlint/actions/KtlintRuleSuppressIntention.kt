package com.nbadal.ktlint.actions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.nbadal.ktlint.ANNOTATED_EXPRESSION
import com.nbadal.ktlint.ANNOTATION
import com.nbadal.ktlint.ANNOTATION_ENTRY
import com.nbadal.ktlint.BLOCK
import com.nbadal.ktlint.CONSTRUCTOR_CALLEE
import com.nbadal.ktlint.FILE
import com.nbadal.ktlint.FILE_ANNOTATION_LIST
import com.nbadal.ktlint.FUN
import com.nbadal.ktlint.IMPORT_KEYWORD
import com.nbadal.ktlint.MODIFIER_LIST
import com.nbadal.ktlint.SCRIPT
import com.nbadal.ktlint.VALUE_ARGUMENT_LIST
import com.nbadal.ktlint.elementTypeName
import com.nbadal.ktlint.findElementType
import com.pinterest.ktlint.rule.engine.api.LintError

class KtlintRuleSuppressIntention(
    private val lintError: LintError,
) : BaseIntentionAction(),
    HighPriorityAction {
    override fun getFamilyName() = "KtLint"

    override fun getText() = "Suppress '${lintError.ruleId.value}'"

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ): Boolean {
        // Skip if no error element can be located for the error offset
        return psiFile.findElementAtLintErrorOffset() != null
    }

    private fun PsiFile.findElementAtLintErrorOffset(): PsiElement? =
        viewProvider
            .document
            ?.takeIf { lintError.line <= it.lineCount }
            ?.let { doc ->
                findElementAt(doc.getLineStartOffset(lintError.line - 1) + lintError.col - 1)
                    ?.let { psiElement ->
                        // The "@Suppress" annotation can not be placed on a whitespace. So get the next non-whitespace sibling
                        psiElement
                            .takeIf { it is PsiWhiteSpace }
                            ?.nextSibling
                            ?: psiElement
                    }
            }

    override fun invoke(
        project: Project,
        editor: Editor?,
        psiFile: PsiFile,
    ) {
        psiFile
            .findElementAtLintErrorOffset()
            ?.let { psiElement ->
                psiElement
                    .findAssociatedSuppressAnnotationValueArgumentList()
                    ?.let { suppressValueArgumentList ->
                        // The "Suppress" annotation is not repeatable. The code will not compile if a second suppress annotation is added
                        // to the same element. As of that the new rule id has to be merged into this annotation.
                        val suppressions =
                            suppressValueArgumentList
                                .children
                                .map { it.text }
                                .toList()
                        psiFile
                            .createModifierListWithSuppressAnnotation(suppressions)
                            .findElementType(ANNOTATION_ENTRY)
                            ?.findElementType(VALUE_ARGUMENT_LIST)
                            ?.let { newValueArgumentList ->
                                suppressValueArgumentList.replace(newValueArgumentList)
                            }
                        return
                    }

                // Suppress annotation does not yet exist
                when {
                    psiElement.elementTypeName() == IMPORT_KEYWORD -> {
                        val annotation = psiFile.createFileSuppressAnnotation()
                        psiFile.firstChild.addBefore(annotation, null)
                    }

                    psiElement.parent.elementTypeName() == MODIFIER_LIST -> {
                        val modifierListWithSuppressAnnotation = psiFile.createModifierListWithSuppressAnnotation()
                        psiElement
                            .parent
                            .addBefore(
                                modifierListWithSuppressAnnotation
                                    .findElementType(ANNOTATION_ENTRY)
                                    ?.takeIf { it.text.startsWith("@Suppress") }
                                    ?: throw IllegalArgumentException(),
//                                modifierListWithSuppressAnnotation
//                                    .nextSibling
//                                    .takeIf { it.text == "\n" }
//                                    ?: throw IllegalArgumentException(),
                                psiElement,
                            )
                    }

                    else -> {
                        val modifierListWithSuppressAnnotation = psiFile.createModifierListWithSuppressAnnotation()
                        psiElement
                            .parent
                            .addRangeBefore(
                                modifierListWithSuppressAnnotation
                                    .takeIf { it.text.startsWith("@Suppress") }
                                    ?: throw IllegalArgumentException(),
                                modifierListWithSuppressAnnotation
                                    .nextSibling
                                    .takeIf { it.text == "\n" }
                                    ?: throw IllegalArgumentException(),
                                psiElement,
                            )
                    }
                }
            }
    }

    private fun PsiElement.findAssociatedSuppressAnnotationValueArgumentList(): PsiElement? {
        var current = this

        if (current.elementTypeName() == IMPORT_KEYWORD) {
            while (current.parent != null && current.elementTypeName() != FILE) {
                current = current.parent
            }
        }

        current
            .findSuppressAnnotationValueArgumentList()
            ?.let { return it }

        current
            .takeUnless { it.elementTypeName() == FILE }
            ?.parent
            ?.takeIf { it.elementTypeName() == ANNOTATED_EXPRESSION || it.elementTypeName() == MODIFIER_LIST }
            ?.findSuppressAnnotationValueArgumentList()
            ?.let { return it }
//        current
//            .takeUnless { it.elementTypeName() == FILE }
//            ?.parent
//            ?.let { parent ->
//                parent
//                    .takeIf { it.elementTypeName() == ANNOTATED_EXPRESSION || it.elementTypeName() == MODIFIER_LIST }
//                    ?.findSuppressAnnotationValueArgumentList()
//                    ?.let { return it }
//                parent
//                    .findElementType(MODIFIER_LIST)
//                    .findSuppressAnnotationValueArgumentList()
//                    ?.let { return it }
//            }

        return null
    }

    // Locates a "Suppress" annotation in any of constructs below
    //     @file:Suppress("ktlint:standard:indent")
    // or
    //     @file:[
    //         Suppress("ktlint:standard:indent")
    //         // other annotation(s)
    //     ]
    // or
    //     @Suppress("ktlint:standard:indent")
    //     fun foo() {} // or any other declaration
    // or
    //     @[
    //         SuppressWarnings("...")
    //         // other annotation(s)
    //     ]
    //     fun foo() {} // or any other declaration
    // or
    //    val foo =
    //        @Suppress("ktlint:standard:indent")
    //        "foo" // or any other expression
    // or
    //    val foo =
    //        @[
    //            Suppress("ktlint:standard:indent")
    //            // other annotation(s)
    //        ]
    //        "foo" // or any other expression
    // or
    //    val foo =
    //        @Suppress("ktlint:standard:indent")
    //        "foo" // or any other expression
    // or
    //    val foo =
    //        @[
    //            Suppress("ktlint:standard:indent")
    //            // other annotation(s)
    //        ]
    //        "foo" // or any other expression
    private fun PsiElement?.findSuppressAnnotationValueArgumentList(): PsiElement? =
        when (this?.elementTypeName()) {
            ANNOTATION_ENTRY -> {
                takeIf { findElementType(CONSTRUCTOR_CALLEE)?.text == "Suppress" }
                    ?.findElementType(VALUE_ARGUMENT_LIST)
            }

            ANNOTATED_EXPRESSION, ANNOTATION, FILE_ANNOTATION_LIST, MODIFIER_LIST -> {
                children.firstNotNullOfOrNull { it.findSuppressAnnotationValueArgumentList() }
            }

            FILE -> {
                findElementType(FILE_ANNOTATION_LIST).findSuppressAnnotationValueArgumentList()
            }

            else -> null
        }

    private fun PsiFile.createFileSuppressAnnotation(): PsiElement =
        PsiFileFactory
            .getInstance(project)
            .createFileFromText(
                language,
                """
                @file:Suppress("ktlint:${lintError.ruleId.value}")
                fun foo() {}
                """.trimIndent(),
            ).findElementType(FILE_ANNOTATION_LIST)
            ?: throw IllegalStateException("Can not create '@file:Suppress' annotation")

    private fun PsiFile.createModifierListWithSuppressAnnotation(existingSuppressions: List<String> = emptyList()): PsiElement {
        val suppressions =
            existingSuppressions
                .plus(
                    """
                    "ktlint:${lintError.ruleId.value}"
                    """.trimIndent(),
                ).distinct()
                .sorted()
                .joinToString()

        return PsiFileFactory
            .getInstance(project)
            .createFileFromText(
                language,
                """
                @Suppress($suppressions)
                fun foo() {}
                """.trimIndent(),
            )?.findElementType(SCRIPT)
            ?.findElementType(BLOCK)
            ?.findElementType(FUN)
            ?.findElementType(MODIFIER_LIST)
            ?: throw IllegalStateException("Can not create '@Suppress' annotation")
    }
}
