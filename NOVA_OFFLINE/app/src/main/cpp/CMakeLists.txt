cmake_minimum_required(VERSION 3.22.1)

project("nova_native")

# Creates and names a library, sets it as either STATIC or SHARED, and provides the relative paths to its source code.
add_library(
    nova_native
    SHARED
    nova_native.cpp
)

# Searches for a specified prebuilt library and stores the path as a variable.
find_library(
    log-lib
    log
)

# Specifies libraries CMake should link to your target library.
target_link_libraries(
    nova_native
    ${log-lib}
)
