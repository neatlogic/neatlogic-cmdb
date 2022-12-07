#
# Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
# 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
#

POM_VERSION=`cat pom.xml|grep version|sed -n '2p'`
POM_VERSION=${POM_VERSION#*>}
POM_VERSION=${POM_VERSION%<*}
if [ $POM_VERSION != "" ]; then
	PROJECT_ID=$1
	REF=$2
	curl -H "PRIVATE-TOKEN: 5zWk91yBpWfrbrN5fZDV" -X POST -d "tag_name=$POM_VERSION&ref=$REF" http://git.techsure.cn:7070/api/v4/projects/$PROJECT_ID/repository/tags
fi
