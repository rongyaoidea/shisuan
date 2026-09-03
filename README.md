# 食算 — 工厂食品成本换算工具

> 专为食品工厂一线人员设计：把实验室小批量成本，逐级换算成吨价、箱价、包价。
> 核心计算由 **Rust 引擎**（libshisuan_core.so）通过 JNI 在设备本地执行。

## 核心功能

- **产品工作流**：新建产品 → 产品详情 → 为产品新建批次（含配料明细）
- **成本换算链路**：实验室小样成本 → 克单价 → 吨价（×1,000,000）→ 箱价 → 包价
- **配料明细**：每个批次记录原料用量×单价，自动汇总原料成本
- **批次管理**：每次实验/试产记录一条，实时计算成本
- **成本差异对比**：同产品批次间自动标注涨跌幅（↑/↓ 百分比）
- **物理动效**：按压弹簧回弹、卡片错峰弹性入场

## 技术栈

- Kotlin + Jetpack Compose（UI 层）
- **Rust 计算引擎**（`shisuan-rs/` → libshisuan_core.so，经 JNI 调用）
- Room（本地数据库，完全离线）
- Navigation Compose + Hilt（依赖注入）
- MVVM + Repository 架构

## 架构

```text
┌────────────────────────────────────────────────┐
│ Compose UI（产品列表 → 详情 → 新建批次）        │
│   CostCalculator.kt ──委托──▶ ShisuanCore (JNI) │
└───────────────────────┬────────────────────────┘
                        │ JNI (external fun)
┌───────────────────────▼────────────────────────┐
│ Rust 引擎 libshisuan_core.so（shisuan-rs/）     │
│   成本换算 · 差异对比 · 单价换算 · 四舍五入      │
└────────────────────────────────────────────────┘
```

Rust 引擎源码、接口说明与交叉编译方法见 [shisuan-rs/README.md](shisuan-rs/README.md)。

## 数据模型

| 实体 | 说明 |
|------|------|
| Product | 产品（草莓酱/蓝莓酱…） |
| BatchRecord | 批次记录（样品重量、加工费，关联产品） |
| BatchIngredient | 批次配料明细（用量×单价→小计） |
| Ingredient | 原料档案（产地/供应商/保质期等特性） |
| UnitConfig | 换算配置（每箱克数、每箱包数） |
| BatchResult | 批次成果（口感/颜色/pH/评分） |
| BatchProblem | 问题记录（分类/严重度/解决方案） |
| BatchSnapshot | 版本快照（防丢，可回溯历史版本） |
| OperationLog | 操作日志（审计追踪） |

## 构建

```bash
# 1. （可选）重新编译 Rust 引擎到 jniLibs
cd shisuan-rs && ./build-android.sh && cd ..

# 2. 构建 APK
./gradlew assembleDebug
# APK 输出: app/build/outputs/apk/debug/app-debug.apk
```

## 截图

![食算主界面](shisuan_screenshot.png)

## 许可

MIT License
