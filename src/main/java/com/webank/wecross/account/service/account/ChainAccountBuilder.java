package com.webank.wecross.account.service.account;

import com.webank.wecross.account.service.authentication.packet.AddChainAccountRequest;
import com.webank.wecross.account.service.config.Default;
import com.webank.wecross.account.service.db.ChainAccountTableBean;
import com.webank.wecross.account.service.exception.AccountManagerException;
import com.webank.wecross.account.service.exception.AddChainAccountException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChainAccountBuilder {
    private static Logger logger = LoggerFactory.getLogger(ChainAccountBuilder.class);

    private static final Pattern CERT_PATTERN =
            Pattern.compile(
                    "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+"
                            + // Header
                            "([A-Za-z0-9+/=\\r\\n]+)"
                            + // Base64 text
                            "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern SEC_KEY_PATTERN =
            Pattern.compile(
                    "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+"
                            + // Header
                            "([A-Za-z0-9+/=\\r\\n]+)"
                            + // Base64 text
                            "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PUB_KEY_PATTERN =
            Pattern.compile(
                    "-+BEGIN\\s+.*PUBLIC\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+"
                            + // Header
                            "([A-Za-z0-9+/=\\r\\n]+)"
                            + // Base64 text
                            "-+END\\s+.*PUBLIC\\s+KEY[^-]*-+", // Footer
                    Pattern.CASE_INSENSITIVE);

    public static ChainAccount buildFromTableBean(ChainAccountTableBean tableBean)
            throws AccountManagerException {
        ChainAccount account = new ChainAccount();
        account.setId(tableBean.getId());
        account.setType(tableBean.getType());
        account.setKeyID(tableBean.getKeyID());
        account.setIdentity(tableBean.getIdentity());
        account.setUsername(tableBean.getUsername());
        account.setDefault(tableBean.isDefault());

        account.setPubKey(tableBean.getPub());
        account.setSecKey(tableBean.getSec());
        account.setExt0(tableBean.getExt0());
        account.setExt1(tableBean.getExt1());
        account.setExt2(tableBean.getExt2());
        account.setExt3(tableBean.getExt3());

        return account;
    }

    public static ChainAccount buildFromRequest(AddChainAccountRequest request, String username)
            throws AddChainAccountException {
        ChainAccount account = new ChainAccount();
        account.setUsername(username);
        account.setType(request.getType());
        account.setPubKey(request.getPubKey());
        account.setSecKey(request.getSecKey());
        account.setExt0(request.getExt());
        account.setDefault(request.getIsDefault());

        checkSecKey(account.getSecKey());

        String type = request.getType();
        switch (type) {
            case Default.BCOS_STUB_TYPE:
                checkPubKey(request.getPubKey());
                checkAddressFormat(request.getExt());
                account.setIdentity(request.getExt());
                break;
            case Default.BCOS_GM_STUB_TYPE:
                checkPubKey(request.getPubKey());
                checkAddressFormat(request.getExt());
                account.setIdentity(request.getExt());
                break;
            case Default.FABRIC_STUB_TYPE:
                checkCertificatePem(request.getPubKey());
                checkMSPID(request.getExt());
                account.setIdentity(request.getPubKey());
                break;
            default:
                logger.error("request unkown ChainAccount type: " + type);
                throw new AddChainAccountException("Unkown ChainAccount type: " + type);
        }

        return account;
    }

    private static void checkSecKey(String key) throws AddChainAccountException {
        if (!SEC_KEY_PATTERN.matcher(key).find()) {
            throw new AddChainAccountException("Invalid secret key:" + key);
        }
    }

    private static void checkPubKey(String key) throws AddChainAccountException {
        if (!PUB_KEY_PATTERN.matcher(key).find()) {
            throw new AddChainAccountException("Invalid pub key:" + key);
        }
    }

    private static void checkCertificatePem(String content) throws AddChainAccountException {
        if (!CERT_PATTERN.matcher(content).find()) {
            throw new AddChainAccountException("Invalid certificate file:" + content);
        }
    }

    private static void checkAddressFormat(String address) throws AddChainAccountException {
        if (!address.contains("0x") || address.length() != 42) {
            throw new AddChainAccountException(
                    "Invalid address format, address must start with \"0x\" and with 42 characters");
        }
    }

    private static void checkMSPID(String mspID) throws AddChainAccountException {
        if (mspID == null || mspID.length() == 0) {
            throw new AddChainAccountException("MSPID is empty!");
        }
    }
}
