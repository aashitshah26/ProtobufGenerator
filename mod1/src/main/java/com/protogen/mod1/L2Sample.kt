package com.protogen.mod1

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.IgnoreProtoProperty
import com.protogen.core.OneOfParent

@OneOfParent(true)
@AutoProtoGenerator("abc.sdasd.s")
data class L2Sample(
    val a: String,

    @IgnoreProtoProperty
    val b: Int
)