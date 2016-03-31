package com.amchealth.test;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class UtilSsl {

	static SSLContext sslContext = null;
	
	public static SSLContext getSslContext() {
		if (sslContext != null)
			return sslContext;
		
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");	
			InputStream caFile = UtilSsl.class.getResourceAsStream("server-crt.pem");
			Certificate ca;
			try {
				ca = cf.generateCertificate(caFile);
			} finally {
		        caFile.close();
			}
			
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, null);
			keyStore.setCertificateEntry("ca", ca);
			
	        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
	        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
	        tmf.init(keyStore);
	        
	        KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
	        kmf.init( keyStore,null);
	
			SSLContext context = SSLContext.getInstance( "TLS" );
	        context.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
	        sslContext = context;
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sslContext;
	}
}
