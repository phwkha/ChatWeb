import forge from 'node-forge';
import { storePrivateKey, storePublicKey, getPrivateKey } from './indexedDB';

/**
 * Generate RSA Key Pair and store in IndexedDB
 * Returns the public key to be sent to the server
 */
export const generateAndStoreKeyPair = async (username) => {
  return new Promise((resolve, reject) => {
    forge.pki.rsa.generateKeyPair({ bits: 2048, workers: 2 }, async (err, keypair) => {
      if (err) {
        return reject(err);
      }
      
      const privateKeyPem = forge.pki.privateKeyToPem(keypair.privateKey);
      const publicKeyPem = forge.pki.publicKeyToPem(keypair.publicKey);
      
      await storePrivateKey(username, privateKeyPem);
      await storePublicKey(username, publicKeyPem);
      
      resolve(publicKeyPem);
    });
  });
};

/**
 * Encrypt message with recipient's public key
 */
export const encryptMessage = (message, publicKeyPem) => {
  try {
    const publicKey = forge.pki.publicKeyFromPem(publicKeyPem);
    const encrypted = publicKey.encrypt(message, 'RSA-OAEP', {
      md: forge.md.sha256.create(),
      mgf1: { md: forge.md.sha256.create() }
    });
    return forge.util.encode64(encrypted);
  } catch (error) {
    console.error("Encryption failed:", error);
    return null;
  }
};

/**
 * Decrypt message with user's private key
 */
export const decryptMessage = async (username, encryptedMessageBase64) => {
  try {
    const privateKeyPem = await getPrivateKey(username);
    if (!privateKeyPem) throw new Error("Private key not found in IndexedDB");
    
    const privateKey = forge.pki.privateKeyFromPem(privateKeyPem);
    const encryptedBytes = forge.util.decode64(encryptedMessageBase64);
    
    const decrypted = privateKey.decrypt(encryptedBytes, 'RSA-OAEP', {
      md: forge.md.sha256.create(),
      mgf1: { md: forge.md.sha256.create() }
    });
    
    return decrypted;
  } catch (error) {
    console.error("Decryption failed:", error);
    return null;
  }
};
