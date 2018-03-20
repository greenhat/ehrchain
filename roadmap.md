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
- [x] patient revokes append access for the provider(contract);

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

