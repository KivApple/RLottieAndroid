cmake_minimum_required(VERSION 3.6.0)

set(CMAKE_BUILD_TYPE Release)

add_subdirectory("${CMAKE_CURRENT_LIST_DIR}/src/main/jni/rlottie")
include_directories("${CMAKE_CURRENT_LIST_DIR}/src/main/jni/rlottie/inc")

add_library(rlottie_jni SHARED src/main/jni/main.cpp)
target_link_libraries(rlottie_jni rlottie jnigraphics)
