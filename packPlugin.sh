#!/bin/bash

# 插件打包脚本

set -e  # 遇到错误时退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查是否在项目根目录
check_project_root() {
    if [ ! -f "build.gradle.kts" ]; then
        print_error "请在项目根目录执行此脚本"
        exit 1
    fi
}

# 清理之前的构建
clean_build() {
    print_info "清理之前的构建..."
    ./gradlew clean
}

# 构建插件
build_plugin() {
    print_info "开始构建插件..."
    ./gradlew buildPlugin
}

# 查找并复制插件文件
copy_plugin() {
    print_info "查找插件文件..."
    
    # 查找插件文件
    PLUGIN_FILE=$(find build/distributions -name "*.zip" -type f | head -n 1)
    
    if [ -z "$PLUGIN_FILE" ]; then
        print_error "未找到插件文件"
        exit 1
    fi
    
    print_info "找到插件文件: $PLUGIN_FILE"
    
    # 获取文件名
    FILENAME=$(basename "$PLUGIN_FILE")
    
    # 复制到项目根目录
    cp "$PLUGIN_FILE" "./$FILENAME"
    
    print_success "插件已复制到项目根目录: $FILENAME"
    
    # 显示文件信息
    print_info "文件信息:"
    ls -lh "./$FILENAME"
}

# 主函数
main() {
    print_info "开始插件打包流程..."
    
    # 检查项目根目录
    check_project_root
    
    # 清理构建
    clean_build
    
    # 构建插件
    build_plugin
    
    # 复制插件文件
    copy_plugin
    
    print_success "插件打包完成！"
    print_info "插件文件已保存到项目根目录"
}

# 执行主函数
main "$@" 