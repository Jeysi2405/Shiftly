package com.example.shiftly

import com.example.shiftly.utils.PinSecurity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityTest {

    @Test
    fun isValidPinFormat_acceptsExactlyFourDigits() {
        assertTrue(PinSecurity.isValidPinFormat("0000"))
        assertTrue(PinSecurity.isValidPinFormat("4829"))
        assertFalse(PinSecurity.isValidPinFormat(""))
        assertFalse(PinSecurity.isValidPinFormat("123"))
        assertFalse(PinSecurity.isValidPinFormat("12345"))
        assertFalse(PinSecurity.isValidPinFormat("12a4"))
    }

    @Test
    fun hashPin_usesSaltedPbkdf2AndVerifiesCorrectly() {
        val hashA = PinSecurity.hashPin("4829")
        val hashB = PinSecurity.hashPin("4829")

        assertTrue(PinSecurity.isHashed(hashA))
        assertNotEquals(hashA, hashB)
        assertTrue(PinSecurity.verifyPin("4829", hashA))
        assertTrue(PinSecurity.verifyPin("4829", hashB))
        assertFalse(PinSecurity.verifyPin("0000", hashA))
        assertFalse(PinSecurity.verifyPin("482", hashA))
        assertFalse(PinSecurity.verifyPin("4829", ""))
    }

    @Test
    fun verifyPin_supportsLegacyPlaintextFallback() {
        assertFalse(PinSecurity.isHashed("1234"))
        assertTrue(PinSecurity.verifyPin("1234", "1234"))
        assertFalse(PinSecurity.verifyPin("9999", "1234"))
    }
}
