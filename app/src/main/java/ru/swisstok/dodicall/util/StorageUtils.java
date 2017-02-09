/*
 *
 * Copyright (C) 2016, Telco Cloud Trading & Logistic Ltd
 *
 * This file is part of dodicall.
 * dodicall is free software : you can redistribute it and / or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * dodicall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dodicall.If not, see <http://www.gnu.org/licenses/>.
 */

package ru.swisstok.dodicall.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.IntDef;
import android.util.Base64;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;

public class StorageUtils {

    private static final String PRIVATE_KEY = "private_key";
    private static final String PUBLIC_KEY = "public_key";
    private static final String PASSWORD = "password";

    private static final String ALIAS = "dodicall";
    private static final String ALGORITHM = "RSA";
    private static final String CIPHER = "RSA/ECB/PKCS1Padding";

    private static final int PUBLIC_KEY_TYPE = 1;
    private static final int PRIVATE_KEY_TYPE = 2;

    @Retention(RetentionPolicy.SOURCE)
    @Target(ElementType.PARAMETER)
    @IntDef({PUBLIC_KEY_TYPE, PRIVATE_KEY_TYPE})
    @interface KeyType {
    }

    private static final boolean IS_BEFORE_JELLYBEAN;
    private static final String PROVIDER;
    private static final String GENERATOR_PROVIDER;

    static {
        IS_BEFORE_JELLYBEAN = Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2;
        PROVIDER = IS_BEFORE_JELLYBEAN ? "BouncyCastle" : "AndroidKeyStore";
        GENERATOR_PROVIDER = IS_BEFORE_JELLYBEAN ? "BC" : "AndroidKeyStore";
    }

    public static void storePassword(Context context, char[] password) throws Exception {
        storeData(context, password, PASSWORD);
    }

    public static char[] getPassword(Context context) throws Exception {
        return getData(context, PASSWORD);
    }

    public static void storeChatKey(Context context, char[] chatKey, String userEmail) throws Exception {
        byte[] bytes = new byte[chatKey.length];
        for (int i = 0; i < chatKey.length; i++) {
            bytes[i] = (byte) chatKey[i];
        }
        storeBytes(context, bytes, userEmail);
    }

    public static char[] getChatKey(Context context, String userEmail) throws Exception {
        byte[] passwordBytes = getBytes(context, userEmail);
        char[] chars = new char[passwordBytes.length];
        for (int i = 0; i < passwordBytes.length; i++) {
            chars[i] = (char) passwordBytes[i];
        }
        return chars;
    }

    private static void storeData(Context context, char[] data, String storageIdentifier) throws Exception {
        PublicKey publicKey = (PublicKey) getKey(context, PUBLIC_KEY_TYPE, true);

        Cipher c = Cipher.getInstance(CIPHER);
        c.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        byte[] encodedBytes = c.doFinal(bytes);
        storeBytes(context, encodedBytes, storageIdentifier);
    }

    private static char[] getData(Context context, String storageIdentifier) throws Exception {
        PrivateKey privateKey = (PrivateKey) getKey(context, PRIVATE_KEY_TYPE, false);

        byte[] passwordBytes = getBytes(context, storageIdentifier);
        Cipher c = Cipher.getInstance(CIPHER);
        c.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = c.doFinal(passwordBytes);
        char[] chars = new char[decodedBytes.length];
        for (int i = 0; i < decodedBytes.length; i++) {
            chars[i] = (char) decodedBytes[i];
        }
        return chars;
    }

    private static boolean hasKeys(Context context) throws Exception {
        if (!IS_BEFORE_JELLYBEAN) {
            KeyStore keyStore = KeyStore.getInstance(PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(ALIAS);
        } else {
            SharedPreferences sharedPreferences = context.getSharedPreferences(ALIAS, Context.MODE_PRIVATE);
            return sharedPreferences.contains(PRIVATE_KEY) && sharedPreferences.contains(PUBLIC_KEY);
        }
    }

    private static KeyPairGenerator getKeyGenerator(Context context) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, GENERATOR_PROVIDER);
        if (!IS_BEFORE_JELLYBEAN) {
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 25);
            KeyPairGeneratorSpec kg = new KeyPairGeneratorSpec.Builder(context).
                    setAlias(ALIAS).
                    setSubject(new X500Principal("CN=dodicall, O=Android Authority")).
                    setSerialNumber(BigInteger.ONE).
                    setStartDate(start.getTime()).
                    setEndDate(end.getTime()).
                    build();
            kpg.initialize(kg);
        } else {
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F0);
            kpg.initialize(spec);
        }
        return kpg;
    }

    private static void storeKeys(PublicKey publicKey, PrivateKey privateKey, Context context) {
        if (IS_BEFORE_JELLYBEAN) {
            storeBytes(context, publicKey.getEncoded(), PUBLIC_KEY);
            storeBytes(context, privateKey.getEncoded(), PRIVATE_KEY);
        }
    }

    private static Key getKey(Context context, @KeyType int keyType, boolean withKeyGeneration) throws Exception {
        if (!hasKeys(context)) {
            if (withKeyGeneration) {
                KeyPairGenerator kpg = getKeyGenerator(context);
                KeyPair kp = kpg.generateKeyPair();
                PublicKey publicKey = kp.getPublic();
                PrivateKey privateKey = kp.getPrivate();
                storeKeys(publicKey, privateKey, context);
            } else {
                throw new IllegalStateException("Password is retrieved before Keys were generated");
            }
        }
        if (!IS_BEFORE_JELLYBEAN) {
            KeyStore keyStore = KeyStore.getInstance(PROVIDER);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(ALIAS, null);
            if (keyType == PUBLIC_KEY_TYPE) {
                return privateKeyEntry.getCertificate().getPublicKey();
            } else if (keyType == PRIVATE_KEY_TYPE) {
                return privateKeyEntry.getPrivateKey();
            }
        } else {
            KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
            if (keyType == PUBLIC_KEY_TYPE) {
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(getBytes(context, PUBLIC_KEY));
                return keyFactory.generatePublic(publicKeySpec);
            } else if (keyType == PRIVATE_KEY_TYPE) {
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(getBytes(context, PRIVATE_KEY));
                return keyFactory.generatePrivate(privateKeySpec);
            }
        }
        throw new IllegalStateException("Key Type is unknown");
    }


    private static void storeBytes(Context context, byte[] bytes, String storeKey) {
        String encodedBytesString = Base64.encodeToString(bytes, Base64.DEFAULT);
        context.getSharedPreferences(ALIAS, Context.MODE_PRIVATE).
                edit().
                putString(storeKey, encodedBytesString).
                apply();
    }

    private static byte[] getBytes(Context context, String storeKey) {
        String privateString = context.getSharedPreferences(ALIAS, Context.MODE_PRIVATE).getString(storeKey, "");
        return Base64.decode(privateString, Base64.DEFAULT);
    }
}
