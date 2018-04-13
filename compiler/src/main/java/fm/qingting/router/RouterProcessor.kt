package fm.qingting.router

import com.google.auto.service.AutoService
import com.google.common.reflect.TypeToken
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
        if (map.isEmpty()&&methodMap.isEmpty()) {
            return
        }
        val builder = classBuilder(if (moduleName != null && moduleName.isNotEmpty())
            moduleName
        else
            CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        builder.addField(CLASS_TYPE, "ROUTER_MAP", Modifier.PUBLIC,
                Modifier.STATIC, Modifier.FINAL)
        builder.addField(METHOD_TYPE, "METHOD_MAP", Modifier.PUBLIC,
                Modifier.STATIC, Modifier.FINAL)
        val mcBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
        builder.addMethod(mcBuilder.build())
        //
        val scBuilder = CodeBlock.builder()
        scBuilder.addStatement("java.util.Map<String, Class<?>> classCache=new java.util.HashMap<>()")
        scBuilder.addStatement("java.util.Map<String, fm.qingting.router.RouterIntercept> methodCache=new java.util.HashMap<>()")
        for (key in map.keys) {
            scBuilder.addStatement(String.format(Locale.getDefault(),
                    "classCache.put(\"%s\",%s.class)", key,
                    map[key].toString()))
        }
        if (processingEnv.options.containsKey(ATTACH_NAME)) {
            assertIsEmpty(processingEnv.options[ATTACH_NAME])
            processingEnv.options[ATTACH_NAME]!!.split(",").forEach({
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "classCache.putAll(fm.qingting.router.%s.ROUTER_MAP)",
                        it))
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "methodCache.putAll(fm.qingting.router.%s.METHOD_MAP)",
                        it))
            })

        }
        for (key in methodMap.keys) {
            val method = methodMap[key]
            if (checkHasNoErrors(method)) {
                val classElement = method?.enclosingElement as TypeElement
                scBuilder.beginControlFlow("").addStatement("").endControlFlow()
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "methodCache.put(\"%s\",%s::%s)", key, classElement.toString(),
                        methodMap[key]?.simpleName))
            }
        }
        scBuilder.addStatement("ROUTER_MAP=java.util.Collections.unmodifiableMap(classCache)")
        scBuilder.addStatement("METHOD_MAP=java.util.Collections.unmodifiableMap(methodCache)")
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

    companion object {
        private val CLASS_NAME = "RouterContainer"
        private val MODULE_NAME = "ModuleName"
        private val ATTACH_NAME = "Include"
        private val CLASS_TYPE = object : TypeToken<Map<String, Class<*>>>() {
        }.type
        private val METHOD_TYPE = object : TypeToken<Map<String, Runnable>>() {
        }.type

    }

}