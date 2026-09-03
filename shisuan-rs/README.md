# shisuan-core - 食算核心计算引擎 (Rust)

食算 App 的 Rust 核心计算引擎。编译为 `libshisuan_core.so`，通过 JNI 嵌入 Android 应用，
负责所有成本计算逻辑，Kotlin 仅做参数传递与 UI 渲染。

## 架构

```text
┌─────────────────────────────────────────────┐
│ Android 应用层 (Kotlin + Jetpack Compose)    │
│   ProductListScreen → ProductDetailScreen   │
│   CostCalculator.kt (委托 ShisuanCore)       │
└──────────────────┬──────────────────────────┘
                   │ JNI (external fun)
┌──────────────────▼──────────────────────────┐
│ JNI 桥接层 (src/jni_bridge.rs)               │
│   Java_com_example_shisuan_core_ShisuanCore_*│
└──────────────────┬──────────────────────────┘
┌──────────────────▼──────────────────────────┐
│ 纯计算逻辑 (src/calc.rs) - 无 JNI 依赖       │
│   成本换算 · 差异对比 · 单价换算 · 四舍五入    │
└─────────────────────────────────────────────┘
```

## 暴露给 Kotlin 的接口

| Kotlin (ShisuanCore) | Rust 函数 | 说明 |
|---|---|---|
| `version()` | `Java_..._version` | 引擎版本 |
| `calculate(...)` | `Java_..._calculate` | 核心成本换算（克单价→吨价→箱价→包价） |
| `calcDifferential(...)` | `Java_..._calcDifferential` | 批次吨价差异对比 |
| `round2(...)` | `Java_..._round2` | 保留两位小数 |
| `unitPriceToTotal(...)` | `Java_..._unitPriceToTotal` | 用量×单价→原料成本 |
| `engineInfo()` | `Java_..._engineInfo` | 引擎诊断信息 |

## 成本计算公式

```
总成本 = 原料成本 + 加工费
克单价 = 总成本 / 样品重量(g)
吨价   = 克单价 × 1,000,000
箱数   = 1,000,000 / 每箱克数
箱价   = 克单价 × 每箱克数
包价   = 箱价 / 每箱包数
```

## 交叉编译

### 前置条件

- Rust stable：`rustup default stable`
- Android 目标：
  ```bash
  rustup target add aarch64-linux-android armv7-linux-androideabi \
      x86_64-linux-android i686-linux-android
  ```
- Android NDK 26+：设置 `ANDROID_NDK_HOME` 指向 NDK 目录

### 构建

```bash
# 一键构建全部 ABI 并部署到 Android jniLibs（Windows 用 Git Bash）
ANDROID_NDK_HOME=/path/to/ndk ./build-android.sh

# 或手动构建单个 ABI
cargo build --release --target x86_64-linux-android
```

产物自动复制到 `../app/src/main/jniLibs/{abi}/libshisuan_core.so`

### 支持 ABI

| ABI | Rust target | jniLibs 目录 |
|---|---|---|
| ARM64 | aarch64-linux-android | arm64-v8a |
| ARM32 | armv7-linux-androideabi | armeabi-v7a |
| x86_64 | x86_64-linux-android | x86_64 |
| x86 | i686-linux-android | x86 |

## 单元测试

```bash
cargo test
```

核心计算逻辑（calc.rs）不依赖 JNI，可在宿主机直接运行测试：
- 基础成本换算（10g 样品 → 0.6元/g → 60万元/吨）
- 零值/非法输入防护
- 批次差异百分比与金额
- 四舍五入精度
- 元/kg 与 元/g 单价换算

## 目录结构

```
shisuan-rs/
├── Cargo.toml              # cdylib 配置（LTO + strip，最小 .so）
├── build-android.sh        # 一键交叉编译脚本
├── .cargo/
│   └── config.toml         # 各 ABI 链接器配置
└── src/
    ├── lib.rs              # crate 入口 + 单元测试
    ├── calc.rs             # 纯计算逻辑（无 JNI 依赖）
    └── jni_bridge.rs       # JNI 导出函数（Kotlin external fun 对应）
```
