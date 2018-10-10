package fm.qingting.router

import com.google.auto.service.AutoService
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec.classBuilder
import fm.qingting.router.annotations.RouterPath
import java.io.File
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
    private val callbackClassName = ClassName.get("fm.qingting.router", "RouterTaskCallBack")
    private val bundleClassName = ClassName.get("android.os", "Bundle")
    private val lifeCycleClassName = ClassName.get("android.arch.lifecycle", "Lifecycle")
    private lateinit var defaultHost: String
    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RouterPath::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    private fun RouterPath.url(): String {
        if (host.isEmpty()) {
            return defaultHost + value
        }
        return host + value
    }

    override fun process(set: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val className = processingEnv.options[MODULE_NAME]
        val classCache = HashMap<String, Element>()
        val methodCache = HashMap<String, ExecutableElement>()
        val routerSet = roundEnv.getElementsAnnotatedWith(RouterPath::class.java)
        val tempHost = processingEnv.options[DEFAULT_HOST]
        defaultHost = when {
            tempHost != null -> tempHost
            else -> ""
        }
        for (element in ElementFilter.typesIn(routerSet)) {
            if (element.kind == ElementKind.CLASS) {
                val path = element.getAnnotation(RouterPath::class.java)
                if (classCache.containsKey(path.url()) || methodCache.containsKey(path.url())) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format(Locale.getDefault(), "The key:%s with %s already exist",
                                    path.url(), element.toString()))
                }
                classCache[path.url()] = element
                processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                        String.format(Locale.getDefault(), path.url() + " = " + element.toString()))
            }
        }

        for (element in ElementFilter.methodsIn(routerSet)) {
            val path = element.getAnnotation(RouterPath::class.java)
            if (classCache.containsKey(path.url()) || methodCache.containsKey(path.url())) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                        String.format(Locale.getDefault(), "The key:%s with %s already exist",
                                path.url(), element.toString()))
            }
            methodCache[path.url()] = element
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,
                    String.format(Locale.getDefault(), path.url() + " = " + element.toString()))
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
        val methodKeyCache = HashSet<String>()
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
        scBuilder.addStatement("java.util.Set<String> methodPathCache=new java.util.HashSet<>()")
        for (key in map.keys) {
            scBuilder.addStatement("Router.INSTANCE.registerUrl(\"$key\",${map[key]}.class)")
        }
        if (methodMap.keys.isNotEmpty()) {
            scBuilder.beginControlFlow("RouterIntercept methodRouteIntercept = new RouterIntercept()")
                    .beginControlFlow("public boolean launch($contextClassName context, $uriClassName uri, $callbackClassName callback, $bundleClassName options, $lifeCycleClassName lifeCycle)")
            scBuilder.beginControlFlow("switch(uri.getHost()+uri.getPath())")
            methodMap.keys.forEach {
                val method = methodMap[it]
                if (checkHasNoErrors(method)) {
                    val classElement = method?.enclosingElement as TypeElement
                    scBuilder.addStatement("case \"$it\":")
                            .addStatement("${classElement.qualifiedName}.${method.simpleName}(${getParameterString(method.parameters)})")
                            .addStatement("return true")
                    methodKeyCache.add(it)
                }
            }
            scBuilder.endControlFlow().addStatement("return false")
                    .endControlFlow()
                    .addStatement("}")
            methodKeyCache.forEach {
                scBuilder.addStatement("methodPathCache.add(\"$it\")")
            }
            scBuilder.addStatement("Router.INSTANCE.registerMethodIntercept(methodRouteIntercept,methodPathCache)")
        }
        if (processingEnv.options.containsKey(ATTACH_NAME)) {
            assertIsEmpty(processingEnv.options[ATTACH_NAME])
            processingEnv.options[ATTACH_NAME]?.split(",")?.forEach {
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "fm.qingting.router.%s.init()",
                       it))
            }

        }
        builder.addStaticBlock(scBuilder.build())
        val javaFile = JavaFile.builder("fm.qingting.router", builder.build()).build()
        javaFile.writeTo(this.processingEnv.filer)
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
                "fm.qingting.router.RouterTaskCallBack" -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "callback"
                }

                bundleClassName.toString() -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "options"
                }
                lifeCycleClassName.toString() -> {
                    if (!result.isEmpty()) {
                        result += ","
                    }
                    result += "lifeCycle"
                }
            }
        }
        return result
    }

    companion object {
        private const val CLASS_NAME = "RouterContainer"
        private const val MODULE_NAME = "ModuleName"
        private const val ATTACH_NAME = "Include"
        private const val DEFAULT_HOST = "DefaultHost"
    }

}