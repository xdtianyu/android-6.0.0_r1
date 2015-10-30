/*
 * Copyright 2010, The Android Open Source Project
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

#include "slang_rs_pragma_handler.h"

#include <sstream>
#include <string>

#include "clang/Basic/TokenKinds.h"

#include "clang/Lex/LiteralSupport.h"
#include "clang/Lex/Preprocessor.h"
#include "clang/Lex/Token.h"

#include "slang_assert.h"
#include "slang_rs_context.h"

namespace slang {

namespace {  // Anonymous namespace

class RSExportTypePragmaHandler : public RSPragmaHandler {
 private:
  void handleItem(const std::string &Item) {
    mContext->addPragma(this->getName(), Item);
    mContext->addExportType(Item);
  }

 public:
  RSExportTypePragmaHandler(llvm::StringRef Name, RSContext *Context)
      : RSPragmaHandler(Name, Context) { }

  void HandlePragma(clang::Preprocessor &PP,
                    clang::PragmaIntroducerKind Introducer,
                    clang::Token &FirstToken) {
    this->handleItemListPragma(PP, FirstToken);
  }
};

class RSJavaPackageNamePragmaHandler : public RSPragmaHandler {
 public:
  RSJavaPackageNamePragmaHandler(llvm::StringRef Name, RSContext *Context)
      : RSPragmaHandler(Name, Context) { }

  void HandlePragma(clang::Preprocessor &PP,
                    clang::PragmaIntroducerKind Introducer,
                    clang::Token &FirstToken) {
    // FIXME: Need to validate the extracted package name from pragma.
    // Currently "all chars" specified in pragma will be treated as package
    // name.
    //
    // 18.1 The Grammar of the Java Programming Language
    // (http://java.sun.com/docs/books/jls/third_edition/html/syntax.html#18.1)
    //
    // CompilationUnit:
    //     [[Annotations] package QualifiedIdentifier   ;  ] {ImportDeclaration}
    //     {TypeDeclaration}
    //
    // QualifiedIdentifier:
    //     Identifier { . Identifier }
    //
    // Identifier:
    //     IDENTIFIER
    //
    // 3.8 Identifiers
    // (http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.8)
    //
    //

    clang::Token &PragmaToken = FirstToken;
    std::string PackageName;

    // Skip first token, "java_package_name"
    PP.LexUnexpandedToken(PragmaToken);

    // Now, the current token must be clang::tok::lpara
    if (PragmaToken.isNot(clang::tok::l_paren))
      return;

    while (PragmaToken.isNot(clang::tok::eod)) {
      // Lex package name
      PP.LexUnexpandedToken(PragmaToken);

      bool Invalid;
      std::string Spelling = PP.getSpelling(PragmaToken, &Invalid);
      if (!Invalid)
        PackageName.append(Spelling);

      // Pre-mature end (syntax error will be triggered by preprocessor later)
      if (PragmaToken.is(clang::tok::eod) || PragmaToken.is(clang::tok::eof)) {
        break;
      } else {
        // Next token is ')' (end of pragma)
        const clang::Token &NextTok = PP.LookAhead(0);
        if (NextTok.is(clang::tok::r_paren)) {
          mContext->addPragma(this->getName(), PackageName);
          mContext->setReflectJavaPackageName(PackageName);
          // Lex until meets clang::tok::eod
          do {
            PP.LexUnexpandedToken(PragmaToken);
          } while (PragmaToken.isNot(clang::tok::eod));
          break;
        }
      }
    }
  }
};

class RSReflectLicensePragmaHandler : public RSPragmaHandler {
 private:
  void handleItem(const std::string &Item) {
    mContext->addPragma(this->getName(), Item);
    mContext->setLicenseNote(Item);
  }

 public:
  RSReflectLicensePragmaHandler(llvm::StringRef Name, RSContext *Context)
      : RSPragmaHandler(Name, Context) { }

  void HandlePragma(clang::Preprocessor &PP,
                    clang::PragmaIntroducerKind Introducer,
                    clang::Token &FirstToken) {
    this->handleOptionalStringLiteralParamPragma(PP, FirstToken);
  }
};

class RSVersionPragmaHandler : public RSPragmaHandler {
 private:
  void handleInt(clang::Preprocessor &PP,
                 clang::Token &Tok,
                 const int v) {
    if (v != 1) {
      PP.Diag(Tok,
              PP.getDiagnostics().getCustomDiagID(
                  clang::DiagnosticsEngine::Error,
                  "pragma for version in source file must be set to 1"));
      mContext->setVersion(1);
      return;
    }
    std::stringstream ss;
    ss << v;
    mContext->addPragma(this->getName(), ss.str());
    mContext->setVersion(v);
  }

 public:
  RSVersionPragmaHandler(llvm::StringRef Name, RSContext *Context)
      : RSPragmaHandler(Name, Context) { }

  void HandlePragma(clang::Preprocessor &PP,
                    clang::PragmaIntroducerKind Introducer,
                    clang::Token &FirstToken) {
    this->handleIntegerParamPragma(PP, FirstToken);
  }
};

// Handles the pragmas rs_fp_full, rs_fp_relaxed, and rs_fp_imprecise.
// There's one instance of this handler for each of the above values.
// Only getName() differs between the instances.
class RSPrecisionPragmaHandler : public RSPragmaHandler {
public:
  RSPrecisionPragmaHandler(llvm::StringRef Name, RSContext *Context)
      : RSPragmaHandler(Name, Context) {}

  void HandlePragma(clang::Preprocessor &PP,
                    clang::PragmaIntroducerKind Introducer,
                    clang::Token &Token) {
    std::string Precision = getName();
    // We are deprecating rs_fp_imprecise.
    if (Precision == "rs_fp_imprecise") {
      PP.Diag(Token, PP.getDiagnostics().getCustomDiagID(
                         clang::DiagnosticsEngine::Warning,
                         "rs_fp_imprecise is deprecated.  Assuming "
                         "rs_fp_relaxed instead."));
      Precision = "rs_fp_relaxed";
    }
    // Check if we have already encountered a precision pragma already.
    std::string PreviousPrecision = mContext->getPrecision();
    if (!PreviousPrecision.empty()) {
      // If the previous specified a different value, it's an error.
      if (PreviousPrecision != Precision) {
        PP.Diag(Token, PP.getDiagnostics().getCustomDiagID(
                           clang::DiagnosticsEngine::Error,
                           "Multiple float precisions specified.  Encountered "
                           "%0 previously."))
            << PreviousPrecision;
      }
      // Otherwise we ignore redundant entries.
      return;
    }

    mContext->addPragma(Precision, "");
    mContext->setPrecision(Precision);
  }
};

}  // namespace

void RSPragmaHandler::handleItemListPragma(clang::Preprocessor &PP,
                                           clang::Token &FirstToken) {
  clang::Token &PragmaToken = FirstToken;

  // Skip first token, like "export_var"
  PP.LexUnexpandedToken(PragmaToken);

  // Now, the current token must be clang::tok::lpara
  if (PragmaToken.isNot(clang::tok::l_paren))
    return;

  while (PragmaToken.isNot(clang::tok::eod)) {
    // Lex variable name
    PP.LexUnexpandedToken(PragmaToken);
    if (PragmaToken.is(clang::tok::identifier))
      this->handleItem(PP.getSpelling(PragmaToken));
    else
      break;

    slangAssert(PragmaToken.isNot(clang::tok::eod));

    PP.LexUnexpandedToken(PragmaToken);

    if (PragmaToken.isNot(clang::tok::comma))
      break;
  }
}

void RSPragmaHandler::handleNonParamPragma(clang::Preprocessor &PP,
                                           clang::Token &FirstToken) {
  clang::Token &PragmaToken = FirstToken;

  // Skip first token, like "export_var_all"
  PP.LexUnexpandedToken(PragmaToken);

  // Should be end immediately
  if (PragmaToken.isNot(clang::tok::eod))
    if (PragmaToken.isNot(clang::tok::r_paren)) {
      PP.Diag(PragmaToken,
              PP.getDiagnostics().getCustomDiagID(
                  clang::DiagnosticsEngine::Error,
                  "expected a ')'"));
      return;
    }
}

void RSPragmaHandler::handleOptionalStringLiteralParamPragma(
    clang::Preprocessor &PP, clang::Token &FirstToken) {
  clang::Token &PragmaToken = FirstToken;

  // Skip first token, like "set_reflect_license"
  PP.LexUnexpandedToken(PragmaToken);

  // Now, the current token must be clang::tok::lpara
  if (PragmaToken.isNot(clang::tok::l_paren))
    return;

  // If not ')', eat the following string literal as the license
  PP.LexUnexpandedToken(PragmaToken);
  if (PragmaToken.isNot(clang::tok::r_paren)) {
    // Eat the whole string literal
    clang::StringLiteralParser StringLiteral(PragmaToken, PP);
    if (StringLiteral.hadError) {
      // Diagnostics will be generated automatically
      return;
    } else {
      this->handleItem(std::string(StringLiteral.GetString()));
    }

    // The current token should be clang::tok::r_para
    PP.LexUnexpandedToken(PragmaToken);
    if (PragmaToken.isNot(clang::tok::r_paren)) {
      PP.Diag(PragmaToken,
              PP.getDiagnostics().getCustomDiagID(
                  clang::DiagnosticsEngine::Error,
                  "expected a ')'"));
      return;
    }
  } else {
    // If no argument, remove the license
    this->handleItem("");
  }
}

void RSPragmaHandler::handleIntegerParamPragma(
    clang::Preprocessor &PP, clang::Token &FirstToken) {
  clang::Token &PragmaToken = FirstToken;

  // Skip first token, like "version"
  PP.LexUnexpandedToken(PragmaToken);

  // Now, the current token must be clang::tok::lpara
  if (PragmaToken.isNot(clang::tok::l_paren)) {
    // If no argument, set the version to 0
    this->handleInt(PP, PragmaToken, 0);
    return;
  }
  PP.LexUnexpandedToken(PragmaToken);

  if (PragmaToken.is(clang::tok::numeric_constant)) {
    llvm::SmallString<128> SpellingBuffer;
    SpellingBuffer.resize(PragmaToken.getLength() + 1);
    llvm::StringRef TokSpelling = PP.getSpelling(PragmaToken, SpellingBuffer);
    clang::NumericLiteralParser NumericLiteral(TokSpelling,
        PragmaToken.getLocation(), PP);
    if (NumericLiteral.hadError) {
      // Diagnostics will be generated automatically
      return;
    } else {
      llvm::APInt Val(32, 0);
      NumericLiteral.GetIntegerValue(Val);
      this->handleInt(PP, PragmaToken, static_cast<int>(Val.getSExtValue()));
    }
    PP.LexUnexpandedToken(PragmaToken);
  } else {
    // If no argument, set the version to 0
    this->handleInt(PP, PragmaToken, 0);
  }

  if (PragmaToken.isNot(clang::tok::r_paren)) {
    PP.Diag(PragmaToken,
            PP.getDiagnostics().getCustomDiagID(
                clang::DiagnosticsEngine::Error,
                "expected a ')'"));
    return;
  }

  do {
    PP.LexUnexpandedToken(PragmaToken);
  } while (PragmaToken.isNot(clang::tok::eod));
}

void AddPragmaHandlers(clang::Preprocessor &PP, RSContext *RsContext) {
  // For #pragma rs export_type
  PP.AddPragmaHandler("rs",
                      new RSExportTypePragmaHandler("export_type", RsContext));

  // For #pragma rs java_package_name
  PP.AddPragmaHandler(
      "rs", new RSJavaPackageNamePragmaHandler("java_package_name", RsContext));

  // For #pragma rs set_reflect_license
  PP.AddPragmaHandler(
      "rs", new RSReflectLicensePragmaHandler("set_reflect_license", RsContext));

  // For #pragma version
  PP.AddPragmaHandler(new RSVersionPragmaHandler("version", RsContext));

  // For #pragma rs_fp*
  PP.AddPragmaHandler(new RSPrecisionPragmaHandler("rs_fp_full", RsContext));
  PP.AddPragmaHandler(new RSPrecisionPragmaHandler("rs_fp_relaxed", RsContext));
  PP.AddPragmaHandler(new RSPrecisionPragmaHandler("rs_fp_imprecise", RsContext));
}


}  // namespace slang
