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
- [ ] run three instances with demo scenario: patient (makes append and read contracts), provider1 (makes record tx), provider2 (reads the record) scripted with actors;

### v 9.0
- [ ] expose record creation/access via the REST API (integration with provider's EMR);

### v 10.0
- [ ] run demo scenario via the REST API?

### v X.0
- [ ] lightweight client proof-of-concept for mobile (contracts management, record access);
- [ ] use hierarchical deterministic wallet for key pair generation;
- [ ] provider should be able to share their access to the patient's records with another provider;
- [ ] some patient's records should not be accessible by the patient (psychotherapy notes, IP, etc.);
- [ ] eliminate any Patient <-> Doctor relationship in transactions (zero knowledge proofs);

