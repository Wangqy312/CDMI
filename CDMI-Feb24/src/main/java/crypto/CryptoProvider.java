/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crypto;

//import junit.framework.TestCase;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.Use;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;

import org.jose4j.lang.JoseException;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.*;

//import com.nimbusds.jose.*;
//import com.nimbusds.jose.crypto.*;
/**
 *
 * @author 310219516
 */
public class CryptoProvider {
   
    public  String ekey;
    public  String dkey;
    public  String symkey;
    public  String km_alg; //key management algorithm
    public  String enc_alg; //encryption algorithm

    public CryptoProvider()
    {
        
    }
    
    public String getEkey() {
        return ekey;
    }

    public void setEkey(String ekey) {
        this.ekey = ekey;
    }

    public String getDkey() {
        return dkey;
    }

    public void setDkey(String dkey) {
        this.dkey = dkey;
    }

    public String getSymkey() {
        return symkey;
    }

    public void setSymkey(String symkey) {
        this.symkey = symkey;
    }

    public String getKm_alg() {
        return km_alg;
    }

    public void setKm_alg(String km_alg) {
        this.km_alg = km_alg;
    }

    public String getEnc_alg() {
        return enc_alg;
    }

    public void setEnc_alg(String enc_alg) {
        this.enc_alg = enc_alg;
    }
    
     //make sure symkey, km_alg, enc_alg has been initialized
    public String doJWEEncrypt(String plaintext)
    {
        String ciphertext = "";
        try
        {
            
            JsonWebKey jwk = JsonWebKey.Factory.newJwk(symkey);
            JsonWebEncryption senderJwe = new JsonWebEncryption();
            
            senderJwe.setAlgorithmHeaderValue(this.km_alg);
            senderJwe.setEncryptionMethodHeaderParameter(this.enc_alg);
            
             senderJwe.setKey(jwk.getKey());
            // The plaintext of the JWE is the message that we want to encrypt.
            senderJwe.setPlaintext(plaintext);
            //make sure symkey, km_alg, enc_alg has been initialized
            
            ciphertext =  senderJwe.getCompactSerialization();
            return ciphertext; 
        }
        catch(JoseException je)
        {
            je.printStackTrace();
        }
        
        return ciphertext;
        
        
    }
    
    public String doJWEDecrypt(String ciphertext)
    {
        String plaintext = "";
        try
        {
             JsonWebKey jwk = JsonWebKey.Factory.newJwk(symkey);
            JsonWebEncryption receiverJwe = new JsonWebEncryption();

            // Set the compact serialization on new Json Web Encryption object
            receiverJwe.setCompactSerialization(ciphertext);

            //receiverJwe.setAlgorithmHeaderValue(this.km_alg);
            //receiverJwe.setEncryptionMethodHeaderParameter(this.enc_alg);
            // Symmetric encryption, like we are doing here, requires that both parties have the same key.
            // The key will have had to have been securely exchanged out-of-band somehow.
             receiverJwe.setKey(jwk.getKey());

    // Get the message that was encrypted in the JWE. This step performs the actual decryption steps.
    plaintext = receiverJwe.getPlaintextString();

            return plaintext; 
        }
        catch(JoseException je)
        {
            je.printStackTrace();
        }
        
        return plaintext;
    }
    
    public static void main(String[] args) throws JoseException
    {
        CryptoProvider cp = new CryptoProvider();
        cp.setSymkey("{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2I\"}");
        cp.setEnc_alg(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
        cp.setKm_alg(KeyManagementAlgorithmIdentifiers.DIRECT);
        
        String cip = cp.doJWEEncrypt("hello");
        System.out.println("XXX: "+cip);
        String plain = cp.doJWEDecrypt(cip);
        System.out.println("XXX: "+plain);
       
    /*    
    //
    // An example showing the use of JSON Web Encryption (JWE) to encrypt and then decrypt some content
    // using a symmetric key and direct encryption.
    //

    // The content to be encrypted
    String message = "Well, as of this moment, they're on DOUBLE SECRET PROBATION!";

    // The shared secret or shared symmetric key represented as a octet sequence JSON Web Key (JWK)
    String jwkJson = "{\"kty\":\"oct\",\"k\":\"Fdh9u8rINxfivbrianbbVT1u232VQBZYKx1HGAGPt2I\"}";
    JsonWebKey jwk = JsonWebKey.Factory.newJwk(jwkJson);

    // Create a new Json Web Encryption object
    JsonWebEncryption senderJwe = new JsonWebEncryption();

    // The plaintext of the JWE is the message that we want to encrypt.
    senderJwe.setPlaintext(message);

    // Set the "alg" header, which indicates the key management mode for this JWE.
    // In this example we are using the direct key management mode, which means
    // the given key will be used directly as the content encryption key.
    senderJwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.DIRECT);

    // Set the "enc" header, which indicates the content encryption algorithm to be used.
    // This example is using AES_128_CBC_HMAC_SHA_256 which is a composition of AES CBC
    // and HMAC SHA2 that provides authenticated encryption.
    senderJwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);

    // Set the key on the JWE. In this case, using direct mode, the key will used directly as
    // the content encryption key. AES_128_CBC_HMAC_SHA_256, which is being used to encrypt the
    // content requires a 256 bit key.
    senderJwe.setKey(jwk.getKey());

    // Produce the JWE compact serialization, which is where the actual encryption is done.
    // The JWE compact serialization consists of five base64url encoded parts
    // combined with a dot ('.') character in the general format of
    // <header>.<encrypted key>.<initialization vector>.<ciphertext>.<authentication tag>
    // Direct encryption doesn't use an encrypted key so that field will be an empty string
    // in this case.
    String compactSerialization = senderJwe.getCompactSerialization();

    // Do something with the JWE. Like send it to some other party over the clouds
    // and through the interwebs.
    System.out.println("JWE compact serialization: " + compactSerialization);

    // That other party, the receiver, can then use JsonWebEncryption to decrypt the message.
    JsonWebEncryption receiverJwe = new JsonWebEncryption();

    // Set the compact serialization on new Json Web Encryption object
    receiverJwe.setCompactSerialization(compactSerialization);

    // Symmetric encryption, like we are doing here, requires that both parties have the same key.
    // The key will have had to have been securely exchanged out-of-band somehow.
    receiverJwe.setKey(jwk.getKey());

    // Get the message that was encrypted in the JWE. This step performs the actual decryption steps.
    String plaintext = receiverJwe.getPlaintextString();

    // And do whatever you need to do with the clear text message.
    System.out.println("plaintext: " + plaintext);
*/


    }
    /*
    public static void main(String[] args) throws Exception
    {   // The shared key
        byte[] key128 = {
            (byte)177, (byte)119, (byte) 33, (byte) 13, (byte)164, (byte) 30, (byte)108, (byte)121,
            (byte)207, (byte)136, (byte)107, (byte)242, (byte) 12, (byte)224, (byte) 19, (byte)226 };

        // Create the header
JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A128GCM);

// Set the plain text
Payload payload = new Payload("Hello world!");

// Create the JWE object and encrypt it
JWEObject jweObject = new JWEObject(header, payload);
jweObject.encrypt(new DirectEncrypter(key128));

// Serialise to compact JOSE form...
String jweString = jweObject.serialize();

// Parse into JWE object again...
jweObject = JWEObject.parse(jweString);

// Decrypt
jweObject.decrypt(new DirectDecrypter(key128));

// Get the plain text
payload = jweObject.getPayload();
System.out.println(jweObject);
System.out.println(payload);
    }*/
}
