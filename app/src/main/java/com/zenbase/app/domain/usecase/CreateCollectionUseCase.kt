package com.zenbase.app.domain.usecase

import com.zenbase.app.domain.repository.CollectionRepository
import javax.inject.Inject

/**
 * Use case zodpovědný za formální deklaraci nové prázdné sbírky logicko-obchodních entit a databází s nimi korespondujících.
 */
class CreateCollectionUseCase @Inject constructor(
    private val collectionRepository: CollectionRepository
) {
    /**
     * Provede založení bez složité procesní vrstvy přímo delegující výkon dálší obslužné vrstvě.
     * @param name Název s validním obsahem od zadavatele.
     * @return Očekávaný autovygenerovaný klíč.
     */
    suspend operator fun invoke(name: String): Long {
        if (name.isBlank()) {
            throw IllegalArgumentException("Název kolekce nesmí být prázdný.")
        }
        return collectionRepository.createCollection(name)
    }
}
