//! 纯计算逻辑 - 无任何 JNI/Android 依赖，可在宿主上单测

/// 成本计算结果
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct CostResult {
    /// 克单价（元/g）
    pub unit_cost_per_gram: f64,
    /// 吨价（元/吨）
    pub unit_cost_per_ton: f64,
    /// 每吨箱数
    pub boxes_per_ton: f64,
    /// 每箱成本（元）
    pub cost_per_box: f64,
    /// 每包成本（元）
    pub cost_per_package: f64,
}

/// 与上一批次的成本差异
#[derive(Debug, Clone, Copy, PartialEq)]
pub struct CostDifferential {
    /// 差异百分比（正=上涨，负=下降）
    pub diff_percent: f64,
    /// 差异金额（元/吨）
    pub diff_amount: f64,
    /// 是否上涨
    pub is_increased: bool,
}

/// 成本计算引擎
pub struct CostCalculator;

impl CostCalculator {
    /// 核心换算公式：
    /// 克单价 = 总成本 / 样品重量(g)
    /// 吨价   = 克单价 × 1,000,000
    /// 箱数   = 1,000,000 / 每箱克数
    /// 箱价   = 克单价 × 每箱克数
    /// 包价   = 箱价 / 每箱包数
    pub fn calculate(
        sample_weight_gram: f64,
        material_cost: f64,
        processing_cost: f64,
        weight_per_box_gram: f64,
        packages_per_box: i32,
    ) -> CostResult {
        if sample_weight_gram <= 0.0 || weight_per_box_gram <= 0.0 || packages_per_box <= 0 {
            return CostResult {
                unit_cost_per_gram: 0.0,
                unit_cost_per_ton: 0.0,
                boxes_per_ton: 0.0,
                cost_per_box: 0.0,
                cost_per_package: 0.0,
            };
        }

        let total_cost = material_cost + processing_cost;
        let unit_cost_per_gram = total_cost / sample_weight_gram;
        let unit_cost_per_ton = unit_cost_per_gram * 1_000_000.0;
        let boxes_per_ton = 1_000_000.0 / weight_per_box_gram;
        let cost_per_box = unit_cost_per_gram * weight_per_box_gram;
        let cost_per_package = cost_per_box / packages_per_box as f64;

        CostResult {
            unit_cost_per_gram: Self::round2(unit_cost_per_gram),
            unit_cost_per_ton: Self::round2(unit_cost_per_ton),
            boxes_per_ton: Self::round2(boxes_per_ton),
            cost_per_box: Self::round2(cost_per_box),
            cost_per_package: Self::round2(cost_per_package),
        }
    }

    /// 计算与上一批次吨价的差异
    pub fn calc_differential(current_ton: f64, previous_ton: Option<f64>) -> Option<CostDifferential> {
        // current 非有限（NaN/±Inf）时无法得出有意义的差异，
        // 返回 None 让上层按「无对比数据」处理，避免 NaN/Inf 直写 UI
        if !current_ton.is_finite() {
            return None;
        }
        let prev = previous_ton?;
        if !prev.is_finite() || prev <= 0.0 {
            return None;
        }
        let diff_amount = current_ton - prev;
        let diff_percent = diff_amount / prev * 100.0;
        Some(CostDifferential {
            diff_percent: Self::round2(diff_percent),
            diff_amount: Self::round2(diff_amount),
            is_increased: diff_amount > 0.0,
        })
    }

    /// 保留两位小数
    pub fn round2(value: f64) -> f64 {
        (value * 100.0).round() / 100.0
    }

    /// 由用量(g)和单价换算原料成本
    ///
    /// - `is_per_gram=true`  单价单位为 元/g
    /// - `is_per_gram=false` 单价单位为 元/kg（默认）
    pub fn unit_price_to_total(weight_gram: f64, unit_price: f64, is_per_gram: bool) -> f64 {
        if weight_gram <= 0.0 || unit_price < 0.0 {
            return 0.0;
        }
        let price_per_gram = if is_per_gram {
            unit_price
        } else {
            unit_price / 1000.0
        };
        Self::round2(weight_gram * price_per_gram)
    }
}
