#!/usr/bin/env bash
# 服务编译构建脚本。
#
# 注意：
# ----
# 如果项目在 Plus 发布系统上使用的是 MDP 发布类型，则项目不使用当前脚本。参见：https://docs.sankuai.com/dp/hbar/mdp-docs/master/deploy/#32

if [ ! -z "$APP_KEY" ]; then
    echo "start replace appkey...\n"
    find . -type f -name "app.properties" -exec sed -i '1s/app.name=.*/app.name='$APP_KEY'/g' {} +
fi

if [ -z "$ACTIVE_PROFILE" ]; then
    ACTIVE_PROFILE=$PLUS_TEMPLATE_ENV
fi
mvn clean package -P $ACTIVE_PROFILE -DskipTests=true -Dmaven.source.skip=true -Dsource.skip=true