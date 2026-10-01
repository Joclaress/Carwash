package com.example.carwash.model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.carwash.repository.AuthRepository
import com.example.carwash.repository.PayMongoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val payMongoRepository: PayMongoRepository
) : ViewModel() {
    private val _loading = MutableLiveData(false)
    val loading: MutableLiveData<Boolean> = _loading
    private val _error = MutableLiveData<String?>(null)
    val error: MutableLiveData<String?> = _error

    private val _user = MutableStateFlow<User?>(null)
    val user = _user.asStateFlow()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            repository.observeUser().collect { updatedUser ->
                _user.value = updatedUser
            }
        }
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.login(email, password)
            _loading.value = false

            if (result.isSuccess) {
                onSuccess()
                observeUser()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun register(
        email: String,
        password: String,
        name: String,
        adminPassword: String = "admin123",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.register(email, password, name, adminPassword)
            _loading.value = false

            if (result.isSuccess) {
                onSuccess()
                observeUser()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun updateAdminPassword(newPassword: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.updateAdminPassword(newPassword)
            if (result.isSuccess) {
                loadUser()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun loadUser() {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.getUser()
            _loading.value = false

            if (result.isSuccess) {
                _user.value = result.getOrNull()
            } else {
                _error.value = result.exceptionOrNull()?.message
            }
        }
    }

    fun createCheckoutSession(onUrlReady: (String) -> Unit) {
        viewModelScope.launch {
            val result = payMongoRepository.createCheckoutSession()
            val url = result.getOrDefault("https://paymongo.page/l/custoworks-sub")
            onUrlReady(url)
        }
    }

    fun activateSubscription(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val result = payMongoRepository.activateSubscription()
            _loading.value = false
            if (result.isSuccess) {
                loadUser()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
