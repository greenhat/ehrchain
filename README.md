# EHRChain
Proof-of-Concept of storing EHR in a blockchain.

## Goal
Store EHR (Electronic Health Records) in the public blockchain. The patients are in control who can read and update their medical records.

## Benefits for patients:
- All their medical records are in one place and shareable with any authorized health care provider;
- Full control of who can read and update their records;
- Persistence. Inability to loose data;

## Benefits for health care providers:
- All medical records from all health care providers for the patient are in one place;
- Eliminated security risks of loosing/leaking of patient's medical records;

## Roadmap
### v 1.0
- [x] naive transaction verification(provider authorship);
- [x] simple block generation/mining(included transactions validity);

### v 2.0
- [x] patient grants append access to the provider(contract);
- [x] provider appends medical record to the authorized patient;

### v 3.0
- [x] record metadata format, attached files(transaction valid if all attached files are available locally);

### v 4.0
- [x] record file encryption;
- [x] record file hash;
- [x] patients read their own medical records;

### v 5.0
- [x] patient grants read access to the provider(contract);
- [x] provider reads patient's medical records;

### v 6.0
- [ ] patient revokes append access to the provider(contract);

### v 7.0 
- [ ] requesting/retrieving files attached to record transactions;

### v 8.0
- [ ] deployable testnet(few nodes with a scripted interactions: add/read records);

### v 9.0
- [ ] expose record creation/access via the REST API (integration with provider's EMR);

### v 10.0
- [ ] lightweight client proof-of-concept for mobile (contracts management, record access);

### v 11.0
- [ ] use hierarchical deterministic wallet for patient's secondary key pair generation;

### v 12.0
- [ ] provider should be able to share their access to the patient's records with another provider;

### v 13.0
- [ ] some patient's records should not be accessible by the patient (psychotherapy notes, IP, etc.);

### v 14.0 
- [ ] eliminate any Patient <-> Doctor relationship in transaction (zero knowledge proofs);

## Transaction validation
### Semantic validity 
#### Signature 
Originator of the transaction (`generator` property with their public key) makes a signature of the transaction with their private key and includes it as `signature` in the transaction.
### Generic validity
#### Record transaction authorization
For each record transaction a valid append-only contract must exist. 
#### Record transaction included record files verification
For each record transaction a record file must be accessible by the local system. The file authenticity if verified with hash included in the transaction. 

## Block validation
### Semantic validity
#### Signature
Generator of the block (`generator` property with their public key) makes a signature of the block with their private key and includes it as `signature` in the block.
#### PoW
Each block must have such `nonce` value so hash of the block has X leading zeroes (determined by current difficulty).

## Patient registration
Patient starts with master key pair generation. 
Using the hierarchical deterministic wallet (BIP-0032 https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki ) secondary(derived) key pairs are generated to grant access to the providers to append medical records. Provider uses the provided patient's secondary public key to encrypt the appended record.

## Provider registration
Provider starts with master key pair generation. 

## Patient grants append access to the provider
Patient creates a transaction(append-only contract) where puts one of their secondary public key, a provider's public key and a statement for the access properties (term, etc.). 

## Provider appends a record for the patient
Provider creates a key for AES-256 by getting SHA-256 digest from the following items:
 - shared secret using ECDH from provider's private key and patient's public key;
 - patient's public key;
 - provider's public key;
This key is used to encrypt the record file. The hash of the encrypted record file is put in the record transaction.
Provider creates a transaction and signs it with it's private key. 
Transaction is valid only if if an append-only contract for this patient's public key exists in the blockchain and is active. Contract is active if it's terms are valid and there is no revocation contract further in the blockchain that cancels it. Must be checked by a node mining a block as a part of transaction validity check.

## Patient grants read access to their records to a provider
Patient creates a transaction(read-only contract) with their secondary key pairs (encrypted with provider's public key) from corresponding secondary public keys given to providers in the past and used to encrypt appended records. Most likely patient should give all their secondary private keys in case it's desirable for the provider to have access to future patient's records. Patient sign the transaction with one of the included secondary private key.

## Provider reads patient's records
Find a read-only contracts(transaction) with provider's public key. For the sake of anonymity provider have to go through all of them decrypting patient's private keys and fetching all patients records and then selecting the one. 
For a found read-only contract select transactions with all the patient's public keys. Check the transaction's signatures to be signed by authorized (through append-only contracts) providers. Use corresponding patient private keys to decrypt the records. 

## Patient revokes append-only contract
Patient creates a transaction referencing an append-only contract (by transaction id) signed with the same secondary private key that was used in append-only contract.

