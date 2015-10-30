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

import android.databinding.tool.expr.*
import android.databinding.tool.reflection.Callable

class CodeGenUtil {
    companion object {
        fun toCode(it : Expr, full : Boolean) : KCode {
            if (it.isDynamic() && !full) {
                return kcode(it.executePendingLocalName)
            }
            return when (it) {
                is ComparisonExpr -> kcode("") {
                    app("", it.getLeft().toCode())
                    app(" ", it.getOp()).app(" ")
                    app("", it.getRight().toCode())
                }
                is InstanceOfExpr -> kcode("") {
                    app("", it.getExpr().toCode())
                    app(" instanceof ")
                    app("", it.getType().toJavaCode())
                }
                is FieldAccessExpr -> kcode("") {
                    if (it.isListener()) {
                        app("(")
                        if (it.getMinApi() > 1) {
                            app("", "(getBuildSdkInt() < ${it.getMinApi()}) ? null : ")
                        }
                        if (it.getChild().isDynamic()) {
                            val value = it.getChild().toCode().generate();
                            app("", "((${it.fieldName} == null) ? (${it.fieldName} = (new ${it.listenerClassName}()).setValue(${value})) : ${it.fieldName}.setValue(${value}))")
                        } else {
                            app("", "((${it.fieldName} == null) ? (${it.fieldName} = new ${it.listenerClassName}()) : ${it.fieldName})")
                        }
                        app(")")
                    } else {
                        app("", it.getChild().toCode())
                        if (it.getGetter().type == Callable.Type.FIELD) {
                            app(".", it.getGetter().name)
                        } else {
                            app(".", it.getGetter().name).app("()")
                        }
                    }
                }
                is GroupExpr -> kcode("(").app("", it.getWrapped().toCode()).app(")")
                is StaticIdentifierExpr -> kcode(it.getResolvedType().toJavaCode())
                is IdentifierExpr -> kcode(it.executePendingLocalName)
                is MathExpr -> kcode("") {
                    app("", it.getLeft().toCode())
                    app(it.getOp())
                    app("", it.getRight().toCode())
                }
                is UnaryExpr -> kcode("") {
                    app(it.getOp(), it.getExpr().toCode())
                }
                is BitShiftExpr -> kcode("") {
                    app("", it.getLeft().toCode())
                    app(it.getOp())
                    app("", it.getRight().toCode())
                }
                is MethodCallExpr -> kcode("") {
                    app("", it.getTarget().toCode())
                    app(".", it.getGetter().name)
                    app("(")
                    var first = true
                    it.getArgs().forEach {
                        apps(if (first) "" else ",", it.toCode())
                        first = false
                    }
                    app(")")
                }
                is SymbolExpr -> kcode(it.getText()) // TODO
                is TernaryExpr -> kcode("") {
                    app("", it.getPred().toCode())
                    app(" ? ", it.getIfTrue().toCode())
                    app(" : ", it.getIfFalse().toCode())
                }
                is ResourceExpr -> kcode("") {
                    app("", it.toJava())
                }
                is BracketExpr -> kcode("") {
                    app("", it.getTarget().toCode())
                    val bracketType = it.getAccessor()!!
                    when (bracketType) {
                        BracketExpr.BracketAccessor.ARRAY -> {
                            app("[", it.getArg().toCode())
                            app("]")
                        }
                        BracketExpr.BracketAccessor.LIST -> {
                            app(".get(")
                            if (it.argCastsInteger()) {
                                app("(Integer)")
                            }
                            app("", it.getArg().toCode())
                            app(")")
                        }
                        BracketExpr.BracketAccessor.MAP -> {
                            app(".get(", it.getArg().toCode())
                            app(")")
                        }
                    }
                }
                is CastExpr -> kcode("") {
                    app("(", it.getCastType())
                    app(") ", it.getCastExpr().toCode())
                }
                is ArgListExpr -> throw IllegalStateException("should never try to convert an argument expressions into code");
                else -> kcode("//NOT IMPLEMENTED YET")
            }
        }
    }
}