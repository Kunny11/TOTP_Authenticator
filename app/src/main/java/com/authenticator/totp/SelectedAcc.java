package com.authenticator.totp;

public class SelectedAcc {
        private String accountName;
        private String algorithm;
        private String issuer;
        private int otpLength;
        private String secret;
        private int userTimeStep;

        public SelectedAcc(String accountName, String algorithm, String issuer, int otpLength, String secret, int userTimeStep) {
            this.accountName = accountName;
            this.algorithm = algorithm;
            this.issuer = issuer;
            this.otpLength = otpLength;
            this.secret = secret;
            this.userTimeStep = userTimeStep;
        }
    }
