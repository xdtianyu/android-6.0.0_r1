/*
// Copyright (c) 2014 Intel Corporation 
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
#include <HwcTrace.h>
#include <PlatfBufferManager.h>
#include <tangier/TngGrallocBuffer.h>
#include <tangier/TngGrallocBufferMapper.h>

namespace android {
namespace intel {

PlatfBufferManager::PlatfBufferManager()
    : BufferManager()
{

}

PlatfBufferManager::~PlatfBufferManager()
{

}

bool PlatfBufferManager::initialize()
{
    return BufferManager::initialize();
}

void PlatfBufferManager::deinitialize()
{
    BufferManager::deinitialize();
}

DataBuffer* PlatfBufferManager::createDataBuffer(gralloc_module_t *module,
                                                 buffer_handle_t handle)
{
    return new TngGrallocBuffer(handle);
}

BufferMapper* PlatfBufferManager::createBufferMapper(gralloc_module_t *module,
                                                        DataBuffer& buffer)
{
    if (!module)
        return 0;

    return new TngGrallocBufferMapper(*(IMG_gralloc_module_public_t*)module,
                                        buffer);
}

bool PlatfBufferManager::blit(buffer_handle_t srcHandle, buffer_handle_t destHandle,
                              const crop_t& destRect, bool filter, bool async)

{
    IMG_gralloc_module_public_t *imgGrallocModule = (IMG_gralloc_module_public_t *) mGrallocModule;
    if (imgGrallocModule->Blit(imgGrallocModule, srcHandle,
                                destHandle,
                                destRect.w, destRect.h, destRect.x,
                                destRect.y, filter, 0, async)) {
        ETRACE("Blit failed");
        return false;
    }
    return true;
}

} // namespace intel
} // namespace android
