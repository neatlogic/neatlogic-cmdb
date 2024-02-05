/*
 * Copyright(c) 2024 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Generated from /Users/chenqiwei/idea_project/codedriver/neatlogic-cmdb-base/src/main/resources/CmdbDSL.g4 by ANTLR 4.12.0

package neatlogic.module.cmdb.dsl.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class CmdbDSLParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            T__0 = 1, NUMBER_ARRAY = 2, STRING_ARRAY = 3, NUMBER = 4, BRACKET_LEFT = 5, BRACKET_RIGHT = 6,
            AND = 7, OR = 8, EQ = 9, GT = 10, LT = 11, LE = 12, GE = 13, PLUS = 14, SUBTRACT = 15, MULTIPLY = 16,
            DIVIDE = 17, NOTEQ = 18, NOTLIKE = 19, LIKE = 20, INCLUDE = 21, EXCLUDE = 22, ATTR = 23,
            STRING = 24, WHITESPACE = 25;
    public static final int
            RULE_calculateExpressions = 0, RULE_expressions = 1, RULE_attrs = 2, RULE_logicalOperator = 3,
            RULE_comparisonOperator = 4, RULE_calculateOperator = 5;

    private static String[] makeRuleNames() {
        return new String[]{
                "calculateExpressions", "expressions", "attrs", "logicalOperator", "comparisonOperator",
                "calculateOperator"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'.'", null, null, null, "'('", "')'", "'&&'", "'||'", "'=='",
                "'>'", "'<'", "'<='", "'>='", "'+'", "'-'", "'*'", "'/'", "'!='", "'not like'",
                "'like'", "'include'", "'exclude'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, null, "NUMBER_ARRAY", "STRING_ARRAY", "NUMBER", "BRACKET_LEFT",
                "BRACKET_RIGHT", "AND", "OR", "EQ", "GT", "LT", "LE", "GE", "PLUS", "SUBTRACT",
                "MULTIPLY", "DIVIDE", "NOTEQ", "NOTLIKE", "LIKE", "INCLUDE", "EXCLUDE",
                "ATTR", "STRING", "WHITESPACE"
        };
    }

    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "CmdbDSL.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public CmdbDSLParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CalculateExpressionsContext extends ParserRuleContext {
        public Token op;

        public AttrsContext attrs() {
            return getRuleContext(AttrsContext.class, 0);
        }

        public TerminalNode NUMBER() {
            return getToken(CmdbDSLParser.NUMBER, 0);
        }

        public TerminalNode BRACKET_LEFT() {
            return getToken(CmdbDSLParser.BRACKET_LEFT, 0);
        }

        public List<CalculateExpressionsContext> calculateExpressions() {
            return getRuleContexts(CalculateExpressionsContext.class);
        }

        public CalculateExpressionsContext calculateExpressions(int i) {
            return getRuleContext(CalculateExpressionsContext.class, i);
        }

        public TerminalNode BRACKET_RIGHT() {
            return getToken(CmdbDSLParser.BRACKET_RIGHT, 0);
        }

        public TerminalNode MULTIPLY() {
            return getToken(CmdbDSLParser.MULTIPLY, 0);
        }

        public TerminalNode DIVIDE() {
            return getToken(CmdbDSLParser.DIVIDE, 0);
        }

        public TerminalNode PLUS() {
            return getToken(CmdbDSLParser.PLUS, 0);
        }

        public TerminalNode SUBTRACT() {
            return getToken(CmdbDSLParser.SUBTRACT, 0);
        }

        public CalculateExpressionsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_calculateExpressions;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterCalculateExpressions(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitCalculateExpressions(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitCalculateExpressions(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CalculateExpressionsContext calculateExpressions() throws RecognitionException {
        return calculateExpressions(0);
    }

    private CalculateExpressionsContext calculateExpressions(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        CalculateExpressionsContext _localctx = new CalculateExpressionsContext(_ctx, _parentState);
        CalculateExpressionsContext _prevctx = _localctx;
        int _startState = 0;
        enterRecursionRule(_localctx, 0, RULE_calculateExpressions, _p);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(19);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case ATTR: {
                        setState(13);
                        attrs();
                    }
                    break;
                    case NUMBER: {
                        setState(14);
                        match(NUMBER);
                    }
                    break;
                    case BRACKET_LEFT: {
                        setState(15);
                        match(BRACKET_LEFT);
                        setState(16);
                        calculateExpressions(0);
                        setState(17);
                        match(BRACKET_RIGHT);
                    }
                    break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(29);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 2, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            setState(27);
                            _errHandler.sync(this);
                            switch (getInterpreter().adaptivePredict(_input, 1, _ctx)) {
                                case 1: {
                                    _localctx = new CalculateExpressionsContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_calculateExpressions);
                                    setState(21);
                                    if (!(precpred(_ctx, 5)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 5)");
                                    setState(22);
                                    ((CalculateExpressionsContext) _localctx).op = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!(_la == MULTIPLY || _la == DIVIDE)) {
                                        ((CalculateExpressionsContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(23);
                                    calculateExpressions(6);
                                }
                                break;
                                case 2: {
                                    _localctx = new CalculateExpressionsContext(_parentctx, _parentState);
                                    pushNewRecursionContext(_localctx, _startState, RULE_calculateExpressions);
                                    setState(24);
                                    if (!(precpred(_ctx, 4)))
                                        throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                                    setState(25);
                                    ((CalculateExpressionsContext) _localctx).op = _input.LT(1);
                                    _la = _input.LA(1);
                                    if (!(_la == PLUS || _la == SUBTRACT)) {
                                        ((CalculateExpressionsContext) _localctx).op = (Token) _errHandler.recoverInline(this);
                                    } else {
                                        if (_input.LA(1) == Token.EOF) matchedEOF = true;
                                        _errHandler.reportMatch(this);
                                        consume();
                                    }
                                    setState(26);
                                    calculateExpressions(5);
                                }
                                break;
                            }
                        }
                    }
                    setState(31);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 2, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionsContext extends ParserRuleContext {
        public ExpressionsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_expressions;
        }

        public ExpressionsContext() {
        }

        public void copyFrom(ExpressionsContext ctx) {
            super.copyFrom(ctx);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionJoinContext extends ExpressionsContext {
        public List<ExpressionsContext> expressions() {
            return getRuleContexts(ExpressionsContext.class);
        }

        public ExpressionsContext expressions(int i) {
            return getRuleContext(ExpressionsContext.class, i);
        }

        public LogicalOperatorContext logicalOperator() {
            return getRuleContext(LogicalOperatorContext.class, 0);
        }

        public ExpressionJoinContext(ExpressionsContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterExpressionJoin(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitExpressionJoin(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitExpressionJoin(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionContext extends ExpressionsContext {
        public AttrsContext attrs() {
            return getRuleContext(AttrsContext.class, 0);
        }

        public ComparisonOperatorContext comparisonOperator() {
            return getRuleContext(ComparisonOperatorContext.class, 0);
        }

        public TerminalNode STRING() {
            return getToken(CmdbDSLParser.STRING, 0);
        }

        public TerminalNode NUMBER() {
            return getToken(CmdbDSLParser.NUMBER, 0);
        }

        public TerminalNode NUMBER_ARRAY() {
            return getToken(CmdbDSLParser.NUMBER_ARRAY, 0);
        }

        public TerminalNode STRING_ARRAY() {
            return getToken(CmdbDSLParser.STRING_ARRAY, 0);
        }

        public CalculateExpressionsContext calculateExpressions() {
            return getRuleContext(CalculateExpressionsContext.class, 0);
        }

        public ExpressionContext(ExpressionsContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterExpression(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitExpression(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor) return ((CmdbDSLVisitor<? extends T>) visitor).visitExpression(this);
            else return visitor.visitChildren(this);
        }
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ExpressionGroupContext extends ExpressionsContext {
        public TerminalNode BRACKET_LEFT() {
            return getToken(CmdbDSLParser.BRACKET_LEFT, 0);
        }

        public ExpressionsContext expressions() {
            return getRuleContext(ExpressionsContext.class, 0);
        }

        public TerminalNode BRACKET_RIGHT() {
            return getToken(CmdbDSLParser.BRACKET_RIGHT, 0);
        }

        public ExpressionGroupContext(ExpressionsContext ctx) {
            copyFrom(ctx);
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterExpressionGroup(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitExpressionGroup(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitExpressionGroup(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ExpressionsContext expressions() throws RecognitionException {
        return expressions(0);
    }

    private ExpressionsContext expressions(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        ExpressionsContext _localctx = new ExpressionsContext(_ctx, _parentState);
        ExpressionsContext _prevctx = _localctx;
        int _startState = 2;
        enterRecursionRule(_localctx, 2, RULE_expressions, _p);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(46);
                _errHandler.sync(this);
                switch (_input.LA(1)) {
                    case ATTR: {
                        _localctx = new ExpressionContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;

                        setState(33);
                        attrs();
                        setState(34);
                        comparisonOperator();
                        setState(40);
                        _errHandler.sync(this);
                        switch (getInterpreter().adaptivePredict(_input, 3, _ctx)) {
                            case 1: {
                                setState(35);
                                match(STRING);
                            }
                            break;
                            case 2: {
                                setState(36);
                                match(NUMBER);
                            }
                            break;
                            case 3: {
                                setState(37);
                                match(NUMBER_ARRAY);
                            }
                            break;
                            case 4: {
                                setState(38);
                                match(STRING_ARRAY);
                            }
                            break;
                            case 5: {
                                setState(39);
                                calculateExpressions(0);
                            }
                            break;
                        }
                    }
                    break;
                    case BRACKET_LEFT: {
                        _localctx = new ExpressionGroupContext(_localctx);
                        _ctx = _localctx;
                        _prevctx = _localctx;
                        setState(42);
                        match(BRACKET_LEFT);
                        setState(43);
                        expressions(0);
                        setState(44);
                        match(BRACKET_RIGHT);
                    }
                    break;
                    default:
                        throw new NoViableAltException(this);
                }
                _ctx.stop = _input.LT(-1);
                setState(54);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        if (_parseListeners != null) triggerExitRuleEvent();
                        _prevctx = _localctx;
                        {
                            {
                                _localctx = new ExpressionJoinContext(new ExpressionsContext(_parentctx, _parentState));
                                pushNewRecursionContext(_localctx, _startState, RULE_expressions);
                                setState(48);
                                if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
                                setState(49);
                                logicalOperator();
                                setState(50);
                                expressions(3);
                            }
                        }
                    }
                    setState(56);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 5, _ctx);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class AttrsContext extends ParserRuleContext {
        public List<TerminalNode> ATTR() {
            return getTokens(CmdbDSLParser.ATTR);
        }

        public TerminalNode ATTR(int i) {
            return getToken(CmdbDSLParser.ATTR, i);
        }

        public AttrsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_attrs;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterAttrs(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitAttrs(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor) return ((CmdbDSLVisitor<? extends T>) visitor).visitAttrs(this);
            else return visitor.visitChildren(this);
        }
    }

    public final AttrsContext attrs() throws RecognitionException {
        AttrsContext _localctx = new AttrsContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_attrs);
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
                setState(61);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
                while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
                    if (_alt == 1) {
                        {
                            {
                                setState(57);
                                match(ATTR);
                                setState(58);
                                match(T__0);
                            }
                        }
                    }
                    setState(63);
                    _errHandler.sync(this);
                    _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
                }
                setState(64);
                match(ATTR);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class LogicalOperatorContext extends ParserRuleContext {
        public TerminalNode AND() {
            return getToken(CmdbDSLParser.AND, 0);
        }

        public TerminalNode OR() {
            return getToken(CmdbDSLParser.OR, 0);
        }

        public LogicalOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_logicalOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterLogicalOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitLogicalOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitLogicalOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final LogicalOperatorContext logicalOperator() throws RecognitionException {
        LogicalOperatorContext _localctx = new LogicalOperatorContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_logicalOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(66);
                _la = _input.LA(1);
                if (!(_la == AND || _la == OR)) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class ComparisonOperatorContext extends ParserRuleContext {
        public TerminalNode EQ() {
            return getToken(CmdbDSLParser.EQ, 0);
        }

        public TerminalNode GT() {
            return getToken(CmdbDSLParser.GT, 0);
        }

        public TerminalNode LT() {
            return getToken(CmdbDSLParser.LT, 0);
        }

        public TerminalNode LE() {
            return getToken(CmdbDSLParser.LE, 0);
        }

        public TerminalNode GE() {
            return getToken(CmdbDSLParser.GE, 0);
        }

        public TerminalNode NOTEQ() {
            return getToken(CmdbDSLParser.NOTEQ, 0);
        }

        public TerminalNode INCLUDE() {
            return getToken(CmdbDSLParser.INCLUDE, 0);
        }

        public TerminalNode EXCLUDE() {
            return getToken(CmdbDSLParser.EXCLUDE, 0);
        }

        public TerminalNode LIKE() {
            return getToken(CmdbDSLParser.LIKE, 0);
        }

        public TerminalNode NOTLIKE() {
            return getToken(CmdbDSLParser.NOTLIKE, 0);
        }

        public ComparisonOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_comparisonOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterComparisonOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitComparisonOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitComparisonOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final ComparisonOperatorContext comparisonOperator() throws RecognitionException {
        ComparisonOperatorContext _localctx = new ComparisonOperatorContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_comparisonOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(68);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 8142336L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    @SuppressWarnings("CheckReturnValue")
    public static class CalculateOperatorContext extends ParserRuleContext {
        public TerminalNode PLUS() {
            return getToken(CmdbDSLParser.PLUS, 0);
        }

        public TerminalNode SUBTRACT() {
            return getToken(CmdbDSLParser.SUBTRACT, 0);
        }

        public TerminalNode MULTIPLY() {
            return getToken(CmdbDSLParser.MULTIPLY, 0);
        }

        public TerminalNode DIVIDE() {
            return getToken(CmdbDSLParser.DIVIDE, 0);
        }

        public CalculateOperatorContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_calculateOperator;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).enterCalculateOperator(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof CmdbDSLListener) ((CmdbDSLListener) listener).exitCalculateOperator(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof CmdbDSLVisitor)
                return ((CmdbDSLVisitor<? extends T>) visitor).visitCalculateOperator(this);
            else return visitor.visitChildren(this);
        }
    }

    public final CalculateOperatorContext calculateOperator() throws RecognitionException {
        CalculateOperatorContext _localctx = new CalculateOperatorContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_calculateOperator);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(70);
                _la = _input.LA(1);
                if (!((((_la) & ~0x3f) == 0 && ((1L << _la) & 245760L) != 0))) {
                    _errHandler.recoverInline(this);
                } else {
                    if (_input.LA(1) == Token.EOF) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
            case 0:
                return calculateExpressions_sempred((CalculateExpressionsContext) _localctx, predIndex);
            case 1:
                return expressions_sempred((ExpressionsContext) _localctx, predIndex);
        }
        return true;
    }

    private boolean calculateExpressions_sempred(CalculateExpressionsContext _localctx, int predIndex) {
        switch (predIndex) {
            case 0:
                return precpred(_ctx, 5);
            case 1:
                return precpred(_ctx, 4);
        }
        return true;
    }

    private boolean expressions_sempred(ExpressionsContext _localctx, int predIndex) {
        switch (predIndex) {
            case 2:
                return precpred(_ctx, 2);
        }
        return true;
    }

    public static final String _serializedATN =
            "\u0004\u0001\u0019I\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002" +
                    "\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002" +
                    "\u0005\u0007\u0005\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001" +
                    "\u0000\u0001\u0000\u0001\u0000\u0003\u0000\u0014\b\u0000\u0001\u0000\u0001" +
                    "\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u001c" +
                    "\b\u0000\n\u0000\f\u0000\u001f\t\u0000\u0001\u0001\u0001\u0001\u0001\u0001" +
                    "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001" +
                    ")\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001" +
                    "/\b\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001" +
                    "5\b\u0001\n\u0001\f\u00018\t\u0001\u0001\u0002\u0001\u0002\u0005\u0002" +
                    "<\b\u0002\n\u0002\f\u0002?\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003" +
                    "\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005" +
                    "\u0000\u0002\u0000\u0002\u0006\u0000\u0002\u0004\u0006\b\n\u0000\u0005" +
                    "\u0001\u0000\u0010\u0011\u0001\u0000\u000e\u000f\u0001\u0000\u0007\b\u0002" +
                    "\u0000\t\r\u0012\u0016\u0001\u0000\u000e\u0011M\u0000\u0013\u0001\u0000" +
                    "\u0000\u0000\u0002.\u0001\u0000\u0000\u0000\u0004=\u0001\u0000\u0000\u0000" +
                    "\u0006B\u0001\u0000\u0000\u0000\bD\u0001\u0000\u0000\u0000\nF\u0001\u0000" +
                    "\u0000\u0000\f\r\u0006\u0000\uffff\uffff\u0000\r\u0014\u0003\u0004\u0002" +
                    "\u0000\u000e\u0014\u0005\u0004\u0000\u0000\u000f\u0010\u0005\u0005\u0000" +
                    "\u0000\u0010\u0011\u0003\u0000\u0000\u0000\u0011\u0012\u0005\u0006\u0000" +
                    "\u0000\u0012\u0014\u0001\u0000\u0000\u0000\u0013\f\u0001\u0000\u0000\u0000" +
                    "\u0013\u000e\u0001\u0000\u0000\u0000\u0013\u000f\u0001\u0000\u0000\u0000" +
                    "\u0014\u001d\u0001\u0000\u0000\u0000\u0015\u0016\n\u0005\u0000\u0000\u0016" +
                    "\u0017\u0007\u0000\u0000\u0000\u0017\u001c\u0003\u0000\u0000\u0006\u0018" +
                    "\u0019\n\u0004\u0000\u0000\u0019\u001a\u0007\u0001\u0000\u0000\u001a\u001c" +
                    "\u0003\u0000\u0000\u0005\u001b\u0015\u0001\u0000\u0000\u0000\u001b\u0018" +
                    "\u0001\u0000\u0000\u0000\u001c\u001f\u0001\u0000\u0000\u0000\u001d\u001b" +
                    "\u0001\u0000\u0000\u0000\u001d\u001e\u0001\u0000\u0000\u0000\u001e\u0001" +
                    "\u0001\u0000\u0000\u0000\u001f\u001d\u0001\u0000\u0000\u0000 !\u0006\u0001" +
                    "\uffff\uffff\u0000!\"\u0003\u0004\u0002\u0000\"(\u0003\b\u0004\u0000#" +
                    ")\u0005\u0018\u0000\u0000$)\u0005\u0004\u0000\u0000%)\u0005\u0002\u0000" +
                    "\u0000&)\u0005\u0003\u0000\u0000\')\u0003\u0000\u0000\u0000(#\u0001\u0000" +
                    "\u0000\u0000($\u0001\u0000\u0000\u0000(%\u0001\u0000\u0000\u0000(&\u0001" +
                    "\u0000\u0000\u0000(\'\u0001\u0000\u0000\u0000)/\u0001\u0000\u0000\u0000" +
                    "*+\u0005\u0005\u0000\u0000+,\u0003\u0002\u0001\u0000,-\u0005\u0006\u0000" +
                    "\u0000-/\u0001\u0000\u0000\u0000. \u0001\u0000\u0000\u0000.*\u0001\u0000" +
                    "\u0000\u0000/6\u0001\u0000\u0000\u000001\n\u0002\u0000\u000012\u0003\u0006" +
                    "\u0003\u000023\u0003\u0002\u0001\u000335\u0001\u0000\u0000\u000040\u0001" +
                    "\u0000\u0000\u000058\u0001\u0000\u0000\u000064\u0001\u0000\u0000\u0000" +
                    "67\u0001\u0000\u0000\u00007\u0003\u0001\u0000\u0000\u000086\u0001\u0000" +
                    "\u0000\u00009:\u0005\u0017\u0000\u0000:<\u0005\u0001\u0000\u0000;9\u0001" +
                    "\u0000\u0000\u0000<?\u0001\u0000\u0000\u0000=;\u0001\u0000\u0000\u0000" +
                    "=>\u0001\u0000\u0000\u0000>@\u0001\u0000\u0000\u0000?=\u0001\u0000\u0000" +
                    "\u0000@A\u0005\u0017\u0000\u0000A\u0005\u0001\u0000\u0000\u0000BC\u0007" +
                    "\u0002\u0000\u0000C\u0007\u0001\u0000\u0000\u0000DE\u0007\u0003\u0000" +
                    "\u0000E\t\u0001\u0000\u0000\u0000FG\u0007\u0004\u0000\u0000G\u000b\u0001" +
                    "\u0000\u0000\u0000\u0007\u0013\u001b\u001d(.6=";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}