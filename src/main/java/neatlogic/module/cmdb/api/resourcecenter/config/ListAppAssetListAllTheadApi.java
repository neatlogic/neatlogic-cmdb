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

package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.IResourceCenterDataSource;
import neatlogic.framework.cmdb.resourcecenter.datasource.core.ResourceCenterDataSourceFactory;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class ListAppAssetListAllTheadApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取应用清单资产清单所有表头列表";
    }

    @Input({})
    @Output({})
    @Description(desc = "获取资产清单所有表头列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
//        Map<JSONObject, String> map = new HashMap<>();
//        {
//            JSONObject jsonObj = new JSONObject();
//            jsonObj.put("key", "key1");
//            jsonObj.put("title", "title1");
//            map.put(jsonObj, "String1");
//        }
//        {
//            JSONObject jsonObj = new JSONObject();
//            jsonObj.put("key", "key1");
//            jsonObj.put("title", "title1");
//            String s = map.get(jsonObj);
//            System.out.println("s = " + s);
//        }
//        Map<ValueTextVo, String> map = new HashMap<>();
//        {
//            map.put(new ValueTextVo("key1", "title1"), "String2");
//        }
//        {
//            String s = map.get(new ValueTextVo("key1", "title1"));
//            System.out.println("s = " + s);
//        }
//        List<ValueTextVo> fieldList = ResourceEntityFactory.getFieldListByViewName("scence_application_asset_list_detail");
        List<ValueTextVo> fieldList = new ArrayList<>();
        fieldList.add(new ValueTextVo("id", "ID"));
        fieldList.add(new ValueTextVo("name", "名称"));
        fieldList.add(new ValueTextVo("ip", "IP地址"));
//        fieldList.add(new ValueTextVo("type_id", "类型ID"));
//        fieldList.add(new ValueTextVo("type_name", "类型名称"));
//        fieldList.add(new ValueTextVo("type_label", "类型Label"));
        fieldList.add(new ValueTextVo("ci", "模型"));
        fieldList.add(new ValueTextVo("fcu", "创建者"));
        fieldList.add(new ValueTextVo("fcd", "创建日期"));
        fieldList.add(new ValueTextVo("lcu", "修改者"));
        fieldList.add(new ValueTextVo("lcd", "修改日期"));
//        fieldList.add(new ValueTextVo("maintenance_window", "维护窗口"));
        fieldList.add(new ValueTextVo("maintenanceWindow", "维护窗口"));
        fieldList.add(new ValueTextVo("description", "描述"));
//        fieldList.add(new ValueTextVo("network_area", "网络区域"));
//        fieldList.add(new ValueTextVo("inspect_status", "巡检状态"));
//        fieldList.add(new ValueTextVo("inspect_time", "巡检时间"));
//        fieldList.add(new ValueTextVo("monitor_status", "监控状态"));
//        fieldList.add(new ValueTextVo("monitor_time", "监控时间"));
        fieldList.add(new ValueTextVo("networkArea", "网络区域"));
        fieldList.add(new ValueTextVo("inspectStatus", "巡检状态"));
        fieldList.add(new ValueTextVo("inspectTime", "巡检时间"));
        fieldList.add(new ValueTextVo("monitorStatus", "监控状态"));
        fieldList.add(new ValueTextVo("monitorTime", "监控时间"));
        fieldList.add(new ValueTextVo("port", "端口"));
//        fieldList.add(new ValueTextVo("bg_id", "分组ID"));
//        fieldList.add(new ValueTextVo("bg_name", "分组名称"));
        fieldList.add(new ValueTextVo("businessGroup", "事业部"));
//        fieldList.add(new ValueTextVo("allip_id", "IP列表的ID"));
//        fieldList.add(new ValueTextVo("allip_ip", "IP列表的IP地址"));
//        fieldList.add(new ValueTextVo("allip_label", "IP列表的描述"));
        fieldList.add(new ValueTextVo("allIp", "IP列表"));
//        fieldList.add(new ValueTextVo("user_id", "用户ID"));
//        fieldList.add(new ValueTextVo("user_uuid", "用户UUID"));
//        fieldList.add(new ValueTextVo("user_name", "用户名"));
        fieldList.add(new ValueTextVo("owner", "负责人"));
//        fieldList.add(new ValueTextVo("state_id", "状态ID"));
//        fieldList.add(new ValueTextVo("state_name", "状态名"));
//        fieldList.add(new ValueTextVo("state_label", "状态描述"));
        fieldList.add(new ValueTextVo("state", "状态"));
//        fieldList.add(new ValueTextVo("vendor_id", "厂商ID"));
//        fieldList.add(new ValueTextVo("vendor_name", "厂商名称"));
//        fieldList.add(new ValueTextVo("vendor_label", "厂商描述"));
        fieldList.add(new ValueTextVo("vendor", "厂商"));
//        fieldList.add(new ValueTextVo("datacenter_id", "数据中心ID"));
//        fieldList.add(new ValueTextVo("datacenter_name", "数据中心名称"));
        fieldList.add(new ValueTextVo("dataCenter", "数据中心"));
//        fieldList.add(new ValueTextVo("env_id", "环境ID"));
//        fieldList.add(new ValueTextVo("env_name", "环境名称"));
//        fieldList.add(new ValueTextVo("env_seq_no", "环境序号"));
        fieldList.add(new ValueTextVo("appEnvironment", "应用环境"));
//        fieldList.add(new ValueTextVo("app_module_id", "应用模块ID"));
//        fieldList.add(new ValueTextVo("app_module_name", "应用模块名"));
//        fieldList.add(new ValueTextVo("app_module_abbr_name", "应用模块简称"));
        fieldList.add(new ValueTextVo("appModule", "应用模块"));
//        fieldList.add(new ValueTextVo("app_system_id", "应用系统ID"));
//        fieldList.add(new ValueTextVo("app_system_name", "应用系统名"));
//        fieldList.add(new ValueTextVo("app_system_abbr_name", "应用系统简称"));
        fieldList.add(new ValueTextVo("appSystem", "应用系统"));
        IResourceCenterDataSource resourceCenterDataSource = ResourceCenterDataSourceFactory.getResourceCenterDataSource();
        List<ValueTextVo> appAssertAllTheadList = resourceCenterDataSource.getAppAssertAllTheadList();
        return TableResultUtil.getResult(appAssertAllTheadList);
    }

    @Override
    public String getToken() {
        return "resourcecenter/app/assetlist/theadlist";
    }
}
