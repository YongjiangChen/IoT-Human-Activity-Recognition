package com.specknet.pdiotapp;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class UserAccount
{
    private static UserAccount INSTANCE = null;
    public GoogleSignInAccount googleAccount = null;

    private UserAccount(){}

    public void setGoogleAccount(GoogleSignInAccount newGoogleAccount)
    {
        googleAccount = newGoogleAccount;
    }

    public static UserAccount getAccount()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new UserAccount();
        }
        return INSTANCE;
    }

}
