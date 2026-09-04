package com.example.shisuan.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shisuan.data.database.Ingredient
import com.example.shisuan.data.repository.CostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 配料库 ViewModel - 全局原料库管理
 * 展示所有产品录入过的原料（名称 + 最新成本），跨产品复用
 */
@HiltViewModel
class IngredientLibraryViewModel @Inject constructor(
    private val repo: CostRepository
) : ViewModel() {

    /** 全部原料（按名称排序） */
    val ingredients: StateFlow<List<Ingredient>> =
        repo.allIngredients.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** 新增原料：按名称+品牌去重，同名同品牌存在则更新为最新成本 */
    fun saveIngredient(name: String, brand: String, category: String, unitPricePerKg: Double) {
        viewModelScope.launch {
            repo.saveIngredientByNameAndBrand(name, brand, category, unitPricePerKg)
        }
    }

    /** 编辑原料：更新名称/品牌/分类/成本 */
    fun updateIngredient(
        ingredient: Ingredient,
        name: String, brand: String, category: String, unitPricePerKg: Double
    ) {
        viewModelScope.launch {
            repo.updateIngredient(
                ingredient.copy(
                    name = name.trim(),
                    supplier = brand.trim(),
                    category = category,
                    unitPrice = unitPricePerKg,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 删除原料 */
    fun deleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            repo.deleteIngredient(ingredient)
        }
    }
}
