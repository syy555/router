package fm.qingting.router

import com.google.auto.service.AutoService
import com.google.common.reflect.TypeToken
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
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
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
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE,"Router Processor Begin")
        val cache = HashMap<String, Element>()
        for (element in roundEnv.getElementsAnnotatedWith(RouterPath::class.java)) {
            if (element is TypeElement) {
                val path = element.getAnnotation(RouterPath::class.java)
                if (cache.containsKey(path.value)) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                            String.format(Locale.getDefault(), "The key:%s with %s already exist",
                                    path.value, element.toString()))
                }
                cache.put(path.value, element)
            }
        }

        try {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING,
                    "size:" + cache.size)
            generate(cache)
        } catch (e: IOException) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR,
                    e.toString())
        }

        return true
    }


    @Throws(IOException::class)
    private fun generate(map: Map<String, Element>) {
        if (map.isEmpty()) {
            return
        }
        val options = processingEnv.options
        val className = options[MODULE_NAME]
        val builder = classBuilder(if (className != null && className.isNotEmpty())
            className
        else
            CLASS_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        builder.addField(VALUE_TYPE, "ROUTER_MAP", Modifier.PUBLIC,
                Modifier.STATIC, Modifier.FINAL)
        val mcBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE)
        builder.addMethod(mcBuilder.build())
        //
        val scBuilder = CodeBlock.builder()
        scBuilder.addStatement("java.util.Map<String, Class<?>> cache=new java.util.HashMap<>()")
        val keys = map.keys
        for (key in keys) {
            scBuilder.addStatement(String.format(Locale.getDefault(),
                    "cache.put(\"%s\",%s.class)", key ,
                    map[key].toString()))
        }
        if (options.containsKey(ATTACH_NAME)) {
            assertIsEmpty(options[ATTACH_NAME])
            scBuilder.addStatement(String.format(Locale.getDefault(),
                    "cache.putAll(fm.qingting.router.%s.ROUTER_MAP)",
                    options[ATTACH_NAME]))
        } else if (options.containsKey(ATTACH_NAME + "0")) {
            var i = 0
            var key = (ATTACH_NAME + i)
            while (options.containsKey(key)) {
                assertIsEmpty(options[key])
                scBuilder.addStatement(String.format(Locale.getDefault(),
                        "cache.putAll(fm.qingting.router.%s.ROUTER_MAP)",
                        options[key]))
                i++
                key = (ATTACH_NAME + i)
            }
        }
        scBuilder.addStatement("ROUTER_MAP=java.util.Collections.unmodifiableMap(cache)")
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

    companion object {
        private val CLASS_NAME = "RouterContainer"
        private val MODULE_NAME = "ModuleName"
        private val ATTACH_NAME = "Include"

        private val VALUE_TYPE = object : TypeToken<Map<String, Class<*>>>() {

        }.type
    }

}