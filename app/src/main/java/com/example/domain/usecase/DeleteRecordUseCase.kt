package com.example.domain.usecase

import com.example.database.RecordEntity
import com.example.domain.repository.RecordRepository
import javax.inject.Inject

/**
 * Encapsulace procesu vymazání existujícího záznamu pro striktní dodržení vzoru Clean Architecture.
 */
class DeleteRecordUseCase @Inject constructor(
    private val recordRepository: RecordRepository
) {
    /**
     * Využije existující infrastruktury repozitáře a deleguje kompletní proces trvalé výmazy po ověření oprávnění.
     * @param record Konkrétní odstranitelná logická datová sekvence.
     */
    suspend operator fun invoke(record: RecordEntity) {
        recordRepository.deleteRecord(record)
    }
}
