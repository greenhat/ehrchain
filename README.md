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
### v 0.1
- [x] naive transaction verification(provider authorship);
- [x] simple block generation/mining(included transactions validity);

### v 0.2
- [ ] patient's inception transaction(registration);
- [ ] provider's inception transaction(registration);

### v 0.3
- [ ] patient grants read/append access to the provider(contract);
- [ ] provider appends medical record to the authorized patient;

### v 0.4
- [ ] patient revokes read access to the provider(contract);
- [ ] patient revokes append access to the provider(contract);

### v 0.5
- [ ] provider reads patient's medical records;
- [ ] patients read their own medical records;

### v 0.6 
- [ ] attach files(transaction valid if all attached files are available locally);

### v 0.5
- [ ] eliminate any Patient <-> Doctor relationship in transaction (zero knowledge proofs);

### v 0.6
- [ ] some EHR should not be accessible by the patient (psychotherapy notes, IP, etc.);
