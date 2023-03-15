package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.FieldConvertedType

@AutoProtoGenerator(javaPackage = "com.proto.FieldConvertedTypeTest")
data class FieldConvertedTypeTestCase(
    @FieldConvertedType(
        type = List::class,
        typeParams = [
            FieldConvertedType(
                List::class,
                [FieldConvertedType(String::class, isNullable = true)]
            )
        ],
        isNullable = false
    )
    val mappedProp: List<Int> = listOf(),
)