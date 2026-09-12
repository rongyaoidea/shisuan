package com.example.shisuan.ui.viewModel

import androidx.lifecycle.viewModelScope
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.database.IngredientWithUseCount
import com.example.shisuan.data.repository.CostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 配料库 ViewModel - 全局原料库管理
 * 展示所有产品录入过的原料（名称 + 最新成本 + 使用频次），按频次降序排列
 */
@HiltViewModel
class IngredientLibraryViewModel @Inject constructor(
    private val repo: CostRepository
) : BaseViewModel() {

    /** 全部原料（按使用频次降序，常用在前；频次同值按名称升序） */
    val ingredients: StateFlow<List<IngredientWithUseCount>> =
        repo.allIngredientsWithUseCount.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )

    /** 新增原料：按名称+品牌去重，同名同品牌存在则更新为最新成本 */
    fun saveIngredient(name: String, brand: String, category: String, unitPricePerKg: Double) {
        launchSafe {
            repo.saveIngredientByNameAndBrand(name, brand, category, unitPricePerKg)
        }
    }

    /** 编辑原料：更新名称/品牌/分类/成本 */
    fun updateIngredient(
        ingredient: Ingredient,
        name: String, brand: String, category: String, unitPricePerKg: Double
    ) {
        launchSafe {
            val targetName = name.trim()
            val targetBrand = brand.trim()
            // 改名/改品牌可能撞上 (name, supplier) 唯一索引：先查重给出明确提示，
            // 而不是让 DB 层抛出笼统的「数据冲突」。
            val duplicate = repo.findIngredientByNameAndBrand(targetName, targetBrand)
            if (duplicate != null && duplicate.id != ingredient.id) {
                val label = if (targetBrand.isEmpty()) targetName else "$targetName（$targetBrand）"
                showError("配料库中已存在「$label」，请直接编辑该条或修改品牌")
                return@launchSafe
            }
            repo.updateIngredient(
                ingredient.copy(
                    name = targetName,
                    supplier = targetBrand,
                    category = category,
                    unitPrice = unitPricePerKg,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 删除原料 */
    fun deleteIngredient(ingredient: Ingredient) {
        launchSafe {
            repo.deleteIngredient(ingredient)
        }
    }
}
