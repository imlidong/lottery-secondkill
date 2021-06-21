#!/usr/bin/env bash
# 服务启动运行脚本。
#
# 注意：
# ----
# 如果项目在 Plus 发布系统上使用的是 MDP 发布类型，则项目不使用当前脚本。参见：https://docs.sankuai.com/dp/hbar/mdp-docs/master/deploy/#32

#generate memory options for JVM according to the memory size of current system
#-------------Using G1 GC
#1 G Xms=512m MaxMetaspaceSize=256m ReservedCodeCacheSize=240m
#2 G Xms=1g MaxMetaspaceSize=256m ReservedCodeCacheSize=240m
#4 G Xms=2g MaxMetaspaceSize=256m ReservedCodeCacheSize=240m
#8 G Xms=4g MaxMetaspaceSize=512m ReservedCodeCacheSize=240m
#16G Xms=12g MaxMetaspaceSize=512m ReservedCodeCacheSize=240m
#22-28G Xms=18g MaxMetaspaceSize=512m ReservedCodeCacheSize=240m
#32-44G Xms=24g MaxMetaspaceSize=1g ReservedCodeCacheSize=240m
#64-88G Xms=32g MaxMetaspaceSize=2g ReservedCodeCacheSize=240m

function init() {
    APP_KEY="lottery-lidong-service"

    if [ -z "$LOG_PATH" ]; then
        LOG_PATH="/opt/logs/$APP_KEY"
    fi
    mkdir -p "$LOG_PATH"

    if [ -z "$WORK_PATH" ]; then
        WORK_PATH="/opt/meituan/$APP_KEY"
    fi

    if [ -z "$REMOTE_DEBUG" ]; then
        REMOTE_DEBUG=$(remoteDebug)
    fi

    JAVA_CMD="java8"
    if ! command -v $JAVA_CMD >/dev/null 2>&1; then
        JAVA_CMD="/usr/local/$JAVA_CMD/bin/java"
    fi

    JVM_ARGS="-server -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Djava.io.tmpdir=/tmp -Djava.net.preferIPv6Addresses=false"

    if [ -z "$JVM_GC" ]; then
        GC_LOG_VERBOSE="-XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps -XX:+PrintAdaptiveSizePolicy \
            -XX:+PrintGCApplicationStoppedTime -XX:+PrintHeapAtGC -XX:+PrintStringTableStatistics -XX:+PrintTenuringDistribution"
        GC_LOG_FILE="-Xloggc:$LOG_PATH/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=30 -XX:GCLogFileSize=50M"
        JVM_GC="-XX:+UseG1GC -XX:G1HeapRegionSize=4M -XX:InitiatingHeapOccupancyPercent=40 -XX:MaxGCPauseMillis=100 \
            -XX:+TieredCompilation -XX:CICompilerCount=4 -XX:-UseBiasedLocking $GC_LOG_VERBOSE $GC_LOG_FILE -XX:+PrintFlagsFinal"
    fi

    if [ -z "$JVM_EXT_ARGS" ]; then
        JVM_EXT_ARGS=""
    fi

    if [ -z "$JVM_HEAP" ]; then
        JVM_HEAP=$(getJVMMemSizeOpt)
    fi
}

function run() {
    EXEC="exec"
    EXEC_JAVA="$EXEC $JAVA_CMD $JVM_ARGS $JVM_EXT_ARGS $JVM_HEAP $JVM_GC $REMOTE_DEBUG \
        -XX:ErrorFile=$LOG_PATH/vmerr.log \
        -XX:HeapDumpPath=$LOG_PATH/HeapDump"

    if [ "$UID" = "0" ]; then
        ulimit -n 1024000
        umask 000
    else
        echo "$EXEC_JAVA"
    fi
    cd "$WORK_PATH" || exit
    pwd
    targetPackage=$(find . -maxdepth 1 -type f \( -name "*.jar" -o -name "*.war" \))
    echo "this target jar will be executed: $targetPackage"
    $EXEC_JAVA -jar "$targetPackage" 2>&1
}

function getEnv(){
    FILE_NAME="/data/webapps/appenv"
    PROP_KEY="env"
    PROP_VALUE=""
    if [[ -f "$FILE_NAME" ]]; then
        PROP_VALUE=$(grep -w $PROP_KEY $FILE_NAME | cut -d'=' -f2)
    fi
    echo "$PROP_VALUE"
}

function remoteDebug(){
    if [ -z "$DEBUG_PORT" ]; then
        DEBUG_PORT=44399
    fi
    #QA要求在线下环境提供覆盖率扫描功能参数
    if [ -z "$JACOCO_ENABLED" ]; then
        JACOCO_ENABLED=true
    fi
    DEBUG_CMD=""
    current_env=$(getEnv)
    supported_envs="dev test"
    for env in $supported_envs
    do
        if [ "$current_env" == "$env" ]; then
            DEBUG_CMD="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=$DEBUG_PORT"
            if [ "$JACOCO_ENABLED" == "true" ]; then
                DEBUG_CMD="$DEBUG_CMD -javaagent:/opt/meituan/qa_test/jacocoagent.jar=output=tcpserver,port=6300,address=*"
            fi
            break
        fi
    done
    echo "$DEBUG_CMD"
}


function getTotalMemSizeMb() {
    memsizeKb=`cat /proc/meminfo|grep MemTotal|awk '{print $2}'`
    if [ -z "$memsizeKb" ]; then
        memsizeKb=8*1000*1000
    fi
    memsizeMb=$(( $memsizeKb/1024 ))
    echo $memsizeMb
}

function outputJvmArgs() {
    jvmSize=$1
    MaxMetaspaceSize=$2
    ReservedCodeCacheSize=$3
    echo "-Xss512k -Xmx$jvmSize -Xms$jvmSize -XX:MetaspaceSize=$MaxMetaspaceSize -XX:MaxMetaspaceSize=$MaxMetaspaceSize \
        -XX:+AlwaysPreTouch -XX:ReservedCodeCacheSize=$ReservedCodeCacheSize -XX:+HeapDumpOnOutOfMemoryError "
}

function getJVMMemSizeOpt() {
    memsizeMb=`getTotalMemSizeMb`

    #公司的机器内存比实际标的数字要小，比如8G实际是7900M左右，一般误差小于1G
    #内存分级，单位兆/M
    let maxSize_lvl1=63*1024
    let maxSize_lvl2=31*1024
    let maxSize_lvl3=21*1024
    let maxSize_lvl4=15*1024
    let maxSize_lvl5=7*1024
    let maxSize_lvl6=3*1024
    let maxSize_lvl7=1024
    let maxSize_lvl8=512

    if [[ $memsizeMb -gt $maxSize_lvl1 ]]
    then
        jvmSize="32g"
        MaxMetaspaceSize="2g"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl2 && $memsizeMb -le $maxSize_lvl1 ]]
    then
        jvmSize="24g"
        MaxMetaspaceSize="1g"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl3 && $memsizeMb -le $maxSize_lvl2 ]]
    then
        jvmSize="18g"
        MaxMetaspaceSize="512m"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl4 && $memsizeMb -le $maxSize_lvl3 ]]
    then
        jvmSize="12g"
        MaxMetaspaceSize="512m"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl5 && $memsizeMb -le $maxSize_lvl4 ]]
    then
        jvmSize="4g"
        MaxMetaspaceSize="512m"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl6 && $memsizeMb -le $maxSize_lvl5 ]]
    then
        jvmSize="2g"
        MaxMetaspaceSize="256m"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl7 && $memsizeMb -le $maxSize_lvl6 ]]
    then
        jvmSize="1g"
        MaxMetaspaceSize="256m"
        ReservedCodeCacheSize="240m"
    fi

    if [[ $memsizeMb -gt $maxSize_lvl8 && $memsizeMb -le $maxSize_lvl7 ]]
    then
        jvmSize="512m"
        MaxMetaspaceSize="256m"
        ReservedCodeCacheSize="240m"
    fi

    if [ $memsizeMb -le $maxSize_lvl8 ]; then
        echo "service start fail:not enough memory for MDP service"
        exit 1
    fi

    outputJvmArgs $jvmSize $MaxMetaspaceSize $ReservedCodeCacheSize
    exit 0
}

# ------------------------------------
# actually work
# ------------------------------------
init
run
