package org.benf.cfr.reader.bytecode.analysis.parse.expression;

import org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.PrimitiveBoxingRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.Expression;
import org.benf.cfr.reader.bytecode.analysis.parse.StatementContainer;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.CloneHelper;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriter;
import org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterFlags;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.*;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.entities.ConstantPool;
import org.benf.cfr.reader.entities.ConstantPoolEntryMethodRef;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.output.Dumper;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: lee
 * Date: 16/03/2012
 * Time: 17:26
 * To change this template use File | Settings | File Templates.
 */
public class SuperFunctionInvokation extends AbstractFunctionInvokation {
    private final ConstantPoolEntryMethodRef function;
    private Expression object;
    private final List<Expression> args;
    private final ConstantPool cp;
    private final MethodPrototype methodPrototype;

    public SuperFunctionInvokation(ConstantPool cp, ConstantPoolEntryMethodRef function, MethodPrototype methodPrototype, Expression object, List<Expression> args) {
        super(cp, function, methodPrototype, object, args);
        this.function = function;
        this.methodPrototype = methodPrototype;
        this.object = object;
        this.args = args;
        this.cp = cp;
    }

    @Override
    public Expression deepClone(CloneHelper cloneHelper) {
        return new SuperFunctionInvokation(cp, function, methodPrototype, cloneHelper.replaceOrClone(object), cloneHelper.replaceOrClone(args));
    }

    @Override
    public Expression replaceSingleUsageLValues(LValueRewriter lValueRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer) {
        object = object.replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer);
        for (int x = 0; x < args.size(); ++x) {
            args.set(x, args.get(x).replaceSingleUsageLValues(lValueRewriter, ssaIdentifiers, statementContainer));
        }
        return this;
    }

    @Override
    public Expression applyExpressionRewriter(ExpressionRewriter expressionRewriter, SSAIdentifiers ssaIdentifiers, StatementContainer statementContainer, ExpressionRewriterFlags flags) {
        object = expressionRewriter.rewriteExpression(object, ssaIdentifiers, statementContainer, flags);
        for (int x = 0; x < args.size(); ++x) {
            args.set(x, expressionRewriter.rewriteExpression(args.get(x), ssaIdentifiers, statementContainer, flags));
        }
        return this;
    }

    private boolean isSyntheticThisFirstArg() {
        JavaTypeInstance superType = function.getClassEntry().getTypeInstance();
        return superType.getInnerClassHereInfo().isHideSyntheticThis();
    }

    public boolean isEmptyIgnoringSynthetics() {
        return (args.size() == (isSyntheticThisFirstArg() ? 1 : 0));
    }

    @Override
    public boolean rewriteBoxing(PrimitiveBoxingRewriter boxingRewriter) {
        return false;
    }

    @Override
    public Dumper dump(Dumper d) {
        if (methodPrototype.getName().equals(MiscConstants.INIT_METHOD)) {
            d.print("super(");
        } else {
            d.print("super.").print(methodPrototype.getName()).print("(");
        }
        boolean first = true;

        int start = isSyntheticThisFirstArg() ? 1 : 0;
        for (int x = start; x < args.size(); ++x) {
            Expression arg = args.get(x);
            if (!first) d.print(", ");
            first = false;
            methodPrototype.dumpAppropriatelyCastedArgumentString(arg, x, d);
        }
        d.print(")");
        return d;
    }

    public Expression getObject() {
        return object;
    }

    public ConstantPoolEntryMethodRef getFunction() {
        return function;
    }

    public List<Expression> getArgs() {
        return args;
    }

    public Expression getAppropriatelyCastArgument(int idx) {
        return methodPrototype.getAppropriatelyCastedArgument(args.get(idx), idx);
    }

    public ConstantPool getCp() {
        return cp;
    }

    @Override
    public void collectUsedLValues(LValueUsageCollector lValueUsageCollector) {
        for (Expression expression : args) {
            expression.collectUsedLValues(lValueUsageCollector);
        }
    }

}
