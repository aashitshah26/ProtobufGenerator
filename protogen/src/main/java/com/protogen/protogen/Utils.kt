package com.protogen.protogen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.gson.annotations.SerializedName
import com.protogen.core.AutoProtoGenerator
import com.protogen.core.IgnoreProtoProperty
import com.protogen.core.OneOfChild
import com.protogen.core.OneOfParent
import kotlin.reflect.KClass


fun KSClassDeclaration.isSealed() =
    classKind == ClassKind.CLASS && modifiers.contains(Modifier.SEALED)

fun KSClassDeclaration.isEnum() = classKind == ClassKind.ENUM_CLASS

@OptIn(KspExperimental::class)
fun KSAnnotated.getSerializedName() =
    getAnnotationsByType(SerializedName::class)
        .firstOrNull()?.value

@OptIn(KspExperimental::class)
fun KSClassDeclaration.isOneOfParent() =
    getAnnotationsByType(OneOfParent::class).firstOrNull() != null

@OptIn(KspExperimental::class)
fun KSClassDeclaration.canGenerateSelf() =
    getAnnotationsByType(OneOfParent::class).firstOrNull()?.shouldGenerateSelf == true

fun Resolver.getOneOfChilds(declaration: KSClassDeclaration) =
    getSymbolsWithAnnotation(OneOfChild::class.qualifiedName.orEmpty())
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.CLASS }
        .filter { it.oneOfParentChecker(declaration) }

fun KSClassDeclaration.oneOfParentChecker(parent: KSClassDeclaration) =
    (annotations.firstOrNull {
        it.shortName.asString() == OneOfChild::class.simpleName
    }?.arguments?.first()?.value as? KSType)?.declaration?.let {
        it.simpleName.asString() == parent.simpleName.asString() &&
                it.qualifiedName?.asString() == parent.qualifiedName?.asString()
    } ?: false

fun KSClassDeclaration.isSubclassOf(cls: KClass<*>): Boolean =
    (simpleName.asString() == cls.simpleName && qualifiedName?.asString() == cls.qualifiedName) ||
            superTypes
                .map { it.resolve().declaration }
                .filter { (it as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                .firstOrNull {
                    cls.simpleName == it.simpleName.asString() && cls.qualifiedName == it.qualifiedName?.asString()
                }?.let { true } ?: false

fun KSClassDeclaration.isSubclassOf(simpleNameK: String, qualifiedNameK: String): Boolean =
    (simpleName.asString() == simpleNameK && qualifiedName?.asString() == qualifiedNameK) ||
            superTypes
                .map { it.resolve().declaration }
                .filter { (it as? KSClassDeclaration)?.classKind == ClassKind.CLASS }
                .firstOrNull {
                    simpleNameK == it.simpleName.asString() && qualifiedNameK == it.qualifiedName?.asString()
                }?.let { true } ?: false

fun Resolver.getSymbols(cls: KClass<*>) =
    getSymbolsWithAnnotation(cls.qualifiedName.orEmpty())
        .filterIsInstance<KSClassDeclaration>()
        .filter { it.classKind == ClassKind.CLASS }

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getProtoOptions() =
    getAnnotationsByType(AutoProtoGenerator::class)
        .firstOrNull()?.let {
            ProtoOptions(javaPackage = it.javaPackage, javaMultipleFile = it.javaMultipleFile)
        } ?: ProtoOptions(javaPackage = packageName.asString() + ".protogen", javaMultipleFile = true)

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getAllProtoProperties() =
    getAllProperties()
        .filterNot { it.isAnnotationPresent(IgnoreProtoProperty::class) || it.isAnnotationPresent(Transient::class) }

@OptIn(KspExperimental::class)
fun KSClassDeclaration.getEnumConstants() =
    declarations
        .filter { it is KSClassDeclaration }
        .filterNot { (it as KSClassDeclaration).isCompanionObject }
        .filterNot { it.isAnnotationPresent(IgnoreProtoProperty::class) || it.isAnnotationPresent(Transient::class) }