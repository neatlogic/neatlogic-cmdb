/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.config;

import neatlogic.framework.config.ITenantConfig;

public enum CmdbTenantConfig implements ITenantConfig {
    SYNC_BATCH_RUNNER_COUNT("sync.batch.runner.count", "5", "同步自动采集数据进CMDB时并发线程数，默认是5"),
    SYNC_MONGODB_CURSOR_MAX_RETRY("sync.mongodb.cursor.max.retry", "5", "同步自动采集数据进CDMB时遇到mongodb游标丢失后，进行自动重连的最大次数，默认是5");

    final String key;
    final String value;
    final String description;

    CmdbTenantConfig(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
