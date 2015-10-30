/*
 * Copyright (C) 2015 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.databinding.tool.writer

import android.databinding.tool.BindingTarget
import android.databinding.tool.LayoutBinder
import android.databinding.tool.expr.Expr
import android.databinding.tool.expr.ExprModel
import android.databinding.tool.expr.FieldAccessExpr
import android.databinding.tool.expr.IdentifierExpr
import android.databinding.tool.expr.TernaryExpr
import android.databinding.tool.ext.androidId
import android.databinding.tool.ext.br
import android.databinding.tool.ext.joinToCamelCaseAsVar
import android.databinding.tool.ext.lazy
import android.databinding.tool.ext.versionedLazy
import android.databinding.tool.reflection.ModelAnalyzer
import android.databinding.tool.util.L
import java.util.ArrayList
import java.util.Arrays
import java.util.BitSet
import java.util.HashMap
import java.util.HashSet
import kotlin.properties.Delegates

fun String.stripNonJava() = this.split("[^a-zA-Z0-9]".toRegex()).map{ it.trim() }.joinToCamelCaseAsVar()

enum class Scope {
    FIELD,
    METHOD,
    FLAG,
    EXECUTE_PENDING_METHOD,
    CONSTRUCTOR_PARAM
}

class ExprModelExt {
    val usedFieldNames = hashMapOf<Scope, MutableSet<String>>();
    init {
        Scope.values().forEach { usedFieldNames[it] = hashSetOf<String>() }
    }
    val localizedFlags = arrayListOf<FlagSet>()

    fun localizeFlag(set : FlagSet, name:String) : FlagSet {
        localizedFlags.add(set)
        val result = getUniqueName(name, Scope.FLAG)
        set.setLocalName(result)
        return set
    }

    fun getUniqueName(base : String, scope : Scope) : String {
        var candidate = base
        var i = 0
        while (usedFieldNames[scope].contains(candidate)) {
            i ++
            candidate = base + i
        }
        usedFieldNames[scope].add(candidate)
        return candidate
    }
}

val ExprModel.ext by Delegates.lazy { target : ExprModel ->
    ExprModelExt()
}

fun ExprModel.getUniqueFieldName(base : String) : String = ext.getUniqueName(base, Scope.FIELD)
fun ExprModel.getUniqueMethodName(base : String) : String = ext.getUniqueName(base, Scope.METHOD)
fun ExprModel.getUniqueFlagName(base : String) : String = ext.getUniqueName(base, Scope.FLAG)
fun ExprModel.getConstructorParamName(base : String) : String = ext.getUniqueName(base, Scope.CONSTRUCTOR_PARAM)

fun ExprModel.localizeFlag(set : FlagSet, base : String) : FlagSet = ext.localizeFlag(set, base)

// not necessarily unique. Uniqueness is solved per scope
val BindingTarget.readableName by Delegates.lazy { target: BindingTarget ->
    if (target.getId() == null) {
        "boundView" + indexFromTag(target.getTag())
    } else {
        target.getId().androidId().stripNonJava()
    }
}

fun BindingTarget.superConversion(variable : String) : String {
    if (getResolvedType() != null && getResolvedType().extendsViewStub()) {
        return "new android.databinding.ViewStubProxy((android.view.ViewStub) ${variable})"
    } else {
        return "(${interfaceType}) ${variable}"
    }
}

val BindingTarget.fieldName : String by Delegates.lazy { target : BindingTarget ->
    val name : String
    if (target.getId() == null) {
        name = "m${target.readableName}"
    } else {
        name = target.readableName
    }
    target.getModel().getUniqueFieldName(name)
}

val BindingTarget.androidId by Delegates.lazy { target : BindingTarget ->
    if (target.getId().startsWith("@android:id/")) {
        "android.R.id.${target.getId().androidId()}"
    } else {
        "R.id.${target.getId().androidId()}"
    }
}

val BindingTarget.interfaceType by Delegates.lazy { target : BindingTarget ->
    if (target.getResolvedType() != null && target.getResolvedType().extendsViewStub()) {
        "android.databinding.ViewStubProxy"
    } else {
        target.getInterfaceType()
    }
}

val BindingTarget.constructorParamName by Delegates.lazy { target : BindingTarget ->
    target.getModel().getConstructorParamName(target.readableName)
}

// not necessarily unique. Uniqueness is decided per scope
val Expr.readableName by Delegates.lazy { expr : Expr ->
    val stripped = "${expr.getUniqueKey().stripNonJava()}"
    L.d("readableUniqueName for [%s] %s is %s", System.identityHashCode(expr), expr.getUniqueKey(), stripped)
    stripped
}

val Expr.fieldName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueFieldName("m${expr.readableName.capitalize()}")
}

val Expr.listenerClassName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueFieldName("${expr.readableName.capitalize()}Impl")
}

val Expr.oldValueName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueFieldName("mOld${expr.readableName.capitalize()}")
}

val Expr.executePendingLocalName by Delegates.lazy { expr : Expr ->
    "${expr.getModel().ext.getUniqueName(expr.readableName, Scope.EXECUTE_PENDING_METHOD)}"
}

val Expr.setterName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueMethodName("set${expr.readableName.capitalize()}")
}

val Expr.onChangeName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueMethodName("onChange${expr.readableName.capitalize()}")
}

val Expr.getterName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueMethodName("get${expr.readableName.capitalize()}")
}

val Expr.dirtyFlagName by Delegates.lazy { expr : Expr ->
    expr.getModel().getUniqueFlagName("sFlag${expr.readableName.capitalize()}")
}


fun Expr.toCode(full : Boolean = false) : KCode = CodeGenUtil.toCode(this, full)

fun Expr.isVariable() = this is IdentifierExpr && this.isDynamic()

fun Expr.conditionalFlagName(output : Boolean, suffix : String) = "${dirtyFlagName}_${output}$suffix"

val Expr.dirtyFlagSet by Delegates.lazy { expr : Expr ->
    FlagSet(expr.getInvalidFlags(), expr.getModel().getFlagBucketCount())
}

val Expr.invalidateFlagSet by Delegates.lazy { expr : Expr ->
    FlagSet(expr.getId())
}

val Expr.shouldReadFlagSet by Delegates.versionedLazy { expr : Expr ->
    FlagSet(expr.getShouldReadFlags(), expr.getModel().getFlagBucketCount())
}

val Expr.conditionalFlags by Delegates.lazy { expr : Expr ->
    arrayListOf(FlagSet(expr.getRequirementFlagIndex(false)),
            FlagSet(expr.getRequirementFlagIndex(true)))
}

val LayoutBinder.requiredComponent by Delegates.lazy { layoutBinder: LayoutBinder ->
    val required = layoutBinder.
            getBindingTargets().
            flatMap { it.getBindings() }.
            firstOrNull { it.getBindingAdapterInstanceClass() != null }
    required?.getBindingAdapterInstanceClass()
}

fun Expr.getRequirementFlagSet(expected : Boolean) : FlagSet = conditionalFlags[if(expected) 1 else 0]

fun FlagSet.notEmpty(cb : (suffix : String, value : Long) -> Unit) {
    buckets.withIndex().forEach {
        if (it.value != 0L) {
            cb(getWordSuffix(it.index), buckets[it.index])
        }
    }
}

fun FlagSet.getWordSuffix(wordIndex : Int) : String {
    return if(wordIndex == 0) "" else "_${wordIndex}"
}

fun FlagSet.localValue(bucketIndex : Int) =
        if (getLocalName() == null) binaryCode(bucketIndex)
        else "${getLocalName()}${getWordSuffix(bucketIndex)}"

fun FlagSet.binaryCode(bucketIndex : Int) = longToBinary(buckets[bucketIndex])


fun longToBinary(l : Long) =
        "0b${java.lang.Long.toBinaryString(l)}L"

fun <T> FlagSet.mapOr(other : FlagSet, cb : (suffix : String, index : Int) -> T) : List<T> {
    val min = Math.min(buckets.size(), other.buckets.size())
    val result = arrayListOf<T>()
    for (i in 0..(min - 1)) {
        // if these two can match by any chance, call the callback
        if (intersect(other, i)) {
            result.add(cb(getWordSuffix(i), i))
        }
    }
    return result
}

fun indexFromTag(tag : String) : kotlin.Int {
    val startIndex : kotlin.Int
    if (tag.startsWith("binding_")) {
        startIndex = "binding_".length();
    } else {
        startIndex = tag.lastIndexOf('_') + 1
    }
    return Integer.parseInt(tag.substring(startIndex))
}

class LayoutBinderWriter(val layoutBinder : LayoutBinder) {
    val model = layoutBinder.getModel()
    val indices = HashMap<BindingTarget, kotlin.Int>()
    val mDirtyFlags by Delegates.lazy {
        val fs = FlagSet(BitSet(), model.getFlagBucketCount());
        Arrays.fill(fs.buckets, -1)
        fs.setDynamic(true)
        model.localizeFlag(fs, "mDirtyFlags")
        fs
    }

    val dynamics by Delegates.lazy { model.getExprMap().values().filter { it.isDynamic() } }
    val className = layoutBinder.getImplementationName()

    val baseClassName = "${layoutBinder.getClassName()}"

    val includedBinders by Delegates.lazy {
        layoutBinder.getBindingTargets().filter { it.isBinder() }
    }

    val variables by Delegates.lazy {
        model.getExprMap().values().filterIsInstance(javaClass<IdentifierExpr>()).filter { it.isVariable() }
    }

    val usedVariables by Delegates.lazy {
        variables.filter {it.isUsed()}
    }

    public fun write(minSdk : kotlin.Int) : String  {
        layoutBinder.resolveWhichExpressionsAreUsed()
        calculateIndices();
        return kcode("package ${layoutBinder.getPackage()};") {
            nl("import ${layoutBinder.getModulePackage()}.R;")
            nl("import ${layoutBinder.getModulePackage()}.BR;")
            nl("import android.view.View;")
            val classDeclaration : String
            if (layoutBinder.hasVariations()) {
                classDeclaration = "${className} extends ${baseClassName}"
            } else {
                classDeclaration = "${className} extends android.databinding.ViewDataBinding"
            }
            nl("public class ${classDeclaration} {") {
                tab(declareIncludeViews())
                tab(declareViews())
                tab(declareVariables())
                tab(declareBoundValues())
                tab(declareListeners())
                tab(declareConstructor(minSdk))
                tab(declareInvalidateAll())
                tab(declareHasPendingBindings())
                tab(declareLog())
                tab(declareSetVariable())
                tab(variableSettersAndGetters())
                tab(onFieldChange())

                tab(executePendingBindings())

                tab(declareListenerImpls())
                tab(declareDirtyFlags())
                if (!layoutBinder.hasVariations()) {
                    tab(declareFactories())
                }
            }
            nl("}")
            tab(flagMapping())
            tab("//end")
        }.generate()
    }
    fun calculateIndices() : Unit {
        val taggedViews = layoutBinder.getBindingTargets().filter{
            it.isUsed() && it.getTag() != null && !it.isBinder()
        }
        taggedViews.forEach {
            indices.put(it, indexFromTag(it.getTag()))
        }
        val indexStart = maxIndex() + 1
        layoutBinder.getBindingTargets().filter{
            it.isUsed() && !taggedViews.contains(it)
        }.withIndex().forEach {
            indices.put(it.value, it.index + indexStart)
        }
    }
    fun declareIncludeViews() = kcode("") {
        nl("private static final android.databinding.ViewDataBinding.IncludedLayouts sIncludes;")
        nl("private static final android.util.SparseIntArray sViewsWithIds;")
        nl("static {") {
            val hasBinders = layoutBinder.getBindingTargets().firstOrNull{ it.isUsed() && it.isBinder()} != null
            if (!hasBinders) {
                tab("sIncludes = null;")
            } else {
                val numBindings = layoutBinder.getBindingTargets().filter{ it.isUsed() }.count()
                tab("sIncludes = new android.databinding.ViewDataBinding.IncludedLayouts(${numBindings});")
                val includeMap = HashMap<BindingTarget, ArrayList<BindingTarget>>()
                layoutBinder.getBindingTargets().filter{ it.isUsed() && it.isBinder() }.forEach {
                    val includeTag = it.getTag();
                    val parent = layoutBinder.getBindingTargets().firstOrNull {
                        it.isUsed() && !it.isBinder() && includeTag.equals(it.getTag())
                    }
                    if (parent == null) {
                        throw IllegalStateException("Could not find parent of include file")
                    }
                    var list = includeMap.get(parent)
                    if (list == null) {
                        list = ArrayList<BindingTarget>()
                        includeMap.put(parent, list)
                    }
                    list.add(it)
                }

                includeMap.keySet().forEach {
                    val index = indices.get(it)
                    tab("sIncludes.setIncludes(${index}, ") {
                        tab ("new String[] {${
                        includeMap.get(it).map {
                            "\"${it.getIncludedLayout()}\""
                        }.joinToString(", ")
                        }},")
                        tab("new int[] {${
                        includeMap.get(it).map {
                            "${indices.get(it)}"
                        }.joinToString(", ")
                        }},")
                        tab("new int[] {${
                        includeMap.get(it).map {
                            "R.layout.${it.getIncludedLayout()}"
                        }.joinToString(", ")
                        }});")
                    }
                }
            }
            val viewsWithIds = layoutBinder.getBindingTargets().filter {
                it.isUsed() && !it.isBinder() && (!it.supportsTag() || (it.getId() != null && it.getTag() == null))
            }
            if (viewsWithIds.isEmpty()) {
                tab("sViewsWithIds = null;")
            } else {
                tab("sViewsWithIds = new android.util.SparseIntArray();")
                viewsWithIds.forEach {
                    tab("sViewsWithIds.put(${it.androidId}, ${indices.get(it)});")
                }
            }
        }
        nl("}")
    }

    fun maxIndex() : kotlin.Int {
        val maxIndex = indices.values().max()
        if (maxIndex == null) {
            return -1
        } else {
            return maxIndex
        }
    }

    fun declareConstructor(minSdk : kotlin.Int) = kcode("") {
        val bindingCount = maxIndex() + 1
        val parameterType : String
        val superParam : String
        if (layoutBinder.isMerge()) {
            parameterType = "View[]"
            superParam = "root[0]"
        } else {
            parameterType = "View"
            superParam = "root"
        }
        val rootTagsSupported = minSdk >= 14
        if (layoutBinder.hasVariations()) {
            nl("")
            nl("public ${className}(android.databinding.DataBindingComponent bindingComponent, ${parameterType} root) {") {
                tab("this(bindingComponent, ${superParam}, mapBindings(bindingComponent, root, ${bindingCount}, sIncludes, sViewsWithIds));")
            }
            nl("}")
            nl("private ${className}(android.databinding.DataBindingComponent bindingComponent, ${parameterType} root, Object[] bindings) {") {
                tab("super(bindingComponent, ${superParam}, ${model.getObservables().size()}") {
                    layoutBinder.getSortedTargets().filter { it.getId() != null }.forEach {
                        tab(", ${fieldConversion(it)}")
                    }
                    tab(");")
                }
            }
        } else {
            nl("public ${baseClassName}(android.databinding.DataBindingComponent bindingComponent, ${parameterType} root) {") {
                tab("super(bindingComponent, ${superParam}, ${model.getObservables().size()});")
                tab("final Object[] bindings = mapBindings(bindingComponent, root, ${bindingCount}, sIncludes, sViewsWithIds);")
            }
        }
        if (layoutBinder.requiredComponent != null) {
            tab("ensureBindingComponentIsNotNull(${layoutBinder.requiredComponent}.class);")
        }
        val taggedViews = layoutBinder.getSortedTargets().filter{it.isUsed()}
        taggedViews.forEach {
            if (!layoutBinder.hasVariations() || it.getId() == null) {
                tab("this.${it.fieldName} = ${fieldConversion(it)};")
            }
            if (!it.isBinder()) {
                if (it.getResolvedType() != null && it.getResolvedType().extendsViewStub()) {
                    tab("this.${it.fieldName}.setContainingBinding(this);")
                }
                if (it.supportsTag() && it.getTag() != null &&
                        (rootTagsSupported || it.getTag().startsWith("binding_"))) {
                    val originalTag = it.getOriginalTag();
                    var tagValue = "null"
                    if (originalTag != null) {
                        tagValue = "\"${originalTag}\""
                        if (originalTag.startsWith("@")) {
                            var packageName = layoutBinder.getModulePackage()
                            if (originalTag.startsWith("@android:")) {
                                packageName = "android"
                            }
                            val slashIndex = originalTag.indexOf('/')
                            val resourceId = originalTag.substring(slashIndex + 1)
                            tagValue = "root.getResources().getString(${packageName}.R.string.${resourceId})"
                        }
                    }
                    tab("this.${it.fieldName}.setTag(${tagValue});")
                }
            }
        }
        tab("setRootTag(root);")
        tab("invalidateAll();");
        nl("}")
    }

    fun fieldConversion(target : BindingTarget) : String {
        if (!target.isUsed()) {
            return "null"
        } else {
            val index = indices.get(target)
            if (index == null) {
                throw IllegalStateException("Unknown binding target")
            }
            val variableName = "bindings[${index}]"
            return target.superConversion(variableName)
        }
    }

    fun declareInvalidateAll() = kcode("") {
        nl("@Override")
        nl("public void invalidateAll() {") {
            val fs = FlagSet(layoutBinder.getModel().getInvalidateAnyBitSet(),
                    layoutBinder.getModel().getFlagBucketCount());
            tab("synchronized(this) {") {
                for (i in (0..(mDirtyFlags.buckets.size() - 1))) {
                    tab("${mDirtyFlags.localValue(i)} = ${fs.localValue(i)};")
                }
            } tab("}")
            includedBinders.filter{it.isUsed()}.forEach { binder ->
                tab("${binder.fieldName}.invalidateAll();")
            }
            tab("requestRebind();");
        }
        nl("}")
    }

    fun declareHasPendingBindings()  = kcode("") {
        nl("@Override")
        nl("public boolean hasPendingBindings() {") {
            if (mDirtyFlags.buckets.size() > 0) {
                tab("synchronized(this) {") {
                    val flagCheck = 0.rangeTo(mDirtyFlags.buckets.size() - 1).map {
                            "${mDirtyFlags.localValue(it)} != 0"
                    }.joinToString(" || ")
                    tab("if (${flagCheck}) {") {
                        tab("return true;")
                    }
                    tab("}")
                }
                tab("}")
            }
            includedBinders.filter{it.isUsed()}.forEach { binder ->
                tab("if (${binder.fieldName}.hasPendingBindings()) {") {
                    tab("return true;")
                }
                tab("}")
            }
            tab("return false;")
        }
        nl("}")
    }

    fun declareSetVariable() = kcode("") {
        nl("public boolean setVariable(int variableId, Object variable) {") {
            tab("switch(variableId) {") {
                usedVariables.forEach {
                    tab ("case ${it.getName().br()} :") {
                        tab("${it.setterName}((${it.getResolvedType().toJavaCode()}) variable);")
                        tab("return true;")
                    }
                }
            }
            tab("}")
            tab("return false;")
        }
        nl("}")
    }

    fun declareLog() = kcode("") {
        nl("private void log(String msg, long i) {") {
            tab("""android.util.Log.d("BINDER", msg + ":" + Long.toHexString(i));""")
        }
        nl("}")
    }

    fun variableSettersAndGetters() = kcode("") {
        variables.filterNot{it.isUsed()}.forEach {
            nl("public void ${it.setterName}(${it.getResolvedType().toJavaCode()} ${it.readableName}) {") {
                tab("// not used, ignore")
            }
            nl("}")
            nl("")
            nl("public ${it.getResolvedType().toJavaCode()} ${it.getterName}() {") {
                tab("return ${it.getDefaultValue()};")
            }
            nl("}")
        }
        usedVariables.forEach {
            if (it.getUserDefinedType() != null) {
                nl("public void ${it.setterName}(${it.getResolvedType().toJavaCode()} ${it.readableName}) {") {
                    if (it.isObservable()) {
                        tab("updateRegistration(${it.getId()}, ${it.readableName});");
                    }
                    tab("this.${it.fieldName} = ${it.readableName};")
                    // set dirty flags!
                    val flagSet = it.invalidateFlagSet
                    tab("synchronized(this) {") {
                        mDirtyFlags.mapOr(flagSet) { suffix, index ->
                            tab("${mDirtyFlags.getLocalName()}$suffix |= ${flagSet.localValue(index)};")
                        }
                    } tab ("}")
                    tab("super.requestRebind();")
                }
                nl("}")
                nl("")
                nl("public ${it.getResolvedType().toJavaCode()} ${it.getterName}() {") {
                    tab("return ${it.fieldName};")
                }
                nl("}")
            }
        }
    }

    fun onFieldChange() = kcode("") {
        nl("@Override")
        nl("protected boolean onFieldChange(int localFieldId, Object object, int fieldId) {") {
            tab("switch (localFieldId) {") {
                model.getObservables().forEach {
                    tab("case ${it.getId()} :") {
                        tab("return ${it.onChangeName}((${it.getResolvedType().toJavaCode()}) object, fieldId);")
                    }
                }
            }
            tab("}")
            tab("return false;")
        }
        nl("}")
        nl("")

        model.getObservables().forEach {
            nl("private boolean ${it.onChangeName}(${it.getResolvedType().toJavaCode()} ${it.readableName}, int fieldId) {") {
                tab("switch (fieldId) {", {
                    val accessedFields: List<FieldAccessExpr> = it.getParents().filterIsInstance(javaClass<FieldAccessExpr>())
                    accessedFields.filter { it.hasBindableAnnotations() }
                            .groupBy { it.getName() }
                            .forEach {
                                tab("case ${it.key.br()}:") {
                                    val field = it.value.first()
                                    tab("synchronized(this) {") {
                                        mDirtyFlags.mapOr(field.invalidateFlagSet) { suffix, index ->
                                            tab("${mDirtyFlags.localValue(index)} |= ${field.invalidateFlagSet.localValue(index)};")
                                        }
                                    } tab("}")
                                    tab("return true;")
                                }

                            }
                    tab("case ${"".br()}:") {
                        val flagSet = it.invalidateFlagSet
                        tab("synchronized(this) {") {
                            mDirtyFlags.mapOr(flagSet) { suffix, index ->
                                tab("${mDirtyFlags.getLocalName()}$suffix |= ${flagSet.localValue(index)};")
                            }
                        } tab("}")
                        tab("return true;")
                    }

                })
                tab("}")
                tab("return false;")
            }
            nl("}")
            nl("")
        }
    }

    fun declareViews() = kcode("// views") {
        val oneLayout = !layoutBinder.hasVariations();
        layoutBinder.getSortedTargets().filter {it.isUsed() && (oneLayout || it.getId() == null)}.forEach {
            val access : String
            if (oneLayout && it.getId() != null) {
                access = "public"
            } else {
                access = "private"
            }
            nl("${access} final ${it.interfaceType} ${it.fieldName};")
        }
    }

    fun declareVariables() = kcode("// variables") {
        usedVariables.forEach {
            nl("private ${it.getResolvedType().toJavaCode()} ${it.fieldName};")
        }
    }

    fun declareBoundValues() = kcode("// values") {
        layoutBinder.getSortedTargets().filter { it.isUsed() }
                .flatMap { it.getBindings() }
                .filter { it.requiresOldValue() }
                .flatMap{ it.getComponentExpressions().toArrayList() }
                .groupBy { it }
                .forEach {
                    val expr = it.getKey()
                    nl("private ${expr.getResolvedType().toJavaCode()} ${expr.oldValueName};")
                }
    }

    fun declareListeners() = kcode("// listeners") {
        model.getExprMap().values().filter {
            it is FieldAccessExpr && it.isListener()
        }.groupBy { it }.forEach {
            val expr = it.key as FieldAccessExpr
            nl("private ${expr.listenerClassName} ${expr.fieldName};")
        }
    }

    fun declareDirtyFlags() = kcode("// dirty flag") {
        model.ext.localizedFlags.forEach { flag ->
            flag.notEmpty { suffix, value ->
                nl("private")
                app(" ", if(flag.isDynamic()) null else "static final");
                app(" ", " ${flag.type} ${flag.getLocalName()}$suffix = ${longToBinary(value)};")
            }
        }
    }

    fun flagMapping() = kcode("/* flag mapping") {
        if (model.getFlagMapping() != null) {
            val mapping = model.getFlagMapping()
            for (i in mapping.indices) {
                tab("flag $i: ${mapping[i]}")
            }
        }
        nl("flag mapping end*/")
    }

    fun executePendingBindings() = kcode("") {
        nl("@Override")
        nl("protected void executeBindings() {") {
            val tmpDirtyFlags = FlagSet(mDirtyFlags.buckets)
            tmpDirtyFlags.setLocalName("dirtyFlags");
            for (i in (0..mDirtyFlags.buckets.size() - 1)) {
                tab("${tmpDirtyFlags.type} ${tmpDirtyFlags.localValue(i)} = 0;")
            }
            tab("synchronized(this) {") {
                for (i in (0..mDirtyFlags.buckets.size() - 1)) {
                    tab("${tmpDirtyFlags.localValue(i)} = ${mDirtyFlags.localValue(i)};")
                    tab("${mDirtyFlags.localValue(i)} = 0;")
                }
            } tab("}")
            model.getPendingExpressions().filterNot {!it.canBeEvaluatedToAVariable() || (it.isVariable() && !it.isUsed())}.forEach {
                tab("${it.getResolvedType().toJavaCode()} ${it.executePendingLocalName} = ${if(it.isVariable()) it.fieldName else it.getDefaultValue()};")
            }
            L.d("writing executePendingBindings for %s", className)
            do {
                val batch = ExprModel.filterShouldRead(model.getPendingExpressions()).toArrayList()
                L.d("batch: %s", batch)
                val mJustRead = arrayListOf<Expr>()
                while (!batch.none()) {
                    val readNow = batch.filter { it.shouldReadNow(mJustRead) }
                    if (readNow.isEmpty()) {
                        throw IllegalStateException("do not know what I can read. bailing out ${batch.joinToString("\n")}")
                    }
                    L.d("new read now. batch size: %d, readNow size: %d", batch.size(), readNow.size())

                    readNow.forEach {
                        nl(readWithDependants(it, mJustRead, batch, tmpDirtyFlags))
                    }
                    batch.removeAll(mJustRead)
                }
                tab("// batch finished")
            } while(model.markBitsRead())
            // verify everything is read.
            val batch = ExprModel.filterShouldRead(model.getPendingExpressions()).toArrayList()
            if (batch.isNotEmpty()) {
                L.e("could not generate code for %s. This might be caused by circular dependencies."
                        + "Please report on b.android.com", layoutBinder.getLayoutname())
            }
            //
            layoutBinder.getSortedTargets().filter { it.isUsed() }
                    .flatMap { it.getBindings() }
                    .groupBy { it.getExpr() }
                    .forEach {
                        val flagSet = it.key.dirtyFlagSet
                        tab("if (${tmpDirtyFlags.mapOr(flagSet){ suffix, index ->
                            "(${tmpDirtyFlags.localValue(index)} & ${flagSet.localValue(index)}) != 0"
                        }.joinToString(" || ")
                        }) {") {
                            it.value.forEach { binding ->
                                tab("// api target ${binding.getMinApi()}")
                                val fieldName : String
                                if (binding.getTarget().getViewClass().
                                        equals(binding.getTarget().getInterfaceType())) {
                                    fieldName = "this.${binding.getTarget().fieldName}"
                                } else {
                                    fieldName = "((${binding.getTarget().getViewClass()}) this.${binding.getTarget().fieldName})"
                                }
                                val bindingCode = binding.toJavaCode(fieldName, "this.mBindingComponent")
                                if (binding.getMinApi() > 1) {
                                    tab("if(getBuildSdkInt() >= ${binding.getMinApi()}) {") {
                                        tab("$bindingCode;")
                                    }
                                    tab("}")
                                } else {
                                    tab("$bindingCode;")
                                }
                            }
                        }
                        tab("}")
                    }

            layoutBinder.getSortedTargets().filter { it.isUsed() }
                    .flatMap { it.getBindings() }
                    .filter { it.requiresOldValue() }
                    .groupBy { it.getExpr() }
                    .forEach {
                        val flagSet = it.key.dirtyFlagSet
                        tab("if (${tmpDirtyFlags.mapOr(flagSet) { suffix, index ->
                            "(${tmpDirtyFlags.localValue(index)} & ${flagSet.localValue(index)}) != 0"
                        }.joinToString(" || ")
                        }) {") {
                            it.value.first().getComponentExpressions().forEach { expr ->
                                tab("this.${expr.oldValueName} = ${expr.toCode(false).generate()};")
                            }
                        }
                        tab("}")
                    }
            includedBinders.filter{it.isUsed()}.forEach { binder ->
                tab("${binder.fieldName}.executePendingBindings();")
            }
            layoutBinder.getSortedTargets().filter{
                it.isUsed() && it.getResolvedType() != null && it.getResolvedType().extendsViewStub()
            }.forEach {
                tab("if (${it.fieldName}.getBinding() != null) {") {
                    tab("${it.fieldName}.getBinding().executePendingBindings();")
                }
                tab("}")
            }
        }
        nl("}")
    }

    fun readWithDependants(expr : Expr, mJustRead : MutableList<Expr>, batch : MutableList<Expr>,
            tmpDirtyFlags : FlagSet, inheritedFlags : FlagSet? = null) : KCode = kcode("") {
        mJustRead.add(expr)
        L.d("%s / readWithDependants %s", className, expr.getUniqueKey());
        val flagSet = expr.shouldReadFlagSet
        val needsIfWrapper = inheritedFlags == null || !flagSet.bitsEqual(inheritedFlags)
        L.d("flag set:%s . inherited flags: %s. need another if: %s", flagSet, inheritedFlags, needsIfWrapper);
        val ifClause = "if (${tmpDirtyFlags.mapOr(flagSet){ suffix, index ->
            "(${tmpDirtyFlags.localValue(index)} & ${flagSet.localValue(index)}) != 0"
        }.joinToString(" || ")
        })"

        val readCode = kcode("") {
            if (expr.canBeEvaluatedToAVariable() && !expr.isVariable()) {
                // it is not a variable read it.
                tab("// read ${expr.getUniqueKey()}")
                // create an if case for all dependencies that might be null
                val nullables = expr.getDependencies().filter {
                    it.isMandatory() && it.getOther().getResolvedType().isNullable()
                }.map { it.getOther() }
                if (!expr.isEqualityCheck() && nullables.isNotEmpty()) {
                    tab ("if ( ${nullables.map { "${it.executePendingLocalName} != null" }.joinToString(" && ")}) {") {
                        tab("${expr.executePendingLocalName}").app(" = ", expr.toCode(true)).app(";")
                    }
                    tab("}")
                } else {
                    tab("${expr.executePendingLocalName}").app(" = ", expr.toCode(true)).app(";")
                }
                if (expr.isObservable()) {
                    tab("updateRegistration(${expr.getId()}, ${expr.executePendingLocalName});")
                }
            }

            // if I am the condition for an expression, set its flag
            val conditionals = expr.getDependants().filter { !it.isConditional()
                    && it.getDependant() is TernaryExpr && (it.getDependant() as TernaryExpr).getPred() == expr }
                    .map { it.getDependant() }
            if (conditionals.isNotEmpty()) {
                tab("// setting conditional flags")
                tab("if (${expr.executePendingLocalName}) {") {
                    conditionals.forEach {
                        val set = it.getRequirementFlagSet(true)
                        mDirtyFlags.mapOr(set) { suffix , index ->
                            tab("${tmpDirtyFlags.localValue(index)} |= ${set.localValue(index)};")
                        }
                    }
                }
                tab("} else {") {
                    conditionals.forEach {
                        val set = it.getRequirementFlagSet(false)
                        mDirtyFlags.mapOr(set) { suffix , index ->
                            tab("${tmpDirtyFlags.localValue(index)} |= ${set.localValue(index)};")
                        }
                    }
                } tab("}")
            }

            val chosen = expr.getDependants().filter {
                val dependant = it.getDependant()
                batch.contains(dependant) &&
                        dependant.shouldReadFlagSet.andNot(flagSet).isEmpty() &&
                        dependant.shouldReadNow(mJustRead)
            }
            if (chosen.isNotEmpty()) {
                val nextInheritedFlags = if (needsIfWrapper) flagSet else inheritedFlags
                chosen.forEach {
                    nl(readWithDependants(it.getDependant(), mJustRead, batch, tmpDirtyFlags, nextInheritedFlags))
                }
            }
        }
        if (needsIfWrapper) {
            tab(ifClause) {
                app(" {")
                nl(readCode)
            }
            tab("}")
        } else {
            nl(readCode)
        }
    }

    fun declareListenerImpls() = kcode("// Listener Stub Implementations") {
        model.getExprMap().values().filter {
            it.isUsed() && it is FieldAccessExpr && it.isListener()
        }.groupBy { it }.forEach {
            val expr = it.key as FieldAccessExpr
            val listeners = expr.getListenerTypes()
            val extends = listeners.firstOrNull{ !it.isInterface() }
            val extendsImplements = StringBuilder()
            if (extends != null) {
                extendsImplements.append("extends ${extends.toJavaCode()} ");
            }
            val implements = expr.getListenerTypes().filter{ it.isInterface() }.map {
                it.toJavaCode()
            }.joinToString(", ")
            if (!implements.isEmpty()) {
                extendsImplements.append("implements ${implements}")
            }
            nl("public static class ${expr.listenerClassName} ${extendsImplements} {") {
                tab("public ${expr.listenerClassName}() {}")
                if (expr.getChild().isDynamic()) {
                    tab("private ${expr.getChild().getResolvedType().toJavaCode()} value;")
                    tab("public ${expr.listenerClassName} setValue(${expr.getChild().getResolvedType().toJavaCode()} value) {") {
                        tab("this.value = value;")
                        tab("return value == null ? null : this;")
                    }
                    tab("}")
                }
                val signatures = HashSet<String>()
                expr.getListenerMethods().withIndex().forEach {
                    val listener = it.value
                    val calledMethod = expr.getCalledMethods().get(it.index)
                    val parameterTypes = listener.getParameterTypes()
                    val returnType = listener.getReturnType(parameterTypes.toArrayList())
                    val signature = "public ${returnType} ${listener.getName()}(${
                    parameterTypes.withIndex().map {
                        "${it.value.toJavaCode()} arg${it.index}"
                    }.joinToString(", ")
                    }) {"
                    if (!signatures.contains(signature)) {
                        signatures.add(signature)
                        tab("@Override")
                        tab(signature) {
                            val obj : String
                            if (expr.getChild().isDynamic()) {
                                obj = "this.value"
                            } else {
                                obj = expr.getChild().toCode(false).generate();
                            }
                            val returnStr : String
                            if (!returnType.isVoid()) {
                                returnStr = "return "
                            } else {
                                returnStr = ""
                            }
                            val args = parameterTypes.withIndex().map {
                                "arg${it.index}"
                            }.joinToString(", ")
                            tab("${returnStr}${obj}.${calledMethod.getName()}(${args});")
                        }
                        tab("}")
                    }
                }
            }
            nl("}")
        }
    }

    fun declareFactories() = kcode("") {
        nl("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot) {") {
            tab("return inflate(inflater, root, attachToRoot, android.databinding.DataBindingUtil.getDefaultComponent());")
        }
        nl("}")
        nl("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot, android.databinding.DataBindingComponent bindingComponent) {") {
            tab("return android.databinding.DataBindingUtil.<${baseClassName}>inflate(inflater, ${layoutBinder.getModulePackage()}.R.layout.${layoutBinder.getLayoutname()}, root, attachToRoot, bindingComponent);")
        }
        nl("}")
        if (!layoutBinder.isMerge()) {
            nl("public static ${baseClassName} inflate(android.view.LayoutInflater inflater) {") {
                tab("return inflate(inflater, android.databinding.DataBindingUtil.getDefaultComponent());")
            }
            nl("}")
            nl("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.databinding.DataBindingComponent bindingComponent) {") {
                tab("return bind(inflater.inflate(${layoutBinder.getModulePackage()}.R.layout.${layoutBinder.getLayoutname()}, null, false), bindingComponent);")
            }
            nl("}")
            nl("public static ${baseClassName} bind(android.view.View view) {") {
                tab("return bind(view, android.databinding.DataBindingUtil.getDefaultComponent());")
            }
            nl("}")
            nl("public static ${baseClassName} bind(android.view.View view, android.databinding.DataBindingComponent bindingComponent) {") {
                tab("if (!\"${layoutBinder.getTag()}_0\".equals(view.getTag())) {") {
                    tab("throw new RuntimeException(\"view tag isn't correct on view:\" + view.getTag());")
                }
                tab("}")
                tab("return new ${baseClassName}(bindingComponent, view);")
            }
            nl("}")
        }
    }

    /**
     * When called for a library compilation, we do not generate real implementations
     */
    public fun writeBaseClass(forLibrary : Boolean) : String =
        kcode("package ${layoutBinder.getPackage()};") {
            nl("import android.databinding.Bindable;")
            nl("import android.databinding.DataBindingUtil;")
            nl("import android.databinding.ViewDataBinding;")
            nl("public abstract class ${baseClassName} extends ViewDataBinding {")
            layoutBinder.getSortedTargets().filter{it.getId() != null}.forEach {
                tab("public final ${it.interfaceType} ${it.fieldName};")
            }
            nl("")
            tab("protected ${baseClassName}(android.databinding.DataBindingComponent bindingComponent, android.view.View root_, int localFieldCount") {
                layoutBinder.getSortedTargets().filter{it.getId() != null}.forEach {
                    tab(", ${it.interfaceType} ${it.constructorParamName}")
                }
            }
            tab(") {") {
                tab("super(bindingComponent, root_, localFieldCount);")
                layoutBinder.getSortedTargets().filter{it.getId() != null}.forEach {
                    tab("this.${it.fieldName} = ${it.constructorParamName};")
                }
            }
            tab("}")
            nl("")
            variables.forEach {
                if (it.getUserDefinedType() != null) {
                    val type = ModelAnalyzer.getInstance().applyImports(it.getUserDefinedType(), model.getImports())
                    tab("public abstract void ${it.setterName}(${type} ${it.readableName});")
                }
            }
            tab("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot) {") {
                tab("return inflate(inflater, root, attachToRoot, android.databinding.DataBindingUtil.getDefaultComponent());")
            }
            tab("}")
            tab("public static ${baseClassName} inflate(android.view.LayoutInflater inflater) {") {
                tab("return inflate(inflater, android.databinding.DataBindingUtil.getDefaultComponent());")
            }
            tab("}")
            tab("public static ${baseClassName} bind(android.view.View view) {") {
                if (forLibrary) {
                    tab("return null;")
                } else {
                    tab("return bind(view, android.databinding.DataBindingUtil.getDefaultComponent());")
                }
            }
            tab("}")
            tab("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.view.ViewGroup root, boolean attachToRoot, android.databinding.DataBindingComponent bindingComponent) {") {
                if (forLibrary) {
                    tab("return null;")
                } else {
                    tab("return DataBindingUtil.<${baseClassName}>inflate(inflater, ${layoutBinder.getModulePackage()}.R.layout.${layoutBinder.getLayoutname()}, root, attachToRoot, bindingComponent);")
                }
            }
            tab("}")
            tab("public static ${baseClassName} inflate(android.view.LayoutInflater inflater, android.databinding.DataBindingComponent bindingComponent) {") {
                if (forLibrary) {
                    tab("return null;")
                } else {
                    tab("return DataBindingUtil.<${baseClassName}>inflate(inflater, ${layoutBinder.getModulePackage()}.R.layout.${layoutBinder.getLayoutname()}, null, false, bindingComponent);")
                }
            }
            tab("}")
            tab("public static ${baseClassName} bind(android.view.View view, android.databinding.DataBindingComponent bindingComponent) {") {
                if (forLibrary) {
                    tab("return null;")
                } else {
                    tab("return (${baseClassName})bind(bindingComponent, view, ${layoutBinder.getModulePackage()}.R.layout.${layoutBinder.getLayoutname()});")
                }
            }
            tab("}")
            nl("}")
        }.generate()
}
