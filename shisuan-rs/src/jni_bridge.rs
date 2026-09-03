//! JNI 桥接层
//!
//! 将纯计算逻辑暴露给 Kotlin：
//! `com.example.shisuan.core.ShisuanCore` 对象中的 `external fun` 一一对应。

use jni::JNIEnv;
use jni::objects::{JClass, JDoubleArray};
use jni::sys::{jboolean, jdouble, jdoubleArray, jint, jstring};

use crate::calc::CostCalculator;

const ERROR_OUT_OF_MEMORY: jint = -2; // 内部保留
const OK: jint = 0;

/// 引擎版本号
const ENGINE_VERSION: &str = "1.0.0";

// ──────────────────────────────────────────────
// Kotlin: external fun version(): String
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_version(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    match env.new_string(ENGINE_VERSION) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

// ──────────────────────────────────────────────
// Kotlin:
//   external fun calculate(
//       sampleWeightGram: Double, materialCost: Double, processingCost: Double,
//       weightPerBoxGram: Double, packagesPerBox: Int, out: DoubleArray
//   ): Int
//
// out 数组 5 个元素: [克单价, 吨价, 每吨箱数, 箱价, 包价]
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_calculate(
    mut env: JNIEnv,
    _class: JClass,
    sample_weight_gram: jdouble,
    material_cost: jdouble,
    processing_cost: jdouble,
    weight_per_box_gram: jdouble,
    packages_per_box: jint,
    out: jdoubleArray,
) -> jint {
    let result = CostCalculator::calculate(
        sample_weight_gram,
        material_cost,
        processing_cost,
        weight_per_box_gram,
        packages_per_box,
    );

    let buf = [
        result.unit_cost_per_gram,
        result.unit_cost_per_ton,
        result.boxes_per_ton,
        result.cost_per_box,
        result.cost_per_package,
    ];

    let array = unsafe { JDoubleArray::from_raw(out) };
    if env.set_double_array_region(array, 0, &buf).is_err() {
        return ERROR_OUT_OF_MEMORY;
    }
    OK
}

// ──────────────────────────────────────────────
// Kotlin:
//   external fun calcDifferential(
//       currentTonCost: Double, previousTonCost: Double, out: DoubleArray
//   ): Int
//
// previousTonCost <= 0 表示没有上一批次
// out 数组 3 个元素: [差异百分比, 差异金额, 是否上涨(1.0/0.0)]
// 返回 0 = 成功计算，1 = 无上一批次可对比
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_calcDifferential(
    mut env: JNIEnv,
    _class: JClass,
    current_ton_cost: jdouble,
    previous_ton_cost: jdouble,
    out: jdoubleArray,
) -> jint {
    let prev = if previous_ton_cost > 0.0 {
        Some(previous_ton_cost)
    } else {
        None
    };

    let array = unsafe { JDoubleArray::from_raw(out) };
    match CostCalculator::calc_differential(current_ton_cost, prev) {
        Some(diff) => {
            let buf = [
                diff.diff_percent,
                diff.diff_amount,
                if diff.is_increased { 1.0 } else { 0.0 },
            ];
            if env.set_double_array_region(array, 0, &buf).is_err() {
                return ERROR_OUT_OF_MEMORY;
            }
            OK
        }
        None => {
            // 无对比数据，写 0 占位
            let buf = [0.0f64, 0.0, 0.0];
            let _ = env.set_double_array_region(array, 0, &buf);
            1
        }
    }
}

// ──────────────────────────────────────────────
// Kotlin: external fun round2(value: Double): Double
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_round2(
    _env: JNIEnv,
    _class: JClass,
    value: jdouble,
) -> jdouble {
    CostCalculator::round2(value)
}

// ──────────────────────────────────────────────
// Kotlin:
//   external fun unitPriceToTotal(
//       weightGram: Double, unitPrice: Double, isPerGram: Boolean
//   ): Double
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_unitPriceToTotal(
    _env: JNIEnv,
    _class: JClass,
    weight_gram: jdouble,
    unit_price: jdouble,
    is_per_gram: jboolean,
) -> jdouble {
    CostCalculator::unit_price_to_total(weight_gram, unit_price, is_per_gram != 0)
}

// ──────────────────────────────────────────────
// Kotlin: external fun engineInfo(): String
// 返回引擎描述（用于诊断）
// ──────────────────────────────────────────────
#[no_mangle]
pub extern "system" fn Java_com_example_shisuan_core_ShisuanCore_engineInfo(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let info = format!("shisuan-core v{} (rust)", ENGINE_VERSION);
    match env.new_string(info) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}
