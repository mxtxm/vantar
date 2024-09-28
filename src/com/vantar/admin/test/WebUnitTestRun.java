package com.vantar.admin.test;

import com.vantar.admin.index.Admin;
import com.vantar.admin.service.AdminService;
import com.vantar.common.VantarParam;
import com.vantar.database.common.Db;
import com.vantar.database.query.QueryBuilder;
import com.vantar.exception.*;
import com.vantar.http.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.util.bool.BoolUtil;
import com.vantar.util.json.Json;
import com.vantar.util.object.ObjectUtil;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.apache.http.Header;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class WebUnitTestRun {

    public static void run(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_TEST, params, response, true);

        List<Long> ids = params.getLongList("delete-check");
        if (ids == null || ids.size() == 0) {
            ui.addErrorMessage(VantarKey.NO_CONTENT).finish();
            return;
        }

        QueryBuilder q = new QueryBuilder(new WebUnitTestCase());
        q.condition().inNumber("id", ids);
        q.sort("order:ASC");
        try {
            ui.addMessage("> switching to test sandbox, production must be halted until tests are completed!").write();
            Db.mongo.switchToTest();
            ui  .addMessage("> switched to test sandbox")
                .addMessage("> preparing sandbox")
                .write();
            AdminService.factoryReset(ui, 1, 20, null);
            ui  .addMessage("< sandbox prepared")
                .addMessage("> starting tests")
                .addMessage("------------------------------------------")
                .write();

            Db.modelMongo.forEach(q, dto -> {
                runTests(ui, (WebUnitTestCase) dto);
                return true;
            });

            ui.addMessage("------------------------------------------").addMessage("< tests completed").write();
        } catch (VantarException e) {
            ui.addErrorMessage(e).finish();
            return;
        } finally {
            ui.addMessage("> switching to production").write();
            Db.mongo.switchToProduction();
            ui.addMessage("> switched to production").write();
        }

        ui.finish();
    }

    private static void runTests(WebUi ui, WebUnitTestCase u) {
        ui.addHeading(2, u.title);

        u.tests.sort(Comparator.comparingInt(o -> o.order));
        for (WebUnitTestCaseItem item : u.tests) {
            addCallWebservice(ui, item);
        }
    }

    private static void addCallWebservice(WebUi ui, WebUnitTestCaseItem testItem) {
        String url = ui.params.getBaseUrl() + testItem.url;
        ui.addHeading(3, testItem.httpMethod + ": " + url).write();

        HttpConfig config = new HttpConfig();
        if (testItem.headers != null) {
            Map<String, String> headers = new HashMap<>(testItem.headers);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String v = e.getValue();
                if (v.startsWith("THE-SIGNIN-TOKEN")) {
                    String[] parts = StringUtil.splitTrim(v, VantarParam.SEPARATOR_KEY_VAL);
                    if (parts.length > 1) {
                        ServiceAuth auth = Services.get(ServiceAuth.class);
                        try {
                            CommonUser user = auth.forceSignin(parts[1].trim());
                            e.setValue(user.getToken());
                        } catch (AuthException | ServerException x) {
                            ui.addErrorMessage(x).write();
                        }
                    }
                }
            }
            config.headers.putAll(headers);
        }
        HttpConnect connect = new HttpConnect(config);
        try {
            HttpResponse res;
            if ("POST".equalsIgnoreCase(testItem.httpMethod)) {
                res = connect.post(url, testItem.inputMap);
            } else if ("POST JSON".equalsIgnoreCase(testItem.httpMethod)) {
                res = connect.postJson(url, testItem.inputMap == null ? testItem.inputList : testItem.inputMap);
            } else if ("GET".equalsIgnoreCase(testItem.httpMethod)) {
                res = connect.get(url, testItem.inputMap);
            } else {
                return;
            }

            // > > > compare status code
            if (testItem.assertStatusCode != res.getStatusCode()) {
                ui  .addErrorMessage("FAILED status-code: expected:" + testItem.assertStatusCode + " got:" + res.getStatusCode())
                    .write();
                return;
            }
            // compare status code < < <

            // > > > compare headers
            if (BoolUtil.isTrue(testItem.assertCheckHeaders)) {
                if (testItem.assertHeadersCheckMethod == null) {
                    testItem.assertHeadersCheckMethod = WebUnitTestCaseItem.AssertCheckMethod.assertInResponse;
                }
                if (testItem.assertHeadersCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.assertInResponse) {
                    Map<String, String> headers = new HashMap<>(res.getHeaders().length);
                    for (Header h : res.getHeaders()) {
                        headers.put(h.getName(), h.getValue());
                    }
                    for (Map.Entry<String, String> entry : testItem.assertHeaders.entrySet()) {
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersExclude) && testItem.assertHeadersExclude.contains(entry.getKey())) {
                            return;
                        }
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersInclude) && !testItem.assertHeadersInclude.contains(entry.getKey())) {
                            return;
                        }
                        if (!entry.getValue().equals(headers.get(entry.getKey()))) {
                            failHeaders(ui, testItem.assertHeaders, res.getHeaders());
                            return;
                        }
                    }

                } else if (testItem.assertHeadersCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.responseInAssert) {
                    for (Header h : res.getHeaders()) {
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersExclude) && testItem.assertHeadersExclude.contains(h.getName())) {
                            return;
                        }
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersInclude) && !testItem.assertHeadersInclude.contains(h.getName())) {
                            return;
                        }
                        String value = testItem.assertHeaders.get(h.getName());
                        if (!h.getValue().equals(value)) {
                            failHeaders(ui, testItem.assertHeaders, res.getHeaders());
                            return;
                        }
                    }

                } else if (testItem.assertHeadersCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.exact) {
                    if (res.getHeaders().length != testItem.assertHeaders.size()) {
                        failHeaders(ui, testItem.assertHeaders, res.getHeaders());
                        return;
                    }
                    for (Header h : res.getHeaders()) {
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersExclude) && testItem.assertHeadersExclude.contains(h.getName())) {
                            return;
                        }
                        if (ObjectUtil.isNotEmpty(testItem.assertHeadersInclude) && !testItem.assertHeadersInclude.contains(h.getName())) {
                            return;
                        }
                        if (!h.getValue().equals(testItem.assertHeaders.get(h.getName()))) {
                            failHeaders(ui, testItem.assertHeaders, res.getHeaders());
                            return;
                        }
                    }
                }
            }
            // compare headers < < <

            // > > > compare response
            Class<?> assertResponseClass;
            try {
                assertResponseClass = Class.forName(testItem.assertResponseObjectClass);
            } catch (Exception e) {
                ui  .addErrorMessage("FAILED response value:")
                    .addBlock("pre", "expected:\n" + testItem.assertResponse)
                    .addBlock("pre", "got: error");
                ui.addErrorMessage(e);
                return;
            }

            if (testItem.assertResponseCheckMethod == null) {
                testItem.assertResponseCheckMethod = WebUnitTestCaseItem.AssertCheckMethod.assertInResponse;
            }
            if (testItem.assertResponseCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.assertInResponse) {

            } else if (testItem.assertResponseCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.responseInAssert) {

            } else if (testItem.assertResponseCheckMethod == WebUnitTestCaseItem.AssertCheckMethod.exact) {
                try {
                    Object resultObj = res.toObject(assertResponseClass);
                    Object assertObj = Json.d.fromJson(testItem.assertResponse, assertResponseClass);
                    if (!assertObj.equals(resultObj)) {
                        ui  .addErrorMessage("FAILED response value:")
                            .addBlock("pre", "expected:\n" + testItem.assertResponse)
                            .addBlock("pre", "got:\n" + Json.d.toJsonPretty(resultObj));
                    }
                } catch (Exception e) {
                    ui  .addErrorMessage("FAILED response value:")
                        .addBlock("pre", "expected:\n" + testItem.assertResponse)
                        .addBlock("pre", "got: error");
                    ui.addErrorMessage(e);
                }
            }
            // compare response < < <

            ui.addMessage("PASSED!").write();
        } catch (HttpException e) {
            ui.addErrorMessage(testItem.url + ObjectUtil.toString(e)).write();
        }
    }

    private static void failHeaders(WebUi ui, Map<String, String> assertHeaders, Header[] responseHeaders) {
        ui  .addErrorMessage("FAILED header:")
            .addBlock("pre", "expected:\n" + Json.d.toJsonPretty(assertHeaders));
        Map<String, String> headers = new HashMap<>(responseHeaders.length);
        for (Header h : responseHeaders) {
            headers.put(h.getName(), h.getValue());
        }
        ui  .addBlock("pre", "got:\n" + Json.d.toJsonPretty(headers))
            .write();
    }
}
