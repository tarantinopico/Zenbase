package com.zenbase.app.domain.usecase

import com.zenbase.app.database.RecordEntity
import com.zenbase.app.domain.repository.RecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Jednoduchý delegátor k zajištění přenosu StateFlow záznamů od repozitáře k modelům uživatelských rozhraní.
 */
class GetRecordsForCollectionUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    /**
     * Předává asynchronní reaktivní záznam definované sady entit pro bezchybné zobrazení stavu aplikace.
     * @param collectionId ID sledované rodinné kolekce s datovými políčky.
     * @return Reaktivní StateFlow.
     */
    operator fun invoke(collectionId: Long): Flow<List<RecordEntity>> {
        return recordRepository.getRecordsForCollection(collectionId)
    }
}
