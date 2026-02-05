package com.example.aireceiptbackend.model;

public class UserUpdateRequest {
    private String email;
    private String password;
    private String currency;

    public UserUpdateRequest() {}

    public UserUpdateRequest(String email, String password, String currency) {
        this.email = email;
        this.password = password;
        this.currency = currency;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
