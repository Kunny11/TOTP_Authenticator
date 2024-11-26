package com.authenticator.totp;

public class OtpInfo {
    private int id;
    public String accountName;

    public String issuer;
    public String secret;
    public int otpLength;
    public int userTimeStep;
    public String algorithm;
    private String generatedOTP;

    public OtpInfo() {
    }

    public OtpInfo(String accountName, String issuer, String secret, int otpLength, int userTimeStep, String algorithm) {
        this.accountName = accountName;
        this.issuer = issuer;
        this.secret = secret;
        this.otpLength = otpLength;
        this.userTimeStep = userTimeStep;
        this.algorithm = algorithm;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getOtpLength() {
        return otpLength;
    }

    public void setOtpLength(int otpLength) {
        this.otpLength = otpLength;
    }

    public int getUserTimeStep() {
        return userTimeStep;
    }

    public void setUserTimeStep(int userTimeStep) {
        this.userTimeStep = userTimeStep;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getGeneratedOTP() {
        return generatedOTP;
    }

    public void setGeneratedOTP(String generatedOTP) {
        this.generatedOTP = generatedOTP;
    }

}
