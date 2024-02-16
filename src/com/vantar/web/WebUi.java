package com.vantar.web;

import com.vantar.admin.model.database.AdminData;
import com.vantar.business.ModelMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.database.query.PageData;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.Services;
import com.vantar.service.auth.*;
import com.vantar.service.dbarchive.ServiceDbArchive;
import com.vantar.service.log.ServiceLog;
import com.vantar.service.log.dto.UserLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.DateTime;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.*;

@SuppressWarnings("unchecked")
public class WebUi extends WebUiBasics<WebUi> {

    public WebUi(Params params, HttpServletResponse response) {
        this(params);
        this.response = response;
    }

    public WebUi(Params params) {
        authToken = params.getString(VantarParam.AUTH_TOKEN);
        lang = params.getLang();
        if (lang.equals("fa")) {
            direction = "rtl";
            boxFloat = "float-right";
            align = "right";
            alignKey = "left";
            alignValue = "right";
        } else {
            lang = "en";
            direction = "ltr";
            boxFloat = "float-left";
            align = "left";
            alignKey = "right";
            alignValue = "left";
        }
        this.params = params;
    }

    @Override
    protected WebUi getThis() {
        return this;
    }












    // > > > TOOLS

    public WebUi addLogPage(String clazz, Long id) {
        html.append("<div id='log-rows'></div><input id='pageLog' value='1' style='display:none'/><div><button id='more'"
            +" onclick=\"loadRows('").append(clazz).append("', ").append(id).append(")\">more</button></div>");

        html.append("<script>"
            + "function loadRows(clazz, id) { var p = parseInt($('#pageLog').val(), 10); $('#pageLog').val(p+1); "
            + " getString('/admin/data/log/rows', {dto:clazz,id:id,page:p}, '")
            .append(authToken).append("', 'en', function(o) { if (o=='FINISHED') { $('#more').hide(); return;} $('#log-rows').append(o)}, ); } "

            + "function setObject(id) { if ($('#log-object').hasClass('loaded')) {return;} "
            + " $('#log-object').addClass('loaded'); getString('/admin/data/log/object', {id:id}, '")
            .append(authToken).append("', 'en', function(o) {$('#log-object' + id).html(o)}, ); }</script>");

        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");
        setJs("/js/webservice.js");
        return this;
    }

    public void addLogRows(UserLog.View userLog, CommonUser user) {
        html.append("<div class='log-row clearfix'>");

        html.append("<div class='log-col-user'>")
            .append("<p class='action'>").append(userLog.action).append("</p>")
            .append("<p class='url'>").append(userLog.url).append("</p>")
            .append("<p class='thread-id'>").append("thread: ").append(userLog.threadId).append("</p>")
            .append("<p class='time'>").append(userLog.time.formatter().getDateTime()).append("</p>")
            .append("<p class='time'>").append(userLog.time.formatter().getDateTimePersianAsString()).append("</p>");
        if (user != null) {
            html.append("<p class='user'>(").append(user.getId()).append(") ")
                .append(user.getUsername()).append(" - ").append(user.getFullName()).append("</p>");
        }
        html.append("</div>");

        html.append("<div class='log-col-data'><pre class='object'>");
        if (userLog.className != null) {
            html.append("<strong>").append(escape(userLog.className));
            if (userLog.objectId != null) {
                html.append(" (").append(userLog.objectId).append(")");
            }
            html.append("</strong>\n");
        }
        html.append("</pre><textarea id='log-object" + userLog.id + "' onclick='setObject(").append(userLog.id).append(")' class='object'>");
        html.append("</textarea>");
        html.append("</div></div>");
    }


    public WebUi addImportForm(String data) {
        beginFormPost();
        addTextArea(VantarKey.ADMIN_DATA, "import", escape(data), "large ltr");
        addCheckbox(VantarKey.ADMIN_DATABASE_DELETE_ALL, "deleteall");
        addSubmit(VantarKey.ADMIN_DATA_ENTRY);
        return blockEnd();
    }

    public WebUi addPurgeForm() {
        beginFormPost();
        addErrorMessage(Locale.getString(VantarKey.ADMIN_DELETE));
        addCheckbox(Locale.getString(VantarKey.ADMIN_DELETE_ALL_CONFIRM), PARAM_CONFIRM);
        addSubmit(Locale.getString(VantarKey.ADMIN_DELETE));
        return blockEnd();
    }

    public WebUi addDeleteForm(List<Dto> dtos) {
        beginFormPost();
        addErrorMessage(Locale.getString(VantarKey.ADMIN_DELETE)).addEmptyLine();

//        boolean isLogical = dtos.size() > 0 && dtos.get(0).isDeleteLogicalEnabled();
//        if (isLogical) {
//            html.append("<div>");
//            addCheckbox(Locale.getString(VantarKey.LOGICAL_DELETED), "delete-logic");
//            html.append("<div>");
//            addCheckbox(Locale.getString(VantarKey.LOGICAL_DELETED_UNDO), VantarParam.LOGICAL_DELETED_UNDO);
//            html.append("</div><br/><br/>");
//        }

        for (Dto dto : dtos) {
            html.append("<div class='row delete-item'><input name='ids' type='checkbox' value='")
                .append(dto.getId()).append("'/> ");
            for (String name : dto.getPresentationPropertyNames()) {
                Object value = dto.getPropertyValue(name);
                html.append(value).append(" - ");
            }
            html.setLength(html.length() - 3);
            html.append("</div>");
        }
//        addSubmit(isLogical ? Locale.getString(VantarKey.ADMIN_DO) : Locale.getString(VantarKey.ADMIN_DELETE));
        addSubmit(Locale.getString(VantarKey.ADMIN_DELETE));
        return blockEnd();
    }

    public WebUi addDtoAddForm(Dto dto, String... fields) {
        return addDtoForm(dto, "insert", fields);
    }

    public WebUi addDtoUpdateForm(Dto dto, String... fields) {
        return addDtoForm(dto, "update", fields);
    }

    /**
     * VantarAdminTags: not exists: include, none: exclude, insert/update: include for action
     */
    private WebUi addDtoForm(Dto dto, String action, String... include) {
        html.append("<form method='post' id='dto-form' autocomplete='off' ><input name='f' value='1' type='hidden'/>\n");
        addSubmit(Locale.getString(VantarKey.ADMIN_DO));

        if ("update".equals(action)) {
            addInput("id", "id", dto.getId());
        }

        for (String name : include) {
            if ("id".equals(name)) {
                continue;
            }

            setAdditive(dto, name, Required.class, "<pre class='required'>*</pre>");
            setAdditive(dto, name, Unique.class, "<pre class='unique'>unique</pre>");
            setAdditive(dto, name, UniqueCi.class, "<pre class='unique'>unique</pre>");
            Default a = dto.getAnnotation(name, Default.class);
            if (a != null) {
                setAdditive(
                    dto,
                    name, Default.class, "<pre class='default' title=\"default value\">"
                        + dto.getAnnotation(name, Default.class).value() + "</pre>"
                );
            }
            setAdditive(dto, name, Localized.class, "<pre class='format'>{\"en\":\"\", \"fa\":\"\"}</pre>");

            Field field = dto.getField(name);
            Class<?> type = dto.getPropertyType(name);
            Object value = dto.getPropertyValue(name);
            if (value == null || (type == String.class && StringUtil.isEmpty((String) value))) {
                value = dto.getDefaultValue(name);
            }

            if (ClassUtil.isInstantiable(type, Number.class)) {

                Depends depends = field.getAnnotation(Depends.class);
                if (depends != null) {
                    Class<? extends Dto> depClass = depends.value();
                    if (dto.getClass().isAnnotationPresent(Mongo.class)) {
                        try {
                            List<Dto> dtos = ModelMongo.getAll(DtoDictionary.getInstance(depClass));
                            if (dtos.size() <= 1000) {
                                Map<String, String> map = new HashMap<>();
                                for (Dto depDto : dtos) {
                                    map.put(depDto.getId().toString(), depDto.getPresentationValue());
                                }
                                addSelect(name, name, map, false, value == null ? null : value.toString());
                                continue;
                            }
                        } catch (NoContentException e) {
                            addText("No data: " + depClass.getSimpleName());
                            continue;
                        } catch (VantarException ignore) {

                        }
                    }
                }

                addInput(name, name, value == null ? null : value.toString());

            } else if (type == Boolean.class) {
                addSelect(name, name, new String[] {"Yes", "No"}, false, value == null ? null : ((Boolean) value ? "Yes" : "No"));

            } else if (type.isEnum()) {
                final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) type;
                String[] values = new String[enumType.getEnumConstants().length];
                int i = 0;
                for (Enum<?> x : enumType.getEnumConstants()) {
                    values[i++] = x.toString();
                }
                addSelect(name, name, values, false, value == null ? null : value.toString());

            } else if (ClassUtil.isInstantiable(type, CollectionUtil.class) && dto.getPropertyGenericTypes(name)[0].isEnum()) {
                final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) dto.getPropertyGenericTypes(name)[0];
                String[] values = new String[enumType.getEnumConstants().length];
                int i = 0;
                for (Enum<?> x : enumType.getEnumConstants()) {
                    values[i++] = x.toString();
                }

                List<String> selectedValues;
                if (value == null) {
                    selectedValues = null;
                } else {
                    selectedValues = new ArrayList<>();
                    for (Object x : (List<?>) value) {
                        selectedValues.add(x.toString());
                    }
                }
                addSelect(name, name, values, true, value == null ? null : selectedValues, "json");

            } else if (CollectionUtil.isCollection(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g)) {
                    if (ClassUtil.isInstantiable(g[0], Dto.class)) {
                        Object obj = ClassUtil.getInstance(g[0]);
                        if (obj != null) {
                            setAdditive("<pre class='format'>[" + Json.getWithNulls().toJsonPretty(obj) + "]</pre>");
                        }
                    } else {
                        setAdditive("<pre class='format'>[" + g[0].getSimpleName() + "]</pre>");
                    }
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else if (CollectionUtil.isMap(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g) && g.length == 2) {
                    setAdditive("<pre class='format'>{" + g[0].getSimpleName() + ": " + g[1].getSimpleName() + "}</pre>");
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else if (ClassUtil.isInstantiable(type, Dto.class)) {
                DtoDictionary.Info info = DtoDictionary.get(type);
                if (info == null) {
                    ServiceLog.log.error("! dto not in dictionary? ({})", type);
                } else {
                    Dto dtoX = info.getDtoInstance();
                    if (dtoX == null) {
                        ServiceLog.log.error("! dto not in dictionary? ({})", type);
                    } else {
                        setAdditive("<pre class='format'>" + Json.getWithNulls().toJsonPretty(dtoX) + "</pre>");
                    }
                }
                addTextArea(
                    name,
                    name,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json"
                );

            } else {
                if (Json.isJsonShallow(value)) {
                    addTextArea(
                        name,
                        name,
                        Json.getWithNulls().toJsonPretty(value),
                        "small json"
                    );
                    continue;
                }

                String iType;
                if (name.contains("password")) {
                    iType = "password";
                    value = "";
                } else {
                    iType = null;
                }
                if (value instanceof Location) {
                    Location location = (Location) value;
                    setAdditive("<a target='_blank' href='https://www.google.com/maps/search/?api=1&query="
                        + location.latitude + "," + location.longitude + "'>map</a>");
                }
                addInput(name, name, value == null ? null : value.toString(), iType);
            }
        }

        if (dto instanceof CommonUser && !(dto instanceof CommonUserPassword)) {
            addInput("password", "password", null, "password");
        }

        addSubmit(Locale.getString(VantarKey.ADMIN_DO));
        html.append("<input type='hidden' name='asjson' id='asjson'/>").append("</form>");
        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");
        return this;
    }

    public WebUi addDtoView(Dto dto) {
        for (Map.Entry<String, Object> item : dto.getPropertyValues().entrySet()) {
            String value;
            if (item.getValue() == null) {
                value = "NULL";
            } else if (item.getValue() instanceof DateTime) {
                value = lang != null && lang.equals("fa") ?
                    ((DateTime) item.getValue()).formatter().getDateTimePersianStyled() :
                    ((DateTime) item.getValue()).formatter().getDateTimeStyled();
            } else {
                value = ObjectUtil.toStringViewable(dto.getPropertyValue(item.getKey()));
            }

            if (value.toLowerCase().contains("jpg") || value.toLowerCase().contains("png")) {
                StringBuilder images = new StringBuilder(250);
                for (String v : StringUtil.splitTrim(value, '|')) {
                    if (StringUtil.isNotEmpty(v)) {
                        images
                            .append("<img src='").append(v).append("' alt='").append(v)
                            .append("' title='").append(v).append("'/> ");
                    }
                }
                value = images.toString();
            } else {
                value = escapeWithNtoBr(value);
            }

            addKeyValue(item.getKey(), value);
        }
        return this;
    }

    public WebUi addDtoListWithHeader(PageData data, DtoDictionary.Info info, String... fields) {
        Dto dto = info.getDtoInstance();

        Class<?> upperClass = dto.getClass().getDeclaringClass();
        String className = upperClass == null ? dto.getClass().getSimpleName() : upperClass.getSimpleName();

        html.append("<div id='actions'>")
            .append(" <a href='").append(getLink("/admin/data/insert?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_NEW_RECORD)).append("</a>")
            .append(" | <a href='").append(getLink("/admin/data/import?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_IMPORT)).append("</a>")
            .append(" | <a href='").append(getLink("/admin/data/export?dto=" + className))
            .append("'>").append(Locale.getString(VantarKey.ADMIN_EXPORT)).append("</a>");

        if (data != null) {
            html.append(" | <a href='").append(getLink("/admin/data/update?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_BATCH_EDIT)).append("</a>")
                .append(" | <a href='").append(getLink("/admin/data/delete?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_BATCH_DELETE)).append("</a>")
                .append(" | <a href='").append(getLink("/admin/data/purge?dto=" + className))
                .append("'>").append(Locale.getString(VantarKey.ADMIN_DATABASE_DELETE_ALL)).append("</a>");
        }

        html.append("</div>");

        if (dto.hasAnnotation(Archive.class)) {
            html.append("<div id='archives'>");
            ServiceDbArchive.ArchiveInfo aInfo = ServiceDbArchive.getArchives().get(className);
            if (aInfo == null || ObjectUtil.isEmpty(aInfo.collections)) {
                html.append("no archives");
            } else {
                String linkM = className.equalsIgnoreCase(aInfo.activeCollection) ?
                    "(active)" :
                    getHref("switch", "/admin/data/archive/switch?dto=" + className + "&a=" + className, true, false, "");
                addKeyValue("current", className + " " + linkM, null, false);

                aInfo.collections.forEach((name, date) -> {
                    String link = name.equalsIgnoreCase(aInfo.activeCollection) ?
                        "(active)" :
                        getHref("switch", "/admin/data/archive/switch?dto=" + className + "&a=" + name, true, false, "");
                    addKeyValue(date, name + " " + link, null, false);
                });
            }
            html.append("</div>");
        }

        // PAGE
        // SORT
        // PAGE
        int pageNo = params.getInteger("page-no", 1);
        int pageLength = params.getInteger("page-length", AdminData.N_PER_PAGE);
        String sort = params.getString("sort", VantarParam.ID);
        String sortPos = params.getString("sortpos", "desc");

        html.append("<form id='dto-list-form' method='post'>");
        if (authToken != null) {
            html.append("<input type='hidden' name='" + VantarParam.AUTH_TOKEN + "' value='")
                .append(authToken).append("'/>\n");
        }
        // N PER PAGE
        html.append("<table id='container-search-options'>")

            // N PER PAGE
            .append("<tr>")
            .append("<td class='dto-list-form-title'>").append(Locale.getString(VantarKey.ADMIN_PAGING)).append("</td>")
            .append("<td class='dto-list-form-option'>")
            .append("<input id='page-length' name='page-length' title='page length' value='").append(pageLength).append("'/>")
            .append("<input id='page-no' name='page-no' title='page number' value='").append(pageNo).append("'/>");

            if (data != null) {
                html.append("<input id='total-pages' value='")
                    .append((long) Math.ceil((double) data.total / (double) data.length)).append(" pages'/>");
            }

            html.append("</td>")
                .append("</tr>")

            // SORT
            .append("<tr>").append("<td class='dto-list-form-title'>")
            .append(Locale.getString(VantarKey.ADMIN_SORT)).append("</td>")
            .append("<td class='dto-list-form-option'>");


        html.append("<input id='sort' name='sort' value='id'/>");

//        html.append("<select id='sort' name='sort'>");
//        for (String name : dto.getProperties()) {
//            html.append("<option ");
//            if (name.equals(sort)) {
//                html.append("selected='selected' ");
//            }
//            html.append("value='").append(name).append("'>").append(name).append("</option>");
//        }
//        html.append("</select>");


        html.append("<select id='sortpos' name='sortpos'>")
            .append("<option ");
        if ("asc".equals(sortPos)) {
            html.append("selected='selected' ");
        }
        html.append("value='asc'>asc</option>")
            .append("<option ");
        if ("desc".equals(sortPos)) {
            html.append("selected='selected' ");
        }
        html.append("value='desc'>desc</option>")
            .append("</select>")
            .append("</td></tr>");

        // search
        String jsonSearch = params.getString("jsonsearch", "");

        html.append("<tr>")
            .append("<td class='dto-list-form-title'>").append(Locale.getString(VantarKey.ADMIN_SEARCH)).append("</td>")
            .append("<td class='dto-list-form-option'><div>");

        html.append("<input id='search-col' value='id'/>");
//        html.append("<select id='search-col'>");
//        for (String name : dto.getProperties()) {
//            if (ClassUtil.isInstantiable(dto.getPropertyType(name), Dto.class)) {
//                DtoDictionary.Info obj = DtoDictionary.get(name);
//                if (obj == null) {
//                    continue;
//                }
//                for (String nameInner : obj.getDtoInstance().getProperties()) {
//                    nameInner = name + '.' + nameInner;
//                    html.append("<option value='").append(nameInner).append("'>").append(nameInner).append("</option>");
//                }
//            } else {
//                html.append("<option value='").append(name).append("'>").append(name).append("</option>");
//            }
//        }
//        html.append("</select>");

        html.append("<select id='search-type'>")
            .append("    <optgroup label='equal' selected='selected'>")
            .append("        <option value='EQUAL'>EQUAL</option>")
            .append("        <option value='NOT_EQUAL'>NOT_EQUAL</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='string'>")
            .append("        <option value='LIKE'>LIKE</option>")
            .append("        <option value='NOT_LIKE'>NOT_LIKE</option>")
            .append("        <option value='FULL_SEARCH'>FULL_SEARCH</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='collection'>")
            .append("        <option value='IN' title='if col value exists in list'>IN [list,]</option>")
            .append("        <option value='NOT_IN' title='if col value not-exists in list'>NOT_IN [list,]</option>")
            .append("        <option value='CONTAINS_ALL' title='if col(list) contains all items of list'>CONTAINS_ALL [list,]</option>")
            .append("        <option value='CONTAINS_ALL-object' title='if col(dictionary).k has value v'>CONTAINS_ALL {k:v,}</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='number/date compare'>")
            .append("        <option value='LESS_THAN'>LESS_THAN</option>")
            .append("        <option value='LESS_THAN_EQUAL'>LESS_THAN_EQUAL</option>")
            .append("        <option value='GREATER_THAN'>GREATER_THAN</option>")
            .append("        <option value='GREATER_THAN_EQUAL'>GREATER_THAN_EQUAL</option>")
            .append("        <option value='BETWEEN'>BETWEEN (x, y)</option>")
            .append("        <option value='BETWEEN-2'>BETWEEN (col2, x, y)</option>")
            .append("        <option value='NOT_BETWEEN'>NOT_BETWEEN (x, y)</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='empty check'>")
            .append("        <option value='IS_NULL'>IS_NULL</option>")
            .append("        <option value='IS_NOT_NULL'>IS_NOT_NULL</option>")
            .append("        <option value='IS_EMPTY'>IS_EMPTY</option>")
            .append("        <option value='IS_NOT_EMPTY'>IS_NOT_EMPTY</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='point'>")
            .append("        <option value='NEAR'>NEAR (lat, lng, maxDistance(m), minDistance(m))</option>")
            .append("        <option value='FAR'>FAR (lat, lng, maxDistance(m))</option>")
            .append("    </optgroup>")
            .append("    <optgroup label='polygon'>")
            .append("        <option value='WITHIN'>WITHIN [lat1, lng1, lat2, lng2,]</option>")
            .append("    </optgroup>")
            .append("</select>")

            .append("<input type='text' id='search-value'/>")
            .append("<button id='add-search-item' type='button'>+</button>")
            .append("</div></td>")
            .append("</tr>")

            .append("<tr><td></td>")
            .append("<td>")
            .append("<ul id='search-items'></ul>")
            .append("<p><textarea id='jsonsearch' name='jsonsearch'>")
            .append(jsonSearch).append("</textarea></p>")
            .append("</td>")
            .append("</tr>");

//        if (dto.isDeleteLogicalEnabled()) {
//            String deletePolicy = params.getString("delete-policy", "n");
//            html.append("<tr><td></td><td class='dto-list-form-option'><select name='delete-policy'>")
//                .append("<option value='n'")
//                    .append(deletePolicy.equals("n") ? " selected='selected'" : "").append(">")
//                    .append(Locale.getString(VantarKey.SHOW_NOT_DELETED)).append("</option>")
//                .append("<option value='d'")
//                    .append(deletePolicy.equals("d") ? " selected='selected'" : "").append(">")
//                    .append(Locale.getString(VantarKey.SHOW_DELETED)).append("</option>")
//                .append("<option value='a'")
//                    .append(deletePolicy.equals("a") ? " selected='selected'" : "").append(">")
//                    .append(Locale.getString(VantarKey.SHOW_ALL)).append("</option>")
//                .append("</select></td></tr>");
//        }

        // button
        html.append("<tr><td>");

        html.append("<select id='search-op'>")
            .append("    <option value='AND' selected='selected'>AND</option>")
            .append("    <option value='OR'>OR</option>")
            .append("    <option value='XOR'>XOR</option>")
            .append("</select>");

        html.append("</td><td class='dto-list-form-option'>");

        html.append("<button type='submit'>&gt;&gt;</button><button id='get-search-json' type='button'>JSON</button></td></tr>")
            .append("<tr><td></td><td>")
            .append("</td></tr></table></form>")
            .append("<form method='post' action='/admin/data/delete/many'>");
        if (authToken != null) {
            html.append("<input type='hidden' name='" + VantarParam.AUTH_TOKEN + "' value='")
                .append(authToken).append("'/>\n");
        }
        if (data == null) {
            addMessage(Locale.getString(VantarKey.NO_CONTENT));
        } else {
            addDtoList(data, true, fields);

            html.append("<div id='delete-container'>")
                .append("<input name='dto' value='").append(dto.getClass().getSimpleName())
                .append("' type='hidden'/>");

            html.append("<div class='delete-type-container'>")
                .append("<input type='checkbox' id='delete-select-all'/> ")
                .append("<label for='delete-select-all'>").append(Locale.getString(VantarKey.SELECT_ALL))
                .append("</label></div>");

//            if (dto.isDeleteLogicalEnabled()) {
//                html.append("<div class='delete-type-container'>")
//                    .append("<input type='checkbox' name='do-logical-delete' id='do-logical-delete' checked='checked'/> ")
//                    .append("<label for='do-logical-delete'>").append(Locale.getString(VantarKey.LOGICAL_DELETED))
//                    .append("</label></div>");
//            }

            html.append("<div><input type='checkbox' name='confirm-delete'/> <label for='confirm-delete'>")
                .append(Locale.getString(VantarKey.ADMIN_CONFIRM)).append("</label></div>")
                .append("<div class='delete-button-container'><button id='delete-button' type='submit'>")
                .append(Locale.getString(VantarKey.ADMIN_DELETE)).append("</button></div>")
                .append("</div>");
        }

        html.append("</form>");
        setJs("/js/jquery.min.js");
        setJs("/js/vantar.js");
        return this;
    }

    public WebUi addDtoList(PageData data, boolean options, String... fields) {
        html.append("<div class='scroll'><table class='list'>");
        boolean isLogServiceUp = Services.isUp(ServiceLog.class);
        boolean isFirst = true;
        for (Dto dto : data.data) {
            if (isFirst) {
                html.append("<tr>");
                if (options) {
                    html.append("<th id='total' colspan='").append(isLogServiceUp ? 3 : 2).append("'>")
                        .append(data.total).append("</th>");
                }
                html.append("<th class='list-head-id'>id</th>");
                for (String name : fields) {
                    if ("id".equals(name)) {
                        continue;
                    }
                    Class<?> type = dto.getPropertyType(name);
                    if ((!name.equals("name") && !name.equals("title")) &&
                        (CollectionUtil.isCollectionOrMap(type) || ClassUtil.isInstantiable(type, Dto.class))) {
                        continue;
                    }

                    html.append("<th>").append(name).append("</th>");
                }
                html.append("</tr>");
                isFirst = false;
            }

            html.append("<tr>");
            if (options) {
                String x = authToken == null ? "" : ("&" + VantarParam.AUTH_TOKEN + "=" + authToken);
                if (lang != null) {
                    x += "&" + VantarParam.LANG + "=" + lang;
                }

                Class<?> upperClass = dto.getClass().getDeclaringClass();
                String className = upperClass == null ? dto.getClass().getSimpleName() : upperClass.getSimpleName();

                html
                    .append("<td class='option delete-option'><input class='delete-check' name='delete-check' value='")
                    .append(dto.getId()).append("' type='checkbox'/></td>")
                    .append("<td class='option'><div class='option'><a href='" + "/admin/data/update?dto=")
                    .append(className).append("&id=").append(dto.getId()).append(x).append("'>")
                    .append(Locale.getString(VantarKey.ADMIN_EDIT)).append("</a></div></td>");


                if (isLogServiceUp) {
                    html.append("<td class='option'><div class='option'><a href='" + "/admin/data/log?dto=")
                        .append(dto.getClass().getSimpleName()).append("&id=").append(dto.getId()).append(x).append("'>")
                        .append(Locale.getString(VantarKey.ADMIN_ACTION_LOG)).append("</a></div></td>");
                }
            }

            html.append("<td><div onclick='cellClick(this)'>");
            html.append(dto.getId()).append("<input type='hidden' value='").append(dto.getId()).append("'/>").append("</div></td>");

            for (String name : fields) {
                if ("id".equals(name)) {
                    continue;
                }
                Class<?> type = dto.getPropertyType(name);

                if ((!name.equals("name") && !name.equals("title")) &&
                    (CollectionUtil.isCollectionOrMap(type) || ClassUtil.isInstantiable(type, Dto.class))) {
                    continue;
                }

                Object realValue = dto.getPropertyValue(name);
                if (!(realValue instanceof Long) && !(realValue instanceof DateTime)) {
                    realValue = "";
                }

                Object value = ObjectUtil.toStringViewable(dto.getPropertyValue(name));
                if (value == null) {
                    value = "-";
                }

                html.append("<td><div onclick='cellClick(this)'>");
                if (realValue instanceof DateTime) {
                    html.append(((DateTime) realValue).formatter().getDateTimePersianStyled()).append("<br/>");
                }
                html.append(value).append("<input type='hidden' value='").append(realValue).append("'/>").append("</div></td>");
            }
            html.append("</tr>");
        }
        html.append("</table></div>");
        return this;
    }

    public WebUi addPagination(long total, int pageNumber, int nPerPage) {
        return addPagination(total, pageNumber, nPerPage, null);
    }

    public WebUi addPagination(long total, int pageNumber, int nPerPage, String newParams) {
        html.append("<div class='pagination'>").append("<select class='page-select' onchange='location=this.value;'>");

        for (long i = 1, l = (long) Math.ceil((double) total / (double) nPerPage); i <= l; ++i) {
            html.append("<option ").append(pageNumber == i ? "selected='selected' " : "").append("value='?");

            Map<String, Object> q = params.getAll();
            q.put("page", Long.toString(i));
            if (authToken != null) {
                q.put(VantarParam.AUTH_TOKEN, authToken);
            }
            if (lang != null) {
                q.put(VantarParam.LANG, lang);
            }
            boolean first = true;
            for (Map.Entry<String, Object> entry : q.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue().toString();
                if (!first) {
                    html.append("&");
                }
                first = false;
                html.append(k).append("=").append(v);
            }

            if (StringUtil.isNotEmpty(newParams)) {
                html.append('&').append(newParams);
            }

            html.append("'>").append(i).append("</option>");
        }
        html.append("</select></div>");
        return this;
    }
}