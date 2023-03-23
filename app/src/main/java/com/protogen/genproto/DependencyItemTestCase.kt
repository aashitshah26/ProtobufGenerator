package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.mod1.L2Sample

@AutoProtoGenerator(javaPackage = "com.proto.DependencyItemTestCase")
data class DependencyItemTestCase(
    val l2: L2Sample
)