package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.OneOfChild
import com.protogen.mod1.L2Sample

@AutoProtoGenerator(javaPackage = "com.proto.DependencyItemTestCase")
data class DependencyItemTestCase(
    val l2: L2Sample,
    val a1: DependencyItemTestCase
)

@OneOfChild(L2Sample::class)
data class L2SampleChild3(
    val a3: String,
)