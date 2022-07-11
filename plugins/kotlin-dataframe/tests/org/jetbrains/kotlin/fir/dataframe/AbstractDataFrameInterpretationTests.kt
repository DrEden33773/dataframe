/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.unwrapArgument
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.test.assertEquals

abstract class AbstractDataFrameInterpretationTests : AbstractKotlinCompilerTest() {
    lateinit var filePath: String

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
        }

        useConfigurators(
            { testServices: TestServices -> Configurator(testServices, { filePath }) },
            ::DataFramePluginAnnotationsProvider
        )
    }

    override fun runTest(filePath: String) {
        this.filePath = filePath
        super.runTest(filePath)
    }

    class Configurator(testServices: TestServices, val function: () -> String) : EnvironmentConfigurator(testServices) {
        override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
            FirExtensionRegistrar.registerExtension(project, object : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    +{ session: FirSession -> InterpretersRunner(session, function) }
                }
            })
        }
    }

    class InterpretersRunner(session: FirSession, val function: () -> String) : FirExpressionResolutionExtension(session) {
        override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
            functionCall.calleeReference.name.identifierOrNullIfSpecial?.let {
                if (it == "test") {
                    val id = (functionCall.arguments[0].unwrapArgument() as FirConstExpression<*>).value as String
                    val call = functionCall.arguments[1].unwrapArgument() as FirFunctionCall
                    val interpreter = call.loadInterpreter()!!
                    val result = interpret(call, interpreter)?.value ?: TODO("test error cases")
                    assertEquals(expectedResult(id), result)
                }
            }
            return emptyList()
        }

        fun expectedResult(id: String): Any? {
            val map = mapOf(
                "string_1" to "42"
            )
            return map[id]
        }
    }
}