package com.zenbase.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zenbase.app.domain.repository.FieldDefinitionRepository
import com.zenbase.app.database.FieldDefinitionDao
import com.zenbase.app.domain.repository.RecordRepository
import com.zenbase.app.database.RecordDao
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CsvUseCaseTest {

    @Test
    fun testUseCasesInitialization() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = com.zenbase.app.di.AppContainer(context)
        
        val exportUseCase = container.exportCsvUseCase
        assertNotNull(exportUseCase)
        
        val importUseCase = container.importCsvUseCase
        assertNotNull(importUseCase)
    }
}
