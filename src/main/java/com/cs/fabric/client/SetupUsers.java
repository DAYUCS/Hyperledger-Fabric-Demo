package com.cs.fabric.client;

import static java.lang.String.format;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import com.cs.fabric.client.utils.ClientHelper;
import com.cs.fabric.sdk.utils.ClientConfig;
import com.cs.fabric.sdk.utils.ClientConfigHelper;
import com.cs.fabric.sdkintegration.SampleOrg;
import com.cs.fabric.sdkintegration.SampleStore;
import com.cs.fabric.sdkintegration.SampleUser;

public class SetupUsers {

	private static final ClientHelper clientHelper = new ClientHelper();
	private static final ClientConfig clientConfig = ClientConfig.getConfig();
	private static final String TEST_ADMIN_NAME = "admin";
	private static final String TESTUSER_1_NAME = "user1";

	private static final Log logger = LogFactory.getLog(SetupUsers.class);

	public static void main(String[] args) throws Exception {

		ClientConfigHelper configHelper = new ClientConfigHelper();
		configHelper.clearConfig();
		configHelper.customizeConfig();

		// Get Org1
		SampleOrg sampleOrg = clientConfig.getIntegrationTestsSampleOrg("peerOrg1");
		sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));

		////////////////////////////
		// Set up USERS

		// Persistence is not part of SDK. Sample file store is for
		// demonstration purposes only!
		// MUST be replaced with more robust application implementation
		// (Database, LDAP)
		File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
		if (sampleStoreFile.exists()) { // For testing start fresh
			sampleStoreFile.delete();
			logger.info("delete users store file.");
		}

		final SampleStore sampleStore = new SampleStore(sampleStoreFile);
		// sampleStoreFile.deleteOnExit();

		// SampleUser can be any implementation that implements
		// org.hyperledger.fabric.sdk.User Interface

		////////////////////////////
		// get users for Org1

		HFCAClient ca = sampleOrg.getCAClient();
		final String mspid = sampleOrg.getMSPID();
		ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
		SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, sampleOrg.getName());
		if (!admin.isEnrolled()) { // Preregistered admin only needs to be
									// enrolled with Fabric caClient.
			admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
			admin.setMspId(mspid);
		}

		sampleOrg.setAdmin(admin); // The admin of this org --
		logger.info("Set sampleOrg admin");

		SampleUser user = sampleStore.getMember(TESTUSER_1_NAME, sampleOrg.getName());
		if (!user.isRegistered()) { // users need to be registered AND enrolled
			RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
			user.setEnrollmentSecret(ca.register(rr, admin));
		}
		if (!user.isEnrolled()) {
			user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
			user.setMspId(mspid);
		}
		sampleOrg.addUser(user); // Remember user belongs to this Org
		logger.info("Set sampleOrg user");

		clientHelper.setPeerAdmin(sampleStore, sampleOrg);
		logger.info("Set peer admin");
		

        final String sampleOrgName = sampleOrg.getName();
        final String sampleOrgDomainName = sampleOrg.getDomainName();

        // src/test/fixture/sdkintegration/e2e-2Orgs/channel/crypto-config/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp/keystore/
        SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                ClientHelper.findFile_sk(Paths.get(clientConfig.getTestChannelPath(), "crypto-config/peerOrganizations/",
                        sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                Paths.get(clientConfig.getTestChannelPath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                        format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

        sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can create channels, join peers and install chaincode

		logger.info("Set up users for Org1. OK!");
	}

}
