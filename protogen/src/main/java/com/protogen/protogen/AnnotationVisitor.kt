package com.protogen.protogen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class AnnotationVisitor(
    private val logger: KSPLogger
) : KSTopDownVisitor<MutableMap<KSAnnotated, List<KSAnnotation>>, Unit>() {
    override fun defaultHandler(node: KSNode, data: MutableMap<KSAnnotated, List<KSAnnotation>>) = Unit

    override fun visitAnnotated(annotated: KSAnnotated, data: MutableMap<KSAnnotated, List<KSAnnotation>>) {
        logger.warn("Annotations: ${getName(annotated)} ${annotated.annotations.toList().map { it.shortName.asString() }}")
//        if (annotated.annotations.toList().isNotEmpty()) {
//            data[annotated] = annotated.annotations.toList()
//            logger.warn("Annotations: ${getName(annotated)} ${annotated.annotations.toList().map { it.shortName.asString() }}")
//        }
        super.visitAnnotated(annotated, data)
    }

    override fun visitTypeReference(typeReference: KSTypeReference, data: MutableMap<KSAnnotated, List<KSAnnotation>>) {
        // don't traverse type references
    }

    private fun getName(annotated: KSAnnotated): String {
        return when(annotated) {
            is KSClassDeclaration -> annotated.simpleName.asString()
            is KSPropertyDeclaration -> annotated.simpleName.asString()
            else -> "$annotated"
        }
    }
}