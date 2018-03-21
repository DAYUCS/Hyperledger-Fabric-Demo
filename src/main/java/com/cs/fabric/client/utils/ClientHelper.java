package com.cs.fabric.client.utils;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import com.cs.fabric.sdk.utils.ClientConfig;
import com.cs.fabric.sdkintegration.SampleOrg;
import com.cs.fabric.sdkintegration.SampleStore;
import com.cs.fabric.sdkintegration.SampleUser;

public class ClientHelper {

	private static final String TEST_ADMIN_NAME = "admin";
	private static final String TESTUSER_1_NAME = "user1";
	private static final String FOO_CHANNEL_NAME = "foo";
	private static final String CHAIN_CODE_NAME = "trade_finance_go";
	private static final String CHAIN_CODE_PATH = "github.com/trade_finance";
	private static final String CHAIN_CODE_VERSION = "1";
	private static final ClientConfig clientConfig = ClientConfig.getConfig();

	private static final Log logger = LogFactory.getLog(ClientHelper.class);

	public SampleOrg getSamleOrg()
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException {

		// Get SampleStore
		File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
		SampleStore sampleStore = new SampleStore(sampleStoreFile);

		// Get Org1 from configuration
		SampleOrg sampleOrg = clientConfig.getIntegrationTestsSampleOrg("peerOrg1");
		logger.info("Get peerOrg1 SampleOrg");

		// Set up HFCA for Org1
		sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
		logger.info("Set CA Client of peerOrg1 SampleOrg");

		sampleOrg.setAdmin(sampleStore.getMember(TEST_ADMIN_NAME, sampleOrg.getName())); // The
																							// admin
																							// of
																							// this
																							// org
		sampleOrg.addUser(sampleStore.getMember(TESTUSER_1_NAME, sampleOrg.getName())); // The
																						// user
																						// of
																						// this
																						// org

		return setPeerAdmin(sampleStore, sampleOrg);
	}

	public SampleOrg setPeerAdmin(SampleStore sampleStore, SampleOrg sampleOrg)
			throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		final String sampleOrgName = sampleOrg.getName();
		final String sampleOrgDomainName = sampleOrg.getDomainName();

		SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
				findFile_sk(Paths.get(clientConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
						sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
				Paths.get(clientConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
						format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName,
								sampleOrgDomainName))
						.toFile());

		sampleOrg.setPeerAdmin(peerOrgAdmin); // A special user that can crate
												// channels, join peers and
												// install chain code
												// and jump tall blockchains in
												// a single leap!
		return sampleOrg;
	}

	public HFClient getHFClient() throws CryptoException, InvalidArgumentException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {

		// Create instance of client.
		HFClient client = HFClient.createNewInstance();
		logger.info("Create instance of HFClient");

		client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
		logger.info("Set Crypto Suite of HFClient");

		return client;
	}

	public static File findFile_sk(File directory) {

		File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

		if (null == matches) {
			throw new RuntimeException(
					format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
		}

		if (matches.length != 1) {
			throw new RuntimeException(format("Expected in %s only 1 sk file but found %d",
					directory.getAbsoluteFile().getName(), matches.length));
		}

		return matches[0];

	}

	public Channel getChannel() throws NoSuchAlgorithmException, NoSuchProviderException,
			InvalidKeySpecException, IOException, CryptoException, InvalidArgumentException, TransactionException, IllegalAccessException, InstantiationException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
		SampleOrg sampleOrg = this.getSamleOrg();
		HFClient client = this.getHFClient();

		client.setUserContext(sampleOrg.getPeerAdmin());
		return getChannel(sampleOrg, client);

	}

//	public Channel getChannelWithUser() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException,
//			IOException, CryptoException, InvalidArgumentException, TransactionException {
//		SampleOrg sampleOrg = this.getSamleOrg();
//		HFClient client = this.getHFClient();
//
//		client.setUserContext(sampleOrg.getUser(TESTUSER_1_NAME));
//		return getChannel(sampleOrg, client);
//
//	}

	private Channel getChannel(SampleOrg sampleOrg, HFClient client)
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, IOException,
			CryptoException, InvalidArgumentException, TransactionException {

		Channel channel = client.newChannel(FOO_CHANNEL_NAME);
		logger.info("Get Chain " + FOO_CHANNEL_NAME);

		//channel.setTransactionWaitTime(clientConfig.getTransactionWaitTime());
		//channel.setDeployWaitTime(clientConfig.getDeployWaitTime());

		// Collection<Peer> channelPeers = new LinkedList<>();
		for (String peerName : sampleOrg.getPeerNames()) {
			String peerLocation = sampleOrg.getPeerLocation(peerName);

			Properties peerProperties = clientConfig.getPeerProperties(peerName);
			if (peerProperties == null) {
				peerProperties = new Properties();
			}
			// Example of setting specific options on grpc's
			// ManagedChannelBuilder
			peerProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
			// channelPeers.add(client.newPeer(peerName, peerLocation,
			// peerProperties));
			channel.addPeer(client.newPeer(peerName, peerLocation, peerProperties));
		}

		Collection<Orderer> orderers = new LinkedList<>();

		for (String orderName : sampleOrg.getOrdererNames()) {
			orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
					clientConfig.getOrdererProperties(orderName)));
		}

		// Just pick the first orderer in the list to create the chain.
		Orderer anOrderer = orderers.iterator().next();
		channel.addOrderer(anOrderer);

		for (String eventHubName : sampleOrg.getEventHubNames()) {
			EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
					clientConfig.getEventHubProperties(eventHubName));
			channel.addEventHub(eventHub);
		}

		if (!channel.isInitialized()) {
			channel.initialize();
		}

		return channel;
	}

	public ChaincodeID getChaincodeID() {
		return ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME).setVersion(CHAIN_CODE_VERSION).setPath(CHAIN_CODE_PATH)
				.build();
	}
}
