package fm.qingting.router

import com.google.auto.service.AutoService
import com.squareup.javapoet.*
import fm.qingting.router.annotations.RouterField
import fm.qingting.router.annotations.RouterPath
import java.io.IOException
import java.util.*
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements

/**
 * Created by lee on 2018/4/9.
 */

@AutoService(Processor::class)
class RouterProcessor1 : AbstractProcessor() {
    private var elementUtils: Elements? = null
    private var targetModuleName = ""

    override fun getSupportedAnnotationTypes(): Set<String> {
        return setOf(RouterPath::class.java.canonicalName)
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        //        System.out.println("handle RouterProcessor"+annotations.size());
        if (annotations.size == 0) {
            return false
        }

        val elements = roundEnv.getElementsAnnotatedWith(RouterPath::class.java)
        val activityRouteTableInitializer = ClassName.get("com.thejoyrun.router", "RouterInitializer")
        val typeSpec = TypeSpec.classBuilder((if (targetModuleName.length == 0) "Apt" else targetModuleName) + "RouterInitializer")
                .addSuperinterface(activityRouteTableInitializer)
                .addModifiers(Modifier.PUBLIC)
                .addStaticBlock(CodeBlock.builder().add(String.format("Router.register(new %sRouterInitializer());", if (targetModuleName.length == 0) "Apt" else targetModuleName)).build())

        val activityRouteTableInitializertypeElement = elementUtils?.getTypeElement(activityRouteTableInitializer.toString())
        val members = elementUtils?.getAllMembers(activityRouteTableInitializertypeElement)
        var bindViewMethodSpecBuilder: MethodSpec.Builder? = null
        if (members != null) {
            for (element in members) {
                //            System.out.println(element.getSimpleName());
                if ("init" == element.simpleName.toString()) {
                    bindViewMethodSpecBuilder = MethodSpec.overriding(element as ExecutableElement)
                    break
                }
            }
        }
        if (bindViewMethodSpecBuilder == null) {
            return false
        }
        val activityHelperClassName = ClassName.get("com.thejoyrun.router", "ActivityHelper")

        val methodSpecs = ArrayList<MethodSpec>()
        for (element in elements) {
            val routerActivity = element.getAnnotation(RouterPath::class.java)
            val typeElement = element as TypeElement
            bindViewMethodSpecBuilder.addStatement("arg0.put(\$S, \$T.class)", routerActivity.value, typeElement.asType())
            val className = buildActivityHelper(routerActivity.value, activityHelperClassName, element)

            val methodSpec = MethodSpec.methodBuilder("get" + className.simpleName())
                    .addStatement("return new \$T()", className)
                    .returns(className)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .build()
            methodSpecs.add(methodSpec)
        }
        val typeSpecRouterHelper = TypeSpec.classBuilder(targetModuleName + "RouterHelper")
                .addModifiers(Modifier.PUBLIC)
                .addMethods(methodSpecs)
                .build()
        val javaFileRouterHelper = JavaFile.builder("com.thejoyrun.router", typeSpecRouterHelper).build()


        val javaFile = JavaFile.builder("com.thejoyrun.router", typeSpec.addMethod(bindViewMethodSpecBuilder.build()).build()).build()
        try {
            javaFile.writeTo(processingEnv.filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        try {
            javaFileRouterHelper.writeTo(processingEnv.filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    //TODO 支持在gradle关闭该功能
    private fun buildActivityHelper(routerActivityName: String, activityHelperClassName: ClassName, typeElement: TypeElement): ClassName {
        val members = elementUtils!!.getAllMembers(typeElement)
        val methodSpecs = ArrayList<MethodSpec>()
        val className = ClassName.get("com.thejoyrun.router", typeElement.simpleName.toString() + "Helper")
        for (element in members) {
            val routerField = element.getAnnotation(RouterField::class.java) ?: continue
            var name = element.simpleName.toString()
            if (name.length >= 2 && name[0] == 'm' && Character.isUpperCase(name[1])) {
                name = name.substring(1, 2).toLowerCase() + name.substring(2)
            }
            val upperName = name.substring(0, 1).toUpperCase() + name.substring(1)
            val methodSpec = MethodSpec.methodBuilder("with" + upperName)
                    .addParameter(TypeName.get(element.asType()), name)
                    .addStatement(String.format("put(\"%s\",%s )", routerField.value, name))
                    .addStatement("return this")
                    .returns(className)
                    .addModifiers(Modifier.PUBLIC)
                    .build()
            methodSpecs.add(methodSpec)
        }
        val methodSpec = MethodSpec.constructorBuilder()
                .addStatement("super(\$S)", routerActivityName)
                .addModifiers(Modifier.PUBLIC)
                .build()

        val typeSpec = TypeSpec.classBuilder(typeElement.simpleName.toString() + "Helper")
                .superclass(activityHelperClassName)
                .addModifiers(Modifier.PUBLIC)
                .addMethods(methodSpecs)
                .addMethod(methodSpec)
                .build()
        val javaFile = JavaFile.builder("com.thejoyrun.router", typeSpec).build()

        try {
            javaFile.writeTo(processingEnv.filer)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return className
    }

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        elementUtils = processingEnv.elementUtils
        val map = processingEnv.options
        val keys = map.keys
        for (key in keys) {
            if ("targetModuleName" == key) {
                this.targetModuleName = map[key].toString()
            }
            println(key + " = " + map[key])
        }
    }


}