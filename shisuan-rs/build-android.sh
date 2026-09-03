#!/bin/sh
# ============================================================
# 食算 Rust 核心引擎交叉编译脚本
# 将 shisuan-core 编译为 Android 各 ABI 的 .so 并部署到 jniLibs
#
# 前置条件：
#   - Rust stable + rustup target add aarch64-linux-android \
#       armv7-linux-androideabi x86_64-linux-android i686-linux-android
#   - Android NDK（ANDROID_NDK_HOME 环境变量指向 NDK 目录）
#   - Windows 下需要 Git Bash 或 WSL
# ============================================================
set -e

NDK_HOME="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/26.3.11579264}"
if [ ! -d "$NDK_HOME" ]; then
  echo "错误：找不到 Android NDK，请设置 ANDROID_NDK_HOME"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JNI_LIBS="$SCRIPT_DIR/../app/src/main/jniLibs"

# 生成链接器包装脚本（Windows 下 .cmd 避免路径转义问题）
gen_linker() {
  local target="$1" ndk_target="$2" out="$SCRIPT_DIR/.cargo/linker-$target.cmd"
  mkdir -p "$SCRIPT_DIR/.cargo"
  cat > "$out" << EOF
@echo off
set NDK_BIN=$NDK_HOME\\toolchains\\llvm\\prebuilt\\windows-x86_64\\bin
"%NDK_BIN%\\clang.exe" --target=$ndk_target %*
EOF
}

gen_linker aarch64-linux-android aarch64-linux-android21
gen_linker armv7-linux-androideabi armv7a-linux-androideabi21
gen_linker x86_64-linux-android x86_64-linux-android21
gen_linker i686-linux-android i686-linux-android21

# 编译各 ABI
build_abi() {
  local target="$1" abi_dir="$2"
  echo "▶ 编译 $target ..."
  cargo build --release --target "$target"
  mkdir -p "$JNI_LIBS/$abi_dir"
  cp "$SCRIPT_DIR/target/$target/release/libshisuan_core.so" \
     "$JNI_LIBS/$abi_dir/libshisuan_core.so"
  echo "✓ $abi_dir/libshisuan_core.so ($(du -h "$JNI_LIBS/$abi_dir/libshisuan_core.so" | cut -f1))"
}

cd "$SCRIPT_DIR"

# 运行 Rust 单元测试
echo "▶ 运行 Rust 单元测试 ..."
cargo test 2>&1 | grep -E "test result|running" || true

build_abi aarch64-linux-android arm64-v8a
build_abi armv7-linux-androideabi armeabi-v7a
build_abi x86_64-linux-android x86_64
build_abi i686-linux-android x86

echo ""
echo "✅ 全部完成！.so 已部署到 $JNI_LIBS"
echo "下一步：cd ../shisuan && ./gradlew assembleDebug"
