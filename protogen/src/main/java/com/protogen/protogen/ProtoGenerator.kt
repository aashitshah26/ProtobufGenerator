@file:OptIn(KspExperimental::class)

package com.protogen.protogen

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.gson.JsonElement
import com.protogen.core.AutoProtoGenerator

class ProtoGenerator(
    private val logger: KSPLogger,
    private val callback: NewProtoCallback
) {

    private val messageMap: MutableMap<String, String> = mutableMapOf()
    private val typeProjectionMap: HashMap<String, KSType> = HashMap()
    private val importList: MutableList<String> = mutableListOf()
    private val messageInPipeline: MutableList<String> = mutableListOf()

    companion object {
        private val DEFAULT_CLASSES = listOf("String", "Int", "Float", "Long", "Double", "Boolean")
        private val REPEATABLE_CLASSES = listOf(
            "Array", "IntArray", "FloatArray", "LongArray", "DoubleArray", "BooleanArray", "ArrayList"
        )
        private const val OPTION_JAVA_PACKAGE = "option java_package"
        private const val OPTION_JAVA_MULTIPLE_FILES = "option java_multiple_files"
        private const val SYNTAX_PROTO3 = "syntax = \"proto3\";"
        private const val PACKAGE = "package ${ProtoGeneratorSymbolProcessor.PACKAGE};"
        private val PACKAGE_IMPORT = "${ProtoGeneratorSymbolProcessor.PACKAGE.replace(".","/")}"
        private const val IMPORT = "import"
        private val NUMBER_PATTERN = Regex("\\d+")
        private const val PROTO_EXTENSION = "proto"
    }


    fun createProto(
        cls: KSClassDeclaration,
        options: ProtoOptions,
        resolver: Resolver
    ): String {
        val protoData = StringBuilder()
        protoData.appendLine(SYNTAX_PROTO3)
        protoData.appendLine(PACKAGE)
        cls.generateMessage(resolver)
        for (import in importList) {
            protoData.appendLine("${IMPORT} \"$import\";")
        }
        protoData.appendLine(getProtoOptions(options))

        for (message in messageMap) {
            protoData.appendLine(message.value)
        }
        return protoData.toString()
    }

    private fun KSClassDeclaration.generateMessage(resolver: Resolver) {
        val messageName = simpleName.asString()
        if (messageMap.contains(messageName) || messageInPipeline.contains(simpleName.asString())) {
            return
        }
        if (isSealed()) {
            messageInPipeline.add(messageName)
            val data = StringBuilder()
            data.appendLine("message $messageName {")
            data.appendLine("\toneof single_field_oneof {")
            getSealedSubclasses().forEachIndexed { index, sealedInnerClass ->
                val nm = sealedInnerClass.simpleName.asString()
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${index+1};")
                sealedInnerClass.generateMessage(resolver)
            }
            data.appendLine("\t}")
            data.appendLine("}")
            messageMap[messageName] = data.toString()
            messageInPipeline.remove(messageName)
        } else if (isOneOfParent()) {
            messageInPipeline.add(messageName)
            val data = StringBuilder()
            data.appendLine("message $messageName {")
            data.appendLine("\toneof single_field_oneof {")
            var count = 1
            getOneOfChilds(resolver).forEachIndexed { index, t ->
                val nm = t.simpleName.asString()
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${count++};")
                t.generateMessage(resolver)
            }
            if (canGenerateSelf()) {
                val nm = messageName + "Self"
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${count++};")
                generateDefaultMessage(nm, resolver)
            }
            data.appendLine("\t}")
            data.appendLine("}")
            messageMap[messageName] = data.toString()
            messageInPipeline.remove(messageName)
        } else if (isEnum()) {
            messageInPipeline.add(messageName)
            val data = StringBuilder()
            data.appendLine("enum $messageName {")
            getEnumConstants().forEachIndexed { index, item ->
                data.append("\t")
                val nm = item.getSerializedName() ?: item.simpleName.asString()
                data.appendLine("${messageName}_${nm} = ${index};")
            }
            data.appendLine("}")
            messageMap[messageName] = data.toString()
            messageInPipeline.remove(messageName)
        } else {
            generateDefaultMessage(messageName, resolver)
        }
    }

    private fun KSClassDeclaration.generateDefaultMessage(messageName: String, resolver: Resolver) {
        if (messageMap.contains(messageName) || messageInPipeline.contains(messageName)) {
            return
        }
        messageInPipeline.add(messageName)
        val data = StringBuilder()
        data.appendLine("message $messageName {")
        var countItems = 1
        getAllProtoProperties().forEachIndexed { index, variable ->
            val type = variable.getConvertedType(resolver, logger) ?: variable.type.resolve()

            val isNullable = type.isMarkedNullable
            val name = variable.getSerializedName() ?: variable.simpleName.asString()
            val typeAsPerHandledItem = handleArguments(name, type, resolver)

            val realClass = when (val classifier = type.declaration) {
                is KSClassDeclaration -> classifier
                is KSTypeParameter -> typeProjectionMap[classifier.simpleName.asString()]?.declaration as? KSClassDeclaration
                is KSTypeAlias -> {
                    classifier.type.resolve().declaration as KSClassDeclaration
                }
                else -> {
                    logger.error("Member type for variable $name of $messageName is null $type")
                    null
                }
            }

            realClass?.let { rC ->
                if (rC.shouldGenerateMsg()) {
                    rC.generateMessage(resolver)
                }
                data.append("\t")
                if (isNullable && rC.canFieldBeOptional()) {
                    data.append("optional ")
                }
                if (rC.isRepeatableDataStructure()) {
                    data.append("repeated ")
                }
                data.append("$typeAsPerHandledItem ")
                data.appendLine("$name = ${countItems++};")
            }
        }
            data.appendLine("}")
        messageMap[messageName] = data.toString()
        messageInPipeline.remove(messageName)
    }

    private fun KSClassDeclaration.canFieldBeOptional(): Boolean {
        return isRepeatableDataStructure().not() && isSubclassOf(Map::class).not() && isSubclassOf(java.util.HashMap::class).not()
    }

    private fun KSClassDeclaration.isRepeatableDataStructure(): Boolean {
        return isSubclassOf(List::class) ||
                isSubclassOf(Collection::class) ||
                isSubclassOf(ArrayList::class) ||
                (simpleName.asString() in REPEATABLE_CLASSES)
    }

    private fun KSClassDeclaration.shouldGenerateMsg(): Boolean {
        return (isDefaultDataStructure() ||
                isRepeatableDataStructure() ||
                isSubclassOf(Map::class) ||
                isSubclassOf(java.util.HashMap::class) ||
                isSubclassOf(JsonElement::class) ||
                isSubclassOf("JSONArray", "org.json.JSONArray") ||
                isSubclassOf("JSONObject", "org.json.JSONObject") ||
                isAnnotationPresent(AutoProtoGenerator::class)).not()

    }

    private fun KSClassDeclaration.isDefaultDataStructure(): Boolean {
        return simpleName.asString() in DEFAULT_CLASSES
    }

    private fun handleArguments(
        variableName: String,
        type: KSType,
        resolver: Resolver,
        missRepeated: Boolean = true
    ): String {
        val data = when (val classifier = type.declaration) {
            is KSClassDeclaration -> {
                classifier.typeParameters.forEachIndexed { index, param ->
                    type.arguments[index].type?.let {
                        typeProjectionMap[param.name.asString()] = it.resolve()
                    }
                }
                classifier.handleClassDeclarationArgument(
                    variableName,
                    type,
                    resolver,
                    missRepeated
                ) { _variableName, _type, _resolver, _missRepeated ->
                    handleArguments(_variableName, _type, _resolver, _missRepeated)
                }
            }
            is KSTypeParameter -> {
                typeProjectionMap[classifier.name.asString()]?.let { handleArguments(variableName, it, resolver, missRepeated) }
                    ?: "Got null projection"
            }
            is KSFunctionDeclaration -> {
                logger.error("KSFunctionDeclaration can't be null ${classifier.simpleName.asString()}")
                "-----"
            }
            is KSPropertyDeclaration -> {
                logger.error("KSPropertyDeclaration can't be null ${classifier.simpleName.asString()}")
                "-----"
            }
            is KSTypeAlias -> {
                classifier.typeParameters.forEachIndexed { index, param ->
                    type.arguments[index].type?.let {
                        typeProjectionMap[param.name.asString()] = it.resolve()
                    }
                }
                val cls = classifier.type.resolve().declaration as KSClassDeclaration
                cls.handleClassDeclarationArgument(
                    variableName,
                    classifier.type.resolve(),
                    resolver,
                    missRepeated
                ) { _variableName, _type, _resolver, _missRepeated ->
                    val typePara = _type.declaration.simpleName.asString()
                    typeProjectionMap[typePara]?.let { handleArguments(_variableName, it, _resolver, _missRepeated) }
                        ?: typePara
                }
            }
            else -> {
                logger.error("Classifier can't be null")
                "-----"
            }
        }
        return data
    }

    private fun KSClassDeclaration.handleClassDeclarationArgument(
        variableName: String,
        type: KSType,
        resolver: Resolver,
        missRepeated: Boolean = true,
        handleGeneration: (String, KSType, Resolver, Boolean) -> String
    ): String {
        return if (typeParameters.isEmpty()) {
            if (shouldGenerateMsg()) {
                generateMessage(resolver)
            }
            getProtoDataType()
        } else if (isRepeatableDataStructure()) {
            if (missRepeated) {
                handleGeneration(variableName, type.arguments[0].type!!.resolve(), resolver, false)
            } else {
                createListMessage(variableName, handleGeneration(variableName, type.arguments[0].type!!.resolve(), resolver, missRepeated))
            }
        } else if (isSubclassOf(Map::class) || isSubclassOf(java.util.HashMap::class)) {
            "map<${handleGeneration(variableName, type.arguments[0].type!!.resolve(), resolver, false)}," +
                    "${handleGeneration(variableName, type.arguments[1].type!!.resolve(), resolver, false)}>"
        } else if (shouldGenerateMsg()) {
            generateMessage(resolver)
            getProtoDataType()
        } else {
            getProtoDataType()
        }
    }

    private fun KSClassDeclaration.getProtoDataType(): String {
        return if (isAnnotationPresent(AutoProtoGenerator::class)) {
            val name = simpleName.asString()
            if (messageMap.contains(name).not() && messageInPipeline.contains(name).not()) {
                callback.onCreateNewProtoFile(this)
                addImportIfNotAvailable("${PACKAGE_IMPORT}/${name}.${PROTO_EXTENSION}")
            }
            name
        } else if (isSubclassOf(JsonElement::class) ||
            isSubclassOf("JSONArray", "org.json.JSONArray") ||
            isSubclassOf("JSONObject", "org.json.JSONObject")
        ) {
            addImportIfNotAvailable("google/protobuf/struct.proto")
            "google.protobuf.Struct"
        } else when (val name = simpleName.asString()) {
            "String" -> "string"
            "Int" -> "int32"
            "Float" -> "float"
            "Long" -> "int64"
            "Double" -> "double"
            "Boolean" -> "bool"
            "IntArray" -> "int32"
            "FloatArray" -> "float"
            "LongArray" -> "int64"
            "DoubleArray" -> "double"
            "BooleanArray" -> "bool"
            null -> {
                logger.error("Class name can't be null")
                "null_name"
            }
            else -> name
        }
    }

    private fun createListMessage(variableName: String, type: String): String {
        val messageName = getRepeatableName(type, variableName)
        if (messageMap.contains(messageName).not()) {
            val data = StringBuilder()
            data.appendLine("message $messageName {")
            data.appendLine("\t repeated $type $variableName = 1;")
            data.appendLine("}")
            messageMap[messageName] = data.toString()
        }
        return messageName
    }

    private fun getRepeatableName(variableName: String, type: String): String {
        val msgName = type.getMessageName(variableName)
        return if (msgName.startsWith("RepeatableOf")) {
            msgName.increment()
        } else {
            "RepeatableOf$msgName"
        }
    }

    private fun String.getMessageName(variableName: String): String {
        return (replace("google.protobuf.Struct", "Struct") + ",${variableName}")
            .removeCommaAndCapitalize()

    }

    private fun String.removeCommaAndCapitalize(): String {
        return split("<",",",">").map { it.capitalize()}.joinToString("")
    }

    private fun getProtoOptions(options: ProtoOptions): String {
        val optionsData = StringBuilder()
        optionsData.appendLine("${OPTION_JAVA_PACKAGE} = \"${options.javaPackage}\";")
        optionsData.appendLine("${OPTION_JAVA_MULTIPLE_FILES} = ${options.javaMultipleFile};")
        return optionsData.toString()
    }

    private fun addImportIfNotAvailable(import: String) {
        if (importList.contains(import).not()) {
            importList.add(import)
        }
    }

    private fun String.increment(): String {
        val m = NUMBER_PATTERN.find(this) ?: return this+"1"
        val num = m.value
        val inc = num.toInt() + 1
        val incStr = String.format("%0" + num.length + "d", inc)
        return (this.substring(0, m.range.first) + incStr)
    }
}
