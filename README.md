# ProtobufGenerator

> KSP Compiler plugin for protobuf generation from POJO.

To implement protobuf, we use Wire to serialize/deserialize, which takes .proto files and generate its data class and builders. To follow this approach for the existing data classes, we have to manually generate the proto files. Which is a very teadious job. This plugin aims to remove that overhead by generating the proto files for the annotated classes.

## Installing

[ ![Maven Central] (https://badgen.net/maven/v/maven-central/io.github.aashitshah26/protobufgenerator-core) ] (https://central.sonatype.com/artifact/io.github.aashitshah26/protobufgenerator-core/1.0.4)

Add KSP plugin, gradle dependency, packing options and include generated ksp directory to your sourceSets

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<version>"
}

android {
    ....
    packagingOptions {
      resources.pickFirsts.add("**.proto")
    }

    sourceSets {
        getByName("debug") {
          resources.srcDirs("build/generated/ksp/debug/resources")
        }
        getByName("release") {
          resources.srcDirs("build/generated/ksp/release/resources")
        }
    }
    ...
}

dependencies {
    implementation("io.github.aashitshah26:protobufgenerator-core:1.0.4")
    ksp("io.github.aashitshah26:protobufgenerator-protogen:1.0.4")
}


```

## Usage

Just add the `@AutoProtoGenerator` annotation to your data/POJO class and wait for the magic 🪄.

```kotlin
@AutoProtoGenerator(javaPackage = "com.proto.animal", javaMultipleFile = true)
data class Animal(
    val name: String,
    val canStayOnLand: Boolean,
    val canStayInWater: Boolean?,
    val canFly: Boolean
)
```

If there is a class which is inherited by some other classes and the parent class is referenced in your data class. We will have to use `@OneOfParent` and `@OneOfChild` annotations to support the generation of porto messages for child as well. See [test 2](https://github.com/aashitshah26/ProtobufGenerator/blob/main/app/src/main/java/com/protogen/genproto/Test2.kt) for better understanding.

```kotlin
@OneOfParent(shouldGenerateSelf = true)
open class AnimalAttribute {
    val isMammal: Boolean = false
}

@OneOfChild(AnimalAttribute::class)
data class LandAttribute(
    val runningSpeed: Float = 0F
)
```

If you are using wire to generate the classes from proto, include the following code snippet to run wire after your proto is generated and set the wire source to the generated location.

```kotlin

wire {
    sourcePath {
        srcDir("build/generated/ksp/debug/resources/com/protogen")
    }
    kotlin {
        // to implement android.os.Parcelable
        android = true
    }
}

afterEvaluate {
    android.applicationVariants.forEach {
        val buildType = it.buildType.name
        val kspTask = "ksp${buildType.capitalize()}Kotlin"
        val wireTask = "generate${buildType.capitalize()}Protos"
        tasks.named(wireTask) {
            dependsOn(kspTask)
        }
    }
}
```

## License 

    Copyright 2023 Aashit Shah
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
