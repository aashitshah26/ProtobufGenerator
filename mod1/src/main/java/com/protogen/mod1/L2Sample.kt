package com.protogen.mod1

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.IgnoreProtoProperty
import com.protogen.core.OneOfChild
import com.protogen.core.OneOfMessage

@OneOfMessage(
    shouldGenerateSelf = true,
    childs = [L2SampleChild1::class, L2SampleChild2::class],
    findOneOfChilds = true
)
@AutoProtoGenerator("abc.sdasd.s")
data class L2Sample(
    val a: String,

    @IgnoreProtoProperty
    val b: Int
)

@OneOfChild(L2Sample::class)
data class L2SampleChild1(
    val a1: String
)

@OneOfChild(L2Sample::class)
data class L2SampleChild2(
    val a2: String,
)