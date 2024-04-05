package com.vantar.admin.database.data.panel;

import com.vantar.business.ModelMongo;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.VantarException;
import com.vantar.service.log.dto.*;
import com.vantar.web.Params;
import java.util.*;


public class AdminLogWeb {

    public static UserWebLog getData(Params params) throws VantarException {
        UserLog userLog = ModelMongo.getById(params, new UserLog());

        QueryBuilder q = new QueryBuilder(new UserWebLog());
        q.condition()
            .equal("userId", userLog.userId)
            .equal("threadId", userLog.threadId)
            .equal("url", userLog.url)
            .equal("objectId", userLog.objectId)
            .equal("action", "REQUEST");

        List<UserWebLog> data = ModelMongo.getData(q);
        if (data.size() == 1) {
            return data.get(0);
        }
        TreeMap<Long, UserWebLog> sortByTime = new TreeMap<>();
        for (UserWebLog d : data) {
            sortByTime.put(d.time.diffSeconds(userLog.time), d);
        }
        return sortByTime.firstEntry().getValue();
    }
}