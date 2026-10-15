#include "CNetwork.h"
#include <stdint.h>

#include "util/CJavaWrapper.h"

const char* g_szServerNames[MAX_SERVERS] = {
	"LUX RUSSIA | RED",
	"LUX RUSSIA | TEST"
};

const CSetServer::CServerInstanceEncrypted g_sEncryptedAddresses[MAX_SERVERS] = {
	CSetServer::create("172.96.140.62", 1, 16, 5441, false),
	CSetServer::create("172.96.140.62", 1, 16, 5441, false)	
};