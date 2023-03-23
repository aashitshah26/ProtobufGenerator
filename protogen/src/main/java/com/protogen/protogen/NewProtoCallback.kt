package com.protogen.protogen

import com.google.devtools.ksp.symbol.KSClassDeclaration

fun interface NewProtoCallback {
    fun onCreateNewProtoFile(declaration: KSClassDeclaration)
}