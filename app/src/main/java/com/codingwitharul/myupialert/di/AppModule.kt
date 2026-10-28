package com.codingwitharul.myupialert.di

import android.content.Context
import com.codingwitharul.myupialert.db.AppDatabase
import com.codingwitharul.myupialert.dedup.PaymentDeduplicator
import com.codingwitharul.myupialert.repository.SettingsRepository
import com.codingwitharul.myupialert.repository.TransactionRepository
import com.codingwitharul.myupialert.smsparser.SmsParser
import com.codingwitharul.myupialert.ui.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // Database and Dao
    single { AppDatabase.getInstance(androidContext()) }
    single { get<AppDatabase>().transactionDao() }
    
    // SharedPreferences
    single { androidContext().getSharedPreferences("UPI_PREFS", Context.MODE_PRIVATE) }
    
    // Repositories
    singleOf(::TransactionRepository)
    singleOf(::SettingsRepository)
    
    // Parsers & Deduplication Engine
    singleOf(::SmsParser)
    singleOf(::PaymentDeduplicator)
    
    // ViewModel
    viewModelOf(::MainViewModel)
}
