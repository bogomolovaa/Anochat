package bogomolov.aa.anochat.repository

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositoryImpl
@Inject constructor(private val db: AppDatabase) : Repository {

}