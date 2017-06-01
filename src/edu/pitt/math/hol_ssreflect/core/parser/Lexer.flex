package edu.pitt.math.hol_ssreflect.core.parser;

%%
%function get_token
%type Token
%eofval{
        return new Token(TokenType.EOF);
%eofval}
%eofclose

%line
%column

%class Scanner
%{
        StringBuffer string = new StringBuffer();

        private Token lastToken;

        public Token peekToken() throws java.io.IOException {
                if (lastToken != null)
                        return lastToken;
                else
                        return lastToken = get_token();
        }

        public Token nextToken() throws java.io.IOException {
                if (lastToken != null) {
                        Token tmp = lastToken;
                        lastToken = null;
                        return tmp;
                }

                return get_token();
        }
%}
%eofval{
    return new Token(TokenType.EOF);
%eofval}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]

WhiteSpace = [ \t\f]

IdentifierSymbol = [a-zA-Z]

Identifier = {IdentifierSymbol} ({IdentifierSymbol} | [_0-9])*

Integer = [-]? [1-9] [0-9]*

//StringCharacter = [^\r\n\"\\]
StringCharacter = [^\r\n\"]

%state STRING

%%

<YYINITIAL> {
          /* separators */
        "(" { return new Token(TokenType.LPAR); }
        ")" { return new Token(TokenType.RPAR); }
        "[" { return new Token(TokenType.LBRACK); }
        "]" { return new Token(TokenType.RBRACK); }
        "," { return new Token(TokenType.COMMA); }
		";" { return new Token(TokenType.SEMICOLON); }
		":" { return new Token(TokenType.COLON); }
		
		/* keywords */
		"Tyapp" { return new Token(TokenType.Tyapp); }
		"Tyvar" { return new Token(TokenType.Tyvar); }
		"Var" { return new Token(TokenType.Var); }
		"Const" { return new Token(TokenType.Const); }
		"Comb" { return new Token(TokenType.Comb); }
		"Abs" { return new Token(TokenType.Abs); }
		
		/* boolean values */
		"false" { return new Token(TokenType.False); }
		"true" { return new Token(TokenType.True); }
		
		/* types */
		"String" { return new Token(TokenType.String); }
		"Int" { return new Token(TokenType.Int); }
		"HOLType" { return new Token(TokenType.HOLType); }
		"Term" { return new Token(TokenType.Term); }
		"Theorem" { return new Token(TokenType.Theorem); }
		"List" { return new Token(TokenType.List); }
		"Pair" { return new Token(TokenType.Pair); }
		
		"Goal" { return new Token(TokenType.Goal); }
		"Goalstate" { return new Token(TokenType.Goalstate); }
		

        /* string literal */
        \"    { yybegin(STRING); string.setLength(0); }

        {WhiteSpace}        {}
        {LineTerminator}        {}

		{Integer} { return new Token(TokenType.INTEGER, yytext()); }
        {Identifier} { return new Token(TokenType.IDENTIFIER, yytext()); }
}

<STRING> {
          \"  { yybegin(YYINITIAL); return new Token(TokenType.STRING, string.toString()); }

  {StringCharacter}+             { string.append( yytext() ); }

  /* escape sequences */
/*  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); } */
}

. { System.err.println("Illegal character: "+yytext()); }
