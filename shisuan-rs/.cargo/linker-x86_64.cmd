@echo off
set NDK_BIN=C:\Users\Administrator\Android\Sdk\ndk\26.3.11579264\toolchains\llvm\prebuilt\windows-x86_64\bin
"%NDK_BIN%\clang.exe" --target=x86_64-linux-android21 %*
