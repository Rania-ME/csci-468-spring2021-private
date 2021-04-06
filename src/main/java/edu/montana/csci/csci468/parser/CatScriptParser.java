package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.*;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;
    CatScriptProgram program = null;
    CatScriptProgram for_programs = null;
    //private Expression var_x = null;
    private Dictionary<String, Expression> var_dict = new Hashtable<>();
    private Dictionary<String, CatscriptType> type_dict = new Hashtable<>();
    //private Dictionary<String, Expression> for_dict = new Hashtable<>();
    //private TokenList for_tokens = null;
    private String token_src;
    private int for_indx = 0;
    private boolean value_set = true;
    private boolean consum = false;
    //private boolean in_for = false;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        token_src = source;

        // first parse an expression
        program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        if (tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }
        if (for_programs != null) {
            List<Statement> for_stments = for_programs.getStatements();
            for (int i = 0; i < for_stments.size(); i++) {
                program.addStatement(for_stments.get(i));
            }
        }
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        do {
            value_set = false;
            Statement statement = parseStatement();//RE
            if (statement != null) {//RE
                /*if (consum) {
                    if (tokens.match(RIGHT_BRACE)) {
                        tokens.consumeToken();
                    } else {
                        statement.
                    }
                    consum = false;
                }*/
                return statement;//RE
            }
            statement = parseFunctionDeclaration();//RE
            if (statement != null) {//RE
                /*if (consum) {
                    tokens.consumeToken();
                    consum = false;
                }*/
                return statement;//RE
            }
            statement = parsePrintStatement();//carson
            if (statement != null) {//RE
                /*if (consum) {
                    tokens.consumeToken();
                    consum = false;
                }*/
                return statement;//RE
            }
        } while (value_set);
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseStatement() {
        if (tokens.match(IF)) {
            IfStatement ifStatement = new IfStatement();
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            ifStatement.setEnd(require(RIGHT_PAREN, ifStatement));

            //Expression bool_exp = ifStatement.getExpression();
            //if (bool_exp instanceof  BooleanLiteralExpression) {
            tokens.consumeToken();
            Statement expr = parseProgramStatement();
            List<Statement> statements = new LinkedList<>();
            statements.add(expr);
            ifStatement.setTrueStatements(statements);
            if (tokens.match(RIGHT_BRACE)) {
                tokens.consumeToken();
            } else {
                ifStatement.addError(ErrorType.UNEXPECTED_TOKEN);
            }
            if (tokens.match(ELSE)) {
                tokens.consumeToken();//else
                tokens.consumeToken();//{
                if (!tokens.hasMoreTokens()) {
                    ifStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                    return ifStatement;
                }
                expr = parseProgramStatement();
                statements = new LinkedList<>();
                statements.add(expr);
                ifStatement.setElseStatements(statements);
                if (tokens.match(RIGHT_BRACE)) {
                    tokens.consumeToken();
                } else {
                    ifStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                }
            }
            return ifStatement;
            /*} else if (bool_exp instanceof ComparisonExpression) {
                tokens.consumeToken(); //left bracket
                Statement expr = parseProgramStatement();

                tokens.consumeToken(); //right bracket
                while (tokens.match(ELSE)) {
                    tokens.consumeToken();
                    tokens.consumeToken();
                    Statement garb = parseProgramStatement();
                    tokens.consumeToken();
                }
                return expr;
            } else {
                return null;
            }*/
        } else if (tokens.match(VAR)) {
            VariableStatement varState = new VariableStatement();
            Token tok = tokens.consumeToken();//var
            //boolean is_list = false;
            Expression id = parseExpression();//var name
            //set explicit type
            while (!tokens.match(EQUAL)) {
                if (tokens.match(COLON)) {
                    tokens.consumeToken();//:
                    /*Expression type = parseExpression();
                    if (type instanceof IdentifierExpression && ((IdentifierExpression)type).getName().equals("object")) {
                        varState.setExplicitType(CatscriptType.OBJECT);
                    }
                    Token type = tokens.consumeToken();
                    if (type.getStringValue().equals("object")) {
                        varState.setExplicitType(CatscriptType.OBJECT);
                    }*/
                    if (tokens.getCurrentToken().getStringValue().equals("list")) {
                        //is_list = true;
                        while (!tokens.match(EQUAL)) {
                            tokens.consumeToken();
                        }
                        break;
                    }
                    CatscriptType type;// = new CatscriptType();
                    Expression e_type = parseExpression();
                    String arg_type_name = ((IdentifierExpression)e_type).getName();
                    if (arg_type_name.equals("object")) {
                        type = CatscriptType.OBJECT;
                    } else if (arg_type_name.equals("int")) {
                        type = CatscriptType.INT;
                    } else if (arg_type_name.equals("string")) {
                        type = CatscriptType.STRING;
                    }  else /*if (arg_type_name.equals("bool"))*/ {
                        type = (CatscriptType.BOOLEAN);
                    }
                    varState.setExplicitType(type);
                } else {
                    tokens.consumeToken();
                }
            }
            tokens.consumeToken();//=
            Expression id_val = parseExpression();//value
            try {
                String name = ((IdentifierExpression) id).getName();
                varState.setVariableName(name);
                varState.setExpression(id_val);
                if (id_val instanceof ListLiteralExpression) {
                    CatscriptType elemType = ((ListLiteralExpression) id_val).getValues().get(0).getType();
                    varState.setExplicitType(CatscriptType.getListType(elemType));
                    /*if (varState.getExplicitType() == null) {
                        varState.setExplicitType(CatscriptType.getListType(elemType));
                    }*/
                } else {
                    varState.setType(id_val.getType());
                    /*if (varState.getExplicitType() == null) {
                        varState.setExplicitType(id_val.getType());
                    }*/
                }
                if (var_dict.get(name) == null) {
                    if (id_val instanceof IdentifierExpression) {
                        var_dict.put(name, var_dict.get(((IdentifierExpression) id_val).getName()));
                    } else {
                        var_dict.put(name, id_val);
                    }
                } else {
                    //var_dict.put(((IdentifierExpression) id).getName(), var_dict.get(name));
                    var_dict.remove(name);
                    if (id_val instanceof IdentifierExpression) {
                        var_dict.put(name, var_dict.get(((IdentifierExpression) id_val).getName()));
                    } else {
                        var_dict.put(name, id_val);
                    }
                }
            } catch (Exception e) {
                var_dict.put(((IdentifierExpression) id).getName(), id_val);
            }

            if (id_val instanceof IntegerLiteralExpression) {
                varState.setType(CatscriptType.INT);
            } else if (id_val instanceof BooleanLiteralExpression) {
                varState.setType(CatscriptType.BOOLEAN);
            }

            value_set = true;
            varState.setStart(tok);
            return varState;
        } else if (tokens.match(IDENTIFIER)) {
            Expression id = parseExpression();
            if (id instanceof FunctionCallExpression) {
                FunctionCallExpression func_expr = (FunctionCallExpression)id;
                FunctionCallStatement func_call_statement = new FunctionCallStatement(func_expr);
                return func_call_statement;
            } else {
                AssignmentStatement ass = new AssignmentStatement();
                ass.setVariableName(((IdentifierExpression) id).getName());
                tokens.consumeToken();
                Expression expr = parseExpression();
                ass.setExpression(expr);
                if (var_dict.get(ass.getVariableName()) == null) {
                    ass.addError(ErrorType.UNEXPECTED_TOKEN);
                } else {
                    if (var_dict.get(ass.getVariableName()).getType() == ass.getType()) {
                        var_dict.remove(ass.getVariableName());
                        var_dict.put(ass.getVariableName(), ass.getExpression());
                    } else {
                        ass.addError(ErrorType.INCOMPATIBLE_TYPES);
                    }
                }
                return ass;
            }
        } else if (tokens.match(FOR)) {
            for_programs = new CatScriptProgram();
            for_programs.setStart(tokens.getCurrentToken());
            ForStatement forStatement = new ForStatement();
            int num_statements;
            Expression lst;
            Expression id;
            do {
                num_statements = 0;
                tokens.consumeToken(); //for
                tokens.consumeToken(); //paren
                id = parseExpression();
                if (forStatement.getVariableName() == null) {
                    forStatement.setVariableName(((IdentifierExpression) id).getName());
                }
                tokens.consumeToken(); //in
                lst = parseExpression();
                if (forStatement.getExpression() == null) {
                    forStatement.setExpression(lst);
                }
                tokens.consumeToken(); //paren
                tokens.consumeToken(); //left bracket
                //var is already in dict
                if (var_dict.get(((IdentifierExpression) id).getName()) != null) {
                    var_dict.remove(((IdentifierExpression) id).getName());
                }
                //put loop var in the dict
                var_dict.put(((IdentifierExpression) id).getName(), ((ListLiteralExpression) lst).getValues().get(for_indx));
                while (true) {
                    if (tokens.match(RIGHT_BRACE)) {
                        break;
                    } else if (tokens.match(EOF)){
                        forStatement.addError(ErrorType.UNEXPECTED_TOKEN);
                        return forStatement;
                    }
                    Statement body = parseProgramStatement();
                    for_programs.addStatement(body);
                    num_statements += 1;
                    if (for_indx == 0) {
                        List<Statement> body_lst = forStatement.getBody();
                        if (body_lst == null) {
                            body_lst = new ArrayList<>();
                        }
                        body_lst.add(body);
                        forStatement.setBody(body_lst);
                    }
                }
                //reset tokens
                tokens = new CatScriptTokenizer(token_src).getTokens();
                for_indx += 1;
            } while (for_indx < ((ListLiteralExpression)lst).getValues().size()-1);
            //finished loop (except last)
            for_indx = ((ListLiteralExpression)lst).getValues().size()-1;
            tokens.consumeToken(); //for
            tokens.consumeToken(); //paren
            id = parseExpression();
            tokens.consumeToken(); //in
            lst = parseExpression();
            tokens.consumeToken(); //paren
            tokens.consumeToken(); //left bracket
            //var is already in dict
            if (var_dict.get(((IdentifierExpression) id).getName()) != null) {
                var_dict.remove(((IdentifierExpression) id).getName());
            }
            //put loop var in the dict
            var_dict.put(((IdentifierExpression) id).getName(), ((ListLiteralExpression) lst).getValues().get(for_indx));
            for (int i = 0; i<num_statements; i++) {
                for_programs.addStatement(parseProgramStatement());
            }
            for_indx = 0;
            value_set = true;
            //consum = true;
            tokens.consumeToken();
            return forStatement;
        } else if (tokens.match(RETURN)) {
            tokens.consumeToken();//return
            ReturnStatement returnStatement = new ReturnStatement();
            if (!tokens.match(RIGHT_BRACE)) {
                returnStatement.setExpression(parseExpression());
            }
            return returnStatement;
        } else {
            return null;
        }
    }

    private Statement parseFunctionDeclaration() {
         if (tokens.match(FUNCTION)) {
            FunctionDefinitionStatement func_def = new FunctionDefinitionStatement();
            Token tok = tokens.consumeToken();//function
            FunctionCallExpression func_def_expr = (FunctionCallExpression)parseExpression();
            func_def.setName(func_def_expr.getName());
            List<Expression> arg_list = func_def_expr.getArguments();
            for (int i=0;i<arg_list.size();i++) {
                TypeLiteral type = new TypeLiteral();
                String arg_name = ((IdentifierExpression)(arg_list.get(i))).getName();
                if (((i+1)<arg_list.size()) && (arg_list.get(i+1) instanceof SyntaxErrorExpression)) {
                    String arg_type_name = ((IdentifierExpression)(arg_list.get(i+2))).getName();
                    if (arg_type_name.equals("object")) {
                        type.setType(CatscriptType.OBJECT);
                    } else if (arg_type_name.equals("int")) {
                        type.setType(CatscriptType.INT);
                    } else if (arg_type_name.equals("bool")) {
                        type.setType(CatscriptType.BOOLEAN);
                    }
                    i += 2;
                } else {
                    type.setType(CatscriptType.OBJECT);
                }
                func_def.addParameter(arg_name, type);
            }
            if (tokens.match(COLON)) {
                tokens.consumeToken();//:
                TypeLiteral type = new TypeLiteral();
                Expression return_type = parseExpression();
                String arg_type_name = ((IdentifierExpression)return_type).getName();
                if (arg_type_name.equals("object")) {
                    type.setType(CatscriptType.OBJECT);
                } else if (arg_type_name.equals("int")) {
                    type.setType(CatscriptType.INT);
                } else if (arg_type_name.equals("bool")) {
                    type.setType(CatscriptType.BOOLEAN);
                }
                func_def.setType(type);
            }
            tokens.consumeToken();//{
            List<Statement> body = new ArrayList<>();
            int i = 0;
            while (i<5 && !tokens.match(RIGHT_BRACE) && !tokens.match(EOF)) {
                Statement body_statement = parseProgramStatement();
                if (body_statement instanceof ReturnStatement) {
                    ((ReturnStatement)body_statement).setFunctionDefinition(func_def);
                }
                body.add(body_statement);
                i++;
            }
            func_def.setBody(body);
            if (tokens.match(RIGHT_BRACE)) {
                tokens.consumeToken();
            } else {
                func_def.addError(ErrorType.UNEXPECTED_TOKEN);
            }
            if (func_def.getType() == null) {
                TypeLiteral type = new TypeLiteral();
                type.setType(CatscriptType.VOID);
                func_def.setType(type);
            }
            func_def.setStart(tok);
            return func_def;
        }
        return null;
    }
    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);

            Expression to_print = parseExpression();
            try {
                String name = ((IdentifierExpression) to_print).getName();
                if (var_dict.get(name) == null) {
                    printStatement.setExpression(to_print);
                } else {
                    printStatement.setExpression(var_dict.get(name));
                }
            } catch (Exception e) {
                printStatement.setExpression(to_print);
            }

            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        //return parseAdditiveExpression(); //Given
        //return parseComparisonExpression();
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression(){ //REEE
        Expression expression = parseComparisonExpression();
        while(tokens.match(BANG_EQUAL,EQUAL_EQUAL)){
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseComparisonExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rightHandSide);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(expression.getEnd());
            expression = equalityExpression;
        }
        return expression;
    }

    private Expression parseComparisonExpression(){ //RE
        Expression expression = parseAdditiveExpression();
        while(tokens.match(GREATER,GREATER_EQUAL,LESS,LESS_EQUAL)){
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseAdditiveExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rightHandSide);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(expression.getEnd());
            expression = comparisonExpression;
        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = parseFactorExpression();
            if ((expression instanceof StringLiteralExpression) && !(rightHandSide instanceof StringLiteralExpression)) {
                if (rightHandSide instanceof NullLiteralExpression) {
                    rightHandSide = new StringLiteralExpression("null");
                } else {
                    rightHandSide = new StringLiteralExpression(rightHandSide.toString());
                }
            } else if (!(expression instanceof StringLiteralExpression) && (rightHandSide instanceof StringLiteralExpression)) {
                if (expression instanceof NullLiteralExpression) {
                    expression = new StringLiteralExpression("null");
                } else {
                    expression = new StringLiteralExpression(expression.toString());
                }
            }
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression(){ //RE
        Expression expression = parseUnaryExpression();
        while(tokens.match(SLASH,STAR)){
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(expression.getEnd());
            expression = factorExpression;
        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() { //RE
        if (tokens.match(INTEGER)) {
            Token integerToken = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
            integerExpression.setToken(integerToken);
            return integerExpression;
        } else if (tokens.match(STRING)) {
            Token stringToken = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
            stringExpression.setToken(stringToken);
            return stringExpression;
        } else if (tokens.match(TRUE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression boolExpression = new BooleanLiteralExpression(true);
            boolExpression.setToken(boolToken);
            return boolExpression;
        } else if (tokens.match(FALSE)) {
            Token boolToken = tokens.consumeToken();
            BooleanLiteralExpression boolExpression = new BooleanLiteralExpression(false);
            boolExpression.setToken(boolToken);
            return boolExpression;
        } else if (tokens.match(NULL)) {
            Token nullToken = tokens.consumeToken();
            NullLiteralExpression nullExpression = new NullLiteralExpression();
            nullExpression.setToken(nullToken);
            return nullExpression;
        } else if (tokens.match(LEFT_PAREN, RIGHT_PAREN)) { //TBC
            Token parenToken = tokens.consumeToken();
            final Expression rightHandSide = parseEqualityExpression();
            ParenthesizedExpression parenExpression = new ParenthesizedExpression(rightHandSide);
            //parenExpression.setStart(expression.getStart());
            //parenExpression.setEnd(expression.getEnd());
            //expression = factorExpression;
            //ParenthesizedExpression parenExpression = new ParenthesizedExpression(parenToken.getStringValue());
            parenExpression.setToken(parenToken);
            return parenExpression;
        } else if (tokens.match(LEFT_BRACKET, LEFT_BRACE, RIGHT_BRACKET, RIGHT_BRACE)) {
            List<Expression> list_expressions = new LinkedList<>();
            tokens.consumeToken();
            boolean error = false;
            if (tokens.hasMoreTokens() && tokens.match(RIGHT_BRACKET, RIGHT_BRACE)) {
                tokens.consumeToken();
            } else {
                while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACKET, RIGHT_BRACE)) {
                    if (!tokens.match(COMMA)) {
                        list_expressions.add(parseEqualityExpression());
                    } else {
                        tokens.consumeToken();
                    }
                }
                if (tokens.hasMoreTokens()) {
                    tokens.consumeToken();
                } else {
                    error = true;
                }
            }
            ListLiteralExpression listExpression = new ListLiteralExpression(list_expressions);
            if (error) {
                listExpression.addError(ErrorType.UNTERMINATED_LIST);
            }
            return listExpression;
        } else if (tokens.match(IDENTIFIER)) {
            Token func_name = tokens.consumeToken();
            if (!tokens.hasMoreTokens() || !tokens.match(LEFT_PAREN)) {
                IdentifierExpression identifierExpression = new IdentifierExpression(func_name.getStringValue());
                identifierExpression.setStart(func_name);
                return identifierExpression;
            }
            tokens.consumeToken();
            List<Expression> arg_list = new LinkedList<>();
            boolean error = false;
            if (tokens.hasMoreTokens() && tokens.match(RIGHT_PAREN)) {
                tokens.consumeToken();
            } else {
                while (tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)) {
                    if (!tokens.match(COMMA)) {
                        if (tokens.match(COLON)) {
                            tokens.consumeToken();
                            Token tok = tokens.consumeToken();
                            if (tok.getStringValue().equals("list") && tokens.match(LESS)) {
                                tokens.consumeToken();
                                tokens.consumeToken();
                                tokens.consumeToken();
                            }
                        } else {
                            arg_list.add(parseEqualityExpression());
                        }
                    } else {
                        tokens.consumeToken();
                    }
                }
                if (tokens.hasMoreTokens()) {
                    tokens.consumeToken();
                } else {
                    error = true;
                }
            }
            if (tokens.match(COLON)) {
                tokens.consumeToken();
                Token tok = tokens.consumeToken();
                if (tok.getStringValue().equals("list") && tokens.match(LESS)) {
                    tokens.consumeToken();
                    tokens.consumeToken();
                    tokens.consumeToken();
                }
            }
            FunctionCallExpression funcExpression = new FunctionCallExpression(func_name.getStringValue(), arg_list);
            if (error) {
                funcExpression.addError(ErrorType.UNTERMINATED_ARG_LIST);
            }
            return funcExpression;
        } else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
