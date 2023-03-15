package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator

@AutoProtoGenerator(javaPackage = "com.proto.ImportTestCase")
data class ImportTestCase(
    val animal: Animal
)