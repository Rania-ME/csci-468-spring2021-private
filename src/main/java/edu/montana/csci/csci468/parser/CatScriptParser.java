package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
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
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
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
            final Expression rightHandSide = parseFactorExpression();
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
        } else if (tokens.match(LEFT_PAREN, RIGHT_PAREN)) {
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
                        arg_list.add(parseEqualityExpression());
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
