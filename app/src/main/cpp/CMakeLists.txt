cmake_minimum_required(VERSION 3.22.1)

project("nova_native")

# llama.cpp location
set(LLAMA_DIR
    "${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/llama.cpp"
)

# Make sure llama.cpp was downloaded
if(NOT EXISTS "${LLAMA_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "llama.cpp not found at ${LLAMA_DIR}")
endif()

# llama.cpp build options
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_LLAMAFILE OFF CACHE BOOL "" FORCE)

# Build llama.cpp
add_subdirectory(
    "${LLAMA_DIR}"
    "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp"
)

# NOVA native library
add_library(
    nova_native
    SHARED
    nova_native.cpp
)

# Android log library
find_library(
    log-lib
    log
)

# llama.cpp headers
target_include_directories(
    nova_native
    PRIVATE
    "${LLAMA_DIR}/include"
)

# Link llama.cpp + Android log
target_link_libraries(
    nova_native
    llama
    ggml
    ${log-lib}
)