package com.protogen.protogen

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.gson.JsonElement

class ProtoGenerator(
    private val logger: KSPLogger
) {
    private val messageMap: MutableMap<String, String> = mutableMapOf()
    private val typeProjectionMap: MutableMap<String, KSType> = mutableMapOf()
    private val importList: MutableList<String> = mutableListOf()

    companion object {
        private val DEFAULT_CLASSES = listOf("String", "Int", "Float", "Long", "Double", "Boolean")
        private val REPEATABLE_CLASSES = listOf(
            "Array", "IntArray", "FloatArray", "LongArray", "DoubleArray", "BooleanArray"
        )
        private const val OPTION_JAVA_PACKAGE = "option java_package"
        private const val OPTION_JAVA_MULTIPLE_FILES = "option java_multiple_files"
        private const val SYNTAX_PROTO3 = "syntax = \"proto3\";"
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
        if (isSealed()) {
            val data = StringBuilder()
            data.appendLine("message ${simpleName.asString()} {")
            data.appendLine("\toneof single_field_oneof {")
            getSealedSubclasses().forEachIndexed { index, sealedInnerClass ->
                val nm = sealedInnerClass.simpleName.asString()
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${index+1};")
                sealedInnerClass.generateMessage(resolver)
            }
            data.appendLine("\t}")
            data.appendLine("}")
            messageMap[simpleName.asString()] = data.toString()
        } else if (isOneOfParent()) {
            val data = StringBuilder()
            data.appendLine("message ${simpleName.asString()} {")
            data.appendLine("\toneof single_field_oneof {")
            var count = 1
            resolver.getOneOfChilds(this).forEachIndexed { index, t ->
                val nm = t.simpleName.asString()
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${count++};")
                t.generateMessage(resolver)
            }
            if (canGenerateSelf()) {
                val nm = simpleName.asString() + "Self"
                data.appendLine("\t\t${nm} ${nm.lowercase()} = ${count++};")
                generateDefaultMessage(nm, resolver)
            }
            data.appendLine("\t}")
            data.appendLine("}")
            messageMap[simpleName.asString()] = data.toString()
        } else {
            generateDefaultMessage(simpleName.asString(), resolver)
        }
    }

    private fun KSClassDeclaration.generateDefaultMessage(messageName: String, resolver: Resolver) {
        if (messageMap.contains(messageName)) {
            return
        }
        logger.info("creatingMessage $messageName")
        val data = StringBuilder()
        if (isEnum()) {
            data.appendLine("enum $messageName {")
            declarations.filter { it is KSClassDeclaration }
                .forEachIndexed { index, item ->
                    data.append("\t")
                    data.appendLine("${item.simpleName.asString()} = ${index};")
                }
            data.appendLine("}")
        } else {
            data.appendLine("message $messageName {")
            var countItems = 1
            getAllProperties().forEachIndexed { index, variable ->
                val type = variable.type.resolve()
                val isNullable = type.isMarkedNullable
                val name = variable.getSerializedName() ?: variable.simpleName.asString()
                val typeAsPerHandledItem = handleArguments(name, type, resolver)

                val realClass = when (val classifier = type.declaration) {
                    is KSClassDeclaration -> classifier
                    is KSTypeParameter -> typeProjectionMap[classifier.simpleName.asString()]?.declaration as? KSClassDeclaration
                    else -> {
                        logger.error("Member type for variable $name of $messageName is null")
                        null
                    }
                }

                realClass?.let { rC ->
                    val isRepeatableDS = rC.isRepeatableDataStructure()
                    if (rC.shouldGenerateMsg()) {
                        rC.generateMessage(resolver)
                    }
                    data.append("\t")
                    if (isNullable && isRepeatableDS.not() && rC.isSubclassOf(Map::class).not()) {
                        data.append("optional ")
                    }
                    if (isRepeatableDS) {
                        data.append("repeated ")
                    }
                    data.append("$typeAsPerHandledItem ")
                    data.appendLine("$name = ${countItems++};")
                }
            }
            data.appendLine("}")
        }
        messageMap[messageName] = data.toString()
    }

    private fun KSClassDeclaration.isRepeatableDataStructure(): Boolean {
        return isSubclassOf(List::class) ||
                isSubclassOf(Collection::class) ||
                (simpleName.asString() in REPEATABLE_CLASSES)
    }

    private fun KSClassDeclaration.shouldGenerateMsg(): Boolean {
        return (isDefaultDataStructure() ||
                isRepeatableDataStructure() ||
                isSubclassOf(Map::class) ||
                isSubclassOf(JsonElement::class) ||
                isSubclassOf("JSONArray", "org.json.JSONArray") ||
                isSubclassOf("JSONObject", "org.json.JSONObject")).not()
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
                if (classifier.typeParameters.isEmpty()) {
                    if (classifier.shouldGenerateMsg()) {
                        classifier.generateMessage(resolver)
                    }
                    classifier.getProtoDataType()
                } else if (classifier.isRepeatableDataStructure()) {
                    if (missRepeated) {
                        handleArguments(variableName, type.arguments[0].type!!.resolve(), resolver, false)
                    } else {
                        createListMessage(variableName, handleArguments(variableName, type.arguments[0].type!!.resolve(), resolver, missRepeated))
                    }
                } else if (classifier.isSubclassOf(Map::class)) {
                    return "map<${handleArguments(variableName, type.arguments[0].type!!.resolve(), resolver, false)}," +
                            "${handleArguments(variableName, type.arguments[1].type!!.resolve(), resolver, false)}>"
                } else if (classifier.shouldGenerateMsg()) {
                    classifier.generateMessage(resolver)
                    classifier.simpleName.asString()
                } else {
                    classifier.getProtoDataType()
                }
            }
            is KSTypeParameter -> {
                typeProjectionMap[classifier.name.asString()]?.let { handleArguments(variableName, it, resolver, missRepeated) }
                    ?: "Got null projection"
            }
            else -> {
                logger.error("Classifier can't be null")
                "-----"
            }
        }
        return data
    }

    private fun KSClassDeclaration.getProtoDataType(): String {
        return if (isSubclassOf(JsonElement::class) ||
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
        val messageName = getRepeatableName(type)
        if (messageMap.contains(messageName).not()) {
            val data = StringBuilder()
            data.appendLine("message $messageName {")
            data.appendLine("\t repeated $type $variableName = 1;")
            data.appendLine("}")
            messageMap[messageName] = data.toString()
        }
        return messageName
    }

    private fun getRepeatableName(type: String): String {
        val msgName = type.getMessageName()
        return if (msgName.startsWith("RepeatableOf")) {
            msgName.increment()
        } else {
            "RepeatableOf$msgName"
        }
    }

    private fun String.getMessageName(): String {
        return replace("google.protobuf.Struct", "Struct").removeCommaAndCapitalize()

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
