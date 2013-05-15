package org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters;

import org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.parse.LValue;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.FieldVariable;
import org.benf.cfr.reader.bytecode.analysis.parse.lvalue.StaticVariable;
import org.benf.cfr.reader.bytecode.analysis.structured.StructuredStatement;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.Block;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredAssignment;
import org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredComment;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.util.Functional;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.Predicate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lee
 * Date: 14/05/2013
 * Time: 17:20
 */
public class StaticLifter {

    private final ClassFile classFile;

    public StaticLifter(ClassFile classFile) {
        this.classFile = classFile;
    }

    public void liftStatics(Method staticInit) {

        // All uninitialised static fields, in definition order.
        LinkedList<ClassFileField> classFileFields = new LinkedList<ClassFileField>(Functional.filter(classFile.getFields(), new Predicate<ClassFileField>() {
            @Override
            public boolean test(ClassFileField in) {
                if (!in.getField().testAccessFlag(AccessFlag.ACC_STATIC)) return false;
                if (in.getInitialValue() != null) return false;
                return true;
            }
        }));

        Op04StructuredStatement staticCode = staticInit.getAnalysis();
        /* We use a LUDICROUSLY simple plan - while the first line is a valid static initialiser, we move it into
         * static init code.
         * (Only exception, we skip over comments).
         *
         * (We also need to make sure that initialisation is performed in declaration order).
         *
         * This means we don't need to worry about illegal 'natural' initialisation code, as we can't have any
         * temporaries, and we can't be out of order.
         */
        StructuredStatement topCode = staticCode.getStatement();
        if (!(topCode instanceof Block)) return;

        Block block = (Block) topCode;
        List<Op04StructuredStatement> statements = block.getBlockStatements();

        /*
         * Explicit iterator so we can remove.
         */
        Iterator<Op04StructuredStatement> iterator = statements.iterator();
        while (iterator.hasNext()) {
            Op04StructuredStatement statement = iterator.next();
            StructuredStatement structuredStatement = statement.getStatement();
            if (structuredStatement instanceof StructuredComment) continue;
            if (!(structuredStatement instanceof StructuredAssignment)) break;

            StructuredAssignment assignment = (StructuredAssignment) structuredStatement;
            if (!liftStatic(assignment, classFileFields)) return;
        }
    }

    private boolean liftStatic(StructuredAssignment assignment, LinkedList<ClassFileField> classFileFields) {
        LValue lValue = assignment.getLvalue();
        if (!(lValue instanceof StaticVariable)) return false;
        StaticVariable fieldVariable = (StaticVariable) lValue;
        ClassFileField field;
        try {
            field = classFile.getFieldByName(fieldVariable.getVarName());
        } catch (NoSuchFieldException e) {
            return false;
        }
        if (field != classFileFields.getFirst()) return false;
        classFileFields.removeFirst();

        // by definition, but let's check again....
        if (field.getInitialValue() != null) return false;

        field.setInitialValue(assignment.getRvalue());
        assignment.getContainer().nopOut();
        return true;
    }
}