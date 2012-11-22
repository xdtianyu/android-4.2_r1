/** 
 **
 ** Copyright 2010, The Android Open Source Project
 **
 ** Licensed under the Apache License, Version 2.0 (the "License"); 
 ** you may not use this file except in compliance with the License. 
 ** You may obtain a copy of the License at 
 **
 **     http://www.apache.org/licenses/LICENSE-2.0 
 **
 ** Unless required by applicable law or agreed to in writing, software 
 ** distributed under the License is distributed on an "AS IS" BASIS, 
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 ** See the License for the specific language governing permissions and 
 ** limitations under the License.
 */

#include "texture.h"

#include <assert.h>
#include <string.h>
#include <math.h>

#include "pixelflinger2.h"

#if USE_LLVM_EXECUTIONENGINE
#include <llvm/Module.h>
#include <llvm/ExecutionEngine/JIT.h>
#include <llvm/DerivedTypes.h>
#endif

#if !USE_LLVM_TEXTURE_SAMPLER

const struct GGLContext * textureGGLContext;

union Pixel { unsigned char channels[4]; unsigned int val; };

static inline void PixelRGBAToVector4 (const Pixel *pixel, Vector4 * color)  __attribute__((always_inline));
static inline void PixelRGBAToVector4 (const Pixel *pixel, Vector4 * color)
{
#if defined(__ARM_HAVE_NEON) && USE_NEON
    int32x4_t c;
    c = vsetq_lane_s32(pixel->channels[0], c, 0);
    c = vsetq_lane_s32(pixel->channels[1], c, 1);
    c = vsetq_lane_s32(pixel->channels[2], c, 2);
    c = vsetq_lane_s32(pixel->channels[3], c, 3);
    color->f4 = vcvtq_f32_s32(c);
    color->f4 = vmulq_n_f32(color->f4, 1 / 255.0f);
#else
	color->r = (float)pixel->channels[0] / 255;
	color->g = (float)pixel->channels[1] / 255;
	color->b = (float)pixel->channels[2] / 255;
	color->a = (float)pixel->channels[3] / 255;
#endif
}

static inline void RGBAToVector4(const unsigned int rgba, Vector4 * color)
{
	PixelRGBAToVector4((const Pixel *)&rgba, color);
}

static inline void Lerp(Vec4<int> * a, Vec4<int> * b, int x, Vec4<int> * d)
{
    for (unsigned i = 0; i < 4; i++)
    {
        int r = b->i[i] - a->i[i], s = a->i[i];
        d->i[i] = (r * x >> 16) + s;
    }
}

static inline void ToIntVec(Vec4<int> * a)
{
    a->u[3] = a->u[0] >> 24;
    a->u[2] = (a->u[0] >> 16) & 0xff;
    a->u[1] = (a->u[0] >> 8) & 0xff;
    a->u[0] &= 0xff;
}

template<GGLPixelFormat format>
static void PointSample(unsigned sample[4], const unsigned * data, const unsigned index)
{
    if (GGL_PIXEL_FORMAT_RGBA_8888 == format)
        *sample = *(data + index);
    else if (GGL_PIXEL_FORMAT_RGBX_8888 == format)
    {
        *sample = *(data + index);
        *sample |= 0xff000000;
    }
    else if (GGL_PIXEL_FORMAT_RGB_565 == format)
    {
        sample[0] = *((const unsigned short *)data + index);
        sample[1] = (sample[0] & 0x7e0) << 5;
        sample[2] = (sample[0] & 0xf800) << 8;
        sample[0] = (sample[0] & 0x1f) << 3;
        
        sample[0] |= sample[0] >> 5;
        sample[1] = (sample[1] | (sample[1] >> 6)) & 0xff00;
        sample[2] = (sample[2] | (sample[2] >> 5)) & 0xff0000;
        
        sample[0] |= sample[1];
        sample[0] |= sample[2];
        sample[0] |= 0xff000000;
    }
    else if (GGL_PIXEL_FORMAT_UNKNOWN == format)
        sample[0] = 0xff00ffff;
    else 
        assert(0);
}

static unsigned texcoordWrap(const unsigned wrap, float r, const unsigned size,
                                    unsigned * lerp)
{
    const unsigned shift = 16;
    unsigned odd = 0;
    int tc;    

    tc = r * (1 << shift);
    
    odd = tc & (1 << shift);
    if (0 == wrap || 2 == wrap) // REPEAT or MIRRORED
        tc &= (1 << shift) - 1; // only take mantissa
    tc *= size - 1;
    // TODO DXL linear filtering needs to be fixed for texcoord outside of [0,1]
    *lerp = tc & ((1 << shift) - 1);
    tc >>= shift;
     
    if (0 == wrap) // GL_REPEAT
    { }
    else if (1 == wrap) // GL_CLAMP_TO_EDGE
        tc = MIN2(size - 1, MAX2(0, tc));
    else if (2 == wrap)
        tc = odd ? size - 1 - tc : tc;
    else
        assert(0);
    return tc;
}

template<GGLPixelFormat format, ChannelType output, unsigned minMag, unsigned wrapS, unsigned wrapT>
static void tex2d(unsigned sample[4], const float tex_coord[4], const unsigned sampler)
{
   const unsigned * data = (const unsigned *)textureGGLContext->textureState.textureData[sampler];
   const unsigned width = textureGGLContext->textureState.textureDimensions[sampler * 2];
	const unsigned height = textureGGLContext->textureState.textureDimensions[sampler * 2 + 1];
    unsigned xLerp = 0, yLerp = 0;
    const unsigned x0 = texcoordWrap(wrapS, tex_coord[0], width, &xLerp);
    const unsigned y0 = texcoordWrap(wrapT, tex_coord[1], height, &yLerp);
    
    if (0 == minMag)
    {
        PointSample<format>(sample, data, y0 * width + x0);
        sample[1] = (sample[0] & 0xff00) >> 8;
        sample[2] = (sample[0] & 0xff0000) >> 16;
        sample[3] = (sample[0] & 0xff000000) >> 24;
        sample[0] &= 0xff;
    }
    else if (1 == minMag)
    {
        const unsigned x1 = MIN2(width - 1, x0 + 1), y1 = MIN2(height - 1, y0 + 1);
        Vec4<int> samples[4] = {0};
        PointSample<format>((unsigned *)(samples + 0), data, y0 * width + x0);
        ToIntVec(samples + 0);
        PointSample<format>((unsigned *)(samples + 1), data, y0 * width + x1);
        ToIntVec(samples + 1);
        PointSample<format>((unsigned *)(samples + 2), data, y1 * width + x1);
        ToIntVec(samples + 2);
        PointSample<format>((unsigned *)(samples + 3), data, y1 * width + x0);
        ToIntVec(samples + 3);
        
        Lerp(samples + 0, samples + 1, xLerp, samples + 0);
        Lerp(samples + 3, samples + 2, xLerp, samples + 3);
        Lerp(samples + 0, samples + 3, yLerp, (Vec4<int> *)sample);
    }
    else
        assert(0);
    
    if (Fixed0 == output) // i32 non vector
        sample[0] = (sample[3] << 24) | (sample[2] << 16) | (sample[1] << 8) | sample[0];
    else if (Fixed8 == output) // 4 x i32
        ; // do nothing
    else if (Fixed16 == output) // 4 x i32
    {
        sample[0] <<= 8; sample[1] <<= 8; sample[2] <<= 8; sample[3] <<= 8;
    }
    else if (Float == output) // 4 x float
    {
        float * fsample = (float *)sample;
        fsample[0] = sample[0] / 255.0f; fsample[1] = sample[1] / 255.0f;
        fsample[2] = sample[2] / 255.0f; fsample[3] = sample[3] / 255.0f;
    }
}

template<GGLPixelFormat format, ChannelType output, unsigned minMag, unsigned wrapS, unsigned wrapT>
void texcube(unsigned sample[4], const float tex_coord[4], const unsigned sampler)
{
    float mx = fabs(tex_coord[0]), my = fabs(tex_coord[1]), mz = fabs(tex_coord[2]);
    float s = 0, t = 0, ma = 0;
    unsigned face = 0;
    if (mx > my && mx > mz)
    {
        if (tex_coord[0] >= 0)
        {
            s = -tex_coord[2];
            t = -tex_coord[1];
            face = 0;
        }
        else
        {
            s = tex_coord[2];
            t = -tex_coord[1];
            face = 1;
        }
        ma = mx;
    }
    else if (my > mx && my > mz)
    {
        if (tex_coord[1] >= 0)
        {
            s = tex_coord[0];
            t = tex_coord[2];
            face = 2;
        }
        else
        {
            s = tex_coord[0];
            t = -tex_coord[2];
            face = 3;
        }
        ma = my;
    }
    else
    {
        if (tex_coord[2] >= 0)
        {
            s = tex_coord[0];
            t = -tex_coord[1];
            face = 4;
        }
        else
        {
            s = -tex_coord[0];
            t = -tex_coord[2];
            face = 5;
        }
        ma = mz;
    }
    
    s = (s / ma + 1) * 0.5f;
    t = (t / ma + 1) * 0.5f;
   
    const unsigned * data = (const unsigned *)textureGGLContext->textureState.textureData[sampler];
    const unsigned width = textureGGLContext->textureState.textureDimensions[sampler * 2];
	const unsigned height = textureGGLContext->textureState.textureDimensions[sampler * 2 + 1];
    unsigned xLerp = 0, yLerp = 0;
    const unsigned x0 = texcoordWrap(wrapS, s, width, &xLerp);
    const unsigned y0 = texcoordWrap(wrapT, t, height, &yLerp);
    
    if (0 == minMag)
    {
        PointSample<format>(sample, data, y0 * width + x0);
        sample[1] = (sample[0] & 0xff00) >> 8;
        sample[2] = (sample[0] & 0xff0000) >> 16;
        sample[3] = (sample[0] & 0xff000000) >> 24;
        sample[0] &= 0xff;
    }
    else if (1 == minMag)
    {
        const unsigned x1 = MIN2(width - 1, x0 + 1), y1 = MIN2(height - 1, y0 + 1);
        Vec4<int> samples[4] = {0};
        PointSample<format>((unsigned *)(samples + 0), data, face * width * height + y0 * width + x0);
        ToIntVec(samples + 0);
        PointSample<format>((unsigned *)(samples + 1), data, face * width * height + y0 * width + x1);
        ToIntVec(samples + 1);
        PointSample<format>((unsigned *)(samples + 2), data, face * width * height + y1 * width + x1);
        ToIntVec(samples + 2);
        PointSample<format>((unsigned *)(samples + 3), data, face * width * height + y1 * width + x0);
        ToIntVec(samples + 3);
        
        Lerp(samples + 0, samples + 1, xLerp, samples + 0);
        Lerp(samples + 3, samples + 2, xLerp, samples + 3);
        Lerp(samples + 0, samples + 3, yLerp, (Vec4<int> *)sample);
    }
    else
        assert(0);
    
    if (Fixed0 == output) // i32 non vector
        sample[0] = (sample[3] << 24) | (sample[2] << 16) | (sample[1] << 8) | sample[0];
    else if (Fixed8 == output) // 4 x i32
        ; // do nothing
    else if (Fixed16 == output) // 4 x i32
    {
        sample[0] <<= 8; sample[1] <<= 8; sample[2] <<= 8; sample[3] <<= 8;
    }
    else if (Float == output) // 4 x float
    {
        float * fsample = (float *)sample;
        fsample[0] = sample[0] / 255.0f; fsample[1] = sample[1] / 255.0f;
        fsample[2] = sample[2] / 255.0f; fsample[3] = sample[3] / 255.0f;
    }
    
}

#define TEXTURE_FUNCTION_ENTRY(target,format,output,filter,wrapS,wrapT) \
{ #target"_"#format"_"#output"_"#filter"_"#wrapS"_"#wrapT, \
target<GGL_PIXEL_FORMAT_##format, output, filter, wrapS, wrapT> },

#define TEXTURE_FUNCTION_ENTRY_WRAPT(target,format,output,minMag,wrapS) \
TEXTURE_FUNCTION_ENTRY(target,format,output,minMag,wrapS,0) \
TEXTURE_FUNCTION_ENTRY(target,format,output,minMag,wrapS,1) \
TEXTURE_FUNCTION_ENTRY(target,format,output,minMag,wrapS,2)

#define TEXTURE_FUNCTION_ENTRY_WRAPS(target,format,output,minMag) \
TEXTURE_FUNCTION_ENTRY_WRAPT(target,format,output,minMag,0) \
TEXTURE_FUNCTION_ENTRY_WRAPT(target,format,output,minMag,1) \
TEXTURE_FUNCTION_ENTRY_WRAPT(target,format,output,minMag,2)

#define TEXTURE_FUNCTION_ENTRY_FILTER(target,format,output) \
TEXTURE_FUNCTION_ENTRY_WRAPS(target,format,output,0) \
TEXTURE_FUNCTION_ENTRY_WRAPS(target,format,output,1)

#define TEXTURE_FUNCTION_ENTRY_OUTPUT(target,format) \
TEXTURE_FUNCTION_ENTRY_FILTER(target,format,Float) \
TEXTURE_FUNCTION_ENTRY_FILTER(target,format,Fixed16) \
TEXTURE_FUNCTION_ENTRY_FILTER(target,format,Fixed8) \
TEXTURE_FUNCTION_ENTRY_FILTER(target,format,Fixed0)

#define TEXTURE_FUNCTION_ENTRY_FORMAT(target) \
TEXTURE_FUNCTION_ENTRY_OUTPUT(target,RGBA_8888) \
TEXTURE_FUNCTION_ENTRY_OUTPUT(target,RGBX_8888) \
TEXTURE_FUNCTION_ENTRY_OUTPUT(target,RGB_565) \
TEXTURE_FUNCTION_ENTRY_OUTPUT(target,UNKNOWN)

#define TEXTURE_FUNCTION_ENTRIES \
TEXTURE_FUNCTION_ENTRY_FORMAT(tex2d) \
TEXTURE_FUNCTION_ENTRY_FORMAT(texcube)

static struct TextureFunctionMapping
{
    const char * name;
    void (* function)(unsigned sample[4], const float tex_coord[4], const unsigned int tex_id);
} textureFunctionMapping [] = { TEXTURE_FUNCTION_ENTRIES };


#undef TEXTURE_FUNCTION_ENTRY

#endif //#if !USE_LLVM_TEXTURE_SAMPLER

#if USE_LLVM_EXECUTIONENGINE && !USE_LLVM_TEXTURE_SAMPLER

void DeclareTextureFunctions(llvm::Module * mod)
{
	llvm::LLVMContext & llvm_ctx = mod->getContext();
    
    std::vector<const llvm::Type*> funcArgs;
    llvm::VectorType *vectorType = llvm::VectorType::get(llvm::Type::getFloatTy(llvm_ctx), 4);
    llvm::PointerType * vectorPtr = llvm::PointerType::get(vectorType, 0);
	
    funcArgs.push_back(vectorPtr);
	funcArgs.push_back(vectorPtr);
	funcArgs.push_back(llvm::Type::getInt32Ty(llvm_ctx));
	// void function(float[4], const float[4], unsigned)
	
    llvm::FunctionType *functionType = llvm::FunctionType::get(llvm::Type::getVoidTy(llvm_ctx),
                                                   funcArgs,
                                                   false);
	    
    for (unsigned i = 0; i < sizeof(textureFunctionMapping) / sizeof(*textureFunctionMapping); i++)
    {
        llvm::Function * func = llvm::cast<llvm::Function>(
        mod->getOrInsertFunction(textureFunctionMapping[i].name, functionType));
        func->setLinkage(llvm::GlobalValue::ExternalLinkage);
        func->setCallingConv(llvm::CallingConv::C);
    }
}

void AddTextureFunctionMappings(llvm::Module * mod, llvm::ExecutionEngine * ee)
{
    if (mod->getFunction("tex2d_soa"))
        assert(0);//ee->addGlobalMapping(func, (void *)tex2d_soa);
    
    for (unsigned i = 0; i < sizeof(textureFunctionMapping) / sizeof(*textureFunctionMapping); i++)
    {
        llvm::Function * function = mod->getFunction(textureFunctionMapping[i].name);
        if (function)
            ee->updateGlobalMapping(function, (void *)textureFunctionMapping[i].function);
    }
}
#endif // #if USE_LLVM_EXECUTIONENGINE && !USE_LLVM_TEXTURE_SAMPLER

static void SetSampler(GGLInterface * iface, const unsigned sampler, GGLTexture * texture)
{
    assert(GGL_MAXCOMBINEDTEXTUREIMAGEUNITS > sampler);
    GGL_GET_CONTEXT(ctx, iface);
    if (!texture)
        SetShaderVerifyFunctions(iface);
    else if (ctx->state.textureState.textures[sampler].format != texture->format)
        SetShaderVerifyFunctions(iface);
    else if (ctx->state.textureState.textures[sampler].wrapS != texture->wrapS)
        SetShaderVerifyFunctions(iface);
    else if (ctx->state.textureState.textures[sampler].wrapT != texture->wrapT)
        SetShaderVerifyFunctions(iface);
    else if (ctx->state.textureState.textures[sampler].minFilter != texture->minFilter)
        SetShaderVerifyFunctions(iface);
    else if (ctx->state.textureState.textures[sampler].magFilter != texture->magFilter)
        SetShaderVerifyFunctions(iface);
             
    if (texture)
    {
        ctx->state.textureState.textures[sampler] = *texture; // shallow copy, data pointed to must remain valid 
        //ctx->state.textureState.textureData[sampler] = texture->levels[0];
        ctx->state.textureState.textureData[sampler] = texture->levels;
        ctx->state.textureState.textureDimensions[sampler * 2] = texture->width;
        ctx->state.textureState.textureDimensions[sampler * 2 + 1] = texture->height;
    }
    else
    {
        memset(ctx->state.textureState.textures + sampler, 0, sizeof(ctx->state.textureState.textures[sampler]));
        ctx->state.textureState.textureData[sampler] = NULL;
        ctx->state.textureState.textureDimensions[sampler * 2] = 0;
        ctx->state.textureState.textureDimensions[sampler * 2 + 1] = 0;
    }
}

void InitializeTextureFunctions(GGLInterface * iface)
{
    iface->SetSampler = SetSampler;
}