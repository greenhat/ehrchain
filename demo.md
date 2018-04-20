# EHRChain Demo
Simulates a scenario where patient's records are created by provider A and then read by the provider B. Three nodes(instances) are launched, one for each actor. They exchange transactions and blocks maintaining a blockchain(consensus).

## Actors
### Patient 
Creates an append contract for provider A to grant the rights to create records.
Creates a read contract for provider B to grant read access to the patient's records added by provider A.

### Provider A 
Creates a new record for the patient every X seconds by creating record transaction.

### Provider B 
Reads and decrypts patient's records every X seconds.

## How to run this demo
### Requirements
To run 256-bit encryption you need to download and install 'Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files 6' from [JCE](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html)

### Run
Open three shell sessions pointing to source root folder and run the following commands (each in its own shell):

`sbt "runMain ehr.EhrApp src/main/resources/settingsPatient.conf patient"`

`sbt "runMain ehr.EhrApp src/main/resources/settingsProviderA.conf providerA"`

`sbt "runMain ehr.EhrApp src/main/resources/settingsProviderB.conf providerB"`

Check the logs to see generated transactions and blocks synced to all participants.  
See [sources](https://github.com/greenhat/ehrchain/tree/master/src/main/scala/ehr/demo) for the designated behaviour for each role. 
