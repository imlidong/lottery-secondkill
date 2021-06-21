#!/usr/bin/env bash
# 服务启动后状态检查脚本。
#
# 注意：
# ----
# 如果项目在 Plus 发布系统上使用的是 MDP 发布类型，则项目不使用当前脚本。参见：https://docs.sankuai.com/dp/hbar/mdp-docs/master/deploy/#32

set

k=1
echo "check service......"

if [[ -z ${WAIT_SECONDS} ]]; then
    WAIT_SECONDS=30
fi

if [ -z $TEST_URL ]; then
    TEST_URL="127.0.0.1:8080/monitor/alive"
fi

for k in $(seq 1 $WAIT_SECONDS)
do
    sleep 1
    STATUS_CODE=`curl -o /dev/null -s -w %{http_code} $TEST_URL`
    if [ "$STATUS_CODE" = "200" ];then
        echo "$k request test_url:$TEST_URL succeeded!"
        echo "$k response code:$STATUS_CODE"
        break;
    elif [ "$STATUS_CODE" = "000" ];then
        echo "$k request test_url:$TEST_URL failed!"
        echo "$k response code: $STATUS_CODE"
    else
        echo "$k request test_url:$TEST_URL failed!"
        echo "$k response code: $STATUS_CODE"
        exit -1
    fi
    if [ $k -eq $WAIT_SECONDS ];then
        echo "have tried ${k} times, no more try"
        echo "failed"
        exit -1
    fi
done

#测试环境安装arthas
function getEnv(){
    FILE_NAME="/data/webapps/appenv"
    PROP_KEY="env"
    PROP_VALUE=""
    if [[ -f "$FILE_NAME" ]]; then
        PROP_VALUE=`cat ${FILE_NAME} | grep -w ${PROP_KEY} | cut -d'=' -f2`
    fi
    echo $PROP_VALUE
}

function installArthas() {
    if [ -z "$ARTHAS_ENABLED" ]; then
        ARTHAS_ENABLED=true
    fi
    if [ "$ARTHAS_ENABLED" != "true" ]; then
        echo "arthas is disabled."
        return
    fi

    if [ ! -d arthas ]; then
        mkdir arthas
    fi
    cd arthas || return
    if [ ! -x as.sh ]; then
        echo "Need to download and install arthas."
        curl -s -O https://s3plus.sankuai.com/v1/mss_c517ad14def1420690117f60f5150b79/arthas/arthas-3.1.4-mdp.tar.gz
        tar -xzf arthas-3.1.4-mdp.tar.gz
        sh install-local.sh
        echo "Successfully installed arthas."
    else
        echo "Found arthas, no need to install."
    fi
    if [ -x as.sh ]; then
        ./as.sh --attach-only @0.0.0.0:44397:44398
    fi
    echo "Successfully enabled arthas."
    cd ..
}

whoami
echo $PATH

ENV=`getEnv`
if [ "$ENV" == "test" ]; then
    installArthas
fi
exit 0