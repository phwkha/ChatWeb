

const RSA_ALGO = "RSA-OAEP";
const RSA_HASH = "SHA-256";
const AES_ALGO = "AES-GCM";
const AES_LENGTH = 256;
const PBKDF2_ITERATIONS = 100000;


function base64ToBuffer(base64) {
    const binaryString = atob(base64);
    const len = binaryString.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryString.charCodeAt(i);
    }
    return bytes.buffer;
}

function bufferToBase64(buffer) {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    const len = bytes.byteLength;
    for (let i = 0; i < len; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
}

function textToBuffer(text) {
    return new TextEncoder().encode(text);
}

function bufferToText(buffer) {
    return new TextDecoder().decode(buffer);
}


export const cryptoService = {
    generateSalt: () => {
        return window.crypto.getRandomValues(new Uint8Array(16));
    },

    deriveKeyFromPin: async (pin, salt) => {
        const pinBuffer = textToBuffer(pin);
        const baseKey = await window.crypto.subtle.importKey(
            "raw",
            pinBuffer,
            { name: "PBKDF2" },
            false,
            ["deriveKey"]
        );
        return window.crypto.subtle.deriveKey(
            {
                name: "PBKDF2",
                salt: salt,
                iterations: PBKDF2_ITERATIONS,
                hash: "SHA-256",
            },
            baseKey,
            { name: AES_ALGO, length: AES_LENGTH },
            true,
            ["encrypt", "decrypt"]
        );
    },

    generateRsaKeyPair: async () => {
        return window.crypto.subtle.generateKey(
            {
                name: RSA_ALGO,
                modulusLength: 2048,
                publicExponent: new Uint8Array([0x01, 0x00, 0x01]), // 65537
                hash: RSA_HASH,
            },
            true,
            ["wrapKey", "unwrapKey"]
        );
    },

    exportPublicKey: async (key) => {
        const exported = await window.crypto.subtle.exportKey("spki", key);
        return bufferToBase64(exported);
    },

    exportPrivateKey: async (key) => {
        const exported = await window.crypto.subtle.exportKey("pkcs8", key);
        return bufferToBase64(exported);
    },

    importPublicKey: async (base64Key) => {
        const buffer = base64ToBuffer(base64Key);
        return window.crypto.subtle.importKey(
            "spki",
            buffer,
            { name: RSA_ALGO, hash: RSA_HASH },
            true,
            ["wrapKey"]
        );
    },

    importPrivateKey: async (base64Key) => {
        const buffer = base64ToBuffer(base64Key);
        return window.crypto.subtle.importKey(
            "pkcs8",
            buffer,
            { name: RSA_ALGO, hash: RSA_HASH },
            true,
            ["unwrapKey"]
        );
    },

    encryptPrivateKey: async (privateKey, pinDerivedKey, salt) => {
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const privateKeyBase64 = await cryptoService.exportPrivateKey(privateKey);
        const encodedKey = textToBuffer(privateKeyBase64);

        const encrypted = await window.crypto.subtle.encrypt(
            { name: AES_ALGO, iv: iv },
            pinDerivedKey,
            encodedKey
        );
        
        return bufferToBase64(salt) + "." + bufferToBase64(iv.buffer) + "." + bufferToBase64(encrypted);
    },

    decryptPrivateKey: async (encryptedData, pin) => {
        const parts = encryptedData.split('.');
        
        let salt, iv, encryptedKey;

        if (parts.length === 3) {
            salt = base64ToBuffer(parts[0]);
            iv = base64ToBuffer(parts[1]);
            encryptedKey = base64ToBuffer(parts[2]);
        } else {
            throw new Error("Dữ liệu khóa cũ không hỗ trợ Salt động. Vui lòng tạo khóa mới.");
        }

        const pinDerivedKey = await cryptoService.deriveKeyFromPin(pin, salt);

        const decryptedBuffer = await window.crypto.subtle.decrypt(
            { name: AES_ALGO, iv: iv },
            pinDerivedKey,
            encryptedKey
        );

        const privateKeyBase64 = bufferToText(decryptedBuffer);
        return cryptoService.importPrivateKey(privateKeyBase64);
    },

    // --- Quản lý Khóa Phiên (AES) ---

    generateSessionKey: async () => {
        return window.crypto.subtle.generateKey(
            { name: AES_ALGO, length: AES_LENGTH },
            true,
            ["encrypt", "decrypt"]
        );
    },

    exportRawKey: async (key) => {
        const exported = await window.crypto.subtle.exportKey("raw", key);
        return bufferToBase64(exported);
    },

    importRawKey: async (base64Key) => {
        const buffer = base64ToBuffer(base64Key);
        return window.crypto.subtle.importKey(
            "raw",
            buffer,
            { name: AES_ALGO },
            true,
            ["encrypt", "decrypt"]
        );
    },

    // --- Mã hóa/Wrap ---

    wrapSessionKey: async (sessionKey, publicKey) => {
        const exportedKey = await cryptoService.exportRawKey(sessionKey);
        const keyBuffer = base64ToBuffer(exportedKey);
        const wrapped = await window.crypto.subtle.wrapKey(
            "raw",
            sessionKey,
            publicKey,
            { name: RSA_ALGO, hash: RSA_HASH }
        );
        return bufferToBase64(wrapped);
    },

    unwrapSessionKey: async (wrappedKey, privateKey) => {
        try {
            console.log("unwrapSessionKey called. wrappedKey length:", wrappedKey ? wrappedKey.length : 0,
                "privateKey:", privateKey ? (privateKey.type || typeof privateKey) : 'null');
        } catch (d) {
            console.log("unwrapSessionKey diagnostics logging failed", d);
        }
        const keyBuffer = base64ToBuffer(wrappedKey);
        return window.crypto.subtle.unwrapKey(
            "raw",
            keyBuffer,
            privateKey,
            { name: RSA_ALGO, hash: RSA_HASH },
            { name: AES_ALGO, length: AES_LENGTH },
            true,
            ["encrypt", "decrypt"]
        );
    },

    // --- Mã hóa Tin nhắn ---

    encryptMessage: async (text, sessionKey) => {
        const iv = window.crypto.getRandomValues(new Uint8Array(12));
        const encodedText = textToBuffer(text);

        const ciphertext = await window.crypto.subtle.encrypt(
            { name: AES_ALGO, iv: iv },
            sessionKey,
            encodedText
        );

        return {
            iv: bufferToBase64(iv.buffer),
            ciphertext: bufferToBase64(ciphertext),
        };
    },

    decryptMessage: async (ciphertextB64, ivB64, sessionKey) => {
        try {
            console.log("decryptMessage called. ivB64 length:", ivB64 ? ivB64.length : 0, 
                "ciphertextB64 length:", ciphertextB64 ? ciphertextB64.length : 0,
                "sessionKey:", sessionKey ? (sessionKey.type || typeof sessionKey) : 'null');
        } catch (d) {
            console.log("decryptMessage diagnostics logging failed", d);
        }

        const iv = ivB64 ? base64ToBuffer(ivB64) : new Uint8Array(12).buffer;
        let ciphertext;
        try {
            ciphertext = base64ToBuffer(ciphertextB64);
        } catch (e) {
            console.error("decryptMessage: ciphertext is not valid base64", e, ciphertextB64);
            return "⚠️ Không thể giải mã tin nhắn này.";
        }

        try {
             const decryptedBuffer = await window.crypto.subtle.decrypt(
                { name: AES_ALGO, iv: iv },
                sessionKey,
                ciphertext
            );
            return bufferToText(decryptedBuffer);
        } catch (e) {
            console.error("Giải mã thất bại:", e);
            return "⚠️ Không thể giải mã tin nhắn này.";
        }
    }
};