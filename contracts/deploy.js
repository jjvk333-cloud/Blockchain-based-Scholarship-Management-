const fs = require('fs');
const path = require('path');
const solc = require('solc');
const { Web3 } = require('web3');

async function main() {
    console.log("=== 1. Compiling ScholarshipLedger.sol ===");
    const contractPath = path.resolve(__dirname, 'ScholarshipLedger.sol');
    const source = fs.readFileSync(contractPath, 'utf8');

    const input = {
        language: 'Solidity',
        sources: {
            'ScholarshipLedger.sol': {
                content: source,
            },
        },
        settings: {
            outputSelection: {
                '*': {
                    '*': ['abi', 'evm.bytecode'],
                },
            },
        },
    };

    const output = JSON.parse(solc.compile(JSON.stringify(input)));

    if (output.errors) {
        let hasError = false;
        output.errors.forEach(err => {
            console.error(err.formattedMessage);
            if (err.severity === 'error') hasError = true;
        });
        if (hasError) process.exit(1);
    }

    const compiledContract = output.contracts['ScholarshipLedger.sol']['ScholarshipLedger'];
    const abi = compiledContract.abi;
    const bytecode = compiledContract.evm.bytecode.object;

    console.log("Compilation Successful!");

    // Save artifact
    const artifact = { abi, bytecode };
    fs.writeFileSync(path.resolve(__dirname, 'ScholarshipLedger.json'), JSON.stringify(artifact, null, 2));

    console.log("Saved ABI and Bytecode to contracts/ScholarshipLedger.json");

    // Connect to Ganache
    const rpcUrl = process.env.RPC_URL || 'http://127.0.0.1:8545';
    console.log(`\n=== 2. Connecting to Local Ethereum Node at ${rpcUrl} ===`);
    const web3 = new Web3(rpcUrl);

    const accounts = await web3.eth.getAccounts();
    console.log("Available Accounts in Node:", accounts.length);
    const deployer = accounts[0];
    console.log("Deployer Address:", deployer);

    const balance = await web3.eth.getBalance(deployer);
    console.log("Deployer Balance:", web3.utils.fromWei(balance, 'ether'), "ETH");

    console.log("\n=== 3. Deploying Smart Contract to Local Blockchain ===");
    const contract = new web3.eth.Contract(abi);
    const deployTx = contract.deploy({ data: '0x' + bytecode });

    const gas = await deployTx.estimateGas({ from: deployer });
    console.log("Estimated Gas:", gas.toString());

    const deployedInstance = await deployTx.send({
        from: deployer,
        gas: Math.floor(Number(gas) * 1.5).toString(),
    });

    const contractAddress = deployedInstance.options.address;
    console.log(">>> CONTRACT DEPLOYED SUCCESSFULLY! <<<");
    console.log("Contract Address:", contractAddress);

    // Save deployed address
    const deploymentInfo = {
        network: "ganache-local",
        rpcUrl: rpcUrl,
        contractAddress: contractAddress,
        adminAddress: deployer,
        deployedAt: new Date().toISOString()
    };
    fs.writeFileSync(path.resolve(__dirname, 'deployment.json'), JSON.stringify(deploymentInfo, null, 2));
    console.log("Saved deployment metadata to contracts/deployment.json");

    // Automatically synchronize application.properties
    const appPropsPath = path.resolve(__dirname, '../src/main/resources/application.properties');
    if (fs.existsSync(appPropsPath)) {
        let propsContent = fs.readFileSync(appPropsPath, 'utf8');
        propsContent = propsContent.replace(
            /blockchain\.contract-address=\$\{BLOCKCHAIN_CONTRACT_ADDRESS:0x[a-fA-F0-9]{40}\}/,
            `blockchain.contract-address=\${BLOCKCHAIN_CONTRACT_ADDRESS:${contractAddress}}`
        );
        fs.writeFileSync(appPropsPath, propsContent, 'utf8');
        console.log(`Synchronized application.properties with contract address: ${contractAddress}\n`);
    }

    // Synchronize .env.example
    const envExamplePath = path.resolve(__dirname, '../.env.example');
    if (fs.existsSync(envExamplePath)) {
        let envContent = fs.readFileSync(envExamplePath, 'utf8');
        envContent = envContent.replace(
            /BLOCKCHAIN_CONTRACT_ADDRESS=.*/,
            `BLOCKCHAIN_CONTRACT_ADDRESS=${contractAddress}`
        );
        fs.writeFileSync(envExamplePath, envContent, 'utf8');
        console.log(`Synchronized .env.example with contract address: ${contractAddress}`);
    }
}

main().catch(err => {
    console.error("Deployment failed:", err);
    process.exit(1);
});