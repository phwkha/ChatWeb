import React, { createContext, useContext, useState, useCallback } from 'react';
import { authService } from '../services/authService';
import { cryptoService } from '../services/cryptoService';

const CryptoContext = createContext();

export const useCrypto = () => React.useContext(CryptoContext);

// Giữ nguyên salt cố định
// const HARDCODED_SALT = new Uint8Array([
//     12, 55, 201, 34, 9, 153, 22, 111,
//     78, 2, 213, 47, 88, 199, 234, 10
// ]);

export const CryptoProvider = ({ children }) => {
    const [isUnlocked, setIsUnlocked] = useState(false);
    const [rsaKeyPair, setRsaKeyPair] = useState(null);
    const [pinDerivedKey, setPinDerivedKey] = useState(null);
    const [isGenerating, setIsGenerating] = useState(false);
    const [error, setError] = useState('');

    const [isUnlockModalVisible, setIsUnlockModalVisible] = useState(false);
    const [pendingUnlockAction, setPendingUnlockAction] = useState(null);

    const lockKeys = useCallback(() => {
        setIsUnlocked(false);
        setRsaKeyPair(null);
        setPinDerivedKey(null);
        setError('');
        setPendingUnlockAction(null);
        setIsUnlockModalVisible(false);
    }, []);

    // Hàm này được gọi khi người dùng nhập PIN
    const unlockKeys = useCallback(async (pin) => {
        setError('');
        try {
            const encryptedKeyData = await authService.getEncryptedRsaKey();
            if (!encryptedKeyData) {
                setError("Không tìm thấy khóa. Vui lòng tạo khóa mới.");
                return false;
            }

            const privateKey = await cryptoService.decryptPrivateKey(encryptedKeyData, pin);
            
            // Lấy Public Key
            const currentUser = authService.getCurrentUser();
            const publicKey = await authService.getPublicKey(currentUser.username);

            setRsaKeyPair({ publicKey, privateKey });
            setIsUnlocked(true);
            setIsUnlockModalVisible(false);

            if (typeof pendingUnlockAction === 'function') {
                pendingUnlockAction(); 
                setPendingUnlockAction(null); 
            }
            return true;
        } catch (e) {
            console.error("Unlock failed:", e);
            setError("PIN không đúng hoặc khóa bị hỏng.");
            return false;
        }
    }, [pendingUnlockAction]);

    const generateAndSaveKeys = useCallback(async (pin) => {
        setError('');
        setIsGenerating(true);
        try {
            const salt = cryptoService.generateSalt(); 
            
            const derivedKey = await cryptoService.deriveKeyFromPin(pin, salt);
            const { publicKey, privateKey } = await cryptoService.generateRsaKeyPair();

            const encryptedKey = await cryptoService.encryptPrivateKey(privateKey, derivedKey, salt);
            
            await authService.savePublicKey(publicKey);
            await authService.saveEncryptedRsaKey(encryptedKey);

            setRsaKeyPair({ publicKey, privateKey });
            setPinDerivedKey(derivedKey);
            setIsUnlocked(true);
            setIsUnlockModalVisible(false);

            if (typeof pendingUnlockAction === 'function') {
                pendingUnlockAction();
                setPendingUnlockAction(null);
            }
            return true;
        } catch (e) {
            console.error("Generate keys failed:", e);
            setError("Không thể tạo khóa. " + e.message);
            return false;
        } finally {
            setIsGenerating(false);
        }
    }, [pendingUnlockAction]);

    const requestUnlock = useCallback((action = null) => {
        if (isUnlocked) {
            if (action) action();
        } else {
            setPendingUnlockAction(() => action);
            setIsUnlockModalVisible(true);
        }
    }, [isUnlocked]);

    const cancelUnlock = useCallback(() => {
        setIsUnlockModalVisible(false);
        setPendingUnlockAction(null);
        setError('');
    }, []);

    const value = {
        isUnlocked,
        rsaKeyPair,
        pinDerivedKey,
        unlockKeys,
        generateAndSaveKeys,
        isGenerating,
        error,

        isUnlockModalVisible,
        requestUnlock,
        cancelUnlock,
        lockKeys
    };

    return (
        <CryptoContext.Provider value={value}>
            {children}
        </CryptoContext.Provider>
    );
};