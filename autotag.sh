#Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.
#
#This program is free software: you can redistribute it and/or modify
#it under the terms of the GNU Affero General Public License as published by
#the Free Software Foundation, either version 3 of the License, or
#(at your option) any later version.
#
#This program is distributed in the hope that it will be useful,
#but WITHOUT ANY WARRANTY; without even the implied warranty of
#MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#GNU Affero General Public License for more details.
#
#You should have received a copy of the GNU Affero General Public License
#along with this program.  If not, see <http://www.gnu.org/licenses/>.

POM_VERSION=`cat pom.xml|grep "<version>"|sed -n '2p'`
POM_VERSION=${POM_VERSION#*>}
POM_VERSION=${POM_VERSION%<*}
if [ $POM_VERSION != "" ]; then
	PROJECT_ID=$1
	REF=$2
	curl -H "PRIVATE-TOKEN: 5zWk91yBpWfrbrN5fZDV" -X POST -d "tag_name=$POM_VERSION&ref=$REF" http://192.168.0.82:7070/api/v4/projects/$PROJECT_ID/repository/tags
fi
