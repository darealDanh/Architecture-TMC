package com.tmc.mobile.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tmc.mobile.domain.fakes.DeviceKeyStoreContract
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidKeystoreContractTest {

    private val alias = "tmc_contract_test_key"
    private val store = AndroidKeystoreDeviceKeyStore(alias)
    private val contract = DeviceKeyStoreContract(store, AndroidKeystorePayloadSigner(alias))

    @Before
    @After
    fun removeKey() {
        store.delete()
    }

    @Test
    fun createdKeyHasPublicKey() = contract.createdKeyHasPublicKey()

    @Test
    fun signaturesVerifyWithPublicKey() = contract.signaturesVerifyWithPublicKey()

    @Test
    fun createKeyWhenOneExistsKeepsTheSameKey() = contract.createKeyWhenOneExistsKeepsTheSameKey()

    @Test
    fun deleteRemovesKey() = contract.deleteRemovesKey()

    @Test
    fun signingWithoutKeyFailsAsMissingKey() = contract.signingWithoutKeyFailsAsMissingKey()
}
