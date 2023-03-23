package com.protogen.protogen

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.protogen.core.AutoProtoGenerator
import java.util.LinkedList

class ProtoGeneratorSymbolProcessor(
    private val environment: SymbolProcessorEnvironment,
    private val logger: KSPLogger
) : SymbolProcessor {

    companion object {
        internal const val PROTO_EXTENSION = "proto"
        internal const val PACKAGE = "com.protogen"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbols(AutoProtoGenerator::class)
        symbols.forEach {
            logger.warn("symbols: ${it.simpleName.asString()}")
        }
        val unableToProcess = symbols.filterNot { it.validate() }.toList()
        val symbolList = LinkedList<KSClassDeclaration>()
        symbolList.addAll(symbols.filter { it.validate() })
        while (symbolList.isNotEmpty()) {
            val item = symbolList.poll()
            environment.codeGenerator.createNewFile(
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = PACKAGE,
                fileName = item.simpleName.asString(),
                extensionName = PROTO_EXTENSION
            ).apply {
                write(ProtoGenerator(logger) {
                    if (symbolList.contains(it).not()) {
                        symbolList.add(it)
                    }
                }.createProto(
                    item,
                    item.getProtoOptions(),
                    resolver
                ).toByteArray(Charsets.UTF_8))
            }
        }
        return unableToProcess
    }
}
