package com.example.data.repository

import com.example.data.local.dao.AiModelDao
import com.example.data.local.entity.AiModelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ModelRepository(private val aiModelDao: AiModelDao) {
    val allModels: Flow<List<AiModelEntity>> = aiModelDao.getAllModels()
    val enabledModels: Flow<List<AiModelEntity>> = aiModelDao.getEnabledModels()
    val enabledCount: Flow<Int> = aiModelDao.getEnabledModelCount()

    suspend fun getModelById(id: String): AiModelEntity? {
        return aiModelDao.getModelById(id)
    }

    suspend fun getDefaultModel(): AiModelEntity? {
        val default = aiModelDao.getDefaultModel()
        if (default != null && default.isEnabled) {
            return default
        }
        val enabledList = aiModelDao.getEnabledModels().first()
        return enabledList.firstOrNull()
    }

    suspend fun addModel(model: AiModelEntity) {
        if (model.isDefault) {
            aiModelDao.clearDefaultModel()
        }
        aiModelDao.insertModel(model)
    }

    suspend fun updateModel(model: AiModelEntity) {
        if (model.isDefault) {
            aiModelDao.clearDefaultModel()
        }
        aiModelDao.updateModel(model)
    }

    suspend fun toggleEnabled(id: String, enabled: Boolean) {
        val model = aiModelDao.getModelById(id) ?: return
        var newDefault = model.isDefault
        if (!enabled && model.isDefault) {
            newDefault = false
        }
        aiModelDao.updateModel(model.copy(isEnabled = enabled, isDefault = newDefault))

        // If default was disabled, find another enabled model to make default
        if (!enabled && model.isDefault) {
            val otherEnabled = aiModelDao.getEnabledModels().first().firstOrNull { it.id != id }
            if (otherEnabled != null) {
                aiModelDao.updateModel(otherEnabled.copy(isDefault = true))
            }
        }
    }

    suspend fun setDefaultModel(id: String) {
        val model = aiModelDao.getModelById(id) ?: return
        aiModelDao.clearDefaultModel()
        aiModelDao.updateModel(model.copy(isDefault = true, isEnabled = true))
    }

    suspend fun deleteModel(id: String) {
        val model = aiModelDao.getModelById(id)
        val wasDefault = model?.isDefault == true
        aiModelDao.deleteModel(id)

        if (wasDefault) {
            val remaining = aiModelDao.getEnabledModels().first().firstOrNull()
            if (remaining != null) {
                aiModelDao.updateModel(remaining.copy(isDefault = true))
            }
        }
    }
}
