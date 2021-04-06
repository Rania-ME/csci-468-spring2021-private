package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.statements.*;

import java.util.LinkedList;
import java.util.List;

public class FunctionCallExpression extends Expression {
    private final String name;
    List<Expression> arguments;
    private CatscriptType type;

    public FunctionCallExpression(String functionName, List<Expression> arguments) {
        this.arguments = new LinkedList<>();
        for (Expression value : arguments) {
            this.arguments.add(addChild(value));
        }
        this.name = functionName;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    public String getName() {
        return name;
    }

    @Override
    public CatscriptType getType() {
        return type;
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        FunctionDefinitionStatement function = symbolTable.getFunction(getName());
        if (function == null) {
            addError(ErrorType.UNKNOWN_NAME);
            type = CatscriptType.OBJECT;
        } else {
            type = function.getType();
            if (arguments.size() != function.getParameterCount()) {
                addError(ErrorType.ARG_MISMATCH);
            } else {
                for (int i = 0; i < arguments.size(); i++) {
                    Expression argument = arguments.get(i);
                    argument.validate(symbolTable);
                    CatscriptType parameterType = function.getParameterType(i);
                    if (!parameterType.isAssignableFrom(argument.getType())) {
                        argument.addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
            }
        }
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        //return super.evaluate(runtime);
        //FunctionDefinitionStatement func_def = (FunctionDefinitionStatement)((CatScriptProgram)parent).getStatements().get(0);
        FunctionDefinitionStatement func_def = (FunctionDefinitionStatement)((CatScriptProgram)(parent.getParent())).getStatements().get(0);
        List<Statement> func_body = func_def.getBody();
        boolean found;
        for (int i=0;i<func_body.size();i++) {
            Statement statement_i = func_body.get(i);
            found=false;
            if (statement_i instanceof PrintStatement) {
                if (((PrintStatement)statement_i).getExpression() instanceof IdentifierExpression) {
                    for (int j=0;j<func_def.getParameterCount();j++) {
                        if (((IdentifierExpression) ((PrintStatement) statement_i).getExpression()).getName().equals(func_def.getParameterName(j))) {
                            ((PrintStatement)statement_i).setExpression(getArguments().get(j));
                            statement_i.execute(runtime);
                            found=true;
                        }
                    }
                }
                if (!found) {
                    statement_i.execute(runtime);
                }
            } else if (statement_i instanceof ReturnStatement) {
                if (((ReturnStatement)statement_i).getExpression() instanceof IdentifierExpression) {
                    for (int j=0;j<func_def.getParameterCount();j++) {
                        if (((IdentifierExpression) ((ReturnStatement) statement_i).getExpression()).getName().equals(func_def.getParameterName(j))) {
                            ((ReturnStatement)statement_i).setExpression(getArguments().get(j));
                            statement_i.execute(runtime);
                            found=true;
                        }
                    }
                }
                if (!found) {
                    return ((ReturnStatement)statement_i).getExpression().evaluate(runtime);
                }
            }
        }
        return null;

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        super.compile(code);
    }


}
