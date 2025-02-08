
package net.liopyu.kotlinscript

import com.google.gson.GsonBuilder
import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.liopyu.kotlinscript.util.KotlinObject
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File

class FabricBootstrap : ModInitializer {
    override fun onInitialize() {
        KotlinScriptInit.preInitialize()
        val instanceDir = File(System.getProperty("user.dir"))
        val sourcesDir = File(instanceDir, "kotlinsources")
        if (!sourcesDir.exists() || !sourcesDir.isDirectory) {
            LogUtils.getLogger().warn("Kotlin sources folder not found at ${sourcesDir.absolutePath}")
            return
        }
        val functions = extractTopLevelFunctions(sourcesDir.absolutePath)
        val uniqueFunctions = functions.distinctBy { it.fullyQualifiedName }
        uniqueFunctions.forEach { suggestion ->
            LogUtils.getLogger().info("Testing method: ${suggestion.fullyQualifiedName}, source: ${suggestion.source}, path: ${suggestion.path}")
        }
        val validAndRelevantSuggestions = uniqueFunctions/*KotlinScriptInit.testKotlinSuggestions(uniqueFunctions)*/
        val occurrenceMap = mutableMapOf<String, Int>()
        val enrichedSuggestions = validAndRelevantSuggestions.map { obj ->
            val currentCount = occurrenceMap.getOrDefault(obj.simpleName, 0)
            occurrenceMap[obj.simpleName] = currentCount + 1
            val newSimpleName = if (currentCount > 0) obj.fullyQualifiedName else obj.simpleName
            obj.copy(simpleName = newSimpleName)
        }
        saveSuggestionsToJson(enrichedSuggestions, "kotlin_suggestions.json")
    }
    fun extractTopLevelFunctions(sourcePath: String): List<KotlinObject> {
        val disposable: Disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration().apply {
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, org.jetbrains.kotlin.cli.common.messages.MessageCollector.NONE)
            put(CommonConfigurationKeys.MODULE_NAME, "extraction")
            addKotlinSourceRoot(sourcePath)
        }
        val environment = KotlinCoreEnvironment.createForProduction(disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val ktFiles: List<KtFile> = environment.getSourceFiles()
        val functions = mutableListOf<KotlinObject>()
        for (file in ktFiles) {
            val pkg = file.packageFqName.asString()
            val sourceName = file.name.substringBeforeLast(".")
            file.declarations.filterIsInstance<KtNamedFunction>()
                .filter { !it.hasModifier(KtTokens.PRIVATE_KEYWORD) }
                .forEach { function ->
                    function.name?.let { name ->
                        val receiverType = function.receiverTypeReference?.text
                        val fqName = if (pkg == "kotlin" && receiverType != null && receiverType.matches(Regex("^[A-Z]$"))) {
                            name
                        } else {
                            if (pkg.isNotEmpty()) "$pkg.$name" else name
                        }
                        val type = when {
                            function.hasModifier(KtTokens.INFIX_KEYWORD) &&
                                    pkg == "kotlin" && fqName == name &&
                                    receiverType != null && receiverType.matches(Regex("^[A-Z]$")) ->
                                "infix_lambda"
                            function.hasModifier(KtTokens.INFIX_KEYWORD) ->
                                "infix"
                            pkg == "kotlin" && fqName == name &&
                                    receiverType != null && receiverType.matches(Regex("^[A-Z]$")) ->
                                "lambda"
                            function.valueParameters.isNotEmpty() ->
                                "method"
                            else ->
                                "property"
                        }
                        functions.add(
                            KotlinObject(
                                fullyQualifiedName = fqName,
                                simpleName = name,
                                source = sourceName,
                                type = type,
                                path = pkg,
                                parentType = receiverType
                            )
                        )
                    }
                }
        }
        Disposer.dispose(disposable)
        return functions
    }

    fun saveSuggestionsToJson(entities: List<KotlinObject>, outputPath: String) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(entities)
        val file = File(outputPath)
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}