package com.cs.fabric.client;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

public class QueryByRefNo {

	public static void main(String[] args) throws CryptoException, InvalidArgumentException, NoSuchAlgorithmException,
			NoSuchProviderException, InvalidKeySpecException, TransactionException, IOException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {

		final String[] arguments = new String[] { "query", "IMLC-000001" };

		InvokeChainCode invokeChainCode = new InvokeChainCode(arguments);

		try {
			invokeChainCode.queryByRefNo();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProposalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
