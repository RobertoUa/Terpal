package io.exoquery.terpal.plugin.transform

import io.exoquery.terpal.plugin.findMethodOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import kotlin.collections.plus

class CallGlobalMethod(private val invoke: IrSimpleFunctionSymbol, private val tpe: IrType?, private val extReciever: IrExpression?) {
  companion object {
    context(bc: BuilderContext) operator fun invoke(callable: CallableId, tpe: IrType?, extReciever: IrExpression?): CallGlobalMethod =
      CallGlobalMethod(bc.pluginCtx.referenceFunctions(callable).first(), tpe, extReciever)
    context(_: BuilderContext) operator fun invoke(funPath: String, funName: String, tpe: IrType?, extReciever: IrExpression?): CallGlobalMethod =
      CallGlobalMethod(CallableId(FqName(funPath), Name.identifier(funName)), tpe, extReciever)
  }

  context(bc: BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (bc.builder) {
      val invocation = if (tpe != null) irCall(invoke, tpe) else irCall(invoke)
      val allArgs = (extReciever?.let { listOf(it) } ?: emptyList()) + args.toList()
      invocation.apply {
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}

context(_: BuilderContext) fun callGlobalMethod(path: String, name: String, extReciever: IrExpression? = null) = CallGlobalMethod(path, name, null, extReciever)
context(_: BuilderContext) fun callGlobalMethod(callable: CallableId, extReciever: IrExpression? = null) = CallGlobalMethod(callable, null, extReciever)
fun callGlobalMethod(invoke: IrSimpleFunctionSymbol, extReciever: IrExpression? = null) = CallGlobalMethod(invoke, null, extReciever)


class CallMethod(private val host: IrExpression, private val lambdaInvoke: IrSimpleFunctionSymbol, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, funName: String, tpe: IrType?): CallMethod =
      CallMethod(host, host.type.findMethodOrFail(funName), tpe)
  }

  context(bc: BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (bc.builder) {
      val invocation = if (tpe != null) irCall(lambdaInvoke, tpe) else irCall(lambdaInvoke)
      val allArgs = listOf(host) + args.toList()
      invocation.apply {
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}

fun IrExpression.callMethod(name: String) = CallMethod(this, name, null)
fun IrExpression.callMethodWithType(name: String, tpe: IrType) = CallMethod(this, name, tpe)
fun IrExpression.callMethod(function: IrSimpleFunctionSymbol) = CallMethod(this, function, null)
fun IrExpression.callMethodWithType(function: IrSimpleFunctionSymbol, tpe: IrType) = CallMethod(this, function, tpe)

class CallMethodTypedArgs(private val host: IrExpression, private val function: IrSimpleFunctionSymbol, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, functionName: String, tpe: IrType?): CallMethodTypedArgs =
      CallMethodTypedArgs(host, host.type.findMethodOrFail(functionName), tpe)
  }

  operator fun invoke(vararg types: IrType): CallMethodTyped =
    CallMethodTyped(host, function, types.toList(), tpe)
}

class CallMethodTyped(private val dispatchArg: IrExpression, private val function: IrSimpleFunctionSymbol, private val types: List<IrType>, private val tpe: IrType?) {
  companion object {
    operator fun invoke(host: IrExpression, functionName: String, types: List<IrType>, tpe: IrType?): CallMethodTyped =
      CallMethodTyped(host, host.type.findMethodOrFail(functionName), types, tpe)
  }

  context(bc: BuilderContext) operator fun invoke(vararg args: IrExpression): IrExpression {
    return with (bc.builder) {
      val invocation = if (tpe != null) irCall(function, tpe) else irCall(function)
      invocation.apply {
        val allArgs = listOf(dispatchArg) + args.toList()
        for ((index, expr) in allArgs.withIndex()) {
          arguments[index] = expr
        }
      }
    }
  }
}


fun IrExpression.callMethodTyped(name: String): CallMethodTypedArgs = CallMethodTypedArgs(this, name, null)
fun IrExpression.callMethodTyped(function: IrSimpleFunctionSymbol): CallMethodTypedArgs = CallMethodTypedArgs(this, function, null)
fun IrExpression.callMethodTypedWithType(name: String, tpe: IrType): CallMethodTypedArgs = CallMethodTypedArgs(this, name, tpe)

context(_: BuilderContext) fun createLambda0(functionBody: IrExpression, functionParent: IrDeclarationParent): IrFunctionExpression =
  createLambdaN(functionBody, listOf(), functionParent)

context(bc: BuilderContext) fun createLambdaN(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrFunctionExpression =
  with(bc.builder) {
    val functionClosure = createLambdaClosure(functionBody, params, functionParent)

    val typeWith = params.map { it.type } + functionClosure.returnType
    val functionType =
      bc.pluginCtx.irBuiltIns.functionN(params.size)
        // Remember this is FunctionN<InputA, InputB, ... Output> so these input/output args need to be both specified here
        .typeWith(typeWith)

    IrFunctionExpressionImpl(startOffset, endOffset, functionType, functionClosure, IrStatementOrigin.LAMBDA)
  }

context(bc: BuilderContext) fun createLambdaClosure(functionBody: IrExpression, params: List<IrValueParameter>, functionParent: IrDeclarationParent): IrSimpleFunction {
  return with(bc.pluginCtx) {
    irFactory.buildFun {
      origin = IrDeclarationOrigin.LOCAL_FUNCTION
      name = SpecialNames.NO_NAME_PROVIDED
      visibility = DescriptorVisibilities.LOCAL
      returnType = functionBody.type
      modality = Modality.FINAL
      isSuspend = false
    }.apply {
      parent = functionParent

      if (params.size > 0) {
        parameters = params
      }
      /*
      VERY important here to create a new irBuilder from the symbol i.e. createIrBuilder because
      the return-point needs to be the caller-function (which kotlin gets from the irBuilder).
      If the builder in the BuilderContext is used it will return back to whatever context the
      TransformInterpolatorInvoke IrCall expression is coming from (and this will be a non-local return)
      and since the return-type is wrong it will fail with a very large error that ultimately says:
      RETURN: Incompatible return type
       */
      body = bc.pluginCtx.createIrBuilder(symbol).run {
        // don't use expr body, coroutine codegen can't generate for it.
        irBlockBody {
          +irReturn(functionBody)
        }
      }
    }
  }
}




/**
 * The interpolator receiver is referenced by the backend call and by every `wrap` call, so anything that is not a plain
 * read is evaluated once into a temporary. Callers must `deepCopyWithSymbols()` the returned reference at each use.
 */
fun stabilizeReceiver(expr: IrExpression, parent: IrDeclarationParent): Pair<IrExpression, IrVariable?> {
  val isPlainRead = expr is IrGetObjectValue || expr is IrConst || (expr is IrGetValue && (expr.symbol.owner as? IrVariable)?.isVar != true)
  if (isPlainRead) return expr to null
  val temp = buildVariable(parent, expr.startOffset, expr.endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, Name.identifier("tmp_interpolator"), expr.type).apply {
    initializer = expr
  }
  return IrGetValueImpl(expr.startOffset, expr.endOffset, temp.type, temp.symbol) to temp
}

context(bc: BuilderContext) fun withReceiverTemp(temp: IrVariable?, output: IrExpression): IrExpression =
  if (temp == null) output
  else bc.builder.irBlock(resultType = output.type) {
    +temp
    +output
  }

fun IrPluginContext.createIrBuilder(
  symbol: IrSymbol,
  startOffset: Int = UNDEFINED_OFFSET,
  endOffset: Int = UNDEFINED_OFFSET,
) = DeclarationIrBuilder(this, symbol, startOffset, endOffset)
