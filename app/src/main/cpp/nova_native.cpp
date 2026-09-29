#include <jni.h>
#include <string>
#include <vector>
#include <atomic>
#include <android/log.h>

#include "llama.h"
#include "ggml-backend.h"

#define TAG "NOVA_Native_JNI"

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static std::atomic<bool> g_stop_requested(false);

static llama_model * g_model = nullptr;
static llama_context * g_ctx = nullptr;

static std::string g_model_path;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_nova_offline_NativeAI_nativeInit(
        JNIEnv *env,
        jclass clazz) {

    LOGI("Initializing llama.cpp backend.");

    ggml_backend_load_all();
    llama_backend_init();

    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL
Java_com_nova_offline_NativeAI_nativeLoadModel(
        JNIEnv *env,
        jclass clazz,
        jstring model_path,
        jint context_length,
        jint n_threads) {

    if (model_path == nullptr) {
        LOGE("Model path is null.");
        return JNI_FALSE;
    }

    const char *path =
            env->GetStringUTFChars(model_path, nullptr);

    if (path == nullptr) {
        return JNI_FALSE;
    }

    LOGI(
        "Loading model: %s | ctx=%d | threads=%d",
        path,
        context_length,
        n_threads
    );

    // Remove previous model/context.
    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }

    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    llama_model_params model_params =
            llama_model_default_params();

    // CPU only for initial Android test.
    model_params.n_gpu_layers = 0;

    g_model =
            llama_model_load_from_file(
                    path,
                    model_params
            );

    if (g_model == nullptr) {

        LOGE("Failed to load GGUF model.");

        env->ReleaseStringUTFChars(
                model_path,
                path
        );

        return JNI_FALSE;
    }

    llama_context_params ctx_params =
            llama_context_default_params();

    ctx_params.n_ctx =
            static_cast<uint32_t>(context_length);

    ctx_params.n_batch = 512;

    ctx_params.n_threads =
            n_threads;

    ctx_params.n_threads_batch =
            n_threads;

    g_ctx =
            llama_init_from_model(
                    g_model,
                    ctx_params
            );

    if (g_ctx == nullptr) {

        LOGE("Failed to create llama context.");

        llama_model_free(g_model);
        g_model = nullptr;

        env->ReleaseStringUTFChars(
                model_path,
                path
        );

        return JNI_FALSE;
    }

    g_model_path = path;

    env->ReleaseStringUTFChars(
            model_path,
            path
    );

    LOGI("GGUF model loaded successfully.");

    return JNI_TRUE;
}


JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeUnloadModel(
        JNIEnv *env,
        jclass clazz) {

    LOGI("Unloading model.");

    if (g_ctx != nullptr) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }

    if (g_model != nullptr) {
        llama_model_free(g_model);
        g_model = nullptr;
    }

    g_model_path.clear();
}


JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeGenerate(
        JNIEnv *env,
        jclass clazz,
        jstring prompt,
        jstring system_prompt,
        jfloat temperature,
        jint max_tokens,
        jobject callback) {

    g_stop_requested.store(false);

    if (g_model == nullptr || g_ctx == nullptr) {

        jclass callbackClass =
                env->GetObjectClass(callback);

        jmethodID onError =
                env->GetMethodID(
                        callbackClass,
                        "onNativeError",
                        "(Ljava/lang/String;)V"
                );

        if (onError != nullptr) {

            jstring msg =
                    env->NewStringUTF(
                            "Model is not loaded."
                    );

            env->CallVoidMethod(
                    callback,
                    onError,
                    msg
            );

            env->DeleteLocalRef(msg);
        }

        return;
    }

    jclass callbackClass =
            env->GetObjectClass(callback);

    jmethodID onToken =
            env->GetMethodID(
                    callbackClass,
                    "onNativeToken",
                    "(Ljava/lang/String;)V"
            );

    jmethodID onDone =
            env->GetMethodID(
                    callbackClass,
                    "onNativeDone",
                    "()V"
            );

    jmethodID onError =
            env->GetMethodID(
                    callbackClass,
                    "onNativeError",
                    "(Ljava/lang/String;)V"
            );

    if (!onToken || !onDone || !onError) {

        LOGE("Callback methods not found.");
        return;
    }

    const char *p =
            env->GetStringUTFChars(
                    prompt,
                    nullptr
            );

    const char *s =
            env->GetStringUTFChars(
                    system_prompt,
                    nullptr
            );

    std::string full_prompt;

    if (s != nullptr && s[0] != '\0') {

        full_prompt +=
                "<|system|>\n";

        full_prompt += s;

        full_prompt +=
                "\n<|end|>\n";
    }

    if (p != nullptr) {

        full_prompt +=
                "<|user|>\n";

        full_prompt += p;

        full_prompt +=
                "\n<|end|>\n";

        full_prompt +=
                "<|assistant|>\n";
    }

    if (p != nullptr) {
        env->ReleaseStringUTFChars(
                prompt,
                p
        );
    }

    if (s != nullptr) {
        env->ReleaseStringUTFChars(
                system_prompt,
                s
        );
    }

    const llama_vocab *vocab =
            llama_model_get_vocab(g_model);

    // Find token count.
    int n_prompt =
            -llama_tokenize(
                    vocab,
                    full_prompt.c_str(),
                    full_prompt.size(),
                    nullptr,
                    0,
                    true,
                    true
            );

    if (n_prompt <= 0) {

        jstring msg =
                env->NewStringUTF(
                        "Tokenization failed."
                );

        env->CallVoidMethod(
                callback,
                onError,
                msg
        );

        env->DeleteLocalRef(msg);

        return;
    }

    std::vector<llama_token> tokens(
            n_prompt
    );

    int actual_tokens =
            llama_tokenize(
                    vocab,
                    full_prompt.c_str(),
                    full_prompt.size(),
                    tokens.data(),
                    tokens.size(),
                    true,
                    true
            );

    if (actual_tokens < 0) {

        jstring msg =
                env->NewStringUTF(
                        "Prompt tokenization failed."
                );

        env->CallVoidMethod(
                callback,
                onError,
                msg
        );

        env->DeleteLocalRef(msg);

        return;
    }

    tokens.resize(actual_tokens);

    // Create batch.
    llama_batch batch =
            llama_batch_init(
                    512,
                    0,
                    1
            );

    // Add prompt tokens.
    for (size_t i = 0; i < tokens.size(); ++i) {

        batch.token[batch.n_tokens] =
                tokens[i];

        batch.pos[batch.n_tokens] =
                static_cast<llama_pos>(i);

        batch.n_seq_id[batch.n_tokens] = 1;

        batch.seq_id[batch.n_tokens][0] = 0;

        batch.logits[batch.n_tokens] =
                (i == tokens.size() - 1);

        batch.n_tokens++;
    }

    // Evaluate prompt.
    if (llama_decode(g_ctx, batch) != 0) {

        llama_batch_free(batch);

        jstring msg =
                env->NewStringUTF(
                        "Prompt evaluation failed."
                );

        env->CallVoidMethod(
                callback,
                onError,
                msg
        );

        env->DeleteLocalRef(msg);

        return;
    }

    // Sampler.
    llama_sampler_chain_params sampler_params =
            llama_sampler_chain_default_params();

    llama_sampler *sampler =
            llama_sampler_chain_init(
                    sampler_params
            );

    llama_sampler_chain_add(
            sampler,
            llama_sampler_init_temp(
                    temperature
            )
    );

    llama_sampler_chain_add(
            sampler,
            llama_sampler_init_dist(
                    LLAMA_DEFAULT_SEED
            )
    );

    llama_pos current_pos =
            static_cast<llama_pos>(
                    tokens.size()
            );

    for (int i = 0;
         i < max_tokens;
         ++i) {

        if (g_stop_requested.load()) {
            break;
        }

        llama_token token =
                llama_sampler_sample(
                        sampler,
                        g_ctx,
                        -1
                );

        if (llama_vocab_is_eog(
                vocab,
                token
        )) {
            break;
        }

        std::string piece;

        int piece_size =
                llama_token_to_piece(
                        vocab,
                        token,
                        nullptr,
                        0,
                        0,
                        true
                );

        if (piece_size > 0) {

            std::vector<char> buffer(
                    piece_size + 1
            );

            llama_token_to_piece(
                    vocab,
                    token,
                    buffer.data(),
                    piece_size,
                    0,
                    true
            );

            buffer[piece_size] = '\0';

            piece.assign(
                    buffer.data(),
                    piece_size
            );
        }

        if (!piece.empty()) {

            jstring jPiece =
                    env->NewStringUTF(
                            piece.c_str()
                    );

            env->CallVoidMethod(
                    callback,
                    onToken,
                    jPiece
            );

            env->DeleteLocalRef(jPiece);
        }

        batch.n_tokens = 0;

        batch.token[0] = token;
        batch.pos[0] = current_pos;

        batch.n_seq_id[0] = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0] = true;

        batch.n_tokens = 1;

        if (llama_decode(
                g_ctx,
                batch
        ) != 0) {

            break;
        }

        current_pos++;
    }

    llama_sampler_free(sampler);
    llama_batch_free(batch);

    env->CallVoidMethod(
            callback,
            onDone
    );
}


JNIEXPORT void JNICALL
Java_com_nova_offline_NativeAI_nativeStop(
        JNIEnv *env,
        jclass clazz) {

    LOGI("Stop requested.");

    g_stop_requested.store(true);
}

}