package com.authenticator.totp.otp;

public class OtpKeyInput {
        private String seed32;
        private int timeStep;
        private int totpLength;
        private String algorithm;


        public OtpKeyInput(String seed32, int timeStep, int totpLength, String algorithm) {
            this.seed32 = seed32;
            this.timeStep = timeStep;
            this.totpLength = totpLength;
            this.algorithm = algorithm;
        }

        public String getSeed32() {
            return seed32;
        }

        public void setSeed32(String seed32) {
            this.seed32 = seed32;
        }

        public int getTimeStep() {
            return timeStep;
        }

        public void setTimeStep(int timeStep) {
            this.timeStep = timeStep;
        }

        public int getTotpLength() {
            return totpLength;
        }

        public void setTotpLength(int totpLength) {
            this.totpLength = totpLength;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(String algorithm) {
            this.algorithm = algorithm;
        }
}
