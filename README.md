# EHRChain
Proof-of-Concept for storing health records in a public blockchain.

## Goal
Store EHR (Electronic Health Records) in the public blockchain. Patients and authorized health care providers should have access to the records at any time. 

## Benefits
For patients:
- All their medical records are in one place and shareable with any health care provider(other organizations);
- Full control of who can read and update their records;
- Persistence. Records cannot be lost.

For health care providers:
- All medical records from all health care providers for the patient are available in one place;
- Eliminated security risks of loosing/leaking of patient's medical records;

## Overview
Patient authorizes a provider for record creation via a contract (see append contract) on the blockchain. Records stored in the blockchain are encrypted with [ECDH](https://en.wikipedia.org/wiki/Elliptic-curve_Diffie–Hellman) derived keys so that both patient and provider who made them can decrypt them. A patient can securely share these keys with any provider via contract (see read contract) on the blockchain.

Encrypted record files are stored on each full node as a part of the blockchain. Any organization with enough resources can run a full node. The full node exposes all available operations through the REST API for integration with existing systems (EMR, etc.). 

Patients use lightweight node (as a mobile app, etc.) to access their records without any gateway server, directly from the blockchain. 

## Roadmap
See [roadmap](roadmap.md)

## Consensus protocol
Proof-of-work.

## Transaction validation
### Semantic validity 
#### Signature 
The originator of the transaction makes a signature of the transaction with their private key and includes it in the transaction ([src](src/main/scala/ehr/transaction/EhrTransaction.scala#L17)).
### Generic validity
#### Record transaction authorization
For each record transaction, a valid append contract issued by the same patient must be active (not expired or revoked) ([src](src/main/scala/ehr/transaction/RecordTransactionContractValidator.scala#L7)).
#### Record transaction included record files verification
For each record transaction, all record files must be accessible by the local system. The file authenticity if verified with hash included in the transaction ([src](src/main/scala/ehr/transaction/RecordTransactionFileValidator.scala#L8)).

## Block validation
### Semantic validity
#### Signature
The generator of the block (`generator` property with their public key) makes a signature of the block with their private key and includes it as `signature` in the block. See `EhrBlock.validity`.
#### Proof-of-work
Each block references the previous block and must have such `nonce` value, so hash of the block has X leading zeroes (determined by current difficulty). See `EhrBlock.powValidity` and `EhrBlock.generate`.

## Patient and providers registration
Starts with a key pair generation. Since it is the core authentication mechanism, it's crucial not to lose them. Using the hierarchical deterministic wallet [BIP-0032](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki) generate public/private key pair(s) from a seed phrase. Seed phrase should be stored in the safe place. The same key pair can be generated from the seed phrase at any time.

## Patient grants append access to the provider
Patient creates a contract transaction(append contract) where puts their public key, a provider's public key and a statement for the access properties (term, etc.). See `AppendContract`.

## Provider appends a record for the patient
Provider creates an AES-256 key by getting SHA-256 digest from the following items:
 - shared secret using [ECDH](https://en.wikipedia.org/wiki/Elliptic-curve_Diffie–Hellman) from provider's private key and patient's public key;
 - the patient's public key;
 - provider's public key;
 
This key is used to encrypt each record file. The hash of the encrypted record file is put in the record transaction (see `RecordTransaction`).
Provider creates a transaction and signs it with its private key. 

The transaction is valid only if an append contract for this patient's public key exists in the blockchain and is active. Contract is active if it's terms are valid and there is no revocation contract further in the blockchain that cancels it. Must be checked by a node mining a block as a part of transaction validity check. See `RecordTransactionContractValidator`.

## Patient grants read access to their records to a provider
Patient creates a contract transaction with read contract (see `ContractTransaction` with `ReadContract`) with all AES-256 keys used in record transactions (record keys). For each record key provider's public key is stored to identify which AES-256 key to use for the particular encrypted record. Record keys are encrypted with an AES-256 key derived by getting SHA-256 digest from:
 - shared secret using [ECDH](https://en.wikipedia.org/wiki/Elliptic-curve_Diffie–Hellman) from patient's private key and provider's public key;
 - the patient's public key;
 - provider's public key;
 
This method gives provider access to the records created within existing append contracts with providers. See `AccessRecordsSpec`. 

When new provider gets append contract, the patient should create new read contracts to give existing providers access to the records that would be created by this new provider.

## Provider reads patient's records
Provider finds read contracts(transactions) with its own public key and decrypts included record keys with an AES-256 key derived by getting SHA-256 digest from:
 - shared secret using [ECDH](https://en.wikipedia.org/wiki/Elliptic-curve_Diffie–Hellman) from provider's private key and patient's public key;
 - provider's public key;
 - the patient's public key;
Each patient's record file is then can be decrypted with the appropriate record key found by provider's public key from record transaction. See `AccessRecordsSpec`.

## Patient revokes append contract
Patient creates a revoke append contract transaction where includes a provider which invalidates all active append contracts between this patient and this provider.
