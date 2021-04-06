package edu.montana.csci.csci468.parser.statements;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.parser.expressions.Expression;
import edu.montana.csci.csci468.parser.expressions.FunctionCallExpression;
import edu.montana.csci.csci468.parser.expressions.IdentifierExpression;

import java.util.List;

public class FunctionCallStatement extends Statement {
    private FunctionCallExpression expression;
    public FunctionCallStatement(FunctionCallExpression parseExpression) {
        this.expression = addChild(parseExpression);
    }

    public List<Expression> getArguments() {
        return expression.getArguments();
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        expression.validate(symbolTable);
    }

    public String getName() {
        return expression.getName();
    }

    //==============================================================
    // Implementation
    //==============================================================
    @Override
    public void execute(CatscriptRuntime runtime) {
        //super.execute(runtime);
        FunctionDefinitionStatement func_def = (FunctionDefinitionStatement)((CatScriptProgram)parent).getStatements().get(0);
        List<Statement> func_body = func_def.getBody();
        boolean found;
        for (int i=0;i<func_body.size();i++) {
            Statement statement_i = func_body.get(i);
            found=false;
            if (statement_i instanceof PrintStatement) {
                if (((PrintStatement)statement_i).getExpression() instanceof IdentifierExpression) {
                    for (int j=0;j<func_def.getParameterCount();j++) {
                        if (((IdentifierExpression) ((PrintStatement) statement_i).getExpression()).getName().equals(func_def.getParameterName(j))) {
                            ((PrintStatement)statement_i).setExpression(this.expression.getArguments().get(j));
                            statement_i.execute(runtime);
                            found=true;
                        }
                    }
                }
                if (!found) {
                    statement_i.execute(runtime);
                }
            }
        }
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
