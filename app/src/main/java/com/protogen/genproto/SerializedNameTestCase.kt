package com.protogen.genproto

import com.google.gson.annotations.SerializedName
import com.protogen.core.AutoProtoGenerator

@AutoProtoGenerator(javaPackage = "com.proto.animal")
data class Animal(
    @SerializedName("nm") val name: String,
    @SerializedName("canStayOnLand") val canStayOnLand: ArrayList<String>,
    @SerializedName("canStayInWater") val canStayInWater: Boolean?,
    @SerializedName("canFly") val canFly: MutableList<Boolean>
)