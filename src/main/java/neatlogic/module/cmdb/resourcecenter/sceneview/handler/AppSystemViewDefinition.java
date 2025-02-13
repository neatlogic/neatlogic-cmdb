/*
 * Copyright (C) 2025  深圳极向量科技有限公司 All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package neatlogic.module.cmdb.resourcecenter.sceneview.handler;

import neatlogic.framework.cmdb.resourcecenter.sceneview.core.ISceneViewDefinition;
import neatlogic.framework.cmdb.resourcecenter.sceneview.core.SceneViewFieldVo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AppSystemViewDefinition implements ISceneViewDefinition {

    private final List<SceneViewFieldVo> fieldList = new ArrayList<>();
    private final List<String> functionPathList = new ArrayList<>();
    @Override
    public String getName() {
        return "scence_appsystem";
    }

    @Override
    public String getLabel() {
        return "应用系统场景";
    }

    @Override
    public List<String> getFunctionPathList() {
        if (CollectionUtils.isEmpty(functionPathList)) {
            functionPathList.add("配置管理/应用清单");
        }
        return new ArrayList<>(functionPathList);
    }

    @Override
    public List<SceneViewFieldVo> getFieldList() {
        if (CollectionUtils.isEmpty(fieldList)) {
            fieldList.add(new SceneViewFieldVo("id", "ID", true));
            fieldList.add(new SceneViewFieldVo("name", "名称", true));
            fieldList.add(new SceneViewFieldVo("abbr_name", "简称", true));
            fieldList.add(new SceneViewFieldVo("type_id", "类型ID", true));
            fieldList.add(new SceneViewFieldVo("type_name", "类型名称", true));
            fieldList.add(new SceneViewFieldVo("type_label", "类型Label", true));
            fieldList.add(new SceneViewFieldVo("fcu", "创建者", true));
            fieldList.add(new SceneViewFieldVo("fcd", "创建日期", true));
            fieldList.add(new SceneViewFieldVo("lcu", "修改者", true));
            fieldList.add(new SceneViewFieldVo("lcd", "修改日期", true));
            fieldList.add(new SceneViewFieldVo("maintenance_window", "维护窗口", true));
            fieldList.add(new SceneViewFieldVo("description", "描述", true));
            fieldList.add(new SceneViewFieldVo("inspect_status", "巡检状态", true));
            fieldList.add(new SceneViewFieldVo("inspect_time", "巡检时间", true));
            fieldList.add(new SceneViewFieldVo("monitor_status", "监控状态", true));
            fieldList.add(new SceneViewFieldVo("monitor_time", "监控时间", true));
        }
        return new ArrayList<>(fieldList);
    }
}
