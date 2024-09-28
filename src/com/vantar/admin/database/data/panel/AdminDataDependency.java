package com.vantar.admin.database.data.panel;

import com.vantar.admin.index.Admin;
import com.vantar.database.dependency.*;
import com.vantar.database.dto.*;
import com.vantar.exception.*;
import com.vantar.locale.VantarKey;
import com.vantar.web.*;
import javax.servlet.http.HttpServletResponse;
import java.util.*;


public class AdminDataDependency {

    // todo dependency
    public static void databaseRelations(Params params, HttpServletResponse response) throws FinishException {
        WebUi ui = Admin.getUi(VantarKey.ADMIN_DEPENDENCIES, params, response, true);
        Map<Class<? extends Dto>, List<RelationMap.Relation>> relations = new RelationMap().getRelations();
    }

    public static void getDto(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDto(VantarKey.ADMIN_DEPENDENCIES, "dependencies", params, response, info);

        Map<Class<? extends Dto>, List<RelationMap.Relation>> all = new RelationMap().getRelations();
        u.ui.addHeading(3, "Relations");
        List<RelationMap.Relation> r = all.get(info.dtoClass);
        if (r != null) {
            for (RelationMap.Relation relation : r) {
                StringBuilder colBuff = new StringBuilder(30);
                for (RelationMap.RelationRoute route : relation.fieldRoute) {
                    colBuff.append(route.fieldName).append('.');
                }
                RelationMap.RelationRoute lastField = relation.fieldRoute.get(relation.fieldRoute.size() - 1);
                if (lastField.isPkFk || lastField.isCollection) {
                    // > > > fk or collection
                    if (lastField.isDto) {
                        colBuff.append("id");
                    } else {
                        colBuff.setLength(colBuff.length() - 1);
                    }
                    // fk or collection < < <
                } else if (lastField.isMapKey) {
                    // > > > map
                    colBuff.setLength(colBuff.length() - 1);
                    colBuff.append("<key>");
                    // map < < <
                } else if (lastField.isMapValue) {
                    // > > > map
                    if (lastField.isDto) {
                        colBuff.append("id");
                    } else {
                        colBuff.setLength(colBuff.length() - 1);
                    }
                    colBuff.append("<value>");
                    // map < < <
                }
                String name1 = relation.fkClasses[0].getSimpleName();
                String name2 = relation.fkClasses.length == 2 ? relation.fkClasses[1].getSimpleName() : null;
                u.ui.addKeyValue(
                    u.ui.getHref(name1, "/admin/data/dependencies/dto?dto=" + name1, false, false, null)
                    + (name2 == null ? "" : "," + u.ui.getHref(name2, "/admin/data/dependencies/dto?dto=" + name2, false, false, null)),
                    colBuff.toString(),
                    null,
                    false
                );
            }
        }

        Map<Class<? extends Dto>, Set<ReverseRelationMap.Relation>> rev = new ReverseRelationMap().getRelations();
        u.ui.addHeading(3, "Reverse relations");
        Set<ReverseRelationMap.Relation> rr = rev.get(info.dtoClass);
        if (rr != null) {
            for (ReverseRelationMap.Relation relation : rr) {
                StringBuilder colBuff = new StringBuilder(30);
                for (ReverseRelationMap.RelationRoute route : relation.fieldRoute) {
                    colBuff.append(route.fieldName).append('.');
                }
                ReverseRelationMap.RelationRoute lastField = relation.fieldRoute.get(relation.fieldRoute.size() - 1);
                if (lastField.isPkFk || lastField.isCollection) {
                    // > > > fk or collection
                    if (lastField.isDto) {
                        colBuff.append("id");
                    } else {
                        colBuff.setLength(colBuff.length() - 1);
                    }
                    // fk or collection < < <
                } else if (lastField.isMapKey) {
                    // > > > map
                    colBuff.setLength(colBuff.length() - 1);
                    colBuff.append("<key>");
                    // map < < <
                } else if (lastField.isMapValue) {
                    // > > > map
                    if (lastField.isDto) {
                        colBuff.append("id");
                    } else {
                        colBuff.setLength(colBuff.length() - 1);
                    }
                    colBuff.append("<value>");
                    // map < < <
                }
                String name = relation.fkClass.getSimpleName();
                u.ui.addKeyValue(
                    u.ui.getHref(name, "/admin/data/dependencies/dto?dto=" + name, false, false, null),
                    colBuff.toString(),
                    null,
                    false
                );
            }
        }

        u.ui.finish();
    }

    public static void getDtoItem(Params params, HttpServletResponse response, DtoDictionary.Info info) throws FinishException {
        DataUtil.Ui u = DataUtil.initDtoItem(VantarKey.ADMIN_DEPENDENCIES, "dependencies", params, response, info);

        int limit = params.getInteger("limit", 7);
        String filterDto = params.getString("filter-dto");
        u.ui.addEmptyLine()
            .beginFormGet()
            .addInput("Limit", "limit", limit)
            .addInput("Filter DTO", "filter-dto", filterDto)
            .addHidden("dto", u.dto.getClass().getSimpleName())
            .addHidden("id", u.dto.getId())
            .addSubmit()
            .blockEnd();

        u.ui.addHeading(2, info.dtoClass.getSimpleName() + " (" + u.dto.getId() + ")");

        DataDependencyMongo ddm = new DataDependencyMongo(u.dto);
        ddm.setLimit(limit);
        ddm.setFilterDto(filterDto);
        plot(u.ui, ddm.getDependencies());

        u.ui.finish();
    }

    private static void plot(WebUi ui, List<DataDependencyMongo.Dependency> deps) {
        for (DataDependencyMongo.Dependency d : deps) {
            ui.beginTree(d.baseClass + " --> " + d.blockerClass);
           for (Map.Entry<String, List<DataDependencyMongo.Dependency>> e : d.items.entrySet()) {
                ui.addHeading(3, e.getKey());
                plot(ui, e.getValue());
            }
            ui.blockEnd().write();
        }
    }
}