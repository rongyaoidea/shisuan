//! JNI 桥接层
//!
//! 将纯计算逻辑暴露给 Kotlin：
//! `com.example.shisuan.core.ShisuanCore` 对象中的 `external fun` 一一对应。

use jni::JNIEnv;
use jni::objects::{JClass, JDoubleArray};
use jni::sys::{jboolean, jdouble, jdoubleArray, jint, jstring};

use crate::calc::CostCalculator;

const ERROR_OUT_OF_MEMORY: jint = -2; // 写回 out 数组失败
const ERROR_INVALID_ARGS: jint = 2; // out 数组为 null 或长度不足，或数值输入非有限（Kotlin 侧非 0 即回退实现）
const OK: jint = 0;

/// 引擎版本号
const ENGINE_VERSION: &str = "1.0.0";

// ──────────────────────────────────────────────
// Kotlin: external fun version(): String?
// Rust 建串失败时返回 null，Kotlin 侧需 ?: 处理防 NPE
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
// 返回 OK(0) = 成功，ERROR_INVALID_ARGS(2) = 参数无效/out 数组无效（Kotlin 侧非 0 即回退 Kotlin 实现），
// ERROR_OUT_OF_MEMORY(-2) = 写回失败
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
    // 卫语句：非有限输入直接返回 ERROR_INVALID_ARGS，与 Kotlin 侧 sanitize 一致
    //（Kotlin 侧已先拦截，此处防未来直调 JNI 的调用方违约）。
    if !sample_weight_gram.is_finite()
        || !material_cost.is_finite()
        || !processing_cost.is_finite()
        || !weight_per_box_gram.is_finite()
    {
        return ERROR_INVALID_ARGS;
    }
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

    // 防御：out 为 null 或长度不足时直接拒绝，避免越界写导致 JVM 崩溃
    // （Kotlin 契约固定传 DoubleArray(5)，此处只防未来调用方违约）
    if out.is_null() {
        return ERROR_INVALID_ARGS;
    }
    let array = unsafe { JDoubleArray::from_raw(out) };
    match env.get_array_length(&array) {
        Ok(len) if (len as usize) >= buf.len() => {}
        _ => return ERROR_INVALID_ARGS,
    }
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
// 返回 OK(0) = 成功计算，1 = 无上一批次可对比，ERROR_INVALID_ARGS(2) = out 数组无效
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

    // 防御：out 为 null 或长度不足时直接拒绝（同 calculate）
    if out.is_null() {
        return ERROR_INVALID_ARGS;
    }
    let array = unsafe { JDoubleArray::from_raw(out) };
    match env.get_array_length(&array) {
        Ok(len) if len >= 3 => {}
        _ => return ERROR_INVALID_ARGS,
    }
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
// half-away 实现；与 Kotlin 在负数 -x.xx5 差 1 分钱，成本域非负钳零故无影响。
// 注：Kotlin 侧 round2 已常驻 Kotlin 实现，此 JNI 仅保留供诊断/兼容。
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
    // 卫语句：非有限输入返回 0.0，与 Kotlin 侧先拦截行为一致。
    if !weight_gram.is_finite() || !unit_price.is_finite() {
        return 0.0;
    }
    CostCalculator::unit_price_to_total(weight_gram, unit_price, is_per_gram != 0)
}

// ──────────────────────────────────────────────
// Kotlin: external fun engineInfo(): String?
// 返回引擎描述（用于诊断）；建串失败返回 null，Kotlin 侧需 ?: "" 处理
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
