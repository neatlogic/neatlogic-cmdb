#Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.

POM_VERSION=`cat pom.xml|grep "<version>"|sed -n '2p'`
POM_VERSION=${POM_VERSION#*>}
POM_VERSION=${POM_VERSION%<*}
if [ $POM_VERSION != "" ]; then
	PROJECT_ID=$1
	REF=$2
	curl -H "PRIVATE-TOKEN: 5zWk91yBpWfrbrN5fZDV" -X POST -d "tag_name=$POM_VERSION&ref=$REF" http://git.techsure.cn:7070/api/v4/projects/$PROJECT_ID/repository/tags
fi
