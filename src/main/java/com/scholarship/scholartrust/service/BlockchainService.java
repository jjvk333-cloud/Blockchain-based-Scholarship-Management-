package com.scholarship.scholartrust.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final String contractAddress;
    private final Credentials credentials;
    private final TransactionManager txManager;
    private final TransactionReceiptProcessor receiptProcessor;

    public BlockchainService(@Value("${blockchain.rpc-url:http://127.0.0.1:8545}") String rpcUrl,
                             @Value("${blockchain.contract-address}") String contractAddress,
                             @Value("${blockchain.admin-private-key}") String privateKey) {
        this.web3j = Web3j.build(new HttpService(rpcUrl));
        this.contractAddress = contractAddress;
        this.credentials = Credentials.create(privateKey);
        this.txManager = new RawTransactionManager(web3j, credentials);
        this.receiptProcessor = new PollingTransactionReceiptProcessor(web3j, 1000, 40);
        log.info("Initialized Web3j connecting to {} with Admin Account {}", rpcUrl, credentials.getAddress());
        log.info("Target ScholarshipLedger contract address: {}", contractAddress);
    }

    public String recordApplicationOnChain(Long applicationId, String studentWallet, Long scholarshipId, String docHash) {
        try {
            Function function = new Function(
                    "recordApplication",
                    Arrays.asList(
                            new Uint256(BigInteger.valueOf(applicationId)),
                            new Address(studentWallet),
                            new Uint256(BigInteger.valueOf(scholarshipId)),
                            new Utf8String(docHash)
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            BigInteger gasLimit = BigInteger.valueOf(300000);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

            String txHash = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, encoded, BigInteger.ZERO).getTransactionHash();
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

            log.info("Blockchain: Application #{} recorded on-chain. TxHash: {}, Block: {}",
                    applicationId, receipt.getTransactionHash(), receipt.getBlockNumber());
            return receipt.getTransactionHash();
        } catch (Exception ex) {
            log.error("Failed to record application on blockchain", ex);
            throw new RuntimeException("Blockchain transaction failed: " + ex.getMessage(), ex);
        }
    }

    public String updateStatusOnChain(Long applicationId, int statusOrdinal) {
        try {
            Function function = new Function(
                    "updateApplicationStatus",
                    Arrays.asList(
                            new Uint256(BigInteger.valueOf(applicationId)),
                            new Uint8(statusOrdinal)
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            BigInteger gasLimit = BigInteger.valueOf(150000);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

            String txHash = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, encoded, BigInteger.ZERO).getTransactionHash();
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

            log.info("Blockchain: Application #{} status updated to ordinal {} on-chain. TxHash: {}",
                    applicationId, statusOrdinal, receipt.getTransactionHash());
            return receipt.getTransactionHash();
        } catch (Exception ex) {
            log.error("Failed to update status on blockchain", ex);
            throw new RuntimeException("Blockchain status update failed: " + ex.getMessage(), ex);
        }
    }

    public TransactionReceipt disburseScholarshipOnChain(Long applicationId, String studentWallet, BigDecimal amount) {
        try {
            BigInteger amountUnits = amount.toBigInteger();

            Function function = new Function(
                    "disburseScholarship",
                    Arrays.asList(
                            new Uint256(BigInteger.valueOf(applicationId)),
                            new Address(studentWallet),
                            new Uint256(amountUnits)
                    ),
                    Collections.emptyList()
            );

            String encoded = FunctionEncoder.encode(function);
            BigInteger gasLimit = BigInteger.valueOf(350000);
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();

            String txHash = txManager.sendTransaction(gasPrice, gasLimit, contractAddress, encoded, BigInteger.ZERO).getTransactionHash();
            TransactionReceipt receipt = receiptProcessor.waitForTransactionReceipt(txHash);

            log.info("Blockchain: Funds disbursed for App #{} to {}. TxHash: {}, Block: {}",
                    applicationId, studentWallet, receipt.getTransactionHash(), receipt.getBlockNumber());
            return receipt;
        } catch (Exception ex) {
            log.error("Failed to disburse scholarship on blockchain", ex);
            throw new RuntimeException("Blockchain disbursement failed: " + ex.getMessage(), ex);
        }
    }

    public boolean verifyHashOnChain(Long applicationId, String docHash) {
        try {
            Function function = new Function(
                    "verifyDocumentHash",
                    Arrays.asList(
                            new Uint256(BigInteger.valueOf(applicationId)),
                            new Utf8String(docHash)
                    ),
                    Arrays.asList(
                            new TypeReference<Bool>() {},
                            new TypeReference<Utf8String>() {}
                    )
            );

            String encoded = FunctionEncoder.encode(function);
            EthCall ethCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> results = FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
            if (results != null && !results.isEmpty()) {
                Bool isMatch = (Bool) results.get(0);
                return isMatch.getValue();
            }
            return false;
        } catch (Exception ex) {
            log.error("Failed to query verifyDocumentHash from blockchain", ex);
            return false;
        }
    }

    public List<Type> getApplicationFromChain(Long applicationId) {
        try {
            Function function = new Function(
                    "getApplication",
                    Collections.singletonList(new Uint256(BigInteger.valueOf(applicationId))),
                    Arrays.asList(
                            new TypeReference<Uint256>() {},
                            new TypeReference<Address>() {},
                            new TypeReference<Uint256>() {},
                            new TypeReference<Utf8String>() {},
                            new TypeReference<Uint8>() {},
                            new TypeReference<Uint256>() {},
                            new TypeReference<Bool>() {}
                    )
            );

            String encoded = FunctionEncoder.encode(function);
            EthCall ethCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            return FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        } catch (Exception ex) {
            log.error("Failed to query getApplication from blockchain", ex);
            return Collections.emptyList();
        }
    }

    public List<Type> getDisbursementFromChain(Long applicationId) {
        try {
            Function function = new Function(
                    "getDisbursement",
                    Collections.singletonList(new Uint256(BigInteger.valueOf(applicationId))),
                    Arrays.asList(
                            new TypeReference<Uint256>() {},
                            new TypeReference<Address>() {},
                            new TypeReference<Uint256>() {},
                            new TypeReference<Uint256>() {},
                            new TypeReference<Bool>() {}
                    )
            );

            String encoded = FunctionEncoder.encode(function);
            EthCall ethCall = web3j.ethCall(
                    Transaction.createEthCallTransaction(credentials.getAddress(), contractAddress, encoded),
                    DefaultBlockParameterName.LATEST
            ).send();

            return FunctionReturnDecoder.decode(ethCall.getValue(), function.getOutputParameters());
        } catch (Exception ex) {
            log.error("Failed to query getDisbursement from blockchain", ex);
            return Collections.emptyList();
        }
    }

    public BigInteger getLatestBlockNumber() {
        try {
            return web3j.ethBlockNumber().send().getBlockNumber();
        } catch (Exception e) {
            return BigInteger.ZERO;
        }
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getAdminAddress() {
        return credentials.getAddress();
    }
}