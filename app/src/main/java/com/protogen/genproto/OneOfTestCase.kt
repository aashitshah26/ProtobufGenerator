package com.protogen.genproto

import com.protogen.core.AutoProtoGenerator
import com.protogen.core.OneOfChild
import com.protogen.core.OneOfParent

@AutoProtoGenerator(javaPackage = "com.proto.animal")
class Animal1 {
    val name: String = ""
    val attributes: AnimalAttribute? = null
    val habitat: Habitat? = null
}

@OneOfParent(shouldGenerateSelf = true)
open class AnimalAttribute {
    val isMammal: Boolean = false
}

@OneOfChild(AnimalAttribute::class)
data class LandAttribute(
    val runningSpeed: Float = 0F
)

@OneOfChild(AnimalAttribute::class)
data class WaterAttribute(
    val swimmingSpeed: Float = 0F
)

@OneOfChild(AnimalAttribute::class)
data class AirAttribute(
    val flyingSpeed: Float = 0F
)

enum class Habitat {
    LAND, WATER, AIR
}



