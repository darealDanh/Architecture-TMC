package com.tmc.mobile.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.SessionStore
import kotlinx.coroutines.flow.first

class DataStoreSessionStore(private val store: DataStore<Preferences>) : SessionStore {

    override suspend fun current(): Session? = store.data.first()[KEY]?.let(SessionCodec::decode)

    override suspend fun save(session: Session) {
        store.edit { it[KEY] = SessionCodec.encode(session) }
    }

    override suspend fun clear() {
        store.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("session")
    }
}

class DataStoreCertificateStore(private val store: DataStore<Preferences>) : CertificateStore {

    override suspend fun current(): DeviceCertificate? = store.data.first()[KEY]?.let(CertificateCodec::decode)

    override suspend fun save(certificate: DeviceCertificate) {
        store.edit { it[KEY] = CertificateCodec.encode(certificate) }
    }

    override suspend fun clear() {
        store.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("device_certificate")
    }
}
