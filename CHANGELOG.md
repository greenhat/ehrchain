## Changelog
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
- [x] requesting/retrieving files attached to record transactions;

### v 8.0
- [x] run three instances with demo scenario: patient (makes append and read contracts), provider1 (makes record tx), provider2 (reads the record) scripted with actors;
