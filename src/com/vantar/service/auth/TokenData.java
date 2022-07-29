package com.vantar.service.auth;

import com.vantar.util.datetime.DateTime;
import com.vantar.util.object.ObjectUtil;
import java.util.*;


public class TokenData {

    public CommonUser user;
    public DateTime lastInteraction;
    public Type type;
    public Map<String, Object> extraData;


    public TokenData() {

    }

    public TokenData(CommonUser user) {
        this.user = user;
        lastInteraction = new DateTime();
    }

    public TokenData(CommonUser user, Type type) {
        lastInteraction = new DateTime();
        this.user = user;
        this.type = type;
    }

    public String toString() {
        return ObjectUtil.toStringViewable(this);
    }

    public Object getExtraValue(String key) {
        return extraData == null ? null : extraData.get(key);
    }

    public void setExtraValue(String key, Object value) {
        if (extraData == null) {
            extraData = new HashMap<>(5);
        }
        extraData.put(key, value);
    }


    public enum Type {
        VERIFY_EMAIL,
        VERIFY_MOBILE,
    }
}
