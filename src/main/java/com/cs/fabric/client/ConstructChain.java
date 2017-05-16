package com.cs.fabric.client;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainConfiguration;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;

import com.cs.fabric.client.utils.ClientHelper;
import com.cs.fabric.sdk.utils.ClientConfig;
import com.cs.fabric.sdkintegration.SampleOrg;

public class ConstructChain {

	private static final ClientHelper clientHelper = new ClientHelper();
	private static final ClientConfig clientConfig = ClientConfig.getConfig();

	private static final String TEST_FIXTURES_PATH = "src/test/fixture";
	private static final String FOO_CHAIN_NAME = "foo";

	private static final Log logger = LogFactory.getLog(ConstructChain.class);

	public static void main(String[] args) throws Exception {

		// Get Org1
		SampleOrg sampleOrg = clientHelper.getSamleOrg();

		// Create instance of client.
		HFClient client = clientHelper.getHFClient();

		// Begin construction
		logger.info("Constructing chain " + FOO_CHAIN_NAME);

		Collection<Orderer> orderers = new LinkedList<>();

		for (String orderName : sampleOrg.getOrdererNames()) {
			orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
					clientConfig.getOrdererProperties(orderName)));
		}

		// Just pick the first orderer in the list to create the chain.

		Orderer anOrderer = orderers.iterator().next();
		orderers.remove(anOrderer);

		ChainConfiguration chainConfiguration = new ChainConfiguration(
				new File(TEST_FIXTURES_PATH + "/sdkintegration/e2e-2Orgs/channel/" + FOO_CHAIN_NAME + ".tx"));

		// Only peer Admin org
		client.setUserContext(sampleOrg.getPeerAdmin());

		// Create chain that has only one signer that is this orgs peer admin.
		// If chain creation policy needed more signature they would need to be
		// added too.
		Chain newChain = client.newChain(FOO_CHAIN_NAME, anOrderer, chainConfiguration,
				client.getChainConfigurationSignature(chainConfiguration, sampleOrg.getPeerAdmin()));

		logger.info("Created chain " + FOO_CHAIN_NAME);

		for (String peerName : sampleOrg.getPeerNames()) {
			String peerLocation = sampleOrg.getPeerLocation(peerName);

			Properties peerProperties = clientConfig.getPeerProperties(peerName);// test
																					// properties
																					// for
																					// peer..
																					// if
																					// any.
			if (peerProperties == null) {
				peerProperties = new Properties();
			}
			// Example of setting specific options on grpc's
			// ManagedChannelBuilder
			peerProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);

			Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
			newChain.joinPeer(peer);
			logger.info("Peer " + peerName + "joined chain " + FOO_CHAIN_NAME);
			sampleOrg.addPeer(peer);
		}

		for (Orderer orderer : orderers) { // add remaining orderers if any.
			newChain.addOrderer(orderer);
		}

		for (String eventHubName : sampleOrg.getEventHubNames()) {
			EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
					clientConfig.getEventHubProperties(eventHubName));
			newChain.addEventHub(eventHub);
		}

		newChain.initialize();

		logger.info("Finished initialization chain " + FOO_CHAIN_NAME);
		
		//newChain.shutdown(true);
		
		//logger.info("Shutdown chain " + FOO_CHAIN_NAME);

	}
}
