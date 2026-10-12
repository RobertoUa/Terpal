package io.exoquery.terpal.plugin

import io.exoquery.terpal.plugin.transform.VisitTransformExpressions
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class GenerationExtension(
    private val options: Options,
    private val config: CompilerConfiguration,
    private val messages: MessageCollector
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment
            .transform(
                VisitTransformExpressions(pluginContext, config, options),
                null
            )
    }
}
