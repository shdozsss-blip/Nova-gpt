#include <jni.h>
#include <string>
#include <android/log.h>
#include <atomic>
#include <thread>
#include <chrono>

#define TAG "NOVA_Native_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::atomic<bool> g_stop_requested(false);
static bool g_model_loaded = false;
static std::string g_model_path = "";

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_nova_offline_NativeAI_nativeInit(JNIEnv *env, jclass clazz) {
    LOGI("NOVA Native JNI initialized.");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_nova_offline_NativeAI_nativeLoadModel(JNIEnv *env, jclass clazz, jstring model_path, jint context_length, jint n_threads) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    LOGI("Loading GGUF model from: %s (ctx: %d, threads: %d)", path, context_length, n_threads);

    // In a complete llama.cpp build:
    // llama_model_params model_params = llama_model_default_params();
    // llama_model *model = llama_load_model_from_file(path, model_params);
    // llama_context_params ctx_params = llama_context_default_params();
    // ctx_params.n_ctx = context_length;
    // ctx_params.n_threads = n_threads;
    // llama_context *ctx = llama_new_context_with_model(model, ctx_params);

    g_model_path = path;
    g_model_loaded = true;
    env->ReleaseStringUTFChars(model_path, path);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeUnloadModel(JNIEnv *env, jclass clazz) {
    LOGI("Unloading GGUF model from memory.");
    // In llama.cpp:
    // llama_free(ctx);
    // llama_free_model(model);
    g_model_loaded = false;
    g_model_path.clear();
}

JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeGenerate(JNIEnv *env, jclass clazz,
                                             jstring prompt, jstring system_prompt,
                                             jfloat temperature, jint max_tokens,
                                             jobject callback) {
    g_stop_requested.store(false);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onNativeToken", "(Ljava/lang/String;)V");
    jmethodID onDoneMethod = env->GetMethodID(callbackClass, "onNativeDone", "()V");
    jmethodID onErrorMethod = env->GetMethodID(callbackClass, "onNativeError", "(Ljava/lang/String;)V");

    if (!onTokenMethod || !onDoneMethod || !onErrorMethod) {
        LOGE("Callback methods not found!");
        return;
    }

    const char *pStr = env->GetStringUTFChars(prompt, nullptr);
    const char *sStr = env->GetStringUTFChars(system_prompt, nullptr);
    LOGI("Generating prompt: %s (temp: %.2f, max: %d)", pStr, temperature, max_tokens);

    // When llama.cpp is linked, inference token loop:
    // while (!g_stop_requested.load() && n_cur < max_tokens) {
    //     llama_token id = llama_sample_token(ctx, ...);
    //     std::string piece = llama_token_to_piece(ctx, id);
    //     jstring jPiece = env->NewStringUTF(piece.c_str());
    //     env->CallVoidMethod(callback, onTokenMethod, jPiece);
    //     env->DeleteLocalRef(jPiece);
    // }

    std::string responseIntro = "NOVA Native Inference active for: ";
    responseIntro += pStr;
    jstring jResponse = env->NewStringUTF(responseIntro.c_str());
    env->CallVoidMethod(callback, onTokenMethod, jResponse);
    env->DeleteLocalRef(jResponse);

    env->CallVoidMethod(callback, onDoneMethod);

    env->ReleaseStringUTFChars(prompt, pStr);
    env->ReleaseStringUTFChars(system_prompt, sStr);
}

JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeStop(JNIEnv *env, jclass clazz) {
    LOGI("Stop generation requested from Native.");
    g_stop_requested.store(true);
}

} // extern "C"
