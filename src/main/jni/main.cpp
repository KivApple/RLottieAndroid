#include <memory>
#include <jni.h>
#include <android/bitmap.h>
#include <rlottie.h>

struct LottieInfo {
    std::string name;
	std::unique_ptr<rlottie::Animation> animation;
	size_t frameCount;
	double framePerSecond;
	std::future<rlottie::Surface> renderFuture;
};

extern "C" JNIEXPORT jlong Java_com_eternal_1search_rlottie_RLottieMethods_createFromJson(JNIEnv *env, jclass cls, jstring name, jstring json, jintArray params) {
	auto info = new LottieInfo;
	auto nameString = env->GetStringUTFChars(name, nullptr);
	info->name = nameString;
	env->ReleaseStringUTFChars(name, nameString);
	auto jsonString = env->GetStringUTFChars(json, nullptr);
	info->animation = rlottie::Animation::loadFromData(jsonString, info->name);
	env->ReleaseStringUTFChars(json, jsonString);
	if (!info->animation) {
	    delete info;
	    return 0;
	}
	info->frameCount = info->animation->totalFrame();
	info->framePerSecond = info->animation->frameRate();
 
	if (env->GetArrayLength(params) >= 2) {
        auto paramsArr = env->GetIntArrayElements(params, nullptr);
        if (paramsArr) {
            paramsArr[0] = (int) info->frameCount;
            paramsArr[1] = (int) info->framePerSecond;
            env->ReleaseIntArrayElements(params, paramsArr, 0);
        }
    }
    
	return (jlong) (size_t) info;
}

extern "C" JNIEXPORT void Java_com_eternal_1search_rlottie_RLottieMethods_destroy(JNIEnv *env, jclass cls, jlong ptr) {
    if (!ptr) return;
    auto info = (LottieInfo*) (size_t) ptr;
    if (info->renderFuture.valid()) {
		info->renderFuture.wait();
	}
    delete info;
}

extern "C" JNIEXPORT jboolean Java_com_eternal_1search_rlottie_RLottieMethods_renderFrame(JNIEnv *env, jclass cls, jlong ptr, jint frame, jobject bitmap, jint w, jint h, jint stride) {
    if (!ptr) return JNI_FALSE;
    if (!bitmap) return JNI_FALSE;
    auto info = (LottieInfo*) (size_t) ptr;
    void *pixels;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
    	rlottie::Surface surface((uint32_t*) pixels, (size_t) w, (size_t) h, (size_t) stride);
        info->renderFuture = info->animation->render((size_t) frame, surface);
    }
    return JNI_FALSE;
}

extern "C" JNIEXPORT void Java_com_eternal_1search_rlottie_RLottieMethods_getFrame(JNIEnv *env, jclass cls, jlong ptr, jobject bitmap) {
	if (!ptr) return;
	if (!bitmap) return;
	auto info = (LottieInfo*) (size_t) ptr;
	auto surface = info->renderFuture.get();
	auto pixels = (uint8_t*) surface.buffer();
	for (auto i = 0; i < surface.width() * surface.height(); i++) {
		auto pixel = pixels + i * 4;
		auto tmp = pixel[0];
		pixel[0] = pixel[2];
		pixel[2] = tmp;
	}
	AndroidBitmap_unlockPixels(env, bitmap);
}
