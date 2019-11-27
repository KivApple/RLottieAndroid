# RLottie

RLottie library bindings for Android. Includes `RLottieView` and `RLottieDrawable`. Uses native libraries for better performance.

## Usage

Execute this command under your project root:

    git submodule add https://github.com/KivApple/RLottieAndroid.git rlottie

Add these lines to your Android project files:

`/settings.gradle`

    include ':app', ..., ':rlottie'

`/build.gradle`

    buildscript {
        ext.kotlin_version = '1.3.50'
        ext.appCompatVersion = '1.0.2'
        ...
    }

`/app/build.gradle`

    dependencies {
        ...
        implementation project(':rlottie')
        ...
    }
