/*
 * Copyright 2015, The Android Open Source Project
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

#ifndef BCC_RS_UTILS_H
#define BCC_RS_UTILS_H

#include "rsDefines.h"

#include <llvm/IR/Type.h>
#include <llvm/IR/DerivedTypes.h>

namespace {

static inline llvm::StringRef getUnsuffixedStructName(const llvm::StructType *T) {
  // Get just the object type name with no suffix.
  size_t LastDot = T->getName().rfind('.');
  if (LastDot == strlen("struct")) {
    // If we get back to just the "struct" part, we know that we had a
    // raw typename (i.e. struct.rs_element with no ".[0-9]+" suffix on it.
    // In that case, we will want to create our slice such that it contains
    // the entire name.
    LastDot = T->getName().size();
  }
  return T->getStructName().slice(0, LastDot);
}

const char kAllocationTypeName[] = "struct.rs_allocation";
const char kElementTypeName[]    = "struct.rs_element";
const char kSamplerTypeName[]    = "struct.rs_sampler";
const char kScriptTypeName[]     = "struct.rs_script";
const char kTypeTypeName[]       = "struct.rs_type";

// Returns the RsDataType for a given input LLVM type.
// This is only used to distinguish the associated RS object types (i.e.
// rs_allocation, rs_element, rs_sampler, rs_script, and rs_type).
// All other types are reported back as RS_TYPE_NONE, since no special
// handling would be necessary.
static inline enum RsDataType getRsDataTypeForType(const llvm::Type *T) {
  if (T->isStructTy()) {
    const llvm::StringRef StructName = getUnsuffixedStructName(llvm::dyn_cast<const llvm::StructType>(T));
    if (StructName.equals(kAllocationTypeName)) {
      return RS_TYPE_ALLOCATION;
    } else if (StructName.equals(kElementTypeName)) {
      return RS_TYPE_ELEMENT;
    } else if (StructName.equals(kSamplerTypeName)) {
      return RS_TYPE_SAMPLER;
    } else if (StructName.equals(kScriptTypeName)) {
      return RS_TYPE_SCRIPT;
    } else if (StructName.equals(kTypeTypeName)) {
      return RS_TYPE_TYPE;
    }
  }
  return RS_TYPE_NONE;
}

// Returns true if the input type is one of our RenderScript object types
// (allocation, element, sampler, script, type) and false if it is not.
static inline bool isRsObjectType(const llvm::Type *T) {
  return getRsDataTypeForType(T) != RS_TYPE_NONE;
}

}  // end namespace

#endif // BCC_RS_UTILS_H
