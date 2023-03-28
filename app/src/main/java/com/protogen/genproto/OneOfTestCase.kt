package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.OneOfMessage

@AutoProtoGenerator(javaPackage = "com.proto.animal")
class Animal1 {
    val name: String = ""
    val attributes: AnimalAttribute? = null
    val habitat: Habitat? = null
}

@OneOfMessage(
    shouldGenerateSelf = true,
    childs = [
        LandAttribute::class,
        WaterAttribute::class,
        AirAttribute::class
    ]
)
open class AnimalAttribute {
    val isMammal: Boolean = false
}

data class LandAttribute(
    val runningSpeed: Float = 0F
)

data class WaterAttribute(
    val swimmingSpeed: Float = 0F
)

data class AirAttribute(
    val flyingSpeed: Float = 0F
)

enum class Habitat {
    LAND, WATER, AIR
}



