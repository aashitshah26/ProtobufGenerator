package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.IgnoreProtoProperty

@AutoProtoGenerator(javaPackage = "com.proto.test3")
data class IgnoreProtoPropertyTestCase(
    val name: ArrayList<ArrayList<String>>? = null,

    @IgnoreProtoProperty
    val ignoredProp: String? = null,
)