//! 食算核心计算引擎
//!
//! 纯 Rust 实现，编译为 `libshisuan_core.so` 供 Android 应用通过 JNI 调用。
//!
//! ## 架构
//! ```text
//! Kotlin 应用层 (Compose UI)
//!      │  JNI (external fun)
//!      ▼
//! Java_*_ShisuanCore_* 导出函数 (jni crate)
//!      │
//!      ▼
//! 纯计算逻辑 (calc.rs) — 无外部依赖，可单元测试
//! ```

mod calc;
mod jni_bridge;

// 重新导出供外部（测试/文档）使用
pub use calc::{CostCalculator, CostDifferential, CostResult};
pub use jni_bridge::*;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_basic_calculation() {
        // 样品10g，原料成本5元，加工费1元，每箱5000g，每箱20包
        let result = CostCalculator::calculate(10.0, 5.0, 1.0, 5000.0, 20);
        // 克单价 = 6/10 = 0.6元/g
        // 吨价 = 0.6 * 1_000_000 = 600_000元
        // 箱数 = 1_000_000 / 5000 = 200箱
        // 箱价 = 0.6 * 5000 = 3000元
        // 包价 = 3000 / 20 = 150元
        assert!((result.unit_cost_per_gram - 0.6).abs() < 0.01);
        assert!((result.unit_cost_per_ton - 600_000.0).abs() < 0.01);
        assert!((result.boxes_per_ton - 200.0).abs() < 0.01);
        assert!((result.cost_per_box - 3000.0).abs() < 0.01);
        assert!((result.cost_per_package - 150.0).abs() < 0.01);
    }

    #[test]
    fn test_zero_inputs() {
        let result = CostCalculator::calculate(0.0, 5.0, 1.0, 5000.0, 20);
        assert_eq!(result.unit_cost_per_ton, 0.0);
        let result = CostCalculator::calculate(10.0, 5.0, 1.0, 0.0, 20);
        assert_eq!(result.cost_per_box, 0.0);
    }

    #[test]
    fn test_differential() {
        let diff = CostCalculator::calc_differential(700_000.0, Some(600_000.0)).unwrap();
        assert!((diff.diff_percent - 16.67).abs() < 0.01);
        assert!((diff.diff_amount - 100_000.0).abs() < 0.01);
        assert!(diff.is_increased);

        let diff = CostCalculator::calc_differential(500_000.0, Some(600_000.0)).unwrap();
        assert!((diff.diff_percent - (-16.67)).abs() < 0.01);
        assert!(!diff.is_increased);

        assert!(CostCalculator::calc_differential(100.0, None).is_none());
        assert!(CostCalculator::calc_differential(100.0, Some(0.0)).is_none());
    }

    #[test]
    fn test_round2() {
        assert_eq!(CostCalculator::round2(1.23456), 1.23);
        assert_eq!(CostCalculator::round2(1.235), 1.24);
        assert_eq!(CostCalculator::round2(0.0), 0.0);
    }

    #[test]
    fn test_unit_price_to_total() {
        // 12元/kg，用5g → 成本 0.06元
        let cost = CostCalculator::unit_price_to_total(5.0, 12.0, false);
        assert!((cost - 0.06).abs() < 0.0001);
        // 0.012元/g，用5g → 成本 0.06元
        let cost = CostCalculator::unit_price_to_total(5.0, 0.012, true);
        assert!((cost - 0.06).abs() < 0.0001);
    }
}
