/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

// Generated from /Users/chenqiwei/idea_project/codedriver/neatlogic-cmdb-base/src/main/resources/CmdbDSL.g4 by ANTLR 4.12.0

package neatlogic.module.cmdb.dsl.parser;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class CmdbDSLLexer extends Lexer {
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
    public static String[] channelNames = {
            "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
            "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[]{
                "T__0", "NUMBER_ARRAY", "STRING_ARRAY", "NUMBER", "BRACKET_LEFT", "BRACKET_RIGHT",
                "AND", "OR", "EQ", "GT", "LT", "LE", "GE", "PLUS", "SUBTRACT", "MULTIPLY",
                "DIVIDE", "NOTEQ", "NOTLIKE", "LIKE", "INCLUDE", "EXCLUDE", "ATTR", "STRING",
                "WHITESPACE"
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


    public CmdbDSLLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
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
    public String[] getChannelNames() {
        return channelNames;
    }

    @Override
    public String[] getModeNames() {
        return modeNames;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public static final String _serializedATN =
            "\u0004\u0000\u0019\u00c6\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002" +
                    "\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002" +
                    "\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002" +
                    "\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002" +
                    "\u000b\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e" +
                    "\u0002\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011" +
                    "\u0002\u0012\u0007\u0012\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014" +
                    "\u0002\u0015\u0007\u0015\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017" +
                    "\u0002\u0018\u0007\u0018\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001" +
                    "\u0001\u0001\u0001\u0001\u0005\u0001:\b\u0001\n\u0001\f\u0001=\t\u0001" +
                    "\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002" +
                    "\u0001\u0002\u0005\u0002F\b\u0002\n\u0002\f\u0002I\t\u0002\u0001\u0002" +
                    "\u0001\u0002\u0001\u0002\u0001\u0003\u0003\u0003O\b\u0003\u0001\u0003" +
                    "\u0001\u0003\u0001\u0003\u0005\u0003T\b\u0003\n\u0003\f\u0003W\t\u0003" +
                    "\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0004\u0003]\b\u0003" +
                    "\u000b\u0003\f\u0003^\u0001\u0003\u0001\u0003\u0005\u0003c\b\u0003\n\u0003" +
                    "\f\u0003f\t\u0003\u0001\u0003\u0001\u0003\u0004\u0003j\b\u0003\u000b\u0003" +
                    "\f\u0003k\u0003\u0003n\b\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001" +
                    "\u0005\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001" +
                    "\u0007\u0001\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\n\u0001\n\u0001\u000b" +
                    "\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\r\u0001\r\u0001" +
                    "\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001" +
                    "\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0001" +
                    "\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001\u0012\u0001" +
                    "\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014\u0001" +
                    "\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001" +
                    "\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001" +
                    "\u0015\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0005\u0016\u00b2" +
                    "\b\u0016\n\u0016\f\u0016\u00b5\t\u0016\u0001\u0017\u0001\u0017\u0005\u0017" +
                    "\u00b9\b\u0017\n\u0017\f\u0017\u00bc\t\u0017\u0001\u0017\u0001\u0017\u0001" +
                    "\u0018\u0004\u0018\u00c1\b\u0018\u000b\u0018\f\u0018\u00c2\u0001\u0018" +
                    "\u0001\u0018\u0001\u00ba\u0000\u0019\u0001\u0001\u0003\u0002\u0005\u0003" +
                    "\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f\b\u0011\t\u0013\n\u0015" +
                    "\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f\u001f\u0010!\u0011#\u0012" +
                    "%\u0013\'\u0014)\u0015+\u0016-\u0017/\u00181\u0019\u0001\u0000\u0007\u0001" +
                    "\u000000\u0001\u000019\u0001\u000009\u0003\u0000AZ__az\u0004\u000009A" +
                    "Z__az\u0002\u0000\"\"\\\\\u0003\u0000\t\n\r\r  \u00d2\u0000\u0001\u0001" +
                    "\u0000\u0000\u0000\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001" +
                    "\u0000\u0000\u0000\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000" +
                    "\u0000\u0000\u0000\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000" +
                    "\u0000\u0000\u000f\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000" +
                    "\u0000\u0000\u0013\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000" +
                    "\u0000\u0000\u0017\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000" +
                    "\u0000\u0000\u001b\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000" +
                    "\u0000\u0000\u001f\u0001\u0000\u0000\u0000\u0000!\u0001\u0000\u0000\u0000" +
                    "\u0000#\u0001\u0000\u0000\u0000\u0000%\u0001\u0000\u0000\u0000\u0000\'" +
                    "\u0001\u0000\u0000\u0000\u0000)\u0001\u0000\u0000\u0000\u0000+\u0001\u0000" +
                    "\u0000\u0000\u0000-\u0001\u0000\u0000\u0000\u0000/\u0001\u0000\u0000\u0000" +
                    "\u00001\u0001\u0000\u0000\u0000\u00013\u0001\u0000\u0000\u0000\u00035" +
                    "\u0001\u0000\u0000\u0000\u0005A\u0001\u0000\u0000\u0000\u0007N\u0001\u0000" +
                    "\u0000\u0000\to\u0001\u0000\u0000\u0000\u000bq\u0001\u0000\u0000\u0000" +
                    "\rs\u0001\u0000\u0000\u0000\u000fv\u0001\u0000\u0000\u0000\u0011y\u0001" +
                    "\u0000\u0000\u0000\u0013|\u0001\u0000\u0000\u0000\u0015~\u0001\u0000\u0000" +
                    "\u0000\u0017\u0080\u0001\u0000\u0000\u0000\u0019\u0083\u0001\u0000\u0000" +
                    "\u0000\u001b\u0086\u0001\u0000\u0000\u0000\u001d\u0088\u0001\u0000\u0000" +
                    "\u0000\u001f\u008a\u0001\u0000\u0000\u0000!\u008c\u0001\u0000\u0000\u0000" +
                    "#\u008e\u0001\u0000\u0000\u0000%\u0091\u0001\u0000\u0000\u0000\'\u009a" +
                    "\u0001\u0000\u0000\u0000)\u009f\u0001\u0000\u0000\u0000+\u00a7\u0001\u0000" +
                    "\u0000\u0000-\u00af\u0001\u0000\u0000\u0000/\u00b6\u0001\u0000\u0000\u0000" +
                    "1\u00c0\u0001\u0000\u0000\u000034\u0005.\u0000\u00004\u0002\u0001\u0000" +
                    "\u0000\u00005;\u0005[\u0000\u000067\u0003\u0007\u0003\u000078\u0005,\u0000" +
                    "\u00008:\u0001\u0000\u0000\u000096\u0001\u0000\u0000\u0000:=\u0001\u0000" +
                    "\u0000\u0000;9\u0001\u0000\u0000\u0000;<\u0001\u0000\u0000\u0000<>\u0001" +
                    "\u0000\u0000\u0000=;\u0001\u0000\u0000\u0000>?\u0003\u0007\u0003\u0000" +
                    "?@\u0005]\u0000\u0000@\u0004\u0001\u0000\u0000\u0000AG\u0005[\u0000\u0000" +
                    "BC\u0003/\u0017\u0000CD\u0005,\u0000\u0000DF\u0001\u0000\u0000\u0000E" +
                    "B\u0001\u0000\u0000\u0000FI\u0001\u0000\u0000\u0000GE\u0001\u0000\u0000" +
                    "\u0000GH\u0001\u0000\u0000\u0000HJ\u0001\u0000\u0000\u0000IG\u0001\u0000" +
                    "\u0000\u0000JK\u0003/\u0017\u0000KL\u0005]\u0000\u0000L\u0006\u0001\u0000" +
                    "\u0000\u0000MO\u0005-\u0000\u0000NM\u0001\u0000\u0000\u0000NO\u0001\u0000" +
                    "\u0000\u0000Om\u0001\u0000\u0000\u0000Pn\u0007\u0000\u0000\u0000QU\u0007" +
                    "\u0001\u0000\u0000RT\u0007\u0002\u0000\u0000SR\u0001\u0000\u0000\u0000" +
                    "TW\u0001\u0000\u0000\u0000US\u0001\u0000\u0000\u0000UV\u0001\u0000\u0000" +
                    "\u0000Vn\u0001\u0000\u0000\u0000WU\u0001\u0000\u0000\u0000XY\u00050\u0000" +
                    "\u0000YZ\u0005.\u0000\u0000Z\\\u0001\u0000\u0000\u0000[]\u0007\u0002\u0000" +
                    "\u0000\\[\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^\\\u0001\u0000" +
                    "\u0000\u0000^_\u0001\u0000\u0000\u0000_n\u0001\u0000\u0000\u0000`d\u0007" +
                    "\u0001\u0000\u0000ac\u0007\u0002\u0000\u0000ba\u0001\u0000\u0000\u0000" +
                    "cf\u0001\u0000\u0000\u0000db\u0001\u0000\u0000\u0000de\u0001\u0000\u0000" +
                    "\u0000eg\u0001\u0000\u0000\u0000fd\u0001\u0000\u0000\u0000gi\u0005.\u0000" +
                    "\u0000hj\u0007\u0002\u0000\u0000ih\u0001\u0000\u0000\u0000jk\u0001\u0000" +
                    "\u0000\u0000ki\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000ln\u0001" +
                    "\u0000\u0000\u0000mP\u0001\u0000\u0000\u0000mQ\u0001\u0000\u0000\u0000" +
                    "mX\u0001\u0000\u0000\u0000m`\u0001\u0000\u0000\u0000n\b\u0001\u0000\u0000" +
                    "\u0000op\u0005(\u0000\u0000p\n\u0001\u0000\u0000\u0000qr\u0005)\u0000" +
                    "\u0000r\f\u0001\u0000\u0000\u0000st\u0005&\u0000\u0000tu\u0005&\u0000" +
                    "\u0000u\u000e\u0001\u0000\u0000\u0000vw\u0005|\u0000\u0000wx\u0005|\u0000" +
                    "\u0000x\u0010\u0001\u0000\u0000\u0000yz\u0005=\u0000\u0000z{\u0005=\u0000" +
                    "\u0000{\u0012\u0001\u0000\u0000\u0000|}\u0005>\u0000\u0000}\u0014\u0001" +
                    "\u0000\u0000\u0000~\u007f\u0005<\u0000\u0000\u007f\u0016\u0001\u0000\u0000" +
                    "\u0000\u0080\u0081\u0005<\u0000\u0000\u0081\u0082\u0005=\u0000\u0000\u0082" +
                    "\u0018\u0001\u0000\u0000\u0000\u0083\u0084\u0005>\u0000\u0000\u0084\u0085" +
                    "\u0005=\u0000\u0000\u0085\u001a\u0001\u0000\u0000\u0000\u0086\u0087\u0005" +
                    "+\u0000\u0000\u0087\u001c\u0001\u0000\u0000\u0000\u0088\u0089\u0005-\u0000" +
                    "\u0000\u0089\u001e\u0001\u0000\u0000\u0000\u008a\u008b\u0005*\u0000\u0000" +
                    "\u008b \u0001\u0000\u0000\u0000\u008c\u008d\u0005/\u0000\u0000\u008d\"" +
                    "\u0001\u0000\u0000\u0000\u008e\u008f\u0005!\u0000\u0000\u008f\u0090\u0005" +
                    "=\u0000\u0000\u0090$\u0001\u0000\u0000\u0000\u0091\u0092\u0005n\u0000" +
                    "\u0000\u0092\u0093\u0005o\u0000\u0000\u0093\u0094\u0005t\u0000\u0000\u0094" +
                    "\u0095\u0005 \u0000\u0000\u0095\u0096\u0005l\u0000\u0000\u0096\u0097\u0005" +
                    "i\u0000\u0000\u0097\u0098\u0005k\u0000\u0000\u0098\u0099\u0005e\u0000" +
                    "\u0000\u0099&\u0001\u0000\u0000\u0000\u009a\u009b\u0005l\u0000\u0000\u009b" +
                    "\u009c\u0005i\u0000\u0000\u009c\u009d\u0005k\u0000\u0000\u009d\u009e\u0005" +
                    "e\u0000\u0000\u009e(\u0001\u0000\u0000\u0000\u009f\u00a0\u0005i\u0000" +
                    "\u0000\u00a0\u00a1\u0005n\u0000\u0000\u00a1\u00a2\u0005c\u0000\u0000\u00a2" +
                    "\u00a3\u0005l\u0000\u0000\u00a3\u00a4\u0005u\u0000\u0000\u00a4\u00a5\u0005" +
                    "d\u0000\u0000\u00a5\u00a6\u0005e\u0000\u0000\u00a6*\u0001\u0000\u0000" +
                    "\u0000\u00a7\u00a8\u0005e\u0000\u0000\u00a8\u00a9\u0005x\u0000\u0000\u00a9" +
                    "\u00aa\u0005c\u0000\u0000\u00aa\u00ab\u0005l\u0000\u0000\u00ab\u00ac\u0005" +
                    "u\u0000\u0000\u00ac\u00ad\u0005d\u0000\u0000\u00ad\u00ae\u0005e\u0000" +
                    "\u0000\u00ae,\u0001\u0000\u0000\u0000\u00af\u00b3\u0007\u0003\u0000\u0000" +
                    "\u00b0\u00b2\u0007\u0004\u0000\u0000\u00b1\u00b0\u0001\u0000\u0000\u0000" +
                    "\u00b2\u00b5\u0001\u0000\u0000\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000" +
                    "\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4.\u0001\u0000\u0000\u0000\u00b5" +
                    "\u00b3\u0001\u0000\u0000\u0000\u00b6\u00ba\u0005\"\u0000\u0000\u00b7\u00b9" +
                    "\b\u0005\u0000\u0000\u00b8\u00b7\u0001\u0000\u0000\u0000\u00b9\u00bc\u0001" +
                    "\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00ba\u00b8\u0001" +
                    "\u0000\u0000\u0000\u00bb\u00bd\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001" +
                    "\u0000\u0000\u0000\u00bd\u00be\u0005\"\u0000\u0000\u00be0\u0001\u0000" +
                    "\u0000\u0000\u00bf\u00c1\u0007\u0006\u0000\u0000\u00c0\u00bf\u0001\u0000" +
                    "\u0000\u0000\u00c1\u00c2\u0001\u0000\u0000\u0000\u00c2\u00c0\u0001\u0000" +
                    "\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000\u0000\u00c3\u00c4\u0001\u0000" +
                    "\u0000\u0000\u00c4\u00c5\u0006\u0018\u0000\u0000\u00c52\u0001\u0000\u0000" +
                    "\u0000\f\u0000;GNU^dkm\u00b3\u00ba\u00c2\u0001\u0000\u0001\u0000";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}