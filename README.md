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
- [ ] patient's inception transaction(registration);
- [ ] provider's inception transaction(registration);

### v 3.0
- [ ] patient grants append access to the provider(contract);
- [ ] provider appends medical record to the authorized patient;

### v 4.0
- [ ] patients read their own medical records;

### v 5.0
- [ ] patient grants read access to the provider(contract);
- [ ] provider reads patient's medical records;

### v 6.0
- [ ] patient revokes read access to the provider(contract);
- [ ] patient revokes append access to the provider(contract);

### v 7.0 
- [ ] attach files(transaction valid if all attached files are available locally);

### v 8.0
- [ ] use hierarchical deterministic wallet for secondary key pair generation;

### v 9.0
- [ ] provider should be able to share their access to the patient's records with another provider;

### v 10.0
- [ ] some EHR should not be accessible by the patient (psychotherapy notes, IP, etc.);

### v 11.0 
- [ ] eliminate any Patient <-> Doctor relationship in transaction (zero knowledge proofs);

## Patient registration
Patient starts with master key pair generation. 

Using the hierarchical deterministic wallet (BIP-0032 https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki ) secondary(derived) key pairs are generated to grant access to the providers to append medical records. Provider uses the provided patient's secondary public key to encrypt the appended record.

## Provider registration
Provider starts with master key pair generation. 

## Patient grants append access to the provider
Patient creates a transaction(append-only contract) where puts one of their secondary public key encrypted with a provider's public key, a provider public key and a statement for the access (term, etc.).

## Provider appends a record for the patient
Provider using the patient's public key decrypted with it's own private key from provided in the contract encrypts the appended record and signs the transaction with it's private key.   

## Patient grants read access to their records to a provider
Patient creates a transaction(read-only contract) with their secondary private keys(encrypted with provider's public key) from corresponding secondary public keys given to providers in the past and used to encrypt appended records.

## Read access to patient's records
Select transactions with all the patient's public keys. Check the transaction's signatures to be signed by patient authorized (through contracts) providers. Use corresponding patient private keys to decrypt the records. 

