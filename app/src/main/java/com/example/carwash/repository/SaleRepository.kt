package com.example.carwash.repository

import com.example.carwash.add.Sale
import com.example.carwash.add.SaleDraft
import kotlinx.coroutines.flow.Flow

interface SaleRepository {

    suspend fun saveSale(
        draft: SaleDraft,
    ): Result<String>

    suspend fun getSale(
        saleId: String
    ): Result<Sale>

    suspend fun deleteSale(
        saleId: String
    ): Result<Unit>

    suspend fun updateSale(
        sale: Sale
    ): Result<Unit>

    fun observeRecentSale(
        limit: Long = 20
    ): Flow<List<Sale>>
}
