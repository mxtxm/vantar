package com.vantar.admin.test;

import com.vantar.admin.documentation.get.WebServiceData;
import com.vantar.admin.index.Admin;
import com.vantar.admin.service.AdminService;
import com.vantar.business.ModelCommon;
import com.vantar.common.Settings;
import com.vantar.database.common.Db;
import com.vantar.exception.*;
import com.vantar.http.*;
import com.vantar.locale.VantarKey;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.file.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import com.vantar.web.*;
import org.apache.http.Header;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class WebUnitTestCreate {

    @SuppressWarnings("unchecked")
    public static void create(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_MENU_TEST, params, response, true);

        if (params.contains("f")) {

            // > > > step 4 - store tests
            if (params.contains("f4")) {
                WebUnitTestCase testCase = new WebUnitTestCase();
                testCase.tag = params.getString("tag");
                testCase.title = params.getString("ti");
                testCase.order = params.getInteger("or");
                testCase.tests = Json.d.listFromJson(params.getString("tc"), WebUnitTestCaseItem.class);
                try {
                    ResponseMessage r = Db.modelMongo.insert(new ModelCommon.Settings(testCase).mutex(false).logEvent(false));
                    ui.addMessage(r.message);
                } catch (VantarException e) {
                    ui.addErrorMessage(e);
                }
                ui.finish();
                return;
            }
            // step 4 - store tests < < <


            // > > > step 3 - service call JSON
            if (params.contains("f3")) {
                List<WebUnitTestCaseItem> testCases = Json.d.listFromJson(params.getString("tc"), WebUnitTestCaseItem.class);
                testCases.sort(Comparator.comparingInt(o -> o.order));

                try {
                    Db.mongo.switchToTest();
                    AdminService.factoryReset(null, 1, 20, null);
                    for (WebUnitTestCaseItem item : testCases) {
                        addAssertToCase(ui, item);
                    }
                } finally {
                    Db.mongo.switchToProduction();
                }

                ui  .beginFormPost()
                    .addInput("Tag", "tag")
                    .addInput("Title", "ti")
                    .addInput("Order", "or")
                    .addTextArea("Test cases as JSON", "tc", Json.d.toJsonPretty(testCases))
                    .addHidden("f4", 1)
                    .addSubmit()
                    .finish();
                return;
            }
            // step 3 - service call JSON < < <


            // > > > step 2 - service call JSON
            List<String> paths = params.getStringList("path-check");
            String username = params.getString("un");
            List<WebUnitTestCaseItem> testCases = new ArrayList<>(1000);
            if (paths != null) {
                int i = 0;
                for (String path : paths) {
                    WebServiceData.Data data = getInput(path);
                    if (data == null) {
                        continue;
                    }
                    WebUnitTestCaseItem item = new WebUnitTestCaseItem();
                    item.url = path;
                    item.httpMethod = data.httpMethod;
                    item.headers = data.headers;
                    if (item.headers != null) {
                        for (Map.Entry<String, String> e : item.headers.entrySet()) {
                            if ("THE-SIGNIN-TOKEN".equalsIgnoreCase(e.getValue())) {
                                e.setValue("THE-SIGNIN-TOKEN:" + username);
                            }
                        }
                    }
                    if (data.inputSample instanceof Map) {
                        item.inputMap = (Map<String, Object>) data.inputSample;
                    } else if (data.inputSample instanceof List) {
                        item.inputList = (List<Object>) data.inputSample;
                    }
                    item.order = ++i;
                    testCases.add(item);
                }
            }
            ui  .beginFormPost()
                .addTextArea("Test cases as JSON", "tc", Json.d.toJsonPretty(testCases))
                .addHidden("f3", 1)
                .addSubmit()
                .finish();
            return;
            // step 2 - service call JSON < < <

        }

        // > > > step 1 - select services
        ui  .beginBlock("div", "test-paths")
            .addEmptyLine(3)
            .beginFormPost();
        List<Class<?>> classList = ClassUtil.getClasses(Settings.config.getProperty("package.web"), WebServlet.class);
        classList.sort(Comparator.comparing(Class::getSimpleName));
        for (Class<?> cls : classList) {
            ui.addHeading(3, cls.getSimpleName());
            List<String> paths = Arrays.asList(cls.getAnnotation(WebServlet.class).value());
            paths.sort(String::compareTo);
            for (String path : paths) {
                ui.addCheckbox(path, "path-check", false, path, "path-check", false).write();
            }
        }
        ui  .addInput("Username", "un")
            .addSubmit()
            .finish();
        // step 1 - select services < < <
    }

    private static WebServiceData.Data getInput(String url) {
        String[] content = new String[] {null, null};

        DirUtil.browseFile(FileUtil.getClassPathAbsolutePath("/document"), file -> {
            content[0] = FileUtil.getFileContent(file.getAbsolutePath());
            if (content[0].contains(url)) {
                content[1] = file.getAbsolutePath();
                return true;
            }
            content[0] = null;
            return false;
        });


        if (content[0] == null) {
            return null;
        }
        WebServiceData serviceData = new WebServiceData();

        return serviceData.get(content[0], url);
    }

    private static void addAssertToCase(WebUi ui, WebUnitTestCaseItem item) {
        String url = ui.params.getBaseUrl() + item.url;
        HttpConfig config = new HttpConfig();
        if (item.headers != null) {
            Map<String, String> headers = new HashMap<>(item.headers);
            for (Map.Entry<String, String> e : headers.entrySet()) {
                String v = e.getValue();
                if (v.startsWith("THE-SIGNIN-TOKEN")) {
                    String[] parts = StringUtil.split(v, ':');
                    if (parts.length > 1) {
                        ServiceAuth auth = Services.get(ServiceAuth.class);
                        try {
                            CommonUser user = auth.forceSignin(parts[1].trim());
                            e.setValue(user.getToken());
                        } catch (AuthException | ServerException x) {
                            ui.addErrorMessage(x);
                        }
                    }
                }
            }
            config.headers.putAll(headers);
        }
        HttpConnect connect = new HttpConnect(config);
        try {
            HttpResponse res;
            if ("POST".equalsIgnoreCase(item.httpMethod)) {
                res = connect.post(url, item.inputMap);
            } else if ("POST JSON".equalsIgnoreCase(item.httpMethod)) {
                res = connect.postJson(url, item.inputMap == null ? item.inputList : item.inputMap);
            } else if ("GET".equalsIgnoreCase(item.httpMethod)) {
                res = connect.get(url, item.inputMap);
            } else {
                return;
            }
            item.assertStatusCode = res.getStatusCode();

            item.assertHeaders = new HashMap<>(14, 1);
            for (Header h : res.getHeaders()) {
                item.assertHeaders.put(h.getName(), h.getValue());
            }
            item.assertCheckHeaders = false;
            item.assertHeadersCheckMethod = WebUnitTestCaseItem.AssertCheckMethod.assertInResponse;

            item.assertResponse = res.toString();
            item.assertResponseObjectClass = res.getClass().getName();
            item.assertResponseCheckMethod = WebUnitTestCaseItem.AssertCheckMethod.assertInResponse;

        } catch (HttpException e) {
            ui.addErrorMessage(item.url + ObjectUtil.toString(e));
        }
    }
}
