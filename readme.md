## Introduction 

This is a Maven Java project built with Eclipse 4.32.0

This project exposes a change in the smbj library's functionality after version 0.10.0. The change is related to the default SMB dialect version. This change is causing the SMB Connector (mule-smb-connector) to not be backwards compatible, and is a breaking change.

### Problem for mule-smb-connector

The SMB Connector for Mulesoft, com.mulesoft.connectors.mule-smb-connector version 4.0.0, utilizes smbj version 0.12.2, which contains a change in the smbj library's SMB dialect version when setting up a new SMBClient. Version 0.10.0 and prior had a default SMB Dialect version of SMB2Dialect.SMB_2_1. After 0.10.0, the smbj library seems to use SMB2Dialect.SMB_3_1_1.

Anybody using mule-smb-connector that is using SMB 2.1 shares will not be able to upgrade to mule-smb-connector 4.0.0 as they will receive the error: 

```
com.hierynomus.mssmb2.SMBApiException: STATUS_ACCESS_DENIED (0xc0000022): Could not connect to \\host\share
```

In order to upgrade our Mulesoft projects to use Java 17, we are required to use mule-smb-connector 4.0.0. The breaking change means we are not able to upgrade our projects unless we use custom code or build our own connector. While I have already built an SMB Connector that can replace yours, I would prefer to use a Mulesoft approved vendor supported connector.

### Solution

At a high level, the solution for mule-smb-connector is to provide a configuration that allows us to pick the dialect version, namely 2.1 and 3.1.1.

I have code in this project that shows you how to pick the dialect version with smbj 0.12.0. You can view this code in the file: src/main/java/ucsdh/SMBUtilities.java, in the Initialize() function.

Below is the snippet of code that will provide your fix:

```java
SMB2Dialect [] supportedSmdDialects = {
    //SMB2Dialect.SMB_2_1 // LOOK HERE
	SMB2Dialect.SMB_3_1_1 // LOOK HERE
	};
	SmbConfig cfg = SmbConfig.builder().
	withDialects(supportedSmdDialects).// LOOK HERE
	withMultiProtocolNegotiate(true).
	            //withSigningRequired(false).
	            build();
	            
this._smbClient = new SMBClient(cfg); // above config added to the SMBClient constructor
```

## Getting Setup
Maven Java project built with Eclipse 4.32.0 and JUnit

GitHub Repository: https://github.com/rjborges/smbj-dialect-test


### SMB Settings

In order to reproduce the problem, you will need to setup SMB shares that utilize dialects 2.1 and 3.1.1, separately.

You can setup your host, username, password, domain, and sharename in the JUnit file src/test/java/AllTests.java, in the Setup() function.


### SMB Dialect Version

The SMB dialect versions to use, and how to use them, are described in the Solution section above. 

## Run with JUnit

JUnit testing is already setup at the file src/test/java/AllTests.java. You can simply run any or all of the unit tests to reproduce this issue.

You will need to setup a JUnit build profile so you can run/debug using JUnit. There are no special arguments to place into the profile.

