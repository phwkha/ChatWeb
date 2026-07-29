import { openDB } from 'idb';

const DB_NAME = 'ChatWebE2E';
const STORE_NAME = 'keys';

const initDB = async () => {
  return openDB(DB_NAME, 1, {
    upgrade(db) {
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME);
      }
    },
  });
};

export const storePrivateKey = async (username, privateKeyPem) => {
  const db = await initDB();
  await db.put(STORE_NAME, privateKeyPem, `${username}_privateKey`);
};

export const getPrivateKey = async (username) => {
  const db = await initDB();
  return db.get(STORE_NAME, `${username}_privateKey`);
};

export const storePublicKey = async (username, publicKeyPem) => {
  const db = await initDB();
  await db.put(STORE_NAME, publicKeyPem, `${username}_publicKey`);
};

export const getPublicKey = async (username) => {
  const db = await initDB();
  return db.get(STORE_NAME, `${username}_publicKey`);
};

export const clearKeys = async (username) => {
  const db = await initDB();
  await db.delete(STORE_NAME, `${username}_privateKey`);
  await db.delete(STORE_NAME, `${username}_publicKey`);
};
