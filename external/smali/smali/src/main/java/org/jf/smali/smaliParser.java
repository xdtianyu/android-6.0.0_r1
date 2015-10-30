// $ANTLR 3.5 /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g 2015-02-19 13:37:38

package org.jf.smali;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

import org.antlr.runtime.tree.*;


@SuppressWarnings("all")
public class smaliParser extends Parser {
	public static final String[] tokenNames = new String[] {
		"<invalid>", "<EOR>", "<DOWN>", "<UP>", "ACCESS_SPEC", "ANNOTATION_DIRECTIVE",
		"ANNOTATION_VISIBILITY", "ARRAY_DATA_DIRECTIVE", "ARRAY_DESCRIPTOR", "ARROW",
		"BOOL_LITERAL", "BYTE_LITERAL", "CATCHALL_DIRECTIVE", "CATCH_DIRECTIVE",
		"CHAR_LITERAL", "CLASS_DESCRIPTOR", "CLASS_DIRECTIVE", "CLOSE_BRACE",
		"CLOSE_PAREN", "COLON", "COMMA", "DOTDOT", "DOUBLE_LITERAL", "DOUBLE_LITERAL_OR_ID",
		"END_ANNOTATION_DIRECTIVE", "END_ARRAY_DATA_DIRECTIVE", "END_FIELD_DIRECTIVE",
		"END_LOCAL_DIRECTIVE", "END_METHOD_DIRECTIVE", "END_PACKED_SWITCH_DIRECTIVE",
		"END_PARAMETER_DIRECTIVE", "END_SPARSE_SWITCH_DIRECTIVE", "END_SUBANNOTATION_DIRECTIVE",
		"ENUM_DIRECTIVE", "EPILOGUE_DIRECTIVE", "EQUAL", "FIELD_DIRECTIVE", "FIELD_OFFSET",
		"FLOAT_LITERAL", "FLOAT_LITERAL_OR_ID", "IMPLEMENTS_DIRECTIVE", "INLINE_INDEX",
		"INSTRUCTION_FORMAT10t", "INSTRUCTION_FORMAT10x", "INSTRUCTION_FORMAT10x_ODEX",
		"INSTRUCTION_FORMAT11n", "INSTRUCTION_FORMAT11x", "INSTRUCTION_FORMAT12x",
		"INSTRUCTION_FORMAT12x_OR_ID", "INSTRUCTION_FORMAT20bc", "INSTRUCTION_FORMAT20t",
		"INSTRUCTION_FORMAT21c_FIELD", "INSTRUCTION_FORMAT21c_FIELD_ODEX", "INSTRUCTION_FORMAT21c_STRING",
		"INSTRUCTION_FORMAT21c_TYPE", "INSTRUCTION_FORMAT21ih", "INSTRUCTION_FORMAT21lh",
		"INSTRUCTION_FORMAT21s", "INSTRUCTION_FORMAT21t", "INSTRUCTION_FORMAT22b",
		"INSTRUCTION_FORMAT22c_FIELD", "INSTRUCTION_FORMAT22c_FIELD_ODEX", "INSTRUCTION_FORMAT22c_TYPE",
		"INSTRUCTION_FORMAT22cs_FIELD", "INSTRUCTION_FORMAT22s", "INSTRUCTION_FORMAT22s_OR_ID",
		"INSTRUCTION_FORMAT22t", "INSTRUCTION_FORMAT22x", "INSTRUCTION_FORMAT23x",
		"INSTRUCTION_FORMAT30t", "INSTRUCTION_FORMAT31c", "INSTRUCTION_FORMAT31i",
		"INSTRUCTION_FORMAT31i_OR_ID", "INSTRUCTION_FORMAT31t", "INSTRUCTION_FORMAT32x",
		"INSTRUCTION_FORMAT35c_METHOD", "INSTRUCTION_FORMAT35c_METHOD_ODEX", "INSTRUCTION_FORMAT35c_TYPE",
		"INSTRUCTION_FORMAT35mi_METHOD", "INSTRUCTION_FORMAT35ms_METHOD", "INSTRUCTION_FORMAT3rc_METHOD",
		"INSTRUCTION_FORMAT3rc_METHOD_ODEX", "INSTRUCTION_FORMAT3rc_TYPE", "INSTRUCTION_FORMAT3rmi_METHOD",
		"INSTRUCTION_FORMAT3rms_METHOD", "INSTRUCTION_FORMAT51l", "INTEGER_LITERAL",
		"INVALID_TOKEN", "I_ACCESS_LIST", "I_ANNOTATION", "I_ANNOTATIONS", "I_ANNOTATION_ELEMENT",
		"I_ARRAY_ELEMENTS", "I_ARRAY_ELEMENT_SIZE", "I_CATCH", "I_CATCHALL", "I_CATCHES",
		"I_CLASS_DEF", "I_ENCODED_ARRAY", "I_ENCODED_ENUM", "I_ENCODED_FIELD",
		"I_ENCODED_METHOD", "I_END_LOCAL", "I_EPILOGUE", "I_FIELD", "I_FIELDS",
		"I_FIELD_INITIAL_VALUE", "I_FIELD_TYPE", "I_IMPLEMENTS", "I_LABEL", "I_LINE",
		"I_LOCAL", "I_LOCALS", "I_METHOD", "I_METHODS", "I_METHOD_PROTOTYPE",
		"I_METHOD_RETURN_TYPE", "I_ORDERED_METHOD_ITEMS", "I_PACKED_SWITCH_ELEMENTS",
		"I_PACKED_SWITCH_START_KEY", "I_PARAMETER", "I_PARAMETERS", "I_PARAMETER_NOT_SPECIFIED",
		"I_PROLOGUE", "I_REGISTERS", "I_REGISTER_LIST", "I_REGISTER_RANGE", "I_RESTART_LOCAL",
		"I_SOURCE", "I_SPARSE_SWITCH_ELEMENTS", "I_STATEMENT_ARRAY_DATA", "I_STATEMENT_FORMAT10t",
		"I_STATEMENT_FORMAT10x", "I_STATEMENT_FORMAT11n", "I_STATEMENT_FORMAT11x",
		"I_STATEMENT_FORMAT12x", "I_STATEMENT_FORMAT20bc", "I_STATEMENT_FORMAT20t",
		"I_STATEMENT_FORMAT21c_FIELD", "I_STATEMENT_FORMAT21c_STRING", "I_STATEMENT_FORMAT21c_TYPE",
		"I_STATEMENT_FORMAT21ih", "I_STATEMENT_FORMAT21lh", "I_STATEMENT_FORMAT21s",
		"I_STATEMENT_FORMAT21t", "I_STATEMENT_FORMAT22b", "I_STATEMENT_FORMAT22c_FIELD",
		"I_STATEMENT_FORMAT22c_TYPE", "I_STATEMENT_FORMAT22s", "I_STATEMENT_FORMAT22t",
		"I_STATEMENT_FORMAT22x", "I_STATEMENT_FORMAT23x", "I_STATEMENT_FORMAT30t",
		"I_STATEMENT_FORMAT31c", "I_STATEMENT_FORMAT31i", "I_STATEMENT_FORMAT31t",
		"I_STATEMENT_FORMAT32x", "I_STATEMENT_FORMAT35c_METHOD", "I_STATEMENT_FORMAT35c_TYPE",
		"I_STATEMENT_FORMAT3rc_METHOD", "I_STATEMENT_FORMAT3rc_TYPE", "I_STATEMENT_FORMAT51l",
		"I_STATEMENT_PACKED_SWITCH", "I_STATEMENT_SPARSE_SWITCH", "I_SUBANNOTATION",
		"I_SUPER", "LINE_COMMENT", "LINE_DIRECTIVE", "LOCALS_DIRECTIVE", "LOCAL_DIRECTIVE",
		"LONG_LITERAL", "MEMBER_NAME", "METHOD_DIRECTIVE", "NEGATIVE_INTEGER_LITERAL",
		"NULL_LITERAL", "OPEN_BRACE", "OPEN_PAREN", "PACKED_SWITCH_DIRECTIVE",
		"PARAMETER_DIRECTIVE", "PARAM_LIST_END", "PARAM_LIST_OR_ID_END", "PARAM_LIST_OR_ID_START",
		"PARAM_LIST_START", "POSITIVE_INTEGER_LITERAL", "PRIMITIVE_TYPE", "PROLOGUE_DIRECTIVE",
		"REGISTER", "REGISTERS_DIRECTIVE", "RESTART_LOCAL_DIRECTIVE", "SHORT_LITERAL",
		"SIMPLE_NAME", "SOURCE_DIRECTIVE", "SPARSE_SWITCH_DIRECTIVE", "STRING_LITERAL",
		"SUBANNOTATION_DIRECTIVE", "SUPER_DIRECTIVE", "VERIFICATION_ERROR_TYPE",
		"VOID_TYPE", "VTABLE_INDEX", "WHITE_SPACE"
	};
	public static final int EOF=-1;
	public static final int ACCESS_SPEC=4;
	public static final int ANNOTATION_DIRECTIVE=5;
	public static final int ANNOTATION_VISIBILITY=6;
	public static final int ARRAY_DATA_DIRECTIVE=7;
	public static final int ARRAY_DESCRIPTOR=8;
	public static final int ARROW=9;
	public static final int BOOL_LITERAL=10;
	public static final int BYTE_LITERAL=11;
	public static final int CATCHALL_DIRECTIVE=12;
	public static final int CATCH_DIRECTIVE=13;
	public static final int CHAR_LITERAL=14;
	public static final int CLASS_DESCRIPTOR=15;
	public static final int CLASS_DIRECTIVE=16;
	public static final int CLOSE_BRACE=17;
	public static final int CLOSE_PAREN=18;
	public static final int COLON=19;
	public static final int COMMA=20;
	public static final int DOTDOT=21;
	public static final int DOUBLE_LITERAL=22;
	public static final int DOUBLE_LITERAL_OR_ID=23;
	public static final int END_ANNOTATION_DIRECTIVE=24;
	public static final int END_ARRAY_DATA_DIRECTIVE=25;
	public static final int END_FIELD_DIRECTIVE=26;
	public static final int END_LOCAL_DIRECTIVE=27;
	public static final int END_METHOD_DIRECTIVE=28;
	public static final int END_PACKED_SWITCH_DIRECTIVE=29;
	public static final int END_PARAMETER_DIRECTIVE=30;
	public static final int END_SPARSE_SWITCH_DIRECTIVE=31;
	public static final int END_SUBANNOTATION_DIRECTIVE=32;
	public static final int ENUM_DIRECTIVE=33;
	public static final int EPILOGUE_DIRECTIVE=34;
	public static final int EQUAL=35;
	public static final int FIELD_DIRECTIVE=36;
	public static final int FIELD_OFFSET=37;
	public static final int FLOAT_LITERAL=38;
	public static final int FLOAT_LITERAL_OR_ID=39;
	public static final int IMPLEMENTS_DIRECTIVE=40;
	public static final int INLINE_INDEX=41;
	public static final int INSTRUCTION_FORMAT10t=42;
	public static final int INSTRUCTION_FORMAT10x=43;
	public static final int INSTRUCTION_FORMAT10x_ODEX=44;
	public static final int INSTRUCTION_FORMAT11n=45;
	public static final int INSTRUCTION_FORMAT11x=46;
	public static final int INSTRUCTION_FORMAT12x=47;
	public static final int INSTRUCTION_FORMAT12x_OR_ID=48;
	public static final int INSTRUCTION_FORMAT20bc=49;
	public static final int INSTRUCTION_FORMAT20t=50;
	public static final int INSTRUCTION_FORMAT21c_FIELD=51;
	public static final int INSTRUCTION_FORMAT21c_FIELD_ODEX=52;
	public static final int INSTRUCTION_FORMAT21c_STRING=53;
	public static final int INSTRUCTION_FORMAT21c_TYPE=54;
	public static final int INSTRUCTION_FORMAT21ih=55;
	public static final int INSTRUCTION_FORMAT21lh=56;
	public static final int INSTRUCTION_FORMAT21s=57;
	public static final int INSTRUCTION_FORMAT21t=58;
	public static final int INSTRUCTION_FORMAT22b=59;
	public static final int INSTRUCTION_FORMAT22c_FIELD=60;
	public static final int INSTRUCTION_FORMAT22c_FIELD_ODEX=61;
	public static final int INSTRUCTION_FORMAT22c_TYPE=62;
	public static final int INSTRUCTION_FORMAT22cs_FIELD=63;
	public static final int INSTRUCTION_FORMAT22s=64;
	public static final int INSTRUCTION_FORMAT22s_OR_ID=65;
	public static final int INSTRUCTION_FORMAT22t=66;
	public static final int INSTRUCTION_FORMAT22x=67;
	public static final int INSTRUCTION_FORMAT23x=68;
	public static final int INSTRUCTION_FORMAT30t=69;
	public static final int INSTRUCTION_FORMAT31c=70;
	public static final int INSTRUCTION_FORMAT31i=71;
	public static final int INSTRUCTION_FORMAT31i_OR_ID=72;
	public static final int INSTRUCTION_FORMAT31t=73;
	public static final int INSTRUCTION_FORMAT32x=74;
	public static final int INSTRUCTION_FORMAT35c_METHOD=75;
	public static final int INSTRUCTION_FORMAT35c_METHOD_ODEX=76;
	public static final int INSTRUCTION_FORMAT35c_TYPE=77;
	public static final int INSTRUCTION_FORMAT35mi_METHOD=78;
	public static final int INSTRUCTION_FORMAT35ms_METHOD=79;
	public static final int INSTRUCTION_FORMAT3rc_METHOD=80;
	public static final int INSTRUCTION_FORMAT3rc_METHOD_ODEX=81;
	public static final int INSTRUCTION_FORMAT3rc_TYPE=82;
	public static final int INSTRUCTION_FORMAT3rmi_METHOD=83;
	public static final int INSTRUCTION_FORMAT3rms_METHOD=84;
	public static final int INSTRUCTION_FORMAT51l=85;
	public static final int INTEGER_LITERAL=86;
	public static final int INVALID_TOKEN=87;
	public static final int I_ACCESS_LIST=88;
	public static final int I_ANNOTATION=89;
	public static final int I_ANNOTATIONS=90;
	public static final int I_ANNOTATION_ELEMENT=91;
	public static final int I_ARRAY_ELEMENTS=92;
	public static final int I_ARRAY_ELEMENT_SIZE=93;
	public static final int I_CATCH=94;
	public static final int I_CATCHALL=95;
	public static final int I_CATCHES=96;
	public static final int I_CLASS_DEF=97;
	public static final int I_ENCODED_ARRAY=98;
	public static final int I_ENCODED_ENUM=99;
	public static final int I_ENCODED_FIELD=100;
	public static final int I_ENCODED_METHOD=101;
	public static final int I_END_LOCAL=102;
	public static final int I_EPILOGUE=103;
	public static final int I_FIELD=104;
	public static final int I_FIELDS=105;
	public static final int I_FIELD_INITIAL_VALUE=106;
	public static final int I_FIELD_TYPE=107;
	public static final int I_IMPLEMENTS=108;
	public static final int I_LABEL=109;
	public static final int I_LINE=110;
	public static final int I_LOCAL=111;
	public static final int I_LOCALS=112;
	public static final int I_METHOD=113;
	public static final int I_METHODS=114;
	public static final int I_METHOD_PROTOTYPE=115;
	public static final int I_METHOD_RETURN_TYPE=116;
	public static final int I_ORDERED_METHOD_ITEMS=117;
	public static final int I_PACKED_SWITCH_ELEMENTS=118;
	public static final int I_PACKED_SWITCH_START_KEY=119;
	public static final int I_PARAMETER=120;
	public static final int I_PARAMETERS=121;
	public static final int I_PARAMETER_NOT_SPECIFIED=122;
	public static final int I_PROLOGUE=123;
	public static final int I_REGISTERS=124;
	public static final int I_REGISTER_LIST=125;
	public static final int I_REGISTER_RANGE=126;
	public static final int I_RESTART_LOCAL=127;
	public static final int I_SOURCE=128;
	public static final int I_SPARSE_SWITCH_ELEMENTS=129;
	public static final int I_STATEMENT_ARRAY_DATA=130;
	public static final int I_STATEMENT_FORMAT10t=131;
	public static final int I_STATEMENT_FORMAT10x=132;
	public static final int I_STATEMENT_FORMAT11n=133;
	public static final int I_STATEMENT_FORMAT11x=134;
	public static final int I_STATEMENT_FORMAT12x=135;
	public static final int I_STATEMENT_FORMAT20bc=136;
	public static final int I_STATEMENT_FORMAT20t=137;
	public static final int I_STATEMENT_FORMAT21c_FIELD=138;
	public static final int I_STATEMENT_FORMAT21c_STRING=139;
	public static final int I_STATEMENT_FORMAT21c_TYPE=140;
	public static final int I_STATEMENT_FORMAT21ih=141;
	public static final int I_STATEMENT_FORMAT21lh=142;
	public static final int I_STATEMENT_FORMAT21s=143;
	public static final int I_STATEMENT_FORMAT21t=144;
	public static final int I_STATEMENT_FORMAT22b=145;
	public static final int I_STATEMENT_FORMAT22c_FIELD=146;
	public static final int I_STATEMENT_FORMAT22c_TYPE=147;
	public static final int I_STATEMENT_FORMAT22s=148;
	public static final int I_STATEMENT_FORMAT22t=149;
	public static final int I_STATEMENT_FORMAT22x=150;
	public static final int I_STATEMENT_FORMAT23x=151;
	public static final int I_STATEMENT_FORMAT30t=152;
	public static final int I_STATEMENT_FORMAT31c=153;
	public static final int I_STATEMENT_FORMAT31i=154;
	public static final int I_STATEMENT_FORMAT31t=155;
	public static final int I_STATEMENT_FORMAT32x=156;
	public static final int I_STATEMENT_FORMAT35c_METHOD=157;
	public static final int I_STATEMENT_FORMAT35c_TYPE=158;
	public static final int I_STATEMENT_FORMAT3rc_METHOD=159;
	public static final int I_STATEMENT_FORMAT3rc_TYPE=160;
	public static final int I_STATEMENT_FORMAT51l=161;
	public static final int I_STATEMENT_PACKED_SWITCH=162;
	public static final int I_STATEMENT_SPARSE_SWITCH=163;
	public static final int I_SUBANNOTATION=164;
	public static final int I_SUPER=165;
	public static final int LINE_COMMENT=166;
	public static final int LINE_DIRECTIVE=167;
	public static final int LOCALS_DIRECTIVE=168;
	public static final int LOCAL_DIRECTIVE=169;
	public static final int LONG_LITERAL=170;
	public static final int MEMBER_NAME=171;
	public static final int METHOD_DIRECTIVE=172;
	public static final int NEGATIVE_INTEGER_LITERAL=173;
	public static final int NULL_LITERAL=174;
	public static final int OPEN_BRACE=175;
	public static final int OPEN_PAREN=176;
	public static final int PACKED_SWITCH_DIRECTIVE=177;
	public static final int PARAMETER_DIRECTIVE=178;
	public static final int PARAM_LIST_END=179;
	public static final int PARAM_LIST_OR_ID_END=180;
	public static final int PARAM_LIST_OR_ID_START=181;
	public static final int PARAM_LIST_START=182;
	public static final int POSITIVE_INTEGER_LITERAL=183;
	public static final int PRIMITIVE_TYPE=184;
	public static final int PROLOGUE_DIRECTIVE=185;
	public static final int REGISTER=186;
	public static final int REGISTERS_DIRECTIVE=187;
	public static final int RESTART_LOCAL_DIRECTIVE=188;
	public static final int SHORT_LITERAL=189;
	public static final int SIMPLE_NAME=190;
	public static final int SOURCE_DIRECTIVE=191;
	public static final int SPARSE_SWITCH_DIRECTIVE=192;
	public static final int STRING_LITERAL=193;
	public static final int SUBANNOTATION_DIRECTIVE=194;
	public static final int SUPER_DIRECTIVE=195;
	public static final int VERIFICATION_ERROR_TYPE=196;
	public static final int VOID_TYPE=197;
	public static final int VTABLE_INDEX=198;
	public static final int WHITE_SPACE=199;

	// delegates
	public Parser[] getDelegates() {
		return new Parser[] {};
	}

	// delegators


	public smaliParser(TokenStream input) {
		this(input, new RecognizerSharedState());
	}
	public smaliParser(TokenStream input, RecognizerSharedState state) {
		super(input, state);
	}

	protected TreeAdaptor adaptor = new CommonTreeAdaptor();

	public void setTreeAdaptor(TreeAdaptor adaptor) {
		this.adaptor = adaptor;
	}
	public TreeAdaptor getTreeAdaptor() {
		return adaptor;
	}
	@Override public String[] getTokenNames() { return smaliParser.tokenNames; }
	@Override public String getGrammarFileName() { return "/mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g"; }


	  public static final int ERROR_CHANNEL = 100;

	  private boolean verboseErrors = false;
	  private boolean allowOdex = false;
	  private int apiLevel = 15;
	  private Opcodes opcodes = new Opcodes(apiLevel);

	  public void setVerboseErrors(boolean verboseErrors) {
	    this.verboseErrors = verboseErrors;
	  }

	  public void setAllowOdex(boolean allowOdex) {
	      this.allowOdex = allowOdex;
	  }

	  public void setApiLevel(int apiLevel) {
	      this.opcodes = new Opcodes(apiLevel);
	      this.apiLevel = apiLevel;
	  }

	  public String getErrorMessage(RecognitionException e,
	    String[] tokenNames) {

	    if (verboseErrors) {
	      List stack = getRuleInvocationStack(e, this.getClass().getName());
	      String msg = null;

	      if (e instanceof NoViableAltException) {
	        NoViableAltException nvae = (NoViableAltException)e;
	        msg = " no viable alt; token="+getTokenErrorDisplay(e.token)+
	        " (decision="+nvae.decisionNumber+
	        " state "+nvae.stateNumber+")"+
	        " decision=<<"+nvae.grammarDecisionDescription+">>";
	      } else {
	        msg = super.getErrorMessage(e, tokenNames);
	      }

	      return stack + " " + msg;
	    } else {
	      return super.getErrorMessage(e, tokenNames);
	    }
	  }

	  public String getTokenErrorDisplay(Token t) {
	    if (!verboseErrors) {
	      String s = t.getText();
	      if ( s==null ) {
	        if ( t.getType()==Token.EOF ) {
	          s = "<EOF>";
	        }
	        else {
	          s = "<"+tokenNames[t.getType()]+">";
	        }
	      }
	      s = s.replaceAll("\n","\\\\n");
	      s = s.replaceAll("\r","\\\\r");
	      s = s.replaceAll("\t","\\\\t");
	      return "'"+s+"'";
	    }

	    CommonToken ct = (CommonToken)t;

	    String channelStr = "";
	    if (t.getChannel()>0) {
	      channelStr=",channel="+t.getChannel();
	    }
	    String txt = t.getText();
	    if ( txt!=null ) {
	      txt = txt.replaceAll("\n","\\\\n");
	      txt = txt.replaceAll("\r","\\\\r");
	      txt = txt.replaceAll("\t","\\\\t");
	    }
	    else {
	      txt = "<no text>";
	    }
	    return "[@"+t.getTokenIndex()+","+ct.getStartIndex()+":"+ct.getStopIndex()+"='"+txt+"',<"+tokenNames[t.getType()]+">"+channelStr+","+t.getLine()+":"+t.getCharPositionInLine()+"]";
	  }

	  public String getErrorHeader(RecognitionException e) {
	    return getSourceName()+"["+ e.line+","+e.charPositionInLine+"]";
	  }

	  private CommonTree buildTree(int type, String text, List<CommonTree> children) {
	    CommonTree root = new CommonTree(new CommonToken(type, text));
	    for (CommonTree child: children) {
	      root.addChild(child);
	    }
	    return root;
	  }

	  private CommonToken getParamListSubToken(CommonToken baseToken, String str, int typeStartIndex) {
	    CommonToken token = new CommonToken(baseToken);
	    token.setStartIndex(baseToken.getStartIndex() + typeStartIndex);

	    switch (str.charAt(typeStartIndex)) {
	      case 'Z':
	      case 'B':
	      case 'S':
	      case 'C':
	      case 'I':
	      case 'J':
	      case 'F':
	      case 'D':
	      {
	        token.setType(PRIMITIVE_TYPE);
	        token.setText(str.substring(typeStartIndex, typeStartIndex+1));
	        token.setStopIndex(baseToken.getStartIndex() + typeStartIndex);
	        break;
	      }
	      case 'L':
	      {
	        int i = typeStartIndex;
	        while (str.charAt(++i) != ';');

	        token.setType(CLASS_DESCRIPTOR);
	        token.setText(str.substring(typeStartIndex, i + 1));
	        token.setStopIndex(baseToken.getStartIndex() + i);
	        break;
	      }
	      case '[':
	      {
	        int i = typeStartIndex;
	            while (str.charAt(++i) == '[');

	            if (str.charAt(i++) == 'L') {
	                while (str.charAt(i++) != ';');
	        }

	            token.setType(ARRAY_DESCRIPTOR);
	            token.setText(str.substring(typeStartIndex, i));
	            token.setStopIndex(baseToken.getStartIndex() + i - 1);
	            break;
	      }
	      default:
	        throw new RuntimeException(String.format("Invalid character '%c' in param list \"%s\" at position %d", str.charAt(typeStartIndex), str, typeStartIndex));
	    }

	    return token;
	  }

	  private CommonTree parseParamList(CommonToken paramListToken) {
	    String paramList = paramListToken.getText();
	    CommonTree root = new CommonTree();

	    int startIndex = paramListToken.getStartIndex();

	    int i=0;
	    while (i<paramList.length()) {
	      CommonToken token = getParamListSubToken(paramListToken, paramList, i);
	      root.addChild(new CommonTree(token));
	      i += token.getText().length();
	    }

	    if (root.getChildCount() == 0) {
	      return null;
	    }
	    return root;
	  }

	  private void throwOdexedInstructionException(IntStream input, String odexedInstruction)
	      throws OdexedInstructionException {
	    /*this has to be done in a separate method, otherwise java will complain about the
	    auto-generated code in the rule after the throw not being reachable*/
	    throw new OdexedInstructionException(input, odexedInstruction);
	  }


	protected static class smali_file_scope {
		boolean hasClassSpec;
		boolean hasSuperSpec;
		boolean hasSourceSpec;
		List<CommonTree> classAnnotations;
	}
	protected Stack<smali_file_scope> smali_file_stack = new Stack<smali_file_scope>();

	public static class smali_file_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "smali_file"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:418:1: smali_file : ({...}? => class_spec |{...}? => super_spec | implements_spec |{...}? => source_spec | method | field | annotation )+ EOF -> ^( I_CLASS_DEF class_spec ( super_spec )? ( implements_spec )* ( source_spec )? ^( I_METHODS ( method )* ) ^( I_FIELDS ( field )* ) ) ;
	public final smaliParser.smali_file_return smali_file() throws RecognitionException {
		smali_file_stack.push(new smali_file_scope());
		smaliParser.smali_file_return retval = new smaliParser.smali_file_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token EOF8=null;
		ParserRuleReturnScope class_spec1 =null;
		ParserRuleReturnScope super_spec2 =null;
		ParserRuleReturnScope implements_spec3 =null;
		ParserRuleReturnScope source_spec4 =null;
		ParserRuleReturnScope method5 =null;
		ParserRuleReturnScope field6 =null;
		ParserRuleReturnScope annotation7 =null;

		CommonTree EOF8_tree=null;
		RewriteRuleTokenStream stream_EOF=new RewriteRuleTokenStream(adaptor,"token EOF");
		RewriteRuleSubtreeStream stream_field=new RewriteRuleSubtreeStream(adaptor,"rule field");
		RewriteRuleSubtreeStream stream_annotation=new RewriteRuleSubtreeStream(adaptor,"rule annotation");
		RewriteRuleSubtreeStream stream_super_spec=new RewriteRuleSubtreeStream(adaptor,"rule super_spec");
		RewriteRuleSubtreeStream stream_implements_spec=new RewriteRuleSubtreeStream(adaptor,"rule implements_spec");
		RewriteRuleSubtreeStream stream_source_spec=new RewriteRuleSubtreeStream(adaptor,"rule source_spec");
		RewriteRuleSubtreeStream stream_method=new RewriteRuleSubtreeStream(adaptor,"rule method");
		RewriteRuleSubtreeStream stream_class_spec=new RewriteRuleSubtreeStream(adaptor,"rule class_spec");

		 smali_file_stack.peek().hasClassSpec = smali_file_stack.peek().hasSuperSpec = smali_file_stack.peek().hasSourceSpec = false;
		    smali_file_stack.peek().classAnnotations = new ArrayList<CommonTree>();
		
		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:430:3: ( ({...}? => class_spec |{...}? => super_spec | implements_spec |{...}? => source_spec | method | field | annotation )+ EOF -> ^( I_CLASS_DEF class_spec ( super_spec )? ( implements_spec )* ( source_spec )? ^( I_METHODS ( method )* ) ^( I_FIELDS ( field )* ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:431:3: ({...}? => class_spec |{...}? => super_spec | implements_spec |{...}? => source_spec | method | field | annotation )+ EOF
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:431:3: ({...}? => class_spec |{...}? => super_spec | implements_spec |{...}? => source_spec | method | field | annotation )+
			int cnt1=0;
			loop1:
			while (true) {
				int alt1=8;
				int LA1_0 = input.LA(1);
				if ( (LA1_0==CLASS_DIRECTIVE) && ((!smali_file_stack.peek().hasClassSpec))) {
					alt1=1;
				}
				else if ( (LA1_0==SUPER_DIRECTIVE) && ((!smali_file_stack.peek().hasSuperSpec))) {
					alt1=2;
				}
				else if ( (LA1_0==IMPLEMENTS_DIRECTIVE) ) {
					alt1=3;
				}
				else if ( (LA1_0==SOURCE_DIRECTIVE) && ((!smali_file_stack.peek().hasSourceSpec))) {
					alt1=4;
				}
				else if ( (LA1_0==METHOD_DIRECTIVE) ) {
					alt1=5;
				}
				else if ( (LA1_0==FIELD_DIRECTIVE) ) {
					alt1=6;
				}
				else if ( (LA1_0==ANNOTATION_DIRECTIVE) ) {
					alt1=7;
				}

				switch (alt1) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:431:5: {...}? => class_spec
					{
					if ( !((!smali_file_stack.peek().hasClassSpec)) ) {
						throw new FailedPredicateException(input, "smali_file", "!$smali_file::hasClassSpec");
					}
					pushFollow(FOLLOW_class_spec_in_smali_file1070);
					class_spec1=class_spec();
					state._fsp--;

					stream_class_spec.add(class_spec1.getTree());
					smali_file_stack.peek().hasClassSpec = true;
					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:432:5: {...}? => super_spec
					{
					if ( !((!smali_file_stack.peek().hasSuperSpec)) ) {
						throw new FailedPredicateException(input, "smali_file", "!$smali_file::hasSuperSpec");
					}
					pushFollow(FOLLOW_super_spec_in_smali_file1081);
					super_spec2=super_spec();
					state._fsp--;

					stream_super_spec.add(super_spec2.getTree());
					smali_file_stack.peek().hasSuperSpec = true;
					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:433:5: implements_spec
					{
					pushFollow(FOLLOW_implements_spec_in_smali_file1089);
					implements_spec3=implements_spec();
					state._fsp--;

					stream_implements_spec.add(implements_spec3.getTree());
					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:434:5: {...}? => source_spec
					{
					if ( !((!smali_file_stack.peek().hasSourceSpec)) ) {
						throw new FailedPredicateException(input, "smali_file", "!$smali_file::hasSourceSpec");
					}
					pushFollow(FOLLOW_source_spec_in_smali_file1098);
					source_spec4=source_spec();
					state._fsp--;

					stream_source_spec.add(source_spec4.getTree());
					smali_file_stack.peek().hasSourceSpec = true;
					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:435:5: method
					{
					pushFollow(FOLLOW_method_in_smali_file1106);
					method5=method();
					state._fsp--;

					stream_method.add(method5.getTree());
					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:436:5: field
					{
					pushFollow(FOLLOW_field_in_smali_file1112);
					field6=field();
					state._fsp--;

					stream_field.add(field6.getTree());
					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:437:5: annotation
					{
					pushFollow(FOLLOW_annotation_in_smali_file1118);
					annotation7=annotation();
					state._fsp--;

					stream_annotation.add(annotation7.getTree());
					smali_file_stack.peek().classAnnotations.add((annotation7!=null?((CommonTree)annotation7.getTree()):null));
					}
					break;

				default :
					if ( cnt1 >= 1 ) break loop1;
					EarlyExitException eee = new EarlyExitException(1, input);
					throw eee;
				}
				cnt1++;
			}

			EOF8=(Token)match(input,EOF,FOLLOW_EOF_in_smali_file1129);
			stream_EOF.add(EOF8);


			    if (!smali_file_stack.peek().hasClassSpec) {
			      throw new SemanticException(input, "The file must contain a .class directive");
			    }

			    if (!smali_file_stack.peek().hasSuperSpec) {
			      if (!(class_spec1!=null?((smaliParser.class_spec_return)class_spec1).className:null).equals("Ljava/lang/Object;")) {
			        throw new SemanticException(input, "The file must contain a .super directive");
			      }
			    }
			
			// AST REWRITE
			// elements: method, field, source_spec, implements_spec, super_spec, class_spec
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 451:3: -> ^( I_CLASS_DEF class_spec ( super_spec )? ( implements_spec )* ( source_spec )? ^( I_METHODS ( method )* ) ^( I_FIELDS ( field )* ) )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:451:6: ^( I_CLASS_DEF class_spec ( super_spec )? ( implements_spec )* ( source_spec )? ^( I_METHODS ( method )* ) ^( I_FIELDS ( field )* ) )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_CLASS_DEF, "I_CLASS_DEF"), root_1);
				adaptor.addChild(root_1, stream_class_spec.nextTree());
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:453:8: ( super_spec )?
				if ( stream_super_spec.hasNext() ) {
					adaptor.addChild(root_1, stream_super_spec.nextTree());
				}
				stream_super_spec.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:454:8: ( implements_spec )*
				while ( stream_implements_spec.hasNext() ) {
					adaptor.addChild(root_1, stream_implements_spec.nextTree());
				}
				stream_implements_spec.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:455:8: ( source_spec )?
				if ( stream_source_spec.hasNext() ) {
					adaptor.addChild(root_1, stream_source_spec.nextTree());
				}
				stream_source_spec.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:456:8: ^( I_METHODS ( method )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_METHODS, "I_METHODS"), root_2);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:456:20: ( method )*
				while ( stream_method.hasNext() ) {
					adaptor.addChild(root_2, stream_method.nextTree());
				}
				stream_method.reset();

				adaptor.addChild(root_1, root_2);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:456:29: ^( I_FIELDS ( field )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELDS, "I_FIELDS"), root_2);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:456:40: ( field )*
				while ( stream_field.hasNext() ) {
					adaptor.addChild(root_2, stream_field.nextTree());
				}
				stream_field.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_1, buildTree(I_ANNOTATIONS, "I_ANNOTATIONS", smali_file_stack.peek().classAnnotations));
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
			smali_file_stack.pop();
		}
		return retval;
	}
	// $ANTLR end "smali_file"


	public static class class_spec_return extends ParserRuleReturnScope {
		public String className;
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "class_spec"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:458:1: class_spec returns [String className] : CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR -> CLASS_DESCRIPTOR access_list ;
	public final smaliParser.class_spec_return class_spec() throws RecognitionException {
		smaliParser.class_spec_return retval = new smaliParser.class_spec_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token CLASS_DIRECTIVE9=null;
		Token CLASS_DESCRIPTOR11=null;
		ParserRuleReturnScope access_list10 =null;

		CommonTree CLASS_DIRECTIVE9_tree=null;
		CommonTree CLASS_DESCRIPTOR11_tree=null;
		RewriteRuleTokenStream stream_CLASS_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token CLASS_DIRECTIVE");
		RewriteRuleTokenStream stream_CLASS_DESCRIPTOR=new RewriteRuleTokenStream(adaptor,"token CLASS_DESCRIPTOR");
		RewriteRuleSubtreeStream stream_access_list=new RewriteRuleSubtreeStream(adaptor,"rule access_list");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:459:3: ( CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR -> CLASS_DESCRIPTOR access_list )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:459:5: CLASS_DIRECTIVE access_list CLASS_DESCRIPTOR
			{
			CLASS_DIRECTIVE9=(Token)match(input,CLASS_DIRECTIVE,FOLLOW_CLASS_DIRECTIVE_in_class_spec1216);
			stream_CLASS_DIRECTIVE.add(CLASS_DIRECTIVE9);

			pushFollow(FOLLOW_access_list_in_class_spec1218);
			access_list10=access_list();
			state._fsp--;

			stream_access_list.add(access_list10.getTree());
			CLASS_DESCRIPTOR11=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_class_spec1220);
			stream_CLASS_DESCRIPTOR.add(CLASS_DESCRIPTOR11);

			retval.className = (CLASS_DESCRIPTOR11!=null?CLASS_DESCRIPTOR11.getText():null);
			// AST REWRITE
			// elements: CLASS_DESCRIPTOR, access_list
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 459:89: -> CLASS_DESCRIPTOR access_list
			{
				adaptor.addChild(root_0, stream_CLASS_DESCRIPTOR.nextNode());
				adaptor.addChild(root_0, stream_access_list.nextTree());
			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "class_spec"


	public static class super_spec_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "super_spec"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:461:1: super_spec : SUPER_DIRECTIVE CLASS_DESCRIPTOR -> ^( I_SUPER[$start, \"I_SUPER\"] CLASS_DESCRIPTOR ) ;
	public final smaliParser.super_spec_return super_spec() throws RecognitionException {
		smaliParser.super_spec_return retval = new smaliParser.super_spec_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SUPER_DIRECTIVE12=null;
		Token CLASS_DESCRIPTOR13=null;

		CommonTree SUPER_DIRECTIVE12_tree=null;
		CommonTree CLASS_DESCRIPTOR13_tree=null;
		RewriteRuleTokenStream stream_SUPER_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token SUPER_DIRECTIVE");
		RewriteRuleTokenStream stream_CLASS_DESCRIPTOR=new RewriteRuleTokenStream(adaptor,"token CLASS_DESCRIPTOR");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:462:3: ( SUPER_DIRECTIVE CLASS_DESCRIPTOR -> ^( I_SUPER[$start, \"I_SUPER\"] CLASS_DESCRIPTOR ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:462:5: SUPER_DIRECTIVE CLASS_DESCRIPTOR
			{
			SUPER_DIRECTIVE12=(Token)match(input,SUPER_DIRECTIVE,FOLLOW_SUPER_DIRECTIVE_in_super_spec1238);
			stream_SUPER_DIRECTIVE.add(SUPER_DIRECTIVE12);

			CLASS_DESCRIPTOR13=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_super_spec1240);
			stream_CLASS_DESCRIPTOR.add(CLASS_DESCRIPTOR13);

			// AST REWRITE
			// elements: CLASS_DESCRIPTOR
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 462:38: -> ^( I_SUPER[$start, \"I_SUPER\"] CLASS_DESCRIPTOR )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:462:41: ^( I_SUPER[$start, \"I_SUPER\"] CLASS_DESCRIPTOR )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SUPER, (retval.start), "I_SUPER"), root_1);
				adaptor.addChild(root_1, stream_CLASS_DESCRIPTOR.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "super_spec"


	public static class implements_spec_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "implements_spec"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:464:1: implements_spec : IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR -> ^( I_IMPLEMENTS[$start, \"I_IMPLEMENTS\"] CLASS_DESCRIPTOR ) ;
	public final smaliParser.implements_spec_return implements_spec() throws RecognitionException {
		smaliParser.implements_spec_return retval = new smaliParser.implements_spec_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token IMPLEMENTS_DIRECTIVE14=null;
		Token CLASS_DESCRIPTOR15=null;

		CommonTree IMPLEMENTS_DIRECTIVE14_tree=null;
		CommonTree CLASS_DESCRIPTOR15_tree=null;
		RewriteRuleTokenStream stream_IMPLEMENTS_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token IMPLEMENTS_DIRECTIVE");
		RewriteRuleTokenStream stream_CLASS_DESCRIPTOR=new RewriteRuleTokenStream(adaptor,"token CLASS_DESCRIPTOR");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:465:3: ( IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR -> ^( I_IMPLEMENTS[$start, \"I_IMPLEMENTS\"] CLASS_DESCRIPTOR ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:465:5: IMPLEMENTS_DIRECTIVE CLASS_DESCRIPTOR
			{
			IMPLEMENTS_DIRECTIVE14=(Token)match(input,IMPLEMENTS_DIRECTIVE,FOLLOW_IMPLEMENTS_DIRECTIVE_in_implements_spec1259);
			stream_IMPLEMENTS_DIRECTIVE.add(IMPLEMENTS_DIRECTIVE14);

			CLASS_DESCRIPTOR15=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_implements_spec1261);
			stream_CLASS_DESCRIPTOR.add(CLASS_DESCRIPTOR15);

			// AST REWRITE
			// elements: CLASS_DESCRIPTOR
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 465:43: -> ^( I_IMPLEMENTS[$start, \"I_IMPLEMENTS\"] CLASS_DESCRIPTOR )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:465:46: ^( I_IMPLEMENTS[$start, \"I_IMPLEMENTS\"] CLASS_DESCRIPTOR )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_IMPLEMENTS, (retval.start), "I_IMPLEMENTS"), root_1);
				adaptor.addChild(root_1, stream_CLASS_DESCRIPTOR.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "implements_spec"


	public static class source_spec_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "source_spec"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:467:1: source_spec : SOURCE_DIRECTIVE STRING_LITERAL -> ^( I_SOURCE[$start, \"I_SOURCE\"] STRING_LITERAL ) ;
	public final smaliParser.source_spec_return source_spec() throws RecognitionException {
		smaliParser.source_spec_return retval = new smaliParser.source_spec_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SOURCE_DIRECTIVE16=null;
		Token STRING_LITERAL17=null;

		CommonTree SOURCE_DIRECTIVE16_tree=null;
		CommonTree STRING_LITERAL17_tree=null;
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_SOURCE_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token SOURCE_DIRECTIVE");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:468:3: ( SOURCE_DIRECTIVE STRING_LITERAL -> ^( I_SOURCE[$start, \"I_SOURCE\"] STRING_LITERAL ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:468:5: SOURCE_DIRECTIVE STRING_LITERAL
			{
			SOURCE_DIRECTIVE16=(Token)match(input,SOURCE_DIRECTIVE,FOLLOW_SOURCE_DIRECTIVE_in_source_spec1280);
			stream_SOURCE_DIRECTIVE.add(SOURCE_DIRECTIVE16);

			STRING_LITERAL17=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_source_spec1282);
			stream_STRING_LITERAL.add(STRING_LITERAL17);

			// AST REWRITE
			// elements: STRING_LITERAL
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 468:37: -> ^( I_SOURCE[$start, \"I_SOURCE\"] STRING_LITERAL )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:468:40: ^( I_SOURCE[$start, \"I_SOURCE\"] STRING_LITERAL )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SOURCE, (retval.start), "I_SOURCE"), root_1);
				adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "source_spec"


	public static class access_list_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "access_list"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:470:1: access_list : ( ACCESS_SPEC )* -> ^( I_ACCESS_LIST[$start,\"I_ACCESS_LIST\"] ( ACCESS_SPEC )* ) ;
	public final smaliParser.access_list_return access_list() throws RecognitionException {
		smaliParser.access_list_return retval = new smaliParser.access_list_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ACCESS_SPEC18=null;

		CommonTree ACCESS_SPEC18_tree=null;
		RewriteRuleTokenStream stream_ACCESS_SPEC=new RewriteRuleTokenStream(adaptor,"token ACCESS_SPEC");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:3: ( ( ACCESS_SPEC )* -> ^( I_ACCESS_LIST[$start,\"I_ACCESS_LIST\"] ( ACCESS_SPEC )* ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:5: ( ACCESS_SPEC )*
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:5: ( ACCESS_SPEC )*
			loop2:
			while (true) {
				int alt2=2;
				int LA2_0 = input.LA(1);
				if ( (LA2_0==ACCESS_SPEC) ) {
					int LA2_2 = input.LA(2);
					if ( (LA2_2==ACCESS_SPEC||LA2_2==ANNOTATION_VISIBILITY||LA2_2==BOOL_LITERAL||LA2_2==CLASS_DESCRIPTOR||LA2_2==DOUBLE_LITERAL_OR_ID||LA2_2==FLOAT_LITERAL_OR_ID||(LA2_2 >= INSTRUCTION_FORMAT10t && LA2_2 <= INSTRUCTION_FORMAT10x_ODEX)||LA2_2==INSTRUCTION_FORMAT11x||LA2_2==INSTRUCTION_FORMAT12x_OR_ID||(LA2_2 >= INSTRUCTION_FORMAT21c_FIELD && LA2_2 <= INSTRUCTION_FORMAT21c_TYPE)||LA2_2==INSTRUCTION_FORMAT21t||(LA2_2 >= INSTRUCTION_FORMAT22c_FIELD && LA2_2 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA2_2 >= INSTRUCTION_FORMAT22s_OR_ID && LA2_2 <= INSTRUCTION_FORMAT22t)||LA2_2==INSTRUCTION_FORMAT23x||(LA2_2 >= INSTRUCTION_FORMAT31i_OR_ID && LA2_2 <= INSTRUCTION_FORMAT31t)||(LA2_2 >= INSTRUCTION_FORMAT35c_METHOD && LA2_2 <= INSTRUCTION_FORMAT35ms_METHOD)||LA2_2==INSTRUCTION_FORMAT51l||LA2_2==MEMBER_NAME||(LA2_2 >= NEGATIVE_INTEGER_LITERAL && LA2_2 <= NULL_LITERAL)||LA2_2==PARAM_LIST_OR_ID_START||(LA2_2 >= POSITIVE_INTEGER_LITERAL && LA2_2 <= PRIMITIVE_TYPE)||LA2_2==REGISTER||LA2_2==SIMPLE_NAME||(LA2_2 >= VERIFICATION_ERROR_TYPE && LA2_2 <= VOID_TYPE)) ) {
						alt2=1;
					}

				}

				switch (alt2) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:5: ACCESS_SPEC
					{
					ACCESS_SPEC18=(Token)match(input,ACCESS_SPEC,FOLLOW_ACCESS_SPEC_in_access_list1301);
					stream_ACCESS_SPEC.add(ACCESS_SPEC18);

					}
					break;

				default :
					break loop2;
				}
			}

			// AST REWRITE
			// elements: ACCESS_SPEC
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 471:18: -> ^( I_ACCESS_LIST[$start,\"I_ACCESS_LIST\"] ( ACCESS_SPEC )* )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:21: ^( I_ACCESS_LIST[$start,\"I_ACCESS_LIST\"] ( ACCESS_SPEC )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ACCESS_LIST, (retval.start), "I_ACCESS_LIST"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:471:61: ( ACCESS_SPEC )*
				while ( stream_ACCESS_SPEC.hasNext() ) {
					adaptor.addChild(root_1, stream_ACCESS_SPEC.nextNode());
				}
				stream_ACCESS_SPEC.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "access_list"


	public static class field_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "field"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:478:1: field : FIELD_DIRECTIVE access_list member_name COLON nonvoid_type_descriptor ( EQUAL literal )? ( ({...}? annotation )* ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) ) ) ;
	public final smaliParser.field_return field() throws RecognitionException {
		smaliParser.field_return retval = new smaliParser.field_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token FIELD_DIRECTIVE19=null;
		Token COLON22=null;
		Token EQUAL24=null;
		Token END_FIELD_DIRECTIVE27=null;
		ParserRuleReturnScope access_list20 =null;
		ParserRuleReturnScope member_name21 =null;
		ParserRuleReturnScope nonvoid_type_descriptor23 =null;
		ParserRuleReturnScope literal25 =null;
		ParserRuleReturnScope annotation26 =null;

		CommonTree FIELD_DIRECTIVE19_tree=null;
		CommonTree COLON22_tree=null;
		CommonTree EQUAL24_tree=null;
		CommonTree END_FIELD_DIRECTIVE27_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleTokenStream stream_FIELD_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token FIELD_DIRECTIVE");
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleTokenStream stream_END_FIELD_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_FIELD_DIRECTIVE");
		RewriteRuleSubtreeStream stream_annotation=new RewriteRuleSubtreeStream(adaptor,"rule annotation");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_access_list=new RewriteRuleSubtreeStream(adaptor,"rule access_list");
		RewriteRuleSubtreeStream stream_member_name=new RewriteRuleSubtreeStream(adaptor,"rule member_name");
		RewriteRuleSubtreeStream stream_literal=new RewriteRuleSubtreeStream(adaptor,"rule literal");

		List<CommonTree> annotations = new ArrayList<CommonTree>();
		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:480:3: ( FIELD_DIRECTIVE access_list member_name COLON nonvoid_type_descriptor ( EQUAL literal )? ( ({...}? annotation )* ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:480:5: FIELD_DIRECTIVE access_list member_name COLON nonvoid_type_descriptor ( EQUAL literal )? ( ({...}? annotation )* ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) ) )
			{
			FIELD_DIRECTIVE19=(Token)match(input,FIELD_DIRECTIVE,FOLLOW_FIELD_DIRECTIVE_in_field1332);
			stream_FIELD_DIRECTIVE.add(FIELD_DIRECTIVE19);

			pushFollow(FOLLOW_access_list_in_field1334);
			access_list20=access_list();
			state._fsp--;

			stream_access_list.add(access_list20.getTree());
			pushFollow(FOLLOW_member_name_in_field1336);
			member_name21=member_name();
			state._fsp--;

			stream_member_name.add(member_name21.getTree());
			COLON22=(Token)match(input,COLON,FOLLOW_COLON_in_field1338);
			stream_COLON.add(COLON22);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_field1340);
			nonvoid_type_descriptor23=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor23.getTree());
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:480:75: ( EQUAL literal )?
			int alt3=2;
			int LA3_0 = input.LA(1);
			if ( (LA3_0==EQUAL) ) {
				alt3=1;
			}
			switch (alt3) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:480:76: EQUAL literal
					{
					EQUAL24=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_field1343);
					stream_EQUAL.add(EQUAL24);

					pushFollow(FOLLOW_literal_in_field1345);
					literal25=literal();
					state._fsp--;

					stream_literal.add(literal25.getTree());
					}
					break;

			}

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:481:5: ( ({...}? annotation )* ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:481:7: ({...}? annotation )* ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) )
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:481:7: ({...}? annotation )*
			loop4:
			while (true) {
				int alt4=2;
				int LA4_0 = input.LA(1);
				if ( (LA4_0==ANNOTATION_DIRECTIVE) ) {
					int LA4_9 = input.LA(2);
					if ( ((input.LA(1) == ANNOTATION_DIRECTIVE)) ) {
						alt4=1;
					}

				}

				switch (alt4) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:481:8: {...}? annotation
					{
					if ( !((input.LA(1) == ANNOTATION_DIRECTIVE)) ) {
						throw new FailedPredicateException(input, "field", "input.LA(1) == ANNOTATION_DIRECTIVE");
					}
					pushFollow(FOLLOW_annotation_in_field1358);
					annotation26=annotation();
					state._fsp--;

					stream_annotation.add(annotation26.getTree());
					annotations.add((annotation26!=null?((CommonTree)annotation26.getTree()):null));
					}
					break;

				default :
					break loop4;
				}
			}

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:482:7: ( END_FIELD_DIRECTIVE -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) ) )
			int alt5=2;
			int LA5_0 = input.LA(1);
			if ( (LA5_0==END_FIELD_DIRECTIVE) ) {
				alt5=1;
			}
			else if ( (LA5_0==EOF||LA5_0==ANNOTATION_DIRECTIVE||LA5_0==CLASS_DIRECTIVE||LA5_0==FIELD_DIRECTIVE||LA5_0==IMPLEMENTS_DIRECTIVE||LA5_0==METHOD_DIRECTIVE||LA5_0==SOURCE_DIRECTIVE||LA5_0==SUPER_DIRECTIVE) ) {
				alt5=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 5, 0, input);
				throw nvae;
			}

			switch (alt5) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:482:9: END_FIELD_DIRECTIVE
					{
					END_FIELD_DIRECTIVE27=(Token)match(input,END_FIELD_DIRECTIVE,FOLLOW_END_FIELD_DIRECTIVE_in_field1372);
					stream_END_FIELD_DIRECTIVE.add(END_FIELD_DIRECTIVE27);

					// AST REWRITE
					// elements: literal, member_name, access_list, annotation, nonvoid_type_descriptor
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 483:9: -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:12: ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ( annotation )* ) )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD, (retval.start), "I_FIELD"), root_1);
						adaptor.addChild(root_1, stream_member_name.nextTree());
						adaptor.addChild(root_1, stream_access_list.nextTree());
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:65: ^( I_FIELD_TYPE nonvoid_type_descriptor )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD_TYPE, "I_FIELD_TYPE"), root_2);
						adaptor.addChild(root_2, stream_nonvoid_type_descriptor.nextTree());
						adaptor.addChild(root_1, root_2);
						}

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:105: ( ^( I_FIELD_INITIAL_VALUE literal ) )?
						if ( stream_literal.hasNext() ) {
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:105: ^( I_FIELD_INITIAL_VALUE literal )
							{
							CommonTree root_2 = (CommonTree)adaptor.nil();
							root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD_INITIAL_VALUE, "I_FIELD_INITIAL_VALUE"), root_2);
							adaptor.addChild(root_2, stream_literal.nextTree());
							adaptor.addChild(root_1, root_2);
							}

						}
						stream_literal.reset();

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:139: ^( I_ANNOTATIONS ( annotation )* )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATIONS, "I_ANNOTATIONS"), root_2);
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:483:155: ( annotation )*
						while ( stream_annotation.hasNext() ) {
							adaptor.addChild(root_2, stream_annotation.nextTree());
						}
						stream_annotation.reset();

						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:484:21:
					{
					smali_file_stack.peek().classAnnotations.addAll(annotations);
					// AST REWRITE
					// elements: nonvoid_type_descriptor, member_name, literal, access_list
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 485:9: -> ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:485:12: ^( I_FIELD[$start, \"I_FIELD\"] member_name access_list ^( I_FIELD_TYPE nonvoid_type_descriptor ) ( ^( I_FIELD_INITIAL_VALUE literal ) )? ^( I_ANNOTATIONS ) )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD, (retval.start), "I_FIELD"), root_1);
						adaptor.addChild(root_1, stream_member_name.nextTree());
						adaptor.addChild(root_1, stream_access_list.nextTree());
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:485:65: ^( I_FIELD_TYPE nonvoid_type_descriptor )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD_TYPE, "I_FIELD_TYPE"), root_2);
						adaptor.addChild(root_2, stream_nonvoid_type_descriptor.nextTree());
						adaptor.addChild(root_1, root_2);
						}

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:485:105: ( ^( I_FIELD_INITIAL_VALUE literal ) )?
						if ( stream_literal.hasNext() ) {
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:485:105: ^( I_FIELD_INITIAL_VALUE literal )
							{
							CommonTree root_2 = (CommonTree)adaptor.nil();
							root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_FIELD_INITIAL_VALUE, "I_FIELD_INITIAL_VALUE"), root_2);
							adaptor.addChild(root_2, stream_literal.nextTree());
							adaptor.addChild(root_1, root_2);
							}

						}
						stream_literal.reset();

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:485:139: ^( I_ANNOTATIONS )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATIONS, "I_ANNOTATIONS"), root_2);
						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;

			}

			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "field"


	public static class method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:489:1: method : METHOD_DIRECTIVE access_list member_name method_prototype statements_and_directives END_METHOD_DIRECTIVE -> ^( I_METHOD[$start, \"I_METHOD\"] member_name method_prototype access_list statements_and_directives ) ;
	public final smaliParser.method_return method() throws RecognitionException {
		smaliParser.method_return retval = new smaliParser.method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token METHOD_DIRECTIVE28=null;
		Token END_METHOD_DIRECTIVE33=null;
		ParserRuleReturnScope access_list29 =null;
		ParserRuleReturnScope member_name30 =null;
		ParserRuleReturnScope method_prototype31 =null;
		ParserRuleReturnScope statements_and_directives32 =null;

		CommonTree METHOD_DIRECTIVE28_tree=null;
		CommonTree END_METHOD_DIRECTIVE33_tree=null;
		RewriteRuleTokenStream stream_END_METHOD_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_METHOD_DIRECTIVE");
		RewriteRuleTokenStream stream_METHOD_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token METHOD_DIRECTIVE");
		RewriteRuleSubtreeStream stream_access_list=new RewriteRuleSubtreeStream(adaptor,"rule access_list");
		RewriteRuleSubtreeStream stream_method_prototype=new RewriteRuleSubtreeStream(adaptor,"rule method_prototype");
		RewriteRuleSubtreeStream stream_statements_and_directives=new RewriteRuleSubtreeStream(adaptor,"rule statements_and_directives");
		RewriteRuleSubtreeStream stream_member_name=new RewriteRuleSubtreeStream(adaptor,"rule member_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:490:3: ( METHOD_DIRECTIVE access_list member_name method_prototype statements_and_directives END_METHOD_DIRECTIVE -> ^( I_METHOD[$start, \"I_METHOD\"] member_name method_prototype access_list statements_and_directives ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:490:5: METHOD_DIRECTIVE access_list member_name method_prototype statements_and_directives END_METHOD_DIRECTIVE
			{
			METHOD_DIRECTIVE28=(Token)match(input,METHOD_DIRECTIVE,FOLLOW_METHOD_DIRECTIVE_in_method1483);
			stream_METHOD_DIRECTIVE.add(METHOD_DIRECTIVE28);

			pushFollow(FOLLOW_access_list_in_method1485);
			access_list29=access_list();
			state._fsp--;

			stream_access_list.add(access_list29.getTree());
			pushFollow(FOLLOW_member_name_in_method1487);
			member_name30=member_name();
			state._fsp--;

			stream_member_name.add(member_name30.getTree());
			pushFollow(FOLLOW_method_prototype_in_method1489);
			method_prototype31=method_prototype();
			state._fsp--;

			stream_method_prototype.add(method_prototype31.getTree());
			pushFollow(FOLLOW_statements_and_directives_in_method1491);
			statements_and_directives32=statements_and_directives();
			state._fsp--;

			stream_statements_and_directives.add(statements_and_directives32.getTree());
			END_METHOD_DIRECTIVE33=(Token)match(input,END_METHOD_DIRECTIVE,FOLLOW_END_METHOD_DIRECTIVE_in_method1497);
			stream_END_METHOD_DIRECTIVE.add(END_METHOD_DIRECTIVE33);

			// AST REWRITE
			// elements: member_name, statements_and_directives, access_list, method_prototype
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 492:5: -> ^( I_METHOD[$start, \"I_METHOD\"] member_name method_prototype access_list statements_and_directives )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:492:8: ^( I_METHOD[$start, \"I_METHOD\"] member_name method_prototype access_list statements_and_directives )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_METHOD, (retval.start), "I_METHOD"), root_1);
				adaptor.addChild(root_1, stream_member_name.nextTree());
				adaptor.addChild(root_1, stream_method_prototype.nextTree());
				adaptor.addChild(root_1, stream_access_list.nextTree());
				adaptor.addChild(root_1, stream_statements_and_directives.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "method"


	protected static class statements_and_directives_scope {
		boolean hasRegistersDirective;
		List<CommonTree> methodAnnotations;
	}
	protected Stack<statements_and_directives_scope> statements_and_directives_stack = new Stack<statements_and_directives_scope>();

	public static class statements_and_directives_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "statements_and_directives"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:494:1: statements_and_directives : ( ordered_method_item | registers_directive | catch_directive | catchall_directive | parameter_directive | annotation )* -> ( registers_directive )? ^( I_ORDERED_METHOD_ITEMS ( ordered_method_item )* ) ^( I_CATCHES ( catch_directive )* ( catchall_directive )* ) ^( I_PARAMETERS ( parameter_directive )* ) ;
	public final smaliParser.statements_and_directives_return statements_and_directives() throws RecognitionException {
		statements_and_directives_stack.push(new statements_and_directives_scope());
		smaliParser.statements_and_directives_return retval = new smaliParser.statements_and_directives_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		ParserRuleReturnScope ordered_method_item34 =null;
		ParserRuleReturnScope registers_directive35 =null;
		ParserRuleReturnScope catch_directive36 =null;
		ParserRuleReturnScope catchall_directive37 =null;
		ParserRuleReturnScope parameter_directive38 =null;
		ParserRuleReturnScope annotation39 =null;

		RewriteRuleSubtreeStream stream_catchall_directive=new RewriteRuleSubtreeStream(adaptor,"rule catchall_directive");
		RewriteRuleSubtreeStream stream_annotation=new RewriteRuleSubtreeStream(adaptor,"rule annotation");
		RewriteRuleSubtreeStream stream_ordered_method_item=new RewriteRuleSubtreeStream(adaptor,"rule ordered_method_item");
		RewriteRuleSubtreeStream stream_catch_directive=new RewriteRuleSubtreeStream(adaptor,"rule catch_directive");
		RewriteRuleSubtreeStream stream_registers_directive=new RewriteRuleSubtreeStream(adaptor,"rule registers_directive");
		RewriteRuleSubtreeStream stream_parameter_directive=new RewriteRuleSubtreeStream(adaptor,"rule parameter_directive");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:500:3: ( ( ordered_method_item | registers_directive | catch_directive | catchall_directive | parameter_directive | annotation )* -> ( registers_directive )? ^( I_ORDERED_METHOD_ITEMS ( ordered_method_item )* ) ^( I_CATCHES ( catch_directive )* ( catchall_directive )* ) ^( I_PARAMETERS ( parameter_directive )* ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:500:5: ( ordered_method_item | registers_directive | catch_directive | catchall_directive | parameter_directive | annotation )*
			{

			      statements_and_directives_stack.peek().hasRegistersDirective = false;
			      statements_and_directives_stack.peek().methodAnnotations = new ArrayList<CommonTree>();
			
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:504:5: ( ordered_method_item | registers_directive | catch_directive | catchall_directive | parameter_directive | annotation )*
			loop6:
			while (true) {
				int alt6=7;
				switch ( input.LA(1) ) {
				case ARRAY_DATA_DIRECTIVE:
				case COLON:
				case END_LOCAL_DIRECTIVE:
				case EPILOGUE_DIRECTIVE:
				case INSTRUCTION_FORMAT10t:
				case INSTRUCTION_FORMAT10x:
				case INSTRUCTION_FORMAT10x_ODEX:
				case INSTRUCTION_FORMAT11n:
				case INSTRUCTION_FORMAT11x:
				case INSTRUCTION_FORMAT12x:
				case INSTRUCTION_FORMAT12x_OR_ID:
				case INSTRUCTION_FORMAT20bc:
				case INSTRUCTION_FORMAT20t:
				case INSTRUCTION_FORMAT21c_FIELD:
				case INSTRUCTION_FORMAT21c_FIELD_ODEX:
				case INSTRUCTION_FORMAT21c_STRING:
				case INSTRUCTION_FORMAT21c_TYPE:
				case INSTRUCTION_FORMAT21ih:
				case INSTRUCTION_FORMAT21lh:
				case INSTRUCTION_FORMAT21s:
				case INSTRUCTION_FORMAT21t:
				case INSTRUCTION_FORMAT22b:
				case INSTRUCTION_FORMAT22c_FIELD:
				case INSTRUCTION_FORMAT22c_FIELD_ODEX:
				case INSTRUCTION_FORMAT22c_TYPE:
				case INSTRUCTION_FORMAT22cs_FIELD:
				case INSTRUCTION_FORMAT22s:
				case INSTRUCTION_FORMAT22s_OR_ID:
				case INSTRUCTION_FORMAT22t:
				case INSTRUCTION_FORMAT22x:
				case INSTRUCTION_FORMAT23x:
				case INSTRUCTION_FORMAT30t:
				case INSTRUCTION_FORMAT31c:
				case INSTRUCTION_FORMAT31i:
				case INSTRUCTION_FORMAT31i_OR_ID:
				case INSTRUCTION_FORMAT31t:
				case INSTRUCTION_FORMAT32x:
				case INSTRUCTION_FORMAT35c_METHOD:
				case INSTRUCTION_FORMAT35c_METHOD_ODEX:
				case INSTRUCTION_FORMAT35c_TYPE:
				case INSTRUCTION_FORMAT35mi_METHOD:
				case INSTRUCTION_FORMAT35ms_METHOD:
				case INSTRUCTION_FORMAT3rc_METHOD:
				case INSTRUCTION_FORMAT3rc_METHOD_ODEX:
				case INSTRUCTION_FORMAT3rc_TYPE:
				case INSTRUCTION_FORMAT3rmi_METHOD:
				case INSTRUCTION_FORMAT3rms_METHOD:
				case INSTRUCTION_FORMAT51l:
				case LINE_DIRECTIVE:
				case LOCAL_DIRECTIVE:
				case PACKED_SWITCH_DIRECTIVE:
				case PROLOGUE_DIRECTIVE:
				case RESTART_LOCAL_DIRECTIVE:
				case SOURCE_DIRECTIVE:
				case SPARSE_SWITCH_DIRECTIVE:
					{
					alt6=1;
					}
					break;
				case LOCALS_DIRECTIVE:
				case REGISTERS_DIRECTIVE:
					{
					alt6=2;
					}
					break;
				case CATCH_DIRECTIVE:
					{
					alt6=3;
					}
					break;
				case CATCHALL_DIRECTIVE:
					{
					alt6=4;
					}
					break;
				case PARAMETER_DIRECTIVE:
					{
					alt6=5;
					}
					break;
				case ANNOTATION_DIRECTIVE:
					{
					alt6=6;
					}
					break;
				}
				switch (alt6) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:504:7: ordered_method_item
					{
					pushFollow(FOLLOW_ordered_method_item_in_statements_and_directives1542);
					ordered_method_item34=ordered_method_item();
					state._fsp--;

					stream_ordered_method_item.add(ordered_method_item34.getTree());
					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:505:7: registers_directive
					{
					pushFollow(FOLLOW_registers_directive_in_statements_and_directives1550);
					registers_directive35=registers_directive();
					state._fsp--;

					stream_registers_directive.add(registers_directive35.getTree());
					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:506:7: catch_directive
					{
					pushFollow(FOLLOW_catch_directive_in_statements_and_directives1558);
					catch_directive36=catch_directive();
					state._fsp--;

					stream_catch_directive.add(catch_directive36.getTree());
					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:507:7: catchall_directive
					{
					pushFollow(FOLLOW_catchall_directive_in_statements_and_directives1566);
					catchall_directive37=catchall_directive();
					state._fsp--;

					stream_catchall_directive.add(catchall_directive37.getTree());
					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:508:7: parameter_directive
					{
					pushFollow(FOLLOW_parameter_directive_in_statements_and_directives1574);
					parameter_directive38=parameter_directive();
					state._fsp--;

					stream_parameter_directive.add(parameter_directive38.getTree());
					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:509:7: annotation
					{
					pushFollow(FOLLOW_annotation_in_statements_and_directives1582);
					annotation39=annotation();
					state._fsp--;

					stream_annotation.add(annotation39.getTree());
					statements_and_directives_stack.peek().methodAnnotations.add((annotation39!=null?((CommonTree)annotation39.getTree()):null));
					}
					break;

				default :
					break loop6;
				}
			}

			// AST REWRITE
			// elements: catchall_directive, registers_directive, parameter_directive, catch_directive, ordered_method_item
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 511:5: -> ( registers_directive )? ^( I_ORDERED_METHOD_ITEMS ( ordered_method_item )* ) ^( I_CATCHES ( catch_directive )* ( catchall_directive )* ) ^( I_PARAMETERS ( parameter_directive )* )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:511:8: ( registers_directive )?
				if ( stream_registers_directive.hasNext() ) {
					adaptor.addChild(root_0, stream_registers_directive.nextTree());
				}
				stream_registers_directive.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:512:8: ^( I_ORDERED_METHOD_ITEMS ( ordered_method_item )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ORDERED_METHOD_ITEMS, "I_ORDERED_METHOD_ITEMS"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:512:33: ( ordered_method_item )*
				while ( stream_ordered_method_item.hasNext() ) {
					adaptor.addChild(root_1, stream_ordered_method_item.nextTree());
				}
				stream_ordered_method_item.reset();

				adaptor.addChild(root_0, root_1);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:513:8: ^( I_CATCHES ( catch_directive )* ( catchall_directive )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_CATCHES, "I_CATCHES"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:513:20: ( catch_directive )*
				while ( stream_catch_directive.hasNext() ) {
					adaptor.addChild(root_1, stream_catch_directive.nextTree());
				}
				stream_catch_directive.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:513:37: ( catchall_directive )*
				while ( stream_catchall_directive.hasNext() ) {
					adaptor.addChild(root_1, stream_catchall_directive.nextTree());
				}
				stream_catchall_directive.reset();

				adaptor.addChild(root_0, root_1);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:514:8: ^( I_PARAMETERS ( parameter_directive )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PARAMETERS, "I_PARAMETERS"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:514:23: ( parameter_directive )*
				while ( stream_parameter_directive.hasNext() ) {
					adaptor.addChild(root_1, stream_parameter_directive.nextTree());
				}
				stream_parameter_directive.reset();

				adaptor.addChild(root_0, root_1);
				}

				adaptor.addChild(root_0, buildTree(I_ANNOTATIONS, "I_ANNOTATIONS", statements_and_directives_stack.peek().methodAnnotations));
			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
			statements_and_directives_stack.pop();
		}
		return retval;
	}
	// $ANTLR end "statements_and_directives"


	public static class ordered_method_item_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "ordered_method_item"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:518:1: ordered_method_item : ( label | instruction | debug_directive );
	public final smaliParser.ordered_method_item_return ordered_method_item() throws RecognitionException {
		smaliParser.ordered_method_item_return retval = new smaliParser.ordered_method_item_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		ParserRuleReturnScope label40 =null;
		ParserRuleReturnScope instruction41 =null;
		ParserRuleReturnScope debug_directive42 =null;


		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:519:3: ( label | instruction | debug_directive )
			int alt7=3;
			switch ( input.LA(1) ) {
			case COLON:
				{
				alt7=1;
				}
				break;
			case ARRAY_DATA_DIRECTIVE:
			case INSTRUCTION_FORMAT10t:
			case INSTRUCTION_FORMAT10x:
			case INSTRUCTION_FORMAT10x_ODEX:
			case INSTRUCTION_FORMAT11n:
			case INSTRUCTION_FORMAT11x:
			case INSTRUCTION_FORMAT12x:
			case INSTRUCTION_FORMAT12x_OR_ID:
			case INSTRUCTION_FORMAT20bc:
			case INSTRUCTION_FORMAT20t:
			case INSTRUCTION_FORMAT21c_FIELD:
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
			case INSTRUCTION_FORMAT21c_STRING:
			case INSTRUCTION_FORMAT21c_TYPE:
			case INSTRUCTION_FORMAT21ih:
			case INSTRUCTION_FORMAT21lh:
			case INSTRUCTION_FORMAT21s:
			case INSTRUCTION_FORMAT21t:
			case INSTRUCTION_FORMAT22b:
			case INSTRUCTION_FORMAT22c_FIELD:
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
			case INSTRUCTION_FORMAT22c_TYPE:
			case INSTRUCTION_FORMAT22cs_FIELD:
			case INSTRUCTION_FORMAT22s:
			case INSTRUCTION_FORMAT22s_OR_ID:
			case INSTRUCTION_FORMAT22t:
			case INSTRUCTION_FORMAT22x:
			case INSTRUCTION_FORMAT23x:
			case INSTRUCTION_FORMAT30t:
			case INSTRUCTION_FORMAT31c:
			case INSTRUCTION_FORMAT31i:
			case INSTRUCTION_FORMAT31i_OR_ID:
			case INSTRUCTION_FORMAT31t:
			case INSTRUCTION_FORMAT32x:
			case INSTRUCTION_FORMAT35c_METHOD:
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
			case INSTRUCTION_FORMAT35c_TYPE:
			case INSTRUCTION_FORMAT35mi_METHOD:
			case INSTRUCTION_FORMAT35ms_METHOD:
			case INSTRUCTION_FORMAT3rc_METHOD:
			case INSTRUCTION_FORMAT3rc_METHOD_ODEX:
			case INSTRUCTION_FORMAT3rc_TYPE:
			case INSTRUCTION_FORMAT3rmi_METHOD:
			case INSTRUCTION_FORMAT3rms_METHOD:
			case INSTRUCTION_FORMAT51l:
			case PACKED_SWITCH_DIRECTIVE:
			case SPARSE_SWITCH_DIRECTIVE:
				{
				alt7=2;
				}
				break;
			case END_LOCAL_DIRECTIVE:
			case EPILOGUE_DIRECTIVE:
			case LINE_DIRECTIVE:
			case LOCAL_DIRECTIVE:
			case PROLOGUE_DIRECTIVE:
			case RESTART_LOCAL_DIRECTIVE:
			case SOURCE_DIRECTIVE:
				{
				alt7=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 7, 0, input);
				throw nvae;
			}
			switch (alt7) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:519:5: label
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_label_in_ordered_method_item1667);
					label40=label();
					state._fsp--;

					adaptor.addChild(root_0, label40.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:520:5: instruction
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_instruction_in_ordered_method_item1673);
					instruction41=instruction();
					state._fsp--;

					adaptor.addChild(root_0, instruction41.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:521:5: debug_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_debug_directive_in_ordered_method_item1679);
					debug_directive42=debug_directive();
					state._fsp--;

					adaptor.addChild(root_0, debug_directive42.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "ordered_method_item"


	public static class registers_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "registers_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:523:1: registers_directive : (directive= REGISTERS_DIRECTIVE regCount= integral_literal -> ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount) |directive= LOCALS_DIRECTIVE regCount2= integral_literal -> ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2) ) ;
	public final smaliParser.registers_directive_return registers_directive() throws RecognitionException {
		smaliParser.registers_directive_return retval = new smaliParser.registers_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token directive=null;
		ParserRuleReturnScope regCount =null;
		ParserRuleReturnScope regCount2 =null;

		CommonTree directive_tree=null;
		RewriteRuleTokenStream stream_REGISTERS_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token REGISTERS_DIRECTIVE");
		RewriteRuleTokenStream stream_LOCALS_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token LOCALS_DIRECTIVE");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:524:3: ( (directive= REGISTERS_DIRECTIVE regCount= integral_literal -> ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount) |directive= LOCALS_DIRECTIVE regCount2= integral_literal -> ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:524:5: (directive= REGISTERS_DIRECTIVE regCount= integral_literal -> ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount) |directive= LOCALS_DIRECTIVE regCount2= integral_literal -> ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2) )
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:524:5: (directive= REGISTERS_DIRECTIVE regCount= integral_literal -> ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount) |directive= LOCALS_DIRECTIVE regCount2= integral_literal -> ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2) )
			int alt8=2;
			int LA8_0 = input.LA(1);
			if ( (LA8_0==REGISTERS_DIRECTIVE) ) {
				alt8=1;
			}
			else if ( (LA8_0==LOCALS_DIRECTIVE) ) {
				alt8=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 8, 0, input);
				throw nvae;
			}

			switch (alt8) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:525:7: directive= REGISTERS_DIRECTIVE regCount= integral_literal
					{
					directive=(Token)match(input,REGISTERS_DIRECTIVE,FOLLOW_REGISTERS_DIRECTIVE_in_registers_directive1699);
					stream_REGISTERS_DIRECTIVE.add(directive);

					pushFollow(FOLLOW_integral_literal_in_registers_directive1703);
					regCount=integral_literal();
					state._fsp--;

					stream_integral_literal.add(regCount.getTree());
					// AST REWRITE
					// elements: regCount
					// token labels:
					// rule labels: retval, regCount
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
					RewriteRuleSubtreeStream stream_regCount=new RewriteRuleSubtreeStream(adaptor,"rule regCount",regCount!=null?regCount.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 525:63: -> ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount)
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:525:66: ^( I_REGISTERS[$REGISTERS_DIRECTIVE, \"I_REGISTERS\"] $regCount)
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_REGISTERS, directive, "I_REGISTERS"), root_1);
						adaptor.addChild(root_1, stream_regCount.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:526:7: directive= LOCALS_DIRECTIVE regCount2= integral_literal
					{
					directive=(Token)match(input,LOCALS_DIRECTIVE,FOLLOW_LOCALS_DIRECTIVE_in_registers_directive1723);
					stream_LOCALS_DIRECTIVE.add(directive);

					pushFollow(FOLLOW_integral_literal_in_registers_directive1727);
					regCount2=integral_literal();
					state._fsp--;

					stream_integral_literal.add(regCount2.getTree());
					// AST REWRITE
					// elements: regCount2
					// token labels:
					// rule labels: retval, regCount2
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
					RewriteRuleSubtreeStream stream_regCount2=new RewriteRuleSubtreeStream(adaptor,"rule regCount2",regCount2!=null?regCount2.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 526:61: -> ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2)
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:526:64: ^( I_LOCALS[$LOCALS_DIRECTIVE, \"I_LOCALS\"] $regCount2)
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_LOCALS, directive, "I_LOCALS"), root_1);
						adaptor.addChild(root_1, stream_regCount2.nextTree());
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;

			}


			      if (statements_and_directives_stack.peek().hasRegistersDirective) {
			        throw new SemanticException(input, directive, "There can only be a single .registers or .locals directive in a method");
			      }
			      statements_and_directives_stack.peek().hasRegistersDirective =true;
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "registers_directive"


	public static class param_list_or_id_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "param_list_or_id"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:535:1: param_list_or_id : PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )+ PARAM_LIST_OR_ID_END ;
	public final smaliParser.param_list_or_id_return param_list_or_id() throws RecognitionException {
		smaliParser.param_list_or_id_return retval = new smaliParser.param_list_or_id_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token PARAM_LIST_OR_ID_START43=null;
		Token PRIMITIVE_TYPE44=null;
		Token PARAM_LIST_OR_ID_END45=null;

		CommonTree PARAM_LIST_OR_ID_START43_tree=null;
		CommonTree PRIMITIVE_TYPE44_tree=null;
		CommonTree PARAM_LIST_OR_ID_END45_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:536:3: ( PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )+ PARAM_LIST_OR_ID_END )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:536:5: PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )+ PARAM_LIST_OR_ID_END
			{
			root_0 = (CommonTree)adaptor.nil();


			PARAM_LIST_OR_ID_START43=(Token)match(input,PARAM_LIST_OR_ID_START,FOLLOW_PARAM_LIST_OR_ID_START_in_param_list_or_id1759);
			PARAM_LIST_OR_ID_START43_tree = (CommonTree)adaptor.create(PARAM_LIST_OR_ID_START43);
			adaptor.addChild(root_0, PARAM_LIST_OR_ID_START43_tree);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:536:28: ( PRIMITIVE_TYPE )+
			int cnt9=0;
			loop9:
			while (true) {
				int alt9=2;
				int LA9_0 = input.LA(1);
				if ( (LA9_0==PRIMITIVE_TYPE) ) {
					alt9=1;
				}

				switch (alt9) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:536:28: PRIMITIVE_TYPE
					{
					PRIMITIVE_TYPE44=(Token)match(input,PRIMITIVE_TYPE,FOLLOW_PRIMITIVE_TYPE_in_param_list_or_id1761);
					PRIMITIVE_TYPE44_tree = (CommonTree)adaptor.create(PRIMITIVE_TYPE44);
					adaptor.addChild(root_0, PRIMITIVE_TYPE44_tree);

					}
					break;

				default :
					if ( cnt9 >= 1 ) break loop9;
					EarlyExitException eee = new EarlyExitException(9, input);
					throw eee;
				}
				cnt9++;
			}

			PARAM_LIST_OR_ID_END45=(Token)match(input,PARAM_LIST_OR_ID_END,FOLLOW_PARAM_LIST_OR_ID_END_in_param_list_or_id1764);
			PARAM_LIST_OR_ID_END45_tree = (CommonTree)adaptor.create(PARAM_LIST_OR_ID_END45);
			adaptor.addChild(root_0, PARAM_LIST_OR_ID_END45_tree);

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "param_list_or_id"


	public static class simple_name_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "simple_name"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:540:1: simple_name : ( SIMPLE_NAME | ACCESS_SPEC -> SIMPLE_NAME[$ACCESS_SPEC] | VERIFICATION_ERROR_TYPE -> SIMPLE_NAME[$VERIFICATION_ERROR_TYPE] | POSITIVE_INTEGER_LITERAL -> SIMPLE_NAME[$POSITIVE_INTEGER_LITERAL] | NEGATIVE_INTEGER_LITERAL -> SIMPLE_NAME[$NEGATIVE_INTEGER_LITERAL] | FLOAT_LITERAL_OR_ID -> SIMPLE_NAME[$FLOAT_LITERAL_OR_ID] | DOUBLE_LITERAL_OR_ID -> SIMPLE_NAME[$DOUBLE_LITERAL_OR_ID] | BOOL_LITERAL -> SIMPLE_NAME[$BOOL_LITERAL] | NULL_LITERAL -> SIMPLE_NAME[$NULL_LITERAL] | REGISTER -> SIMPLE_NAME[$REGISTER] | param_list_or_id ->| PRIMITIVE_TYPE -> SIMPLE_NAME[$PRIMITIVE_TYPE] | VOID_TYPE -> SIMPLE_NAME[$VOID_TYPE] | ANNOTATION_VISIBILITY -> SIMPLE_NAME[$ANNOTATION_VISIBILITY] | INSTRUCTION_FORMAT10t -> SIMPLE_NAME[$INSTRUCTION_FORMAT10t] | INSTRUCTION_FORMAT10x -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x] | INSTRUCTION_FORMAT10x_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x_ODEX] | INSTRUCTION_FORMAT11x -> SIMPLE_NAME[$INSTRUCTION_FORMAT11x] | INSTRUCTION_FORMAT12x_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT12x_OR_ID] | INSTRUCTION_FORMAT21c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD] | INSTRUCTION_FORMAT21c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD_ODEX] | INSTRUCTION_FORMAT21c_STRING -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_STRING] | INSTRUCTION_FORMAT21c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_TYPE] | INSTRUCTION_FORMAT21t -> SIMPLE_NAME[$INSTRUCTION_FORMAT21t] | INSTRUCTION_FORMAT22c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD] | INSTRUCTION_FORMAT22c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD_ODEX] | INSTRUCTION_FORMAT22c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_TYPE] | INSTRUCTION_FORMAT22cs_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22cs_FIELD] | INSTRUCTION_FORMAT22s_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT22s_OR_ID] | INSTRUCTION_FORMAT22t -> SIMPLE_NAME[$INSTRUCTION_FORMAT22t] | INSTRUCTION_FORMAT23x -> SIMPLE_NAME[$INSTRUCTION_FORMAT23x] | INSTRUCTION_FORMAT31i_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT31i_OR_ID] | INSTRUCTION_FORMAT31t -> SIMPLE_NAME[$INSTRUCTION_FORMAT31t] | INSTRUCTION_FORMAT35c_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD] | INSTRUCTION_FORMAT35c_METHOD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD_ODEX] | INSTRUCTION_FORMAT35c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_TYPE] | INSTRUCTION_FORMAT35mi_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35mi_METHOD] | INSTRUCTION_FORMAT35ms_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35ms_METHOD] | INSTRUCTION_FORMAT51l -> SIMPLE_NAME[$INSTRUCTION_FORMAT51l] );
	public final smaliParser.simple_name_return simple_name() throws RecognitionException {
		smaliParser.simple_name_return retval = new smaliParser.simple_name_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SIMPLE_NAME46=null;
		Token ACCESS_SPEC47=null;
		Token VERIFICATION_ERROR_TYPE48=null;
		Token POSITIVE_INTEGER_LITERAL49=null;
		Token NEGATIVE_INTEGER_LITERAL50=null;
		Token FLOAT_LITERAL_OR_ID51=null;
		Token DOUBLE_LITERAL_OR_ID52=null;
		Token BOOL_LITERAL53=null;
		Token NULL_LITERAL54=null;
		Token REGISTER55=null;
		Token PRIMITIVE_TYPE57=null;
		Token VOID_TYPE58=null;
		Token ANNOTATION_VISIBILITY59=null;
		Token INSTRUCTION_FORMAT10t60=null;
		Token INSTRUCTION_FORMAT10x61=null;
		Token INSTRUCTION_FORMAT10x_ODEX62=null;
		Token INSTRUCTION_FORMAT11x63=null;
		Token INSTRUCTION_FORMAT12x_OR_ID64=null;
		Token INSTRUCTION_FORMAT21c_FIELD65=null;
		Token INSTRUCTION_FORMAT21c_FIELD_ODEX66=null;
		Token INSTRUCTION_FORMAT21c_STRING67=null;
		Token INSTRUCTION_FORMAT21c_TYPE68=null;
		Token INSTRUCTION_FORMAT21t69=null;
		Token INSTRUCTION_FORMAT22c_FIELD70=null;
		Token INSTRUCTION_FORMAT22c_FIELD_ODEX71=null;
		Token INSTRUCTION_FORMAT22c_TYPE72=null;
		Token INSTRUCTION_FORMAT22cs_FIELD73=null;
		Token INSTRUCTION_FORMAT22s_OR_ID74=null;
		Token INSTRUCTION_FORMAT22t75=null;
		Token INSTRUCTION_FORMAT23x76=null;
		Token INSTRUCTION_FORMAT31i_OR_ID77=null;
		Token INSTRUCTION_FORMAT31t78=null;
		Token INSTRUCTION_FORMAT35c_METHOD79=null;
		Token INSTRUCTION_FORMAT35c_METHOD_ODEX80=null;
		Token INSTRUCTION_FORMAT35c_TYPE81=null;
		Token INSTRUCTION_FORMAT35mi_METHOD82=null;
		Token INSTRUCTION_FORMAT35ms_METHOD83=null;
		Token INSTRUCTION_FORMAT51l84=null;
		ParserRuleReturnScope param_list_or_id56 =null;

		CommonTree SIMPLE_NAME46_tree=null;
		CommonTree ACCESS_SPEC47_tree=null;
		CommonTree VERIFICATION_ERROR_TYPE48_tree=null;
		CommonTree POSITIVE_INTEGER_LITERAL49_tree=null;
		CommonTree NEGATIVE_INTEGER_LITERAL50_tree=null;
		CommonTree FLOAT_LITERAL_OR_ID51_tree=null;
		CommonTree DOUBLE_LITERAL_OR_ID52_tree=null;
		CommonTree BOOL_LITERAL53_tree=null;
		CommonTree NULL_LITERAL54_tree=null;
		CommonTree REGISTER55_tree=null;
		CommonTree PRIMITIVE_TYPE57_tree=null;
		CommonTree VOID_TYPE58_tree=null;
		CommonTree ANNOTATION_VISIBILITY59_tree=null;
		CommonTree INSTRUCTION_FORMAT10t60_tree=null;
		CommonTree INSTRUCTION_FORMAT10x61_tree=null;
		CommonTree INSTRUCTION_FORMAT10x_ODEX62_tree=null;
		CommonTree INSTRUCTION_FORMAT11x63_tree=null;
		CommonTree INSTRUCTION_FORMAT12x_OR_ID64_tree=null;
		CommonTree INSTRUCTION_FORMAT21c_FIELD65_tree=null;
		CommonTree INSTRUCTION_FORMAT21c_FIELD_ODEX66_tree=null;
		CommonTree INSTRUCTION_FORMAT21c_STRING67_tree=null;
		CommonTree INSTRUCTION_FORMAT21c_TYPE68_tree=null;
		CommonTree INSTRUCTION_FORMAT21t69_tree=null;
		CommonTree INSTRUCTION_FORMAT22c_FIELD70_tree=null;
		CommonTree INSTRUCTION_FORMAT22c_FIELD_ODEX71_tree=null;
		CommonTree INSTRUCTION_FORMAT22c_TYPE72_tree=null;
		CommonTree INSTRUCTION_FORMAT22cs_FIELD73_tree=null;
		CommonTree INSTRUCTION_FORMAT22s_OR_ID74_tree=null;
		CommonTree INSTRUCTION_FORMAT22t75_tree=null;
		CommonTree INSTRUCTION_FORMAT23x76_tree=null;
		CommonTree INSTRUCTION_FORMAT31i_OR_ID77_tree=null;
		CommonTree INSTRUCTION_FORMAT31t78_tree=null;
		CommonTree INSTRUCTION_FORMAT35c_METHOD79_tree=null;
		CommonTree INSTRUCTION_FORMAT35c_METHOD_ODEX80_tree=null;
		CommonTree INSTRUCTION_FORMAT35c_TYPE81_tree=null;
		CommonTree INSTRUCTION_FORMAT35mi_METHOD82_tree=null;
		CommonTree INSTRUCTION_FORMAT35ms_METHOD83_tree=null;
		CommonTree INSTRUCTION_FORMAT51l84_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_TYPE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35c_METHOD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35c_METHOD");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT11x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT11x");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21t");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35c_TYPE");
		RewriteRuleTokenStream stream_ANNOTATION_VISIBILITY=new RewriteRuleTokenStream(adaptor,"token ANNOTATION_VISIBILITY");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT31i_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT31i_OR_ID");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22s_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22s_OR_ID");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT51l=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT51l");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT23x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT23x");
		RewriteRuleTokenStream stream_NULL_LITERAL=new RewriteRuleTokenStream(adaptor,"token NULL_LITERAL");
		RewriteRuleTokenStream stream_BOOL_LITERAL=new RewriteRuleTokenStream(adaptor,"token BOOL_LITERAL");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_FIELD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_FIELD");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35c_METHOD_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35c_METHOD_ODEX");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_FIELD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_FIELD");
		RewriteRuleTokenStream stream_ACCESS_SPEC=new RewriteRuleTokenStream(adaptor,"token ACCESS_SPEC");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_STRING=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_STRING");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT12x_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT12x_OR_ID");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35ms_METHOD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35ms_METHOD");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35mi_METHOD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35mi_METHOD");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22cs_FIELD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22cs_FIELD");
		RewriteRuleTokenStream stream_VOID_TYPE=new RewriteRuleTokenStream(adaptor,"token VOID_TYPE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT10x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT10x");
		RewriteRuleTokenStream stream_FLOAT_LITERAL_OR_ID=new RewriteRuleTokenStream(adaptor,"token FLOAT_LITERAL_OR_ID");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22t");
		RewriteRuleTokenStream stream_PRIMITIVE_TYPE=new RewriteRuleTokenStream(adaptor,"token PRIMITIVE_TYPE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT10x_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT10x_ODEX");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT31t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT31t");
		RewriteRuleTokenStream stream_DOUBLE_LITERAL_OR_ID=new RewriteRuleTokenStream(adaptor,"token DOUBLE_LITERAL_OR_ID");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_FIELD_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_FIELD_ODEX");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT10t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT10t");
		RewriteRuleTokenStream stream_NEGATIVE_INTEGER_LITERAL=new RewriteRuleTokenStream(adaptor,"token NEGATIVE_INTEGER_LITERAL");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_VERIFICATION_ERROR_TYPE=new RewriteRuleTokenStream(adaptor,"token VERIFICATION_ERROR_TYPE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_TYPE");
		RewriteRuleTokenStream stream_POSITIVE_INTEGER_LITERAL=new RewriteRuleTokenStream(adaptor,"token POSITIVE_INTEGER_LITERAL");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_FIELD_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_FIELD_ODEX");
		RewriteRuleSubtreeStream stream_param_list_or_id=new RewriteRuleSubtreeStream(adaptor,"rule param_list_or_id");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:541:3: ( SIMPLE_NAME | ACCESS_SPEC -> SIMPLE_NAME[$ACCESS_SPEC] | VERIFICATION_ERROR_TYPE -> SIMPLE_NAME[$VERIFICATION_ERROR_TYPE] | POSITIVE_INTEGER_LITERAL -> SIMPLE_NAME[$POSITIVE_INTEGER_LITERAL] | NEGATIVE_INTEGER_LITERAL -> SIMPLE_NAME[$NEGATIVE_INTEGER_LITERAL] | FLOAT_LITERAL_OR_ID -> SIMPLE_NAME[$FLOAT_LITERAL_OR_ID] | DOUBLE_LITERAL_OR_ID -> SIMPLE_NAME[$DOUBLE_LITERAL_OR_ID] | BOOL_LITERAL -> SIMPLE_NAME[$BOOL_LITERAL] | NULL_LITERAL -> SIMPLE_NAME[$NULL_LITERAL] | REGISTER -> SIMPLE_NAME[$REGISTER] | param_list_or_id ->| PRIMITIVE_TYPE -> SIMPLE_NAME[$PRIMITIVE_TYPE] | VOID_TYPE -> SIMPLE_NAME[$VOID_TYPE] | ANNOTATION_VISIBILITY -> SIMPLE_NAME[$ANNOTATION_VISIBILITY] | INSTRUCTION_FORMAT10t -> SIMPLE_NAME[$INSTRUCTION_FORMAT10t] | INSTRUCTION_FORMAT10x -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x] | INSTRUCTION_FORMAT10x_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x_ODEX] | INSTRUCTION_FORMAT11x -> SIMPLE_NAME[$INSTRUCTION_FORMAT11x] | INSTRUCTION_FORMAT12x_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT12x_OR_ID] | INSTRUCTION_FORMAT21c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD] | INSTRUCTION_FORMAT21c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD_ODEX] | INSTRUCTION_FORMAT21c_STRING -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_STRING] | INSTRUCTION_FORMAT21c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_TYPE] | INSTRUCTION_FORMAT21t -> SIMPLE_NAME[$INSTRUCTION_FORMAT21t] | INSTRUCTION_FORMAT22c_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD] | INSTRUCTION_FORMAT22c_FIELD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD_ODEX] | INSTRUCTION_FORMAT22c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_TYPE] | INSTRUCTION_FORMAT22cs_FIELD -> SIMPLE_NAME[$INSTRUCTION_FORMAT22cs_FIELD] | INSTRUCTION_FORMAT22s_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT22s_OR_ID] | INSTRUCTION_FORMAT22t -> SIMPLE_NAME[$INSTRUCTION_FORMAT22t] | INSTRUCTION_FORMAT23x -> SIMPLE_NAME[$INSTRUCTION_FORMAT23x] | INSTRUCTION_FORMAT31i_OR_ID -> SIMPLE_NAME[$INSTRUCTION_FORMAT31i_OR_ID] | INSTRUCTION_FORMAT31t -> SIMPLE_NAME[$INSTRUCTION_FORMAT31t] | INSTRUCTION_FORMAT35c_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD] | INSTRUCTION_FORMAT35c_METHOD_ODEX -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD_ODEX] | INSTRUCTION_FORMAT35c_TYPE -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_TYPE] | INSTRUCTION_FORMAT35mi_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35mi_METHOD] | INSTRUCTION_FORMAT35ms_METHOD -> SIMPLE_NAME[$INSTRUCTION_FORMAT35ms_METHOD] | INSTRUCTION_FORMAT51l -> SIMPLE_NAME[$INSTRUCTION_FORMAT51l] )
			int alt10=39;
			switch ( input.LA(1) ) {
			case SIMPLE_NAME:
				{
				alt10=1;
				}
				break;
			case ACCESS_SPEC:
				{
				alt10=2;
				}
				break;
			case VERIFICATION_ERROR_TYPE:
				{
				alt10=3;
				}
				break;
			case POSITIVE_INTEGER_LITERAL:
				{
				alt10=4;
				}
				break;
			case NEGATIVE_INTEGER_LITERAL:
				{
				alt10=5;
				}
				break;
			case FLOAT_LITERAL_OR_ID:
				{
				alt10=6;
				}
				break;
			case DOUBLE_LITERAL_OR_ID:
				{
				alt10=7;
				}
				break;
			case BOOL_LITERAL:
				{
				alt10=8;
				}
				break;
			case NULL_LITERAL:
				{
				alt10=9;
				}
				break;
			case REGISTER:
				{
				alt10=10;
				}
				break;
			case PARAM_LIST_OR_ID_START:
				{
				alt10=11;
				}
				break;
			case PRIMITIVE_TYPE:
				{
				alt10=12;
				}
				break;
			case VOID_TYPE:
				{
				alt10=13;
				}
				break;
			case ANNOTATION_VISIBILITY:
				{
				alt10=14;
				}
				break;
			case INSTRUCTION_FORMAT10t:
				{
				alt10=15;
				}
				break;
			case INSTRUCTION_FORMAT10x:
				{
				alt10=16;
				}
				break;
			case INSTRUCTION_FORMAT10x_ODEX:
				{
				alt10=17;
				}
				break;
			case INSTRUCTION_FORMAT11x:
				{
				alt10=18;
				}
				break;
			case INSTRUCTION_FORMAT12x_OR_ID:
				{
				alt10=19;
				}
				break;
			case INSTRUCTION_FORMAT21c_FIELD:
				{
				alt10=20;
				}
				break;
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
				{
				alt10=21;
				}
				break;
			case INSTRUCTION_FORMAT21c_STRING:
				{
				alt10=22;
				}
				break;
			case INSTRUCTION_FORMAT21c_TYPE:
				{
				alt10=23;
				}
				break;
			case INSTRUCTION_FORMAT21t:
				{
				alt10=24;
				}
				break;
			case INSTRUCTION_FORMAT22c_FIELD:
				{
				alt10=25;
				}
				break;
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
				{
				alt10=26;
				}
				break;
			case INSTRUCTION_FORMAT22c_TYPE:
				{
				alt10=27;
				}
				break;
			case INSTRUCTION_FORMAT22cs_FIELD:
				{
				alt10=28;
				}
				break;
			case INSTRUCTION_FORMAT22s_OR_ID:
				{
				alt10=29;
				}
				break;
			case INSTRUCTION_FORMAT22t:
				{
				alt10=30;
				}
				break;
			case INSTRUCTION_FORMAT23x:
				{
				alt10=31;
				}
				break;
			case INSTRUCTION_FORMAT31i_OR_ID:
				{
				alt10=32;
				}
				break;
			case INSTRUCTION_FORMAT31t:
				{
				alt10=33;
				}
				break;
			case INSTRUCTION_FORMAT35c_METHOD:
				{
				alt10=34;
				}
				break;
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
				{
				alt10=35;
				}
				break;
			case INSTRUCTION_FORMAT35c_TYPE:
				{
				alt10=36;
				}
				break;
			case INSTRUCTION_FORMAT35mi_METHOD:
				{
				alt10=37;
				}
				break;
			case INSTRUCTION_FORMAT35ms_METHOD:
				{
				alt10=38;
				}
				break;
			case INSTRUCTION_FORMAT51l:
				{
				alt10=39;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 10, 0, input);
				throw nvae;
			}
			switch (alt10) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:541:5: SIMPLE_NAME
					{
					root_0 = (CommonTree)adaptor.nil();


					SIMPLE_NAME46=(Token)match(input,SIMPLE_NAME,FOLLOW_SIMPLE_NAME_in_simple_name1776);
					SIMPLE_NAME46_tree = (CommonTree)adaptor.create(SIMPLE_NAME46);
					adaptor.addChild(root_0, SIMPLE_NAME46_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:542:5: ACCESS_SPEC
					{
					ACCESS_SPEC47=(Token)match(input,ACCESS_SPEC,FOLLOW_ACCESS_SPEC_in_simple_name1782);
					stream_ACCESS_SPEC.add(ACCESS_SPEC47);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 542:17: -> SIMPLE_NAME[$ACCESS_SPEC]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, ACCESS_SPEC47));
					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:543:5: VERIFICATION_ERROR_TYPE
					{
					VERIFICATION_ERROR_TYPE48=(Token)match(input,VERIFICATION_ERROR_TYPE,FOLLOW_VERIFICATION_ERROR_TYPE_in_simple_name1793);
					stream_VERIFICATION_ERROR_TYPE.add(VERIFICATION_ERROR_TYPE48);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 543:29: -> SIMPLE_NAME[$VERIFICATION_ERROR_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, VERIFICATION_ERROR_TYPE48));
					}


					retval.tree = root_0;

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:544:5: POSITIVE_INTEGER_LITERAL
					{
					POSITIVE_INTEGER_LITERAL49=(Token)match(input,POSITIVE_INTEGER_LITERAL,FOLLOW_POSITIVE_INTEGER_LITERAL_in_simple_name1804);
					stream_POSITIVE_INTEGER_LITERAL.add(POSITIVE_INTEGER_LITERAL49);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 544:30: -> SIMPLE_NAME[$POSITIVE_INTEGER_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, POSITIVE_INTEGER_LITERAL49));
					}


					retval.tree = root_0;

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:545:5: NEGATIVE_INTEGER_LITERAL
					{
					NEGATIVE_INTEGER_LITERAL50=(Token)match(input,NEGATIVE_INTEGER_LITERAL,FOLLOW_NEGATIVE_INTEGER_LITERAL_in_simple_name1815);
					stream_NEGATIVE_INTEGER_LITERAL.add(NEGATIVE_INTEGER_LITERAL50);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 545:30: -> SIMPLE_NAME[$NEGATIVE_INTEGER_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, NEGATIVE_INTEGER_LITERAL50));
					}


					retval.tree = root_0;

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:546:5: FLOAT_LITERAL_OR_ID
					{
					FLOAT_LITERAL_OR_ID51=(Token)match(input,FLOAT_LITERAL_OR_ID,FOLLOW_FLOAT_LITERAL_OR_ID_in_simple_name1826);
					stream_FLOAT_LITERAL_OR_ID.add(FLOAT_LITERAL_OR_ID51);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 546:25: -> SIMPLE_NAME[$FLOAT_LITERAL_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, FLOAT_LITERAL_OR_ID51));
					}


					retval.tree = root_0;

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:547:5: DOUBLE_LITERAL_OR_ID
					{
					DOUBLE_LITERAL_OR_ID52=(Token)match(input,DOUBLE_LITERAL_OR_ID,FOLLOW_DOUBLE_LITERAL_OR_ID_in_simple_name1837);
					stream_DOUBLE_LITERAL_OR_ID.add(DOUBLE_LITERAL_OR_ID52);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 547:26: -> SIMPLE_NAME[$DOUBLE_LITERAL_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, DOUBLE_LITERAL_OR_ID52));
					}


					retval.tree = root_0;

					}
					break;
				case 8 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:548:5: BOOL_LITERAL
					{
					BOOL_LITERAL53=(Token)match(input,BOOL_LITERAL,FOLLOW_BOOL_LITERAL_in_simple_name1848);
					stream_BOOL_LITERAL.add(BOOL_LITERAL53);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 548:18: -> SIMPLE_NAME[$BOOL_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, BOOL_LITERAL53));
					}


					retval.tree = root_0;

					}
					break;
				case 9 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:549:5: NULL_LITERAL
					{
					NULL_LITERAL54=(Token)match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_simple_name1859);
					stream_NULL_LITERAL.add(NULL_LITERAL54);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 549:18: -> SIMPLE_NAME[$NULL_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, NULL_LITERAL54));
					}


					retval.tree = root_0;

					}
					break;
				case 10 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:550:5: REGISTER
					{
					REGISTER55=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_simple_name1870);
					stream_REGISTER.add(REGISTER55);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 550:14: -> SIMPLE_NAME[$REGISTER]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, REGISTER55));
					}


					retval.tree = root_0;

					}
					break;
				case 11 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:551:5: param_list_or_id
					{
					pushFollow(FOLLOW_param_list_or_id_in_simple_name1881);
					param_list_or_id56=param_list_or_id();
					state._fsp--;

					stream_param_list_or_id.add(param_list_or_id56.getTree());
					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 551:22: ->
					{
						adaptor.addChild(root_0,  adaptor.create(SIMPLE_NAME, (param_list_or_id56!=null?input.toString(param_list_or_id56.start,param_list_or_id56.stop):null)) );
					}


					retval.tree = root_0;

					}
					break;
				case 12 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:552:5: PRIMITIVE_TYPE
					{
					PRIMITIVE_TYPE57=(Token)match(input,PRIMITIVE_TYPE,FOLLOW_PRIMITIVE_TYPE_in_simple_name1891);
					stream_PRIMITIVE_TYPE.add(PRIMITIVE_TYPE57);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 552:20: -> SIMPLE_NAME[$PRIMITIVE_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, PRIMITIVE_TYPE57));
					}


					retval.tree = root_0;

					}
					break;
				case 13 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:553:5: VOID_TYPE
					{
					VOID_TYPE58=(Token)match(input,VOID_TYPE,FOLLOW_VOID_TYPE_in_simple_name1902);
					stream_VOID_TYPE.add(VOID_TYPE58);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 553:15: -> SIMPLE_NAME[$VOID_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, VOID_TYPE58));
					}


					retval.tree = root_0;

					}
					break;
				case 14 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:554:5: ANNOTATION_VISIBILITY
					{
					ANNOTATION_VISIBILITY59=(Token)match(input,ANNOTATION_VISIBILITY,FOLLOW_ANNOTATION_VISIBILITY_in_simple_name1913);
					stream_ANNOTATION_VISIBILITY.add(ANNOTATION_VISIBILITY59);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 554:27: -> SIMPLE_NAME[$ANNOTATION_VISIBILITY]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, ANNOTATION_VISIBILITY59));
					}


					retval.tree = root_0;

					}
					break;
				case 15 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:555:5: INSTRUCTION_FORMAT10t
					{
					INSTRUCTION_FORMAT10t60=(Token)match(input,INSTRUCTION_FORMAT10t,FOLLOW_INSTRUCTION_FORMAT10t_in_simple_name1924);
					stream_INSTRUCTION_FORMAT10t.add(INSTRUCTION_FORMAT10t60);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 555:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT10t]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT10t60));
					}


					retval.tree = root_0;

					}
					break;
				case 16 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:556:5: INSTRUCTION_FORMAT10x
					{
					INSTRUCTION_FORMAT10x61=(Token)match(input,INSTRUCTION_FORMAT10x,FOLLOW_INSTRUCTION_FORMAT10x_in_simple_name1935);
					stream_INSTRUCTION_FORMAT10x.add(INSTRUCTION_FORMAT10x61);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 556:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT10x61));
					}


					retval.tree = root_0;

					}
					break;
				case 17 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:557:5: INSTRUCTION_FORMAT10x_ODEX
					{
					INSTRUCTION_FORMAT10x_ODEX62=(Token)match(input,INSTRUCTION_FORMAT10x_ODEX,FOLLOW_INSTRUCTION_FORMAT10x_ODEX_in_simple_name1946);
					stream_INSTRUCTION_FORMAT10x_ODEX.add(INSTRUCTION_FORMAT10x_ODEX62);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 557:32: -> SIMPLE_NAME[$INSTRUCTION_FORMAT10x_ODEX]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT10x_ODEX62));
					}


					retval.tree = root_0;

					}
					break;
				case 18 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:558:5: INSTRUCTION_FORMAT11x
					{
					INSTRUCTION_FORMAT11x63=(Token)match(input,INSTRUCTION_FORMAT11x,FOLLOW_INSTRUCTION_FORMAT11x_in_simple_name1957);
					stream_INSTRUCTION_FORMAT11x.add(INSTRUCTION_FORMAT11x63);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 558:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT11x]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT11x63));
					}


					retval.tree = root_0;

					}
					break;
				case 19 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:559:5: INSTRUCTION_FORMAT12x_OR_ID
					{
					INSTRUCTION_FORMAT12x_OR_ID64=(Token)match(input,INSTRUCTION_FORMAT12x_OR_ID,FOLLOW_INSTRUCTION_FORMAT12x_OR_ID_in_simple_name1968);
					stream_INSTRUCTION_FORMAT12x_OR_ID.add(INSTRUCTION_FORMAT12x_OR_ID64);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 559:33: -> SIMPLE_NAME[$INSTRUCTION_FORMAT12x_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT12x_OR_ID64));
					}


					retval.tree = root_0;

					}
					break;
				case 20 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:560:5: INSTRUCTION_FORMAT21c_FIELD
					{
					INSTRUCTION_FORMAT21c_FIELD65=(Token)match(input,INSTRUCTION_FORMAT21c_FIELD,FOLLOW_INSTRUCTION_FORMAT21c_FIELD_in_simple_name1979);
					stream_INSTRUCTION_FORMAT21c_FIELD.add(INSTRUCTION_FORMAT21c_FIELD65);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 560:33: -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT21c_FIELD65));
					}


					retval.tree = root_0;

					}
					break;
				case 21 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:561:5: INSTRUCTION_FORMAT21c_FIELD_ODEX
					{
					INSTRUCTION_FORMAT21c_FIELD_ODEX66=(Token)match(input,INSTRUCTION_FORMAT21c_FIELD_ODEX,FOLLOW_INSTRUCTION_FORMAT21c_FIELD_ODEX_in_simple_name1990);
					stream_INSTRUCTION_FORMAT21c_FIELD_ODEX.add(INSTRUCTION_FORMAT21c_FIELD_ODEX66);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 561:38: -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_FIELD_ODEX]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT21c_FIELD_ODEX66));
					}


					retval.tree = root_0;

					}
					break;
				case 22 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:562:5: INSTRUCTION_FORMAT21c_STRING
					{
					INSTRUCTION_FORMAT21c_STRING67=(Token)match(input,INSTRUCTION_FORMAT21c_STRING,FOLLOW_INSTRUCTION_FORMAT21c_STRING_in_simple_name2001);
					stream_INSTRUCTION_FORMAT21c_STRING.add(INSTRUCTION_FORMAT21c_STRING67);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 562:34: -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_STRING]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT21c_STRING67));
					}


					retval.tree = root_0;

					}
					break;
				case 23 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:563:5: INSTRUCTION_FORMAT21c_TYPE
					{
					INSTRUCTION_FORMAT21c_TYPE68=(Token)match(input,INSTRUCTION_FORMAT21c_TYPE,FOLLOW_INSTRUCTION_FORMAT21c_TYPE_in_simple_name2012);
					stream_INSTRUCTION_FORMAT21c_TYPE.add(INSTRUCTION_FORMAT21c_TYPE68);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 563:32: -> SIMPLE_NAME[$INSTRUCTION_FORMAT21c_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT21c_TYPE68));
					}


					retval.tree = root_0;

					}
					break;
				case 24 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:564:5: INSTRUCTION_FORMAT21t
					{
					INSTRUCTION_FORMAT21t69=(Token)match(input,INSTRUCTION_FORMAT21t,FOLLOW_INSTRUCTION_FORMAT21t_in_simple_name2023);
					stream_INSTRUCTION_FORMAT21t.add(INSTRUCTION_FORMAT21t69);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 564:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT21t]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT21t69));
					}


					retval.tree = root_0;

					}
					break;
				case 25 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:565:5: INSTRUCTION_FORMAT22c_FIELD
					{
					INSTRUCTION_FORMAT22c_FIELD70=(Token)match(input,INSTRUCTION_FORMAT22c_FIELD,FOLLOW_INSTRUCTION_FORMAT22c_FIELD_in_simple_name2034);
					stream_INSTRUCTION_FORMAT22c_FIELD.add(INSTRUCTION_FORMAT22c_FIELD70);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 565:33: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22c_FIELD70));
					}


					retval.tree = root_0;

					}
					break;
				case 26 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:566:5: INSTRUCTION_FORMAT22c_FIELD_ODEX
					{
					INSTRUCTION_FORMAT22c_FIELD_ODEX71=(Token)match(input,INSTRUCTION_FORMAT22c_FIELD_ODEX,FOLLOW_INSTRUCTION_FORMAT22c_FIELD_ODEX_in_simple_name2045);
					stream_INSTRUCTION_FORMAT22c_FIELD_ODEX.add(INSTRUCTION_FORMAT22c_FIELD_ODEX71);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 566:38: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_FIELD_ODEX]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22c_FIELD_ODEX71));
					}


					retval.tree = root_0;

					}
					break;
				case 27 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:567:5: INSTRUCTION_FORMAT22c_TYPE
					{
					INSTRUCTION_FORMAT22c_TYPE72=(Token)match(input,INSTRUCTION_FORMAT22c_TYPE,FOLLOW_INSTRUCTION_FORMAT22c_TYPE_in_simple_name2056);
					stream_INSTRUCTION_FORMAT22c_TYPE.add(INSTRUCTION_FORMAT22c_TYPE72);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 567:32: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22c_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22c_TYPE72));
					}


					retval.tree = root_0;

					}
					break;
				case 28 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:568:5: INSTRUCTION_FORMAT22cs_FIELD
					{
					INSTRUCTION_FORMAT22cs_FIELD73=(Token)match(input,INSTRUCTION_FORMAT22cs_FIELD,FOLLOW_INSTRUCTION_FORMAT22cs_FIELD_in_simple_name2067);
					stream_INSTRUCTION_FORMAT22cs_FIELD.add(INSTRUCTION_FORMAT22cs_FIELD73);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 568:34: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22cs_FIELD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22cs_FIELD73));
					}


					retval.tree = root_0;

					}
					break;
				case 29 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:569:5: INSTRUCTION_FORMAT22s_OR_ID
					{
					INSTRUCTION_FORMAT22s_OR_ID74=(Token)match(input,INSTRUCTION_FORMAT22s_OR_ID,FOLLOW_INSTRUCTION_FORMAT22s_OR_ID_in_simple_name2078);
					stream_INSTRUCTION_FORMAT22s_OR_ID.add(INSTRUCTION_FORMAT22s_OR_ID74);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 569:33: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22s_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22s_OR_ID74));
					}


					retval.tree = root_0;

					}
					break;
				case 30 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:570:5: INSTRUCTION_FORMAT22t
					{
					INSTRUCTION_FORMAT22t75=(Token)match(input,INSTRUCTION_FORMAT22t,FOLLOW_INSTRUCTION_FORMAT22t_in_simple_name2089);
					stream_INSTRUCTION_FORMAT22t.add(INSTRUCTION_FORMAT22t75);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 570:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT22t]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT22t75));
					}


					retval.tree = root_0;

					}
					break;
				case 31 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:571:5: INSTRUCTION_FORMAT23x
					{
					INSTRUCTION_FORMAT23x76=(Token)match(input,INSTRUCTION_FORMAT23x,FOLLOW_INSTRUCTION_FORMAT23x_in_simple_name2100);
					stream_INSTRUCTION_FORMAT23x.add(INSTRUCTION_FORMAT23x76);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 571:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT23x]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT23x76));
					}


					retval.tree = root_0;

					}
					break;
				case 32 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:572:5: INSTRUCTION_FORMAT31i_OR_ID
					{
					INSTRUCTION_FORMAT31i_OR_ID77=(Token)match(input,INSTRUCTION_FORMAT31i_OR_ID,FOLLOW_INSTRUCTION_FORMAT31i_OR_ID_in_simple_name2111);
					stream_INSTRUCTION_FORMAT31i_OR_ID.add(INSTRUCTION_FORMAT31i_OR_ID77);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 572:33: -> SIMPLE_NAME[$INSTRUCTION_FORMAT31i_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT31i_OR_ID77));
					}


					retval.tree = root_0;

					}
					break;
				case 33 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:573:5: INSTRUCTION_FORMAT31t
					{
					INSTRUCTION_FORMAT31t78=(Token)match(input,INSTRUCTION_FORMAT31t,FOLLOW_INSTRUCTION_FORMAT31t_in_simple_name2122);
					stream_INSTRUCTION_FORMAT31t.add(INSTRUCTION_FORMAT31t78);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 573:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT31t]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT31t78));
					}


					retval.tree = root_0;

					}
					break;
				case 34 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:574:5: INSTRUCTION_FORMAT35c_METHOD
					{
					INSTRUCTION_FORMAT35c_METHOD79=(Token)match(input,INSTRUCTION_FORMAT35c_METHOD,FOLLOW_INSTRUCTION_FORMAT35c_METHOD_in_simple_name2133);
					stream_INSTRUCTION_FORMAT35c_METHOD.add(INSTRUCTION_FORMAT35c_METHOD79);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 574:34: -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT35c_METHOD79));
					}


					retval.tree = root_0;

					}
					break;
				case 35 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:575:5: INSTRUCTION_FORMAT35c_METHOD_ODEX
					{
					INSTRUCTION_FORMAT35c_METHOD_ODEX80=(Token)match(input,INSTRUCTION_FORMAT35c_METHOD_ODEX,FOLLOW_INSTRUCTION_FORMAT35c_METHOD_ODEX_in_simple_name2144);
					stream_INSTRUCTION_FORMAT35c_METHOD_ODEX.add(INSTRUCTION_FORMAT35c_METHOD_ODEX80);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 575:39: -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_METHOD_ODEX]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT35c_METHOD_ODEX80));
					}


					retval.tree = root_0;

					}
					break;
				case 36 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:576:5: INSTRUCTION_FORMAT35c_TYPE
					{
					INSTRUCTION_FORMAT35c_TYPE81=(Token)match(input,INSTRUCTION_FORMAT35c_TYPE,FOLLOW_INSTRUCTION_FORMAT35c_TYPE_in_simple_name2155);
					stream_INSTRUCTION_FORMAT35c_TYPE.add(INSTRUCTION_FORMAT35c_TYPE81);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 576:32: -> SIMPLE_NAME[$INSTRUCTION_FORMAT35c_TYPE]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT35c_TYPE81));
					}


					retval.tree = root_0;

					}
					break;
				case 37 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:577:5: INSTRUCTION_FORMAT35mi_METHOD
					{
					INSTRUCTION_FORMAT35mi_METHOD82=(Token)match(input,INSTRUCTION_FORMAT35mi_METHOD,FOLLOW_INSTRUCTION_FORMAT35mi_METHOD_in_simple_name2166);
					stream_INSTRUCTION_FORMAT35mi_METHOD.add(INSTRUCTION_FORMAT35mi_METHOD82);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 577:35: -> SIMPLE_NAME[$INSTRUCTION_FORMAT35mi_METHOD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT35mi_METHOD82));
					}


					retval.tree = root_0;

					}
					break;
				case 38 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:578:5: INSTRUCTION_FORMAT35ms_METHOD
					{
					INSTRUCTION_FORMAT35ms_METHOD83=(Token)match(input,INSTRUCTION_FORMAT35ms_METHOD,FOLLOW_INSTRUCTION_FORMAT35ms_METHOD_in_simple_name2177);
					stream_INSTRUCTION_FORMAT35ms_METHOD.add(INSTRUCTION_FORMAT35ms_METHOD83);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 578:35: -> SIMPLE_NAME[$INSTRUCTION_FORMAT35ms_METHOD]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT35ms_METHOD83));
					}


					retval.tree = root_0;

					}
					break;
				case 39 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:579:5: INSTRUCTION_FORMAT51l
					{
					INSTRUCTION_FORMAT51l84=(Token)match(input,INSTRUCTION_FORMAT51l,FOLLOW_INSTRUCTION_FORMAT51l_in_simple_name2188);
					stream_INSTRUCTION_FORMAT51l.add(INSTRUCTION_FORMAT51l84);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 579:27: -> SIMPLE_NAME[$INSTRUCTION_FORMAT51l]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, INSTRUCTION_FORMAT51l84));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "simple_name"


	public static class member_name_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "member_name"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:581:1: member_name : ( simple_name | MEMBER_NAME -> SIMPLE_NAME[$MEMBER_NAME] );
	public final smaliParser.member_name_return member_name() throws RecognitionException {
		smaliParser.member_name_return retval = new smaliParser.member_name_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token MEMBER_NAME86=null;
		ParserRuleReturnScope simple_name85 =null;

		CommonTree MEMBER_NAME86_tree=null;
		RewriteRuleTokenStream stream_MEMBER_NAME=new RewriteRuleTokenStream(adaptor,"token MEMBER_NAME");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:582:3: ( simple_name | MEMBER_NAME -> SIMPLE_NAME[$MEMBER_NAME] )
			int alt11=2;
			int LA11_0 = input.LA(1);
			if ( (LA11_0==ACCESS_SPEC||LA11_0==ANNOTATION_VISIBILITY||LA11_0==BOOL_LITERAL||LA11_0==DOUBLE_LITERAL_OR_ID||LA11_0==FLOAT_LITERAL_OR_ID||(LA11_0 >= INSTRUCTION_FORMAT10t && LA11_0 <= INSTRUCTION_FORMAT10x_ODEX)||LA11_0==INSTRUCTION_FORMAT11x||LA11_0==INSTRUCTION_FORMAT12x_OR_ID||(LA11_0 >= INSTRUCTION_FORMAT21c_FIELD && LA11_0 <= INSTRUCTION_FORMAT21c_TYPE)||LA11_0==INSTRUCTION_FORMAT21t||(LA11_0 >= INSTRUCTION_FORMAT22c_FIELD && LA11_0 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA11_0 >= INSTRUCTION_FORMAT22s_OR_ID && LA11_0 <= INSTRUCTION_FORMAT22t)||LA11_0==INSTRUCTION_FORMAT23x||(LA11_0 >= INSTRUCTION_FORMAT31i_OR_ID && LA11_0 <= INSTRUCTION_FORMAT31t)||(LA11_0 >= INSTRUCTION_FORMAT35c_METHOD && LA11_0 <= INSTRUCTION_FORMAT35ms_METHOD)||LA11_0==INSTRUCTION_FORMAT51l||(LA11_0 >= NEGATIVE_INTEGER_LITERAL && LA11_0 <= NULL_LITERAL)||LA11_0==PARAM_LIST_OR_ID_START||(LA11_0 >= POSITIVE_INTEGER_LITERAL && LA11_0 <= PRIMITIVE_TYPE)||LA11_0==REGISTER||LA11_0==SIMPLE_NAME||(LA11_0 >= VERIFICATION_ERROR_TYPE && LA11_0 <= VOID_TYPE)) ) {
				alt11=1;
			}
			else if ( (LA11_0==MEMBER_NAME) ) {
				alt11=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 11, 0, input);
				throw nvae;
			}

			switch (alt11) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:582:5: simple_name
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_simple_name_in_member_name2203);
					simple_name85=simple_name();
					state._fsp--;

					adaptor.addChild(root_0, simple_name85.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:583:5: MEMBER_NAME
					{
					MEMBER_NAME86=(Token)match(input,MEMBER_NAME,FOLLOW_MEMBER_NAME_in_member_name2209);
					stream_MEMBER_NAME.add(MEMBER_NAME86);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 583:17: -> SIMPLE_NAME[$MEMBER_NAME]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(SIMPLE_NAME, MEMBER_NAME86));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "member_name"


	public static class method_prototype_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "method_prototype"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:585:1: method_prototype : OPEN_PAREN param_list CLOSE_PAREN type_descriptor -> ^( I_METHOD_PROTOTYPE[$start, \"I_METHOD_PROTOTYPE\"] ^( I_METHOD_RETURN_TYPE type_descriptor ) ( param_list )? ) ;
	public final smaliParser.method_prototype_return method_prototype() throws RecognitionException {
		smaliParser.method_prototype_return retval = new smaliParser.method_prototype_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token OPEN_PAREN87=null;
		Token CLOSE_PAREN89=null;
		ParserRuleReturnScope param_list88 =null;
		ParserRuleReturnScope type_descriptor90 =null;

		CommonTree OPEN_PAREN87_tree=null;
		CommonTree CLOSE_PAREN89_tree=null;
		RewriteRuleTokenStream stream_OPEN_PAREN=new RewriteRuleTokenStream(adaptor,"token OPEN_PAREN");
		RewriteRuleTokenStream stream_CLOSE_PAREN=new RewriteRuleTokenStream(adaptor,"token CLOSE_PAREN");
		RewriteRuleSubtreeStream stream_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule type_descriptor");
		RewriteRuleSubtreeStream stream_param_list=new RewriteRuleSubtreeStream(adaptor,"rule param_list");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:586:3: ( OPEN_PAREN param_list CLOSE_PAREN type_descriptor -> ^( I_METHOD_PROTOTYPE[$start, \"I_METHOD_PROTOTYPE\"] ^( I_METHOD_RETURN_TYPE type_descriptor ) ( param_list )? ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:586:5: OPEN_PAREN param_list CLOSE_PAREN type_descriptor
			{
			OPEN_PAREN87=(Token)match(input,OPEN_PAREN,FOLLOW_OPEN_PAREN_in_method_prototype2224);
			stream_OPEN_PAREN.add(OPEN_PAREN87);

			pushFollow(FOLLOW_param_list_in_method_prototype2226);
			param_list88=param_list();
			state._fsp--;

			stream_param_list.add(param_list88.getTree());
			CLOSE_PAREN89=(Token)match(input,CLOSE_PAREN,FOLLOW_CLOSE_PAREN_in_method_prototype2228);
			stream_CLOSE_PAREN.add(CLOSE_PAREN89);

			pushFollow(FOLLOW_type_descriptor_in_method_prototype2230);
			type_descriptor90=type_descriptor();
			state._fsp--;

			stream_type_descriptor.add(type_descriptor90.getTree());
			// AST REWRITE
			// elements: param_list, type_descriptor
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 587:5: -> ^( I_METHOD_PROTOTYPE[$start, \"I_METHOD_PROTOTYPE\"] ^( I_METHOD_RETURN_TYPE type_descriptor ) ( param_list )? )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:587:8: ^( I_METHOD_PROTOTYPE[$start, \"I_METHOD_PROTOTYPE\"] ^( I_METHOD_RETURN_TYPE type_descriptor ) ( param_list )? )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_METHOD_PROTOTYPE, (retval.start), "I_METHOD_PROTOTYPE"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:587:59: ^( I_METHOD_RETURN_TYPE type_descriptor )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_METHOD_RETURN_TYPE, "I_METHOD_RETURN_TYPE"), root_2);
				adaptor.addChild(root_2, stream_type_descriptor.nextTree());
				adaptor.addChild(root_1, root_2);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:587:99: ( param_list )?
				if ( stream_param_list.hasNext() ) {
					adaptor.addChild(root_1, stream_param_list.nextTree());
				}
				stream_param_list.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "method_prototype"


	public static class param_list_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "param_list"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:589:1: param_list : ( PARAM_LIST_START ( nonvoid_type_descriptor )* PARAM_LIST_END -> ( nonvoid_type_descriptor )* | PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )* PARAM_LIST_OR_ID_END -> ( PRIMITIVE_TYPE )* | ( nonvoid_type_descriptor )* );
	public final smaliParser.param_list_return param_list() throws RecognitionException {
		smaliParser.param_list_return retval = new smaliParser.param_list_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token PARAM_LIST_START91=null;
		Token PARAM_LIST_END93=null;
		Token PARAM_LIST_OR_ID_START94=null;
		Token PRIMITIVE_TYPE95=null;
		Token PARAM_LIST_OR_ID_END96=null;
		ParserRuleReturnScope nonvoid_type_descriptor92 =null;
		ParserRuleReturnScope nonvoid_type_descriptor97 =null;

		CommonTree PARAM_LIST_START91_tree=null;
		CommonTree PARAM_LIST_END93_tree=null;
		CommonTree PARAM_LIST_OR_ID_START94_tree=null;
		CommonTree PRIMITIVE_TYPE95_tree=null;
		CommonTree PARAM_LIST_OR_ID_END96_tree=null;
		RewriteRuleTokenStream stream_PARAM_LIST_OR_ID_START=new RewriteRuleTokenStream(adaptor,"token PARAM_LIST_OR_ID_START");
		RewriteRuleTokenStream stream_PARAM_LIST_OR_ID_END=new RewriteRuleTokenStream(adaptor,"token PARAM_LIST_OR_ID_END");
		RewriteRuleTokenStream stream_PARAM_LIST_START=new RewriteRuleTokenStream(adaptor,"token PARAM_LIST_START");
		RewriteRuleTokenStream stream_PARAM_LIST_END=new RewriteRuleTokenStream(adaptor,"token PARAM_LIST_END");
		RewriteRuleTokenStream stream_PRIMITIVE_TYPE=new RewriteRuleTokenStream(adaptor,"token PRIMITIVE_TYPE");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:590:3: ( PARAM_LIST_START ( nonvoid_type_descriptor )* PARAM_LIST_END -> ( nonvoid_type_descriptor )* | PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )* PARAM_LIST_OR_ID_END -> ( PRIMITIVE_TYPE )* | ( nonvoid_type_descriptor )* )
			int alt15=3;
			switch ( input.LA(1) ) {
			case PARAM_LIST_START:
				{
				alt15=1;
				}
				break;
			case PARAM_LIST_OR_ID_START:
				{
				alt15=2;
				}
				break;
			case ARRAY_DESCRIPTOR:
			case CLASS_DESCRIPTOR:
			case CLOSE_PAREN:
			case PRIMITIVE_TYPE:
				{
				alt15=3;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 15, 0, input);
				throw nvae;
			}
			switch (alt15) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:590:5: PARAM_LIST_START ( nonvoid_type_descriptor )* PARAM_LIST_END
					{
					PARAM_LIST_START91=(Token)match(input,PARAM_LIST_START,FOLLOW_PARAM_LIST_START_in_param_list2260);
					stream_PARAM_LIST_START.add(PARAM_LIST_START91);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:590:22: ( nonvoid_type_descriptor )*
					loop12:
					while (true) {
						int alt12=2;
						int LA12_0 = input.LA(1);
						if ( (LA12_0==ARRAY_DESCRIPTOR||LA12_0==CLASS_DESCRIPTOR||LA12_0==PRIMITIVE_TYPE) ) {
							alt12=1;
						}

						switch (alt12) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:590:22: nonvoid_type_descriptor
							{
							pushFollow(FOLLOW_nonvoid_type_descriptor_in_param_list2262);
							nonvoid_type_descriptor92=nonvoid_type_descriptor();
							state._fsp--;

							stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor92.getTree());
							}
							break;

						default :
							break loop12;
						}
					}

					PARAM_LIST_END93=(Token)match(input,PARAM_LIST_END,FOLLOW_PARAM_LIST_END_in_param_list2265);
					stream_PARAM_LIST_END.add(PARAM_LIST_END93);

					// AST REWRITE
					// elements: nonvoid_type_descriptor
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 590:62: -> ( nonvoid_type_descriptor )*
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:590:65: ( nonvoid_type_descriptor )*
						while ( stream_nonvoid_type_descriptor.hasNext() ) {
							adaptor.addChild(root_0, stream_nonvoid_type_descriptor.nextTree());
						}
						stream_nonvoid_type_descriptor.reset();

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:591:5: PARAM_LIST_OR_ID_START ( PRIMITIVE_TYPE )* PARAM_LIST_OR_ID_END
					{
					PARAM_LIST_OR_ID_START94=(Token)match(input,PARAM_LIST_OR_ID_START,FOLLOW_PARAM_LIST_OR_ID_START_in_param_list2276);
					stream_PARAM_LIST_OR_ID_START.add(PARAM_LIST_OR_ID_START94);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:591:28: ( PRIMITIVE_TYPE )*
					loop13:
					while (true) {
						int alt13=2;
						int LA13_0 = input.LA(1);
						if ( (LA13_0==PRIMITIVE_TYPE) ) {
							alt13=1;
						}

						switch (alt13) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:591:28: PRIMITIVE_TYPE
							{
							PRIMITIVE_TYPE95=(Token)match(input,PRIMITIVE_TYPE,FOLLOW_PRIMITIVE_TYPE_in_param_list2278);
							stream_PRIMITIVE_TYPE.add(PRIMITIVE_TYPE95);

							}
							break;

						default :
							break loop13;
						}
					}

					PARAM_LIST_OR_ID_END96=(Token)match(input,PARAM_LIST_OR_ID_END,FOLLOW_PARAM_LIST_OR_ID_END_in_param_list2281);
					stream_PARAM_LIST_OR_ID_END.add(PARAM_LIST_OR_ID_END96);

					// AST REWRITE
					// elements: PRIMITIVE_TYPE
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 591:65: -> ( PRIMITIVE_TYPE )*
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:591:68: ( PRIMITIVE_TYPE )*
						while ( stream_PRIMITIVE_TYPE.hasNext() ) {
							adaptor.addChild(root_0, stream_PRIMITIVE_TYPE.nextNode());
						}
						stream_PRIMITIVE_TYPE.reset();

					}


					retval.tree = root_0;

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:592:5: ( nonvoid_type_descriptor )*
					{
					root_0 = (CommonTree)adaptor.nil();


					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:592:5: ( nonvoid_type_descriptor )*
					loop14:
					while (true) {
						int alt14=2;
						int LA14_0 = input.LA(1);
						if ( (LA14_0==ARRAY_DESCRIPTOR||LA14_0==CLASS_DESCRIPTOR||LA14_0==PRIMITIVE_TYPE) ) {
							alt14=1;
						}

						switch (alt14) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:592:5: nonvoid_type_descriptor
							{
							pushFollow(FOLLOW_nonvoid_type_descriptor_in_param_list2292);
							nonvoid_type_descriptor97=nonvoid_type_descriptor();
							state._fsp--;

							adaptor.addChild(root_0, nonvoid_type_descriptor97.getTree());

							}
							break;

						default :
							break loop14;
						}
					}

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "param_list"


	public static class type_descriptor_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "type_descriptor"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:594:1: type_descriptor : ( VOID_TYPE | PRIMITIVE_TYPE | CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR );
	public final smaliParser.type_descriptor_return type_descriptor() throws RecognitionException {
		smaliParser.type_descriptor_return retval = new smaliParser.type_descriptor_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token set98=null;

		CommonTree set98_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:595:3: ( VOID_TYPE | PRIMITIVE_TYPE | CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:
			{
			root_0 = (CommonTree)adaptor.nil();


			set98=input.LT(1);
			if ( input.LA(1)==ARRAY_DESCRIPTOR||input.LA(1)==CLASS_DESCRIPTOR||input.LA(1)==PRIMITIVE_TYPE||input.LA(1)==VOID_TYPE ) {
				input.consume();
				adaptor.addChild(root_0, (CommonTree)adaptor.create(set98));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "type_descriptor"


	public static class nonvoid_type_descriptor_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "nonvoid_type_descriptor"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:600:1: nonvoid_type_descriptor : ( PRIMITIVE_TYPE | CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR );
	public final smaliParser.nonvoid_type_descriptor_return nonvoid_type_descriptor() throws RecognitionException {
		smaliParser.nonvoid_type_descriptor_return retval = new smaliParser.nonvoid_type_descriptor_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token set99=null;

		CommonTree set99_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:601:3: ( PRIMITIVE_TYPE | CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:
			{
			root_0 = (CommonTree)adaptor.nil();


			set99=input.LT(1);
			if ( input.LA(1)==ARRAY_DESCRIPTOR||input.LA(1)==CLASS_DESCRIPTOR||input.LA(1)==PRIMITIVE_TYPE ) {
				input.consume();
				adaptor.addChild(root_0, (CommonTree)adaptor.create(set99));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "nonvoid_type_descriptor"


	public static class reference_type_descriptor_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "reference_type_descriptor"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:605:1: reference_type_descriptor : ( CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR );
	public final smaliParser.reference_type_descriptor_return reference_type_descriptor() throws RecognitionException {
		smaliParser.reference_type_descriptor_return retval = new smaliParser.reference_type_descriptor_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token set100=null;

		CommonTree set100_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:606:3: ( CLASS_DESCRIPTOR | ARRAY_DESCRIPTOR )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:
			{
			root_0 = (CommonTree)adaptor.nil();


			set100=input.LT(1);
			if ( input.LA(1)==ARRAY_DESCRIPTOR||input.LA(1)==CLASS_DESCRIPTOR ) {
				input.consume();
				adaptor.addChild(root_0, (CommonTree)adaptor.create(set100));
				state.errorRecovery=false;
			}
			else {
				MismatchedSetException mse = new MismatchedSetException(null,input);
				throw mse;
			}
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "reference_type_descriptor"


	public static class integer_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "integer_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:609:1: integer_literal : ( POSITIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$POSITIVE_INTEGER_LITERAL] | NEGATIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$NEGATIVE_INTEGER_LITERAL] );
	public final smaliParser.integer_literal_return integer_literal() throws RecognitionException {
		smaliParser.integer_literal_return retval = new smaliParser.integer_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token POSITIVE_INTEGER_LITERAL101=null;
		Token NEGATIVE_INTEGER_LITERAL102=null;

		CommonTree POSITIVE_INTEGER_LITERAL101_tree=null;
		CommonTree NEGATIVE_INTEGER_LITERAL102_tree=null;
		RewriteRuleTokenStream stream_NEGATIVE_INTEGER_LITERAL=new RewriteRuleTokenStream(adaptor,"token NEGATIVE_INTEGER_LITERAL");
		RewriteRuleTokenStream stream_POSITIVE_INTEGER_LITERAL=new RewriteRuleTokenStream(adaptor,"token POSITIVE_INTEGER_LITERAL");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:610:3: ( POSITIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$POSITIVE_INTEGER_LITERAL] | NEGATIVE_INTEGER_LITERAL -> INTEGER_LITERAL[$NEGATIVE_INTEGER_LITERAL] )
			int alt16=2;
			int LA16_0 = input.LA(1);
			if ( (LA16_0==POSITIVE_INTEGER_LITERAL) ) {
				alt16=1;
			}
			else if ( (LA16_0==NEGATIVE_INTEGER_LITERAL) ) {
				alt16=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 16, 0, input);
				throw nvae;
			}

			switch (alt16) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:610:5: POSITIVE_INTEGER_LITERAL
					{
					POSITIVE_INTEGER_LITERAL101=(Token)match(input,POSITIVE_INTEGER_LITERAL,FOLLOW_POSITIVE_INTEGER_LITERAL_in_integer_literal2369);
					stream_POSITIVE_INTEGER_LITERAL.add(POSITIVE_INTEGER_LITERAL101);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 610:30: -> INTEGER_LITERAL[$POSITIVE_INTEGER_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(INTEGER_LITERAL, POSITIVE_INTEGER_LITERAL101));
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:611:5: NEGATIVE_INTEGER_LITERAL
					{
					NEGATIVE_INTEGER_LITERAL102=(Token)match(input,NEGATIVE_INTEGER_LITERAL,FOLLOW_NEGATIVE_INTEGER_LITERAL_in_integer_literal2380);
					stream_NEGATIVE_INTEGER_LITERAL.add(NEGATIVE_INTEGER_LITERAL102);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 611:30: -> INTEGER_LITERAL[$NEGATIVE_INTEGER_LITERAL]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(INTEGER_LITERAL, NEGATIVE_INTEGER_LITERAL102));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "integer_literal"


	public static class float_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "float_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:613:1: float_literal : ( FLOAT_LITERAL_OR_ID -> FLOAT_LITERAL[$FLOAT_LITERAL_OR_ID] | FLOAT_LITERAL );
	public final smaliParser.float_literal_return float_literal() throws RecognitionException {
		smaliParser.float_literal_return retval = new smaliParser.float_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token FLOAT_LITERAL_OR_ID103=null;
		Token FLOAT_LITERAL104=null;

		CommonTree FLOAT_LITERAL_OR_ID103_tree=null;
		CommonTree FLOAT_LITERAL104_tree=null;
		RewriteRuleTokenStream stream_FLOAT_LITERAL_OR_ID=new RewriteRuleTokenStream(adaptor,"token FLOAT_LITERAL_OR_ID");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:614:3: ( FLOAT_LITERAL_OR_ID -> FLOAT_LITERAL[$FLOAT_LITERAL_OR_ID] | FLOAT_LITERAL )
			int alt17=2;
			int LA17_0 = input.LA(1);
			if ( (LA17_0==FLOAT_LITERAL_OR_ID) ) {
				alt17=1;
			}
			else if ( (LA17_0==FLOAT_LITERAL) ) {
				alt17=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 17, 0, input);
				throw nvae;
			}

			switch (alt17) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:614:5: FLOAT_LITERAL_OR_ID
					{
					FLOAT_LITERAL_OR_ID103=(Token)match(input,FLOAT_LITERAL_OR_ID,FOLLOW_FLOAT_LITERAL_OR_ID_in_float_literal2395);
					stream_FLOAT_LITERAL_OR_ID.add(FLOAT_LITERAL_OR_ID103);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 614:25: -> FLOAT_LITERAL[$FLOAT_LITERAL_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(FLOAT_LITERAL, FLOAT_LITERAL_OR_ID103));
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:615:5: FLOAT_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					FLOAT_LITERAL104=(Token)match(input,FLOAT_LITERAL,FOLLOW_FLOAT_LITERAL_in_float_literal2406);
					FLOAT_LITERAL104_tree = (CommonTree)adaptor.create(FLOAT_LITERAL104);
					adaptor.addChild(root_0, FLOAT_LITERAL104_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "float_literal"


	public static class double_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "double_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:617:1: double_literal : ( DOUBLE_LITERAL_OR_ID -> DOUBLE_LITERAL[$DOUBLE_LITERAL_OR_ID] | DOUBLE_LITERAL );
	public final smaliParser.double_literal_return double_literal() throws RecognitionException {
		smaliParser.double_literal_return retval = new smaliParser.double_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token DOUBLE_LITERAL_OR_ID105=null;
		Token DOUBLE_LITERAL106=null;

		CommonTree DOUBLE_LITERAL_OR_ID105_tree=null;
		CommonTree DOUBLE_LITERAL106_tree=null;
		RewriteRuleTokenStream stream_DOUBLE_LITERAL_OR_ID=new RewriteRuleTokenStream(adaptor,"token DOUBLE_LITERAL_OR_ID");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:618:3: ( DOUBLE_LITERAL_OR_ID -> DOUBLE_LITERAL[$DOUBLE_LITERAL_OR_ID] | DOUBLE_LITERAL )
			int alt18=2;
			int LA18_0 = input.LA(1);
			if ( (LA18_0==DOUBLE_LITERAL_OR_ID) ) {
				alt18=1;
			}
			else if ( (LA18_0==DOUBLE_LITERAL) ) {
				alt18=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 18, 0, input);
				throw nvae;
			}

			switch (alt18) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:618:5: DOUBLE_LITERAL_OR_ID
					{
					DOUBLE_LITERAL_OR_ID105=(Token)match(input,DOUBLE_LITERAL_OR_ID,FOLLOW_DOUBLE_LITERAL_OR_ID_in_double_literal2416);
					stream_DOUBLE_LITERAL_OR_ID.add(DOUBLE_LITERAL_OR_ID105);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 618:26: -> DOUBLE_LITERAL[$DOUBLE_LITERAL_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(DOUBLE_LITERAL, DOUBLE_LITERAL_OR_ID105));
					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:619:5: DOUBLE_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					DOUBLE_LITERAL106=(Token)match(input,DOUBLE_LITERAL,FOLLOW_DOUBLE_LITERAL_in_double_literal2427);
					DOUBLE_LITERAL106_tree = (CommonTree)adaptor.create(DOUBLE_LITERAL106);
					adaptor.addChild(root_0, DOUBLE_LITERAL106_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "double_literal"


	public static class literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:621:1: literal : ( LONG_LITERAL | integer_literal | SHORT_LITERAL | BYTE_LITERAL | float_literal | double_literal | CHAR_LITERAL | STRING_LITERAL | BOOL_LITERAL | NULL_LITERAL | array_literal | subannotation | type_field_method_literal | enum_literal );
	public final smaliParser.literal_return literal() throws RecognitionException {
		smaliParser.literal_return retval = new smaliParser.literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token LONG_LITERAL107=null;
		Token SHORT_LITERAL109=null;
		Token BYTE_LITERAL110=null;
		Token CHAR_LITERAL113=null;
		Token STRING_LITERAL114=null;
		Token BOOL_LITERAL115=null;
		Token NULL_LITERAL116=null;
		ParserRuleReturnScope integer_literal108 =null;
		ParserRuleReturnScope float_literal111 =null;
		ParserRuleReturnScope double_literal112 =null;
		ParserRuleReturnScope array_literal117 =null;
		ParserRuleReturnScope subannotation118 =null;
		ParserRuleReturnScope type_field_method_literal119 =null;
		ParserRuleReturnScope enum_literal120 =null;

		CommonTree LONG_LITERAL107_tree=null;
		CommonTree SHORT_LITERAL109_tree=null;
		CommonTree BYTE_LITERAL110_tree=null;
		CommonTree CHAR_LITERAL113_tree=null;
		CommonTree STRING_LITERAL114_tree=null;
		CommonTree BOOL_LITERAL115_tree=null;
		CommonTree NULL_LITERAL116_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:622:3: ( LONG_LITERAL | integer_literal | SHORT_LITERAL | BYTE_LITERAL | float_literal | double_literal | CHAR_LITERAL | STRING_LITERAL | BOOL_LITERAL | NULL_LITERAL | array_literal | subannotation | type_field_method_literal | enum_literal )
			int alt19=14;
			switch ( input.LA(1) ) {
			case LONG_LITERAL:
				{
				alt19=1;
				}
				break;
			case POSITIVE_INTEGER_LITERAL:
				{
				int LA19_2 = input.LA(2);
				if ( (LA19_2==EOF||(LA19_2 >= ACCESS_SPEC && LA19_2 <= ANNOTATION_VISIBILITY)||LA19_2==BOOL_LITERAL||(LA19_2 >= CLASS_DIRECTIVE && LA19_2 <= CLOSE_BRACE)||LA19_2==COMMA||(LA19_2 >= DOUBLE_LITERAL_OR_ID && LA19_2 <= END_ANNOTATION_DIRECTIVE)||LA19_2==END_FIELD_DIRECTIVE||LA19_2==END_SUBANNOTATION_DIRECTIVE||LA19_2==FIELD_DIRECTIVE||(LA19_2 >= FLOAT_LITERAL_OR_ID && LA19_2 <= IMPLEMENTS_DIRECTIVE)||(LA19_2 >= INSTRUCTION_FORMAT10t && LA19_2 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_2==INSTRUCTION_FORMAT11x||LA19_2==INSTRUCTION_FORMAT12x_OR_ID||(LA19_2 >= INSTRUCTION_FORMAT21c_FIELD && LA19_2 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_2==INSTRUCTION_FORMAT21t||(LA19_2 >= INSTRUCTION_FORMAT22c_FIELD && LA19_2 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_2 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_2 <= INSTRUCTION_FORMAT22t)||LA19_2==INSTRUCTION_FORMAT23x||(LA19_2 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_2 <= INSTRUCTION_FORMAT31t)||(LA19_2 >= INSTRUCTION_FORMAT35c_METHOD && LA19_2 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_2==INSTRUCTION_FORMAT51l||(LA19_2 >= METHOD_DIRECTIVE && LA19_2 <= NULL_LITERAL)||LA19_2==PARAM_LIST_OR_ID_START||(LA19_2 >= POSITIVE_INTEGER_LITERAL && LA19_2 <= PRIMITIVE_TYPE)||LA19_2==REGISTER||(LA19_2 >= SIMPLE_NAME && LA19_2 <= SOURCE_DIRECTIVE)||(LA19_2 >= SUPER_DIRECTIVE && LA19_2 <= VOID_TYPE)) ) {
					alt19=2;
				}
				else if ( (LA19_2==COLON||LA19_2==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 2, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case NEGATIVE_INTEGER_LITERAL:
				{
				int LA19_3 = input.LA(2);
				if ( (LA19_3==EOF||(LA19_3 >= ACCESS_SPEC && LA19_3 <= ANNOTATION_VISIBILITY)||LA19_3==BOOL_LITERAL||(LA19_3 >= CLASS_DIRECTIVE && LA19_3 <= CLOSE_BRACE)||LA19_3==COMMA||(LA19_3 >= DOUBLE_LITERAL_OR_ID && LA19_3 <= END_ANNOTATION_DIRECTIVE)||LA19_3==END_FIELD_DIRECTIVE||LA19_3==END_SUBANNOTATION_DIRECTIVE||LA19_3==FIELD_DIRECTIVE||(LA19_3 >= FLOAT_LITERAL_OR_ID && LA19_3 <= IMPLEMENTS_DIRECTIVE)||(LA19_3 >= INSTRUCTION_FORMAT10t && LA19_3 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_3==INSTRUCTION_FORMAT11x||LA19_3==INSTRUCTION_FORMAT12x_OR_ID||(LA19_3 >= INSTRUCTION_FORMAT21c_FIELD && LA19_3 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_3==INSTRUCTION_FORMAT21t||(LA19_3 >= INSTRUCTION_FORMAT22c_FIELD && LA19_3 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_3 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_3 <= INSTRUCTION_FORMAT22t)||LA19_3==INSTRUCTION_FORMAT23x||(LA19_3 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_3 <= INSTRUCTION_FORMAT31t)||(LA19_3 >= INSTRUCTION_FORMAT35c_METHOD && LA19_3 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_3==INSTRUCTION_FORMAT51l||(LA19_3 >= METHOD_DIRECTIVE && LA19_3 <= NULL_LITERAL)||LA19_3==PARAM_LIST_OR_ID_START||(LA19_3 >= POSITIVE_INTEGER_LITERAL && LA19_3 <= PRIMITIVE_TYPE)||LA19_3==REGISTER||(LA19_3 >= SIMPLE_NAME && LA19_3 <= SOURCE_DIRECTIVE)||(LA19_3 >= SUPER_DIRECTIVE && LA19_3 <= VOID_TYPE)) ) {
					alt19=2;
				}
				else if ( (LA19_3==COLON||LA19_3==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case SHORT_LITERAL:
				{
				alt19=3;
				}
				break;
			case BYTE_LITERAL:
				{
				alt19=4;
				}
				break;
			case FLOAT_LITERAL_OR_ID:
				{
				int LA19_6 = input.LA(2);
				if ( (LA19_6==EOF||(LA19_6 >= ACCESS_SPEC && LA19_6 <= ANNOTATION_VISIBILITY)||LA19_6==BOOL_LITERAL||(LA19_6 >= CLASS_DIRECTIVE && LA19_6 <= CLOSE_BRACE)||LA19_6==COMMA||(LA19_6 >= DOUBLE_LITERAL_OR_ID && LA19_6 <= END_ANNOTATION_DIRECTIVE)||LA19_6==END_FIELD_DIRECTIVE||LA19_6==END_SUBANNOTATION_DIRECTIVE||LA19_6==FIELD_DIRECTIVE||(LA19_6 >= FLOAT_LITERAL_OR_ID && LA19_6 <= IMPLEMENTS_DIRECTIVE)||(LA19_6 >= INSTRUCTION_FORMAT10t && LA19_6 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_6==INSTRUCTION_FORMAT11x||LA19_6==INSTRUCTION_FORMAT12x_OR_ID||(LA19_6 >= INSTRUCTION_FORMAT21c_FIELD && LA19_6 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_6==INSTRUCTION_FORMAT21t||(LA19_6 >= INSTRUCTION_FORMAT22c_FIELD && LA19_6 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_6 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_6 <= INSTRUCTION_FORMAT22t)||LA19_6==INSTRUCTION_FORMAT23x||(LA19_6 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_6 <= INSTRUCTION_FORMAT31t)||(LA19_6 >= INSTRUCTION_FORMAT35c_METHOD && LA19_6 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_6==INSTRUCTION_FORMAT51l||(LA19_6 >= METHOD_DIRECTIVE && LA19_6 <= NULL_LITERAL)||LA19_6==PARAM_LIST_OR_ID_START||(LA19_6 >= POSITIVE_INTEGER_LITERAL && LA19_6 <= PRIMITIVE_TYPE)||LA19_6==REGISTER||(LA19_6 >= SIMPLE_NAME && LA19_6 <= SOURCE_DIRECTIVE)||(LA19_6 >= SUPER_DIRECTIVE && LA19_6 <= VOID_TYPE)) ) {
					alt19=5;
				}
				else if ( (LA19_6==COLON||LA19_6==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 6, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case FLOAT_LITERAL:
				{
				alt19=5;
				}
				break;
			case DOUBLE_LITERAL_OR_ID:
				{
				int LA19_8 = input.LA(2);
				if ( (LA19_8==EOF||(LA19_8 >= ACCESS_SPEC && LA19_8 <= ANNOTATION_VISIBILITY)||LA19_8==BOOL_LITERAL||(LA19_8 >= CLASS_DIRECTIVE && LA19_8 <= CLOSE_BRACE)||LA19_8==COMMA||(LA19_8 >= DOUBLE_LITERAL_OR_ID && LA19_8 <= END_ANNOTATION_DIRECTIVE)||LA19_8==END_FIELD_DIRECTIVE||LA19_8==END_SUBANNOTATION_DIRECTIVE||LA19_8==FIELD_DIRECTIVE||(LA19_8 >= FLOAT_LITERAL_OR_ID && LA19_8 <= IMPLEMENTS_DIRECTIVE)||(LA19_8 >= INSTRUCTION_FORMAT10t && LA19_8 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_8==INSTRUCTION_FORMAT11x||LA19_8==INSTRUCTION_FORMAT12x_OR_ID||(LA19_8 >= INSTRUCTION_FORMAT21c_FIELD && LA19_8 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_8==INSTRUCTION_FORMAT21t||(LA19_8 >= INSTRUCTION_FORMAT22c_FIELD && LA19_8 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_8 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_8 <= INSTRUCTION_FORMAT22t)||LA19_8==INSTRUCTION_FORMAT23x||(LA19_8 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_8 <= INSTRUCTION_FORMAT31t)||(LA19_8 >= INSTRUCTION_FORMAT35c_METHOD && LA19_8 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_8==INSTRUCTION_FORMAT51l||(LA19_8 >= METHOD_DIRECTIVE && LA19_8 <= NULL_LITERAL)||LA19_8==PARAM_LIST_OR_ID_START||(LA19_8 >= POSITIVE_INTEGER_LITERAL && LA19_8 <= PRIMITIVE_TYPE)||LA19_8==REGISTER||(LA19_8 >= SIMPLE_NAME && LA19_8 <= SOURCE_DIRECTIVE)||(LA19_8 >= SUPER_DIRECTIVE && LA19_8 <= VOID_TYPE)) ) {
					alt19=6;
				}
				else if ( (LA19_8==COLON||LA19_8==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 8, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case DOUBLE_LITERAL:
				{
				alt19=6;
				}
				break;
			case CHAR_LITERAL:
				{
				alt19=7;
				}
				break;
			case STRING_LITERAL:
				{
				alt19=8;
				}
				break;
			case BOOL_LITERAL:
				{
				int LA19_12 = input.LA(2);
				if ( (LA19_12==EOF||(LA19_12 >= ACCESS_SPEC && LA19_12 <= ANNOTATION_VISIBILITY)||LA19_12==BOOL_LITERAL||(LA19_12 >= CLASS_DIRECTIVE && LA19_12 <= CLOSE_BRACE)||LA19_12==COMMA||(LA19_12 >= DOUBLE_LITERAL_OR_ID && LA19_12 <= END_ANNOTATION_DIRECTIVE)||LA19_12==END_FIELD_DIRECTIVE||LA19_12==END_SUBANNOTATION_DIRECTIVE||LA19_12==FIELD_DIRECTIVE||(LA19_12 >= FLOAT_LITERAL_OR_ID && LA19_12 <= IMPLEMENTS_DIRECTIVE)||(LA19_12 >= INSTRUCTION_FORMAT10t && LA19_12 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_12==INSTRUCTION_FORMAT11x||LA19_12==INSTRUCTION_FORMAT12x_OR_ID||(LA19_12 >= INSTRUCTION_FORMAT21c_FIELD && LA19_12 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_12==INSTRUCTION_FORMAT21t||(LA19_12 >= INSTRUCTION_FORMAT22c_FIELD && LA19_12 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_12 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_12 <= INSTRUCTION_FORMAT22t)||LA19_12==INSTRUCTION_FORMAT23x||(LA19_12 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_12 <= INSTRUCTION_FORMAT31t)||(LA19_12 >= INSTRUCTION_FORMAT35c_METHOD && LA19_12 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_12==INSTRUCTION_FORMAT51l||(LA19_12 >= METHOD_DIRECTIVE && LA19_12 <= NULL_LITERAL)||LA19_12==PARAM_LIST_OR_ID_START||(LA19_12 >= POSITIVE_INTEGER_LITERAL && LA19_12 <= PRIMITIVE_TYPE)||LA19_12==REGISTER||(LA19_12 >= SIMPLE_NAME && LA19_12 <= SOURCE_DIRECTIVE)||(LA19_12 >= SUPER_DIRECTIVE && LA19_12 <= VOID_TYPE)) ) {
					alt19=9;
				}
				else if ( (LA19_12==COLON||LA19_12==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 12, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case NULL_LITERAL:
				{
				int LA19_13 = input.LA(2);
				if ( (LA19_13==EOF||(LA19_13 >= ACCESS_SPEC && LA19_13 <= ANNOTATION_VISIBILITY)||LA19_13==BOOL_LITERAL||(LA19_13 >= CLASS_DIRECTIVE && LA19_13 <= CLOSE_BRACE)||LA19_13==COMMA||(LA19_13 >= DOUBLE_LITERAL_OR_ID && LA19_13 <= END_ANNOTATION_DIRECTIVE)||LA19_13==END_FIELD_DIRECTIVE||LA19_13==END_SUBANNOTATION_DIRECTIVE||LA19_13==FIELD_DIRECTIVE||(LA19_13 >= FLOAT_LITERAL_OR_ID && LA19_13 <= IMPLEMENTS_DIRECTIVE)||(LA19_13 >= INSTRUCTION_FORMAT10t && LA19_13 <= INSTRUCTION_FORMAT10x_ODEX)||LA19_13==INSTRUCTION_FORMAT11x||LA19_13==INSTRUCTION_FORMAT12x_OR_ID||(LA19_13 >= INSTRUCTION_FORMAT21c_FIELD && LA19_13 <= INSTRUCTION_FORMAT21c_TYPE)||LA19_13==INSTRUCTION_FORMAT21t||(LA19_13 >= INSTRUCTION_FORMAT22c_FIELD && LA19_13 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA19_13 >= INSTRUCTION_FORMAT22s_OR_ID && LA19_13 <= INSTRUCTION_FORMAT22t)||LA19_13==INSTRUCTION_FORMAT23x||(LA19_13 >= INSTRUCTION_FORMAT31i_OR_ID && LA19_13 <= INSTRUCTION_FORMAT31t)||(LA19_13 >= INSTRUCTION_FORMAT35c_METHOD && LA19_13 <= INSTRUCTION_FORMAT35ms_METHOD)||LA19_13==INSTRUCTION_FORMAT51l||(LA19_13 >= METHOD_DIRECTIVE && LA19_13 <= NULL_LITERAL)||LA19_13==PARAM_LIST_OR_ID_START||(LA19_13 >= POSITIVE_INTEGER_LITERAL && LA19_13 <= PRIMITIVE_TYPE)||LA19_13==REGISTER||(LA19_13 >= SIMPLE_NAME && LA19_13 <= SOURCE_DIRECTIVE)||(LA19_13 >= SUPER_DIRECTIVE && LA19_13 <= VOID_TYPE)) ) {
					alt19=10;
				}
				else if ( (LA19_13==COLON||LA19_13==OPEN_PAREN) ) {
					alt19=13;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 19, 13, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case OPEN_BRACE:
				{
				alt19=11;
				}
				break;
			case SUBANNOTATION_DIRECTIVE:
				{
				alt19=12;
				}
				break;
			case ACCESS_SPEC:
			case ANNOTATION_VISIBILITY:
			case ARRAY_DESCRIPTOR:
			case CLASS_DESCRIPTOR:
			case INSTRUCTION_FORMAT10t:
			case INSTRUCTION_FORMAT10x:
			case INSTRUCTION_FORMAT10x_ODEX:
			case INSTRUCTION_FORMAT11x:
			case INSTRUCTION_FORMAT12x_OR_ID:
			case INSTRUCTION_FORMAT21c_FIELD:
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
			case INSTRUCTION_FORMAT21c_STRING:
			case INSTRUCTION_FORMAT21c_TYPE:
			case INSTRUCTION_FORMAT21t:
			case INSTRUCTION_FORMAT22c_FIELD:
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
			case INSTRUCTION_FORMAT22c_TYPE:
			case INSTRUCTION_FORMAT22cs_FIELD:
			case INSTRUCTION_FORMAT22s_OR_ID:
			case INSTRUCTION_FORMAT22t:
			case INSTRUCTION_FORMAT23x:
			case INSTRUCTION_FORMAT31i_OR_ID:
			case INSTRUCTION_FORMAT31t:
			case INSTRUCTION_FORMAT35c_METHOD:
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
			case INSTRUCTION_FORMAT35c_TYPE:
			case INSTRUCTION_FORMAT35mi_METHOD:
			case INSTRUCTION_FORMAT35ms_METHOD:
			case INSTRUCTION_FORMAT51l:
			case MEMBER_NAME:
			case PARAM_LIST_OR_ID_START:
			case PRIMITIVE_TYPE:
			case REGISTER:
			case SIMPLE_NAME:
			case VERIFICATION_ERROR_TYPE:
			case VOID_TYPE:
				{
				alt19=13;
				}
				break;
			case ENUM_DIRECTIVE:
				{
				alt19=14;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 19, 0, input);
				throw nvae;
			}
			switch (alt19) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:622:5: LONG_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					LONG_LITERAL107=(Token)match(input,LONG_LITERAL,FOLLOW_LONG_LITERAL_in_literal2437);
					LONG_LITERAL107_tree = (CommonTree)adaptor.create(LONG_LITERAL107);
					adaptor.addChild(root_0, LONG_LITERAL107_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:623:5: integer_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_integer_literal_in_literal2443);
					integer_literal108=integer_literal();
					state._fsp--;

					adaptor.addChild(root_0, integer_literal108.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:624:5: SHORT_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					SHORT_LITERAL109=(Token)match(input,SHORT_LITERAL,FOLLOW_SHORT_LITERAL_in_literal2449);
					SHORT_LITERAL109_tree = (CommonTree)adaptor.create(SHORT_LITERAL109);
					adaptor.addChild(root_0, SHORT_LITERAL109_tree);

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:625:5: BYTE_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BYTE_LITERAL110=(Token)match(input,BYTE_LITERAL,FOLLOW_BYTE_LITERAL_in_literal2455);
					BYTE_LITERAL110_tree = (CommonTree)adaptor.create(BYTE_LITERAL110);
					adaptor.addChild(root_0, BYTE_LITERAL110_tree);

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:626:5: float_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_float_literal_in_literal2461);
					float_literal111=float_literal();
					state._fsp--;

					adaptor.addChild(root_0, float_literal111.getTree());

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:627:5: double_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_double_literal_in_literal2467);
					double_literal112=double_literal();
					state._fsp--;

					adaptor.addChild(root_0, double_literal112.getTree());

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:628:5: CHAR_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					CHAR_LITERAL113=(Token)match(input,CHAR_LITERAL,FOLLOW_CHAR_LITERAL_in_literal2473);
					CHAR_LITERAL113_tree = (CommonTree)adaptor.create(CHAR_LITERAL113);
					adaptor.addChild(root_0, CHAR_LITERAL113_tree);

					}
					break;
				case 8 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:629:5: STRING_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					STRING_LITERAL114=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_literal2479);
					STRING_LITERAL114_tree = (CommonTree)adaptor.create(STRING_LITERAL114);
					adaptor.addChild(root_0, STRING_LITERAL114_tree);

					}
					break;
				case 9 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:630:5: BOOL_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BOOL_LITERAL115=(Token)match(input,BOOL_LITERAL,FOLLOW_BOOL_LITERAL_in_literal2485);
					BOOL_LITERAL115_tree = (CommonTree)adaptor.create(BOOL_LITERAL115);
					adaptor.addChild(root_0, BOOL_LITERAL115_tree);

					}
					break;
				case 10 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:631:5: NULL_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					NULL_LITERAL116=(Token)match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_literal2491);
					NULL_LITERAL116_tree = (CommonTree)adaptor.create(NULL_LITERAL116);
					adaptor.addChild(root_0, NULL_LITERAL116_tree);

					}
					break;
				case 11 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:632:5: array_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_array_literal_in_literal2497);
					array_literal117=array_literal();
					state._fsp--;

					adaptor.addChild(root_0, array_literal117.getTree());

					}
					break;
				case 12 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:633:5: subannotation
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_subannotation_in_literal2503);
					subannotation118=subannotation();
					state._fsp--;

					adaptor.addChild(root_0, subannotation118.getTree());

					}
					break;
				case 13 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:634:5: type_field_method_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_type_field_method_literal_in_literal2509);
					type_field_method_literal119=type_field_method_literal();
					state._fsp--;

					adaptor.addChild(root_0, type_field_method_literal119.getTree());

					}
					break;
				case 14 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:635:5: enum_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_enum_literal_in_literal2515);
					enum_literal120=enum_literal();
					state._fsp--;

					adaptor.addChild(root_0, enum_literal120.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "literal"


	public static class parsed_integer_literal_return extends ParserRuleReturnScope {
		public int value;
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "parsed_integer_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:637:1: parsed_integer_literal returns [int value] : integer_literal ;
	public final smaliParser.parsed_integer_literal_return parsed_integer_literal() throws RecognitionException {
		smaliParser.parsed_integer_literal_return retval = new smaliParser.parsed_integer_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		ParserRuleReturnScope integer_literal121 =null;


		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:638:3: ( integer_literal )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:638:5: integer_literal
			{
			root_0 = (CommonTree)adaptor.nil();


			pushFollow(FOLLOW_integer_literal_in_parsed_integer_literal2528);
			integer_literal121=integer_literal();
			state._fsp--;

			adaptor.addChild(root_0, integer_literal121.getTree());

			 retval.value = LiteralTools.parseInt((integer_literal121!=null?input.toString(integer_literal121.start,integer_literal121.stop):null));
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "parsed_integer_literal"


	public static class integral_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "integral_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:640:1: integral_literal : ( LONG_LITERAL | integer_literal | SHORT_LITERAL | CHAR_LITERAL | BYTE_LITERAL );
	public final smaliParser.integral_literal_return integral_literal() throws RecognitionException {
		smaliParser.integral_literal_return retval = new smaliParser.integral_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token LONG_LITERAL122=null;
		Token SHORT_LITERAL124=null;
		Token CHAR_LITERAL125=null;
		Token BYTE_LITERAL126=null;
		ParserRuleReturnScope integer_literal123 =null;

		CommonTree LONG_LITERAL122_tree=null;
		CommonTree SHORT_LITERAL124_tree=null;
		CommonTree CHAR_LITERAL125_tree=null;
		CommonTree BYTE_LITERAL126_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:641:3: ( LONG_LITERAL | integer_literal | SHORT_LITERAL | CHAR_LITERAL | BYTE_LITERAL )
			int alt20=5;
			switch ( input.LA(1) ) {
			case LONG_LITERAL:
				{
				alt20=1;
				}
				break;
			case NEGATIVE_INTEGER_LITERAL:
			case POSITIVE_INTEGER_LITERAL:
				{
				alt20=2;
				}
				break;
			case SHORT_LITERAL:
				{
				alt20=3;
				}
				break;
			case CHAR_LITERAL:
				{
				alt20=4;
				}
				break;
			case BYTE_LITERAL:
				{
				alt20=5;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 20, 0, input);
				throw nvae;
			}
			switch (alt20) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:641:5: LONG_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					LONG_LITERAL122=(Token)match(input,LONG_LITERAL,FOLLOW_LONG_LITERAL_in_integral_literal2540);
					LONG_LITERAL122_tree = (CommonTree)adaptor.create(LONG_LITERAL122);
					adaptor.addChild(root_0, LONG_LITERAL122_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:642:5: integer_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_integer_literal_in_integral_literal2546);
					integer_literal123=integer_literal();
					state._fsp--;

					adaptor.addChild(root_0, integer_literal123.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:643:5: SHORT_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					SHORT_LITERAL124=(Token)match(input,SHORT_LITERAL,FOLLOW_SHORT_LITERAL_in_integral_literal2552);
					SHORT_LITERAL124_tree = (CommonTree)adaptor.create(SHORT_LITERAL124);
					adaptor.addChild(root_0, SHORT_LITERAL124_tree);

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:644:5: CHAR_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					CHAR_LITERAL125=(Token)match(input,CHAR_LITERAL,FOLLOW_CHAR_LITERAL_in_integral_literal2558);
					CHAR_LITERAL125_tree = (CommonTree)adaptor.create(CHAR_LITERAL125);
					adaptor.addChild(root_0, CHAR_LITERAL125_tree);

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:645:5: BYTE_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BYTE_LITERAL126=(Token)match(input,BYTE_LITERAL,FOLLOW_BYTE_LITERAL_in_integral_literal2564);
					BYTE_LITERAL126_tree = (CommonTree)adaptor.create(BYTE_LITERAL126);
					adaptor.addChild(root_0, BYTE_LITERAL126_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "integral_literal"


	public static class fixed_32bit_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "fixed_32bit_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:647:1: fixed_32bit_literal : ( LONG_LITERAL | integer_literal | SHORT_LITERAL | BYTE_LITERAL | float_literal | CHAR_LITERAL | BOOL_LITERAL );
	public final smaliParser.fixed_32bit_literal_return fixed_32bit_literal() throws RecognitionException {
		smaliParser.fixed_32bit_literal_return retval = new smaliParser.fixed_32bit_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token LONG_LITERAL127=null;
		Token SHORT_LITERAL129=null;
		Token BYTE_LITERAL130=null;
		Token CHAR_LITERAL132=null;
		Token BOOL_LITERAL133=null;
		ParserRuleReturnScope integer_literal128 =null;
		ParserRuleReturnScope float_literal131 =null;

		CommonTree LONG_LITERAL127_tree=null;
		CommonTree SHORT_LITERAL129_tree=null;
		CommonTree BYTE_LITERAL130_tree=null;
		CommonTree CHAR_LITERAL132_tree=null;
		CommonTree BOOL_LITERAL133_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:648:3: ( LONG_LITERAL | integer_literal | SHORT_LITERAL | BYTE_LITERAL | float_literal | CHAR_LITERAL | BOOL_LITERAL )
			int alt21=7;
			switch ( input.LA(1) ) {
			case LONG_LITERAL:
				{
				alt21=1;
				}
				break;
			case NEGATIVE_INTEGER_LITERAL:
			case POSITIVE_INTEGER_LITERAL:
				{
				alt21=2;
				}
				break;
			case SHORT_LITERAL:
				{
				alt21=3;
				}
				break;
			case BYTE_LITERAL:
				{
				alt21=4;
				}
				break;
			case FLOAT_LITERAL:
			case FLOAT_LITERAL_OR_ID:
				{
				alt21=5;
				}
				break;
			case CHAR_LITERAL:
				{
				alt21=6;
				}
				break;
			case BOOL_LITERAL:
				{
				alt21=7;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 21, 0, input);
				throw nvae;
			}
			switch (alt21) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:648:5: LONG_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					LONG_LITERAL127=(Token)match(input,LONG_LITERAL,FOLLOW_LONG_LITERAL_in_fixed_32bit_literal2574);
					LONG_LITERAL127_tree = (CommonTree)adaptor.create(LONG_LITERAL127);
					adaptor.addChild(root_0, LONG_LITERAL127_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:649:5: integer_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_integer_literal_in_fixed_32bit_literal2580);
					integer_literal128=integer_literal();
					state._fsp--;

					adaptor.addChild(root_0, integer_literal128.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:650:5: SHORT_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					SHORT_LITERAL129=(Token)match(input,SHORT_LITERAL,FOLLOW_SHORT_LITERAL_in_fixed_32bit_literal2586);
					SHORT_LITERAL129_tree = (CommonTree)adaptor.create(SHORT_LITERAL129);
					adaptor.addChild(root_0, SHORT_LITERAL129_tree);

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:651:5: BYTE_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BYTE_LITERAL130=(Token)match(input,BYTE_LITERAL,FOLLOW_BYTE_LITERAL_in_fixed_32bit_literal2592);
					BYTE_LITERAL130_tree = (CommonTree)adaptor.create(BYTE_LITERAL130);
					adaptor.addChild(root_0, BYTE_LITERAL130_tree);

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:652:5: float_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_float_literal_in_fixed_32bit_literal2598);
					float_literal131=float_literal();
					state._fsp--;

					adaptor.addChild(root_0, float_literal131.getTree());

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:653:5: CHAR_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					CHAR_LITERAL132=(Token)match(input,CHAR_LITERAL,FOLLOW_CHAR_LITERAL_in_fixed_32bit_literal2604);
					CHAR_LITERAL132_tree = (CommonTree)adaptor.create(CHAR_LITERAL132);
					adaptor.addChild(root_0, CHAR_LITERAL132_tree);

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:654:5: BOOL_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BOOL_LITERAL133=(Token)match(input,BOOL_LITERAL,FOLLOW_BOOL_LITERAL_in_fixed_32bit_literal2610);
					BOOL_LITERAL133_tree = (CommonTree)adaptor.create(BOOL_LITERAL133);
					adaptor.addChild(root_0, BOOL_LITERAL133_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "fixed_32bit_literal"


	public static class fixed_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "fixed_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:656:1: fixed_literal : ( integer_literal | LONG_LITERAL | SHORT_LITERAL | BYTE_LITERAL | float_literal | double_literal | CHAR_LITERAL | BOOL_LITERAL );
	public final smaliParser.fixed_literal_return fixed_literal() throws RecognitionException {
		smaliParser.fixed_literal_return retval = new smaliParser.fixed_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token LONG_LITERAL135=null;
		Token SHORT_LITERAL136=null;
		Token BYTE_LITERAL137=null;
		Token CHAR_LITERAL140=null;
		Token BOOL_LITERAL141=null;
		ParserRuleReturnScope integer_literal134 =null;
		ParserRuleReturnScope float_literal138 =null;
		ParserRuleReturnScope double_literal139 =null;

		CommonTree LONG_LITERAL135_tree=null;
		CommonTree SHORT_LITERAL136_tree=null;
		CommonTree BYTE_LITERAL137_tree=null;
		CommonTree CHAR_LITERAL140_tree=null;
		CommonTree BOOL_LITERAL141_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:657:3: ( integer_literal | LONG_LITERAL | SHORT_LITERAL | BYTE_LITERAL | float_literal | double_literal | CHAR_LITERAL | BOOL_LITERAL )
			int alt22=8;
			switch ( input.LA(1) ) {
			case NEGATIVE_INTEGER_LITERAL:
			case POSITIVE_INTEGER_LITERAL:
				{
				alt22=1;
				}
				break;
			case LONG_LITERAL:
				{
				alt22=2;
				}
				break;
			case SHORT_LITERAL:
				{
				alt22=3;
				}
				break;
			case BYTE_LITERAL:
				{
				alt22=4;
				}
				break;
			case FLOAT_LITERAL:
			case FLOAT_LITERAL_OR_ID:
				{
				alt22=5;
				}
				break;
			case DOUBLE_LITERAL:
			case DOUBLE_LITERAL_OR_ID:
				{
				alt22=6;
				}
				break;
			case CHAR_LITERAL:
				{
				alt22=7;
				}
				break;
			case BOOL_LITERAL:
				{
				alt22=8;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 22, 0, input);
				throw nvae;
			}
			switch (alt22) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:657:5: integer_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_integer_literal_in_fixed_literal2620);
					integer_literal134=integer_literal();
					state._fsp--;

					adaptor.addChild(root_0, integer_literal134.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:658:5: LONG_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					LONG_LITERAL135=(Token)match(input,LONG_LITERAL,FOLLOW_LONG_LITERAL_in_fixed_literal2626);
					LONG_LITERAL135_tree = (CommonTree)adaptor.create(LONG_LITERAL135);
					adaptor.addChild(root_0, LONG_LITERAL135_tree);

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:659:5: SHORT_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					SHORT_LITERAL136=(Token)match(input,SHORT_LITERAL,FOLLOW_SHORT_LITERAL_in_fixed_literal2632);
					SHORT_LITERAL136_tree = (CommonTree)adaptor.create(SHORT_LITERAL136);
					adaptor.addChild(root_0, SHORT_LITERAL136_tree);

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:660:5: BYTE_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BYTE_LITERAL137=(Token)match(input,BYTE_LITERAL,FOLLOW_BYTE_LITERAL_in_fixed_literal2638);
					BYTE_LITERAL137_tree = (CommonTree)adaptor.create(BYTE_LITERAL137);
					adaptor.addChild(root_0, BYTE_LITERAL137_tree);

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:661:5: float_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_float_literal_in_fixed_literal2644);
					float_literal138=float_literal();
					state._fsp--;

					adaptor.addChild(root_0, float_literal138.getTree());

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:662:5: double_literal
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_double_literal_in_fixed_literal2650);
					double_literal139=double_literal();
					state._fsp--;

					adaptor.addChild(root_0, double_literal139.getTree());

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:663:5: CHAR_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					CHAR_LITERAL140=(Token)match(input,CHAR_LITERAL,FOLLOW_CHAR_LITERAL_in_fixed_literal2656);
					CHAR_LITERAL140_tree = (CommonTree)adaptor.create(CHAR_LITERAL140);
					adaptor.addChild(root_0, CHAR_LITERAL140_tree);

					}
					break;
				case 8 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:664:5: BOOL_LITERAL
					{
					root_0 = (CommonTree)adaptor.nil();


					BOOL_LITERAL141=(Token)match(input,BOOL_LITERAL,FOLLOW_BOOL_LITERAL_in_fixed_literal2662);
					BOOL_LITERAL141_tree = (CommonTree)adaptor.create(BOOL_LITERAL141);
					adaptor.addChild(root_0, BOOL_LITERAL141_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "fixed_literal"


	public static class array_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "array_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:666:1: array_literal : OPEN_BRACE ( literal ( COMMA literal )* |) CLOSE_BRACE -> ^( I_ENCODED_ARRAY[$start, \"I_ENCODED_ARRAY\"] ( literal )* ) ;
	public final smaliParser.array_literal_return array_literal() throws RecognitionException {
		smaliParser.array_literal_return retval = new smaliParser.array_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token OPEN_BRACE142=null;
		Token COMMA144=null;
		Token CLOSE_BRACE146=null;
		ParserRuleReturnScope literal143 =null;
		ParserRuleReturnScope literal145 =null;

		CommonTree OPEN_BRACE142_tree=null;
		CommonTree COMMA144_tree=null;
		CommonTree CLOSE_BRACE146_tree=null;
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleSubtreeStream stream_literal=new RewriteRuleSubtreeStream(adaptor,"rule literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:3: ( OPEN_BRACE ( literal ( COMMA literal )* |) CLOSE_BRACE -> ^( I_ENCODED_ARRAY[$start, \"I_ENCODED_ARRAY\"] ( literal )* ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:5: OPEN_BRACE ( literal ( COMMA literal )* |) CLOSE_BRACE
			{
			OPEN_BRACE142=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_array_literal2672);
			stream_OPEN_BRACE.add(OPEN_BRACE142);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:16: ( literal ( COMMA literal )* |)
			int alt24=2;
			int LA24_0 = input.LA(1);
			if ( (LA24_0==ACCESS_SPEC||LA24_0==ANNOTATION_VISIBILITY||LA24_0==ARRAY_DESCRIPTOR||(LA24_0 >= BOOL_LITERAL && LA24_0 <= BYTE_LITERAL)||(LA24_0 >= CHAR_LITERAL && LA24_0 <= CLASS_DESCRIPTOR)||(LA24_0 >= DOUBLE_LITERAL && LA24_0 <= DOUBLE_LITERAL_OR_ID)||LA24_0==ENUM_DIRECTIVE||(LA24_0 >= FLOAT_LITERAL && LA24_0 <= FLOAT_LITERAL_OR_ID)||(LA24_0 >= INSTRUCTION_FORMAT10t && LA24_0 <= INSTRUCTION_FORMAT10x_ODEX)||LA24_0==INSTRUCTION_FORMAT11x||LA24_0==INSTRUCTION_FORMAT12x_OR_ID||(LA24_0 >= INSTRUCTION_FORMAT21c_FIELD && LA24_0 <= INSTRUCTION_FORMAT21c_TYPE)||LA24_0==INSTRUCTION_FORMAT21t||(LA24_0 >= INSTRUCTION_FORMAT22c_FIELD && LA24_0 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA24_0 >= INSTRUCTION_FORMAT22s_OR_ID && LA24_0 <= INSTRUCTION_FORMAT22t)||LA24_0==INSTRUCTION_FORMAT23x||(LA24_0 >= INSTRUCTION_FORMAT31i_OR_ID && LA24_0 <= INSTRUCTION_FORMAT31t)||(LA24_0 >= INSTRUCTION_FORMAT35c_METHOD && LA24_0 <= INSTRUCTION_FORMAT35ms_METHOD)||LA24_0==INSTRUCTION_FORMAT51l||(LA24_0 >= LONG_LITERAL && LA24_0 <= MEMBER_NAME)||(LA24_0 >= NEGATIVE_INTEGER_LITERAL && LA24_0 <= OPEN_BRACE)||LA24_0==PARAM_LIST_OR_ID_START||(LA24_0 >= POSITIVE_INTEGER_LITERAL && LA24_0 <= PRIMITIVE_TYPE)||LA24_0==REGISTER||(LA24_0 >= SHORT_LITERAL && LA24_0 <= SIMPLE_NAME)||(LA24_0 >= STRING_LITERAL && LA24_0 <= SUBANNOTATION_DIRECTIVE)||(LA24_0 >= VERIFICATION_ERROR_TYPE && LA24_0 <= VOID_TYPE)) ) {
				alt24=1;
			}
			else if ( (LA24_0==CLOSE_BRACE) ) {
				alt24=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 24, 0, input);
				throw nvae;
			}

			switch (alt24) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:17: literal ( COMMA literal )*
					{
					pushFollow(FOLLOW_literal_in_array_literal2675);
					literal143=literal();
					state._fsp--;

					stream_literal.add(literal143.getTree());
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:25: ( COMMA literal )*
					loop23:
					while (true) {
						int alt23=2;
						int LA23_0 = input.LA(1);
						if ( (LA23_0==COMMA) ) {
							alt23=1;
						}

						switch (alt23) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:26: COMMA literal
							{
							COMMA144=(Token)match(input,COMMA,FOLLOW_COMMA_in_array_literal2678);
							stream_COMMA.add(COMMA144);

							pushFollow(FOLLOW_literal_in_array_literal2680);
							literal145=literal();
							state._fsp--;

							stream_literal.add(literal145.getTree());
							}
							break;

						default :
							break loop23;
						}
					}

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:667:44:
					{
					}
					break;

			}

			CLOSE_BRACE146=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_array_literal2688);
			stream_CLOSE_BRACE.add(CLOSE_BRACE146);

			// AST REWRITE
			// elements: literal
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 668:5: -> ^( I_ENCODED_ARRAY[$start, \"I_ENCODED_ARRAY\"] ( literal )* )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:668:8: ^( I_ENCODED_ARRAY[$start, \"I_ENCODED_ARRAY\"] ( literal )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ENCODED_ARRAY, (retval.start), "I_ENCODED_ARRAY"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:668:53: ( literal )*
				while ( stream_literal.hasNext() ) {
					adaptor.addChild(root_1, stream_literal.nextTree());
				}
				stream_literal.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "array_literal"


	public static class annotation_element_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "annotation_element"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:670:1: annotation_element : simple_name EQUAL literal -> ^( I_ANNOTATION_ELEMENT[$start, \"I_ANNOTATION_ELEMENT\"] simple_name literal ) ;
	public final smaliParser.annotation_element_return annotation_element() throws RecognitionException {
		smaliParser.annotation_element_return retval = new smaliParser.annotation_element_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token EQUAL148=null;
		ParserRuleReturnScope simple_name147 =null;
		ParserRuleReturnScope literal149 =null;

		CommonTree EQUAL148_tree=null;
		RewriteRuleTokenStream stream_EQUAL=new RewriteRuleTokenStream(adaptor,"token EQUAL");
		RewriteRuleSubtreeStream stream_simple_name=new RewriteRuleSubtreeStream(adaptor,"rule simple_name");
		RewriteRuleSubtreeStream stream_literal=new RewriteRuleSubtreeStream(adaptor,"rule literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:671:3: ( simple_name EQUAL literal -> ^( I_ANNOTATION_ELEMENT[$start, \"I_ANNOTATION_ELEMENT\"] simple_name literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:671:5: simple_name EQUAL literal
			{
			pushFollow(FOLLOW_simple_name_in_annotation_element2712);
			simple_name147=simple_name();
			state._fsp--;

			stream_simple_name.add(simple_name147.getTree());
			EQUAL148=(Token)match(input,EQUAL,FOLLOW_EQUAL_in_annotation_element2714);
			stream_EQUAL.add(EQUAL148);

			pushFollow(FOLLOW_literal_in_annotation_element2716);
			literal149=literal();
			state._fsp--;

			stream_literal.add(literal149.getTree());
			// AST REWRITE
			// elements: literal, simple_name
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 672:5: -> ^( I_ANNOTATION_ELEMENT[$start, \"I_ANNOTATION_ELEMENT\"] simple_name literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:672:8: ^( I_ANNOTATION_ELEMENT[$start, \"I_ANNOTATION_ELEMENT\"] simple_name literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATION_ELEMENT, (retval.start), "I_ANNOTATION_ELEMENT"), root_1);
				adaptor.addChild(root_1, stream_simple_name.nextTree());
				adaptor.addChild(root_1, stream_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "annotation_element"


	public static class annotation_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "annotation"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:674:1: annotation : ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY CLASS_DESCRIPTOR ( annotation_element )* END_ANNOTATION_DIRECTIVE -> ^( I_ANNOTATION[$start, \"I_ANNOTATION\"] ANNOTATION_VISIBILITY ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) ) ;
	public final smaliParser.annotation_return annotation() throws RecognitionException {
		smaliParser.annotation_return retval = new smaliParser.annotation_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ANNOTATION_DIRECTIVE150=null;
		Token ANNOTATION_VISIBILITY151=null;
		Token CLASS_DESCRIPTOR152=null;
		Token END_ANNOTATION_DIRECTIVE154=null;
		ParserRuleReturnScope annotation_element153 =null;

		CommonTree ANNOTATION_DIRECTIVE150_tree=null;
		CommonTree ANNOTATION_VISIBILITY151_tree=null;
		CommonTree CLASS_DESCRIPTOR152_tree=null;
		CommonTree END_ANNOTATION_DIRECTIVE154_tree=null;
		RewriteRuleTokenStream stream_ANNOTATION_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token ANNOTATION_DIRECTIVE");
		RewriteRuleTokenStream stream_END_ANNOTATION_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_ANNOTATION_DIRECTIVE");
		RewriteRuleTokenStream stream_ANNOTATION_VISIBILITY=new RewriteRuleTokenStream(adaptor,"token ANNOTATION_VISIBILITY");
		RewriteRuleTokenStream stream_CLASS_DESCRIPTOR=new RewriteRuleTokenStream(adaptor,"token CLASS_DESCRIPTOR");
		RewriteRuleSubtreeStream stream_annotation_element=new RewriteRuleSubtreeStream(adaptor,"rule annotation_element");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:675:3: ( ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY CLASS_DESCRIPTOR ( annotation_element )* END_ANNOTATION_DIRECTIVE -> ^( I_ANNOTATION[$start, \"I_ANNOTATION\"] ANNOTATION_VISIBILITY ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:675:5: ANNOTATION_DIRECTIVE ANNOTATION_VISIBILITY CLASS_DESCRIPTOR ( annotation_element )* END_ANNOTATION_DIRECTIVE
			{
			ANNOTATION_DIRECTIVE150=(Token)match(input,ANNOTATION_DIRECTIVE,FOLLOW_ANNOTATION_DIRECTIVE_in_annotation2741);
			stream_ANNOTATION_DIRECTIVE.add(ANNOTATION_DIRECTIVE150);

			ANNOTATION_VISIBILITY151=(Token)match(input,ANNOTATION_VISIBILITY,FOLLOW_ANNOTATION_VISIBILITY_in_annotation2743);
			stream_ANNOTATION_VISIBILITY.add(ANNOTATION_VISIBILITY151);

			CLASS_DESCRIPTOR152=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_annotation2745);
			stream_CLASS_DESCRIPTOR.add(CLASS_DESCRIPTOR152);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:676:5: ( annotation_element )*
			loop25:
			while (true) {
				int alt25=2;
				int LA25_0 = input.LA(1);
				if ( (LA25_0==ACCESS_SPEC||LA25_0==ANNOTATION_VISIBILITY||LA25_0==BOOL_LITERAL||LA25_0==DOUBLE_LITERAL_OR_ID||LA25_0==FLOAT_LITERAL_OR_ID||(LA25_0 >= INSTRUCTION_FORMAT10t && LA25_0 <= INSTRUCTION_FORMAT10x_ODEX)||LA25_0==INSTRUCTION_FORMAT11x||LA25_0==INSTRUCTION_FORMAT12x_OR_ID||(LA25_0 >= INSTRUCTION_FORMAT21c_FIELD && LA25_0 <= INSTRUCTION_FORMAT21c_TYPE)||LA25_0==INSTRUCTION_FORMAT21t||(LA25_0 >= INSTRUCTION_FORMAT22c_FIELD && LA25_0 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA25_0 >= INSTRUCTION_FORMAT22s_OR_ID && LA25_0 <= INSTRUCTION_FORMAT22t)||LA25_0==INSTRUCTION_FORMAT23x||(LA25_0 >= INSTRUCTION_FORMAT31i_OR_ID && LA25_0 <= INSTRUCTION_FORMAT31t)||(LA25_0 >= INSTRUCTION_FORMAT35c_METHOD && LA25_0 <= INSTRUCTION_FORMAT35ms_METHOD)||LA25_0==INSTRUCTION_FORMAT51l||(LA25_0 >= NEGATIVE_INTEGER_LITERAL && LA25_0 <= NULL_LITERAL)||LA25_0==PARAM_LIST_OR_ID_START||(LA25_0 >= POSITIVE_INTEGER_LITERAL && LA25_0 <= PRIMITIVE_TYPE)||LA25_0==REGISTER||LA25_0==SIMPLE_NAME||(LA25_0 >= VERIFICATION_ERROR_TYPE && LA25_0 <= VOID_TYPE)) ) {
					alt25=1;
				}

				switch (alt25) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:676:5: annotation_element
					{
					pushFollow(FOLLOW_annotation_element_in_annotation2751);
					annotation_element153=annotation_element();
					state._fsp--;

					stream_annotation_element.add(annotation_element153.getTree());
					}
					break;

				default :
					break loop25;
				}
			}

			END_ANNOTATION_DIRECTIVE154=(Token)match(input,END_ANNOTATION_DIRECTIVE,FOLLOW_END_ANNOTATION_DIRECTIVE_in_annotation2754);
			stream_END_ANNOTATION_DIRECTIVE.add(END_ANNOTATION_DIRECTIVE154);

			// AST REWRITE
			// elements: CLASS_DESCRIPTOR, ANNOTATION_VISIBILITY, annotation_element
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 677:5: -> ^( I_ANNOTATION[$start, \"I_ANNOTATION\"] ANNOTATION_VISIBILITY ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:677:8: ^( I_ANNOTATION[$start, \"I_ANNOTATION\"] ANNOTATION_VISIBILITY ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATION, (retval.start), "I_ANNOTATION"), root_1);
				adaptor.addChild(root_1, stream_ANNOTATION_VISIBILITY.nextNode());
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:677:69: ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SUBANNOTATION, (retval.start), "I_SUBANNOTATION"), root_2);
				adaptor.addChild(root_2, stream_CLASS_DESCRIPTOR.nextNode());
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:677:131: ( annotation_element )*
				while ( stream_annotation_element.hasNext() ) {
					adaptor.addChild(root_2, stream_annotation_element.nextTree());
				}
				stream_annotation_element.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "annotation"


	public static class subannotation_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "subannotation"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:679:1: subannotation : SUBANNOTATION_DIRECTIVE CLASS_DESCRIPTOR ( annotation_element )* END_SUBANNOTATION_DIRECTIVE -> ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) ;
	public final smaliParser.subannotation_return subannotation() throws RecognitionException {
		smaliParser.subannotation_return retval = new smaliParser.subannotation_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SUBANNOTATION_DIRECTIVE155=null;
		Token CLASS_DESCRIPTOR156=null;
		Token END_SUBANNOTATION_DIRECTIVE158=null;
		ParserRuleReturnScope annotation_element157 =null;

		CommonTree SUBANNOTATION_DIRECTIVE155_tree=null;
		CommonTree CLASS_DESCRIPTOR156_tree=null;
		CommonTree END_SUBANNOTATION_DIRECTIVE158_tree=null;
		RewriteRuleTokenStream stream_SUBANNOTATION_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token SUBANNOTATION_DIRECTIVE");
		RewriteRuleTokenStream stream_END_SUBANNOTATION_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_SUBANNOTATION_DIRECTIVE");
		RewriteRuleTokenStream stream_CLASS_DESCRIPTOR=new RewriteRuleTokenStream(adaptor,"token CLASS_DESCRIPTOR");
		RewriteRuleSubtreeStream stream_annotation_element=new RewriteRuleSubtreeStream(adaptor,"rule annotation_element");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:680:3: ( SUBANNOTATION_DIRECTIVE CLASS_DESCRIPTOR ( annotation_element )* END_SUBANNOTATION_DIRECTIVE -> ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:680:5: SUBANNOTATION_DIRECTIVE CLASS_DESCRIPTOR ( annotation_element )* END_SUBANNOTATION_DIRECTIVE
			{
			SUBANNOTATION_DIRECTIVE155=(Token)match(input,SUBANNOTATION_DIRECTIVE,FOLLOW_SUBANNOTATION_DIRECTIVE_in_subannotation2787);
			stream_SUBANNOTATION_DIRECTIVE.add(SUBANNOTATION_DIRECTIVE155);

			CLASS_DESCRIPTOR156=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_subannotation2789);
			stream_CLASS_DESCRIPTOR.add(CLASS_DESCRIPTOR156);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:680:46: ( annotation_element )*
			loop26:
			while (true) {
				int alt26=2;
				int LA26_0 = input.LA(1);
				if ( (LA26_0==ACCESS_SPEC||LA26_0==ANNOTATION_VISIBILITY||LA26_0==BOOL_LITERAL||LA26_0==DOUBLE_LITERAL_OR_ID||LA26_0==FLOAT_LITERAL_OR_ID||(LA26_0 >= INSTRUCTION_FORMAT10t && LA26_0 <= INSTRUCTION_FORMAT10x_ODEX)||LA26_0==INSTRUCTION_FORMAT11x||LA26_0==INSTRUCTION_FORMAT12x_OR_ID||(LA26_0 >= INSTRUCTION_FORMAT21c_FIELD && LA26_0 <= INSTRUCTION_FORMAT21c_TYPE)||LA26_0==INSTRUCTION_FORMAT21t||(LA26_0 >= INSTRUCTION_FORMAT22c_FIELD && LA26_0 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA26_0 >= INSTRUCTION_FORMAT22s_OR_ID && LA26_0 <= INSTRUCTION_FORMAT22t)||LA26_0==INSTRUCTION_FORMAT23x||(LA26_0 >= INSTRUCTION_FORMAT31i_OR_ID && LA26_0 <= INSTRUCTION_FORMAT31t)||(LA26_0 >= INSTRUCTION_FORMAT35c_METHOD && LA26_0 <= INSTRUCTION_FORMAT35ms_METHOD)||LA26_0==INSTRUCTION_FORMAT51l||(LA26_0 >= NEGATIVE_INTEGER_LITERAL && LA26_0 <= NULL_LITERAL)||LA26_0==PARAM_LIST_OR_ID_START||(LA26_0 >= POSITIVE_INTEGER_LITERAL && LA26_0 <= PRIMITIVE_TYPE)||LA26_0==REGISTER||LA26_0==SIMPLE_NAME||(LA26_0 >= VERIFICATION_ERROR_TYPE && LA26_0 <= VOID_TYPE)) ) {
					alt26=1;
				}

				switch (alt26) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:680:46: annotation_element
					{
					pushFollow(FOLLOW_annotation_element_in_subannotation2791);
					annotation_element157=annotation_element();
					state._fsp--;

					stream_annotation_element.add(annotation_element157.getTree());
					}
					break;

				default :
					break loop26;
				}
			}

			END_SUBANNOTATION_DIRECTIVE158=(Token)match(input,END_SUBANNOTATION_DIRECTIVE,FOLLOW_END_SUBANNOTATION_DIRECTIVE_in_subannotation2794);
			stream_END_SUBANNOTATION_DIRECTIVE.add(END_SUBANNOTATION_DIRECTIVE158);

			// AST REWRITE
			// elements: annotation_element, CLASS_DESCRIPTOR
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 681:5: -> ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:681:8: ^( I_SUBANNOTATION[$start, \"I_SUBANNOTATION\"] CLASS_DESCRIPTOR ( annotation_element )* )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SUBANNOTATION, (retval.start), "I_SUBANNOTATION"), root_1);
				adaptor.addChild(root_1, stream_CLASS_DESCRIPTOR.nextNode());
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:681:70: ( annotation_element )*
				while ( stream_annotation_element.hasNext() ) {
					adaptor.addChild(root_1, stream_annotation_element.nextTree());
				}
				stream_annotation_element.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "subannotation"


	public static class enum_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "enum_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:683:1: enum_literal : ENUM_DIRECTIVE reference_type_descriptor ARROW simple_name COLON reference_type_descriptor -> ^( I_ENCODED_ENUM reference_type_descriptor simple_name reference_type_descriptor ) ;
	public final smaliParser.enum_literal_return enum_literal() throws RecognitionException {
		smaliParser.enum_literal_return retval = new smaliParser.enum_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ENUM_DIRECTIVE159=null;
		Token ARROW161=null;
		Token COLON163=null;
		ParserRuleReturnScope reference_type_descriptor160 =null;
		ParserRuleReturnScope simple_name162 =null;
		ParserRuleReturnScope reference_type_descriptor164 =null;

		CommonTree ENUM_DIRECTIVE159_tree=null;
		CommonTree ARROW161_tree=null;
		CommonTree COLON163_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleTokenStream stream_ENUM_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token ENUM_DIRECTIVE");
		RewriteRuleTokenStream stream_ARROW=new RewriteRuleTokenStream(adaptor,"token ARROW");
		RewriteRuleSubtreeStream stream_reference_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule reference_type_descriptor");
		RewriteRuleSubtreeStream stream_simple_name=new RewriteRuleSubtreeStream(adaptor,"rule simple_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:684:3: ( ENUM_DIRECTIVE reference_type_descriptor ARROW simple_name COLON reference_type_descriptor -> ^( I_ENCODED_ENUM reference_type_descriptor simple_name reference_type_descriptor ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:684:5: ENUM_DIRECTIVE reference_type_descriptor ARROW simple_name COLON reference_type_descriptor
			{
			ENUM_DIRECTIVE159=(Token)match(input,ENUM_DIRECTIVE,FOLLOW_ENUM_DIRECTIVE_in_enum_literal2820);
			stream_ENUM_DIRECTIVE.add(ENUM_DIRECTIVE159);

			pushFollow(FOLLOW_reference_type_descriptor_in_enum_literal2822);
			reference_type_descriptor160=reference_type_descriptor();
			state._fsp--;

			stream_reference_type_descriptor.add(reference_type_descriptor160.getTree());
			ARROW161=(Token)match(input,ARROW,FOLLOW_ARROW_in_enum_literal2824);
			stream_ARROW.add(ARROW161);

			pushFollow(FOLLOW_simple_name_in_enum_literal2826);
			simple_name162=simple_name();
			state._fsp--;

			stream_simple_name.add(simple_name162.getTree());
			COLON163=(Token)match(input,COLON,FOLLOW_COLON_in_enum_literal2828);
			stream_COLON.add(COLON163);

			pushFollow(FOLLOW_reference_type_descriptor_in_enum_literal2830);
			reference_type_descriptor164=reference_type_descriptor();
			state._fsp--;

			stream_reference_type_descriptor.add(reference_type_descriptor164.getTree());
			// AST REWRITE
			// elements: simple_name, reference_type_descriptor, reference_type_descriptor
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 685:3: -> ^( I_ENCODED_ENUM reference_type_descriptor simple_name reference_type_descriptor )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:685:6: ^( I_ENCODED_ENUM reference_type_descriptor simple_name reference_type_descriptor )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ENCODED_ENUM, "I_ENCODED_ENUM"), root_1);
				adaptor.addChild(root_1, stream_reference_type_descriptor.nextTree());
				adaptor.addChild(root_1, stream_simple_name.nextTree());
				adaptor.addChild(root_1, stream_reference_type_descriptor.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "enum_literal"


	public static class type_field_method_literal_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "type_field_method_literal"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:687:1: type_field_method_literal : ( reference_type_descriptor | ( ( reference_type_descriptor ARROW )? ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) ) ) | PRIMITIVE_TYPE | VOID_TYPE );
	public final smaliParser.type_field_method_literal_return type_field_method_literal() throws RecognitionException {
		smaliParser.type_field_method_literal_return retval = new smaliParser.type_field_method_literal_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ARROW167=null;
		Token COLON169=null;
		Token PRIMITIVE_TYPE173=null;
		Token VOID_TYPE174=null;
		ParserRuleReturnScope reference_type_descriptor165 =null;
		ParserRuleReturnScope reference_type_descriptor166 =null;
		ParserRuleReturnScope member_name168 =null;
		ParserRuleReturnScope nonvoid_type_descriptor170 =null;
		ParserRuleReturnScope member_name171 =null;
		ParserRuleReturnScope method_prototype172 =null;

		CommonTree ARROW167_tree=null;
		CommonTree COLON169_tree=null;
		CommonTree PRIMITIVE_TYPE173_tree=null;
		CommonTree VOID_TYPE174_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleTokenStream stream_ARROW=new RewriteRuleTokenStream(adaptor,"token ARROW");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_method_prototype=new RewriteRuleSubtreeStream(adaptor,"rule method_prototype");
		RewriteRuleSubtreeStream stream_reference_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule reference_type_descriptor");
		RewriteRuleSubtreeStream stream_member_name=new RewriteRuleSubtreeStream(adaptor,"rule member_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:688:3: ( reference_type_descriptor | ( ( reference_type_descriptor ARROW )? ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) ) ) | PRIMITIVE_TYPE | VOID_TYPE )
			int alt29=4;
			switch ( input.LA(1) ) {
			case ARRAY_DESCRIPTOR:
			case CLASS_DESCRIPTOR:
				{
				int LA29_1 = input.LA(2);
				if ( (LA29_1==EOF||(LA29_1 >= ACCESS_SPEC && LA29_1 <= ANNOTATION_VISIBILITY)||LA29_1==BOOL_LITERAL||(LA29_1 >= CLASS_DIRECTIVE && LA29_1 <= CLOSE_BRACE)||LA29_1==COMMA||(LA29_1 >= DOUBLE_LITERAL_OR_ID && LA29_1 <= END_ANNOTATION_DIRECTIVE)||LA29_1==END_FIELD_DIRECTIVE||LA29_1==END_SUBANNOTATION_DIRECTIVE||LA29_1==FIELD_DIRECTIVE||(LA29_1 >= FLOAT_LITERAL_OR_ID && LA29_1 <= IMPLEMENTS_DIRECTIVE)||(LA29_1 >= INSTRUCTION_FORMAT10t && LA29_1 <= INSTRUCTION_FORMAT10x_ODEX)||LA29_1==INSTRUCTION_FORMAT11x||LA29_1==INSTRUCTION_FORMAT12x_OR_ID||(LA29_1 >= INSTRUCTION_FORMAT21c_FIELD && LA29_1 <= INSTRUCTION_FORMAT21c_TYPE)||LA29_1==INSTRUCTION_FORMAT21t||(LA29_1 >= INSTRUCTION_FORMAT22c_FIELD && LA29_1 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA29_1 >= INSTRUCTION_FORMAT22s_OR_ID && LA29_1 <= INSTRUCTION_FORMAT22t)||LA29_1==INSTRUCTION_FORMAT23x||(LA29_1 >= INSTRUCTION_FORMAT31i_OR_ID && LA29_1 <= INSTRUCTION_FORMAT31t)||(LA29_1 >= INSTRUCTION_FORMAT35c_METHOD && LA29_1 <= INSTRUCTION_FORMAT35ms_METHOD)||LA29_1==INSTRUCTION_FORMAT51l||(LA29_1 >= METHOD_DIRECTIVE && LA29_1 <= NULL_LITERAL)||LA29_1==PARAM_LIST_OR_ID_START||(LA29_1 >= POSITIVE_INTEGER_LITERAL && LA29_1 <= PRIMITIVE_TYPE)||LA29_1==REGISTER||(LA29_1 >= SIMPLE_NAME && LA29_1 <= SOURCE_DIRECTIVE)||(LA29_1 >= SUPER_DIRECTIVE && LA29_1 <= VOID_TYPE)) ) {
					alt29=1;
				}
				else if ( (LA29_1==ARROW) ) {
					alt29=2;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 29, 1, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case ACCESS_SPEC:
			case ANNOTATION_VISIBILITY:
			case BOOL_LITERAL:
			case DOUBLE_LITERAL_OR_ID:
			case FLOAT_LITERAL_OR_ID:
			case INSTRUCTION_FORMAT10t:
			case INSTRUCTION_FORMAT10x:
			case INSTRUCTION_FORMAT10x_ODEX:
			case INSTRUCTION_FORMAT11x:
			case INSTRUCTION_FORMAT12x_OR_ID:
			case INSTRUCTION_FORMAT21c_FIELD:
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
			case INSTRUCTION_FORMAT21c_STRING:
			case INSTRUCTION_FORMAT21c_TYPE:
			case INSTRUCTION_FORMAT21t:
			case INSTRUCTION_FORMAT22c_FIELD:
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
			case INSTRUCTION_FORMAT22c_TYPE:
			case INSTRUCTION_FORMAT22cs_FIELD:
			case INSTRUCTION_FORMAT22s_OR_ID:
			case INSTRUCTION_FORMAT22t:
			case INSTRUCTION_FORMAT23x:
			case INSTRUCTION_FORMAT31i_OR_ID:
			case INSTRUCTION_FORMAT31t:
			case INSTRUCTION_FORMAT35c_METHOD:
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
			case INSTRUCTION_FORMAT35c_TYPE:
			case INSTRUCTION_FORMAT35mi_METHOD:
			case INSTRUCTION_FORMAT35ms_METHOD:
			case INSTRUCTION_FORMAT51l:
			case MEMBER_NAME:
			case NEGATIVE_INTEGER_LITERAL:
			case NULL_LITERAL:
			case PARAM_LIST_OR_ID_START:
			case POSITIVE_INTEGER_LITERAL:
			case REGISTER:
			case SIMPLE_NAME:
			case VERIFICATION_ERROR_TYPE:
				{
				alt29=2;
				}
				break;
			case PRIMITIVE_TYPE:
				{
				int LA29_3 = input.LA(2);
				if ( (LA29_3==COLON||LA29_3==OPEN_PAREN) ) {
					alt29=2;
				}
				else if ( (LA29_3==EOF||(LA29_3 >= ACCESS_SPEC && LA29_3 <= ANNOTATION_VISIBILITY)||LA29_3==BOOL_LITERAL||(LA29_3 >= CLASS_DIRECTIVE && LA29_3 <= CLOSE_BRACE)||LA29_3==COMMA||(LA29_3 >= DOUBLE_LITERAL_OR_ID && LA29_3 <= END_ANNOTATION_DIRECTIVE)||LA29_3==END_FIELD_DIRECTIVE||LA29_3==END_SUBANNOTATION_DIRECTIVE||LA29_3==FIELD_DIRECTIVE||(LA29_3 >= FLOAT_LITERAL_OR_ID && LA29_3 <= IMPLEMENTS_DIRECTIVE)||(LA29_3 >= INSTRUCTION_FORMAT10t && LA29_3 <= INSTRUCTION_FORMAT10x_ODEX)||LA29_3==INSTRUCTION_FORMAT11x||LA29_3==INSTRUCTION_FORMAT12x_OR_ID||(LA29_3 >= INSTRUCTION_FORMAT21c_FIELD && LA29_3 <= INSTRUCTION_FORMAT21c_TYPE)||LA29_3==INSTRUCTION_FORMAT21t||(LA29_3 >= INSTRUCTION_FORMAT22c_FIELD && LA29_3 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA29_3 >= INSTRUCTION_FORMAT22s_OR_ID && LA29_3 <= INSTRUCTION_FORMAT22t)||LA29_3==INSTRUCTION_FORMAT23x||(LA29_3 >= INSTRUCTION_FORMAT31i_OR_ID && LA29_3 <= INSTRUCTION_FORMAT31t)||(LA29_3 >= INSTRUCTION_FORMAT35c_METHOD && LA29_3 <= INSTRUCTION_FORMAT35ms_METHOD)||LA29_3==INSTRUCTION_FORMAT51l||(LA29_3 >= METHOD_DIRECTIVE && LA29_3 <= NULL_LITERAL)||LA29_3==PARAM_LIST_OR_ID_START||(LA29_3 >= POSITIVE_INTEGER_LITERAL && LA29_3 <= PRIMITIVE_TYPE)||LA29_3==REGISTER||(LA29_3 >= SIMPLE_NAME && LA29_3 <= SOURCE_DIRECTIVE)||(LA29_3 >= SUPER_DIRECTIVE && LA29_3 <= VOID_TYPE)) ) {
					alt29=3;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 29, 3, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			case VOID_TYPE:
				{
				int LA29_4 = input.LA(2);
				if ( (LA29_4==COLON||LA29_4==OPEN_PAREN) ) {
					alt29=2;
				}
				else if ( (LA29_4==EOF||(LA29_4 >= ACCESS_SPEC && LA29_4 <= ANNOTATION_VISIBILITY)||LA29_4==BOOL_LITERAL||(LA29_4 >= CLASS_DIRECTIVE && LA29_4 <= CLOSE_BRACE)||LA29_4==COMMA||(LA29_4 >= DOUBLE_LITERAL_OR_ID && LA29_4 <= END_ANNOTATION_DIRECTIVE)||LA29_4==END_FIELD_DIRECTIVE||LA29_4==END_SUBANNOTATION_DIRECTIVE||LA29_4==FIELD_DIRECTIVE||(LA29_4 >= FLOAT_LITERAL_OR_ID && LA29_4 <= IMPLEMENTS_DIRECTIVE)||(LA29_4 >= INSTRUCTION_FORMAT10t && LA29_4 <= INSTRUCTION_FORMAT10x_ODEX)||LA29_4==INSTRUCTION_FORMAT11x||LA29_4==INSTRUCTION_FORMAT12x_OR_ID||(LA29_4 >= INSTRUCTION_FORMAT21c_FIELD && LA29_4 <= INSTRUCTION_FORMAT21c_TYPE)||LA29_4==INSTRUCTION_FORMAT21t||(LA29_4 >= INSTRUCTION_FORMAT22c_FIELD && LA29_4 <= INSTRUCTION_FORMAT22cs_FIELD)||(LA29_4 >= INSTRUCTION_FORMAT22s_OR_ID && LA29_4 <= INSTRUCTION_FORMAT22t)||LA29_4==INSTRUCTION_FORMAT23x||(LA29_4 >= INSTRUCTION_FORMAT31i_OR_ID && LA29_4 <= INSTRUCTION_FORMAT31t)||(LA29_4 >= INSTRUCTION_FORMAT35c_METHOD && LA29_4 <= INSTRUCTION_FORMAT35ms_METHOD)||LA29_4==INSTRUCTION_FORMAT51l||(LA29_4 >= METHOD_DIRECTIVE && LA29_4 <= NULL_LITERAL)||LA29_4==PARAM_LIST_OR_ID_START||(LA29_4 >= POSITIVE_INTEGER_LITERAL && LA29_4 <= PRIMITIVE_TYPE)||LA29_4==REGISTER||(LA29_4 >= SIMPLE_NAME && LA29_4 <= SOURCE_DIRECTIVE)||(LA29_4 >= SUPER_DIRECTIVE && LA29_4 <= VOID_TYPE)) ) {
					alt29=4;
				}

				else {
					int nvaeMark = input.mark();
					try {
						input.consume();
						NoViableAltException nvae =
							new NoViableAltException("", 29, 4, input);
						throw nvae;
					} finally {
						input.rewind(nvaeMark);
					}
				}

				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 29, 0, input);
				throw nvae;
			}
			switch (alt29) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:688:5: reference_type_descriptor
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_reference_type_descriptor_in_type_field_method_literal2854);
					reference_type_descriptor165=reference_type_descriptor();
					state._fsp--;

					adaptor.addChild(root_0, reference_type_descriptor165.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:689:5: ( ( reference_type_descriptor ARROW )? ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) ) )
					{
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:689:5: ( ( reference_type_descriptor ARROW )? ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) ) )
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:689:7: ( reference_type_descriptor ARROW )? ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) )
					{
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:689:7: ( reference_type_descriptor ARROW )?
					int alt27=2;
					int LA27_0 = input.LA(1);
					if ( (LA27_0==ARRAY_DESCRIPTOR||LA27_0==CLASS_DESCRIPTOR) ) {
						alt27=1;
					}
					switch (alt27) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:689:8: reference_type_descriptor ARROW
							{
							pushFollow(FOLLOW_reference_type_descriptor_in_type_field_method_literal2863);
							reference_type_descriptor166=reference_type_descriptor();
							state._fsp--;

							stream_reference_type_descriptor.add(reference_type_descriptor166.getTree());
							ARROW167=(Token)match(input,ARROW,FOLLOW_ARROW_in_type_field_method_literal2865);
							stream_ARROW.add(ARROW167);

							}
							break;

					}

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:690:7: ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) )
					int alt28=2;
					alt28 = dfa28.predict(input);
					switch (alt28) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:690:9: member_name COLON nonvoid_type_descriptor
							{
							pushFollow(FOLLOW_member_name_in_type_field_method_literal2877);
							member_name168=member_name();
							state._fsp--;

							stream_member_name.add(member_name168.getTree());
							COLON169=(Token)match(input,COLON,FOLLOW_COLON_in_type_field_method_literal2879);
							stream_COLON.add(COLON169);

							pushFollow(FOLLOW_nonvoid_type_descriptor_in_type_field_method_literal2881);
							nonvoid_type_descriptor170=nonvoid_type_descriptor();
							state._fsp--;

							stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor170.getTree());
							// AST REWRITE
							// elements: member_name, nonvoid_type_descriptor, reference_type_descriptor
							// token labels:
							// rule labels: retval
							// token list labels:
							// rule list labels:
							// wildcard labels:
							retval.tree = root_0;
							RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

							root_0 = (CommonTree)adaptor.nil();
							// 690:51: -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor )
							{
								// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:690:54: ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor )
								{
								CommonTree root_1 = (CommonTree)adaptor.nil();
								root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ENCODED_FIELD, "I_ENCODED_FIELD"), root_1);
								// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:690:72: ( reference_type_descriptor )?
								if ( stream_reference_type_descriptor.hasNext() ) {
									adaptor.addChild(root_1, stream_reference_type_descriptor.nextTree());
								}
								stream_reference_type_descriptor.reset();

								adaptor.addChild(root_1, stream_member_name.nextTree());
								adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
								adaptor.addChild(root_0, root_1);
								}

							}


							retval.tree = root_0;

							}
							break;
						case 2 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:691:9: member_name method_prototype
							{
							pushFollow(FOLLOW_member_name_in_type_field_method_literal2904);
							member_name171=member_name();
							state._fsp--;

							stream_member_name.add(member_name171.getTree());
							pushFollow(FOLLOW_method_prototype_in_type_field_method_literal2906);
							method_prototype172=method_prototype();
							state._fsp--;

							stream_method_prototype.add(method_prototype172.getTree());
							// AST REWRITE
							// elements: member_name, method_prototype, reference_type_descriptor
							// token labels:
							// rule labels: retval
							// token list labels:
							// rule list labels:
							// wildcard labels:
							retval.tree = root_0;
							RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

							root_0 = (CommonTree)adaptor.nil();
							// 691:38: -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype )
							{
								// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:691:41: ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype )
								{
								CommonTree root_1 = (CommonTree)adaptor.nil();
								root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ENCODED_METHOD, "I_ENCODED_METHOD"), root_1);
								// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:691:60: ( reference_type_descriptor )?
								if ( stream_reference_type_descriptor.hasNext() ) {
									adaptor.addChild(root_1, stream_reference_type_descriptor.nextTree());
								}
								stream_reference_type_descriptor.reset();

								adaptor.addChild(root_1, stream_member_name.nextTree());
								adaptor.addChild(root_1, stream_method_prototype.nextTree());
								adaptor.addChild(root_0, root_1);
								}

							}


							retval.tree = root_0;

							}
							break;

					}

					}

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:694:5: PRIMITIVE_TYPE
					{
					root_0 = (CommonTree)adaptor.nil();


					PRIMITIVE_TYPE173=(Token)match(input,PRIMITIVE_TYPE,FOLLOW_PRIMITIVE_TYPE_in_type_field_method_literal2939);
					PRIMITIVE_TYPE173_tree = (CommonTree)adaptor.create(PRIMITIVE_TYPE173);
					adaptor.addChild(root_0, PRIMITIVE_TYPE173_tree);

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:695:5: VOID_TYPE
					{
					root_0 = (CommonTree)adaptor.nil();


					VOID_TYPE174=(Token)match(input,VOID_TYPE,FOLLOW_VOID_TYPE_in_type_field_method_literal2945);
					VOID_TYPE174_tree = (CommonTree)adaptor.create(VOID_TYPE174);
					adaptor.addChild(root_0, VOID_TYPE174_tree);

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "type_field_method_literal"


	public static class method_reference_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "method_reference"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:697:1: method_reference : ( reference_type_descriptor ARROW )? member_name method_prototype -> ( reference_type_descriptor )? member_name method_prototype ;
	public final smaliParser.method_reference_return method_reference() throws RecognitionException {
		smaliParser.method_reference_return retval = new smaliParser.method_reference_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ARROW176=null;
		ParserRuleReturnScope reference_type_descriptor175 =null;
		ParserRuleReturnScope member_name177 =null;
		ParserRuleReturnScope method_prototype178 =null;

		CommonTree ARROW176_tree=null;
		RewriteRuleTokenStream stream_ARROW=new RewriteRuleTokenStream(adaptor,"token ARROW");
		RewriteRuleSubtreeStream stream_method_prototype=new RewriteRuleSubtreeStream(adaptor,"rule method_prototype");
		RewriteRuleSubtreeStream stream_reference_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule reference_type_descriptor");
		RewriteRuleSubtreeStream stream_member_name=new RewriteRuleSubtreeStream(adaptor,"rule member_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:698:3: ( ( reference_type_descriptor ARROW )? member_name method_prototype -> ( reference_type_descriptor )? member_name method_prototype )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:698:5: ( reference_type_descriptor ARROW )? member_name method_prototype
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:698:5: ( reference_type_descriptor ARROW )?
			int alt30=2;
			int LA30_0 = input.LA(1);
			if ( (LA30_0==ARRAY_DESCRIPTOR||LA30_0==CLASS_DESCRIPTOR) ) {
				alt30=1;
			}
			switch (alt30) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:698:6: reference_type_descriptor ARROW
					{
					pushFollow(FOLLOW_reference_type_descriptor_in_method_reference2956);
					reference_type_descriptor175=reference_type_descriptor();
					state._fsp--;

					stream_reference_type_descriptor.add(reference_type_descriptor175.getTree());
					ARROW176=(Token)match(input,ARROW,FOLLOW_ARROW_in_method_reference2958);
					stream_ARROW.add(ARROW176);

					}
					break;

			}

			pushFollow(FOLLOW_member_name_in_method_reference2962);
			member_name177=member_name();
			state._fsp--;

			stream_member_name.add(member_name177.getTree());
			pushFollow(FOLLOW_method_prototype_in_method_reference2964);
			method_prototype178=method_prototype();
			state._fsp--;

			stream_method_prototype.add(method_prototype178.getTree());
			// AST REWRITE
			// elements: member_name, method_prototype, reference_type_descriptor
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 699:3: -> ( reference_type_descriptor )? member_name method_prototype
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:699:6: ( reference_type_descriptor )?
				if ( stream_reference_type_descriptor.hasNext() ) {
					adaptor.addChild(root_0, stream_reference_type_descriptor.nextTree());
				}
				stream_reference_type_descriptor.reset();

				adaptor.addChild(root_0, stream_member_name.nextTree());
				adaptor.addChild(root_0, stream_method_prototype.nextTree());
			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "method_reference"


	public static class field_reference_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "field_reference"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:701:1: field_reference : ( reference_type_descriptor ARROW )? member_name COLON nonvoid_type_descriptor -> ( reference_type_descriptor )? member_name nonvoid_type_descriptor ;
	public final smaliParser.field_reference_return field_reference() throws RecognitionException {
		smaliParser.field_reference_return retval = new smaliParser.field_reference_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ARROW180=null;
		Token COLON182=null;
		ParserRuleReturnScope reference_type_descriptor179 =null;
		ParserRuleReturnScope member_name181 =null;
		ParserRuleReturnScope nonvoid_type_descriptor183 =null;

		CommonTree ARROW180_tree=null;
		CommonTree COLON182_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleTokenStream stream_ARROW=new RewriteRuleTokenStream(adaptor,"token ARROW");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_reference_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule reference_type_descriptor");
		RewriteRuleSubtreeStream stream_member_name=new RewriteRuleSubtreeStream(adaptor,"rule member_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:702:3: ( ( reference_type_descriptor ARROW )? member_name COLON nonvoid_type_descriptor -> ( reference_type_descriptor )? member_name nonvoid_type_descriptor )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:702:5: ( reference_type_descriptor ARROW )? member_name COLON nonvoid_type_descriptor
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:702:5: ( reference_type_descriptor ARROW )?
			int alt31=2;
			int LA31_0 = input.LA(1);
			if ( (LA31_0==ARRAY_DESCRIPTOR||LA31_0==CLASS_DESCRIPTOR) ) {
				alt31=1;
			}
			switch (alt31) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:702:6: reference_type_descriptor ARROW
					{
					pushFollow(FOLLOW_reference_type_descriptor_in_field_reference2986);
					reference_type_descriptor179=reference_type_descriptor();
					state._fsp--;

					stream_reference_type_descriptor.add(reference_type_descriptor179.getTree());
					ARROW180=(Token)match(input,ARROW,FOLLOW_ARROW_in_field_reference2988);
					stream_ARROW.add(ARROW180);

					}
					break;

			}

			pushFollow(FOLLOW_member_name_in_field_reference2992);
			member_name181=member_name();
			state._fsp--;

			stream_member_name.add(member_name181.getTree());
			COLON182=(Token)match(input,COLON,FOLLOW_COLON_in_field_reference2994);
			stream_COLON.add(COLON182);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_field_reference2996);
			nonvoid_type_descriptor183=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor183.getTree());
			// AST REWRITE
			// elements: nonvoid_type_descriptor, member_name, reference_type_descriptor
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 703:3: -> ( reference_type_descriptor )? member_name nonvoid_type_descriptor
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:703:6: ( reference_type_descriptor )?
				if ( stream_reference_type_descriptor.hasNext() ) {
					adaptor.addChild(root_0, stream_reference_type_descriptor.nextTree());
				}
				stream_reference_type_descriptor.reset();

				adaptor.addChild(root_0, stream_member_name.nextTree());
				adaptor.addChild(root_0, stream_nonvoid_type_descriptor.nextTree());
			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "field_reference"


	public static class label_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "label"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:705:1: label : COLON simple_name -> ^( I_LABEL[$COLON, \"I_LABEL\"] simple_name ) ;
	public final smaliParser.label_return label() throws RecognitionException {
		smaliParser.label_return retval = new smaliParser.label_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token COLON184=null;
		ParserRuleReturnScope simple_name185 =null;

		CommonTree COLON184_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleSubtreeStream stream_simple_name=new RewriteRuleSubtreeStream(adaptor,"rule simple_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:706:3: ( COLON simple_name -> ^( I_LABEL[$COLON, \"I_LABEL\"] simple_name ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:706:5: COLON simple_name
			{
			COLON184=(Token)match(input,COLON,FOLLOW_COLON_in_label3017);
			stream_COLON.add(COLON184);

			pushFollow(FOLLOW_simple_name_in_label3019);
			simple_name185=simple_name();
			state._fsp--;

			stream_simple_name.add(simple_name185.getTree());
			// AST REWRITE
			// elements: simple_name
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 706:23: -> ^( I_LABEL[$COLON, \"I_LABEL\"] simple_name )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:706:26: ^( I_LABEL[$COLON, \"I_LABEL\"] simple_name )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_LABEL, COLON184, "I_LABEL"), root_1);
				adaptor.addChild(root_1, stream_simple_name.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "label"


	public static class label_ref_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "label_ref"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:708:1: label_ref : COLON simple_name -> simple_name ;
	public final smaliParser.label_ref_return label_ref() throws RecognitionException {
		smaliParser.label_ref_return retval = new smaliParser.label_ref_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token COLON186=null;
		ParserRuleReturnScope simple_name187 =null;

		CommonTree COLON186_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleSubtreeStream stream_simple_name=new RewriteRuleSubtreeStream(adaptor,"rule simple_name");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:709:3: ( COLON simple_name -> simple_name )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:709:5: COLON simple_name
			{
			COLON186=(Token)match(input,COLON,FOLLOW_COLON_in_label_ref3038);
			stream_COLON.add(COLON186);

			pushFollow(FOLLOW_simple_name_in_label_ref3040);
			simple_name187=simple_name();
			state._fsp--;

			stream_simple_name.add(simple_name187.getTree());
			// AST REWRITE
			// elements: simple_name
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 709:23: -> simple_name
			{
				adaptor.addChild(root_0, stream_simple_name.nextTree());
			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "label_ref"


	public static class register_list_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "register_list"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:711:1: register_list : ( REGISTER ( COMMA REGISTER )* -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ( REGISTER )* ) | -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ) );
	public final smaliParser.register_list_return register_list() throws RecognitionException {
		smaliParser.register_list_return retval = new smaliParser.register_list_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token REGISTER188=null;
		Token COMMA189=null;
		Token REGISTER190=null;

		CommonTree REGISTER188_tree=null;
		CommonTree COMMA189_tree=null;
		CommonTree REGISTER190_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:3: ( REGISTER ( COMMA REGISTER )* -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ( REGISTER )* ) | -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ) )
			int alt33=2;
			int LA33_0 = input.LA(1);
			if ( (LA33_0==REGISTER) ) {
				alt33=1;
			}
			else if ( (LA33_0==CLOSE_BRACE) ) {
				alt33=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 33, 0, input);
				throw nvae;
			}

			switch (alt33) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:5: REGISTER ( COMMA REGISTER )*
					{
					REGISTER188=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_register_list3054);
					stream_REGISTER.add(REGISTER188);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:14: ( COMMA REGISTER )*
					loop32:
					while (true) {
						int alt32=2;
						int LA32_0 = input.LA(1);
						if ( (LA32_0==COMMA) ) {
							alt32=1;
						}

						switch (alt32) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:15: COMMA REGISTER
							{
							COMMA189=(Token)match(input,COMMA,FOLLOW_COMMA_in_register_list3057);
							stream_COMMA.add(COMMA189);

							REGISTER190=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_register_list3059);
							stream_REGISTER.add(REGISTER190);

							}
							break;

						default :
							break loop32;
						}
					}

					// AST REWRITE
					// elements: REGISTER
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 712:32: -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ( REGISTER )* )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:35: ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] ( REGISTER )* )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_REGISTER_LIST, (retval.start), "I_REGISTER_LIST"), root_1);
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:712:80: ( REGISTER )*
						while ( stream_REGISTER.hasNext() ) {
							adaptor.addChild(root_1, stream_REGISTER.nextNode());
						}
						stream_REGISTER.reset();

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:713:5:
					{
					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 713:5: -> ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:713:7: ^( I_REGISTER_LIST[$start, \"I_REGISTER_LIST\"] )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_REGISTER_LIST, (retval.start), "I_REGISTER_LIST"), root_1);
						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "register_list"


	public static class register_range_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "register_range"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:715:1: register_range : (startreg= REGISTER ( DOTDOT endreg= REGISTER )? )? -> ^( I_REGISTER_RANGE[$start, \"I_REGISTER_RANGE\"] ( $startreg)? ( $endreg)? ) ;
	public final smaliParser.register_range_return register_range() throws RecognitionException {
		smaliParser.register_range_return retval = new smaliParser.register_range_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token startreg=null;
		Token endreg=null;
		Token DOTDOT191=null;

		CommonTree startreg_tree=null;
		CommonTree endreg_tree=null;
		CommonTree DOTDOT191_tree=null;
		RewriteRuleTokenStream stream_DOTDOT=new RewriteRuleTokenStream(adaptor,"token DOTDOT");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:3: ( (startreg= REGISTER ( DOTDOT endreg= REGISTER )? )? -> ^( I_REGISTER_RANGE[$start, \"I_REGISTER_RANGE\"] ( $startreg)? ( $endreg)? ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:5: (startreg= REGISTER ( DOTDOT endreg= REGISTER )? )?
			{
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:5: (startreg= REGISTER ( DOTDOT endreg= REGISTER )? )?
			int alt35=2;
			int LA35_0 = input.LA(1);
			if ( (LA35_0==REGISTER) ) {
				alt35=1;
			}
			switch (alt35) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:6: startreg= REGISTER ( DOTDOT endreg= REGISTER )?
					{
					startreg=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_register_range3094);
					stream_REGISTER.add(startreg);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:24: ( DOTDOT endreg= REGISTER )?
					int alt34=2;
					int LA34_0 = input.LA(1);
					if ( (LA34_0==DOTDOT) ) {
						alt34=1;
					}
					switch (alt34) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:25: DOTDOT endreg= REGISTER
							{
							DOTDOT191=(Token)match(input,DOTDOT,FOLLOW_DOTDOT_in_register_range3097);
							stream_DOTDOT.add(DOTDOT191);

							endreg=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_register_range3101);
							stream_REGISTER.add(endreg);

							}
							break;

					}

					}
					break;

			}

			// AST REWRITE
			// elements: endreg, startreg
			// token labels: endreg, startreg
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleTokenStream stream_endreg=new RewriteRuleTokenStream(adaptor,"token endreg",endreg);
			RewriteRuleTokenStream stream_startreg=new RewriteRuleTokenStream(adaptor,"token startreg",startreg);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 716:52: -> ^( I_REGISTER_RANGE[$start, \"I_REGISTER_RANGE\"] ( $startreg)? ( $endreg)? )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:55: ^( I_REGISTER_RANGE[$start, \"I_REGISTER_RANGE\"] ( $startreg)? ( $endreg)? )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_REGISTER_RANGE, (retval.start), "I_REGISTER_RANGE"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:103: ( $startreg)?
				if ( stream_startreg.hasNext() ) {
					adaptor.addChild(root_1, stream_startreg.nextNode());
				}
				stream_startreg.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:716:114: ( $endreg)?
				if ( stream_endreg.hasNext() ) {
					adaptor.addChild(root_1, stream_endreg.nextNode());
				}
				stream_endreg.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "register_range"


	public static class verification_error_reference_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "verification_error_reference"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:718:1: verification_error_reference : ( CLASS_DESCRIPTOR | field_reference | method_reference );
	public final smaliParser.verification_error_reference_return verification_error_reference() throws RecognitionException {
		smaliParser.verification_error_reference_return retval = new smaliParser.verification_error_reference_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token CLASS_DESCRIPTOR192=null;
		ParserRuleReturnScope field_reference193 =null;
		ParserRuleReturnScope method_reference194 =null;

		CommonTree CLASS_DESCRIPTOR192_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:719:3: ( CLASS_DESCRIPTOR | field_reference | method_reference )
			int alt36=3;
			alt36 = dfa36.predict(input);
			switch (alt36) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:719:5: CLASS_DESCRIPTOR
					{
					root_0 = (CommonTree)adaptor.nil();


					CLASS_DESCRIPTOR192=(Token)match(input,CLASS_DESCRIPTOR,FOLLOW_CLASS_DESCRIPTOR_in_verification_error_reference3130);
					CLASS_DESCRIPTOR192_tree = (CommonTree)adaptor.create(CLASS_DESCRIPTOR192);
					adaptor.addChild(root_0, CLASS_DESCRIPTOR192_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:719:24: field_reference
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_field_reference_in_verification_error_reference3134);
					field_reference193=field_reference();
					state._fsp--;

					adaptor.addChild(root_0, field_reference193.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:719:42: method_reference
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_method_reference_in_verification_error_reference3138);
					method_reference194=method_reference();
					state._fsp--;

					adaptor.addChild(root_0, method_reference194.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "verification_error_reference"


	public static class catch_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "catch_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:721:1: catch_directive : CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref -> ^( I_CATCH[$start, \"I_CATCH\"] nonvoid_type_descriptor $from $to $using) ;
	public final smaliParser.catch_directive_return catch_directive() throws RecognitionException {
		smaliParser.catch_directive_return retval = new smaliParser.catch_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token CATCH_DIRECTIVE195=null;
		Token OPEN_BRACE197=null;
		Token DOTDOT198=null;
		Token CLOSE_BRACE199=null;
		ParserRuleReturnScope from =null;
		ParserRuleReturnScope to =null;
		ParserRuleReturnScope using =null;
		ParserRuleReturnScope nonvoid_type_descriptor196 =null;

		CommonTree CATCH_DIRECTIVE195_tree=null;
		CommonTree OPEN_BRACE197_tree=null;
		CommonTree DOTDOT198_tree=null;
		CommonTree CLOSE_BRACE199_tree=null;
		RewriteRuleTokenStream stream_DOTDOT=new RewriteRuleTokenStream(adaptor,"token DOTDOT");
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleTokenStream stream_CATCH_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token CATCH_DIRECTIVE");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:722:3: ( CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref -> ^( I_CATCH[$start, \"I_CATCH\"] nonvoid_type_descriptor $from $to $using) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:722:5: CATCH_DIRECTIVE nonvoid_type_descriptor OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref
			{
			CATCH_DIRECTIVE195=(Token)match(input,CATCH_DIRECTIVE,FOLLOW_CATCH_DIRECTIVE_in_catch_directive3148);
			stream_CATCH_DIRECTIVE.add(CATCH_DIRECTIVE195);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_catch_directive3150);
			nonvoid_type_descriptor196=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor196.getTree());
			OPEN_BRACE197=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_catch_directive3152);
			stream_OPEN_BRACE.add(OPEN_BRACE197);

			pushFollow(FOLLOW_label_ref_in_catch_directive3156);
			from=label_ref();
			state._fsp--;

			stream_label_ref.add(from.getTree());
			DOTDOT198=(Token)match(input,DOTDOT,FOLLOW_DOTDOT_in_catch_directive3158);
			stream_DOTDOT.add(DOTDOT198);

			pushFollow(FOLLOW_label_ref_in_catch_directive3162);
			to=label_ref();
			state._fsp--;

			stream_label_ref.add(to.getTree());
			CLOSE_BRACE199=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_catch_directive3164);
			stream_CLOSE_BRACE.add(CLOSE_BRACE199);

			pushFollow(FOLLOW_label_ref_in_catch_directive3168);
			using=label_ref();
			state._fsp--;

			stream_label_ref.add(using.getTree());
			// AST REWRITE
			// elements: using, from, to, nonvoid_type_descriptor
			// token labels:
			// rule labels: to, retval, using, from
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_to=new RewriteRuleSubtreeStream(adaptor,"rule to",to!=null?to.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
			RewriteRuleSubtreeStream stream_using=new RewriteRuleSubtreeStream(adaptor,"rule using",using!=null?using.getTree():null);
			RewriteRuleSubtreeStream stream_from=new RewriteRuleSubtreeStream(adaptor,"rule from",from!=null?from.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 723:5: -> ^( I_CATCH[$start, \"I_CATCH\"] nonvoid_type_descriptor $from $to $using)
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:723:8: ^( I_CATCH[$start, \"I_CATCH\"] nonvoid_type_descriptor $from $to $using)
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_CATCH, (retval.start), "I_CATCH"), root_1);
				adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				adaptor.addChild(root_1, stream_from.nextTree());
				adaptor.addChild(root_1, stream_to.nextTree());
				adaptor.addChild(root_1, stream_using.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "catch_directive"


	public static class catchall_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "catchall_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:725:1: catchall_directive : CATCHALL_DIRECTIVE OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref -> ^( I_CATCHALL[$start, \"I_CATCHALL\"] $from $to $using) ;
	public final smaliParser.catchall_directive_return catchall_directive() throws RecognitionException {
		smaliParser.catchall_directive_return retval = new smaliParser.catchall_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token CATCHALL_DIRECTIVE200=null;
		Token OPEN_BRACE201=null;
		Token DOTDOT202=null;
		Token CLOSE_BRACE203=null;
		ParserRuleReturnScope from =null;
		ParserRuleReturnScope to =null;
		ParserRuleReturnScope using =null;

		CommonTree CATCHALL_DIRECTIVE200_tree=null;
		CommonTree OPEN_BRACE201_tree=null;
		CommonTree DOTDOT202_tree=null;
		CommonTree CLOSE_BRACE203_tree=null;
		RewriteRuleTokenStream stream_DOTDOT=new RewriteRuleTokenStream(adaptor,"token DOTDOT");
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleTokenStream stream_CATCHALL_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token CATCHALL_DIRECTIVE");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:726:3: ( CATCHALL_DIRECTIVE OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref -> ^( I_CATCHALL[$start, \"I_CATCHALL\"] $from $to $using) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:726:5: CATCHALL_DIRECTIVE OPEN_BRACE from= label_ref DOTDOT to= label_ref CLOSE_BRACE using= label_ref
			{
			CATCHALL_DIRECTIVE200=(Token)match(input,CATCHALL_DIRECTIVE,FOLLOW_CATCHALL_DIRECTIVE_in_catchall_directive3200);
			stream_CATCHALL_DIRECTIVE.add(CATCHALL_DIRECTIVE200);

			OPEN_BRACE201=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_catchall_directive3202);
			stream_OPEN_BRACE.add(OPEN_BRACE201);

			pushFollow(FOLLOW_label_ref_in_catchall_directive3206);
			from=label_ref();
			state._fsp--;

			stream_label_ref.add(from.getTree());
			DOTDOT202=(Token)match(input,DOTDOT,FOLLOW_DOTDOT_in_catchall_directive3208);
			stream_DOTDOT.add(DOTDOT202);

			pushFollow(FOLLOW_label_ref_in_catchall_directive3212);
			to=label_ref();
			state._fsp--;

			stream_label_ref.add(to.getTree());
			CLOSE_BRACE203=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_catchall_directive3214);
			stream_CLOSE_BRACE.add(CLOSE_BRACE203);

			pushFollow(FOLLOW_label_ref_in_catchall_directive3218);
			using=label_ref();
			state._fsp--;

			stream_label_ref.add(using.getTree());
			// AST REWRITE
			// elements: from, to, using
			// token labels:
			// rule labels: to, retval, using, from
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_to=new RewriteRuleSubtreeStream(adaptor,"rule to",to!=null?to.getTree():null);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);
			RewriteRuleSubtreeStream stream_using=new RewriteRuleSubtreeStream(adaptor,"rule using",using!=null?using.getTree():null);
			RewriteRuleSubtreeStream stream_from=new RewriteRuleSubtreeStream(adaptor,"rule from",from!=null?from.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 727:5: -> ^( I_CATCHALL[$start, \"I_CATCHALL\"] $from $to $using)
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:727:8: ^( I_CATCHALL[$start, \"I_CATCHALL\"] $from $to $using)
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_CATCHALL, (retval.start), "I_CATCHALL"), root_1);
				adaptor.addChild(root_1, stream_from.nextTree());
				adaptor.addChild(root_1, stream_to.nextTree());
				adaptor.addChild(root_1, stream_using.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "catchall_directive"


	public static class parameter_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "parameter_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:733:1: parameter_directive : PARAMETER_DIRECTIVE REGISTER ( COMMA STRING_LITERAL )? ({...}? annotation )* ( END_PARAMETER_DIRECTIVE -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) ) ) ;
	public final smaliParser.parameter_directive_return parameter_directive() throws RecognitionException {
		smaliParser.parameter_directive_return retval = new smaliParser.parameter_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token PARAMETER_DIRECTIVE204=null;
		Token REGISTER205=null;
		Token COMMA206=null;
		Token STRING_LITERAL207=null;
		Token END_PARAMETER_DIRECTIVE209=null;
		ParserRuleReturnScope annotation208 =null;

		CommonTree PARAMETER_DIRECTIVE204_tree=null;
		CommonTree REGISTER205_tree=null;
		CommonTree COMMA206_tree=null;
		CommonTree STRING_LITERAL207_tree=null;
		CommonTree END_PARAMETER_DIRECTIVE209_tree=null;
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_END_PARAMETER_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_PARAMETER_DIRECTIVE");
		RewriteRuleTokenStream stream_PARAMETER_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token PARAMETER_DIRECTIVE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_annotation=new RewriteRuleSubtreeStream(adaptor,"rule annotation");

		List<CommonTree> annotations = new ArrayList<CommonTree>();
		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:735:3: ( PARAMETER_DIRECTIVE REGISTER ( COMMA STRING_LITERAL )? ({...}? annotation )* ( END_PARAMETER_DIRECTIVE -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:735:5: PARAMETER_DIRECTIVE REGISTER ( COMMA STRING_LITERAL )? ({...}? annotation )* ( END_PARAMETER_DIRECTIVE -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) ) )
			{
			PARAMETER_DIRECTIVE204=(Token)match(input,PARAMETER_DIRECTIVE,FOLLOW_PARAMETER_DIRECTIVE_in_parameter_directive3257);
			stream_PARAMETER_DIRECTIVE.add(PARAMETER_DIRECTIVE204);

			REGISTER205=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_parameter_directive3259);
			stream_REGISTER.add(REGISTER205);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:735:34: ( COMMA STRING_LITERAL )?
			int alt37=2;
			int LA37_0 = input.LA(1);
			if ( (LA37_0==COMMA) ) {
				alt37=1;
			}
			switch (alt37) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:735:35: COMMA STRING_LITERAL
					{
					COMMA206=(Token)match(input,COMMA,FOLLOW_COMMA_in_parameter_directive3262);
					stream_COMMA.add(COMMA206);

					STRING_LITERAL207=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_parameter_directive3264);
					stream_STRING_LITERAL.add(STRING_LITERAL207);

					}
					break;

			}

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:736:5: ({...}? annotation )*
			loop38:
			while (true) {
				int alt38=2;
				alt38 = dfa38.predict(input);
				switch (alt38) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:736:6: {...}? annotation
					{
					if ( !((input.LA(1) == ANNOTATION_DIRECTIVE)) ) {
						throw new FailedPredicateException(input, "parameter_directive", "input.LA(1) == ANNOTATION_DIRECTIVE");
					}
					pushFollow(FOLLOW_annotation_in_parameter_directive3275);
					annotation208=annotation();
					state._fsp--;

					stream_annotation.add(annotation208.getTree());
					annotations.add((annotation208!=null?((CommonTree)annotation208.getTree()):null));
					}
					break;

				default :
					break loop38;
				}
			}

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:738:5: ( END_PARAMETER_DIRECTIVE -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) ) | -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) ) )
			int alt39=2;
			int LA39_0 = input.LA(1);
			if ( (LA39_0==END_PARAMETER_DIRECTIVE) ) {
				alt39=1;
			}
			else if ( (LA39_0==ANNOTATION_DIRECTIVE||LA39_0==ARRAY_DATA_DIRECTIVE||(LA39_0 >= CATCHALL_DIRECTIVE && LA39_0 <= CATCH_DIRECTIVE)||LA39_0==COLON||(LA39_0 >= END_LOCAL_DIRECTIVE && LA39_0 <= END_METHOD_DIRECTIVE)||LA39_0==EPILOGUE_DIRECTIVE||(LA39_0 >= INSTRUCTION_FORMAT10t && LA39_0 <= INSTRUCTION_FORMAT51l)||(LA39_0 >= LINE_DIRECTIVE && LA39_0 <= LOCAL_DIRECTIVE)||(LA39_0 >= PACKED_SWITCH_DIRECTIVE && LA39_0 <= PARAMETER_DIRECTIVE)||LA39_0==PROLOGUE_DIRECTIVE||(LA39_0 >= REGISTERS_DIRECTIVE && LA39_0 <= RESTART_LOCAL_DIRECTIVE)||(LA39_0 >= SOURCE_DIRECTIVE && LA39_0 <= SPARSE_SWITCH_DIRECTIVE)) ) {
				alt39=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 39, 0, input);
				throw nvae;
			}

			switch (alt39) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:738:7: END_PARAMETER_DIRECTIVE
					{
					END_PARAMETER_DIRECTIVE209=(Token)match(input,END_PARAMETER_DIRECTIVE,FOLLOW_END_PARAMETER_DIRECTIVE_in_parameter_directive3288);
					stream_END_PARAMETER_DIRECTIVE.add(END_PARAMETER_DIRECTIVE209);

					// AST REWRITE
					// elements: STRING_LITERAL, annotation, REGISTER
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 739:7: -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:739:10: ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ( annotation )* ) )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PARAMETER, (retval.start), "I_PARAMETER"), root_1);
						adaptor.addChild(root_1, stream_REGISTER.nextNode());
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:739:56: ( STRING_LITERAL )?
						if ( stream_STRING_LITERAL.hasNext() ) {
							adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
						}
						stream_STRING_LITERAL.reset();

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:739:72: ^( I_ANNOTATIONS ( annotation )* )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATIONS, "I_ANNOTATIONS"), root_2);
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:739:88: ( annotation )*
						while ( stream_annotation.hasNext() ) {
							adaptor.addChild(root_2, stream_annotation.nextTree());
						}
						stream_annotation.reset();

						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:740:19:
					{
					statements_and_directives_stack.peek().methodAnnotations.addAll(annotations);
					// AST REWRITE
					// elements: STRING_LITERAL, REGISTER
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 741:7: -> ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) )
					{
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:741:10: ^( I_PARAMETER[$start, \"I_PARAMETER\"] REGISTER ( STRING_LITERAL )? ^( I_ANNOTATIONS ) )
						{
						CommonTree root_1 = (CommonTree)adaptor.nil();
						root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PARAMETER, (retval.start), "I_PARAMETER"), root_1);
						adaptor.addChild(root_1, stream_REGISTER.nextNode());
						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:741:56: ( STRING_LITERAL )?
						if ( stream_STRING_LITERAL.hasNext() ) {
							adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
						}
						stream_STRING_LITERAL.reset();

						// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:741:72: ^( I_ANNOTATIONS )
						{
						CommonTree root_2 = (CommonTree)adaptor.nil();
						root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ANNOTATIONS, "I_ANNOTATIONS"), root_2);
						adaptor.addChild(root_1, root_2);
						}

						adaptor.addChild(root_0, root_1);
						}

					}


					retval.tree = root_0;

					}
					break;

			}

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "parameter_directive"


	public static class debug_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "debug_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:744:1: debug_directive : ( line_directive | local_directive | end_local_directive | restart_local_directive | prologue_directive | epilogue_directive | source_directive );
	public final smaliParser.debug_directive_return debug_directive() throws RecognitionException {
		smaliParser.debug_directive_return retval = new smaliParser.debug_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		ParserRuleReturnScope line_directive210 =null;
		ParserRuleReturnScope local_directive211 =null;
		ParserRuleReturnScope end_local_directive212 =null;
		ParserRuleReturnScope restart_local_directive213 =null;
		ParserRuleReturnScope prologue_directive214 =null;
		ParserRuleReturnScope epilogue_directive215 =null;
		ParserRuleReturnScope source_directive216 =null;


		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:745:3: ( line_directive | local_directive | end_local_directive | restart_local_directive | prologue_directive | epilogue_directive | source_directive )
			int alt40=7;
			switch ( input.LA(1) ) {
			case LINE_DIRECTIVE:
				{
				alt40=1;
				}
				break;
			case LOCAL_DIRECTIVE:
				{
				alt40=2;
				}
				break;
			case END_LOCAL_DIRECTIVE:
				{
				alt40=3;
				}
				break;
			case RESTART_LOCAL_DIRECTIVE:
				{
				alt40=4;
				}
				break;
			case PROLOGUE_DIRECTIVE:
				{
				alt40=5;
				}
				break;
			case EPILOGUE_DIRECTIVE:
				{
				alt40=6;
				}
				break;
			case SOURCE_DIRECTIVE:
				{
				alt40=7;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 40, 0, input);
				throw nvae;
			}
			switch (alt40) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:745:5: line_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_line_directive_in_debug_directive3361);
					line_directive210=line_directive();
					state._fsp--;

					adaptor.addChild(root_0, line_directive210.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:746:5: local_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_local_directive_in_debug_directive3367);
					local_directive211=local_directive();
					state._fsp--;

					adaptor.addChild(root_0, local_directive211.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:747:5: end_local_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_end_local_directive_in_debug_directive3373);
					end_local_directive212=end_local_directive();
					state._fsp--;

					adaptor.addChild(root_0, end_local_directive212.getTree());

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:748:5: restart_local_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_restart_local_directive_in_debug_directive3379);
					restart_local_directive213=restart_local_directive();
					state._fsp--;

					adaptor.addChild(root_0, restart_local_directive213.getTree());

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:749:5: prologue_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_prologue_directive_in_debug_directive3385);
					prologue_directive214=prologue_directive();
					state._fsp--;

					adaptor.addChild(root_0, prologue_directive214.getTree());

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:750:5: epilogue_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_epilogue_directive_in_debug_directive3391);
					epilogue_directive215=epilogue_directive();
					state._fsp--;

					adaptor.addChild(root_0, epilogue_directive215.getTree());

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:751:5: source_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_source_directive_in_debug_directive3397);
					source_directive216=source_directive();
					state._fsp--;

					adaptor.addChild(root_0, source_directive216.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "debug_directive"


	public static class line_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "line_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:753:1: line_directive : LINE_DIRECTIVE integral_literal -> ^( I_LINE[$start, \"I_LINE\"] integral_literal ) ;
	public final smaliParser.line_directive_return line_directive() throws RecognitionException {
		smaliParser.line_directive_return retval = new smaliParser.line_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token LINE_DIRECTIVE217=null;
		ParserRuleReturnScope integral_literal218 =null;

		CommonTree LINE_DIRECTIVE217_tree=null;
		RewriteRuleTokenStream stream_LINE_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token LINE_DIRECTIVE");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:754:3: ( LINE_DIRECTIVE integral_literal -> ^( I_LINE[$start, \"I_LINE\"] integral_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:754:5: LINE_DIRECTIVE integral_literal
			{
			LINE_DIRECTIVE217=(Token)match(input,LINE_DIRECTIVE,FOLLOW_LINE_DIRECTIVE_in_line_directive3407);
			stream_LINE_DIRECTIVE.add(LINE_DIRECTIVE217);

			pushFollow(FOLLOW_integral_literal_in_line_directive3409);
			integral_literal218=integral_literal();
			state._fsp--;

			stream_integral_literal.add(integral_literal218.getTree());
			// AST REWRITE
			// elements: integral_literal
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 755:5: -> ^( I_LINE[$start, \"I_LINE\"] integral_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:755:8: ^( I_LINE[$start, \"I_LINE\"] integral_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_LINE, (retval.start), "I_LINE"), root_1);
				adaptor.addChild(root_1, stream_integral_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "line_directive"


	public static class local_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "local_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:757:1: local_directive : LOCAL_DIRECTIVE REGISTER ( COMMA ( NULL_LITERAL |name= STRING_LITERAL ) COLON ( VOID_TYPE | nonvoid_type_descriptor ) ( COMMA signature= STRING_LITERAL )? )? -> ^( I_LOCAL[$start, \"I_LOCAL\"] REGISTER ( NULL_LITERAL )? ( $name)? ( nonvoid_type_descriptor )? ( $signature)? ) ;
	public final smaliParser.local_directive_return local_directive() throws RecognitionException {
		smaliParser.local_directive_return retval = new smaliParser.local_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token name=null;
		Token signature=null;
		Token LOCAL_DIRECTIVE219=null;
		Token REGISTER220=null;
		Token COMMA221=null;
		Token NULL_LITERAL222=null;
		Token COLON223=null;
		Token VOID_TYPE224=null;
		Token COMMA226=null;
		ParserRuleReturnScope nonvoid_type_descriptor225 =null;

		CommonTree name_tree=null;
		CommonTree signature_tree=null;
		CommonTree LOCAL_DIRECTIVE219_tree=null;
		CommonTree REGISTER220_tree=null;
		CommonTree COMMA221_tree=null;
		CommonTree NULL_LITERAL222_tree=null;
		CommonTree COLON223_tree=null;
		CommonTree VOID_TYPE224_tree=null;
		CommonTree COMMA226_tree=null;
		RewriteRuleTokenStream stream_COLON=new RewriteRuleTokenStream(adaptor,"token COLON");
		RewriteRuleTokenStream stream_NULL_LITERAL=new RewriteRuleTokenStream(adaptor,"token NULL_LITERAL");
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_LOCAL_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token LOCAL_DIRECTIVE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_VOID_TYPE=new RewriteRuleTokenStream(adaptor,"token VOID_TYPE");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:3: ( LOCAL_DIRECTIVE REGISTER ( COMMA ( NULL_LITERAL |name= STRING_LITERAL ) COLON ( VOID_TYPE | nonvoid_type_descriptor ) ( COMMA signature= STRING_LITERAL )? )? -> ^( I_LOCAL[$start, \"I_LOCAL\"] REGISTER ( NULL_LITERAL )? ( $name)? ( nonvoid_type_descriptor )? ( $signature)? ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:5: LOCAL_DIRECTIVE REGISTER ( COMMA ( NULL_LITERAL |name= STRING_LITERAL ) COLON ( VOID_TYPE | nonvoid_type_descriptor ) ( COMMA signature= STRING_LITERAL )? )?
			{
			LOCAL_DIRECTIVE219=(Token)match(input,LOCAL_DIRECTIVE,FOLLOW_LOCAL_DIRECTIVE_in_local_directive3432);
			stream_LOCAL_DIRECTIVE.add(LOCAL_DIRECTIVE219);

			REGISTER220=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_local_directive3434);
			stream_REGISTER.add(REGISTER220);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:30: ( COMMA ( NULL_LITERAL |name= STRING_LITERAL ) COLON ( VOID_TYPE | nonvoid_type_descriptor ) ( COMMA signature= STRING_LITERAL )? )?
			int alt44=2;
			int LA44_0 = input.LA(1);
			if ( (LA44_0==COMMA) ) {
				alt44=1;
			}
			switch (alt44) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:31: COMMA ( NULL_LITERAL |name= STRING_LITERAL ) COLON ( VOID_TYPE | nonvoid_type_descriptor ) ( COMMA signature= STRING_LITERAL )?
					{
					COMMA221=(Token)match(input,COMMA,FOLLOW_COMMA_in_local_directive3437);
					stream_COMMA.add(COMMA221);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:37: ( NULL_LITERAL |name= STRING_LITERAL )
					int alt41=2;
					int LA41_0 = input.LA(1);
					if ( (LA41_0==NULL_LITERAL) ) {
						alt41=1;
					}
					else if ( (LA41_0==STRING_LITERAL) ) {
						alt41=2;
					}

					else {
						NoViableAltException nvae =
							new NoViableAltException("", 41, 0, input);
						throw nvae;
					}

					switch (alt41) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:38: NULL_LITERAL
							{
							NULL_LITERAL222=(Token)match(input,NULL_LITERAL,FOLLOW_NULL_LITERAL_in_local_directive3440);
							stream_NULL_LITERAL.add(NULL_LITERAL222);

							}
							break;
						case 2 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:53: name= STRING_LITERAL
							{
							name=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_local_directive3446);
							stream_STRING_LITERAL.add(name);

							}
							break;

					}

					COLON223=(Token)match(input,COLON,FOLLOW_COLON_in_local_directive3449);
					stream_COLON.add(COLON223);

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:80: ( VOID_TYPE | nonvoid_type_descriptor )
					int alt42=2;
					int LA42_0 = input.LA(1);
					if ( (LA42_0==VOID_TYPE) ) {
						alt42=1;
					}
					else if ( (LA42_0==ARRAY_DESCRIPTOR||LA42_0==CLASS_DESCRIPTOR||LA42_0==PRIMITIVE_TYPE) ) {
						alt42=2;
					}

					else {
						NoViableAltException nvae =
							new NoViableAltException("", 42, 0, input);
						throw nvae;
					}

					switch (alt42) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:81: VOID_TYPE
							{
							VOID_TYPE224=(Token)match(input,VOID_TYPE,FOLLOW_VOID_TYPE_in_local_directive3452);
							stream_VOID_TYPE.add(VOID_TYPE224);

							}
							break;
						case 2 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:758:93: nonvoid_type_descriptor
							{
							pushFollow(FOLLOW_nonvoid_type_descriptor_in_local_directive3456);
							nonvoid_type_descriptor225=nonvoid_type_descriptor();
							state._fsp--;

							stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor225.getTree());
							}
							break;

					}

					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:759:31: ( COMMA signature= STRING_LITERAL )?
					int alt43=2;
					int LA43_0 = input.LA(1);
					if ( (LA43_0==COMMA) ) {
						alt43=1;
					}
					switch (alt43) {
						case 1 :
							// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:759:32: COMMA signature= STRING_LITERAL
							{
							COMMA226=(Token)match(input,COMMA,FOLLOW_COMMA_in_local_directive3490);
							stream_COMMA.add(COMMA226);

							signature=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_local_directive3494);
							stream_STRING_LITERAL.add(signature);

							}
							break;

					}

					}
					break;

			}

			// AST REWRITE
			// elements: nonvoid_type_descriptor, name, NULL_LITERAL, signature, REGISTER
			// token labels: name, signature
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleTokenStream stream_name=new RewriteRuleTokenStream(adaptor,"token name",name);
			RewriteRuleTokenStream stream_signature=new RewriteRuleTokenStream(adaptor,"token signature",signature);
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 760:5: -> ^( I_LOCAL[$start, \"I_LOCAL\"] REGISTER ( NULL_LITERAL )? ( $name)? ( nonvoid_type_descriptor )? ( $signature)? )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:760:8: ^( I_LOCAL[$start, \"I_LOCAL\"] REGISTER ( NULL_LITERAL )? ( $name)? ( nonvoid_type_descriptor )? ( $signature)? )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_LOCAL, (retval.start), "I_LOCAL"), root_1);
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:760:46: ( NULL_LITERAL )?
				if ( stream_NULL_LITERAL.hasNext() ) {
					adaptor.addChild(root_1, stream_NULL_LITERAL.nextNode());
				}
				stream_NULL_LITERAL.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:760:61: ( $name)?
				if ( stream_name.hasNext() ) {
					adaptor.addChild(root_1, stream_name.nextNode());
				}
				stream_name.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:760:67: ( nonvoid_type_descriptor )?
				if ( stream_nonvoid_type_descriptor.hasNext() ) {
					adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				}
				stream_nonvoid_type_descriptor.reset();

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:760:93: ( $signature)?
				if ( stream_signature.hasNext() ) {
					adaptor.addChild(root_1, stream_signature.nextNode());
				}
				stream_signature.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "local_directive"


	public static class end_local_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "end_local_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:762:1: end_local_directive : END_LOCAL_DIRECTIVE REGISTER -> ^( I_END_LOCAL[$start, \"I_END_LOCAL\"] REGISTER ) ;
	public final smaliParser.end_local_directive_return end_local_directive() throws RecognitionException {
		smaliParser.end_local_directive_return retval = new smaliParser.end_local_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token END_LOCAL_DIRECTIVE227=null;
		Token REGISTER228=null;

		CommonTree END_LOCAL_DIRECTIVE227_tree=null;
		CommonTree REGISTER228_tree=null;
		RewriteRuleTokenStream stream_END_LOCAL_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_LOCAL_DIRECTIVE");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:763:3: ( END_LOCAL_DIRECTIVE REGISTER -> ^( I_END_LOCAL[$start, \"I_END_LOCAL\"] REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:763:5: END_LOCAL_DIRECTIVE REGISTER
			{
			END_LOCAL_DIRECTIVE227=(Token)match(input,END_LOCAL_DIRECTIVE,FOLLOW_END_LOCAL_DIRECTIVE_in_end_local_directive3536);
			stream_END_LOCAL_DIRECTIVE.add(END_LOCAL_DIRECTIVE227);

			REGISTER228=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_end_local_directive3538);
			stream_REGISTER.add(REGISTER228);

			// AST REWRITE
			// elements: REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 764:5: -> ^( I_END_LOCAL[$start, \"I_END_LOCAL\"] REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:764:8: ^( I_END_LOCAL[$start, \"I_END_LOCAL\"] REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_END_LOCAL, (retval.start), "I_END_LOCAL"), root_1);
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "end_local_directive"


	public static class restart_local_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "restart_local_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:766:1: restart_local_directive : RESTART_LOCAL_DIRECTIVE REGISTER -> ^( I_RESTART_LOCAL[$start, \"I_RESTART_LOCAL\"] REGISTER ) ;
	public final smaliParser.restart_local_directive_return restart_local_directive() throws RecognitionException {
		smaliParser.restart_local_directive_return retval = new smaliParser.restart_local_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token RESTART_LOCAL_DIRECTIVE229=null;
		Token REGISTER230=null;

		CommonTree RESTART_LOCAL_DIRECTIVE229_tree=null;
		CommonTree REGISTER230_tree=null;
		RewriteRuleTokenStream stream_RESTART_LOCAL_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token RESTART_LOCAL_DIRECTIVE");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:767:3: ( RESTART_LOCAL_DIRECTIVE REGISTER -> ^( I_RESTART_LOCAL[$start, \"I_RESTART_LOCAL\"] REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:767:5: RESTART_LOCAL_DIRECTIVE REGISTER
			{
			RESTART_LOCAL_DIRECTIVE229=(Token)match(input,RESTART_LOCAL_DIRECTIVE,FOLLOW_RESTART_LOCAL_DIRECTIVE_in_restart_local_directive3561);
			stream_RESTART_LOCAL_DIRECTIVE.add(RESTART_LOCAL_DIRECTIVE229);

			REGISTER230=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_restart_local_directive3563);
			stream_REGISTER.add(REGISTER230);

			// AST REWRITE
			// elements: REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 768:5: -> ^( I_RESTART_LOCAL[$start, \"I_RESTART_LOCAL\"] REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:768:8: ^( I_RESTART_LOCAL[$start, \"I_RESTART_LOCAL\"] REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_RESTART_LOCAL, (retval.start), "I_RESTART_LOCAL"), root_1);
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "restart_local_directive"


	public static class prologue_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "prologue_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:770:1: prologue_directive : PROLOGUE_DIRECTIVE -> ^( I_PROLOGUE[$start, \"I_PROLOGUE\"] ) ;
	public final smaliParser.prologue_directive_return prologue_directive() throws RecognitionException {
		smaliParser.prologue_directive_return retval = new smaliParser.prologue_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token PROLOGUE_DIRECTIVE231=null;

		CommonTree PROLOGUE_DIRECTIVE231_tree=null;
		RewriteRuleTokenStream stream_PROLOGUE_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token PROLOGUE_DIRECTIVE");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:771:3: ( PROLOGUE_DIRECTIVE -> ^( I_PROLOGUE[$start, \"I_PROLOGUE\"] ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:771:5: PROLOGUE_DIRECTIVE
			{
			PROLOGUE_DIRECTIVE231=(Token)match(input,PROLOGUE_DIRECTIVE,FOLLOW_PROLOGUE_DIRECTIVE_in_prologue_directive3586);
			stream_PROLOGUE_DIRECTIVE.add(PROLOGUE_DIRECTIVE231);

			// AST REWRITE
			// elements:
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 772:5: -> ^( I_PROLOGUE[$start, \"I_PROLOGUE\"] )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:772:8: ^( I_PROLOGUE[$start, \"I_PROLOGUE\"] )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PROLOGUE, (retval.start), "I_PROLOGUE"), root_1);
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "prologue_directive"


	public static class epilogue_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "epilogue_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:774:1: epilogue_directive : EPILOGUE_DIRECTIVE -> ^( I_EPILOGUE[$start, \"I_EPILOGUE\"] ) ;
	public final smaliParser.epilogue_directive_return epilogue_directive() throws RecognitionException {
		smaliParser.epilogue_directive_return retval = new smaliParser.epilogue_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token EPILOGUE_DIRECTIVE232=null;

		CommonTree EPILOGUE_DIRECTIVE232_tree=null;
		RewriteRuleTokenStream stream_EPILOGUE_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token EPILOGUE_DIRECTIVE");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:775:3: ( EPILOGUE_DIRECTIVE -> ^( I_EPILOGUE[$start, \"I_EPILOGUE\"] ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:775:5: EPILOGUE_DIRECTIVE
			{
			EPILOGUE_DIRECTIVE232=(Token)match(input,EPILOGUE_DIRECTIVE,FOLLOW_EPILOGUE_DIRECTIVE_in_epilogue_directive3607);
			stream_EPILOGUE_DIRECTIVE.add(EPILOGUE_DIRECTIVE232);

			// AST REWRITE
			// elements:
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 776:5: -> ^( I_EPILOGUE[$start, \"I_EPILOGUE\"] )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:776:8: ^( I_EPILOGUE[$start, \"I_EPILOGUE\"] )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_EPILOGUE, (retval.start), "I_EPILOGUE"), root_1);
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "epilogue_directive"


	public static class source_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "source_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:778:1: source_directive : SOURCE_DIRECTIVE ( STRING_LITERAL )? -> ^( I_SOURCE[$start, \"I_SOURCE\"] ( STRING_LITERAL )? ) ;
	public final smaliParser.source_directive_return source_directive() throws RecognitionException {
		smaliParser.source_directive_return retval = new smaliParser.source_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SOURCE_DIRECTIVE233=null;
		Token STRING_LITERAL234=null;

		CommonTree SOURCE_DIRECTIVE233_tree=null;
		CommonTree STRING_LITERAL234_tree=null;
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_SOURCE_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token SOURCE_DIRECTIVE");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:779:3: ( SOURCE_DIRECTIVE ( STRING_LITERAL )? -> ^( I_SOURCE[$start, \"I_SOURCE\"] ( STRING_LITERAL )? ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:779:5: SOURCE_DIRECTIVE ( STRING_LITERAL )?
			{
			SOURCE_DIRECTIVE233=(Token)match(input,SOURCE_DIRECTIVE,FOLLOW_SOURCE_DIRECTIVE_in_source_directive3628);
			stream_SOURCE_DIRECTIVE.add(SOURCE_DIRECTIVE233);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:779:22: ( STRING_LITERAL )?
			int alt45=2;
			int LA45_0 = input.LA(1);
			if ( (LA45_0==STRING_LITERAL) ) {
				alt45=1;
			}
			switch (alt45) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:779:22: STRING_LITERAL
					{
					STRING_LITERAL234=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_source_directive3630);
					stream_STRING_LITERAL.add(STRING_LITERAL234);

					}
					break;

			}

			// AST REWRITE
			// elements: STRING_LITERAL
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 780:5: -> ^( I_SOURCE[$start, \"I_SOURCE\"] ( STRING_LITERAL )? )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:780:8: ^( I_SOURCE[$start, \"I_SOURCE\"] ( STRING_LITERAL )? )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SOURCE, (retval.start), "I_SOURCE"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:780:39: ( STRING_LITERAL )?
				if ( stream_STRING_LITERAL.hasNext() ) {
					adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
				}
				stream_STRING_LITERAL.reset();

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "source_directive"


	public static class instruction_format12x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "instruction_format12x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:782:1: instruction_format12x : ( INSTRUCTION_FORMAT12x | INSTRUCTION_FORMAT12x_OR_ID -> INSTRUCTION_FORMAT12x[$INSTRUCTION_FORMAT12x_OR_ID] );
	public final smaliParser.instruction_format12x_return instruction_format12x() throws RecognitionException {
		smaliParser.instruction_format12x_return retval = new smaliParser.instruction_format12x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT12x235=null;
		Token INSTRUCTION_FORMAT12x_OR_ID236=null;

		CommonTree INSTRUCTION_FORMAT12x235_tree=null;
		CommonTree INSTRUCTION_FORMAT12x_OR_ID236_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT12x_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT12x_OR_ID");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:783:3: ( INSTRUCTION_FORMAT12x | INSTRUCTION_FORMAT12x_OR_ID -> INSTRUCTION_FORMAT12x[$INSTRUCTION_FORMAT12x_OR_ID] )
			int alt46=2;
			int LA46_0 = input.LA(1);
			if ( (LA46_0==INSTRUCTION_FORMAT12x) ) {
				alt46=1;
			}
			else if ( (LA46_0==INSTRUCTION_FORMAT12x_OR_ID) ) {
				alt46=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 46, 0, input);
				throw nvae;
			}

			switch (alt46) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:783:5: INSTRUCTION_FORMAT12x
					{
					root_0 = (CommonTree)adaptor.nil();


					INSTRUCTION_FORMAT12x235=(Token)match(input,INSTRUCTION_FORMAT12x,FOLLOW_INSTRUCTION_FORMAT12x_in_instruction_format12x3655);
					INSTRUCTION_FORMAT12x235_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT12x235);
					adaptor.addChild(root_0, INSTRUCTION_FORMAT12x235_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:784:5: INSTRUCTION_FORMAT12x_OR_ID
					{
					INSTRUCTION_FORMAT12x_OR_ID236=(Token)match(input,INSTRUCTION_FORMAT12x_OR_ID,FOLLOW_INSTRUCTION_FORMAT12x_OR_ID_in_instruction_format12x3661);
					stream_INSTRUCTION_FORMAT12x_OR_ID.add(INSTRUCTION_FORMAT12x_OR_ID236);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 784:33: -> INSTRUCTION_FORMAT12x[$INSTRUCTION_FORMAT12x_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(INSTRUCTION_FORMAT12x, INSTRUCTION_FORMAT12x_OR_ID236));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "instruction_format12x"


	public static class instruction_format22s_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "instruction_format22s"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:786:1: instruction_format22s : ( INSTRUCTION_FORMAT22s | INSTRUCTION_FORMAT22s_OR_ID -> INSTRUCTION_FORMAT22s[$INSTRUCTION_FORMAT22s_OR_ID] );
	public final smaliParser.instruction_format22s_return instruction_format22s() throws RecognitionException {
		smaliParser.instruction_format22s_return retval = new smaliParser.instruction_format22s_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22s237=null;
		Token INSTRUCTION_FORMAT22s_OR_ID238=null;

		CommonTree INSTRUCTION_FORMAT22s237_tree=null;
		CommonTree INSTRUCTION_FORMAT22s_OR_ID238_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22s_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22s_OR_ID");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:787:3: ( INSTRUCTION_FORMAT22s | INSTRUCTION_FORMAT22s_OR_ID -> INSTRUCTION_FORMAT22s[$INSTRUCTION_FORMAT22s_OR_ID] )
			int alt47=2;
			int LA47_0 = input.LA(1);
			if ( (LA47_0==INSTRUCTION_FORMAT22s) ) {
				alt47=1;
			}
			else if ( (LA47_0==INSTRUCTION_FORMAT22s_OR_ID) ) {
				alt47=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 47, 0, input);
				throw nvae;
			}

			switch (alt47) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:787:5: INSTRUCTION_FORMAT22s
					{
					root_0 = (CommonTree)adaptor.nil();


					INSTRUCTION_FORMAT22s237=(Token)match(input,INSTRUCTION_FORMAT22s,FOLLOW_INSTRUCTION_FORMAT22s_in_instruction_format22s3676);
					INSTRUCTION_FORMAT22s237_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT22s237);
					adaptor.addChild(root_0, INSTRUCTION_FORMAT22s237_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:788:5: INSTRUCTION_FORMAT22s_OR_ID
					{
					INSTRUCTION_FORMAT22s_OR_ID238=(Token)match(input,INSTRUCTION_FORMAT22s_OR_ID,FOLLOW_INSTRUCTION_FORMAT22s_OR_ID_in_instruction_format22s3682);
					stream_INSTRUCTION_FORMAT22s_OR_ID.add(INSTRUCTION_FORMAT22s_OR_ID238);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 788:33: -> INSTRUCTION_FORMAT22s[$INSTRUCTION_FORMAT22s_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(INSTRUCTION_FORMAT22s, INSTRUCTION_FORMAT22s_OR_ID238));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "instruction_format22s"


	public static class instruction_format31i_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "instruction_format31i"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:790:1: instruction_format31i : ( INSTRUCTION_FORMAT31i | INSTRUCTION_FORMAT31i_OR_ID -> INSTRUCTION_FORMAT31i[$INSTRUCTION_FORMAT31i_OR_ID] );
	public final smaliParser.instruction_format31i_return instruction_format31i() throws RecognitionException {
		smaliParser.instruction_format31i_return retval = new smaliParser.instruction_format31i_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT31i239=null;
		Token INSTRUCTION_FORMAT31i_OR_ID240=null;

		CommonTree INSTRUCTION_FORMAT31i239_tree=null;
		CommonTree INSTRUCTION_FORMAT31i_OR_ID240_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT31i_OR_ID=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT31i_OR_ID");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:791:3: ( INSTRUCTION_FORMAT31i | INSTRUCTION_FORMAT31i_OR_ID -> INSTRUCTION_FORMAT31i[$INSTRUCTION_FORMAT31i_OR_ID] )
			int alt48=2;
			int LA48_0 = input.LA(1);
			if ( (LA48_0==INSTRUCTION_FORMAT31i) ) {
				alt48=1;
			}
			else if ( (LA48_0==INSTRUCTION_FORMAT31i_OR_ID) ) {
				alt48=2;
			}

			else {
				NoViableAltException nvae =
					new NoViableAltException("", 48, 0, input);
				throw nvae;
			}

			switch (alt48) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:791:5: INSTRUCTION_FORMAT31i
					{
					root_0 = (CommonTree)adaptor.nil();


					INSTRUCTION_FORMAT31i239=(Token)match(input,INSTRUCTION_FORMAT31i,FOLLOW_INSTRUCTION_FORMAT31i_in_instruction_format31i3697);
					INSTRUCTION_FORMAT31i239_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT31i239);
					adaptor.addChild(root_0, INSTRUCTION_FORMAT31i239_tree);

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:792:5: INSTRUCTION_FORMAT31i_OR_ID
					{
					INSTRUCTION_FORMAT31i_OR_ID240=(Token)match(input,INSTRUCTION_FORMAT31i_OR_ID,FOLLOW_INSTRUCTION_FORMAT31i_OR_ID_in_instruction_format31i3703);
					stream_INSTRUCTION_FORMAT31i_OR_ID.add(INSTRUCTION_FORMAT31i_OR_ID240);

					// AST REWRITE
					// elements:
					// token labels:
					// rule labels: retval
					// token list labels:
					// rule list labels:
					// wildcard labels:
					retval.tree = root_0;
					RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

					root_0 = (CommonTree)adaptor.nil();
					// 792:33: -> INSTRUCTION_FORMAT31i[$INSTRUCTION_FORMAT31i_OR_ID]
					{
						adaptor.addChild(root_0, (CommonTree)adaptor.create(INSTRUCTION_FORMAT31i, INSTRUCTION_FORMAT31i_OR_ID240));
					}


					retval.tree = root_0;

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "instruction_format31i"


	public static class instruction_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "instruction"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:796:1: instruction : ( insn_format10t | insn_format10x | insn_format10x_odex | insn_format11n | insn_format11x | insn_format12x | insn_format20bc | insn_format20t | insn_format21c_field | insn_format21c_field_odex | insn_format21c_string | insn_format21c_type | insn_format21ih | insn_format21lh | insn_format21s | insn_format21t | insn_format22b | insn_format22c_field | insn_format22c_field_odex | insn_format22c_type | insn_format22cs_field | insn_format22s | insn_format22t | insn_format22x | insn_format23x | insn_format30t | insn_format31c | insn_format31i | insn_format31t | insn_format32x | insn_format35c_method | insn_format35c_type | insn_format35c_method_odex | insn_format35mi_method | insn_format35ms_method | insn_format3rc_method | insn_format3rc_method_odex | insn_format3rc_type | insn_format3rmi_method | insn_format3rms_method | insn_format51l | insn_array_data_directive | insn_packed_switch_directive | insn_sparse_switch_directive );
	public final smaliParser.instruction_return instruction() throws RecognitionException {
		smaliParser.instruction_return retval = new smaliParser.instruction_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		ParserRuleReturnScope insn_format10t241 =null;
		ParserRuleReturnScope insn_format10x242 =null;
		ParserRuleReturnScope insn_format10x_odex243 =null;
		ParserRuleReturnScope insn_format11n244 =null;
		ParserRuleReturnScope insn_format11x245 =null;
		ParserRuleReturnScope insn_format12x246 =null;
		ParserRuleReturnScope insn_format20bc247 =null;
		ParserRuleReturnScope insn_format20t248 =null;
		ParserRuleReturnScope insn_format21c_field249 =null;
		ParserRuleReturnScope insn_format21c_field_odex250 =null;
		ParserRuleReturnScope insn_format21c_string251 =null;
		ParserRuleReturnScope insn_format21c_type252 =null;
		ParserRuleReturnScope insn_format21ih253 =null;
		ParserRuleReturnScope insn_format21lh254 =null;
		ParserRuleReturnScope insn_format21s255 =null;
		ParserRuleReturnScope insn_format21t256 =null;
		ParserRuleReturnScope insn_format22b257 =null;
		ParserRuleReturnScope insn_format22c_field258 =null;
		ParserRuleReturnScope insn_format22c_field_odex259 =null;
		ParserRuleReturnScope insn_format22c_type260 =null;
		ParserRuleReturnScope insn_format22cs_field261 =null;
		ParserRuleReturnScope insn_format22s262 =null;
		ParserRuleReturnScope insn_format22t263 =null;
		ParserRuleReturnScope insn_format22x264 =null;
		ParserRuleReturnScope insn_format23x265 =null;
		ParserRuleReturnScope insn_format30t266 =null;
		ParserRuleReturnScope insn_format31c267 =null;
		ParserRuleReturnScope insn_format31i268 =null;
		ParserRuleReturnScope insn_format31t269 =null;
		ParserRuleReturnScope insn_format32x270 =null;
		ParserRuleReturnScope insn_format35c_method271 =null;
		ParserRuleReturnScope insn_format35c_type272 =null;
		ParserRuleReturnScope insn_format35c_method_odex273 =null;
		ParserRuleReturnScope insn_format35mi_method274 =null;
		ParserRuleReturnScope insn_format35ms_method275 =null;
		ParserRuleReturnScope insn_format3rc_method276 =null;
		ParserRuleReturnScope insn_format3rc_method_odex277 =null;
		ParserRuleReturnScope insn_format3rc_type278 =null;
		ParserRuleReturnScope insn_format3rmi_method279 =null;
		ParserRuleReturnScope insn_format3rms_method280 =null;
		ParserRuleReturnScope insn_format51l281 =null;
		ParserRuleReturnScope insn_array_data_directive282 =null;
		ParserRuleReturnScope insn_packed_switch_directive283 =null;
		ParserRuleReturnScope insn_sparse_switch_directive284 =null;


		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:797:3: ( insn_format10t | insn_format10x | insn_format10x_odex | insn_format11n | insn_format11x | insn_format12x | insn_format20bc | insn_format20t | insn_format21c_field | insn_format21c_field_odex | insn_format21c_string | insn_format21c_type | insn_format21ih | insn_format21lh | insn_format21s | insn_format21t | insn_format22b | insn_format22c_field | insn_format22c_field_odex | insn_format22c_type | insn_format22cs_field | insn_format22s | insn_format22t | insn_format22x | insn_format23x | insn_format30t | insn_format31c | insn_format31i | insn_format31t | insn_format32x | insn_format35c_method | insn_format35c_type | insn_format35c_method_odex | insn_format35mi_method | insn_format35ms_method | insn_format3rc_method | insn_format3rc_method_odex | insn_format3rc_type | insn_format3rmi_method | insn_format3rms_method | insn_format51l | insn_array_data_directive | insn_packed_switch_directive | insn_sparse_switch_directive )
			int alt49=44;
			switch ( input.LA(1) ) {
			case INSTRUCTION_FORMAT10t:
				{
				alt49=1;
				}
				break;
			case INSTRUCTION_FORMAT10x:
				{
				alt49=2;
				}
				break;
			case INSTRUCTION_FORMAT10x_ODEX:
				{
				alt49=3;
				}
				break;
			case INSTRUCTION_FORMAT11n:
				{
				alt49=4;
				}
				break;
			case INSTRUCTION_FORMAT11x:
				{
				alt49=5;
				}
				break;
			case INSTRUCTION_FORMAT12x:
			case INSTRUCTION_FORMAT12x_OR_ID:
				{
				alt49=6;
				}
				break;
			case INSTRUCTION_FORMAT20bc:
				{
				alt49=7;
				}
				break;
			case INSTRUCTION_FORMAT20t:
				{
				alt49=8;
				}
				break;
			case INSTRUCTION_FORMAT21c_FIELD:
				{
				alt49=9;
				}
				break;
			case INSTRUCTION_FORMAT21c_FIELD_ODEX:
				{
				alt49=10;
				}
				break;
			case INSTRUCTION_FORMAT21c_STRING:
				{
				alt49=11;
				}
				break;
			case INSTRUCTION_FORMAT21c_TYPE:
				{
				alt49=12;
				}
				break;
			case INSTRUCTION_FORMAT21ih:
				{
				alt49=13;
				}
				break;
			case INSTRUCTION_FORMAT21lh:
				{
				alt49=14;
				}
				break;
			case INSTRUCTION_FORMAT21s:
				{
				alt49=15;
				}
				break;
			case INSTRUCTION_FORMAT21t:
				{
				alt49=16;
				}
				break;
			case INSTRUCTION_FORMAT22b:
				{
				alt49=17;
				}
				break;
			case INSTRUCTION_FORMAT22c_FIELD:
				{
				alt49=18;
				}
				break;
			case INSTRUCTION_FORMAT22c_FIELD_ODEX:
				{
				alt49=19;
				}
				break;
			case INSTRUCTION_FORMAT22c_TYPE:
				{
				alt49=20;
				}
				break;
			case INSTRUCTION_FORMAT22cs_FIELD:
				{
				alt49=21;
				}
				break;
			case INSTRUCTION_FORMAT22s:
			case INSTRUCTION_FORMAT22s_OR_ID:
				{
				alt49=22;
				}
				break;
			case INSTRUCTION_FORMAT22t:
				{
				alt49=23;
				}
				break;
			case INSTRUCTION_FORMAT22x:
				{
				alt49=24;
				}
				break;
			case INSTRUCTION_FORMAT23x:
				{
				alt49=25;
				}
				break;
			case INSTRUCTION_FORMAT30t:
				{
				alt49=26;
				}
				break;
			case INSTRUCTION_FORMAT31c:
				{
				alt49=27;
				}
				break;
			case INSTRUCTION_FORMAT31i:
			case INSTRUCTION_FORMAT31i_OR_ID:
				{
				alt49=28;
				}
				break;
			case INSTRUCTION_FORMAT31t:
				{
				alt49=29;
				}
				break;
			case INSTRUCTION_FORMAT32x:
				{
				alt49=30;
				}
				break;
			case INSTRUCTION_FORMAT35c_METHOD:
				{
				alt49=31;
				}
				break;
			case INSTRUCTION_FORMAT35c_TYPE:
				{
				alt49=32;
				}
				break;
			case INSTRUCTION_FORMAT35c_METHOD_ODEX:
				{
				alt49=33;
				}
				break;
			case INSTRUCTION_FORMAT35mi_METHOD:
				{
				alt49=34;
				}
				break;
			case INSTRUCTION_FORMAT35ms_METHOD:
				{
				alt49=35;
				}
				break;
			case INSTRUCTION_FORMAT3rc_METHOD:
				{
				alt49=36;
				}
				break;
			case INSTRUCTION_FORMAT3rc_METHOD_ODEX:
				{
				alt49=37;
				}
				break;
			case INSTRUCTION_FORMAT3rc_TYPE:
				{
				alt49=38;
				}
				break;
			case INSTRUCTION_FORMAT3rmi_METHOD:
				{
				alt49=39;
				}
				break;
			case INSTRUCTION_FORMAT3rms_METHOD:
				{
				alt49=40;
				}
				break;
			case INSTRUCTION_FORMAT51l:
				{
				alt49=41;
				}
				break;
			case ARRAY_DATA_DIRECTIVE:
				{
				alt49=42;
				}
				break;
			case PACKED_SWITCH_DIRECTIVE:
				{
				alt49=43;
				}
				break;
			case SPARSE_SWITCH_DIRECTIVE:
				{
				alt49=44;
				}
				break;
			default:
				NoViableAltException nvae =
					new NoViableAltException("", 49, 0, input);
				throw nvae;
			}
			switch (alt49) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:797:5: insn_format10t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format10t_in_instruction3720);
					insn_format10t241=insn_format10t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format10t241.getTree());

					}
					break;
				case 2 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:798:5: insn_format10x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format10x_in_instruction3726);
					insn_format10x242=insn_format10x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format10x242.getTree());

					}
					break;
				case 3 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:799:5: insn_format10x_odex
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format10x_odex_in_instruction3732);
					insn_format10x_odex243=insn_format10x_odex();
					state._fsp--;

					adaptor.addChild(root_0, insn_format10x_odex243.getTree());

					}
					break;
				case 4 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:800:5: insn_format11n
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format11n_in_instruction3738);
					insn_format11n244=insn_format11n();
					state._fsp--;

					adaptor.addChild(root_0, insn_format11n244.getTree());

					}
					break;
				case 5 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:801:5: insn_format11x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format11x_in_instruction3744);
					insn_format11x245=insn_format11x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format11x245.getTree());

					}
					break;
				case 6 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:802:5: insn_format12x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format12x_in_instruction3750);
					insn_format12x246=insn_format12x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format12x246.getTree());

					}
					break;
				case 7 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:803:5: insn_format20bc
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format20bc_in_instruction3756);
					insn_format20bc247=insn_format20bc();
					state._fsp--;

					adaptor.addChild(root_0, insn_format20bc247.getTree());

					}
					break;
				case 8 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:804:5: insn_format20t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format20t_in_instruction3762);
					insn_format20t248=insn_format20t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format20t248.getTree());

					}
					break;
				case 9 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:805:5: insn_format21c_field
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21c_field_in_instruction3768);
					insn_format21c_field249=insn_format21c_field();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21c_field249.getTree());

					}
					break;
				case 10 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:806:5: insn_format21c_field_odex
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21c_field_odex_in_instruction3774);
					insn_format21c_field_odex250=insn_format21c_field_odex();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21c_field_odex250.getTree());

					}
					break;
				case 11 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:807:5: insn_format21c_string
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21c_string_in_instruction3780);
					insn_format21c_string251=insn_format21c_string();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21c_string251.getTree());

					}
					break;
				case 12 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:808:5: insn_format21c_type
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21c_type_in_instruction3786);
					insn_format21c_type252=insn_format21c_type();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21c_type252.getTree());

					}
					break;
				case 13 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:809:5: insn_format21ih
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21ih_in_instruction3792);
					insn_format21ih253=insn_format21ih();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21ih253.getTree());

					}
					break;
				case 14 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:810:5: insn_format21lh
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21lh_in_instruction3798);
					insn_format21lh254=insn_format21lh();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21lh254.getTree());

					}
					break;
				case 15 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:811:5: insn_format21s
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21s_in_instruction3804);
					insn_format21s255=insn_format21s();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21s255.getTree());

					}
					break;
				case 16 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:812:5: insn_format21t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format21t_in_instruction3810);
					insn_format21t256=insn_format21t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format21t256.getTree());

					}
					break;
				case 17 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:813:5: insn_format22b
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22b_in_instruction3816);
					insn_format22b257=insn_format22b();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22b257.getTree());

					}
					break;
				case 18 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:814:5: insn_format22c_field
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22c_field_in_instruction3822);
					insn_format22c_field258=insn_format22c_field();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22c_field258.getTree());

					}
					break;
				case 19 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:815:5: insn_format22c_field_odex
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22c_field_odex_in_instruction3828);
					insn_format22c_field_odex259=insn_format22c_field_odex();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22c_field_odex259.getTree());

					}
					break;
				case 20 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:816:5: insn_format22c_type
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22c_type_in_instruction3834);
					insn_format22c_type260=insn_format22c_type();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22c_type260.getTree());

					}
					break;
				case 21 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:817:5: insn_format22cs_field
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22cs_field_in_instruction3840);
					insn_format22cs_field261=insn_format22cs_field();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22cs_field261.getTree());

					}
					break;
				case 22 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:818:5: insn_format22s
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22s_in_instruction3846);
					insn_format22s262=insn_format22s();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22s262.getTree());

					}
					break;
				case 23 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:819:5: insn_format22t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22t_in_instruction3852);
					insn_format22t263=insn_format22t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22t263.getTree());

					}
					break;
				case 24 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:820:5: insn_format22x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format22x_in_instruction3858);
					insn_format22x264=insn_format22x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format22x264.getTree());

					}
					break;
				case 25 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:821:5: insn_format23x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format23x_in_instruction3864);
					insn_format23x265=insn_format23x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format23x265.getTree());

					}
					break;
				case 26 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:822:5: insn_format30t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format30t_in_instruction3870);
					insn_format30t266=insn_format30t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format30t266.getTree());

					}
					break;
				case 27 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:823:5: insn_format31c
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format31c_in_instruction3876);
					insn_format31c267=insn_format31c();
					state._fsp--;

					adaptor.addChild(root_0, insn_format31c267.getTree());

					}
					break;
				case 28 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:824:5: insn_format31i
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format31i_in_instruction3882);
					insn_format31i268=insn_format31i();
					state._fsp--;

					adaptor.addChild(root_0, insn_format31i268.getTree());

					}
					break;
				case 29 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:825:5: insn_format31t
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format31t_in_instruction3888);
					insn_format31t269=insn_format31t();
					state._fsp--;

					adaptor.addChild(root_0, insn_format31t269.getTree());

					}
					break;
				case 30 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:826:5: insn_format32x
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format32x_in_instruction3894);
					insn_format32x270=insn_format32x();
					state._fsp--;

					adaptor.addChild(root_0, insn_format32x270.getTree());

					}
					break;
				case 31 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:827:5: insn_format35c_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format35c_method_in_instruction3900);
					insn_format35c_method271=insn_format35c_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format35c_method271.getTree());

					}
					break;
				case 32 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:828:5: insn_format35c_type
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format35c_type_in_instruction3906);
					insn_format35c_type272=insn_format35c_type();
					state._fsp--;

					adaptor.addChild(root_0, insn_format35c_type272.getTree());

					}
					break;
				case 33 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:829:5: insn_format35c_method_odex
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format35c_method_odex_in_instruction3912);
					insn_format35c_method_odex273=insn_format35c_method_odex();
					state._fsp--;

					adaptor.addChild(root_0, insn_format35c_method_odex273.getTree());

					}
					break;
				case 34 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:830:5: insn_format35mi_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format35mi_method_in_instruction3918);
					insn_format35mi_method274=insn_format35mi_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format35mi_method274.getTree());

					}
					break;
				case 35 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:831:5: insn_format35ms_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format35ms_method_in_instruction3924);
					insn_format35ms_method275=insn_format35ms_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format35ms_method275.getTree());

					}
					break;
				case 36 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:832:5: insn_format3rc_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format3rc_method_in_instruction3930);
					insn_format3rc_method276=insn_format3rc_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format3rc_method276.getTree());

					}
					break;
				case 37 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:833:5: insn_format3rc_method_odex
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format3rc_method_odex_in_instruction3936);
					insn_format3rc_method_odex277=insn_format3rc_method_odex();
					state._fsp--;

					adaptor.addChild(root_0, insn_format3rc_method_odex277.getTree());

					}
					break;
				case 38 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:834:5: insn_format3rc_type
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format3rc_type_in_instruction3942);
					insn_format3rc_type278=insn_format3rc_type();
					state._fsp--;

					adaptor.addChild(root_0, insn_format3rc_type278.getTree());

					}
					break;
				case 39 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:835:5: insn_format3rmi_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format3rmi_method_in_instruction3948);
					insn_format3rmi_method279=insn_format3rmi_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format3rmi_method279.getTree());

					}
					break;
				case 40 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:836:5: insn_format3rms_method
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format3rms_method_in_instruction3954);
					insn_format3rms_method280=insn_format3rms_method();
					state._fsp--;

					adaptor.addChild(root_0, insn_format3rms_method280.getTree());

					}
					break;
				case 41 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:837:5: insn_format51l
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_format51l_in_instruction3960);
					insn_format51l281=insn_format51l();
					state._fsp--;

					adaptor.addChild(root_0, insn_format51l281.getTree());

					}
					break;
				case 42 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:838:5: insn_array_data_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_array_data_directive_in_instruction3966);
					insn_array_data_directive282=insn_array_data_directive();
					state._fsp--;

					adaptor.addChild(root_0, insn_array_data_directive282.getTree());

					}
					break;
				case 43 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:839:5: insn_packed_switch_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_packed_switch_directive_in_instruction3972);
					insn_packed_switch_directive283=insn_packed_switch_directive();
					state._fsp--;

					adaptor.addChild(root_0, insn_packed_switch_directive283.getTree());

					}
					break;
				case 44 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:840:5: insn_sparse_switch_directive
					{
					root_0 = (CommonTree)adaptor.nil();


					pushFollow(FOLLOW_insn_sparse_switch_directive_in_instruction3978);
					insn_sparse_switch_directive284=insn_sparse_switch_directive();
					state._fsp--;

					adaptor.addChild(root_0, insn_sparse_switch_directive284.getTree());

					}
					break;

			}
			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "instruction"


	public static class insn_format10t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format10t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:842:1: insn_format10t : INSTRUCTION_FORMAT10t label_ref -> ^( I_STATEMENT_FORMAT10t[$start, \"I_STATEMENT_FORMAT10t\"] INSTRUCTION_FORMAT10t label_ref ) ;
	public final smaliParser.insn_format10t_return insn_format10t() throws RecognitionException {
		smaliParser.insn_format10t_return retval = new smaliParser.insn_format10t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT10t285=null;
		ParserRuleReturnScope label_ref286 =null;

		CommonTree INSTRUCTION_FORMAT10t285_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT10t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT10t");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:843:3: ( INSTRUCTION_FORMAT10t label_ref -> ^( I_STATEMENT_FORMAT10t[$start, \"I_STATEMENT_FORMAT10t\"] INSTRUCTION_FORMAT10t label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:845:5: INSTRUCTION_FORMAT10t label_ref
			{
			INSTRUCTION_FORMAT10t285=(Token)match(input,INSTRUCTION_FORMAT10t,FOLLOW_INSTRUCTION_FORMAT10t_in_insn_format10t3998);
			stream_INSTRUCTION_FORMAT10t.add(INSTRUCTION_FORMAT10t285);

			pushFollow(FOLLOW_label_ref_in_insn_format10t4000);
			label_ref286=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref286.getTree());
			// AST REWRITE
			// elements: label_ref, INSTRUCTION_FORMAT10t
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 846:5: -> ^( I_STATEMENT_FORMAT10t[$start, \"I_STATEMENT_FORMAT10t\"] INSTRUCTION_FORMAT10t label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:846:8: ^( I_STATEMENT_FORMAT10t[$start, \"I_STATEMENT_FORMAT10t\"] INSTRUCTION_FORMAT10t label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT10t, (retval.start), "I_STATEMENT_FORMAT10t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT10t.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format10t"


	public static class insn_format10x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format10x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:848:1: insn_format10x : INSTRUCTION_FORMAT10x -> ^( I_STATEMENT_FORMAT10x[$start, \"I_STATEMENT_FORMAT10x\"] INSTRUCTION_FORMAT10x ) ;
	public final smaliParser.insn_format10x_return insn_format10x() throws RecognitionException {
		smaliParser.insn_format10x_return retval = new smaliParser.insn_format10x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT10x287=null;

		CommonTree INSTRUCTION_FORMAT10x287_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT10x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT10x");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:849:3: ( INSTRUCTION_FORMAT10x -> ^( I_STATEMENT_FORMAT10x[$start, \"I_STATEMENT_FORMAT10x\"] INSTRUCTION_FORMAT10x ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:850:5: INSTRUCTION_FORMAT10x
			{
			INSTRUCTION_FORMAT10x287=(Token)match(input,INSTRUCTION_FORMAT10x,FOLLOW_INSTRUCTION_FORMAT10x_in_insn_format10x4030);
			stream_INSTRUCTION_FORMAT10x.add(INSTRUCTION_FORMAT10x287);

			// AST REWRITE
			// elements: INSTRUCTION_FORMAT10x
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 851:5: -> ^( I_STATEMENT_FORMAT10x[$start, \"I_STATEMENT_FORMAT10x\"] INSTRUCTION_FORMAT10x )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:851:8: ^( I_STATEMENT_FORMAT10x[$start, \"I_STATEMENT_FORMAT10x\"] INSTRUCTION_FORMAT10x )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT10x, (retval.start), "I_STATEMENT_FORMAT10x"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT10x.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format10x"


	public static class insn_format10x_odex_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format10x_odex"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:853:1: insn_format10x_odex : INSTRUCTION_FORMAT10x_ODEX ;
	public final smaliParser.insn_format10x_odex_return insn_format10x_odex() throws RecognitionException {
		smaliParser.insn_format10x_odex_return retval = new smaliParser.insn_format10x_odex_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT10x_ODEX288=null;

		CommonTree INSTRUCTION_FORMAT10x_ODEX288_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:854:3: ( INSTRUCTION_FORMAT10x_ODEX )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:855:5: INSTRUCTION_FORMAT10x_ODEX
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT10x_ODEX288=(Token)match(input,INSTRUCTION_FORMAT10x_ODEX,FOLLOW_INSTRUCTION_FORMAT10x_ODEX_in_insn_format10x_odex4058);
			INSTRUCTION_FORMAT10x_ODEX288_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT10x_ODEX288);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT10x_ODEX288_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT10x_ODEX288!=null?INSTRUCTION_FORMAT10x_ODEX288.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format10x_odex"


	public static class insn_format11n_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format11n"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:860:1: insn_format11n : INSTRUCTION_FORMAT11n REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT11n[$start, \"I_STATEMENT_FORMAT11n\"] INSTRUCTION_FORMAT11n REGISTER integral_literal ) ;
	public final smaliParser.insn_format11n_return insn_format11n() throws RecognitionException {
		smaliParser.insn_format11n_return retval = new smaliParser.insn_format11n_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT11n289=null;
		Token REGISTER290=null;
		Token COMMA291=null;
		ParserRuleReturnScope integral_literal292 =null;

		CommonTree INSTRUCTION_FORMAT11n289_tree=null;
		CommonTree REGISTER290_tree=null;
		CommonTree COMMA291_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT11n=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT11n");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:861:3: ( INSTRUCTION_FORMAT11n REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT11n[$start, \"I_STATEMENT_FORMAT11n\"] INSTRUCTION_FORMAT11n REGISTER integral_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:862:5: INSTRUCTION_FORMAT11n REGISTER COMMA integral_literal
			{
			INSTRUCTION_FORMAT11n289=(Token)match(input,INSTRUCTION_FORMAT11n,FOLLOW_INSTRUCTION_FORMAT11n_in_insn_format11n4079);
			stream_INSTRUCTION_FORMAT11n.add(INSTRUCTION_FORMAT11n289);

			REGISTER290=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format11n4081);
			stream_REGISTER.add(REGISTER290);

			COMMA291=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format11n4083);
			stream_COMMA.add(COMMA291);

			pushFollow(FOLLOW_integral_literal_in_insn_format11n4085);
			integral_literal292=integral_literal();
			state._fsp--;

			stream_integral_literal.add(integral_literal292.getTree());
			// AST REWRITE
			// elements: REGISTER, integral_literal, INSTRUCTION_FORMAT11n
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 863:5: -> ^( I_STATEMENT_FORMAT11n[$start, \"I_STATEMENT_FORMAT11n\"] INSTRUCTION_FORMAT11n REGISTER integral_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:863:8: ^( I_STATEMENT_FORMAT11n[$start, \"I_STATEMENT_FORMAT11n\"] INSTRUCTION_FORMAT11n REGISTER integral_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT11n, (retval.start), "I_STATEMENT_FORMAT11n"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT11n.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_integral_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format11n"


	public static class insn_format11x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format11x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:865:1: insn_format11x : INSTRUCTION_FORMAT11x REGISTER -> ^( I_STATEMENT_FORMAT11x[$start, \"I_STATEMENT_FORMAT11x\"] INSTRUCTION_FORMAT11x REGISTER ) ;
	public final smaliParser.insn_format11x_return insn_format11x() throws RecognitionException {
		smaliParser.insn_format11x_return retval = new smaliParser.insn_format11x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT11x293=null;
		Token REGISTER294=null;

		CommonTree INSTRUCTION_FORMAT11x293_tree=null;
		CommonTree REGISTER294_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT11x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT11x");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:866:3: ( INSTRUCTION_FORMAT11x REGISTER -> ^( I_STATEMENT_FORMAT11x[$start, \"I_STATEMENT_FORMAT11x\"] INSTRUCTION_FORMAT11x REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:867:5: INSTRUCTION_FORMAT11x REGISTER
			{
			INSTRUCTION_FORMAT11x293=(Token)match(input,INSTRUCTION_FORMAT11x,FOLLOW_INSTRUCTION_FORMAT11x_in_insn_format11x4117);
			stream_INSTRUCTION_FORMAT11x.add(INSTRUCTION_FORMAT11x293);

			REGISTER294=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format11x4119);
			stream_REGISTER.add(REGISTER294);

			// AST REWRITE
			// elements: INSTRUCTION_FORMAT11x, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 868:5: -> ^( I_STATEMENT_FORMAT11x[$start, \"I_STATEMENT_FORMAT11x\"] INSTRUCTION_FORMAT11x REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:868:8: ^( I_STATEMENT_FORMAT11x[$start, \"I_STATEMENT_FORMAT11x\"] INSTRUCTION_FORMAT11x REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT11x, (retval.start), "I_STATEMENT_FORMAT11x"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT11x.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format11x"


	public static class insn_format12x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format12x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:870:1: insn_format12x : instruction_format12x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT12x[$start, \"I_STATEMENT_FORMAT12x\"] instruction_format12x REGISTER REGISTER ) ;
	public final smaliParser.insn_format12x_return insn_format12x() throws RecognitionException {
		smaliParser.insn_format12x_return retval = new smaliParser.insn_format12x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token REGISTER296=null;
		Token COMMA297=null;
		Token REGISTER298=null;
		ParserRuleReturnScope instruction_format12x295 =null;

		CommonTree REGISTER296_tree=null;
		CommonTree COMMA297_tree=null;
		CommonTree REGISTER298_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_instruction_format12x=new RewriteRuleSubtreeStream(adaptor,"rule instruction_format12x");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:871:3: ( instruction_format12x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT12x[$start, \"I_STATEMENT_FORMAT12x\"] instruction_format12x REGISTER REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:872:5: instruction_format12x REGISTER COMMA REGISTER
			{
			pushFollow(FOLLOW_instruction_format12x_in_insn_format12x4149);
			instruction_format12x295=instruction_format12x();
			state._fsp--;

			stream_instruction_format12x.add(instruction_format12x295.getTree());
			REGISTER296=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format12x4151);
			stream_REGISTER.add(REGISTER296);

			COMMA297=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format12x4153);
			stream_COMMA.add(COMMA297);

			REGISTER298=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format12x4155);
			stream_REGISTER.add(REGISTER298);

			// AST REWRITE
			// elements: REGISTER, instruction_format12x, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 873:5: -> ^( I_STATEMENT_FORMAT12x[$start, \"I_STATEMENT_FORMAT12x\"] instruction_format12x REGISTER REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:873:8: ^( I_STATEMENT_FORMAT12x[$start, \"I_STATEMENT_FORMAT12x\"] instruction_format12x REGISTER REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT12x, (retval.start), "I_STATEMENT_FORMAT12x"), root_1);
				adaptor.addChild(root_1, stream_instruction_format12x.nextTree());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format12x"


	public static class insn_format20bc_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format20bc"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:875:1: insn_format20bc : INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference -> ^( I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE verification_error_reference ) ;
	public final smaliParser.insn_format20bc_return insn_format20bc() throws RecognitionException {
		smaliParser.insn_format20bc_return retval = new smaliParser.insn_format20bc_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT20bc299=null;
		Token VERIFICATION_ERROR_TYPE300=null;
		Token COMMA301=null;
		ParserRuleReturnScope verification_error_reference302 =null;

		CommonTree INSTRUCTION_FORMAT20bc299_tree=null;
		CommonTree VERIFICATION_ERROR_TYPE300_tree=null;
		CommonTree COMMA301_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT20bc=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT20bc");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_VERIFICATION_ERROR_TYPE=new RewriteRuleTokenStream(adaptor,"token VERIFICATION_ERROR_TYPE");
		RewriteRuleSubtreeStream stream_verification_error_reference=new RewriteRuleSubtreeStream(adaptor,"rule verification_error_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:876:3: ( INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference -> ^( I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE verification_error_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:877:5: INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE COMMA verification_error_reference
			{
			INSTRUCTION_FORMAT20bc299=(Token)match(input,INSTRUCTION_FORMAT20bc,FOLLOW_INSTRUCTION_FORMAT20bc_in_insn_format20bc4187);
			stream_INSTRUCTION_FORMAT20bc.add(INSTRUCTION_FORMAT20bc299);

			VERIFICATION_ERROR_TYPE300=(Token)match(input,VERIFICATION_ERROR_TYPE,FOLLOW_VERIFICATION_ERROR_TYPE_in_insn_format20bc4189);
			stream_VERIFICATION_ERROR_TYPE.add(VERIFICATION_ERROR_TYPE300);

			COMMA301=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format20bc4191);
			stream_COMMA.add(COMMA301);

			pushFollow(FOLLOW_verification_error_reference_in_insn_format20bc4193);
			verification_error_reference302=verification_error_reference();
			state._fsp--;

			stream_verification_error_reference.add(verification_error_reference302.getTree());

			      if (!allowOdex || opcodes.getOpcodeByName((INSTRUCTION_FORMAT20bc299!=null?INSTRUCTION_FORMAT20bc299.getText():null)) == null || apiLevel >= 14) {
			        throwOdexedInstructionException(input, (INSTRUCTION_FORMAT20bc299!=null?INSTRUCTION_FORMAT20bc299.getText():null));
			      }
			
			// AST REWRITE
			// elements: INSTRUCTION_FORMAT20bc, verification_error_reference, VERIFICATION_ERROR_TYPE
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 883:5: -> ^( I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE verification_error_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:883:8: ^( I_STATEMENT_FORMAT20bc INSTRUCTION_FORMAT20bc VERIFICATION_ERROR_TYPE verification_error_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT20bc, "I_STATEMENT_FORMAT20bc"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT20bc.nextNode());
				adaptor.addChild(root_1, stream_VERIFICATION_ERROR_TYPE.nextNode());
				adaptor.addChild(root_1, stream_verification_error_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format20bc"


	public static class insn_format20t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format20t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:885:1: insn_format20t : INSTRUCTION_FORMAT20t label_ref -> ^( I_STATEMENT_FORMAT20t[$start, \"I_STATEMENT_FORMAT20t\"] INSTRUCTION_FORMAT20t label_ref ) ;
	public final smaliParser.insn_format20t_return insn_format20t() throws RecognitionException {
		smaliParser.insn_format20t_return retval = new smaliParser.insn_format20t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT20t303=null;
		ParserRuleReturnScope label_ref304 =null;

		CommonTree INSTRUCTION_FORMAT20t303_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT20t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT20t");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:886:3: ( INSTRUCTION_FORMAT20t label_ref -> ^( I_STATEMENT_FORMAT20t[$start, \"I_STATEMENT_FORMAT20t\"] INSTRUCTION_FORMAT20t label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:887:5: INSTRUCTION_FORMAT20t label_ref
			{
			INSTRUCTION_FORMAT20t303=(Token)match(input,INSTRUCTION_FORMAT20t,FOLLOW_INSTRUCTION_FORMAT20t_in_insn_format20t4230);
			stream_INSTRUCTION_FORMAT20t.add(INSTRUCTION_FORMAT20t303);

			pushFollow(FOLLOW_label_ref_in_insn_format20t4232);
			label_ref304=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref304.getTree());
			// AST REWRITE
			// elements: INSTRUCTION_FORMAT20t, label_ref
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 888:5: -> ^( I_STATEMENT_FORMAT20t[$start, \"I_STATEMENT_FORMAT20t\"] INSTRUCTION_FORMAT20t label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:888:8: ^( I_STATEMENT_FORMAT20t[$start, \"I_STATEMENT_FORMAT20t\"] INSTRUCTION_FORMAT20t label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT20t, (retval.start), "I_STATEMENT_FORMAT20t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT20t.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format20t"


	public static class insn_format21c_field_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21c_field"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:890:1: insn_format21c_field : INSTRUCTION_FORMAT21c_FIELD REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD REGISTER field_reference ) ;
	public final smaliParser.insn_format21c_field_return insn_format21c_field() throws RecognitionException {
		smaliParser.insn_format21c_field_return retval = new smaliParser.insn_format21c_field_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21c_FIELD305=null;
		Token REGISTER306=null;
		Token COMMA307=null;
		ParserRuleReturnScope field_reference308 =null;

		CommonTree INSTRUCTION_FORMAT21c_FIELD305_tree=null;
		CommonTree REGISTER306_tree=null;
		CommonTree COMMA307_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_FIELD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_FIELD");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_field_reference=new RewriteRuleSubtreeStream(adaptor,"rule field_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:891:3: ( INSTRUCTION_FORMAT21c_FIELD REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD REGISTER field_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:892:5: INSTRUCTION_FORMAT21c_FIELD REGISTER COMMA field_reference
			{
			INSTRUCTION_FORMAT21c_FIELD305=(Token)match(input,INSTRUCTION_FORMAT21c_FIELD,FOLLOW_INSTRUCTION_FORMAT21c_FIELD_in_insn_format21c_field4262);
			stream_INSTRUCTION_FORMAT21c_FIELD.add(INSTRUCTION_FORMAT21c_FIELD305);

			REGISTER306=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21c_field4264);
			stream_REGISTER.add(REGISTER306);

			COMMA307=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21c_field4266);
			stream_COMMA.add(COMMA307);

			pushFollow(FOLLOW_field_reference_in_insn_format21c_field4268);
			field_reference308=field_reference();
			state._fsp--;

			stream_field_reference.add(field_reference308.getTree());
			// AST REWRITE
			// elements: field_reference, INSTRUCTION_FORMAT21c_FIELD, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 893:5: -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD REGISTER field_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:893:8: ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD REGISTER field_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21c_FIELD, (retval.start), "I_STATEMENT_FORMAT21c_FIELD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21c_FIELD.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_field_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21c_field"


	public static class insn_format21c_field_odex_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21c_field_odex"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:895:1: insn_format21c_field_odex : INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER field_reference ) ;
	public final smaliParser.insn_format21c_field_odex_return insn_format21c_field_odex() throws RecognitionException {
		smaliParser.insn_format21c_field_odex_return retval = new smaliParser.insn_format21c_field_odex_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21c_FIELD_ODEX309=null;
		Token REGISTER310=null;
		Token COMMA311=null;
		ParserRuleReturnScope field_reference312 =null;

		CommonTree INSTRUCTION_FORMAT21c_FIELD_ODEX309_tree=null;
		CommonTree REGISTER310_tree=null;
		CommonTree COMMA311_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_FIELD_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_FIELD_ODEX");
		RewriteRuleSubtreeStream stream_field_reference=new RewriteRuleSubtreeStream(adaptor,"rule field_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:896:3: ( INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER field_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:897:5: INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER COMMA field_reference
			{
			INSTRUCTION_FORMAT21c_FIELD_ODEX309=(Token)match(input,INSTRUCTION_FORMAT21c_FIELD_ODEX,FOLLOW_INSTRUCTION_FORMAT21c_FIELD_ODEX_in_insn_format21c_field_odex4300);
			stream_INSTRUCTION_FORMAT21c_FIELD_ODEX.add(INSTRUCTION_FORMAT21c_FIELD_ODEX309);

			REGISTER310=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21c_field_odex4302);
			stream_REGISTER.add(REGISTER310);

			COMMA311=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21c_field_odex4304);
			stream_COMMA.add(COMMA311);

			pushFollow(FOLLOW_field_reference_in_insn_format21c_field_odex4306);
			field_reference312=field_reference();
			state._fsp--;

			stream_field_reference.add(field_reference312.getTree());

			      if (!allowOdex || opcodes.getOpcodeByName((INSTRUCTION_FORMAT21c_FIELD_ODEX309!=null?INSTRUCTION_FORMAT21c_FIELD_ODEX309.getText():null)) == null || apiLevel >= 14) {
			        throwOdexedInstructionException(input, (INSTRUCTION_FORMAT21c_FIELD_ODEX309!=null?INSTRUCTION_FORMAT21c_FIELD_ODEX309.getText():null));
			      }
			
			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT21c_FIELD_ODEX, field_reference
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 903:5: -> ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER field_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:903:8: ^( I_STATEMENT_FORMAT21c_FIELD[$start, \"I_STATEMENT_FORMAT21c_FIELD\"] INSTRUCTION_FORMAT21c_FIELD_ODEX REGISTER field_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21c_FIELD, (retval.start), "I_STATEMENT_FORMAT21c_FIELD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21c_FIELD_ODEX.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_field_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21c_field_odex"


	public static class insn_format21c_string_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21c_string"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:905:1: insn_format21c_string : INSTRUCTION_FORMAT21c_STRING REGISTER COMMA STRING_LITERAL -> ^( I_STATEMENT_FORMAT21c_STRING[$start, \"I_STATEMENT_FORMAT21c_STRING\"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL ) ;
	public final smaliParser.insn_format21c_string_return insn_format21c_string() throws RecognitionException {
		smaliParser.insn_format21c_string_return retval = new smaliParser.insn_format21c_string_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21c_STRING313=null;
		Token REGISTER314=null;
		Token COMMA315=null;
		Token STRING_LITERAL316=null;

		CommonTree INSTRUCTION_FORMAT21c_STRING313_tree=null;
		CommonTree REGISTER314_tree=null;
		CommonTree COMMA315_tree=null;
		CommonTree STRING_LITERAL316_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_STRING=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_STRING");
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:906:3: ( INSTRUCTION_FORMAT21c_STRING REGISTER COMMA STRING_LITERAL -> ^( I_STATEMENT_FORMAT21c_STRING[$start, \"I_STATEMENT_FORMAT21c_STRING\"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:907:5: INSTRUCTION_FORMAT21c_STRING REGISTER COMMA STRING_LITERAL
			{
			INSTRUCTION_FORMAT21c_STRING313=(Token)match(input,INSTRUCTION_FORMAT21c_STRING,FOLLOW_INSTRUCTION_FORMAT21c_STRING_in_insn_format21c_string4344);
			stream_INSTRUCTION_FORMAT21c_STRING.add(INSTRUCTION_FORMAT21c_STRING313);

			REGISTER314=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21c_string4346);
			stream_REGISTER.add(REGISTER314);

			COMMA315=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21c_string4348);
			stream_COMMA.add(COMMA315);

			STRING_LITERAL316=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_insn_format21c_string4350);
			stream_STRING_LITERAL.add(STRING_LITERAL316);

			// AST REWRITE
			// elements: INSTRUCTION_FORMAT21c_STRING, REGISTER, STRING_LITERAL
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 908:5: -> ^( I_STATEMENT_FORMAT21c_STRING[$start, \"I_STATEMENT_FORMAT21c_STRING\"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:908:8: ^( I_STATEMENT_FORMAT21c_STRING[$start, \"I_STATEMENT_FORMAT21c_STRING\"] INSTRUCTION_FORMAT21c_STRING REGISTER STRING_LITERAL )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21c_STRING, (retval.start), "I_STATEMENT_FORMAT21c_STRING"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21c_STRING.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21c_string"


	public static class insn_format21c_type_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21c_type"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:910:1: insn_format21c_type : INSTRUCTION_FORMAT21c_TYPE REGISTER COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT21c_TYPE[$start, \"I_STATEMENT_FORMAT21c\"] INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor ) ;
	public final smaliParser.insn_format21c_type_return insn_format21c_type() throws RecognitionException {
		smaliParser.insn_format21c_type_return retval = new smaliParser.insn_format21c_type_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21c_TYPE317=null;
		Token REGISTER318=null;
		Token COMMA319=null;
		ParserRuleReturnScope nonvoid_type_descriptor320 =null;

		CommonTree INSTRUCTION_FORMAT21c_TYPE317_tree=null;
		CommonTree REGISTER318_tree=null;
		CommonTree COMMA319_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21c_TYPE");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:911:3: ( INSTRUCTION_FORMAT21c_TYPE REGISTER COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT21c_TYPE[$start, \"I_STATEMENT_FORMAT21c\"] INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:912:5: INSTRUCTION_FORMAT21c_TYPE REGISTER COMMA nonvoid_type_descriptor
			{
			INSTRUCTION_FORMAT21c_TYPE317=(Token)match(input,INSTRUCTION_FORMAT21c_TYPE,FOLLOW_INSTRUCTION_FORMAT21c_TYPE_in_insn_format21c_type4382);
			stream_INSTRUCTION_FORMAT21c_TYPE.add(INSTRUCTION_FORMAT21c_TYPE317);

			REGISTER318=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21c_type4384);
			stream_REGISTER.add(REGISTER318);

			COMMA319=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21c_type4386);
			stream_COMMA.add(COMMA319);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_insn_format21c_type4388);
			nonvoid_type_descriptor320=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor320.getTree());
			// AST REWRITE
			// elements: nonvoid_type_descriptor, REGISTER, INSTRUCTION_FORMAT21c_TYPE
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 913:5: -> ^( I_STATEMENT_FORMAT21c_TYPE[$start, \"I_STATEMENT_FORMAT21c\"] INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:913:8: ^( I_STATEMENT_FORMAT21c_TYPE[$start, \"I_STATEMENT_FORMAT21c\"] INSTRUCTION_FORMAT21c_TYPE REGISTER nonvoid_type_descriptor )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21c_TYPE, (retval.start), "I_STATEMENT_FORMAT21c"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21c_TYPE.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21c_type"


	public static class insn_format21ih_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21ih"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:915:1: insn_format21ih : INSTRUCTION_FORMAT21ih REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT21ih[$start, \"I_STATEMENT_FORMAT21ih\"] INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal ) ;
	public final smaliParser.insn_format21ih_return insn_format21ih() throws RecognitionException {
		smaliParser.insn_format21ih_return retval = new smaliParser.insn_format21ih_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21ih321=null;
		Token REGISTER322=null;
		Token COMMA323=null;
		ParserRuleReturnScope fixed_32bit_literal324 =null;

		CommonTree INSTRUCTION_FORMAT21ih321_tree=null;
		CommonTree REGISTER322_tree=null;
		CommonTree COMMA323_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21ih=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21ih");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_fixed_32bit_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_32bit_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:916:3: ( INSTRUCTION_FORMAT21ih REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT21ih[$start, \"I_STATEMENT_FORMAT21ih\"] INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:917:5: INSTRUCTION_FORMAT21ih REGISTER COMMA fixed_32bit_literal
			{
			INSTRUCTION_FORMAT21ih321=(Token)match(input,INSTRUCTION_FORMAT21ih,FOLLOW_INSTRUCTION_FORMAT21ih_in_insn_format21ih4420);
			stream_INSTRUCTION_FORMAT21ih.add(INSTRUCTION_FORMAT21ih321);

			REGISTER322=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21ih4422);
			stream_REGISTER.add(REGISTER322);

			COMMA323=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21ih4424);
			stream_COMMA.add(COMMA323);

			pushFollow(FOLLOW_fixed_32bit_literal_in_insn_format21ih4426);
			fixed_32bit_literal324=fixed_32bit_literal();
			state._fsp--;

			stream_fixed_32bit_literal.add(fixed_32bit_literal324.getTree());
			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT21ih, fixed_32bit_literal
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 918:5: -> ^( I_STATEMENT_FORMAT21ih[$start, \"I_STATEMENT_FORMAT21ih\"] INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:918:8: ^( I_STATEMENT_FORMAT21ih[$start, \"I_STATEMENT_FORMAT21ih\"] INSTRUCTION_FORMAT21ih REGISTER fixed_32bit_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21ih, (retval.start), "I_STATEMENT_FORMAT21ih"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21ih.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_fixed_32bit_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21ih"


	public static class insn_format21lh_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21lh"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:920:1: insn_format21lh : INSTRUCTION_FORMAT21lh REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT21lh[$start, \"I_STATEMENT_FORMAT21lh\"] INSTRUCTION_FORMAT21lh REGISTER fixed_32bit_literal ) ;
	public final smaliParser.insn_format21lh_return insn_format21lh() throws RecognitionException {
		smaliParser.insn_format21lh_return retval = new smaliParser.insn_format21lh_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21lh325=null;
		Token REGISTER326=null;
		Token COMMA327=null;
		ParserRuleReturnScope fixed_32bit_literal328 =null;

		CommonTree INSTRUCTION_FORMAT21lh325_tree=null;
		CommonTree REGISTER326_tree=null;
		CommonTree COMMA327_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21lh=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21lh");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_fixed_32bit_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_32bit_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:921:3: ( INSTRUCTION_FORMAT21lh REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT21lh[$start, \"I_STATEMENT_FORMAT21lh\"] INSTRUCTION_FORMAT21lh REGISTER fixed_32bit_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:922:5: INSTRUCTION_FORMAT21lh REGISTER COMMA fixed_32bit_literal
			{
			INSTRUCTION_FORMAT21lh325=(Token)match(input,INSTRUCTION_FORMAT21lh,FOLLOW_INSTRUCTION_FORMAT21lh_in_insn_format21lh4458);
			stream_INSTRUCTION_FORMAT21lh.add(INSTRUCTION_FORMAT21lh325);

			REGISTER326=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21lh4460);
			stream_REGISTER.add(REGISTER326);

			COMMA327=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21lh4462);
			stream_COMMA.add(COMMA327);

			pushFollow(FOLLOW_fixed_32bit_literal_in_insn_format21lh4464);
			fixed_32bit_literal328=fixed_32bit_literal();
			state._fsp--;

			stream_fixed_32bit_literal.add(fixed_32bit_literal328.getTree());
			// AST REWRITE
			// elements: INSTRUCTION_FORMAT21lh, fixed_32bit_literal, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 923:5: -> ^( I_STATEMENT_FORMAT21lh[$start, \"I_STATEMENT_FORMAT21lh\"] INSTRUCTION_FORMAT21lh REGISTER fixed_32bit_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:923:8: ^( I_STATEMENT_FORMAT21lh[$start, \"I_STATEMENT_FORMAT21lh\"] INSTRUCTION_FORMAT21lh REGISTER fixed_32bit_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21lh, (retval.start), "I_STATEMENT_FORMAT21lh"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21lh.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_fixed_32bit_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21lh"


	public static class insn_format21s_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21s"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:925:1: insn_format21s : INSTRUCTION_FORMAT21s REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT21s[$start, \"I_STATEMENT_FORMAT21s\"] INSTRUCTION_FORMAT21s REGISTER integral_literal ) ;
	public final smaliParser.insn_format21s_return insn_format21s() throws RecognitionException {
		smaliParser.insn_format21s_return retval = new smaliParser.insn_format21s_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21s329=null;
		Token REGISTER330=null;
		Token COMMA331=null;
		ParserRuleReturnScope integral_literal332 =null;

		CommonTree INSTRUCTION_FORMAT21s329_tree=null;
		CommonTree REGISTER330_tree=null;
		CommonTree COMMA331_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21s=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21s");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:926:3: ( INSTRUCTION_FORMAT21s REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT21s[$start, \"I_STATEMENT_FORMAT21s\"] INSTRUCTION_FORMAT21s REGISTER integral_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:927:5: INSTRUCTION_FORMAT21s REGISTER COMMA integral_literal
			{
			INSTRUCTION_FORMAT21s329=(Token)match(input,INSTRUCTION_FORMAT21s,FOLLOW_INSTRUCTION_FORMAT21s_in_insn_format21s4496);
			stream_INSTRUCTION_FORMAT21s.add(INSTRUCTION_FORMAT21s329);

			REGISTER330=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21s4498);
			stream_REGISTER.add(REGISTER330);

			COMMA331=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21s4500);
			stream_COMMA.add(COMMA331);

			pushFollow(FOLLOW_integral_literal_in_insn_format21s4502);
			integral_literal332=integral_literal();
			state._fsp--;

			stream_integral_literal.add(integral_literal332.getTree());
			// AST REWRITE
			// elements: INSTRUCTION_FORMAT21s, integral_literal, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 928:5: -> ^( I_STATEMENT_FORMAT21s[$start, \"I_STATEMENT_FORMAT21s\"] INSTRUCTION_FORMAT21s REGISTER integral_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:928:8: ^( I_STATEMENT_FORMAT21s[$start, \"I_STATEMENT_FORMAT21s\"] INSTRUCTION_FORMAT21s REGISTER integral_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21s, (retval.start), "I_STATEMENT_FORMAT21s"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21s.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_integral_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21s"


	public static class insn_format21t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format21t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:930:1: insn_format21t : INSTRUCTION_FORMAT21t REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT21t[$start, \"I_STATEMENT_FORMAT21t\"] INSTRUCTION_FORMAT21t REGISTER label_ref ) ;
	public final smaliParser.insn_format21t_return insn_format21t() throws RecognitionException {
		smaliParser.insn_format21t_return retval = new smaliParser.insn_format21t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT21t333=null;
		Token REGISTER334=null;
		Token COMMA335=null;
		ParserRuleReturnScope label_ref336 =null;

		CommonTree INSTRUCTION_FORMAT21t333_tree=null;
		CommonTree REGISTER334_tree=null;
		CommonTree COMMA335_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT21t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT21t");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:931:3: ( INSTRUCTION_FORMAT21t REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT21t[$start, \"I_STATEMENT_FORMAT21t\"] INSTRUCTION_FORMAT21t REGISTER label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:932:5: INSTRUCTION_FORMAT21t REGISTER COMMA label_ref
			{
			INSTRUCTION_FORMAT21t333=(Token)match(input,INSTRUCTION_FORMAT21t,FOLLOW_INSTRUCTION_FORMAT21t_in_insn_format21t4534);
			stream_INSTRUCTION_FORMAT21t.add(INSTRUCTION_FORMAT21t333);

			REGISTER334=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format21t4536);
			stream_REGISTER.add(REGISTER334);

			COMMA335=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format21t4538);
			stream_COMMA.add(COMMA335);

			pushFollow(FOLLOW_label_ref_in_insn_format21t4540);
			label_ref336=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref336.getTree());
			// AST REWRITE
			// elements: label_ref, REGISTER, INSTRUCTION_FORMAT21t
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 933:5: -> ^( I_STATEMENT_FORMAT21t[$start, \"I_STATEMENT_FORMAT21t\"] INSTRUCTION_FORMAT21t REGISTER label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:933:8: ^( I_STATEMENT_FORMAT21t[$start, \"I_STATEMENT_FORMAT21t\"] INSTRUCTION_FORMAT21t REGISTER label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT21t, (retval.start), "I_STATEMENT_FORMAT21t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT21t.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format21t"


	public static class insn_format22b_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22b"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:935:1: insn_format22b : INSTRUCTION_FORMAT22b REGISTER COMMA REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT22b[$start, \"I_STATEMENT_FORMAT22b\"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal ) ;
	public final smaliParser.insn_format22b_return insn_format22b() throws RecognitionException {
		smaliParser.insn_format22b_return retval = new smaliParser.insn_format22b_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22b337=null;
		Token REGISTER338=null;
		Token COMMA339=null;
		Token REGISTER340=null;
		Token COMMA341=null;
		ParserRuleReturnScope integral_literal342 =null;

		CommonTree INSTRUCTION_FORMAT22b337_tree=null;
		CommonTree REGISTER338_tree=null;
		CommonTree COMMA339_tree=null;
		CommonTree REGISTER340_tree=null;
		CommonTree COMMA341_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22b=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22b");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:936:3: ( INSTRUCTION_FORMAT22b REGISTER COMMA REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT22b[$start, \"I_STATEMENT_FORMAT22b\"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:937:5: INSTRUCTION_FORMAT22b REGISTER COMMA REGISTER COMMA integral_literal
			{
			INSTRUCTION_FORMAT22b337=(Token)match(input,INSTRUCTION_FORMAT22b,FOLLOW_INSTRUCTION_FORMAT22b_in_insn_format22b4572);
			stream_INSTRUCTION_FORMAT22b.add(INSTRUCTION_FORMAT22b337);

			REGISTER338=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22b4574);
			stream_REGISTER.add(REGISTER338);

			COMMA339=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22b4576);
			stream_COMMA.add(COMMA339);

			REGISTER340=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22b4578);
			stream_REGISTER.add(REGISTER340);

			COMMA341=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22b4580);
			stream_COMMA.add(COMMA341);

			pushFollow(FOLLOW_integral_literal_in_insn_format22b4582);
			integral_literal342=integral_literal();
			state._fsp--;

			stream_integral_literal.add(integral_literal342.getTree());
			// AST REWRITE
			// elements: integral_literal, REGISTER, INSTRUCTION_FORMAT22b, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 938:5: -> ^( I_STATEMENT_FORMAT22b[$start, \"I_STATEMENT_FORMAT22b\"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:938:8: ^( I_STATEMENT_FORMAT22b[$start, \"I_STATEMENT_FORMAT22b\"] INSTRUCTION_FORMAT22b REGISTER REGISTER integral_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22b, (retval.start), "I_STATEMENT_FORMAT22b"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22b.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_integral_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22b"


	public static class insn_format22c_field_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22c_field"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:940:1: insn_format22c_field : INSTRUCTION_FORMAT22c_FIELD REGISTER COMMA REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER field_reference ) ;
	public final smaliParser.insn_format22c_field_return insn_format22c_field() throws RecognitionException {
		smaliParser.insn_format22c_field_return retval = new smaliParser.insn_format22c_field_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22c_FIELD343=null;
		Token REGISTER344=null;
		Token COMMA345=null;
		Token REGISTER346=null;
		Token COMMA347=null;
		ParserRuleReturnScope field_reference348 =null;

		CommonTree INSTRUCTION_FORMAT22c_FIELD343_tree=null;
		CommonTree REGISTER344_tree=null;
		CommonTree COMMA345_tree=null;
		CommonTree REGISTER346_tree=null;
		CommonTree COMMA347_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_FIELD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_FIELD");
		RewriteRuleSubtreeStream stream_field_reference=new RewriteRuleSubtreeStream(adaptor,"rule field_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:941:3: ( INSTRUCTION_FORMAT22c_FIELD REGISTER COMMA REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER field_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:942:5: INSTRUCTION_FORMAT22c_FIELD REGISTER COMMA REGISTER COMMA field_reference
			{
			INSTRUCTION_FORMAT22c_FIELD343=(Token)match(input,INSTRUCTION_FORMAT22c_FIELD,FOLLOW_INSTRUCTION_FORMAT22c_FIELD_in_insn_format22c_field4616);
			stream_INSTRUCTION_FORMAT22c_FIELD.add(INSTRUCTION_FORMAT22c_FIELD343);

			REGISTER344=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_field4618);
			stream_REGISTER.add(REGISTER344);

			COMMA345=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_field4620);
			stream_COMMA.add(COMMA345);

			REGISTER346=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_field4622);
			stream_REGISTER.add(REGISTER346);

			COMMA347=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_field4624);
			stream_COMMA.add(COMMA347);

			pushFollow(FOLLOW_field_reference_in_insn_format22c_field4626);
			field_reference348=field_reference();
			state._fsp--;

			stream_field_reference.add(field_reference348.getTree());
			// AST REWRITE
			// elements: REGISTER, field_reference, INSTRUCTION_FORMAT22c_FIELD, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 943:5: -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER field_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:943:8: ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD REGISTER REGISTER field_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22c_FIELD, (retval.start), "I_STATEMENT_FORMAT22c_FIELD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22c_FIELD.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_field_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22c_field"


	public static class insn_format22c_field_odex_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22c_field_odex"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:945:1: insn_format22c_field_odex : INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER COMMA REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER REGISTER field_reference ) ;
	public final smaliParser.insn_format22c_field_odex_return insn_format22c_field_odex() throws RecognitionException {
		smaliParser.insn_format22c_field_odex_return retval = new smaliParser.insn_format22c_field_odex_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22c_FIELD_ODEX349=null;
		Token REGISTER350=null;
		Token COMMA351=null;
		Token REGISTER352=null;
		Token COMMA353=null;
		ParserRuleReturnScope field_reference354 =null;

		CommonTree INSTRUCTION_FORMAT22c_FIELD_ODEX349_tree=null;
		CommonTree REGISTER350_tree=null;
		CommonTree COMMA351_tree=null;
		CommonTree REGISTER352_tree=null;
		CommonTree COMMA353_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_FIELD_ODEX=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_FIELD_ODEX");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_field_reference=new RewriteRuleSubtreeStream(adaptor,"rule field_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:946:3: ( INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER COMMA REGISTER COMMA field_reference -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER REGISTER field_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:947:5: INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER COMMA REGISTER COMMA field_reference
			{
			INSTRUCTION_FORMAT22c_FIELD_ODEX349=(Token)match(input,INSTRUCTION_FORMAT22c_FIELD_ODEX,FOLLOW_INSTRUCTION_FORMAT22c_FIELD_ODEX_in_insn_format22c_field_odex4660);
			stream_INSTRUCTION_FORMAT22c_FIELD_ODEX.add(INSTRUCTION_FORMAT22c_FIELD_ODEX349);

			REGISTER350=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_field_odex4662);
			stream_REGISTER.add(REGISTER350);

			COMMA351=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_field_odex4664);
			stream_COMMA.add(COMMA351);

			REGISTER352=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_field_odex4666);
			stream_REGISTER.add(REGISTER352);

			COMMA353=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_field_odex4668);
			stream_COMMA.add(COMMA353);

			pushFollow(FOLLOW_field_reference_in_insn_format22c_field_odex4670);
			field_reference354=field_reference();
			state._fsp--;

			stream_field_reference.add(field_reference354.getTree());

			      if (!allowOdex || opcodes.getOpcodeByName((INSTRUCTION_FORMAT22c_FIELD_ODEX349!=null?INSTRUCTION_FORMAT22c_FIELD_ODEX349.getText():null)) == null || apiLevel >= 14) {
			        throwOdexedInstructionException(input, (INSTRUCTION_FORMAT22c_FIELD_ODEX349!=null?INSTRUCTION_FORMAT22c_FIELD_ODEX349.getText():null));
			      }
			
			// AST REWRITE
			// elements: REGISTER, field_reference, REGISTER, INSTRUCTION_FORMAT22c_FIELD_ODEX
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 953:5: -> ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER REGISTER field_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:953:8: ^( I_STATEMENT_FORMAT22c_FIELD[$start, \"I_STATEMENT_FORMAT22c_FIELD\"] INSTRUCTION_FORMAT22c_FIELD_ODEX REGISTER REGISTER field_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22c_FIELD, (retval.start), "I_STATEMENT_FORMAT22c_FIELD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22c_FIELD_ODEX.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_field_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22c_field_odex"


	public static class insn_format22c_type_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22c_type"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:955:1: insn_format22c_type : INSTRUCTION_FORMAT22c_TYPE REGISTER COMMA REGISTER COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT22c_TYPE[$start, \"I_STATEMENT_FORMAT22c_TYPE\"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor ) ;
	public final smaliParser.insn_format22c_type_return insn_format22c_type() throws RecognitionException {
		smaliParser.insn_format22c_type_return retval = new smaliParser.insn_format22c_type_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22c_TYPE355=null;
		Token REGISTER356=null;
		Token COMMA357=null;
		Token REGISTER358=null;
		Token COMMA359=null;
		ParserRuleReturnScope nonvoid_type_descriptor360 =null;

		CommonTree INSTRUCTION_FORMAT22c_TYPE355_tree=null;
		CommonTree REGISTER356_tree=null;
		CommonTree COMMA357_tree=null;
		CommonTree REGISTER358_tree=null;
		CommonTree COMMA359_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22c_TYPE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:956:3: ( INSTRUCTION_FORMAT22c_TYPE REGISTER COMMA REGISTER COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT22c_TYPE[$start, \"I_STATEMENT_FORMAT22c_TYPE\"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:957:5: INSTRUCTION_FORMAT22c_TYPE REGISTER COMMA REGISTER COMMA nonvoid_type_descriptor
			{
			INSTRUCTION_FORMAT22c_TYPE355=(Token)match(input,INSTRUCTION_FORMAT22c_TYPE,FOLLOW_INSTRUCTION_FORMAT22c_TYPE_in_insn_format22c_type4710);
			stream_INSTRUCTION_FORMAT22c_TYPE.add(INSTRUCTION_FORMAT22c_TYPE355);

			REGISTER356=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_type4712);
			stream_REGISTER.add(REGISTER356);

			COMMA357=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_type4714);
			stream_COMMA.add(COMMA357);

			REGISTER358=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22c_type4716);
			stream_REGISTER.add(REGISTER358);

			COMMA359=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22c_type4718);
			stream_COMMA.add(COMMA359);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_insn_format22c_type4720);
			nonvoid_type_descriptor360=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor360.getTree());
			// AST REWRITE
			// elements: REGISTER, nonvoid_type_descriptor, REGISTER, INSTRUCTION_FORMAT22c_TYPE
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 958:5: -> ^( I_STATEMENT_FORMAT22c_TYPE[$start, \"I_STATEMENT_FORMAT22c_TYPE\"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:958:8: ^( I_STATEMENT_FORMAT22c_TYPE[$start, \"I_STATEMENT_FORMAT22c_TYPE\"] INSTRUCTION_FORMAT22c_TYPE REGISTER REGISTER nonvoid_type_descriptor )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22c_TYPE, (retval.start), "I_STATEMENT_FORMAT22c_TYPE"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22c_TYPE.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22c_type"


	public static class insn_format22cs_field_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22cs_field"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:960:1: insn_format22cs_field : INSTRUCTION_FORMAT22cs_FIELD REGISTER COMMA REGISTER COMMA FIELD_OFFSET ;
	public final smaliParser.insn_format22cs_field_return insn_format22cs_field() throws RecognitionException {
		smaliParser.insn_format22cs_field_return retval = new smaliParser.insn_format22cs_field_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22cs_FIELD361=null;
		Token REGISTER362=null;
		Token COMMA363=null;
		Token REGISTER364=null;
		Token COMMA365=null;
		Token FIELD_OFFSET366=null;

		CommonTree INSTRUCTION_FORMAT22cs_FIELD361_tree=null;
		CommonTree REGISTER362_tree=null;
		CommonTree COMMA363_tree=null;
		CommonTree REGISTER364_tree=null;
		CommonTree COMMA365_tree=null;
		CommonTree FIELD_OFFSET366_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:961:3: ( INSTRUCTION_FORMAT22cs_FIELD REGISTER COMMA REGISTER COMMA FIELD_OFFSET )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:962:5: INSTRUCTION_FORMAT22cs_FIELD REGISTER COMMA REGISTER COMMA FIELD_OFFSET
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT22cs_FIELD361=(Token)match(input,INSTRUCTION_FORMAT22cs_FIELD,FOLLOW_INSTRUCTION_FORMAT22cs_FIELD_in_insn_format22cs_field4754);
			INSTRUCTION_FORMAT22cs_FIELD361_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT22cs_FIELD361);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT22cs_FIELD361_tree);

			REGISTER362=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22cs_field4756);
			REGISTER362_tree = (CommonTree)adaptor.create(REGISTER362);
			adaptor.addChild(root_0, REGISTER362_tree);

			COMMA363=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22cs_field4758);
			COMMA363_tree = (CommonTree)adaptor.create(COMMA363);
			adaptor.addChild(root_0, COMMA363_tree);

			REGISTER364=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22cs_field4760);
			REGISTER364_tree = (CommonTree)adaptor.create(REGISTER364);
			adaptor.addChild(root_0, REGISTER364_tree);

			COMMA365=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22cs_field4762);
			COMMA365_tree = (CommonTree)adaptor.create(COMMA365);
			adaptor.addChild(root_0, COMMA365_tree);

			FIELD_OFFSET366=(Token)match(input,FIELD_OFFSET,FOLLOW_FIELD_OFFSET_in_insn_format22cs_field4764);
			FIELD_OFFSET366_tree = (CommonTree)adaptor.create(FIELD_OFFSET366);
			adaptor.addChild(root_0, FIELD_OFFSET366_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT22cs_FIELD361!=null?INSTRUCTION_FORMAT22cs_FIELD361.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22cs_field"


	public static class insn_format22s_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22s"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:967:1: insn_format22s : instruction_format22s REGISTER COMMA REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT22s[$start, \"I_STATEMENT_FORMAT22s\"] instruction_format22s REGISTER REGISTER integral_literal ) ;
	public final smaliParser.insn_format22s_return insn_format22s() throws RecognitionException {
		smaliParser.insn_format22s_return retval = new smaliParser.insn_format22s_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token REGISTER368=null;
		Token COMMA369=null;
		Token REGISTER370=null;
		Token COMMA371=null;
		ParserRuleReturnScope instruction_format22s367 =null;
		ParserRuleReturnScope integral_literal372 =null;

		CommonTree REGISTER368_tree=null;
		CommonTree COMMA369_tree=null;
		CommonTree REGISTER370_tree=null;
		CommonTree COMMA371_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_integral_literal=new RewriteRuleSubtreeStream(adaptor,"rule integral_literal");
		RewriteRuleSubtreeStream stream_instruction_format22s=new RewriteRuleSubtreeStream(adaptor,"rule instruction_format22s");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:968:3: ( instruction_format22s REGISTER COMMA REGISTER COMMA integral_literal -> ^( I_STATEMENT_FORMAT22s[$start, \"I_STATEMENT_FORMAT22s\"] instruction_format22s REGISTER REGISTER integral_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:969:5: instruction_format22s REGISTER COMMA REGISTER COMMA integral_literal
			{
			pushFollow(FOLLOW_instruction_format22s_in_insn_format22s4785);
			instruction_format22s367=instruction_format22s();
			state._fsp--;

			stream_instruction_format22s.add(instruction_format22s367.getTree());
			REGISTER368=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22s4787);
			stream_REGISTER.add(REGISTER368);

			COMMA369=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22s4789);
			stream_COMMA.add(COMMA369);

			REGISTER370=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22s4791);
			stream_REGISTER.add(REGISTER370);

			COMMA371=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22s4793);
			stream_COMMA.add(COMMA371);

			pushFollow(FOLLOW_integral_literal_in_insn_format22s4795);
			integral_literal372=integral_literal();
			state._fsp--;

			stream_integral_literal.add(integral_literal372.getTree());
			// AST REWRITE
			// elements: REGISTER, integral_literal, REGISTER, instruction_format22s
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 970:5: -> ^( I_STATEMENT_FORMAT22s[$start, \"I_STATEMENT_FORMAT22s\"] instruction_format22s REGISTER REGISTER integral_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:970:8: ^( I_STATEMENT_FORMAT22s[$start, \"I_STATEMENT_FORMAT22s\"] instruction_format22s REGISTER REGISTER integral_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22s, (retval.start), "I_STATEMENT_FORMAT22s"), root_1);
				adaptor.addChild(root_1, stream_instruction_format22s.nextTree());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_integral_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22s"


	public static class insn_format22t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:972:1: insn_format22t : INSTRUCTION_FORMAT22t REGISTER COMMA REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT22t[$start, \"I_STATEMENT_FFORMAT22t\"] INSTRUCTION_FORMAT22t REGISTER REGISTER label_ref ) ;
	public final smaliParser.insn_format22t_return insn_format22t() throws RecognitionException {
		smaliParser.insn_format22t_return retval = new smaliParser.insn_format22t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22t373=null;
		Token REGISTER374=null;
		Token COMMA375=null;
		Token REGISTER376=null;
		Token COMMA377=null;
		ParserRuleReturnScope label_ref378 =null;

		CommonTree INSTRUCTION_FORMAT22t373_tree=null;
		CommonTree REGISTER374_tree=null;
		CommonTree COMMA375_tree=null;
		CommonTree REGISTER376_tree=null;
		CommonTree COMMA377_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22t");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:973:3: ( INSTRUCTION_FORMAT22t REGISTER COMMA REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT22t[$start, \"I_STATEMENT_FFORMAT22t\"] INSTRUCTION_FORMAT22t REGISTER REGISTER label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:974:5: INSTRUCTION_FORMAT22t REGISTER COMMA REGISTER COMMA label_ref
			{
			INSTRUCTION_FORMAT22t373=(Token)match(input,INSTRUCTION_FORMAT22t,FOLLOW_INSTRUCTION_FORMAT22t_in_insn_format22t4829);
			stream_INSTRUCTION_FORMAT22t.add(INSTRUCTION_FORMAT22t373);

			REGISTER374=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22t4831);
			stream_REGISTER.add(REGISTER374);

			COMMA375=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22t4833);
			stream_COMMA.add(COMMA375);

			REGISTER376=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22t4835);
			stream_REGISTER.add(REGISTER376);

			COMMA377=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22t4837);
			stream_COMMA.add(COMMA377);

			pushFollow(FOLLOW_label_ref_in_insn_format22t4839);
			label_ref378=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref378.getTree());
			// AST REWRITE
			// elements: REGISTER, label_ref, INSTRUCTION_FORMAT22t, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 975:5: -> ^( I_STATEMENT_FORMAT22t[$start, \"I_STATEMENT_FFORMAT22t\"] INSTRUCTION_FORMAT22t REGISTER REGISTER label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:975:8: ^( I_STATEMENT_FORMAT22t[$start, \"I_STATEMENT_FFORMAT22t\"] INSTRUCTION_FORMAT22t REGISTER REGISTER label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22t, (retval.start), "I_STATEMENT_FFORMAT22t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22t.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22t"


	public static class insn_format22x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format22x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:977:1: insn_format22x : INSTRUCTION_FORMAT22x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT22x[$start, \"I_STATEMENT_FORMAT22x\"] INSTRUCTION_FORMAT22x REGISTER REGISTER ) ;
	public final smaliParser.insn_format22x_return insn_format22x() throws RecognitionException {
		smaliParser.insn_format22x_return retval = new smaliParser.insn_format22x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT22x379=null;
		Token REGISTER380=null;
		Token COMMA381=null;
		Token REGISTER382=null;

		CommonTree INSTRUCTION_FORMAT22x379_tree=null;
		CommonTree REGISTER380_tree=null;
		CommonTree COMMA381_tree=null;
		CommonTree REGISTER382_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT22x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT22x");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:978:3: ( INSTRUCTION_FORMAT22x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT22x[$start, \"I_STATEMENT_FORMAT22x\"] INSTRUCTION_FORMAT22x REGISTER REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:979:5: INSTRUCTION_FORMAT22x REGISTER COMMA REGISTER
			{
			INSTRUCTION_FORMAT22x379=(Token)match(input,INSTRUCTION_FORMAT22x,FOLLOW_INSTRUCTION_FORMAT22x_in_insn_format22x4873);
			stream_INSTRUCTION_FORMAT22x.add(INSTRUCTION_FORMAT22x379);

			REGISTER380=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22x4875);
			stream_REGISTER.add(REGISTER380);

			COMMA381=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format22x4877);
			stream_COMMA.add(COMMA381);

			REGISTER382=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format22x4879);
			stream_REGISTER.add(REGISTER382);

			// AST REWRITE
			// elements: REGISTER, REGISTER, INSTRUCTION_FORMAT22x
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 980:5: -> ^( I_STATEMENT_FORMAT22x[$start, \"I_STATEMENT_FORMAT22x\"] INSTRUCTION_FORMAT22x REGISTER REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:980:8: ^( I_STATEMENT_FORMAT22x[$start, \"I_STATEMENT_FORMAT22x\"] INSTRUCTION_FORMAT22x REGISTER REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT22x, (retval.start), "I_STATEMENT_FORMAT22x"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT22x.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format22x"


	public static class insn_format23x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format23x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:982:1: insn_format23x : INSTRUCTION_FORMAT23x REGISTER COMMA REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT23x[$start, \"I_STATEMENT_FORMAT23x\"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER ) ;
	public final smaliParser.insn_format23x_return insn_format23x() throws RecognitionException {
		smaliParser.insn_format23x_return retval = new smaliParser.insn_format23x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT23x383=null;
		Token REGISTER384=null;
		Token COMMA385=null;
		Token REGISTER386=null;
		Token COMMA387=null;
		Token REGISTER388=null;

		CommonTree INSTRUCTION_FORMAT23x383_tree=null;
		CommonTree REGISTER384_tree=null;
		CommonTree COMMA385_tree=null;
		CommonTree REGISTER386_tree=null;
		CommonTree COMMA387_tree=null;
		CommonTree REGISTER388_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT23x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT23x");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:983:3: ( INSTRUCTION_FORMAT23x REGISTER COMMA REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT23x[$start, \"I_STATEMENT_FORMAT23x\"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:984:5: INSTRUCTION_FORMAT23x REGISTER COMMA REGISTER COMMA REGISTER
			{
			INSTRUCTION_FORMAT23x383=(Token)match(input,INSTRUCTION_FORMAT23x,FOLLOW_INSTRUCTION_FORMAT23x_in_insn_format23x4911);
			stream_INSTRUCTION_FORMAT23x.add(INSTRUCTION_FORMAT23x383);

			REGISTER384=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format23x4913);
			stream_REGISTER.add(REGISTER384);

			COMMA385=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format23x4915);
			stream_COMMA.add(COMMA385);

			REGISTER386=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format23x4917);
			stream_REGISTER.add(REGISTER386);

			COMMA387=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format23x4919);
			stream_COMMA.add(COMMA387);

			REGISTER388=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format23x4921);
			stream_REGISTER.add(REGISTER388);

			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT23x, REGISTER, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 985:5: -> ^( I_STATEMENT_FORMAT23x[$start, \"I_STATEMENT_FORMAT23x\"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:985:8: ^( I_STATEMENT_FORMAT23x[$start, \"I_STATEMENT_FORMAT23x\"] INSTRUCTION_FORMAT23x REGISTER REGISTER REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT23x, (retval.start), "I_STATEMENT_FORMAT23x"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT23x.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format23x"


	public static class insn_format30t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format30t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:987:1: insn_format30t : INSTRUCTION_FORMAT30t label_ref -> ^( I_STATEMENT_FORMAT30t[$start, \"I_STATEMENT_FORMAT30t\"] INSTRUCTION_FORMAT30t label_ref ) ;
	public final smaliParser.insn_format30t_return insn_format30t() throws RecognitionException {
		smaliParser.insn_format30t_return retval = new smaliParser.insn_format30t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT30t389=null;
		ParserRuleReturnScope label_ref390 =null;

		CommonTree INSTRUCTION_FORMAT30t389_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT30t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT30t");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:988:3: ( INSTRUCTION_FORMAT30t label_ref -> ^( I_STATEMENT_FORMAT30t[$start, \"I_STATEMENT_FORMAT30t\"] INSTRUCTION_FORMAT30t label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:989:5: INSTRUCTION_FORMAT30t label_ref
			{
			INSTRUCTION_FORMAT30t389=(Token)match(input,INSTRUCTION_FORMAT30t,FOLLOW_INSTRUCTION_FORMAT30t_in_insn_format30t4955);
			stream_INSTRUCTION_FORMAT30t.add(INSTRUCTION_FORMAT30t389);

			pushFollow(FOLLOW_label_ref_in_insn_format30t4957);
			label_ref390=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref390.getTree());
			// AST REWRITE
			// elements: label_ref, INSTRUCTION_FORMAT30t
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 990:5: -> ^( I_STATEMENT_FORMAT30t[$start, \"I_STATEMENT_FORMAT30t\"] INSTRUCTION_FORMAT30t label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:990:8: ^( I_STATEMENT_FORMAT30t[$start, \"I_STATEMENT_FORMAT30t\"] INSTRUCTION_FORMAT30t label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT30t, (retval.start), "I_STATEMENT_FORMAT30t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT30t.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format30t"


	public static class insn_format31c_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format31c"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:992:1: insn_format31c : INSTRUCTION_FORMAT31c REGISTER COMMA STRING_LITERAL -> ^( I_STATEMENT_FORMAT31c[$start, \"I_STATEMENT_FORMAT31c\"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL ) ;
	public final smaliParser.insn_format31c_return insn_format31c() throws RecognitionException {
		smaliParser.insn_format31c_return retval = new smaliParser.insn_format31c_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT31c391=null;
		Token REGISTER392=null;
		Token COMMA393=null;
		Token STRING_LITERAL394=null;

		CommonTree INSTRUCTION_FORMAT31c391_tree=null;
		CommonTree REGISTER392_tree=null;
		CommonTree COMMA393_tree=null;
		CommonTree STRING_LITERAL394_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT31c=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT31c");
		RewriteRuleTokenStream stream_STRING_LITERAL=new RewriteRuleTokenStream(adaptor,"token STRING_LITERAL");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:993:3: ( INSTRUCTION_FORMAT31c REGISTER COMMA STRING_LITERAL -> ^( I_STATEMENT_FORMAT31c[$start, \"I_STATEMENT_FORMAT31c\"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:994:5: INSTRUCTION_FORMAT31c REGISTER COMMA STRING_LITERAL
			{
			INSTRUCTION_FORMAT31c391=(Token)match(input,INSTRUCTION_FORMAT31c,FOLLOW_INSTRUCTION_FORMAT31c_in_insn_format31c4987);
			stream_INSTRUCTION_FORMAT31c.add(INSTRUCTION_FORMAT31c391);

			REGISTER392=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format31c4989);
			stream_REGISTER.add(REGISTER392);

			COMMA393=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format31c4991);
			stream_COMMA.add(COMMA393);

			STRING_LITERAL394=(Token)match(input,STRING_LITERAL,FOLLOW_STRING_LITERAL_in_insn_format31c4993);
			stream_STRING_LITERAL.add(STRING_LITERAL394);

			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT31c, STRING_LITERAL
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 995:5: -> ^( I_STATEMENT_FORMAT31c[$start, \"I_STATEMENT_FORMAT31c\"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:995:7: ^( I_STATEMENT_FORMAT31c[$start, \"I_STATEMENT_FORMAT31c\"] INSTRUCTION_FORMAT31c REGISTER STRING_LITERAL )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT31c, (retval.start), "I_STATEMENT_FORMAT31c"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT31c.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_STRING_LITERAL.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format31c"


	public static class insn_format31i_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format31i"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:997:1: insn_format31i : instruction_format31i REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT31i[$start, \"I_STATEMENT_FORMAT31i\"] instruction_format31i REGISTER fixed_32bit_literal ) ;
	public final smaliParser.insn_format31i_return insn_format31i() throws RecognitionException {
		smaliParser.insn_format31i_return retval = new smaliParser.insn_format31i_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token REGISTER396=null;
		Token COMMA397=null;
		ParserRuleReturnScope instruction_format31i395 =null;
		ParserRuleReturnScope fixed_32bit_literal398 =null;

		CommonTree REGISTER396_tree=null;
		CommonTree COMMA397_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_instruction_format31i=new RewriteRuleSubtreeStream(adaptor,"rule instruction_format31i");
		RewriteRuleSubtreeStream stream_fixed_32bit_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_32bit_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:998:3: ( instruction_format31i REGISTER COMMA fixed_32bit_literal -> ^( I_STATEMENT_FORMAT31i[$start, \"I_STATEMENT_FORMAT31i\"] instruction_format31i REGISTER fixed_32bit_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:999:5: instruction_format31i REGISTER COMMA fixed_32bit_literal
			{
			pushFollow(FOLLOW_instruction_format31i_in_insn_format31i5024);
			instruction_format31i395=instruction_format31i();
			state._fsp--;

			stream_instruction_format31i.add(instruction_format31i395.getTree());
			REGISTER396=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format31i5026);
			stream_REGISTER.add(REGISTER396);

			COMMA397=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format31i5028);
			stream_COMMA.add(COMMA397);

			pushFollow(FOLLOW_fixed_32bit_literal_in_insn_format31i5030);
			fixed_32bit_literal398=fixed_32bit_literal();
			state._fsp--;

			stream_fixed_32bit_literal.add(fixed_32bit_literal398.getTree());
			// AST REWRITE
			// elements: instruction_format31i, fixed_32bit_literal, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1000:5: -> ^( I_STATEMENT_FORMAT31i[$start, \"I_STATEMENT_FORMAT31i\"] instruction_format31i REGISTER fixed_32bit_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1000:8: ^( I_STATEMENT_FORMAT31i[$start, \"I_STATEMENT_FORMAT31i\"] instruction_format31i REGISTER fixed_32bit_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT31i, (retval.start), "I_STATEMENT_FORMAT31i"), root_1);
				adaptor.addChild(root_1, stream_instruction_format31i.nextTree());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_fixed_32bit_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format31i"


	public static class insn_format31t_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format31t"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1002:1: insn_format31t : INSTRUCTION_FORMAT31t REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT31t[$start, \"I_STATEMENT_FORMAT31t\"] INSTRUCTION_FORMAT31t REGISTER label_ref ) ;
	public final smaliParser.insn_format31t_return insn_format31t() throws RecognitionException {
		smaliParser.insn_format31t_return retval = new smaliParser.insn_format31t_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT31t399=null;
		Token REGISTER400=null;
		Token COMMA401=null;
		ParserRuleReturnScope label_ref402 =null;

		CommonTree INSTRUCTION_FORMAT31t399_tree=null;
		CommonTree REGISTER400_tree=null;
		CommonTree COMMA401_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT31t=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT31t");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1003:3: ( INSTRUCTION_FORMAT31t REGISTER COMMA label_ref -> ^( I_STATEMENT_FORMAT31t[$start, \"I_STATEMENT_FORMAT31t\"] INSTRUCTION_FORMAT31t REGISTER label_ref ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1004:5: INSTRUCTION_FORMAT31t REGISTER COMMA label_ref
			{
			INSTRUCTION_FORMAT31t399=(Token)match(input,INSTRUCTION_FORMAT31t,FOLLOW_INSTRUCTION_FORMAT31t_in_insn_format31t5062);
			stream_INSTRUCTION_FORMAT31t.add(INSTRUCTION_FORMAT31t399);

			REGISTER400=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format31t5064);
			stream_REGISTER.add(REGISTER400);

			COMMA401=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format31t5066);
			stream_COMMA.add(COMMA401);

			pushFollow(FOLLOW_label_ref_in_insn_format31t5068);
			label_ref402=label_ref();
			state._fsp--;

			stream_label_ref.add(label_ref402.getTree());
			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT31t, label_ref
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1005:5: -> ^( I_STATEMENT_FORMAT31t[$start, \"I_STATEMENT_FORMAT31t\"] INSTRUCTION_FORMAT31t REGISTER label_ref )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1005:8: ^( I_STATEMENT_FORMAT31t[$start, \"I_STATEMENT_FORMAT31t\"] INSTRUCTION_FORMAT31t REGISTER label_ref )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT31t, (retval.start), "I_STATEMENT_FORMAT31t"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT31t.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_label_ref.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format31t"


	public static class insn_format32x_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format32x"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1007:1: insn_format32x : INSTRUCTION_FORMAT32x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT32x[$start, \"I_STATEMENT_FORMAT32x\"] INSTRUCTION_FORMAT32x REGISTER REGISTER ) ;
	public final smaliParser.insn_format32x_return insn_format32x() throws RecognitionException {
		smaliParser.insn_format32x_return retval = new smaliParser.insn_format32x_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT32x403=null;
		Token REGISTER404=null;
		Token COMMA405=null;
		Token REGISTER406=null;

		CommonTree INSTRUCTION_FORMAT32x403_tree=null;
		CommonTree REGISTER404_tree=null;
		CommonTree COMMA405_tree=null;
		CommonTree REGISTER406_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT32x=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT32x");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1008:3: ( INSTRUCTION_FORMAT32x REGISTER COMMA REGISTER -> ^( I_STATEMENT_FORMAT32x[$start, \"I_STATEMENT_FORMAT32x\"] INSTRUCTION_FORMAT32x REGISTER REGISTER ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1009:5: INSTRUCTION_FORMAT32x REGISTER COMMA REGISTER
			{
			INSTRUCTION_FORMAT32x403=(Token)match(input,INSTRUCTION_FORMAT32x,FOLLOW_INSTRUCTION_FORMAT32x_in_insn_format32x5100);
			stream_INSTRUCTION_FORMAT32x.add(INSTRUCTION_FORMAT32x403);

			REGISTER404=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format32x5102);
			stream_REGISTER.add(REGISTER404);

			COMMA405=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format32x5104);
			stream_COMMA.add(COMMA405);

			REGISTER406=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format32x5106);
			stream_REGISTER.add(REGISTER406);

			// AST REWRITE
			// elements: REGISTER, INSTRUCTION_FORMAT32x, REGISTER
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1010:5: -> ^( I_STATEMENT_FORMAT32x[$start, \"I_STATEMENT_FORMAT32x\"] INSTRUCTION_FORMAT32x REGISTER REGISTER )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1010:8: ^( I_STATEMENT_FORMAT32x[$start, \"I_STATEMENT_FORMAT32x\"] INSTRUCTION_FORMAT32x REGISTER REGISTER )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT32x, (retval.start), "I_STATEMENT_FORMAT32x"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT32x.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format32x"


	public static class insn_format35c_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format35c_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1012:1: insn_format35c_method : INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference -> ^( I_STATEMENT_FORMAT35c_METHOD[$start, \"I_STATEMENT_FORMAT35c_METHOD\"] INSTRUCTION_FORMAT35c_METHOD register_list method_reference ) ;
	public final smaliParser.insn_format35c_method_return insn_format35c_method() throws RecognitionException {
		smaliParser.insn_format35c_method_return retval = new smaliParser.insn_format35c_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT35c_METHOD407=null;
		Token OPEN_BRACE408=null;
		Token CLOSE_BRACE410=null;
		Token COMMA411=null;
		ParserRuleReturnScope register_list409 =null;
		ParserRuleReturnScope method_reference412 =null;

		CommonTree INSTRUCTION_FORMAT35c_METHOD407_tree=null;
		CommonTree OPEN_BRACE408_tree=null;
		CommonTree CLOSE_BRACE410_tree=null;
		CommonTree COMMA411_tree=null;
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35c_METHOD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35c_METHOD");
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleSubtreeStream stream_register_list=new RewriteRuleSubtreeStream(adaptor,"rule register_list");
		RewriteRuleSubtreeStream stream_method_reference=new RewriteRuleSubtreeStream(adaptor,"rule method_reference");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1013:3: ( INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference -> ^( I_STATEMENT_FORMAT35c_METHOD[$start, \"I_STATEMENT_FORMAT35c_METHOD\"] INSTRUCTION_FORMAT35c_METHOD register_list method_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1014:5: INSTRUCTION_FORMAT35c_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
			{
			INSTRUCTION_FORMAT35c_METHOD407=(Token)match(input,INSTRUCTION_FORMAT35c_METHOD,FOLLOW_INSTRUCTION_FORMAT35c_METHOD_in_insn_format35c_method5138);
			stream_INSTRUCTION_FORMAT35c_METHOD.add(INSTRUCTION_FORMAT35c_METHOD407);

			OPEN_BRACE408=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format35c_method5140);
			stream_OPEN_BRACE.add(OPEN_BRACE408);

			pushFollow(FOLLOW_register_list_in_insn_format35c_method5142);
			register_list409=register_list();
			state._fsp--;

			stream_register_list.add(register_list409.getTree());
			CLOSE_BRACE410=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format35c_method5144);
			stream_CLOSE_BRACE.add(CLOSE_BRACE410);

			COMMA411=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format35c_method5146);
			stream_COMMA.add(COMMA411);

			pushFollow(FOLLOW_method_reference_in_insn_format35c_method5148);
			method_reference412=method_reference();
			state._fsp--;

			stream_method_reference.add(method_reference412.getTree());
			// AST REWRITE
			// elements: method_reference, register_list, INSTRUCTION_FORMAT35c_METHOD
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1015:5: -> ^( I_STATEMENT_FORMAT35c_METHOD[$start, \"I_STATEMENT_FORMAT35c_METHOD\"] INSTRUCTION_FORMAT35c_METHOD register_list method_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1015:8: ^( I_STATEMENT_FORMAT35c_METHOD[$start, \"I_STATEMENT_FORMAT35c_METHOD\"] INSTRUCTION_FORMAT35c_METHOD register_list method_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT35c_METHOD, (retval.start), "I_STATEMENT_FORMAT35c_METHOD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT35c_METHOD.nextNode());
				adaptor.addChild(root_1, stream_register_list.nextTree());
				adaptor.addChild(root_1, stream_method_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format35c_method"


	public static class insn_format35c_type_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format35c_type"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1017:1: insn_format35c_type : INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT35c_TYPE[$start, \"I_STATEMENT_FORMAT35c_TYPE\"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor ) ;
	public final smaliParser.insn_format35c_type_return insn_format35c_type() throws RecognitionException {
		smaliParser.insn_format35c_type_return retval = new smaliParser.insn_format35c_type_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT35c_TYPE413=null;
		Token OPEN_BRACE414=null;
		Token CLOSE_BRACE416=null;
		Token COMMA417=null;
		ParserRuleReturnScope register_list415 =null;
		ParserRuleReturnScope nonvoid_type_descriptor418 =null;

		CommonTree INSTRUCTION_FORMAT35c_TYPE413_tree=null;
		CommonTree OPEN_BRACE414_tree=null;
		CommonTree CLOSE_BRACE416_tree=null;
		CommonTree COMMA417_tree=null;
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT35c_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT35c_TYPE");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_register_list=new RewriteRuleSubtreeStream(adaptor,"rule register_list");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1018:3: ( INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT35c_TYPE[$start, \"I_STATEMENT_FORMAT35c_TYPE\"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1019:5: INSTRUCTION_FORMAT35c_TYPE OPEN_BRACE register_list CLOSE_BRACE COMMA nonvoid_type_descriptor
			{
			INSTRUCTION_FORMAT35c_TYPE413=(Token)match(input,INSTRUCTION_FORMAT35c_TYPE,FOLLOW_INSTRUCTION_FORMAT35c_TYPE_in_insn_format35c_type5180);
			stream_INSTRUCTION_FORMAT35c_TYPE.add(INSTRUCTION_FORMAT35c_TYPE413);

			OPEN_BRACE414=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format35c_type5182);
			stream_OPEN_BRACE.add(OPEN_BRACE414);

			pushFollow(FOLLOW_register_list_in_insn_format35c_type5184);
			register_list415=register_list();
			state._fsp--;

			stream_register_list.add(register_list415.getTree());
			CLOSE_BRACE416=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format35c_type5186);
			stream_CLOSE_BRACE.add(CLOSE_BRACE416);

			COMMA417=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format35c_type5188);
			stream_COMMA.add(COMMA417);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_insn_format35c_type5190);
			nonvoid_type_descriptor418=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor418.getTree());
			// AST REWRITE
			// elements: register_list, nonvoid_type_descriptor, INSTRUCTION_FORMAT35c_TYPE
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1020:5: -> ^( I_STATEMENT_FORMAT35c_TYPE[$start, \"I_STATEMENT_FORMAT35c_TYPE\"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1020:8: ^( I_STATEMENT_FORMAT35c_TYPE[$start, \"I_STATEMENT_FORMAT35c_TYPE\"] INSTRUCTION_FORMAT35c_TYPE register_list nonvoid_type_descriptor )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT35c_TYPE, (retval.start), "I_STATEMENT_FORMAT35c_TYPE"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT35c_TYPE.nextNode());
				adaptor.addChild(root_1, stream_register_list.nextTree());
				adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format35c_type"


	public static class insn_format35c_method_odex_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format35c_method_odex"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1022:1: insn_format35c_method_odex : INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference ;
	public final smaliParser.insn_format35c_method_odex_return insn_format35c_method_odex() throws RecognitionException {
		smaliParser.insn_format35c_method_odex_return retval = new smaliParser.insn_format35c_method_odex_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT35c_METHOD_ODEX419=null;
		Token OPEN_BRACE420=null;
		Token CLOSE_BRACE422=null;
		Token COMMA423=null;
		ParserRuleReturnScope register_list421 =null;
		ParserRuleReturnScope method_reference424 =null;

		CommonTree INSTRUCTION_FORMAT35c_METHOD_ODEX419_tree=null;
		CommonTree OPEN_BRACE420_tree=null;
		CommonTree CLOSE_BRACE422_tree=null;
		CommonTree COMMA423_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1023:3: ( INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1024:5: INSTRUCTION_FORMAT35c_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT35c_METHOD_ODEX419=(Token)match(input,INSTRUCTION_FORMAT35c_METHOD_ODEX,FOLLOW_INSTRUCTION_FORMAT35c_METHOD_ODEX_in_insn_format35c_method_odex5222);
			INSTRUCTION_FORMAT35c_METHOD_ODEX419_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT35c_METHOD_ODEX419);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT35c_METHOD_ODEX419_tree);

			OPEN_BRACE420=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format35c_method_odex5224);
			OPEN_BRACE420_tree = (CommonTree)adaptor.create(OPEN_BRACE420);
			adaptor.addChild(root_0, OPEN_BRACE420_tree);

			pushFollow(FOLLOW_register_list_in_insn_format35c_method_odex5226);
			register_list421=register_list();
			state._fsp--;

			adaptor.addChild(root_0, register_list421.getTree());

			CLOSE_BRACE422=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format35c_method_odex5228);
			CLOSE_BRACE422_tree = (CommonTree)adaptor.create(CLOSE_BRACE422);
			adaptor.addChild(root_0, CLOSE_BRACE422_tree);

			COMMA423=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format35c_method_odex5230);
			COMMA423_tree = (CommonTree)adaptor.create(COMMA423);
			adaptor.addChild(root_0, COMMA423_tree);

			pushFollow(FOLLOW_method_reference_in_insn_format35c_method_odex5232);
			method_reference424=method_reference();
			state._fsp--;

			adaptor.addChild(root_0, method_reference424.getTree());


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT35c_METHOD_ODEX419!=null?INSTRUCTION_FORMAT35c_METHOD_ODEX419.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format35c_method_odex"


	public static class insn_format35mi_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format35mi_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1029:1: insn_format35mi_method : INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX ;
	public final smaliParser.insn_format35mi_method_return insn_format35mi_method() throws RecognitionException {
		smaliParser.insn_format35mi_method_return retval = new smaliParser.insn_format35mi_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT35mi_METHOD425=null;
		Token OPEN_BRACE426=null;
		Token CLOSE_BRACE428=null;
		Token COMMA429=null;
		Token INLINE_INDEX430=null;
		ParserRuleReturnScope register_list427 =null;

		CommonTree INSTRUCTION_FORMAT35mi_METHOD425_tree=null;
		CommonTree OPEN_BRACE426_tree=null;
		CommonTree CLOSE_BRACE428_tree=null;
		CommonTree COMMA429_tree=null;
		CommonTree INLINE_INDEX430_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1030:3: ( INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1031:5: INSTRUCTION_FORMAT35mi_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA INLINE_INDEX
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT35mi_METHOD425=(Token)match(input,INSTRUCTION_FORMAT35mi_METHOD,FOLLOW_INSTRUCTION_FORMAT35mi_METHOD_in_insn_format35mi_method5253);
			INSTRUCTION_FORMAT35mi_METHOD425_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT35mi_METHOD425);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT35mi_METHOD425_tree);

			OPEN_BRACE426=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format35mi_method5255);
			OPEN_BRACE426_tree = (CommonTree)adaptor.create(OPEN_BRACE426);
			adaptor.addChild(root_0, OPEN_BRACE426_tree);

			pushFollow(FOLLOW_register_list_in_insn_format35mi_method5257);
			register_list427=register_list();
			state._fsp--;

			adaptor.addChild(root_0, register_list427.getTree());

			CLOSE_BRACE428=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format35mi_method5259);
			CLOSE_BRACE428_tree = (CommonTree)adaptor.create(CLOSE_BRACE428);
			adaptor.addChild(root_0, CLOSE_BRACE428_tree);

			COMMA429=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format35mi_method5261);
			COMMA429_tree = (CommonTree)adaptor.create(COMMA429);
			adaptor.addChild(root_0, COMMA429_tree);

			INLINE_INDEX430=(Token)match(input,INLINE_INDEX,FOLLOW_INLINE_INDEX_in_insn_format35mi_method5263);
			INLINE_INDEX430_tree = (CommonTree)adaptor.create(INLINE_INDEX430);
			adaptor.addChild(root_0, INLINE_INDEX430_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT35mi_METHOD425!=null?INSTRUCTION_FORMAT35mi_METHOD425.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format35mi_method"


	public static class insn_format35ms_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format35ms_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1036:1: insn_format35ms_method : INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX ;
	public final smaliParser.insn_format35ms_method_return insn_format35ms_method() throws RecognitionException {
		smaliParser.insn_format35ms_method_return retval = new smaliParser.insn_format35ms_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT35ms_METHOD431=null;
		Token OPEN_BRACE432=null;
		Token CLOSE_BRACE434=null;
		Token COMMA435=null;
		Token VTABLE_INDEX436=null;
		ParserRuleReturnScope register_list433 =null;

		CommonTree INSTRUCTION_FORMAT35ms_METHOD431_tree=null;
		CommonTree OPEN_BRACE432_tree=null;
		CommonTree CLOSE_BRACE434_tree=null;
		CommonTree COMMA435_tree=null;
		CommonTree VTABLE_INDEX436_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1037:3: ( INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1038:5: INSTRUCTION_FORMAT35ms_METHOD OPEN_BRACE register_list CLOSE_BRACE COMMA VTABLE_INDEX
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT35ms_METHOD431=(Token)match(input,INSTRUCTION_FORMAT35ms_METHOD,FOLLOW_INSTRUCTION_FORMAT35ms_METHOD_in_insn_format35ms_method5284);
			INSTRUCTION_FORMAT35ms_METHOD431_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT35ms_METHOD431);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT35ms_METHOD431_tree);

			OPEN_BRACE432=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format35ms_method5286);
			OPEN_BRACE432_tree = (CommonTree)adaptor.create(OPEN_BRACE432);
			adaptor.addChild(root_0, OPEN_BRACE432_tree);

			pushFollow(FOLLOW_register_list_in_insn_format35ms_method5288);
			register_list433=register_list();
			state._fsp--;

			adaptor.addChild(root_0, register_list433.getTree());

			CLOSE_BRACE434=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format35ms_method5290);
			CLOSE_BRACE434_tree = (CommonTree)adaptor.create(CLOSE_BRACE434);
			adaptor.addChild(root_0, CLOSE_BRACE434_tree);

			COMMA435=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format35ms_method5292);
			COMMA435_tree = (CommonTree)adaptor.create(COMMA435);
			adaptor.addChild(root_0, COMMA435_tree);

			VTABLE_INDEX436=(Token)match(input,VTABLE_INDEX,FOLLOW_VTABLE_INDEX_in_insn_format35ms_method5294);
			VTABLE_INDEX436_tree = (CommonTree)adaptor.create(VTABLE_INDEX436);
			adaptor.addChild(root_0, VTABLE_INDEX436_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT35ms_METHOD431!=null?INSTRUCTION_FORMAT35ms_METHOD431.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format35ms_method"


	public static class insn_format3rc_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format3rc_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1043:1: insn_format3rc_method : INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA method_reference -> ^( I_STATEMENT_FORMAT3rc_METHOD[$start, \"I_STATEMENT_FORMAT3rc_METHOD\"] INSTRUCTION_FORMAT3rc_METHOD register_range method_reference ) ;
	public final smaliParser.insn_format3rc_method_return insn_format3rc_method() throws RecognitionException {
		smaliParser.insn_format3rc_method_return retval = new smaliParser.insn_format3rc_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT3rc_METHOD437=null;
		Token OPEN_BRACE438=null;
		Token CLOSE_BRACE440=null;
		Token COMMA441=null;
		ParserRuleReturnScope register_range439 =null;
		ParserRuleReturnScope method_reference442 =null;

		CommonTree INSTRUCTION_FORMAT3rc_METHOD437_tree=null;
		CommonTree OPEN_BRACE438_tree=null;
		CommonTree CLOSE_BRACE440_tree=null;
		CommonTree COMMA441_tree=null;
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT3rc_METHOD=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT3rc_METHOD");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleSubtreeStream stream_method_reference=new RewriteRuleSubtreeStream(adaptor,"rule method_reference");
		RewriteRuleSubtreeStream stream_register_range=new RewriteRuleSubtreeStream(adaptor,"rule register_range");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1044:3: ( INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA method_reference -> ^( I_STATEMENT_FORMAT3rc_METHOD[$start, \"I_STATEMENT_FORMAT3rc_METHOD\"] INSTRUCTION_FORMAT3rc_METHOD register_range method_reference ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1045:5: INSTRUCTION_FORMAT3rc_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA method_reference
			{
			INSTRUCTION_FORMAT3rc_METHOD437=(Token)match(input,INSTRUCTION_FORMAT3rc_METHOD,FOLLOW_INSTRUCTION_FORMAT3rc_METHOD_in_insn_format3rc_method5315);
			stream_INSTRUCTION_FORMAT3rc_METHOD.add(INSTRUCTION_FORMAT3rc_METHOD437);

			OPEN_BRACE438=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format3rc_method5317);
			stream_OPEN_BRACE.add(OPEN_BRACE438);

			pushFollow(FOLLOW_register_range_in_insn_format3rc_method5319);
			register_range439=register_range();
			state._fsp--;

			stream_register_range.add(register_range439.getTree());
			CLOSE_BRACE440=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format3rc_method5321);
			stream_CLOSE_BRACE.add(CLOSE_BRACE440);

			COMMA441=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format3rc_method5323);
			stream_COMMA.add(COMMA441);

			pushFollow(FOLLOW_method_reference_in_insn_format3rc_method5325);
			method_reference442=method_reference();
			state._fsp--;

			stream_method_reference.add(method_reference442.getTree());
			// AST REWRITE
			// elements: INSTRUCTION_FORMAT3rc_METHOD, method_reference, register_range
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1046:5: -> ^( I_STATEMENT_FORMAT3rc_METHOD[$start, \"I_STATEMENT_FORMAT3rc_METHOD\"] INSTRUCTION_FORMAT3rc_METHOD register_range method_reference )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1046:8: ^( I_STATEMENT_FORMAT3rc_METHOD[$start, \"I_STATEMENT_FORMAT3rc_METHOD\"] INSTRUCTION_FORMAT3rc_METHOD register_range method_reference )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT3rc_METHOD, (retval.start), "I_STATEMENT_FORMAT3rc_METHOD"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT3rc_METHOD.nextNode());
				adaptor.addChild(root_1, stream_register_range.nextTree());
				adaptor.addChild(root_1, stream_method_reference.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format3rc_method"


	public static class insn_format3rc_method_odex_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format3rc_method_odex"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1048:1: insn_format3rc_method_odex : INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference ;
	public final smaliParser.insn_format3rc_method_odex_return insn_format3rc_method_odex() throws RecognitionException {
		smaliParser.insn_format3rc_method_odex_return retval = new smaliParser.insn_format3rc_method_odex_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT3rc_METHOD_ODEX443=null;
		Token OPEN_BRACE444=null;
		Token CLOSE_BRACE446=null;
		Token COMMA447=null;
		ParserRuleReturnScope register_list445 =null;
		ParserRuleReturnScope method_reference448 =null;

		CommonTree INSTRUCTION_FORMAT3rc_METHOD_ODEX443_tree=null;
		CommonTree OPEN_BRACE444_tree=null;
		CommonTree CLOSE_BRACE446_tree=null;
		CommonTree COMMA447_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1049:3: ( INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1050:5: INSTRUCTION_FORMAT3rc_METHOD_ODEX OPEN_BRACE register_list CLOSE_BRACE COMMA method_reference
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT3rc_METHOD_ODEX443=(Token)match(input,INSTRUCTION_FORMAT3rc_METHOD_ODEX,FOLLOW_INSTRUCTION_FORMAT3rc_METHOD_ODEX_in_insn_format3rc_method_odex5357);
			INSTRUCTION_FORMAT3rc_METHOD_ODEX443_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT3rc_METHOD_ODEX443);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT3rc_METHOD_ODEX443_tree);

			OPEN_BRACE444=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format3rc_method_odex5359);
			OPEN_BRACE444_tree = (CommonTree)adaptor.create(OPEN_BRACE444);
			adaptor.addChild(root_0, OPEN_BRACE444_tree);

			pushFollow(FOLLOW_register_list_in_insn_format3rc_method_odex5361);
			register_list445=register_list();
			state._fsp--;

			adaptor.addChild(root_0, register_list445.getTree());

			CLOSE_BRACE446=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format3rc_method_odex5363);
			CLOSE_BRACE446_tree = (CommonTree)adaptor.create(CLOSE_BRACE446);
			adaptor.addChild(root_0, CLOSE_BRACE446_tree);

			COMMA447=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format3rc_method_odex5365);
			COMMA447_tree = (CommonTree)adaptor.create(COMMA447);
			adaptor.addChild(root_0, COMMA447_tree);

			pushFollow(FOLLOW_method_reference_in_insn_format3rc_method_odex5367);
			method_reference448=method_reference();
			state._fsp--;

			adaptor.addChild(root_0, method_reference448.getTree());


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT3rc_METHOD_ODEX443!=null?INSTRUCTION_FORMAT3rc_METHOD_ODEX443.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format3rc_method_odex"


	public static class insn_format3rc_type_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format3rc_type"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1055:1: insn_format3rc_type : INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT3rc_TYPE[$start, \"I_STATEMENT_FORMAT3rc_TYPE\"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor ) ;
	public final smaliParser.insn_format3rc_type_return insn_format3rc_type() throws RecognitionException {
		smaliParser.insn_format3rc_type_return retval = new smaliParser.insn_format3rc_type_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT3rc_TYPE449=null;
		Token OPEN_BRACE450=null;
		Token CLOSE_BRACE452=null;
		Token COMMA453=null;
		ParserRuleReturnScope register_range451 =null;
		ParserRuleReturnScope nonvoid_type_descriptor454 =null;

		CommonTree INSTRUCTION_FORMAT3rc_TYPE449_tree=null;
		CommonTree OPEN_BRACE450_tree=null;
		CommonTree CLOSE_BRACE452_tree=null;
		CommonTree COMMA453_tree=null;
		RewriteRuleTokenStream stream_CLOSE_BRACE=new RewriteRuleTokenStream(adaptor,"token CLOSE_BRACE");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT3rc_TYPE=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT3rc_TYPE");
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_OPEN_BRACE=new RewriteRuleTokenStream(adaptor,"token OPEN_BRACE");
		RewriteRuleSubtreeStream stream_nonvoid_type_descriptor=new RewriteRuleSubtreeStream(adaptor,"rule nonvoid_type_descriptor");
		RewriteRuleSubtreeStream stream_register_range=new RewriteRuleSubtreeStream(adaptor,"rule register_range");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1056:3: ( INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor -> ^( I_STATEMENT_FORMAT3rc_TYPE[$start, \"I_STATEMENT_FORMAT3rc_TYPE\"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1057:5: INSTRUCTION_FORMAT3rc_TYPE OPEN_BRACE register_range CLOSE_BRACE COMMA nonvoid_type_descriptor
			{
			INSTRUCTION_FORMAT3rc_TYPE449=(Token)match(input,INSTRUCTION_FORMAT3rc_TYPE,FOLLOW_INSTRUCTION_FORMAT3rc_TYPE_in_insn_format3rc_type5388);
			stream_INSTRUCTION_FORMAT3rc_TYPE.add(INSTRUCTION_FORMAT3rc_TYPE449);

			OPEN_BRACE450=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format3rc_type5390);
			stream_OPEN_BRACE.add(OPEN_BRACE450);

			pushFollow(FOLLOW_register_range_in_insn_format3rc_type5392);
			register_range451=register_range();
			state._fsp--;

			stream_register_range.add(register_range451.getTree());
			CLOSE_BRACE452=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format3rc_type5394);
			stream_CLOSE_BRACE.add(CLOSE_BRACE452);

			COMMA453=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format3rc_type5396);
			stream_COMMA.add(COMMA453);

			pushFollow(FOLLOW_nonvoid_type_descriptor_in_insn_format3rc_type5398);
			nonvoid_type_descriptor454=nonvoid_type_descriptor();
			state._fsp--;

			stream_nonvoid_type_descriptor.add(nonvoid_type_descriptor454.getTree());
			// AST REWRITE
			// elements: nonvoid_type_descriptor, register_range, INSTRUCTION_FORMAT3rc_TYPE
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1058:5: -> ^( I_STATEMENT_FORMAT3rc_TYPE[$start, \"I_STATEMENT_FORMAT3rc_TYPE\"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1058:8: ^( I_STATEMENT_FORMAT3rc_TYPE[$start, \"I_STATEMENT_FORMAT3rc_TYPE\"] INSTRUCTION_FORMAT3rc_TYPE register_range nonvoid_type_descriptor )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT3rc_TYPE, (retval.start), "I_STATEMENT_FORMAT3rc_TYPE"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT3rc_TYPE.nextNode());
				adaptor.addChild(root_1, stream_register_range.nextTree());
				adaptor.addChild(root_1, stream_nonvoid_type_descriptor.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format3rc_type"


	public static class insn_format3rmi_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format3rmi_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1060:1: insn_format3rmi_method : INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX ;
	public final smaliParser.insn_format3rmi_method_return insn_format3rmi_method() throws RecognitionException {
		smaliParser.insn_format3rmi_method_return retval = new smaliParser.insn_format3rmi_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT3rmi_METHOD455=null;
		Token OPEN_BRACE456=null;
		Token CLOSE_BRACE458=null;
		Token COMMA459=null;
		Token INLINE_INDEX460=null;
		ParserRuleReturnScope register_range457 =null;

		CommonTree INSTRUCTION_FORMAT3rmi_METHOD455_tree=null;
		CommonTree OPEN_BRACE456_tree=null;
		CommonTree CLOSE_BRACE458_tree=null;
		CommonTree COMMA459_tree=null;
		CommonTree INLINE_INDEX460_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1061:3: ( INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1062:5: INSTRUCTION_FORMAT3rmi_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA INLINE_INDEX
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT3rmi_METHOD455=(Token)match(input,INSTRUCTION_FORMAT3rmi_METHOD,FOLLOW_INSTRUCTION_FORMAT3rmi_METHOD_in_insn_format3rmi_method5430);
			INSTRUCTION_FORMAT3rmi_METHOD455_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT3rmi_METHOD455);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT3rmi_METHOD455_tree);

			OPEN_BRACE456=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format3rmi_method5432);
			OPEN_BRACE456_tree = (CommonTree)adaptor.create(OPEN_BRACE456);
			adaptor.addChild(root_0, OPEN_BRACE456_tree);

			pushFollow(FOLLOW_register_range_in_insn_format3rmi_method5434);
			register_range457=register_range();
			state._fsp--;

			adaptor.addChild(root_0, register_range457.getTree());

			CLOSE_BRACE458=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format3rmi_method5436);
			CLOSE_BRACE458_tree = (CommonTree)adaptor.create(CLOSE_BRACE458);
			adaptor.addChild(root_0, CLOSE_BRACE458_tree);

			COMMA459=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format3rmi_method5438);
			COMMA459_tree = (CommonTree)adaptor.create(COMMA459);
			adaptor.addChild(root_0, COMMA459_tree);

			INLINE_INDEX460=(Token)match(input,INLINE_INDEX,FOLLOW_INLINE_INDEX_in_insn_format3rmi_method5440);
			INLINE_INDEX460_tree = (CommonTree)adaptor.create(INLINE_INDEX460);
			adaptor.addChild(root_0, INLINE_INDEX460_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT3rmi_METHOD455!=null?INSTRUCTION_FORMAT3rmi_METHOD455.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format3rmi_method"


	public static class insn_format3rms_method_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format3rms_method"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1067:1: insn_format3rms_method : INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX ;
	public final smaliParser.insn_format3rms_method_return insn_format3rms_method() throws RecognitionException {
		smaliParser.insn_format3rms_method_return retval = new smaliParser.insn_format3rms_method_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT3rms_METHOD461=null;
		Token OPEN_BRACE462=null;
		Token CLOSE_BRACE464=null;
		Token COMMA465=null;
		Token VTABLE_INDEX466=null;
		ParserRuleReturnScope register_range463 =null;

		CommonTree INSTRUCTION_FORMAT3rms_METHOD461_tree=null;
		CommonTree OPEN_BRACE462_tree=null;
		CommonTree CLOSE_BRACE464_tree=null;
		CommonTree COMMA465_tree=null;
		CommonTree VTABLE_INDEX466_tree=null;

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1068:3: ( INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1069:5: INSTRUCTION_FORMAT3rms_METHOD OPEN_BRACE register_range CLOSE_BRACE COMMA VTABLE_INDEX
			{
			root_0 = (CommonTree)adaptor.nil();


			INSTRUCTION_FORMAT3rms_METHOD461=(Token)match(input,INSTRUCTION_FORMAT3rms_METHOD,FOLLOW_INSTRUCTION_FORMAT3rms_METHOD_in_insn_format3rms_method5461);
			INSTRUCTION_FORMAT3rms_METHOD461_tree = (CommonTree)adaptor.create(INSTRUCTION_FORMAT3rms_METHOD461);
			adaptor.addChild(root_0, INSTRUCTION_FORMAT3rms_METHOD461_tree);

			OPEN_BRACE462=(Token)match(input,OPEN_BRACE,FOLLOW_OPEN_BRACE_in_insn_format3rms_method5463);
			OPEN_BRACE462_tree = (CommonTree)adaptor.create(OPEN_BRACE462);
			adaptor.addChild(root_0, OPEN_BRACE462_tree);

			pushFollow(FOLLOW_register_range_in_insn_format3rms_method5465);
			register_range463=register_range();
			state._fsp--;

			adaptor.addChild(root_0, register_range463.getTree());

			CLOSE_BRACE464=(Token)match(input,CLOSE_BRACE,FOLLOW_CLOSE_BRACE_in_insn_format3rms_method5467);
			CLOSE_BRACE464_tree = (CommonTree)adaptor.create(CLOSE_BRACE464);
			adaptor.addChild(root_0, CLOSE_BRACE464_tree);

			COMMA465=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format3rms_method5469);
			COMMA465_tree = (CommonTree)adaptor.create(COMMA465);
			adaptor.addChild(root_0, COMMA465_tree);

			VTABLE_INDEX466=(Token)match(input,VTABLE_INDEX,FOLLOW_VTABLE_INDEX_in_insn_format3rms_method5471);
			VTABLE_INDEX466_tree = (CommonTree)adaptor.create(VTABLE_INDEX466);
			adaptor.addChild(root_0, VTABLE_INDEX466_tree);


			      throwOdexedInstructionException(input, (INSTRUCTION_FORMAT3rms_METHOD461!=null?INSTRUCTION_FORMAT3rms_METHOD461.getText():null));
			
			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format3rms_method"


	public static class insn_format51l_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_format51l"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1074:1: insn_format51l : INSTRUCTION_FORMAT51l REGISTER COMMA fixed_literal -> ^( I_STATEMENT_FORMAT51l[$start, \"I_STATEMENT_FORMAT51l\"] INSTRUCTION_FORMAT51l REGISTER fixed_literal ) ;
	public final smaliParser.insn_format51l_return insn_format51l() throws RecognitionException {
		smaliParser.insn_format51l_return retval = new smaliParser.insn_format51l_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token INSTRUCTION_FORMAT51l467=null;
		Token REGISTER468=null;
		Token COMMA469=null;
		ParserRuleReturnScope fixed_literal470 =null;

		CommonTree INSTRUCTION_FORMAT51l467_tree=null;
		CommonTree REGISTER468_tree=null;
		CommonTree COMMA469_tree=null;
		RewriteRuleTokenStream stream_COMMA=new RewriteRuleTokenStream(adaptor,"token COMMA");
		RewriteRuleTokenStream stream_REGISTER=new RewriteRuleTokenStream(adaptor,"token REGISTER");
		RewriteRuleTokenStream stream_INSTRUCTION_FORMAT51l=new RewriteRuleTokenStream(adaptor,"token INSTRUCTION_FORMAT51l");
		RewriteRuleSubtreeStream stream_fixed_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1075:3: ( INSTRUCTION_FORMAT51l REGISTER COMMA fixed_literal -> ^( I_STATEMENT_FORMAT51l[$start, \"I_STATEMENT_FORMAT51l\"] INSTRUCTION_FORMAT51l REGISTER fixed_literal ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1076:5: INSTRUCTION_FORMAT51l REGISTER COMMA fixed_literal
			{
			INSTRUCTION_FORMAT51l467=(Token)match(input,INSTRUCTION_FORMAT51l,FOLLOW_INSTRUCTION_FORMAT51l_in_insn_format51l5492);
			stream_INSTRUCTION_FORMAT51l.add(INSTRUCTION_FORMAT51l467);

			REGISTER468=(Token)match(input,REGISTER,FOLLOW_REGISTER_in_insn_format51l5494);
			stream_REGISTER.add(REGISTER468);

			COMMA469=(Token)match(input,COMMA,FOLLOW_COMMA_in_insn_format51l5496);
			stream_COMMA.add(COMMA469);

			pushFollow(FOLLOW_fixed_literal_in_insn_format51l5498);
			fixed_literal470=fixed_literal();
			state._fsp--;

			stream_fixed_literal.add(fixed_literal470.getTree());
			// AST REWRITE
			// elements: REGISTER, fixed_literal, INSTRUCTION_FORMAT51l
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1077:5: -> ^( I_STATEMENT_FORMAT51l[$start, \"I_STATEMENT_FORMAT51l\"] INSTRUCTION_FORMAT51l REGISTER fixed_literal )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1077:8: ^( I_STATEMENT_FORMAT51l[$start, \"I_STATEMENT_FORMAT51l\"] INSTRUCTION_FORMAT51l REGISTER fixed_literal )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_FORMAT51l, (retval.start), "I_STATEMENT_FORMAT51l"), root_1);
				adaptor.addChild(root_1, stream_INSTRUCTION_FORMAT51l.nextNode());
				adaptor.addChild(root_1, stream_REGISTER.nextNode());
				adaptor.addChild(root_1, stream_fixed_literal.nextTree());
				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_format51l"


	public static class insn_array_data_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_array_data_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1079:1: insn_array_data_directive : ARRAY_DATA_DIRECTIVE parsed_integer_literal ( fixed_literal )* END_ARRAY_DATA_DIRECTIVE -> ^( I_STATEMENT_ARRAY_DATA[$start, \"I_STATEMENT_ARRAY_DATA\"] ^( I_ARRAY_ELEMENT_SIZE parsed_integer_literal ) ^( I_ARRAY_ELEMENTS ( fixed_literal )* ) ) ;
	public final smaliParser.insn_array_data_directive_return insn_array_data_directive() throws RecognitionException {
		smaliParser.insn_array_data_directive_return retval = new smaliParser.insn_array_data_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token ARRAY_DATA_DIRECTIVE471=null;
		Token END_ARRAY_DATA_DIRECTIVE474=null;
		ParserRuleReturnScope parsed_integer_literal472 =null;
		ParserRuleReturnScope fixed_literal473 =null;

		CommonTree ARRAY_DATA_DIRECTIVE471_tree=null;
		CommonTree END_ARRAY_DATA_DIRECTIVE474_tree=null;
		RewriteRuleTokenStream stream_ARRAY_DATA_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token ARRAY_DATA_DIRECTIVE");
		RewriteRuleTokenStream stream_END_ARRAY_DATA_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_ARRAY_DATA_DIRECTIVE");
		RewriteRuleSubtreeStream stream_fixed_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_literal");
		RewriteRuleSubtreeStream stream_parsed_integer_literal=new RewriteRuleSubtreeStream(adaptor,"rule parsed_integer_literal");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1080:3: ( ARRAY_DATA_DIRECTIVE parsed_integer_literal ( fixed_literal )* END_ARRAY_DATA_DIRECTIVE -> ^( I_STATEMENT_ARRAY_DATA[$start, \"I_STATEMENT_ARRAY_DATA\"] ^( I_ARRAY_ELEMENT_SIZE parsed_integer_literal ) ^( I_ARRAY_ELEMENTS ( fixed_literal )* ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1080:5: ARRAY_DATA_DIRECTIVE parsed_integer_literal ( fixed_literal )* END_ARRAY_DATA_DIRECTIVE
			{
			ARRAY_DATA_DIRECTIVE471=(Token)match(input,ARRAY_DATA_DIRECTIVE,FOLLOW_ARRAY_DATA_DIRECTIVE_in_insn_array_data_directive5525);
			stream_ARRAY_DATA_DIRECTIVE.add(ARRAY_DATA_DIRECTIVE471);

			pushFollow(FOLLOW_parsed_integer_literal_in_insn_array_data_directive5531);
			parsed_integer_literal472=parsed_integer_literal();
			state._fsp--;

			stream_parsed_integer_literal.add(parsed_integer_literal472.getTree());

			        int elementWidth = (parsed_integer_literal472!=null?((smaliParser.parsed_integer_literal_return)parsed_integer_literal472).value:0);
			        if (elementWidth != 4 && elementWidth != 8 && elementWidth != 1 && elementWidth != 2) {
			            throw new SemanticException(input, (retval.start), "Invalid element width: %d. Must be 1, 2, 4 or 8", elementWidth);
			        }
			
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1088:5: ( fixed_literal )*
			loop50:
			while (true) {
				int alt50=2;
				int LA50_0 = input.LA(1);
				if ( ((LA50_0 >= BOOL_LITERAL && LA50_0 <= BYTE_LITERAL)||LA50_0==CHAR_LITERAL||(LA50_0 >= DOUBLE_LITERAL && LA50_0 <= DOUBLE_LITERAL_OR_ID)||(LA50_0 >= FLOAT_LITERAL && LA50_0 <= FLOAT_LITERAL_OR_ID)||LA50_0==LONG_LITERAL||LA50_0==NEGATIVE_INTEGER_LITERAL||LA50_0==POSITIVE_INTEGER_LITERAL||LA50_0==SHORT_LITERAL) ) {
					alt50=1;
				}

				switch (alt50) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1088:5: fixed_literal
					{
					pushFollow(FOLLOW_fixed_literal_in_insn_array_data_directive5543);
					fixed_literal473=fixed_literal();
					state._fsp--;

					stream_fixed_literal.add(fixed_literal473.getTree());
					}
					break;

				default :
					break loop50;
				}
			}

			END_ARRAY_DATA_DIRECTIVE474=(Token)match(input,END_ARRAY_DATA_DIRECTIVE,FOLLOW_END_ARRAY_DATA_DIRECTIVE_in_insn_array_data_directive5546);
			stream_END_ARRAY_DATA_DIRECTIVE.add(END_ARRAY_DATA_DIRECTIVE474);

			// AST REWRITE
			// elements: fixed_literal, parsed_integer_literal
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1090:5: -> ^( I_STATEMENT_ARRAY_DATA[$start, \"I_STATEMENT_ARRAY_DATA\"] ^( I_ARRAY_ELEMENT_SIZE parsed_integer_literal ) ^( I_ARRAY_ELEMENTS ( fixed_literal )* ) )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1090:8: ^( I_STATEMENT_ARRAY_DATA[$start, \"I_STATEMENT_ARRAY_DATA\"] ^( I_ARRAY_ELEMENT_SIZE parsed_integer_literal ) ^( I_ARRAY_ELEMENTS ( fixed_literal )* ) )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_ARRAY_DATA, (retval.start), "I_STATEMENT_ARRAY_DATA"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1090:67: ^( I_ARRAY_ELEMENT_SIZE parsed_integer_literal )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ARRAY_ELEMENT_SIZE, "I_ARRAY_ELEMENT_SIZE"), root_2);
				adaptor.addChild(root_2, stream_parsed_integer_literal.nextTree());
				adaptor.addChild(root_1, root_2);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1091:8: ^( I_ARRAY_ELEMENTS ( fixed_literal )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_ARRAY_ELEMENTS, "I_ARRAY_ELEMENTS"), root_2);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1091:27: ( fixed_literal )*
				while ( stream_fixed_literal.hasNext() ) {
					adaptor.addChild(root_2, stream_fixed_literal.nextTree());
				}
				stream_fixed_literal.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_array_data_directive"


	public static class insn_packed_switch_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_packed_switch_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1093:1: insn_packed_switch_directive : PACKED_SWITCH_DIRECTIVE fixed_32bit_literal ( label_ref )* END_PACKED_SWITCH_DIRECTIVE -> ^( I_STATEMENT_PACKED_SWITCH[$start, \"I_STATEMENT_PACKED_SWITCH\"] ^( I_PACKED_SWITCH_START_KEY[$start, \"I_PACKED_SWITCH_START_KEY\"] fixed_32bit_literal ) ^( I_PACKED_SWITCH_ELEMENTS[$start, \"I_PACKED_SWITCH_ELEMENTS\"] ( label_ref )* ) ) ;
	public final smaliParser.insn_packed_switch_directive_return insn_packed_switch_directive() throws RecognitionException {
		smaliParser.insn_packed_switch_directive_return retval = new smaliParser.insn_packed_switch_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token PACKED_SWITCH_DIRECTIVE475=null;
		Token END_PACKED_SWITCH_DIRECTIVE478=null;
		ParserRuleReturnScope fixed_32bit_literal476 =null;
		ParserRuleReturnScope label_ref477 =null;

		CommonTree PACKED_SWITCH_DIRECTIVE475_tree=null;
		CommonTree END_PACKED_SWITCH_DIRECTIVE478_tree=null;
		RewriteRuleTokenStream stream_PACKED_SWITCH_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token PACKED_SWITCH_DIRECTIVE");
		RewriteRuleTokenStream stream_END_PACKED_SWITCH_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_PACKED_SWITCH_DIRECTIVE");
		RewriteRuleSubtreeStream stream_fixed_32bit_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_32bit_literal");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1094:5: ( PACKED_SWITCH_DIRECTIVE fixed_32bit_literal ( label_ref )* END_PACKED_SWITCH_DIRECTIVE -> ^( I_STATEMENT_PACKED_SWITCH[$start, \"I_STATEMENT_PACKED_SWITCH\"] ^( I_PACKED_SWITCH_START_KEY[$start, \"I_PACKED_SWITCH_START_KEY\"] fixed_32bit_literal ) ^( I_PACKED_SWITCH_ELEMENTS[$start, \"I_PACKED_SWITCH_ELEMENTS\"] ( label_ref )* ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1094:9: PACKED_SWITCH_DIRECTIVE fixed_32bit_literal ( label_ref )* END_PACKED_SWITCH_DIRECTIVE
			{
			PACKED_SWITCH_DIRECTIVE475=(Token)match(input,PACKED_SWITCH_DIRECTIVE,FOLLOW_PACKED_SWITCH_DIRECTIVE_in_insn_packed_switch_directive5592);
			stream_PACKED_SWITCH_DIRECTIVE.add(PACKED_SWITCH_DIRECTIVE475);

			pushFollow(FOLLOW_fixed_32bit_literal_in_insn_packed_switch_directive5598);
			fixed_32bit_literal476=fixed_32bit_literal();
			state._fsp--;

			stream_fixed_32bit_literal.add(fixed_32bit_literal476.getTree());
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1096:5: ( label_ref )*
			loop51:
			while (true) {
				int alt51=2;
				int LA51_0 = input.LA(1);
				if ( (LA51_0==COLON) ) {
					alt51=1;
				}

				switch (alt51) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1096:5: label_ref
					{
					pushFollow(FOLLOW_label_ref_in_insn_packed_switch_directive5604);
					label_ref477=label_ref();
					state._fsp--;

					stream_label_ref.add(label_ref477.getTree());
					}
					break;

				default :
					break loop51;
				}
			}

			END_PACKED_SWITCH_DIRECTIVE478=(Token)match(input,END_PACKED_SWITCH_DIRECTIVE,FOLLOW_END_PACKED_SWITCH_DIRECTIVE_in_insn_packed_switch_directive5611);
			stream_END_PACKED_SWITCH_DIRECTIVE.add(END_PACKED_SWITCH_DIRECTIVE478);

			// AST REWRITE
			// elements: fixed_32bit_literal, label_ref
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1098:5: -> ^( I_STATEMENT_PACKED_SWITCH[$start, \"I_STATEMENT_PACKED_SWITCH\"] ^( I_PACKED_SWITCH_START_KEY[$start, \"I_PACKED_SWITCH_START_KEY\"] fixed_32bit_literal ) ^( I_PACKED_SWITCH_ELEMENTS[$start, \"I_PACKED_SWITCH_ELEMENTS\"] ( label_ref )* ) )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1098:8: ^( I_STATEMENT_PACKED_SWITCH[$start, \"I_STATEMENT_PACKED_SWITCH\"] ^( I_PACKED_SWITCH_START_KEY[$start, \"I_PACKED_SWITCH_START_KEY\"] fixed_32bit_literal ) ^( I_PACKED_SWITCH_ELEMENTS[$start, \"I_PACKED_SWITCH_ELEMENTS\"] ( label_ref )* ) )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_PACKED_SWITCH, (retval.start), "I_STATEMENT_PACKED_SWITCH"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1099:10: ^( I_PACKED_SWITCH_START_KEY[$start, \"I_PACKED_SWITCH_START_KEY\"] fixed_32bit_literal )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PACKED_SWITCH_START_KEY, (retval.start), "I_PACKED_SWITCH_START_KEY"), root_2);
				adaptor.addChild(root_2, stream_fixed_32bit_literal.nextTree());
				adaptor.addChild(root_1, root_2);
				}

				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1100:10: ^( I_PACKED_SWITCH_ELEMENTS[$start, \"I_PACKED_SWITCH_ELEMENTS\"] ( label_ref )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_PACKED_SWITCH_ELEMENTS, (retval.start), "I_PACKED_SWITCH_ELEMENTS"), root_2);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1101:11: ( label_ref )*
				while ( stream_label_ref.hasNext() ) {
					adaptor.addChild(root_2, stream_label_ref.nextTree());
				}
				stream_label_ref.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_packed_switch_directive"


	public static class insn_sparse_switch_directive_return extends ParserRuleReturnScope {
		CommonTree tree;
		@Override
		public CommonTree getTree() { return tree; }
	};


	// $ANTLR start "insn_sparse_switch_directive"
	// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1104:1: insn_sparse_switch_directive : SPARSE_SWITCH_DIRECTIVE ( fixed_32bit_literal ARROW label_ref )* END_SPARSE_SWITCH_DIRECTIVE -> ^( I_STATEMENT_SPARSE_SWITCH[$start, \"I_STATEMENT_SPARSE_SWITCH\"] ^( I_SPARSE_SWITCH_ELEMENTS[$start, \"I_SPARSE_SWITCH_ELEMENTS\"] ( fixed_32bit_literal label_ref )* ) ) ;
	public final smaliParser.insn_sparse_switch_directive_return insn_sparse_switch_directive() throws RecognitionException {
		smaliParser.insn_sparse_switch_directive_return retval = new smaliParser.insn_sparse_switch_directive_return();
		retval.start = input.LT(1);

		CommonTree root_0 = null;

		Token SPARSE_SWITCH_DIRECTIVE479=null;
		Token ARROW481=null;
		Token END_SPARSE_SWITCH_DIRECTIVE483=null;
		ParserRuleReturnScope fixed_32bit_literal480 =null;
		ParserRuleReturnScope label_ref482 =null;

		CommonTree SPARSE_SWITCH_DIRECTIVE479_tree=null;
		CommonTree ARROW481_tree=null;
		CommonTree END_SPARSE_SWITCH_DIRECTIVE483_tree=null;
		RewriteRuleTokenStream stream_SPARSE_SWITCH_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token SPARSE_SWITCH_DIRECTIVE");
		RewriteRuleTokenStream stream_ARROW=new RewriteRuleTokenStream(adaptor,"token ARROW");
		RewriteRuleTokenStream stream_END_SPARSE_SWITCH_DIRECTIVE=new RewriteRuleTokenStream(adaptor,"token END_SPARSE_SWITCH_DIRECTIVE");
		RewriteRuleSubtreeStream stream_fixed_32bit_literal=new RewriteRuleSubtreeStream(adaptor,"rule fixed_32bit_literal");
		RewriteRuleSubtreeStream stream_label_ref=new RewriteRuleSubtreeStream(adaptor,"rule label_ref");

		try {
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1105:3: ( SPARSE_SWITCH_DIRECTIVE ( fixed_32bit_literal ARROW label_ref )* END_SPARSE_SWITCH_DIRECTIVE -> ^( I_STATEMENT_SPARSE_SWITCH[$start, \"I_STATEMENT_SPARSE_SWITCH\"] ^( I_SPARSE_SWITCH_ELEMENTS[$start, \"I_SPARSE_SWITCH_ELEMENTS\"] ( fixed_32bit_literal label_ref )* ) ) )
			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1105:7: SPARSE_SWITCH_DIRECTIVE ( fixed_32bit_literal ARROW label_ref )* END_SPARSE_SWITCH_DIRECTIVE
			{
			SPARSE_SWITCH_DIRECTIVE479=(Token)match(input,SPARSE_SWITCH_DIRECTIVE,FOLLOW_SPARSE_SWITCH_DIRECTIVE_in_insn_sparse_switch_directive5685);
			stream_SPARSE_SWITCH_DIRECTIVE.add(SPARSE_SWITCH_DIRECTIVE479);

			// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1106:5: ( fixed_32bit_literal ARROW label_ref )*
			loop52:
			while (true) {
				int alt52=2;
				int LA52_0 = input.LA(1);
				if ( ((LA52_0 >= BOOL_LITERAL && LA52_0 <= BYTE_LITERAL)||LA52_0==CHAR_LITERAL||(LA52_0 >= FLOAT_LITERAL && LA52_0 <= FLOAT_LITERAL_OR_ID)||LA52_0==LONG_LITERAL||LA52_0==NEGATIVE_INTEGER_LITERAL||LA52_0==POSITIVE_INTEGER_LITERAL||LA52_0==SHORT_LITERAL) ) {
					alt52=1;
				}

				switch (alt52) {
				case 1 :
					// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1106:6: fixed_32bit_literal ARROW label_ref
					{
					pushFollow(FOLLOW_fixed_32bit_literal_in_insn_sparse_switch_directive5692);
					fixed_32bit_literal480=fixed_32bit_literal();
					state._fsp--;

					stream_fixed_32bit_literal.add(fixed_32bit_literal480.getTree());
					ARROW481=(Token)match(input,ARROW,FOLLOW_ARROW_in_insn_sparse_switch_directive5694);
					stream_ARROW.add(ARROW481);

					pushFollow(FOLLOW_label_ref_in_insn_sparse_switch_directive5696);
					label_ref482=label_ref();
					state._fsp--;

					stream_label_ref.add(label_ref482.getTree());
					}
					break;

				default :
					break loop52;
				}
			}

			END_SPARSE_SWITCH_DIRECTIVE483=(Token)match(input,END_SPARSE_SWITCH_DIRECTIVE,FOLLOW_END_SPARSE_SWITCH_DIRECTIVE_in_insn_sparse_switch_directive5704);
			stream_END_SPARSE_SWITCH_DIRECTIVE.add(END_SPARSE_SWITCH_DIRECTIVE483);

			// AST REWRITE
			// elements: label_ref, fixed_32bit_literal
			// token labels:
			// rule labels: retval
			// token list labels:
			// rule list labels:
			// wildcard labels:
			retval.tree = root_0;
			RewriteRuleSubtreeStream stream_retval=new RewriteRuleSubtreeStream(adaptor,"rule retval",retval!=null?retval.getTree():null);

			root_0 = (CommonTree)adaptor.nil();
			// 1108:5: -> ^( I_STATEMENT_SPARSE_SWITCH[$start, \"I_STATEMENT_SPARSE_SWITCH\"] ^( I_SPARSE_SWITCH_ELEMENTS[$start, \"I_SPARSE_SWITCH_ELEMENTS\"] ( fixed_32bit_literal label_ref )* ) )
			{
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1108:8: ^( I_STATEMENT_SPARSE_SWITCH[$start, \"I_STATEMENT_SPARSE_SWITCH\"] ^( I_SPARSE_SWITCH_ELEMENTS[$start, \"I_SPARSE_SWITCH_ELEMENTS\"] ( fixed_32bit_literal label_ref )* ) )
				{
				CommonTree root_1 = (CommonTree)adaptor.nil();
				root_1 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_STATEMENT_SPARSE_SWITCH, (retval.start), "I_STATEMENT_SPARSE_SWITCH"), root_1);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1109:8: ^( I_SPARSE_SWITCH_ELEMENTS[$start, \"I_SPARSE_SWITCH_ELEMENTS\"] ( fixed_32bit_literal label_ref )* )
				{
				CommonTree root_2 = (CommonTree)adaptor.nil();
				root_2 = (CommonTree)adaptor.becomeRoot((CommonTree)adaptor.create(I_SPARSE_SWITCH_ELEMENTS, (retval.start), "I_SPARSE_SWITCH_ELEMENTS"), root_2);
				// /mnt/ssd1/workspace/aosp_master/external/smali/smali/src/main/antlr3/smaliParser.g:1109:71: ( fixed_32bit_literal label_ref )*
				while ( stream_label_ref.hasNext()||stream_fixed_32bit_literal.hasNext() ) {
					adaptor.addChild(root_2, stream_fixed_32bit_literal.nextTree());
					adaptor.addChild(root_2, stream_label_ref.nextTree());
				}
				stream_label_ref.reset();
				stream_fixed_32bit_literal.reset();

				adaptor.addChild(root_1, root_2);
				}

				adaptor.addChild(root_0, root_1);
				}

			}


			retval.tree = root_0;

			}

			retval.stop = input.LT(-1);

			retval.tree = (CommonTree)adaptor.rulePostProcessing(root_0);
			adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

		}
		catch (RecognitionException re) {
			reportError(re);
			recover(input,re);
			retval.tree = (CommonTree)adaptor.errorNode(input, retval.start, input.LT(-1), re);
		}
		finally {
			// do for sure before leaving
		}
		return retval;
	}
	// $ANTLR end "insn_sparse_switch_directive"

	// Delegated rules


	protected DFA28 dfa28 = new DFA28(this);
	protected DFA36 dfa36 = new DFA36(this);
	protected DFA38 dfa38 = new DFA38(this);
	static final String DFA28_eotS =
		"\55\uffff";
	static final String DFA28_eofS =
		"\55\uffff";
	static final String DFA28_minS =
		"\1\4\12\23\1\u00b8\35\23\2\uffff\1\u00b4\1\23";
	static final String DFA28_maxS =
		"\1\u00c5\12\u00b0\1\u00b8\35\u00b0\2\uffff\1\u00b8\1\u00b0";
	static final String DFA28_acceptS =
		"\51\uffff\1\1\1\2\2\uffff";
	static final String DFA28_specialS =
		"\55\uffff}>";
	static final String[] DFA28_transitionS = {
			"\1\2\1\uffff\1\16\3\uffff\1\10\14\uffff\1\7\17\uffff\1\6\2\uffff\1\17"+
			"\1\20\1\21\1\uffff\1\22\1\uffff\1\23\2\uffff\1\24\1\25\1\26\1\27\3\uffff"+
			"\1\30\1\uffff\1\31\1\32\1\33\1\34\1\uffff\1\35\1\36\1\uffff\1\37\3\uffff"+
			"\1\40\1\41\1\uffff\1\42\1\43\1\44\1\45\1\46\5\uffff\1\47\125\uffff\1"+
			"\50\1\uffff\1\5\1\11\6\uffff\1\13\1\uffff\1\4\1\14\1\uffff\1\12\3\uffff"+
			"\1\1\5\uffff\1\3\1\15",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\53",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"\1\51\u009c\uffff\1\52",
			"",
			"",
			"\1\54\3\uffff\1\53",
			"\1\51\u009c\uffff\1\52"
	};

	static final short[] DFA28_eot = DFA.unpackEncodedString(DFA28_eotS);
	static final short[] DFA28_eof = DFA.unpackEncodedString(DFA28_eofS);
	static final char[] DFA28_min = DFA.unpackEncodedStringToUnsignedChars(DFA28_minS);
	static final char[] DFA28_max = DFA.unpackEncodedStringToUnsignedChars(DFA28_maxS);
	static final short[] DFA28_accept = DFA.unpackEncodedString(DFA28_acceptS);
	static final short[] DFA28_special = DFA.unpackEncodedString(DFA28_specialS);
	static final short[][] DFA28_transition;

	static {
		int numStates = DFA28_transitionS.length;
		DFA28_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA28_transition[i] = DFA.unpackEncodedString(DFA28_transitionS[i]);
		}
	}

	protected class DFA28 extends DFA {

		public DFA28(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 28;
			this.eot = DFA28_eot;
			this.eof = DFA28_eof;
			this.min = DFA28_min;
			this.max = DFA28_max;
			this.accept = DFA28_accept;
			this.special = DFA28_special;
			this.transition = DFA28_transition;
		}
		@Override
		public String getDescription() {
			return "690:7: ( member_name COLON nonvoid_type_descriptor -> ^( I_ENCODED_FIELD ( reference_type_descriptor )? member_name nonvoid_type_descriptor ) | member_name method_prototype -> ^( I_ENCODED_METHOD ( reference_type_descriptor )? member_name method_prototype ) )";
		}
	}

	static final String DFA36_eotS =
		"\61\uffff";
	static final String DFA36_eofS =
		"\61\uffff";
	static final String DFA36_minS =
		"\1\4\1\5\1\11\12\23\1\u00b8\35\23\1\uffff\1\4\2\uffff\1\u00b4\1\23";
	static final String DFA36_maxS =
		"\1\u00c5\1\u00c0\1\11\12\u00b0\1\u00b8\35\u00b0\1\uffff\1\u00c5\2\uffff"+
		"\1\u00b8\1\u00b0";
	static final String DFA36_acceptS =
		"\53\uffff\1\1\1\uffff\1\2\1\3\2\uffff";
	static final String DFA36_specialS =
		"\61\uffff}>";
	static final String[] DFA36_transitionS = {
			"\1\4\1\uffff\1\20\1\uffff\1\2\1\uffff\1\12\4\uffff\1\1\7\uffff\1\11\17"+
			"\uffff\1\10\2\uffff\1\21\1\22\1\23\1\uffff\1\24\1\uffff\1\25\2\uffff"+
			"\1\26\1\27\1\30\1\31\3\uffff\1\32\1\uffff\1\33\1\34\1\35\1\36\1\uffff"+
			"\1\37\1\40\1\uffff\1\41\3\uffff\1\42\1\43\1\uffff\1\44\1\45\1\46\1\47"+
			"\1\50\5\uffff\1\51\125\uffff\1\52\1\uffff\1\7\1\13\6\uffff\1\15\1\uffff"+
			"\1\6\1\16\1\uffff\1\14\3\uffff\1\3\5\uffff\1\5\1\17",
			"\1\53\1\uffff\1\53\1\uffff\1\54\2\uffff\2\53\5\uffff\1\53\7\uffff\2"+
			"\53\5\uffff\1\53\7\uffff\54\53\121\uffff\3\53\7\uffff\2\53\6\uffff\1"+
			"\53\1\uffff\2\53\2\uffff\2\53",
			"\1\54",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\57",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"\1\55\u009c\uffff\1\56",
			"",
			"\1\4\1\uffff\1\20\3\uffff\1\12\14\uffff\1\11\17\uffff\1\10\2\uffff\1"+
			"\21\1\22\1\23\1\uffff\1\24\1\uffff\1\25\2\uffff\1\26\1\27\1\30\1\31\3"+
			"\uffff\1\32\1\uffff\1\33\1\34\1\35\1\36\1\uffff\1\37\1\40\1\uffff\1\41"+
			"\3\uffff\1\42\1\43\1\uffff\1\44\1\45\1\46\1\47\1\50\5\uffff\1\51\125"+
			"\uffff\1\52\1\uffff\1\7\1\13\6\uffff\1\15\1\uffff\1\6\1\16\1\uffff\1"+
			"\14\3\uffff\1\3\5\uffff\1\5\1\17",
			"",
			"",
			"\1\60\3\uffff\1\57",
			"\1\55\u009c\uffff\1\56"
	};

	static final short[] DFA36_eot = DFA.unpackEncodedString(DFA36_eotS);
	static final short[] DFA36_eof = DFA.unpackEncodedString(DFA36_eofS);
	static final char[] DFA36_min = DFA.unpackEncodedStringToUnsignedChars(DFA36_minS);
	static final char[] DFA36_max = DFA.unpackEncodedStringToUnsignedChars(DFA36_maxS);
	static final short[] DFA36_accept = DFA.unpackEncodedString(DFA36_acceptS);
	static final short[] DFA36_special = DFA.unpackEncodedString(DFA36_specialS);
	static final short[][] DFA36_transition;

	static {
		int numStates = DFA36_transitionS.length;
		DFA36_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA36_transition[i] = DFA.unpackEncodedString(DFA36_transitionS[i]);
		}
	}

	protected class DFA36 extends DFA {

		public DFA36(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 36;
			this.eot = DFA36_eot;
			this.eof = DFA36_eof;
			this.min = DFA36_min;
			this.max = DFA36_max;
			this.accept = DFA36_accept;
			this.special = DFA36_special;
			this.transition = DFA36_transition;
		}
		@Override
		public String getDescription() {
			return "718:1: verification_error_reference : ( CLASS_DESCRIPTOR | field_reference | method_reference );";
		}
	}

	static final String DFA38_eotS =
		"\101\uffff";
	static final String DFA38_eofS =
		"\101\uffff";
	static final String DFA38_minS =
		"\1\5\76\uffff\1\0\1\uffff";
	static final String DFA38_maxS =
		"\1\u00c0\76\uffff\1\0\1\uffff";
	static final String DFA38_acceptS =
		"\1\uffff\1\2\76\uffff\1\1";
	static final String DFA38_specialS =
		"\77\uffff\1\0\1\uffff}>";
	static final String[] DFA38_transitionS = {
			"\1\77\1\uffff\1\1\4\uffff\2\1\5\uffff\1\1\7\uffff\2\1\1\uffff\1\1\3\uffff"+
			"\1\1\7\uffff\54\1\121\uffff\3\1\7\uffff\2\1\6\uffff\1\1\1\uffff\2\1\2"+
			"\uffff\2\1",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"",
			"\1\uffff",
			""
	};

	static final short[] DFA38_eot = DFA.unpackEncodedString(DFA38_eotS);
	static final short[] DFA38_eof = DFA.unpackEncodedString(DFA38_eofS);
	static final char[] DFA38_min = DFA.unpackEncodedStringToUnsignedChars(DFA38_minS);
	static final char[] DFA38_max = DFA.unpackEncodedStringToUnsignedChars(DFA38_maxS);
	static final short[] DFA38_accept = DFA.unpackEncodedString(DFA38_acceptS);
	static final short[] DFA38_special = DFA.unpackEncodedString(DFA38_specialS);
	static final short[][] DFA38_transition;

	static {
		int numStates = DFA38_transitionS.length;
		DFA38_transition = new short[numStates][];
		for (int i=0; i<numStates; i++) {
			DFA38_transition[i] = DFA.unpackEncodedString(DFA38_transitionS[i]);
		}
	}

	protected class DFA38 extends DFA {

		public DFA38(BaseRecognizer recognizer) {
			this.recognizer = recognizer;
			this.decisionNumber = 38;
			this.eot = DFA38_eot;
			this.eof = DFA38_eof;
			this.min = DFA38_min;
			this.max = DFA38_max;
			this.accept = DFA38_accept;
			this.special = DFA38_special;
			this.transition = DFA38_transition;
		}
		@Override
		public String getDescription() {
			return "()* loopback of 736:5: ({...}? annotation )*";
		}
		@Override
		public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
			TokenStream input = (TokenStream)_input;
			int _s = s;
			switch ( s ) {
					case 0 :
						int LA38_63 = input.LA(1);
						
						int index38_63 = input.index();
						input.rewind();
						s = -1;
						if ( ((input.LA(1) == ANNOTATION_DIRECTIVE)) ) {s = 64;}
						else if ( (true) ) {s = 1;}
						
						input.seek(index38_63);
						if ( s>=0 ) return s;
						break;
			}
			NoViableAltException nvae =
				new NoViableAltException(getDescription(), 38, _s, input);
			error(nvae);
			throw nvae;
		}
	}

	public static final BitSet FOLLOW_class_spec_in_smali_file1070 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_super_spec_in_smali_file1081 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_implements_spec_in_smali_file1089 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_source_spec_in_smali_file1098 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_method_in_smali_file1106 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_field_in_smali_file1112 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_annotation_in_smali_file1118 = new BitSet(new long[]{0x0000011000010020L,0x0000000000000000L,0x8000100000000000L,0x0000000000000008L});
	public static final BitSet FOLLOW_EOF_in_smali_file1129 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CLASS_DIRECTIVE_in_class_spec1216 = new BitSet(new long[]{0x0000000000008010L});
	public static final BitSet FOLLOW_access_list_in_class_spec1218 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_class_spec1220 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SUPER_DIRECTIVE_in_super_spec1238 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_super_spec1240 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_IMPLEMENTS_DIRECTIVE_in_implements_spec1259 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_implements_spec1261 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SOURCE_DIRECTIVE_in_source_spec1280 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_source_spec1282 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ACCESS_SPEC_in_access_list1301 = new BitSet(new long[]{0x0000000000000012L});
	public static final BitSet FOLLOW_FIELD_DIRECTIVE_in_field1332 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_access_list_in_field1334 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_member_name_in_field1336 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_COLON_in_field1338 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_field1340 = new BitSet(new long[]{0x0000000804000022L});
	public static final BitSet FOLLOW_EQUAL_in_field1343 = new BitSet(new long[]{0xF4795CC200C0CD50L,0x000000000020FB16L,0x65A0EC0000000000L,0x0000000000000036L});
	public static final BitSet FOLLOW_literal_in_field1345 = new BitSet(new long[]{0x0000000004000022L});
	public static final BitSet FOLLOW_annotation_in_field1358 = new BitSet(new long[]{0x0000000004000022L});
	public static final BitSet FOLLOW_END_FIELD_DIRECTIVE_in_field1372 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_METHOD_DIRECTIVE_in_method1483 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_access_list_in_method1485 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_member_name_in_method1487 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0001000000000000L});
	public static final BitSet FOLLOW_method_prototype_in_method1489 = new BitSet(new long[]{0xFFFFFC04180830A0L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_statements_and_directives_in_method1491 = new BitSet(new long[]{0x0000000010000000L});
	public static final BitSet FOLLOW_END_METHOD_DIRECTIVE_in_method1497 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ordered_method_item_in_statements_and_directives1542 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_registers_directive_in_statements_and_directives1550 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_catch_directive_in_statements_and_directives1558 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_catchall_directive_in_statements_and_directives1566 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_parameter_directive_in_statements_and_directives1574 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_annotation_in_statements_and_directives1582 = new BitSet(new long[]{0xFFFFFC04080830A2L,0x00000000003FFFFFL,0x9A06038000000000L,0x0000000000000001L});
	public static final BitSet FOLLOW_label_in_ordered_method_item1667 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_instruction_in_ordered_method_item1673 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_debug_directive_in_ordered_method_item1679 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_REGISTERS_DIRECTIVE_in_registers_directive1699 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_registers_directive1703 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LOCALS_DIRECTIVE_in_registers_directive1723 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_registers_directive1727 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PARAM_LIST_OR_ID_START_in_param_list_or_id1759 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_PRIMITIVE_TYPE_in_param_list_or_id1761 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0110000000000000L});
	public static final BitSet FOLLOW_PARAM_LIST_OR_ID_END_in_param_list_or_id1764 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SIMPLE_NAME_in_simple_name1776 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ACCESS_SPEC_in_simple_name1782 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_VERIFICATION_ERROR_TYPE_in_simple_name1793 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_POSITIVE_INTEGER_LITERAL_in_simple_name1804 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NEGATIVE_INTEGER_LITERAL_in_simple_name1815 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_FLOAT_LITERAL_OR_ID_in_simple_name1826 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_DOUBLE_LITERAL_OR_ID_in_simple_name1837 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BOOL_LITERAL_in_simple_name1848 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NULL_LITERAL_in_simple_name1859 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_REGISTER_in_simple_name1870 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_param_list_or_id_in_simple_name1881 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PRIMITIVE_TYPE_in_simple_name1891 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_VOID_TYPE_in_simple_name1902 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ANNOTATION_VISIBILITY_in_simple_name1913 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10t_in_simple_name1924 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10x_in_simple_name1935 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10x_ODEX_in_simple_name1946 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT11x_in_simple_name1957 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT12x_OR_ID_in_simple_name1968 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_FIELD_in_simple_name1979 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_FIELD_ODEX_in_simple_name1990 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_STRING_in_simple_name2001 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_TYPE_in_simple_name2012 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21t_in_simple_name2023 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_FIELD_in_simple_name2034 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_FIELD_ODEX_in_simple_name2045 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_TYPE_in_simple_name2056 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22cs_FIELD_in_simple_name2067 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22s_OR_ID_in_simple_name2078 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22t_in_simple_name2089 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT23x_in_simple_name2100 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31i_OR_ID_in_simple_name2111 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31t_in_simple_name2122 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_METHOD_in_simple_name2133 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_METHOD_ODEX_in_simple_name2144 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_TYPE_in_simple_name2155 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35mi_METHOD_in_simple_name2166 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35ms_METHOD_in_simple_name2177 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT51l_in_simple_name2188 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_simple_name_in_member_name2203 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_MEMBER_NAME_in_member_name2209 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_OPEN_PAREN_in_method_prototype2224 = new BitSet(new long[]{0x0000000000048100L,0x0000000000000000L,0x0160000000000000L});
	public static final BitSet FOLLOW_param_list_in_method_prototype2226 = new BitSet(new long[]{0x0000000000040000L});
	public static final BitSet FOLLOW_CLOSE_PAREN_in_method_prototype2228 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L,0x0000000000000020L});
	public static final BitSet FOLLOW_type_descriptor_in_method_prototype2230 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PARAM_LIST_START_in_param_list2260 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0108000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_param_list2262 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0108000000000000L});
	public static final BitSet FOLLOW_PARAM_LIST_END_in_param_list2265 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PARAM_LIST_OR_ID_START_in_param_list2276 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0110000000000000L});
	public static final BitSet FOLLOW_PRIMITIVE_TYPE_in_param_list2278 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0110000000000000L});
	public static final BitSet FOLLOW_PARAM_LIST_OR_ID_END_in_param_list2281 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_param_list2292 = new BitSet(new long[]{0x0000000000008102L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_POSITIVE_INTEGER_LITERAL_in_integer_literal2369 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NEGATIVE_INTEGER_LITERAL_in_integer_literal2380 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_FLOAT_LITERAL_OR_ID_in_float_literal2395 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_FLOAT_LITERAL_in_float_literal2406 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_DOUBLE_LITERAL_OR_ID_in_double_literal2416 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_DOUBLE_LITERAL_in_double_literal2427 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LONG_LITERAL_in_literal2437 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_integer_literal_in_literal2443 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SHORT_LITERAL_in_literal2449 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BYTE_LITERAL_in_literal2455 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_float_literal_in_literal2461 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_double_literal_in_literal2467 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CHAR_LITERAL_in_literal2473 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_literal2479 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BOOL_LITERAL_in_literal2485 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_NULL_LITERAL_in_literal2491 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_array_literal_in_literal2497 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_subannotation_in_literal2503 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_type_field_method_literal_in_literal2509 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_enum_literal_in_literal2515 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_integer_literal_in_parsed_integer_literal2528 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LONG_LITERAL_in_integral_literal2540 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_integer_literal_in_integral_literal2546 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SHORT_LITERAL_in_integral_literal2552 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CHAR_LITERAL_in_integral_literal2558 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BYTE_LITERAL_in_integral_literal2564 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LONG_LITERAL_in_fixed_32bit_literal2574 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_integer_literal_in_fixed_32bit_literal2580 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SHORT_LITERAL_in_fixed_32bit_literal2586 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BYTE_LITERAL_in_fixed_32bit_literal2592 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_float_literal_in_fixed_32bit_literal2598 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CHAR_LITERAL_in_fixed_32bit_literal2604 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BOOL_LITERAL_in_fixed_32bit_literal2610 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_integer_literal_in_fixed_literal2620 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LONG_LITERAL_in_fixed_literal2626 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SHORT_LITERAL_in_fixed_literal2632 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BYTE_LITERAL_in_fixed_literal2638 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_float_literal_in_fixed_literal2644 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_double_literal_in_fixed_literal2650 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CHAR_LITERAL_in_fixed_literal2656 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_BOOL_LITERAL_in_fixed_literal2662 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_array_literal2672 = new BitSet(new long[]{0xF4795CC200C2CD50L,0x000000000020FB16L,0x65A0EC0000000000L,0x0000000000000036L});
	public static final BitSet FOLLOW_literal_in_array_literal2675 = new BitSet(new long[]{0x0000000000120000L});
	public static final BitSet FOLLOW_COMMA_in_array_literal2678 = new BitSet(new long[]{0xF4795CC200C0CD50L,0x000000000020FB16L,0x65A0EC0000000000L,0x0000000000000036L});
	public static final BitSet FOLLOW_literal_in_array_literal2680 = new BitSet(new long[]{0x0000000000120000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_array_literal2688 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_simple_name_in_annotation_element2712 = new BitSet(new long[]{0x0000000800000000L});
	public static final BitSet FOLLOW_EQUAL_in_annotation_element2714 = new BitSet(new long[]{0xF4795CC200C0CD50L,0x000000000020FB16L,0x65A0EC0000000000L,0x0000000000000036L});
	public static final BitSet FOLLOW_literal_in_annotation_element2716 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ANNOTATION_DIRECTIVE_in_annotation2741 = new BitSet(new long[]{0x0000000000000040L});
	public static final BitSet FOLLOW_ANNOTATION_VISIBILITY_in_annotation2743 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_annotation2745 = new BitSet(new long[]{0xF4795C8001800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_annotation_element_in_annotation2751 = new BitSet(new long[]{0xF4795C8001800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_END_ANNOTATION_DIRECTIVE_in_annotation2754 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SUBANNOTATION_DIRECTIVE_in_subannotation2787 = new BitSet(new long[]{0x0000000000008000L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_subannotation2789 = new BitSet(new long[]{0xF4795C8100800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_annotation_element_in_subannotation2791 = new BitSet(new long[]{0xF4795C8100800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_END_SUBANNOTATION_DIRECTIVE_in_subannotation2794 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ENUM_DIRECTIVE_in_enum_literal2820 = new BitSet(new long[]{0x0000000000008100L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_enum_literal2822 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_ARROW_in_enum_literal2824 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_simple_name_in_enum_literal2826 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_COLON_in_enum_literal2828 = new BitSet(new long[]{0x0000000000008100L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_enum_literal2830 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_type_field_method_literal2854 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_type_field_method_literal2863 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_ARROW_in_type_field_method_literal2865 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_member_name_in_type_field_method_literal2877 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_COLON_in_type_field_method_literal2879 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_type_field_method_literal2881 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_member_name_in_type_field_method_literal2904 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0001000000000000L});
	public static final BitSet FOLLOW_method_prototype_in_type_field_method_literal2906 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PRIMITIVE_TYPE_in_type_field_method_literal2939 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_VOID_TYPE_in_type_field_method_literal2945 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_method_reference2956 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_ARROW_in_method_reference2958 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_member_name_in_method_reference2962 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0001000000000000L});
	public static final BitSet FOLLOW_method_prototype_in_method_reference2964 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_reference_type_descriptor_in_field_reference2986 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_ARROW_in_field_reference2988 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_member_name_in_field_reference2992 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_COLON_in_field_reference2994 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_field_reference2996 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COLON_in_label3017 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_simple_name_in_label3019 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_COLON_in_label_ref3038 = new BitSet(new long[]{0xF4795C8000800450L,0x000000000020FB16L,0x45A0600000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_simple_name_in_label_ref3040 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_REGISTER_in_register_list3054 = new BitSet(new long[]{0x0000000000100002L});
	public static final BitSet FOLLOW_COMMA_in_register_list3057 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_register_list3059 = new BitSet(new long[]{0x0000000000100002L});
	public static final BitSet FOLLOW_REGISTER_in_register_range3094 = new BitSet(new long[]{0x0000000000200002L});
	public static final BitSet FOLLOW_DOTDOT_in_register_range3097 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_register_range3101 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CLASS_DESCRIPTOR_in_verification_error_reference3130 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_field_reference_in_verification_error_reference3134 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_method_reference_in_verification_error_reference3138 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CATCH_DIRECTIVE_in_catch_directive3148 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_catch_directive3150 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_catch_directive3152 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catch_directive3156 = new BitSet(new long[]{0x0000000000200000L});
	public static final BitSet FOLLOW_DOTDOT_in_catch_directive3158 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catch_directive3162 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_catch_directive3164 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catch_directive3168 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_CATCHALL_DIRECTIVE_in_catchall_directive3200 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_catchall_directive3202 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catchall_directive3206 = new BitSet(new long[]{0x0000000000200000L});
	public static final BitSet FOLLOW_DOTDOT_in_catchall_directive3208 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catchall_directive3212 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_catchall_directive3214 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_catchall_directive3218 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PARAMETER_DIRECTIVE_in_parameter_directive3257 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_parameter_directive3259 = new BitSet(new long[]{0x0000000040100022L});
	public static final BitSet FOLLOW_COMMA_in_parameter_directive3262 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_parameter_directive3264 = new BitSet(new long[]{0x0000000040000022L});
	public static final BitSet FOLLOW_annotation_in_parameter_directive3275 = new BitSet(new long[]{0x0000000040000022L});
	public static final BitSet FOLLOW_END_PARAMETER_DIRECTIVE_in_parameter_directive3288 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_line_directive_in_debug_directive3361 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_local_directive_in_debug_directive3367 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_end_local_directive_in_debug_directive3373 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_restart_local_directive_in_debug_directive3379 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_prologue_directive_in_debug_directive3385 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_epilogue_directive_in_debug_directive3391 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_source_directive_in_debug_directive3397 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LINE_DIRECTIVE_in_line_directive3407 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_line_directive3409 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_LOCAL_DIRECTIVE_in_local_directive3432 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_local_directive3434 = new BitSet(new long[]{0x0000000000100002L});
	public static final BitSet FOLLOW_COMMA_in_local_directive3437 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000400000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_NULL_LITERAL_in_local_directive3440 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_local_directive3446 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_COLON_in_local_directive3449 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L,0x0000000000000020L});
	public static final BitSet FOLLOW_VOID_TYPE_in_local_directive3452 = new BitSet(new long[]{0x0000000000100002L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_local_directive3456 = new BitSet(new long[]{0x0000000000100002L});
	public static final BitSet FOLLOW_COMMA_in_local_directive3490 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_local_directive3494 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_END_LOCAL_DIRECTIVE_in_end_local_directive3536 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_end_local_directive3538 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_RESTART_LOCAL_DIRECTIVE_in_restart_local_directive3561 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_restart_local_directive3563 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PROLOGUE_DIRECTIVE_in_prologue_directive3586 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_EPILOGUE_DIRECTIVE_in_epilogue_directive3607 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SOURCE_DIRECTIVE_in_source_directive3628 = new BitSet(new long[]{0x0000000000000002L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_source_directive3630 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT12x_in_instruction_format12x3655 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT12x_OR_ID_in_instruction_format12x3661 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22s_in_instruction_format22s3676 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22s_OR_ID_in_instruction_format22s3682 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31i_in_instruction_format31i3697 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31i_OR_ID_in_instruction_format31i3703 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format10t_in_instruction3720 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format10x_in_instruction3726 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format10x_odex_in_instruction3732 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format11n_in_instruction3738 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format11x_in_instruction3744 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format12x_in_instruction3750 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format20bc_in_instruction3756 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format20t_in_instruction3762 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21c_field_in_instruction3768 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21c_field_odex_in_instruction3774 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21c_string_in_instruction3780 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21c_type_in_instruction3786 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21ih_in_instruction3792 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21lh_in_instruction3798 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21s_in_instruction3804 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format21t_in_instruction3810 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22b_in_instruction3816 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22c_field_in_instruction3822 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22c_field_odex_in_instruction3828 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22c_type_in_instruction3834 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22cs_field_in_instruction3840 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22s_in_instruction3846 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22t_in_instruction3852 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format22x_in_instruction3858 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format23x_in_instruction3864 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format30t_in_instruction3870 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format31c_in_instruction3876 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format31i_in_instruction3882 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format31t_in_instruction3888 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format32x_in_instruction3894 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format35c_method_in_instruction3900 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format35c_type_in_instruction3906 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format35c_method_odex_in_instruction3912 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format35mi_method_in_instruction3918 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format35ms_method_in_instruction3924 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format3rc_method_in_instruction3930 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format3rc_method_odex_in_instruction3936 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format3rc_type_in_instruction3942 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format3rmi_method_in_instruction3948 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format3rms_method_in_instruction3954 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_format51l_in_instruction3960 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_array_data_directive_in_instruction3966 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_packed_switch_directive_in_instruction3972 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_insn_sparse_switch_directive_in_instruction3978 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10t_in_insn_format10t3998 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format10t4000 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10x_in_insn_format10x4030 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT10x_ODEX_in_insn_format10x_odex4058 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT11n_in_insn_format11n4079 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format11n4081 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format11n4083 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_insn_format11n4085 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT11x_in_insn_format11x4117 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format11x4119 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_instruction_format12x_in_insn_format12x4149 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format12x4151 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format12x4153 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format12x4155 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT20bc_in_insn_format20bc4187 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000010L});
	public static final BitSet FOLLOW_VERIFICATION_ERROR_TYPE_in_insn_format20bc4189 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format20bc4191 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_verification_error_reference_in_insn_format20bc4193 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT20t_in_insn_format20t4230 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format20t4232 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_FIELD_in_insn_format21c_field4262 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21c_field4264 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21c_field4266 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_field_reference_in_insn_format21c_field4268 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_FIELD_ODEX_in_insn_format21c_field_odex4300 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21c_field_odex4302 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21c_field_odex4304 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_field_reference_in_insn_format21c_field_odex4306 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_STRING_in_insn_format21c_string4344 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21c_string4346 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21c_string4348 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_insn_format21c_string4350 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21c_TYPE_in_insn_format21c_type4382 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21c_type4384 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21c_type4386 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_insn_format21c_type4388 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21ih_in_insn_format21ih4420 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21ih4422 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21ih4424 = new BitSet(new long[]{0x000000C000004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_32bit_literal_in_insn_format21ih4426 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21lh_in_insn_format21lh4458 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21lh4460 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21lh4462 = new BitSet(new long[]{0x000000C000004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_32bit_literal_in_insn_format21lh4464 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21s_in_insn_format21s4496 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21s4498 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21s4500 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_insn_format21s4502 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT21t_in_insn_format21t4534 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format21t4536 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format21t4538 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format21t4540 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22b_in_insn_format22b4572 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22b4574 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22b4576 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22b4578 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22b4580 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_insn_format22b4582 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_FIELD_in_insn_format22c_field4616 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_field4618 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_field4620 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_field4622 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_field4624 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_field_reference_in_insn_format22c_field4626 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_FIELD_ODEX_in_insn_format22c_field_odex4660 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_field_odex4662 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_field_odex4664 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_field_odex4666 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_field_odex4668 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_field_reference_in_insn_format22c_field_odex4670 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22c_TYPE_in_insn_format22c_type4710 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_type4712 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_type4714 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22c_type4716 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22c_type4718 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_insn_format22c_type4720 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22cs_FIELD_in_insn_format22cs_field4754 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22cs_field4756 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22cs_field4758 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22cs_field4760 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22cs_field4762 = new BitSet(new long[]{0x0000002000000000L});
	public static final BitSet FOLLOW_FIELD_OFFSET_in_insn_format22cs_field4764 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_instruction_format22s_in_insn_format22s4785 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22s4787 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22s4789 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22s4791 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22s4793 = new BitSet(new long[]{0x0000000000004800L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_integral_literal_in_insn_format22s4795 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22t_in_insn_format22t4829 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22t4831 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22t4833 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22t4835 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22t4837 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format22t4839 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT22x_in_insn_format22x4873 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22x4875 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format22x4877 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format22x4879 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT23x_in_insn_format23x4911 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format23x4913 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format23x4915 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format23x4917 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format23x4919 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format23x4921 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT30t_in_insn_format30t4955 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format30t4957 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31c_in_insn_format31c4987 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format31c4989 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format31c4991 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000002L});
	public static final BitSet FOLLOW_STRING_LITERAL_in_insn_format31c4993 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_instruction_format31i_in_insn_format31i5024 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format31i5026 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format31i5028 = new BitSet(new long[]{0x000000C000004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_32bit_literal_in_insn_format31i5030 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT31t_in_insn_format31t5062 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format31t5064 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format31t5066 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_format31t5068 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT32x_in_insn_format32x5100 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format32x5102 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format32x5104 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format32x5106 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_METHOD_in_insn_format35c_method5138 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format35c_method5140 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format35c_method5142 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format35c_method5144 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format35c_method5146 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_method_reference_in_insn_format35c_method5148 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_TYPE_in_insn_format35c_type5180 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format35c_type5182 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format35c_type5184 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format35c_type5186 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format35c_type5188 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_insn_format35c_type5190 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35c_METHOD_ODEX_in_insn_format35c_method_odex5222 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format35c_method_odex5224 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format35c_method_odex5226 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format35c_method_odex5228 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format35c_method_odex5230 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_method_reference_in_insn_format35c_method_odex5232 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35mi_METHOD_in_insn_format35mi_method5253 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format35mi_method5255 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format35mi_method5257 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format35mi_method5259 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format35mi_method5261 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_INLINE_INDEX_in_insn_format35mi_method5263 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT35ms_METHOD_in_insn_format35ms_method5284 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format35ms_method5286 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format35ms_method5288 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format35ms_method5290 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format35ms_method5292 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000040L});
	public static final BitSet FOLLOW_VTABLE_INDEX_in_insn_format35ms_method5294 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT3rc_METHOD_in_insn_format3rc_method5315 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format3rc_method5317 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_range_in_insn_format3rc_method5319 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format3rc_method5321 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format3rc_method5323 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_method_reference_in_insn_format3rc_method5325 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT3rc_METHOD_ODEX_in_insn_format3rc_method_odex5357 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format3rc_method_odex5359 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_list_in_insn_format3rc_method_odex5361 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format3rc_method_odex5363 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format3rc_method_odex5365 = new BitSet(new long[]{0xF4795C8000808550L,0x000000000020FB16L,0x45A0680000000000L,0x0000000000000030L});
	public static final BitSet FOLLOW_method_reference_in_insn_format3rc_method_odex5367 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT3rc_TYPE_in_insn_format3rc_type5388 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format3rc_type5390 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_range_in_insn_format3rc_type5392 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format3rc_type5394 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format3rc_type5396 = new BitSet(new long[]{0x0000000000008100L,0x0000000000000000L,0x0100000000000000L});
	public static final BitSet FOLLOW_nonvoid_type_descriptor_in_insn_format3rc_type5398 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT3rmi_METHOD_in_insn_format3rmi_method5430 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format3rmi_method5432 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_range_in_insn_format3rmi_method5434 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format3rmi_method5436 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format3rmi_method5438 = new BitSet(new long[]{0x0000020000000000L});
	public static final BitSet FOLLOW_INLINE_INDEX_in_insn_format3rmi_method5440 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT3rms_METHOD_in_insn_format3rms_method5461 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000800000000000L});
	public static final BitSet FOLLOW_OPEN_BRACE_in_insn_format3rms_method5463 = new BitSet(new long[]{0x0000000000020000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_register_range_in_insn_format3rms_method5465 = new BitSet(new long[]{0x0000000000020000L});
	public static final BitSet FOLLOW_CLOSE_BRACE_in_insn_format3rms_method5467 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format3rms_method5469 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0000000000000000L,0x0000000000000040L});
	public static final BitSet FOLLOW_VTABLE_INDEX_in_insn_format3rms_method5471 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_INSTRUCTION_FORMAT51l_in_insn_format51l5492 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0400000000000000L});
	public static final BitSet FOLLOW_REGISTER_in_insn_format51l5494 = new BitSet(new long[]{0x0000000000100000L});
	public static final BitSet FOLLOW_COMMA_in_insn_format51l5496 = new BitSet(new long[]{0x000000C000C04C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_literal_in_insn_format51l5498 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_ARRAY_DATA_DIRECTIVE_in_insn_array_data_directive5525 = new BitSet(new long[]{0x0000000000000000L,0x0000000000000000L,0x0080200000000000L});
	public static final BitSet FOLLOW_parsed_integer_literal_in_insn_array_data_directive5531 = new BitSet(new long[]{0x000000C002C04C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_literal_in_insn_array_data_directive5543 = new BitSet(new long[]{0x000000C002C04C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_END_ARRAY_DATA_DIRECTIVE_in_insn_array_data_directive5546 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_PACKED_SWITCH_DIRECTIVE_in_insn_packed_switch_directive5592 = new BitSet(new long[]{0x000000C000004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_32bit_literal_in_insn_packed_switch_directive5598 = new BitSet(new long[]{0x0000000020080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_packed_switch_directive5604 = new BitSet(new long[]{0x0000000020080000L});
	public static final BitSet FOLLOW_END_PACKED_SWITCH_DIRECTIVE_in_insn_packed_switch_directive5611 = new BitSet(new long[]{0x0000000000000002L});
	public static final BitSet FOLLOW_SPARSE_SWITCH_DIRECTIVE_in_insn_sparse_switch_directive5685 = new BitSet(new long[]{0x000000C080004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_fixed_32bit_literal_in_insn_sparse_switch_directive5692 = new BitSet(new long[]{0x0000000000000200L});
	public static final BitSet FOLLOW_ARROW_in_insn_sparse_switch_directive5694 = new BitSet(new long[]{0x0000000000080000L});
	public static final BitSet FOLLOW_label_ref_in_insn_sparse_switch_directive5696 = new BitSet(new long[]{0x000000C080004C00L,0x0000000000000000L,0x2080240000000000L});
	public static final BitSet FOLLOW_END_SPARSE_SWITCH_DIRECTIVE_in_insn_sparse_switch_directive5704 = new BitSet(new long[]{0x0000000000000002L});
}
