package com.cs.fabric.client;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainCodeID;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import com.cs.fabric.client.utils.ClientHelper;
import com.cs.fabric.sdk.utils.ClientConfig;

public class InvokeChainCode {

	private static final ClientHelper clientHelper = new ClientHelper();
	private static final ClientConfig clientConfig = ClientConfig.getConfig();
	private static final Log logger = LogFactory.getLog(InvokeChainCode.class);
	private String[] args;

	public InvokeChainCode(String[] args) {
		this.args = args;
	}

	public void invoke() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException,
			CryptoException, InvalidArgumentException, TransactionException, IOException, InterruptedException,
			ExecutionException, TimeoutException, ProposalException {

		Collection<ProposalResponse> successful = new LinkedList<>();
		Collection<ProposalResponse> failed = new LinkedList<>();

		// Create instance of client.
		HFClient client = clientHelper.getHFClient();

		// Create instance of chain.
		Chain chain = clientHelper.getChainWithPeerAdmin();

		// Create instance of ChainCodeID
		ChainCodeID chainCodeID = clientHelper.getChainCodeID();

		///////////////
		/// Send transaction proposal to all peers
		TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
		transactionProposalRequest.setChaincodeID(chainCodeID);
		transactionProposalRequest.setFcn("invoke");
		transactionProposalRequest.setArgs(args);

		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
		tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
		tm2.put("result", ":)".getBytes(UTF_8)); /// This should be returned see
													/// chaincode.
		transactionProposalRequest.setTransientMap(tm2);

		logger.info(
				"sending transactionProposal to all peers with arguments inspect(IMLC-000001,Chinasystem,China Ocean Shipping Company)");

		Collection<ProposalResponse> transactionPropResp = chain.sendTransactionProposal(transactionProposalRequest,
				chain.getPeers());
		for (ProposalResponse response : transactionPropResp) {
			if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
				// out("Successful transaction proposal response Txid: %s from
				// peer %s", response.getTransactionID(),
				// response.getPeer().getName());
				successful.add(response);
			} else {
				failed.add(response);
			}
		}
		// out("Received %d transaction proposal responses. Successful+verified:
		// %d . Failed: %d",
		// transactionPropResp.size(), successful.size(), failed.size());
		if (failed.size() > 0) {
			ProposalResponse firstTransactionProposalResponse = failed.iterator().next();
			logger.error("Not enough endorsers for inspect(IMLC-000001,Chinasystem,China Ocean Shipping Company):"
					+ failed.size() + " endorser error: " + firstTransactionProposalResponse.getMessage()
					+ ". Was verified: " + firstTransactionProposalResponse.isVerified());
		} else {
			logger.info("Successfully received transaction proposal responses.");
		}
		ProposalResponse resp = transactionPropResp.iterator().next();
		byte[] x = resp.getChainCodeActionResponsePayload();
		String resultAsString = null;
		if (x != null) {
			resultAsString = new String(x, "UTF-8");
		}
		logger.info(resultAsString);

		////////////////////////////
		// Send Transaction Transaction to orderer
		logger.info(
				"Sending chain code transaction(inspect(IMLC-000001,Chinasystem,China Ocean Shipping Company)) to orderer.");
		logger.info("sending transaction proposal to orderer");
		chain.sendTransaction(successful).thenApply(transactionEvent -> {
			if (transactionEvent.isValid()) {
				logger.info("Successfully send transaction proposal to orderer. Transaction ID: "
						+ transactionEvent.getTransactionID());
			} else {
				logger.info("Failed to send transaction proposal to orderer");
			}
			// chain.shutdown(true);
			return transactionEvent.getTransactionID();
		}).get(clientConfig.getTransactionWaitTime(), TimeUnit.SECONDS);

	}

	public void queryByRefNo() throws CryptoException, InvalidArgumentException, NoSuchAlgorithmException,
			NoSuchProviderException, InvalidKeySpecException, TransactionException, IOException, ProposalException {

		// Create instance of client.
		HFClient client = clientHelper.getHFClient();

		// Create instance of chain.
		Chain chain = clientHelper.getChainWithPeerAdmin();

		// Create instance of ChainCodeID
		ChainCodeID chainCodeID = clientHelper.getChainCodeID();

		QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
		queryByChaincodeRequest.setArgs(args);
		queryByChaincodeRequest.setFcn("invoke");
		queryByChaincodeRequest.setChaincodeID(chainCodeID);

		Map<String, byte[]> tm2 = new HashMap<>();
		tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
		tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
		queryByChaincodeRequest.setTransientMap(tm2);

		Collection<ProposalResponse> queryProposals = chain.queryByChaincode(queryByChaincodeRequest, chain.getPeers());
		for (ProposalResponse proposalResponse : queryProposals) {
			if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
				logger.info("Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: "
						+ proposalResponse.getStatus() + ". Messages: " + proposalResponse.getMessage()
						+ ". Was verified : " + proposalResponse.isVerified());
			} else {
				String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
				logger.info("Query payload of IMLC-0001 from peer: " + proposalResponse.getPeer().getName());
				logger.info("" + payload);
			}
		}

	}

}
