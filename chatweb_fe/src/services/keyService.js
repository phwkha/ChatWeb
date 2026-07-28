import { api } from "./api";
import { cryptoService } from "./cryptoService";

export const keyService = {
    getPublicKey: async (username) => {
        try {
            const response = await api.get(`/api/keys/public-key/${username}`);
            if (response.data && response.data.publicKey) {
                return cryptoService.importPublicKey(response.data.publicKey);
            }
            return null;
        } catch (error) {
            console.error("Fetch public key error:", error);
            throw error;
        }
    },

    savePublicKey: async (publicKey) => {
        try {
            const exportedKey = await cryptoService.exportPublicKey(publicKey);
            await api.post("/api/keys/public-key", { publicKey: exportedKey });
        } catch (error) {
            console.error("Save public key error:", error);
            throw error;
        }
    },

    getEncryptedRsaKey: async () => {
        try {
            const response = await api.get("/api/keys/rsa");
            return response.data.key;
        } catch (error) {
            console.error("Get RSA key error:", error);
            throw error;
        }
    },

    saveEncryptedRsaKey: async (encryptedKey) => {
        try {
            await api.post("/api/keys/rsa", { key: encryptedKey });
        } catch (error) {
            console.error("Save RSA key error:", error);
            throw error;
        }
    }
};
