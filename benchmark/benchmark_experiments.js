const { Web3 } = require('web3');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

async function runBenchmark(numTrials = 20) {
    const deploymentPath = path.resolve(__dirname, '../contracts/deployment.json');
    const artifactPath = path.resolve(__dirname, '../contracts/ScholarshipLedger.json');

    if (!fs.existsSync(deploymentPath) || !fs.existsSync(artifactPath)) {
        console.error("Missing deployment or artifact file!");
        process.exit(1);
    }

    const deployment = JSON.parse(fs.readFileSync(deploymentPath, 'utf8'));
    const artifact = JSON.parse(fs.readFileSync(artifactPath, 'utf8'));

    const web3 = new Web3(deployment.rpcUrl);
    const contract = new web3.eth.Contract(artifact.abi, deployment.contractAddress);
    const admin = deployment.adminAddress;

    console.log(`====================================================`);
    console.log(` ScholarTrust Empirical Benchmark Suite (N = ${numTrials}) `);
    console.log(`====================================================`);
    console.log(`Contract: ${deployment.contractAddress}`);
    console.log(`RPC Node: ${deployment.rpcUrl}`);
    console.log(`----------------------------------------------------\n`);

    const results = [];
    const baseAppId = 2000 + Math.floor(Math.random() * 1000);

    for (let i = 0; i < numTrials; i++) {
        const appId = baseAppId + i;
        const studentWallet = web3.eth.accounts.create().address;
        const dummyDocContent = `ScholarTrust Academic Benchmark File Payload Trial #${i} - Timestamp ${Date.now()}`;
        
        // 1. Measure SHA-256 computation latency
        const hashStart = process.hrtime.bigint();
        const docHash = crypto.createHash('sha256').update(dummyDocContent).digest('hex');
        const hashEnd = process.hrtime.bigint();
        const hashTimeMs = Number(hashEnd - hashStart) / 1e6;

        // 2. Measure Record Application Tx Latency & Gas
        const recStart = process.hrtime.bigint();
        const recTx = await contract.methods.recordApplication(appId, studentWallet, 1, docHash).send({
            from: admin,
            gas: '300000'
        });
        const recEnd = process.hrtime.bigint();
        const recLatencyMs = Number(recEnd - recStart) / 1e6;
        const recGas = Number(recTx.gasUsed);

        // 3. Measure Document Integrity Verification Call Latency
        const verifyStart = process.hrtime.bigint();
        const verifyResult = await contract.methods.verifyDocumentHash(appId, docHash).call();
        const verifyEnd = process.hrtime.bigint();
        const verifyLatencyMs = Number(verifyEnd - verifyStart) / 1e6;

        // 4. Update Status to APPROVED (required by state machine)
        await contract.methods.updateApplicationStatus(appId, 2).send({
            from: admin,
            gas: '150000'
        });

        // 5. Measure Disbursement Tx Latency & Gas
        const disbStart = process.hrtime.bigint();
        const disbTx = await contract.methods.disburseScholarship(appId, studentWallet, 25000).send({
            from: admin,
            gas: '300000'
        });
        const disbEnd = process.hrtime.bigint();
        const disbLatencyMs = Number(disbEnd - disbStart) / 1e6;
        const disbGas = Number(disbTx.gasUsed);

        results.push({
            trial: i + 1,
            appId,
            hashTimeMs,
            recLatencyMs,
            recGas,
            verifyLatencyMs,
            verifyMatch: verifyResult.isMatch,
            disbLatencyMs,
            disbGas
        });

        process.stdout.write(`.` );
    }

    console.log("\n\nAll trials executed successfully!\n");

    // Compute Metrics: Mean, Min, Max, Standard Deviation
    function calcStats(arr) {
        const n = arr.length;
        const mean = arr.reduce((a, b) => a + b, 0) / n;
        const variance = arr.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / (n - 1 || 1);
        const stdDev = Math.sqrt(variance);
        const min = Math.min(...arr);
        const max = Math.max(...arr);
        return { mean, stdDev, min, max };
    }

    const recLatStats = calcStats(results.map(r => r.recLatencyMs));
    const recGasStats = calcStats(results.map(r => r.recGas));
    const verifyLatStats = calcStats(results.map(r => r.verifyLatencyMs));
    const disbLatStats = calcStats(results.map(r => r.disbLatencyMs));
    const disbGasStats = calcStats(results.map(r => r.disbGas));
    const hashStats = calcStats(results.map(r => r.hashTimeMs));

    console.log("=== EMPIRICAL EVALUATION SUMMARY FOR CONFERENCE PAPER ===");
    console.log(`1. Document SHA-256 Computation Time:`);
    console.log(`   Mean: ${hashStats.mean.toFixed(4)} ms | StdDev: ${hashStats.stdDev.toFixed(4)} ms`);
    console.log(`2. Record Application Transaction:`);
    console.log(`   Latency Mean: ${recLatStats.mean.toFixed(2)} ms (StdDev: ${recLatStats.stdDev.toFixed(2)} ms)`);
    console.log(`   Gas Consumption: ${recGasStats.mean.toFixed(0)} units (Min: ${recGasStats.min}, Max: ${recGasStats.max})`);
    console.log(`3. Integrity Verification Query:`);
    console.log(`   Call Latency Mean: ${verifyLatStats.mean.toFixed(2)} ms (StdDev: ${verifyLatStats.stdDev.toFixed(2)} ms)`);
    console.log(`4. Scholarship Disbursement Transaction:`);
    console.log(`   Latency Mean: ${disbLatStats.mean.toFixed(2)} ms (StdDev: ${disbLatStats.stdDev.toFixed(2)} ms)`);
    console.log(`   Gas Consumption: ${disbGasStats.mean.toFixed(0)} units (Min: ${disbGasStats.min}, Max: ${disbGasStats.max})`);
    console.log(`==========================================================\n`);

    // Output CSV for Academic Charting
    const csvHeader = "Trial,AppId,Sha256TimeMs,RecordTxLatencyMs,RecordGasUsed,VerifyLatencyMs,DisburseTxLatencyMs,DisburseGasUsed\n";
    const csvRows = results.map(r => 
        `${r.trial},${r.appId},${r.hashTimeMs.toFixed(4)},${r.recLatencyMs.toFixed(2)},${r.recGas},${r.verifyLatencyMs.toFixed(2)},${r.disbLatencyMs.toFixed(2)},${r.disbGas}`
    ).join("\n");

    const csvPath = path.resolve(__dirname, 'benchmark_results.csv');
    fs.writeFileSync(csvPath, csvHeader + csvRows, 'utf8');
    console.log(`Raw trial data exported to: ${csvPath}`);
}

const trials = process.argv[2] ? parseInt(process.argv[2], 10) : 15;
runBenchmark(trials).catch(err => {
    console.error("Benchmark error:", err);
    process.exit(1);
});
