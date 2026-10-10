const { Web3 } = require('web3');
const fs = require('fs');
const path = require('path');

async function test() {
    const deployment = JSON.parse(fs.readFileSync(path.resolve(__dirname, 'deployment.json'), 'utf8'));
    const artifact = JSON.parse(fs.readFileSync(path.resolve(__dirname, 'ScholarshipLedger.json'), 'utf8'));

    const web3 = new Web3(deployment.rpcUrl);
    const contract = new web3.eth.Contract(artifact.abi, deployment.contractAddress);
    const admin = deployment.adminAddress;

    console.log("=== 1. Blockchain Connection Status ===");
    console.log("RPC URL:", deployment.rpcUrl);
    console.log("Contract Address:", deployment.contractAddress);
    console.log("Current Block Number:", (await web3.eth.getBlockNumber()).toString());

    console.log("\n=== 2. Recording Scholarship Application on Smart Contract ===");
    const appId = Math.floor(Date.now() / 1000);
    const studentWallet = "0x71C7656EC7ab88b098defB751B7401B5f6d8976F";
    const scholarshipId = 1;
    const docHash = "ccd4ada9899fb66011fbc5b444b5477cfbb7b1ef833ccc5ebdacd0e4835b3f94";

    const recordTx = await contract.methods.recordApplication(appId, studentWallet, scholarshipId, docHash).send({
        from: admin,
        gas: '300000'
    });
    console.log("Transaction Hash:", recordTx.transactionHash);
    console.log("Block Number:", recordTx.blockNumber.toString());
    console.log("Gas Used:", recordTx.gasUsed.toString());

    console.log("\n=== 3. Querying Immutable On-Chain Record ===");
    const appData = await contract.methods.getApplication(appId).call();
    console.log("On-Chain Application ID:", appData.applicationId.toString());
    console.log("Student Wallet:", appData.studentAddress);
    console.log("Anchored SHA-256 Hash:", appData.documentHash);
    console.log("Status Ordinal:", appData.status.toString(), "(0 = SUBMITTED)");
    console.log("Timestamp:", new Date(Number(appData.timestamp) * 1000).toLocaleString());

    console.log("\n=== 4. Smart Contract Cryptographic Verification ===");
    const matchCheck = await contract.methods.verifyDocumentHash(appId, docHash).call();
    console.log("Authentic Document Hash Check -> isMatch:", matchCheck.isMatch);

    const fakeHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    const tamperedCheck = await contract.methods.verifyDocumentHash(appId, fakeHash).call();
    console.log("Tampered Document Hash Check -> isMatch:", tamperedCheck.isMatch);

    console.log("\n=== 5. Testing Strict State Machine Transitions ===");
    // Valid: 0 (SUBMITTED) -> 1 (UNDER_REVIEW)
    const reviewTx = await contract.methods.updateApplicationStatus(appId, 1).send({
        from: admin,
        gas: '150000'
    });
    console.log("Transited to UNDER_REVIEW (Ordinal 1). Tx:", reviewTx.transactionHash);

    // Invalid transition test: 1 (UNDER_REVIEW) -> 0 (SUBMITTED) must REVERT
    try {
        await contract.methods.updateApplicationStatus(appId, 0).send({
            from: admin,
            gas: '150000'
        });
        console.error("FAIL: State machine allowed illegal transition back to SUBMITTED!");
        process.exit(1);
    } catch (e) {
        console.log("PASS: Invalid transition back to SUBMITTED reverted as expected.");
    }

    // Valid: 1 (UNDER_REVIEW) -> 2 (APPROVED)
    const approveTx = await contract.methods.updateApplicationStatus(appId, 2).send({
        from: admin,
        gas: '150000'
    });
    console.log("Approval Tx Hash (Transited to APPROVED):", approveTx.transactionHash);

    console.log("\n=== 6. Simulating On-Chain Scholarship Disbursement ===");
    const grantAmount = 50000;
    const disburseTx = await contract.methods.disburseScholarship(appId, studentWallet, grantAmount).send({
        from: admin,
        gas: '300000'
    });
    console.log("Disbursement Tx Hash:", disburseTx.transactionHash);
    console.log("Disbursement Block:", disburseTx.blockNumber.toString());

    const disbData = await contract.methods.getDisbursement(appId).call();
    console.log("Disbursement Verified on Ledger!");
    console.log(" - Recipient:", disbData.studentAddress);
    console.log(" - Amount: ₹" + disbData.amount.toString());
    console.log(" - Timestamp:", new Date(Number(disbData.timestamp) * 1000).toLocaleString());
}

test().catch(err => {
    console.error("Test error:", err);
    process.exit(1);
});