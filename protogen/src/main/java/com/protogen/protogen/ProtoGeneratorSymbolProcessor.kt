package com.protogen.protogen

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.validate
import com.protogen.core.AutoProtoGenerator

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
        val unableToProcess = symbols.filterNot { it.validate() }.toList()
        symbols.filter { it.validate() }.forEach {
            environment.codeGenerator.createNewFile(
                dependencies = Dependencies(false, *resolver.getAllFiles().toList().toTypedArray()),
                packageName = PACKAGE,
                fileName = it.simpleName.asString(),
                extensionName = PROTO_EXTENSION
            ).apply {
                write(ProtoGenerator(logger).createProto(
                    it,
                    it.getProtoOptions(),
                    resolver
                ).toByteArray(Charsets.UTF_8))
            }
        }
        return unableToProcess
    }
}
