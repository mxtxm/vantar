package com.vantar.admin.database.data.field;

import com.vantar.database.dto.DtoDictionary;
import com.vantar.exception.FinishException;
import com.vantar.web.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;

@WebServlet({
    "/admin/data/fields",
})
public class Controller extends RouteToMethod {

    public void dataFields(Params params, HttpServletResponse response) throws FinishException {
        AdminDtoFields.fields(params, response, DtoDictionary.get(params.getString("dto")));
    }
}