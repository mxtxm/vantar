package com.vantar.service.auth;

import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;


public class TokenData {

    public CommonUser user;
    public DateTime lastInteraction;
    public Type type;


    public TokenData() {

    }

    public TokenData(CommonUser user) {
        this.user = user;
        lastInteraction = new DateTime();
    }

    public TokenData(CommonUser user, Type type) {
        this.user = user;
        lastInteraction = new DateTime();
        this.type = type;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }


    public enum Type {
        VERIFY_EMAIL,
        VERIFY_MOBILE,
    }
}
