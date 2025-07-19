#!/bin/bash

# 检查是否在Git仓库中
if [ ! -d ".git" ]; then
    echo "❌ 当前位置不是Git仓库根目录，脚本终止执行"
    exit 1
fi

# 定义命令参数
OUTPUT_DIR="build/codeAnalysisReports"
LOG_FILE="build/codeAnalysis.log"  # 新增日志文件路径

mkdir -p "$OUTPUT_DIR"
touch $LOG_FILE

LOG_FILE_FULL_PATH=$(realpath $LOG_FILE)

# 打印开始信息
echo "开始代码检测..."
echo "检测日志将保存到: $LOG_FILE_FULL_PATH"
echo ""

BRANCH_NAME=$(git rev-parse --abbrev-ref HEAD)
BRANCH_COMMIT=$(git rev-parse HEAD)
REPORT_NAME=$(echo $BRANCH_NAME | tr '/' '-')

echo "【检测报告】" > $LOG_FILE
echo "分支：   ${BRANCH_NAME}" >> $LOG_FILE
echo "commit：${BRANCH_COMMIT}" >> $LOG_FILE
echo "时间：   $(date +"%Y-%m-%d %H:%M:%S")" >> $LOG_FILE
echo -e "--------------------------------------------------------\n" >> $LOG_FILE

# 执行 Gradle 命令并同时输出到终端和文件
#./gradlew checkStaticCode -DcheckType=full -DuseBaseline=true 2>&1 | tee -a "$LOG_FILE"
# 执行 Gradle 命令的结果输出到文件
./gradlew checkStaticCode -DcheckType=full -DuseBaseline=true >> "$LOG_FILE" 2>&1

# 检查命令退出状态（注意使用 PIPESTATUS 获取管道中第一个命令的退出状态）
if [ ${PIPESTATUS[0]} -eq 0 ]; then

    # 创建分支文件并写入 commit ID
    OUTPUT_FILE="$OUTPUT_DIR/$REPORT_NAME"
    echo "$BRANCH_COMMIT" > "$OUTPUT_FILE"

    echo "✅ 代码检测完成，暂无异常问题"
    echo "检测记录已保存"
    echo "    Branch: $REPORT_NAME"
    echo "    Commit: $BRANCH_COMMIT"
    echo "完整检测报告请看: "
    echo "    file://$LOG_FILE_FULL_PATH"
    echo ""

    exit 0
else
    echo "❌ 代码检测失败，请处理异常问题"
    echo "错误信息请看检测报告: "
    echo "    file://${LOG_FILE_FULL_PATH}"
    echo ""

    exit 1
fi