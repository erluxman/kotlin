/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.contracts.model.structure

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.model.ESExpression
import org.jetbrains.kotlin.contracts.model.ESExpressionVisitor
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

open class ESVariable(val descriptor: ValueDescriptor) : ESValue(descriptor.type) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitVariable(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ESVariable

        if (descriptor != other.descriptor) return false

        return true
    }

    override fun hashCode(): Int = descriptor.hashCode()

    override fun toString(): String = descriptor.toString()
}

class ESConstant private constructor(val constantReference: ConstantReference, override val type: KotlinType) : ESValue(type) {
    override fun <T> accept(visitor: ESExpressionVisitor<T>): T = visitor.visitConstant(this)

    override fun equals(other: Any?): Boolean = other is ESConstant && constantReference == other.constantReference

    override fun hashCode(): Int = Objects.hashCode(constantReference)

    override fun toString(): String = constantReference.name

    fun isNullConstant(): Boolean =
        constantReference == ConstantReference.NULL || constantReference == ConstantReference.NOT_NULL

    fun isBooleanConstant(): Boolean =
        constantReference is BooleanConstantReference

    fun negate(): ESConstant =
        if (isBooleanConstant())
            booleanValue(constantReference == BooleanConstantReference.FALSE, type)
        else
            throw UnsupportedOperationException(constantReference.name)

    companion object {
        fun trueValue(builtIns: KotlinBuiltIns): ESConstant =
            booleanValue(true, builtIns)

        fun falseValue(builtIns: KotlinBuiltIns): ESConstant =
            booleanValue(false, builtIns)

        fun booleanValue(value: Boolean, builtIns: KotlinBuiltIns): ESConstant =
            booleanValue(value, builtIns.booleanType)

        fun booleanValue(value: Boolean, booleanType: KotlinType): ESConstant =
            ESConstant(if (value) BooleanConstantReference.TRUE else BooleanConstantReference.FALSE, booleanType)

        fun nullValue(builtIns: KotlinBuiltIns): ESConstant =
            ESConstant(ConstantReference.NULL, builtIns.nullableNothingType)

        fun notNullValue(builtIns: KotlinBuiltIns): ESConstant =
            ESConstant(ConstantReference.NOT_NULL, builtIns.anyType)

        fun wildcard(builtIns: KotlinBuiltIns): ESConstant =
            ESConstant(ConstantReference.WILDCARD, builtIns.nullableAnyType)
    }
}

// TODO: do not use DefaultBuiltIns here
fun Boolean.lift(): ESConstant =
    ESConstant.booleanValue(this, DefaultBuiltIns.Instance)

val ESExpression.constantReference: ConstantReference?
    get() = (this as? ESConstant)?.constantReference

internal val ESExpression.isTrue: Boolean
    get() = constantReference == BooleanConstantReference.TRUE

internal val ESExpression.isFalse: Boolean
    get() = constantReference == BooleanConstantReference.FALSE

internal val ESValue.isWildcard: Boolean
    get() = constantReference == ConstantReference.WILDCARD
