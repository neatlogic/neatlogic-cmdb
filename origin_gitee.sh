dir=$(pwd)
echo "[echo] --------$dir-----------"
# 检查是否存在.git目录或.git配置文件
if [ -d ".git" ] || [ -f ".git" ]; then
    #如果pushgitee不存在，则先补充pushgitee
    is_pushgitee_exist=$(git config --get alias.pushgitee)
    if [ -z "$is_pushgitee_exist" ]; then
        echo "[echo] 不存在[alias]：pushgitee"
        currentProject=$(basename "$(pwd)")
        git config alias.pushgitee "push --force git@gitee.com:neat-logic/$currentProject.git"
    fi
    # 获取origin的URL和pushgitee的URL
    origin_url=$(git remote get-url origin)
    pushgitee_url=$(git config alias.pushgitee | sed 's/push --force //')
    #如果origin不是gitee
    pattern="git@gitee.com"
    if [[ ! $origin_url == *$pattern* ]]; then
        # 如果这两个URL都存在，则交换它们
        if [ ! -z "$origin_url" ] && [ ! -z "$pushgitee_url" ]; then
            git remote set-url origin "$pushgitee_url"
            git config alias.pushgitee "push --force $origin_url"
            echo "[echo] origin_url: $origin_url"
            echo "[echo] pushgitee_url: $pushgitee_url"
        else
            echo "[echo] Missing origin or pushgitee URL in $dir"
        fi
    fi
else
    echo "[echo] No git repository in $dir"
fi
echo "[echo] ----------------------"