// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title ScholarshipLedger
 * @dev Manages immutable on-chain records of scholarship applications,
 * cryptographic document hashes, status transitions, and simulated disbursement receipts.
 */
contract ScholarshipLedger {

    address public admin;

    enum Status { SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED, DISBURSED }

    struct ApplicationRecord {
        uint256 applicationId;
        address studentAddress;
        uint256 scholarshipId;
        string documentHash;
        Status status;
        uint256 timestamp;
        bool exists;
    }

    struct DisbursementRecord {
        uint256 applicationId;
        address studentAddress;
        uint256 amount;
        uint256 timestamp;
        bool exists;
    }

    // applicationId => ApplicationRecord
    mapping(uint256 => ApplicationRecord) public applications;

    // applicationId => DisbursementRecord
    mapping(uint256 => DisbursementRecord) public disbursements;

    // Student address => list of applicationIds
    mapping(address => uint256[]) public studentApplications;

    // Events for off-chain listeners and transparency
    event ApplicationRecorded(
        uint256 indexed applicationId,
        address indexed studentAddress,
        uint256 indexed scholarshipId,
        string documentHash,
        uint256 timestamp
    );

    event StatusUpdated(
        uint256 indexed applicationId,
        Status status,
        uint256 timestamp
    );

    event FundsDisbursed(
        uint256 indexed applicationId,
        address indexed studentAddress,
        uint256 amount,
        uint256 timestamp
    );

    modifier onlyAdmin() {
        require(msg.sender == admin, "Only admin can perform this operation");
        _;
    }

    constructor() {
        admin = msg.sender;
    }

    /**
     * @notice Records a newly submitted scholarship application and anchors its SHA-256 document hash.
     */
    function recordApplication(
        uint256 _applicationId,
        address _studentAddress,
        uint256 _scholarshipId,
        string memory _documentHash
    ) external onlyAdmin {
        require(!applications[_applicationId].exists, "Application already recorded on-chain");
        require(_studentAddress != address(0), "Invalid student address");
        require(bytes(_documentHash).length > 0, "Document hash cannot be empty");

        applications[_applicationId] = ApplicationRecord({
            applicationId: _applicationId,
            studentAddress: _studentAddress,
            scholarshipId: _scholarshipId,
            documentHash: _documentHash,
            status: Status.SUBMITTED,
            timestamp: block.timestamp,
            exists: true
        });

        studentApplications[_studentAddress].push(_applicationId);

        emit ApplicationRecorded(
            _applicationId,
            _studentAddress,
            _scholarshipId,
            _documentHash,
            block.timestamp
        );
    }

    /**
     * @notice Updates the verification / approval status on-chain.
     * Enforces a strict state machine:
     * - SUBMITTED -> UNDER_REVIEW
     * - UNDER_REVIEW -> APPROVED or REJECTED
     * Any other transition (or transition from DISBURSED / REJECTED) is strictly reverted.
     */
    function updateApplicationStatus(
        uint256 _applicationId,
        uint8 _status
    ) external onlyAdmin {
        require(applications[_applicationId].exists, "Application does not exist");
        require(_status <= uint8(Status.REJECTED), "Invalid target status: use disburseScholarship for DISBURSED");

        Status current = applications[_applicationId].status;
        Status target = Status(_status);

        if (current == Status.SUBMITTED) {
            require(target == Status.UNDER_REVIEW, "State Machine Violation: SUBMITTED can only transition to UNDER_REVIEW");
        } else if (current == Status.UNDER_REVIEW) {
            require(target == Status.APPROVED || target == Status.REJECTED, "State Machine Violation: UNDER_REVIEW can only transition to APPROVED or REJECTED");
        } else {
            revert("State Machine Violation: Cannot change status of an already APPROVED, REJECTED, or DISBURSED application");
        }

        applications[_applicationId].status = target;

        emit StatusUpdated(_applicationId, target, block.timestamp);
    }

    /**
     * @notice Simulates scholarship disbursement to the student's Ethereum address.
     * Enforces that an application must be APPROVED before disbursement.
     */
    function disburseScholarship(
        uint256 _applicationId,
        address _studentAddress,
        uint256 _amount
    ) external onlyAdmin {
        require(applications[_applicationId].exists, "Application does not exist");
        require(applications[_applicationId].status == Status.APPROVED, "Application must be APPROVED before disbursement");
        require(!disbursements[_applicationId].exists, "Scholarship already disbursed for this application");
        require(applications[_applicationId].studentAddress == _studentAddress, "Recipient address mismatch");

        applications[_applicationId].status = Status.DISBURSED;

        disbursements[_applicationId] = DisbursementRecord({
            applicationId: _applicationId,
            studentAddress: _studentAddress,
            amount: _amount,
            timestamp: block.timestamp,
            exists: true
        });

        emit FundsDisbursed(_applicationId, _studentAddress, _amount, block.timestamp);
        emit StatusUpdated(_applicationId, Status.DISBURSED, block.timestamp);
    }

    /**
     * @notice Verifies if a given document hash matches the immutable hash on-chain.
     */
    function verifyDocumentHash(
        uint256 _applicationId,
        string memory _docHash
    ) external view returns (bool isMatch, string memory storedHash) {
        require(applications[_applicationId].exists, "Application does not exist");
        string memory recorded = applications[_applicationId].documentHash;
        return (keccak256(bytes(recorded)) == keccak256(bytes(_docHash)), recorded);
    }

    /**
     * @notice Returns full application record.
     */
    function getApplication(uint256 _applicationId) external view returns (
        uint256 applicationId,
        address studentAddress,
        uint256 scholarshipId,
        string memory documentHash,
        uint8 status,
        uint256 timestamp,
        bool exists
    ) {
        ApplicationRecord memory app = applications[_applicationId];
        return (
            app.applicationId,
            app.studentAddress,
            app.scholarshipId,
            app.documentHash,
            uint8(app.status),
            app.timestamp,
            app.exists
        );
    }

    /**
     * @notice Returns disbursement record.
     */
    function getDisbursement(uint256 _applicationId) external view returns (
        uint256 applicationId,
        address studentAddress,
        uint256 amount,
        uint256 timestamp,
        bool exists
    ) {
        DisbursementRecord memory d = disbursements[_applicationId];
        return (
            d.applicationId,
            d.studentAddress,
            d.amount,
            d.timestamp,
            d.exists
        );
    }
}