package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator

@AutoProtoGenerator(javaPackage = "com.proto.animal")
data class Animal(
    val name: String,
    val canStayOnLand: Boolean,
    val canStayInWater: Boolean?,
    val canFly: Boolean
)