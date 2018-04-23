package fm.qingting.router

import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import fm.qingting.router.annotations.RouterPath
import java.io.IOException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


/**
 * Created by lee on 2018/3/12.
 */
@AutoService(Processor::class)
@SuppressWarnings("unused")
class RouterProcessor : AbstractProcessor() {
    private val contextClassName = ClassName.get("android.content", "Context")
    private val uriClassName = ClassName.get("android.net", "Uri")
    private val bundleClassName = ClassName.get("android.os", "Bundle")
    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RouterPath::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }


    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val className = processingEnv.options[MODULE_NAME]
        val classCache = HashMap<String, Element>()
        val methodCache = HashMap<String, ExecutableElement>()
        var routerSet = roundEnv.getElementsAnnotatedWith(RouterPath::class.java)
        for (element in ElementFilter.typesIn(routerSet)) {
            if (element.kind == ElementKind.CLASS) {
                val path = element.getAnnotation(RouterPath::class.java)
                if (classCache.containsKey(path.value) || methodCache.containsKey(path.value)) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format(Locale.getDefault(), "The key:%s with %s already exist",
                                    path.value, element.toString()))
                }
                classCache.put(path.value, element)
                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                        String.format(Locale.getDefault(), path.value + " = " + element.toString()))
            }
        }

        for (element in ElementFilter.methodsIn(routerSet)) {
            val path = element.getAnnotation(RouterPath::class.java)
            if (classCache.containsKey(path.value) || methodCache.containsKey(path.value)) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format(Locale.getDefault(), "The key:%s with %s already exist",
                                path.value, element.toString()))
            }
            methodCache.put(path.value, element)
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                    String.format(Locale.getDefault(), path.value + " = " + element.toString()))
        }
        try {
            generateClassRouter(className, classCache, methodCache)
        } catch (e: IOException) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                    e.toString())
        }

        return true
    }


    @Throws(IOException::class)
    private fun generateClassRouter(moduleName: String?, map: Map<String, Element>, methodMap: Map<String, ExecutableElement>) {
        if (map.isEmpty() && methodMap.isEmpty()) {
            return
        }
        val builder = classBuilder(if (moduleName != null && moduleName.isNotEmpty())
            moduleName
        else
            CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        var mcBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
        builder.addMethod(mcBuilder.build())
        mcBuilder = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        builder.addMethod(mcBuilder.build())
        //
        val scBuilder = CodeBlock.builder()
        scBuilder.addStatement("java.util.Map<String, Class<?>> classCache=new java.util.HashMap<>()")
        for (key in map.keys) {
            scBuilder.addStatement("classCache.put(\"$key\",${map[key]}.class)")
        }
        methodMap.keys.forEach {
            val method = methodMap[it]
            if (checkHasNoErrors(method)) {
                val classElement = method?.enclosingElement as TypeElement
                scBuilder.beginControlFlow("Router.INSTANCE.registerIntercept(new RouterIntercept(\"$it\")")
                        .beginControlFlow("public boolean launch($contextClassName context, $uriClassName uri, String taskId, $bundleClassName options)")
                        .addStatement("${classElement.qualifiedName}.${method.simpleName}(${getParameterString(method.parameters)})")
                        .addStatement("return true")
                        .endControlFlow()
                        .endControlFlow().addStatement(")")
            }
        }
        scBuilder.addStatement("Router.INSTANCE.init(classCache)")
        if (processingEnv.options.containsKey(ATTACH_NAME)) run {
            assertIsEmpty(processingEnv.options[ATTACH_NAME])
            processingEnv.options[ATTACH_NAME]?.split(",")?.forEach {
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "fm.qingting.router.%s.init()",
                        processingEnv.options[ATTACH_NAME]))
            }

        }
        builder.addStaticBlock(scBuilder.build())
        val javaFile = JavaFile.builder("fm.qingting.router", builder.build()).build()
        javaFile.writeTo(processingEnv.filer)
    }

    private fun assertIsEmpty(value: String?) {
        if (value == null || value.trim { it <= ' ' }.isEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
                    "annotationProcessorOptions arguments error")
        }
    }

    private fun checkHasNoErrors(element: ExecutableElement?): Boolean {
        if (element?.modifiers?.contains(Modifier.STATIC) == false) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be static", element)
            return false
        }

        if (element?.modifiers?.contains(Modifier.PUBLIC) == false) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Subscriber method must be public", element)
            return false
        }

        return true
    }

    private fun getParameterString(parameterList: List<VariableElement>): String {
        var result = ""
        parameterList.forEach {
            when (it.asType().toString()) {
                contextClassName.toString() -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "context"
                }
                uriClassName.toString() -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "uri"
                }
                "java.lang.Integer" -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "taskId"
                }
                bundleClassName.toString() -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "options"
                }
            }
        }
        return result
    }

    companion object {
        private val CLASS_NAME = "RouterContainer"
        private val MODULE_NAME = "ModuleName"
        private val ATTACH_NAME = "Include"
    }

}