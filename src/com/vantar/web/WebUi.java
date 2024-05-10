package com.vantar.web;

import com.vantar.admin.database.data.panel.DataUtil;
import com.vantar.business.ModelMongo;
import com.vantar.common.VantarParam;
import com.vantar.database.datatype.Location;
import com.vantar.database.dto.*;
import com.vantar.database.query.PageData;
import com.vantar.exception.*;
import com.vantar.locale.Locale;
import com.vantar.locale.*;
import com.vantar.service.auth.*;
import com.vantar.service.log.ServiceLog;
import com.vantar.util.collection.CollectionUtil;
import com.vantar.util.datetime.*;
import com.vantar.util.json.Json;
import com.vantar.util.object.*;
import com.vantar.util.string.StringUtil;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
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

    // > > > DTO

    public WebUi addDtoLinks(DtoDictionary.Info info, Dto dto, List<String> showItems, String selected) {
        html.append("<div id=\"actions\">");
        String qString = "dto=" + dto.getClass().getSimpleName();
        boolean isFirst = true;
        for (String item : showItems) {
            boolean isSelected = item.equals(selected);
            switch (item) {
                case "list":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DATA_LIST, "list?" + qString, isSelected, false);
                    break;
                case "import":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_IMPORT, "import?" + qString, isSelected, false);
                    break;
                case "export":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_EXPORT, "export?" + qString, isSelected, false);
                    break;
                case "purge":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DATA_PURGE, "purge?" + qString, isSelected, false);
                    break;
                case "insert":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_INSERT, "insert?" + qString, isSelected, false);
                    break;
                case "undelete":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_UNDELETE, "undelete/search?" + qString, isSelected, false);
                    break;
                case "log":
                    addSubMenuLink(
                        isFirst,
                        VantarKey.ADMIN_LIST_OPTION_ACTION_LOG,
                        "log/action/search?type=a&" + qString,
                        isSelected,
                        false
                    );
                    break;
                case "dependencies":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DEPENDENCIES, "dependencies/dto?" + qString, isSelected, false);
                    break;
                case "cache":
                    if (!dto.hasAnnotation(Cache.class)) {
                        continue;
                    }
                    String classProp = "class=\"cached" + ("cache".equals(selected) ? " selected" : "") + "\" ";
                    html.append(" | <a ").append(classProp).append("target=\"_blank\" href=\"")
                        .append(getLink("/admin/database/cache/view?" + qString)).append("\">")
                        .append(Locale.getString(VantarKey.ADMIN_CACHE)).append("</a>");
                    html.append(" | <a ").append(classProp).append("target=\"_blank\" href=\"")
                        .append(getLink("/admin/database/cache/refresh?" + qString)).append("\">")
                        .append(Locale.getString(VantarKey.ADMIN_REFRESH)).append("</a>");
                    break;
                case "index":
                    String url;
                    if (DtoDictionary.Dbms.MONGO.equals(info.dbms)) {
                        url = "/admin/database/mongo/index/get?" + qString;
                    } else if (DtoDictionary.Dbms.SQL.equals(info.dbms)) {
                        url = "/admin/database/sql/index/get?" + qString;
                    } else {
                        continue;
                    }
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DATABASE_INDEX, url, isSelected, true);
                    break;
                default:
                    continue;

            }
            isFirst = false;
        }
        html.append("</div>");
        return getThis();
    }

    public WebUi addDtoItemLinks(String cName, Long id, List<String> showItems, String selected) {
        html.append("<div id=\"actions\">");
        Map<String, String> params = new HashMap<>(7, 1);
        boolean isFirst = true;
        String qString = "";
        for (String item : showItems) {
            if (item.contains(":")) {
                String[] parts = StringUtil.split(item, ":");
                params.put(parts[0], parts[1]);
                continue;
            }
            if (isFirst) {
                qString = "dto=" + cName + "&id=" + id
                    + (params.containsKey("un") ? "&un=" + params.get("un") + "&ufn=" + params.get("ufn") : "");
            }

            boolean isSelected = item.equals(selected);
            switch (item) {
                case "delete":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DELETE, "delete?" + qString, isSelected, false);
                    break;
                case "update":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_UPDATE, "update?" + qString, isSelected, false);
                    break;
                case "view":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_VIEW, "view?" + qString, isSelected, false);
                    break;
                case "dependencies":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_DEPENDENCIES, "dependencies?" + qString, isSelected, false);
                    break;
                case "log-action":
                    addSubMenuLink(
                        isFirst,
                        VantarKey.ADMIN_LIST_OPTION_ACTION_LOG,
                        "log/action/search?type=b&" + qString,
                        isSelected,
                        false
                    );
                    break;
                // only for User
                case "log-activity":
                    addSubMenuLink(
                        isFirst,
                        VantarKey.ADMIN_LIST_OPTION_USER_ACTIVITY,
                        "log/action/search?type=c&" + qString,
                        isSelected,
                        false
                    );
                    break;
                // only for User
                case "log-web":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_LOG_WEB, "log/web/search?type=a&" + qString, isSelected, false);
                    break;
                case "update-property":
                    addSubMenuLink(isFirst, VantarKey.ADMIN_UPDATE_PROPERTY, "update/property?" + qString, isSelected, true);
                default:
                    continue;
            }
            isFirst = false;
        }
        html.append("</div>");
        return getThis();
    }

    private void addSubMenuLink(boolean isFirst, VantarKey title, String url, boolean isSelected, boolean newPage) {
        if (!isFirst) {
            html.append(" | ");
        }
        html.append("<a href=\"").append(url.startsWith("/") ? getLink(url) : getLink("/admin/data/" + url)).append("\"");
        if (isSelected) {
            html.append(" class=\"item-selected\"");
        }
        if (newPage) {
            html.append(" target=\"_blank\"");
        }
        html.append(">").append(Locale.getString(title)).append("</a>");
    }

    public WebUi addDtoAddForm(Dto dto, String... fields) {
        return dtoForm(dto, false, fields);
    }

    public WebUi addDtoUpdateForm(Dto dto, String... fields) {
        return dtoForm(dto, true, fields);
    }

    private WebUi dtoForm(Dto dto, boolean isUpdate, String... include) {
        html.append("<div id='data-hint'><p id='hint-close' onclick=\"$('#data-hint').hide()\">x</p>")
            .append("<div id='hint-container'></div></div>\n")
            .append("<form method='post' id='dto-form' autocomplete='off'><input name='f' value='1' type='hidden'/>\n");

        addSubmit(VantarKey.ADMIN_SUBMIT);
        dto.setLang(params.getLang());

        setWidgetComment("<pre class='type'>auto increment (serial)</pre>");
        addInput("id", "id", dto.getId());

        for (String col : include) {
            if ("id".equals(col)) {
                continue;
            }

            Field field = dto.getField(col);
            Class<?> type = dto.getPropertyType(col);
            Object value = dto.getPropertyValue(col);
            if (ObjectUtil.isEmpty(value)) {
                value = dto.getDefaultValue(col);
            }
            String inputClass = field.isAnnotationPresent(Required.class) ? "required" : null;

            // > > > tags
            setWidgetComment("<pre class='type'>" + type.getSimpleName() + "</pre>");
            setWidgetComment(dto, col, Unique.class, "<pre class='unique'>unique</pre>");
            setWidgetComment(dto, col, UniqueCi.class, "<pre class='unique'>unique-ci</pre>");
            Limit limitA = dto.getAnnotation(col, Limit.class);
            if (limitA != null) {
                setWidgetComment(dto, col, Limit.class, "<pre class='limit' title='limit'>" + limitA.value() + "</pre>");
            }
            if (isUpdate) {
                setWidgetComment(dto, col, UpdateTime.class, "<pre class='default' title='default value'>current date-time</pre>");
            } else {
                setWidgetComment(dto, col, CreateTime.class, "<pre class='default' title='default value'>current date-time</pre>");
            }
            Default defaultA = dto.getAnnotation(col, Default.class);
            if (defaultA != null) {
                setWidgetComment(dto, col, Default.class, "<pre class='default' title='default value'>" + defaultA.value() + "</pre>");
            }
            setWidgetComment(dto, col, Localized.class, "<pre class='format'>{\"en\":\"\", \"fa\":\"\"}</pre>");
            Regex regexA = dto.getAnnotation(col, Regex.class);
            if (regexA != null) {
                setWidgetComment(dto, col, Regex.class, "<pre class='regex'>" + regexA.value() + "</pre>");
            }
            // tags < < <

            // > > > DEPENDING VALUE
            DependsValue dependsValueA = dto.getAnnotation(col, DependsValue.class);
            if (dependsValueA != null) {
                Class<? extends Dto> depClass = dependsValueA.dto();
                String f = dependsValueA.field();
                Collection<Object> data;
                try {
                    if (dto.getClass().isAnnotationPresent(Mongo.class)) {
                        data = ModelMongo.getPropertyList(DtoDictionary.getInstance(depClass), f, params.getLang());
                    } else {
                        // todo sql
                        data = new ArrayList<>(1);
                    }
                } catch (NoContentException e) {
                    continue;
                } catch (VantarException ignore) {
                    data = new ArrayList<>(1);
                }
                addInputSelectable(col, col, data, value, inputClass);
                continue;
            }
            // > > > NUMBER
            if (ClassUtil.isInstantiable(type, Number.class)) {
                // > > > RELATION
                Depends depends = field.getAnnotation(Depends.class);
                if (depends != null) {
                    Class<? extends Dto>[] dependClasses = depends.value();
                    if (dependClasses.length == 1) {
                        Class<? extends Dto> depClass = depends.value()[0];
                        List<Dto> dtos;
                        try {
                            if (dto.getClass().isAnnotationPresent(Mongo.class)) {
                                dtos = ModelMongo.getAll(DtoDictionary.getInstance(depClass));
                            } else {
                                // todo sql
                                dtos = new ArrayList<>(1);
                            }
                        } catch (NoContentException e) {
                            addText("No data: " + depClass.getSimpleName());
                            continue;
                        } catch (VantarException e) {
                            addErrorMessage(e);
                            continue;
                        }
                        if (dtos.size() <= 1000) {
                            Map<String, String> map = new HashMap<>(1000, 1);
                            for (Dto depDto : dtos) {
                                depDto.setLang(params.getLang());
                                map.put(depDto.getId().toString(), depDto.getId() + ": " + depDto.getPresentationValue());
                            }
                            addSelect(col, col, map, false, value, inputClass);
                            continue;
                        }
                    }
                }
                // > > > NORMAL NUMBER
                addInput(col, col, value, inputClass);
                continue;
            }
            // > > > BOOL
            if (type == Boolean.class) {
                addSelect(
                    col,
                    col,
                    new String[] {"Yes", "No"},
                    false, value == null ? null : ((Boolean) value ? "Yes" : "No"),
                    inputClass
                );
                continue;
            }
            // > > > ENUM
            if (type.isEnum()) {
                addSelect(col, col, EnumUtil.getEnumValues((Class<? extends Enum<?>>) type), false, value, inputClass);
                continue;
            }
            // > > > ENUM COLLECTION
            if (CollectionUtil.isCollection(type) && dto.getPropertyGenericTypes(col)[0].isEnum()) {
                List<String> selectedValues;
                if (value == null) {
                    selectedValues = null;
                } else {
                    selectedValues = new ArrayList<>(((List<?>) value).size());
                    for (Enum<?> x : (List<? extends Enum<?>>) value) {
                        selectedValues.add(x.name());
                    }
                }
                addSelect(
                    col,
                    col,
                    EnumUtil.getEnumValues((Class<? extends Enum<?>>) dto.getPropertyGenericTypes(col)[0]),
                    true,
                    selectedValues,
                    inputClass
                );
                continue;
            }
            // > > > COLLECTION
            if (CollectionUtil.isCollection(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g)) {
                    if (ClassUtil.isInstantiable(g[0], Dto.class)) {
                        Object obj = ClassUtil.getInstance(g[0]);
                        if (obj != null) {
                            setWidgetComment("<pre class='format'>[" + Json.getWithNulls().toJsonPretty(obj) + "]</pre>");
                        }
                    } else {
                        setWidgetComment("<pre class='format'>[" + g[0].getSimpleName() + "]</pre>");
                    }
                }
                addTextArea(
                    col,
                    col,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json" + (inputClass == null ? "" : (" " + inputClass))
                );
                continue;
            }
            // > > > MAP
            if (CollectionUtil.isMap(type)) {
                Class<?>[] g = ClassUtil.getGenericTypes(field);
                if (ObjectUtil.isNotEmpty(g) && g.length == 2) {
                    if (ClassUtil.isInstantiable(g[1], Dto.class)) {
                        Object obj = ClassUtil.getInstance(g[1]);
                        if (obj != null) {
                            setWidgetComment("<pre class='format'>{" + g[0].getSimpleName() + ": "
                                + Json.getWithNulls().toJsonPretty(obj) + "}</pre>");
                        }
                    } else if (!field.isAnnotationPresent(Localized.class)) {
                        setWidgetComment("<pre class='format'>{" + g[0].getSimpleName() + ": " + g[1].getSimpleName() + "}</pre>");
                    }
                }
                addTextArea(
                    col,
                    col,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json" + (inputClass == null ? "" : (" " + inputClass))
                );
                continue;

            }
            // > > > DTO
            if (ClassUtil.isInstantiable(type, Dto.class)) {
                DtoDictionary.Info info = DtoDictionary.get(type);
                if (info == null) {
                    ServiceLog.log.error("! dto not in dictionary? ({})", type);
                } else {
                    Dto dtoX = info.getDtoInstance();
                    if (dtoX == null) {
                        ServiceLog.log.error("! dto not in dictionary? ({})", type);
                    } else {
                        setWidgetComment("<pre class='format'>" + Json.getWithNulls().toJsonPretty(dtoX) + "</pre>");
                    }
                }
                addTextArea(
                    col,
                    col,
                    value == null ? null : Json.getWithNulls().toJsonPretty(value),
                    "small json" + (inputClass == null ? "" : (" " + inputClass))
                );
                continue;
            }
            // > > > data looks like JSON
            if (Json.isJsonShallow(value)) {
                addTextArea(
                    col,
                    col,
                    Json.getWithNulls().toJsonPretty(value),
                    "small json" + (inputClass == null ? "" : (" " + inputClass))
                );
                continue;
            }
            // > > > DateRange
            if (value instanceof DateTimeRange) {
                addTextArea(
                    col,
                    col,
                    Json.getWithNulls().toJsonPretty(value),
                    "small json" + (inputClass == null ? "" : (" " + inputClass))
                );
                continue;
            }
            // > > > Location
            if (value instanceof Location) {
                Location location = (Location) value;
                setWidgetComment("<a target='_blank' href='https://www.google.com/maps/search/?api=1&query="
                    + location.latitude + "," + location.longitude + "'>map</a>");
            }

            // > > > STRING/TEXT
            if (field.isAnnotationPresent(Text.class)) {
                addTextArea(col, col, value, "small" + (inputClass == null ? "" : (" " + inputClass)));
            } else if (col.contains("password")) {
                addPassword(col, col, value, "password");
            } else {
                addInput(col, col, value, inputClass);
            }
        }

        if (dto instanceof CommonUser && !(dto instanceof CommonUserPassword)) {
            addPassword("password", "password", null, "password");
        }

        if (!DataUtil.isDtoLog(dto)) {
            addSubmit(VantarKey.ADMIN_SUBMIT);
        }

        html.append("<input type='hidden' name='asjson' id='asjson'/>")
            .append("</form>");
        return this;
    }

    public WebUi addDtoListWithHeader(PageData data, DtoDictionary.Info info, DtoListOptions options) {
        Dto dto = info.getDtoInstance();
        Class<?> upperClass = dto.getClass().getDeclaringClass();
        if (upperClass != null) {
            info = DtoDictionary.get(upperClass);
            dto = info.getDtoInstance();
        }

        // archive > > >
//        if (options.archive && dto.hasAnnotation(Archive.class)) {
//            html.append("<div id='archives'>");
//            ServiceDbArchive.ArchiveInfo aInfo = ServiceDbArchive.getArchives().get(cName);
//            if (aInfo == null || ObjectUtil.isEmpty(aInfo.collections)) {
//                html.append("no archives");
//            } else {
//                String linkM = cName.equalsIgnoreCase(aInfo.activeCollection) ?
//                    "(active)" :
//                    getHref("switch", "/admin/data/archive/switch?dto=" + cName + "&a=" + cName, true, false, "");
//                addKeyValue("current", cName + " " + linkM, null, false);
//
//                aInfo.collections.forEach((name, date) -> {
//                    String link = name.equalsIgnoreCase(aInfo.activeCollection) ?
//                        "(active)" :
//                        getHref("switch", "/admin/data/archive/switch?dto=" + cName + "&a=" + name, true, false, "");
//                    addKeyValue(date, name + " " + link, null, false);
//                });
//            }
//            html.append("</div>");
//        }
        // < < < archive

        // > > > params
        int pageNo = params.getInteger("page-no", 1);
        int pageLength = params.getInteger("page-length", DataUtil.N_PER_PAGE);
        String sort = params.getString("sort", VantarParam.ID);
        String sortPos = params.getString("sortpos", "desc");
        String jsonSearch = params.getString("jsonsearch", "");
        if (jsonSearch != null) {
            jsonSearch = Json.d.toJsonPretty(jsonSearch);
        }

        html.append("<div id=\"s-container\" class=\"clearfix\">");
        html.append("<form id=\"dto-list-form\" method=\"post\">");
        addHidden(VantarParam.AUTH_TOKEN, authToken);
        // > > > pagination
        if (options.pagination) {
            html.append("<div id=\"s-options-a\">");
            html.append("<div class=\"s-col\">");
            html.append("<label>").append(Locale.getString(VantarKey.ADMIN_PAGING)).append("</label>")
                .append("<input id='page-length' name='page-length' title='limit' value='").append(pageLength).append("'/>")
                .append("<input id='page-no' name='page-no' title='page no.' value='").append(pageNo).append("'/>");
            if (data != null) {
                html.append("<span id=\"total-pages\">/ ")
                    .append((long) Math.ceil((double) data.total / (double) data.length)).append("</span>");
            }
            // sort
            html.append("</div>");
            html.append("<div class=\"s-col\">");
            html.append("<label>").append(Locale.getString(VantarKey.ADMIN_SORT)).append("</label>")
                .append("<input id='sort' name='sort' value='").append(sort).append("'/>")
                .append("<select id='sortpos' name='sortpos'>")
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
                .append("</select>");
            html.append("</div>");
            html.append("</div>");
        }
        // > > > search
        if (options.search) {
            html.append("<div id=\"s-options-b\">");
            html.append("<div class=\"s-col\">");
            html.append("<label>").append(Locale.getString(VantarKey.ADMIN_SEARCH)).append("</label>")
                .append("<select id='search-type'>")
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
                .append("        <option value='BETWEEN'>BETWEEN (x, y) x&gt;=col&lt;=y </option>")
                .append("        <option value='BETWEEN'>BETWEEN (colB, x) col&gt;=x&lt;=colB </option>")
                .append("        <option value='NOT_BETWEEN'>NOT_BETWEEN (x, y) x&lt;col&gt;y</option>")
                .append("        <option value='NOT_BETWEEN'>NOT_BETWEEN (colB, y) col&lt;x&gt;colB</option>")
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
                .append("    <optgroup label='recursive (not supported)'>")
                .append("        <option value='QUERY'>QUERY (recursive-query)</option>")
                .append("        <option value='MAP_KEY_EXISTS'>MAP_KEY_EXISTS</option>")
                .append("        <option value='IN_DTO'>IN_DTO (recursive-query)</option>")
                .append("        <option value='IN_LIST'>IN_LIST (recursive-query)</option>")
                .append("    </optgroup>")
                .append("    <optgroup label='polygon'>")
                .append("        <option value='WITHIN'>WITHIN [lat1, lng1, lat2, lng2,]</option>")
                .append("    </optgroup>")
                .append("</select>")
                .append("<input id='search-col' value='id'/>")
                .append("<input id=\"search-value\"/>")
                .append("<button type='button' id=\"add-search-item\">+</button>");
            html.append("</div>");
            html.append("<div class=\"s-col\">");
            html.append("<label></label>")
                .append("<select id='search-op'>")
                .append("    <option value='AND' selected='selected'>AND</option>")
                .append("    <option value='OR'>OR</option>")
                .append("    <option value='XOR'>XOR</option>")
                .append("    <option value='NOR'>XOR</option>")
                .append("    <option value='NOT'>XOR</option>")
                .append("</select>")
                .append("<button type='submit' id=\"search-b\">Search</button>")
                .append("<button type='button' id='get-search-json'>JSON</button>");
            html.append("</div>");
            html.append("</div>");
            // > > > search conditions
            html.append("<div id=\"s-options-c\">")
                .append("<ul id='search-items'></ul>")
                .append("<p><textarea id='jsonsearch' name='jsonsearch'>").append(jsonSearch).append("</textarea></p>")
                .append("</div>\n");
        }
        html.append("</form>");

        // > > > serial
        if (options.lastSerialId != null) {
            html.append("<div id=\"s-options-d\">");
            html.append("<form method='post'>");
            html.append("<div class=\"s-col\">");
            html.append("<input title='current serial id' id='serial-i' name='serial-i' value='").append(options.lastSerialId).append("'/>");
            html.append("</div>");
            html.append("<div class=\"s-col\">");
            html.append("<button id=\"serial-b\">change</button>");
            html.append("</div>");
            html.append("</form>");
            html.append("</div>");
        }

        html.append("</div>");

        // > > > form multi delete
        if (options.checkListFormUrl != null) {
            html.append("<form method='post' action='").append(options.checkListFormUrl).append("'>");
            addHidden(VantarParam.AUTH_TOKEN, authToken);
            addHidden("dto", dto.getClass().getSimpleName());
        }

        if (data == null) {
            addMessage(Locale.getString(VantarKey.NO_CONTENT));
        } else {
            addDtoList(data, options);
            if (options.event != null) {
                options.event.checkListFormContent();
            }
        }
        if (options.checkListFormUrl != null) {
            html.append("</form>");
        }
        return this;
    }

    /**
     * options: include add/delete/...
     */
    public WebUi addDtoList(PageData data, DtoListOptions options) {
        html.append("<div class='scroll'><table class='list'>");

        // > > > header
        Set<String> exclude = new HashSet<>(10, 1);
        Dto dtoSample = data.data.get(0);
        html.append("<tr>");
        if (options.colOptionCount > 0) {
            html.append("<th id='total' colspan='").append(options.colOptionCount).append("'>")
                .append(data.total).append("</th>");
        }
        html.append("<th class='list-head-id'>id</th>");
        for (String name : options.fields) {
            if ("id".equals(name)) {
                continue;
            }
            Field f = dtoSample.getField(name);
            if (f.isAnnotationPresent(NoList.class)) {
                exclude.add(name);
                continue;
            }
            html.append("<th>").append(name).append("</th>");
        }
        html.append("</tr>");

        for (Dto dto : data.data) {
            html.append("<tr>");
            if (options.colOptionCount > 0) {
                for (DtoListOptions.ColOption colOption : options.event.getColOptions(dto)) {
                    html.append("<td class='option");
                    if (colOption.containerClass != null) {
                        html.append(' ').append(colOption.containerClass);
                    }
                    html.append("'>").append(colOption.content).append("</td>");
                }
            }

            html.append("<td>").append(dto.getId()).append("<input type='hidden' value='").append(dto.getId()).append("'/></td>");
            for (String name : options.fields) {
                if ("id".equals(name) || exclude.contains(name)) {
                    continue;
                }
                Field field = dto.getField(name);
                Object actualValue = dto.getPropertyValue(name);

                String value;
                String hint;
                if (actualValue == null) {
                    value = "-";
                    hint = null;
                    // datetime
                } else if (actualValue instanceof DateTime) {
                    if ("fa".equals(lang)) {
                        value = ((DateTime) actualValue).formatter().getDateTimePersian().getDateTime();
                        hint = ((DateTime) actualValue).formatter().getDateTime();
                    } else {
                        value = ((DateTime) actualValue).formatter().getDateTime();
                        hint = ((DateTime) actualValue).formatter().getDateTimePersian().getDateTime();
                    }
                    // lang
                } else if (field.isAnnotationPresent(Localized.class)) {
                    Map<String, String> l = (Map<String, String>) actualValue;
                    if (l.isEmpty()) {
                        value = "-";
                        hint = null;
                    } else {
                        value = l.get(lang);
                        hint = ObjectUtil.toStringViewable(actualValue);
                    }
                    // anything else
                } else {
                    value = ObjectUtil.toStringViewable(actualValue);
                    hint = null;
                }

                html.append("<td><div");
                if (hint != null) {
                    html.append(" title='").append(hint).append("'");
                }
                html.append(">").append(value).append("</div></td>");
            }
            html.append("</tr>");
        }
        html.append("</table></div>");
        return this;
    }

    private void setWidgetComment(Dto dto, String col, Class<? extends Annotation> annotation, String comment) {
        if (dto.hasAnnotation(col, annotation)) {
            setWidgetComment(comment);
        }
    }

    private void setWidgetComment(String comment) {
        if (widgetComment == null) {
            widgetComment = new StringBuilder(100);
        }
        widgetComment.append(comment);
    }


    public static class DtoListOptions {
        public boolean archive;
        public boolean pagination;
        public boolean search;
        public Long lastSerialId;
        public String checkListFormUrl;
        public String[] fields;
        public int colOptionCount;
        public Event event;

        public interface Event {
            void checkListFormContent();
            List<ColOption> getColOptions(Dto dto);
        }

        public static class ColOption {
            public String containerClass;
            public String content;
        }
    }
}