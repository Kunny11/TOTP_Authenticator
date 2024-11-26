package com.authenticator.totp;

class EncryptedJson {
    private boolean encrypted;
    private String content;

    public EncryptedJson(boolean encrypted, String content) {
        this.encrypted = encrypted;
        this.content = content;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getContent() {
        return content;
    }
}
